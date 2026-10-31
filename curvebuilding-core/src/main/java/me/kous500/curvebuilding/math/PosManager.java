package me.kous500.curvebuilding.math;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.config.Message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.WorldeditAdapter.adapt;

/**
 * プレイヤーごとに PosData を紐づけて管理・操作する静的マネージャークラス。
 */
public final class PosManager {
    public static final String[] directionOptions = new String[] {
            "back", "down", "east", "forward", "left", "me", "north", "right", "south", "up"
    };

    /**
     * プレイヤーごとのPosDataを保持するマップ。
     */
    private static final Map<UUID, PosData> POS_MAP = new ConcurrentHashMap<>();

    // ユーティリティクラスとしてインスタンス化を防止
    private PosManager() {
        throw new UnsupportedOperationException("Manager class cannot be instantiated");
    }

    public static void addPos(Player player, int n, int h) {
        if (h < 0 || h > 2) {
            throw new IllegalArgumentException("The value of h must be in the range 0 to 2.");
        }

        Vector3 location = adapt(player.getLocation().toVector()).floor();
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        PosData posData = POS_MAP.computeIfAbsent(uuid, k -> new PosData());

        // クラス名を明記して名前衝突を回避
        if (!Objects.equals(posData.getWorldId(), me.kous500.curvebuilding.WorldeditAdapter.getWorldId(world))) {
            posData.reset(world);
        }

        posData.updatePos(n, h, location);
        Chat.info(player, Message.POS_SET, posToString(n, h), location);
    }

    public static void clearPos(Player player) {
        if (POS_MAP.remove(player.getUniqueId()) != null) {
            Chat.info(player, Message.POS_CLEAR_ALL);
        }
    }

    public static void clearPos(Player player, int n, int h) {
        if (h < 0 || h > 2) {
            throw new IllegalArgumentException("h must be in the range 0 to 2.");
        }

        PosData posData = POS_MAP.get(player.getUniqueId());
        if (posData == null) return;

        if (posData.clearSpecificPos(n, h)) {
            Chat.info(player, Message.POS_CLEAR, posToString(n, h));
        }
    }

    public static void insert(Player player, int n) {
        PosData posData = POS_MAP.get(player.getUniqueId());
        if (posData == null || !posData.containsPos(n)) {
            Chat.error(player, Message.POS_NOT_EXIST, n);
            return;
        }

        posData.insertNodeAt(n);
        addPos(player, n, 0);
    }

    public static void remove(Player player, int n) {
        PosData posData = POS_MAP.get(player.getUniqueId());
        if (posData == null || !posData.containsPos(n)) {
            Chat.error(player, Message.POS_NOT_EXIST, n);
            return;
        }

        posData.removeNodeAt(n);
        Chat.info(player, Message.POS_REMOVE, n);
    }

    public static void shift(Player player, int n, char direction) {
        if ('A' <= direction && direction <= 'Z') direction += 32;

        PosData posData = POS_MAP.get(player.getUniqueId());
        if (posData == null) return;

        double pitch = player.getLocation().getPitch();
        double yaw = player.getLocation().getYaw();
        switch (direction) {
            case 'r' -> yaw += 90;
            case 'l' -> yaw -= 90;
            case 'b' -> yaw += 180;
        }

        if (yaw < -180) yaw += 360;
        else if (yaw > 180) yaw -= 360;

        boolean notPitch = String.valueOf(direction).matches("[frlb]");
        boolean notYaw = String.valueOf(direction).matches("[snew]");
        Vector3 shiftVec = Vector3.at(0, 0, 0);
        if (pitch > 67.5 && !notPitch && direction == 'm' || direction == 'd') shiftVec = Vector3.at(0, -n, 0);
        else if (pitch < -67.5 && !notPitch && direction == 'm' || direction == 'u') shiftVec = Vector3.at(0, n, 0);
        else {
            if (Math.abs(yaw) < 67.5 && !notYaw || direction == 's') shiftVec = shiftVec.add(0, 0, n);
            if (-Math.abs(yaw) + 180 < 67.5 && !notYaw || direction == 'n') shiftVec = shiftVec.add(0, 0, -n);
            if (Math.abs(yaw + 90) < 67.5 && !notYaw || direction == 'e') shiftVec = shiftVec.add(n, 0, 0);
            if (Math.abs(yaw - 90) < 67.5 && !notYaw || direction == 'w') shiftVec = shiftVec.add(-n, 0, 0);
        }

        for (int i : posData.getMap().keySet()) {
            Vector3[] vec = posData.getMap().get(i);
            for (int j = 0; j < 3; j++) {
                if (vec != null && vec[j] != null) vec[j] = vec[j].add(shiftVec);
            }
        }
        Chat.info(player, Message.POS_SHIFT);
    }

    public static NavigableMap<Integer, Vector3[]> getPos(Player player) {
        PosData data = POS_MAP.get(player.getUniqueId());
        return data != null ? data.getMap() : null;
    }

    public static World getWorld(Player player) {
        PosData data = POS_MAP.get(player.getUniqueId());
        return data != null ? data.getWorld() : null;
    }

    public static Map<UUID, PosData> getPosMap() {
        return Collections.unmodifiableMap(POS_MAP);
    }

    public static void clearPosMap() {
        POS_MAP.clear();
    }

    private static String posToString(int n, int h) {
        return switch (h) {
            case 1 -> getMessage(Message.POS_NF, n);
            case 2 -> getMessage(Message.POS_NB, n);
            default -> getMessage(Message.POS_N, n);
        };
    }
}