package me.kous500.curvebuilding.curve;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.curve.CurvePoint.Part;
import me.kous500.curvebuilding.math.Vector3;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static me.kous500.curvebuilding.WorldeditAdapter.adapt;

/** The curve of each player, which {@code //bcpos} changes. */
public final class PosManager {
    // Concurrent, as a client in single player clears it on its own thread while the server in the same game runs
    private static final Map<UUID, PosData> POS_MAP = new ConcurrentHashMap<>();
    private static final AtomicReference<@Nullable Consumer<UUID>> CHANGE_LISTENER = new AtomicReference<>();

    private PosManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * The curve of {@code player}.
     *
     * @param player the player
     * @return the curve, or null when they have not set any point
     */
    public static @Nullable PosData get(Player player) {
        return POS_MAP.get(player.getUniqueId());
    }

    /**
     * Sets {@code part} of point {@code n} where {@code player} stands. A point in another world starts a new curve.
     *
     * @param player the player, whose position is used
     * @param n the point number
     * @param part the part to set
     * @return the position set, as {@link #standingPosition} gives it
     */
    public static Vector3 addPos(Player player, int n, Part part) {
        Vector3 location = standingPosition(adapt(player.getLocation().toVector()));
        addPos(player, n, part, location);
        return location;
    }

    /**
     * The position of a point set where a player stands: x and z of the feet go to the nearest half block, the center
     * or an edge of a block, and y to the middle of the block the feet are in.
     *
     * @param feet the position of the feet
     * @return the position of the point
     */
    public static Vector3 standingPosition(Vector3 feet) {
        return Vector3.at(nearestHalfBlock(feet.x()), Math.floor(feet.y()) + 0.5, nearestHalfBlock(feet.z()));
    }

    /** {@code value} to the nearest multiple of 0.5, upwards when it is halfway between two. */
    private static double nearestHalfBlock(double value) {
        return Math.round(value * 2) / 2.0;
    }

    /**
     * Sets {@code part} of point {@code n} at {@code position}, in the world {@code player} is in. A point in another
     * world starts a new curve.
     *
     * @param player the player
     * @param n the point number
     * @param part the part to set
     * @param position the position to set
     */
    public static void addPos(Player player, int n, Part part, Vector3 position) {
        World world = player.getWorld();

        PosData posData = POS_MAP.computeIfAbsent(player.getUniqueId(), k -> new PosData());
        if (!posData.isIn(world)) {
            posData.reset(world);
        }

        posData.set(n, part, position);
        changed(player);
    }

    /**
     * Removes the whole curve of {@code player}.
     *
     * @param player the player
     * @return whether they had a curve
     */
    public static boolean clearPos(Player player) {
        return changed(player, POS_MAP.remove(player.getUniqueId()) != null);
    }

    /**
     * Clears {@code part} of point {@code n} of {@code player}.
     *
     * @param player the player
     * @param n the point number
     * @param part the part to clear
     * @return whether point {@code n} was there
     */
    public static boolean clearPos(Player player, int n, Part part) {
        PosData posData = get(player);
        return changed(player, posData != null && posData.clear(n, part));
    }

    /**
     * Makes room for a new point {@code n} of {@code player}, before the point that has that number: the points from
     * {@code n} on get the next numbers. The new point is set with {@link #addPos}.
     *
     * @param player the player
     * @param n the number of the new point
     * @return whether there was a point {@code n}; nothing changes without one
     */
    public static boolean insertAt(Player player, int n) {
        PosData posData = get(player);
        if (posData == null || !posData.contains(n)) return false;

        posData.insertAt(n);
        return changed(player, true);
    }

    /**
     * Removes point {@code n} of {@code player}, so that the points after it get the numbers before.
     *
     * @param player the player
     * @param n the number of the point to remove
     * @return whether there was a point {@code n}
     */
    public static boolean remove(Player player, int n) {
        PosData posData = get(player);
        if (posData == null || !posData.contains(n)) return false;

        posData.removeAt(n);
        return changed(player, true);
    }

    /**
     * Moves the whole curve of {@code player} by {@code offset}.
     *
     * @param player the player
     * @param offset how far to move
     * @return whether they had a curve
     */
    public static boolean move(Player player, Vector3 offset) {
        PosData posData = get(player);
        if (posData == null) return false;

        posData.move(offset);
        return changed(player, true);
    }

    /**
     * The curves of every player.
     *
     * @return the curves by the UUID of their player, in a map that cannot be changed but shows the changes made here
     */
    public static Map<UUID, PosData> getPosMap() {
        return Collections.unmodifiableMap(POS_MAP);
    }

    /**
     * Removes the curves of every player.
     */
    public static void clearPosMap() {
        POS_MAP.clear();
    }

    /**
     * Gives {@code player} the curve {@code posData}, such as one read from a file, or removes their curve. The change
     * listener is not told.
     *
     * @param player the UUID of the player
     * @param posData the curve, or null to remove it
     */
    public static void setCurve(UUID player, @Nullable PosData posData) {
        if (posData == null) {
            POS_MAP.remove(player);
        } else {
            POS_MAP.put(player, posData);
        }
    }

    /**
     * Tells {@code listener} the UUID of a player right after {@link #addPos}, {@link #clearPos}, {@link #insertAt},
     * {@link #remove} or {@link #move} changes their curve, on the thread that changed it. There is one listener at a
     * time.
     *
     * @param listener the listener, which replaces the one set before
     */
    public static void setChangeListener(Consumer<UUID> listener) {
        CHANGE_LISTENER.set(listener);
    }

    /**
     * Removes {@code listener}, when it is still the one set.
     *
     * @param listener the listener to remove
     */
    public static void removeChangeListener(Consumer<UUID> listener) {
        CHANGE_LISTENER.compareAndSet(listener, null);
    }

    private static void changed(Player player) {
        Consumer<UUID> listener = CHANGE_LISTENER.get();
        if (listener != null) listener.accept(player.getUniqueId());
    }

    /** Tells the listener about {@code player} when {@code isChanged}, and returns {@code isChanged}. */
    private static boolean changed(Player player, boolean isChanged) {
        if (isChanged) changed(player);
        return isChanged;
    }
}
