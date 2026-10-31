package me.kous500.curvebuilding.math;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.commands.pos.Direction;
import me.kous500.curvebuilding.config.Message;
import me.kous500.curvebuilding.math.CurvePoint.Part;

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

    /** The curve of {@code player}, or null when they have not set any point. */
    public static PosData get(Player player) {
        return POS_MAP.get(player.getUniqueId());
    }

    /** Sets {@code part} of point {@code n} where {@code player} stands. A point in another world starts a new curve. */
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

    public static void clearPos(Player player) {
        if (POS_MAP.remove(player.getUniqueId()) != null) {
            Chat.info(player, Message.POS_CLEAR_ALL);
        }
    }

    public static void clearPos(Player player, int n, Part part) {
        PosData posData = get(player);
        if (posData != null && posData.clear(n, part)) {
            Chat.info(player, Message.POS_CLEAR, posToString(n, part));
        }
    }

    /** Inserts a new point {@code n} where {@code player} stands, before the point that had that number. */
    public static void insert(Player player, int n) {
        PosData posData = get(player);
        if (posData == null || !posData.contains(n)) {
            Chat.error(player, Message.POS_NOT_EXIST, n);
            return;
        }

        posData.insertAt(n);
        addPos(player, n, Part.POINT);
    }

    public static void remove(Player player, int n) {
        PosData posData = get(player);
        if (posData == null || !posData.contains(n)) {
            Chat.error(player, Message.POS_NOT_EXIST, n);
            return;
        }

        posData.removeAt(n);
        Chat.info(player, Message.POS_REMOVE, n);
    }

    /** Moves the whole curve of {@code player} by {@code length} blocks in {@code direction}, seen from the player. */
    public static void shift(Player player, int length, Direction direction) {
        PosData posData = get(player);
        if (posData == null) return;

        Location location = player.getLocation();
        posData.move(direction.offset(length, location.getYaw(), location.getPitch()));
        Chat.info(player, Message.POS_SHIFT);
    }

    public static Map<UUID, PosData> getPosMap() {
        return Collections.unmodifiableMap(POS_MAP);
    }

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
