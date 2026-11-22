package me.kous500.curvebuilding.storage;

import me.kous500.curvebuilding.RecordingConsole;
import me.kous500.curvebuilding.curve.CurvePoint;
import me.kous500.curvebuilding.curve.PosData;
import me.kous500.curvebuilding.curve.PosManager;
import me.kous500.curvebuilding.log.Console;
import me.kous500.curvebuilding.log.Log;
import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the curves of the players in their files. */
class PlayerStoreTest {
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final UUID PLAYER = UUID.fromString("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0");

    @TempDir
    Path temp;

    private Path file;
    private ExecutorService writer;
    private PlayerStore store;
    private Console previousConsole;
    private RecordingConsole console;

    @BeforeEach
    void setUp() {
        previousConsole = Log.console();
        console = new RecordingConsole();
        Log.setConsole(console);
        file = temp.resolve("players/0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0.json");
        writer = Executors.newSingleThreadExecutor();
        store = PlayerStore.start(temp, Clock.systemUTC(), writer);
    }

    @AfterEach
    void tearDown() {
        store.close();
        PosManager.clearPosMap();
        Log.setConsole(previousConsole);
    }

    @Test
    void changedCurveIsWrittenAndReadWhenThePlayerJoinsAgain() throws IOException {
        store.load(PLAYER);
        assertNull(PosManager.getPosMap().get(PLAYER));

        change(curve(1));
        assertTrue(store.awaitWrites(TIMEOUT));
        assertTrue(Files.readString(file).contains("\"point\": [1.0, 2.0, 3.0]"));

        store.unload(PLAYER);
        assertNull(PosManager.getPosMap().get(PLAYER));
        store.load(PLAYER);

        PosData posData = PosManager.getPosMap().get(PLAYER);
        assertEquals("world", posData.worldId());
        assertEquals(points(1), posData.points());
        assertEquals(List.of(), console.entries);
    }

    @Test
    void newestCurveIsWrittenLast() throws IOException {
        store.load(PLAYER);

        for (int x = 1; x <= 50; x++) {
            change(curve(x));
        }
        assertTrue(store.awaitWrites(TIMEOUT));

        assertTrue(Files.readString(file).contains("\"point\": [50.0, 2.0, 3.0]"));
    }

    @Test
    void playerWhoJoinsBeforeTheFileIsWrittenGetsTheNewestCurve() {
        CountDownLatch release = new CountDownLatch(1);
        writer.execute(() -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        store.load(PLAYER);
        change(curve(1));
        store.unload(PLAYER);

        store.load(PLAYER);

        assertFalse(Files.exists(file));
        assertEquals(points(1), PosManager.getPosMap().get(PLAYER).points());
        release.countDown();
        assertTrue(store.awaitWrites(TIMEOUT));
        assertTrue(Files.exists(file));
    }

    @Test
    void removedCurveRemovesTheFile() {
        store.load(PLAYER);
        change(curve(1));
        assertTrue(store.awaitWrites(TIMEOUT));

        change(null);
        assertTrue(store.awaitWrites(TIMEOUT));

        assertFalse(Files.exists(file));
    }

    @Test
    void otherEntriesStayWhenTheCurveChanges() throws IOException {
        write("{\"version\": 1, \"saved\": {\"a\": 1}}");
        store.load(PLAYER);

        change(curve(1));
        assertTrue(store.awaitWrites(TIMEOUT));
        change(null);
        assertTrue(store.awaitWrites(TIMEOUT));

        assertEquals("""
                {
                  "version": 1,
                  "saved": {
                    "a": 1
                  }
                }
                """, Files.readString(file));
    }

    @Test
    void fileOfANewerVersionIsNotWritten() throws IOException {
        String text = "{\"version\": 2}";
        write(text);
        store.load(PLAYER);

        change(curve(1));
        change(null);
        assertTrue(store.awaitWrites(TIMEOUT));

        assertEquals(text, Files.readString(file));
    }

    @Test
    void curveOfAPlayerWhoIsNotLoadedIsNotWritten() {
        change(curve(1));
        assertTrue(store.awaitWrites(TIMEOUT));

        assertFalse(Files.exists(file));
    }

    @Test
    void closedStoreIsNotToldOfTheChanges() {
        store.load(PLAYER);
        store.close();

        change(curve(1));

        assertFalse(Files.exists(file));
    }

    @Test
    void storeWritesTheFilesWaitingWhenItCloses() {
        store.load(PLAYER);
        change(curve(1));

        store.close();

        assertTrue(Files.exists(file));
    }

    @Test
    void closeWaitsUntilTheFilesAreWritten() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        writer.execute(() -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        store.load(PLAYER);
        change(curve(1));

        Thread closing = new Thread(store::close);
        closing.start();
        closing.join(500);
        assertTrue(closing.isAlive(), "close returned before the file was written");
        assertFalse(Files.exists(file));

        release.countDown();
        closing.join(TIMEOUT.toMillis());
        assertFalse(closing.isAlive(), "close did not return after the file was written");
        assertTrue(Files.exists(file));
        assertEquals(List.of(), console.entries);
    }

    /** Gives the player {@code posData} and tells the store, as the commands that change a curve do. */
    private void change(PosData posData) {
        PosManager.setCurve(PLAYER, posData);
        store.changed(PLAYER);
    }

    private static PosData curve(int x) {
        return new PosData("world", points(x));
    }

    private static NavigableMap<Integer, CurvePoint> points(int x) {
        NavigableMap<Integer, CurvePoint> points = new TreeMap<>();
        points.put(1, new CurvePoint(Vector3.at(x, 2, 3), null, null));
        return points;
    }

    private void write(String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }
}
