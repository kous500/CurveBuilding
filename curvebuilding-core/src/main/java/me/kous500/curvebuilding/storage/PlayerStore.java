package me.kous500.curvebuilding.storage;

import me.kous500.curvebuilding.curve.PosData;
import me.kous500.curvebuilding.curve.PosManager;
import me.kous500.curvebuilding.storage.PlayerFile.Contents;
import me.kous500.curvebuilding.storage.PlayerFile.Curve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static me.kous500.curvebuilding.log.Log.console;

/**
 * Keeps the curve of each player in their file, {@code players/<UUID>.json} in a folder (see {@link PlayerFile}). A
 * file is read into {@link PosManager} when its player joins, and written on a thread of its own each time
 * {@link PosManager} changes their curve. The files that cannot be read are backed up in {@code backups/players}.
 *
 * <p>Call {@link #load}, {@link #unload} and {@link #close} on the thread that runs the commands.
 */
public final class PlayerStore {
    private final Path playersFolder;
    private final Path backupFolder;
    private final Clock clock;
    private final Consumer<UUID> listener = this::changed;
    // What the file of each player who is on holds, as it was read or last changed
    private final Map<UUID, Contents> loaded = new ConcurrentHashMap<>();
    // The newest contents of each player that are not written yet. They stay here until they are written, so that a
    // player who joins again before that gets them instead of the file.
    private final Map<UUID, Contents> pending = new ConcurrentHashMap<>();
    private final ExecutorService writer;

    private PlayerStore(Path folder, Clock clock, ExecutorService writer) {
        this.playersFolder = folder.resolve("players");
        this.backupFolder = folder.resolve("backups").resolve("players");
        this.clock = clock;
        this.writer = writer;
    }

    /**
     * Starts keeping the curves in the files in {@code folder}: from now on, each change of a curve in
     * {@link PosManager} is written. The store started before stops being told of the changes.
     *
     * @param folder the folder that has {@code players} and {@code backups}
     * @return the store
     */
    public static PlayerStore start(Path folder) {
        // One thread, so that the files are written in the order of the changes
        ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "CurveBuilding player files");
            thread.setDaemon(true);
            return thread;
        });
        return start(folder, Clock.systemDefaultZone(), writer);
    }

    /**
     * {@link #start(Path)} with the backups named after the time of {@code clock}, and the files written by
     * {@code writer}, which runs one task at a time in the order they are given.
     */
    static PlayerStore start(Path folder, Clock clock, ExecutorService writer) {
        PlayerStore store = new PlayerStore(folder, clock, writer);
        PosManager.setChangeListener(store.listener);
        return store;
    }

    /**
     * Gives {@code player} the curve in their file, or no curve when the file has none. When their file is still
     * waiting to be written, the curve waiting is used.
     *
     * @param player the UUID of the player who joined
     */
    public void load(UUID player) {
        Contents contents = pending.get(player);
        if (contents == null) {
            contents = PlayerFile.read(file(player), backupFolder, clock);
        }
        loaded.put(player, contents);
        Curve curve = contents.curve();
        PosManager.setCurve(player, curve == null ? null : new PosData(curve.world(), curve.points()));
    }

    /**
     * Removes the curve of {@code player} from {@link PosManager}. Their file is already written, or waiting to be.
     *
     * @param player the UUID of the player who left
     */
    public void unload(UUID player) {
        loaded.remove(player);
        PosManager.setCurve(player, null);
    }

    /**
     * Waits until the files waiting to be written when this is called are written.
     *
     * @param timeout how long to wait at most
     * @return whether they were written in time
     */
    public boolean awaitWrites(Duration timeout) {
        try {
            writer.submit(() -> {}).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (RejectedExecutionException e) {
            return writer.isTerminated();
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw new IllegalStateException("A task that does nothing failed", e);
        }
    }

    /**
     * Stops keeping the curves, and waits until the files waiting to be written are written, however long that takes.
     * When the thread is interrupted while it waits, the players whose files are not written yet are logged. The
     * curves stay in {@link PosManager}.
     */
    public void close() {
        PosManager.removeChangeListener(listener);
        writer.shutdown();
        try {
            boolean isWritten = false;
            while (!isWritten) {
                isWritten = writer.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            console().warning("Stopped waiting for the files of these players to be written: " + pending.keySet());
        }
        loaded.clear();
    }

    /** The file of {@code player}. */
    Path file(UUID player) {
        return playersFolder.resolve(player + ".json");
    }

    /** Writes the file of {@code player} with their curve in {@link PosManager}, after it changed. */
    void changed(UUID player) {
        Contents before = loaded.get(player);
        if (before == null || !before.writable()) return;

        PosData posData = PosManager.getPosMap().get(player);
        String world = posData != null ? posData.worldId() : null;
        Contents after = before.withCurve(world != null ? new Curve(world, posData.points()) : null);
        loaded.put(player, after);
        pending.put(player, after);
        // One task for each change; a task finds nothing to write when one before it wrote the newest contents
        writer.execute(() -> writeNewest(player));
    }

    private void writeNewest(UUID player) {
        Contents contents = pending.get(player);
        if (contents == null) return;
        write(player, contents);
        pending.remove(player, contents);
    }

    private void write(UUID player, Contents contents) {
        Path file = file(player);
        String text = PlayerFile.text(contents);
        if (text != null) {
            SafeFiles.write(file, text);
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            console().warning("Could not delete " + file, e);
        }
    }
}
