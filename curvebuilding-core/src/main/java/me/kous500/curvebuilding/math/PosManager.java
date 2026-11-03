package me.kous500.curvebuilding.math;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.commands.pos.Direction;
import me.kous500.curvebuilding.config.Message;
import me.kous500.curvebuilding.math.CurvePoint.Part;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.WorldeditAdapter.adapt;

/** The curve of each player, changed by {@code //bcpos} with a reply to the player. */
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
     */
    public static void addPos(Player player, int n, Part part) {
        Vector3 location = adapt(player.getLocation().toVector()).floor();
        World world = player.getWorld();

        PosData posData = POS_MAP.computeIfAbsent(player.getUniqueId(), k -> new PosData());
        if (!posData.isIn(world)) {
            posData.reset(world);
        }

        posData.set(n, part, location);
        Chat.info(player, Message.POS_SET, posToString(n, part), location);
    }

    /**
     * Removes the whole curve of {@code player}, and tells them when there was one.
     *
     * @param player the player
     */
    public static void clearPos(Player player) {
        if (POS_MAP.remove(player.getUniqueId()) != null) {
            Chat.info(player, Message.POS_CLEAR_ALL);
        }
    }

    /**
     * Clears {@code part} of point {@code n} of {@code player}, and tells them when the point was there.
     *
     * @param player the player
     * @param n the point number
     * @param part the part to clear
     */
    public static void clearPos(Player player, int n, Part part) {
        PosData posData = get(player);
        if (posData != null && posData.clear(n, part)) {
            Chat.info(player, Message.POS_CLEAR, posToString(n, part));
        }
    }

    /**
     * Inserts a new point {@code n} where {@code player} stands, before the point that had that number. An error is
     * shown when there is no point {@code n}.
     *
     * @param player the player, whose position is used
     * @param n the number of the new point
     */
    public static void insert(Player player, int n) {
        PosData posData = get(player);
        if (posData == null || !posData.contains(n)) {
            Chat.error(player, Message.POS_NOT_EXIST, n);
            return;
        }

        posData.insertAt(n);
        addPos(player, n, Part.POINT);
    }

    /**
     * Removes point {@code n} of {@code player}, so that the points after it get the numbers before. An error is shown
     * when there is no point {@code n}.
     *
     * @param player the player
     * @param n the number of the point to remove
     */
    public static void remove(Player player, int n) {
        PosData posData = get(player);
        if (posData == null || !posData.contains(n)) {
            Chat.error(player, Message.POS_NOT_EXIST, n);
            return;
        }

        posData.removeAt(n);
        Chat.info(player, Message.POS_REMOVE, n);
    }

    /**
     * Moves the whole curve of {@code player} by {@code length} blocks in {@code direction}, seen from the player.
     * Nothing happens when they have no curve.
     *
     * @param player the player
     * @param length the number of blocks
     * @param direction the direction, which can depend on where the player looks
     */
    public static void shift(Player player, int length, Direction direction) {
        PosData posData = get(player);
        if (posData == null) return;

        Location location = player.getLocation();
        posData.move(direction.offset(length, location.getYaw(), location.getPitch()));
        Chat.info(player, Message.POS_SHIFT);
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

    private static String posToString(int n, Part part) {
        return switch (part) {
            case F -> getMessage(Message.POS_NF, n);
            case B -> getMessage(Message.POS_NB, n);
            case POINT -> getMessage(Message.POS_N, n);
        };
    }
}
