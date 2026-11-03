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

import static me.kous500.curvebuilding.WorldeditAdapter.adapt;

/** The curve of each player, which {@code //bcpos} changes. */
public final class PosManager {
    // Concurrent, as a client in single player clears it on its own thread while the server in the same game runs
    private static final Map<UUID, PosData> POS_MAP = new ConcurrentHashMap<>();

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
     * @return the position set: the corner of the block the player stands in
     */
    public static Vector3 addPos(Player player, int n, Part part) {
        Vector3 location = adapt(player.getLocation().toVector()).floor();
        World world = player.getWorld();

        PosData posData = POS_MAP.computeIfAbsent(player.getUniqueId(), k -> new PosData());
        if (!posData.isIn(world)) {
            posData.reset(world);
        }

        posData.set(n, part, location);
        return location;
    }

    /**
     * Removes the whole curve of {@code player}.
     *
     * @param player the player
     * @return whether they had a curve
     */
    public static boolean clearPos(Player player) {
        return POS_MAP.remove(player.getUniqueId()) != null;
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
        return posData != null && posData.clear(n, part);
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
        return true;
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
        return true;
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
        return true;
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
}
