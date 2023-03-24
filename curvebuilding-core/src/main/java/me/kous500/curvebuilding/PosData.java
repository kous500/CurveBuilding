package me.kous500.curvebuilding;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.Util.*;

/**
 * posのデータの保存方法とデータを操作するメゾットを定義する。
 */
public final class PosData {
    public World world;
    public NavigableMap<Integer, Vector3[]> p = new TreeMap<>();

    private PosData(World world, Vector3 location, int n, int h) {
        this.world = world;
        Vector3[] l = new Vector3[3];
        l[h] = location;
        this.p.put(n, l);
    }

    /**
     * プレイヤーごとのposのデータを保存する。
     */
    private static final Map<UUID, PosData> POS_MAP = new HashMap<>();

    /**
     * 指定されたプレイヤーのposをposListに追加する。
     *
     * @param  player posを保存するプレイヤー
     * @param  n posの番号
     * @param  h posの種類
     * @throws AssertionError hが0以上2以下ではない場合
     */
    public static void addPos(@NotNull Player player, int n, int h) {
        assert (h >= 0) && (h <= 2) : "The value of h must be in the range 0 to 2.";

        Vector3 location = floorVector(player.getLocation().toVector()).toVector3();
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();
        PosData posData = POS_MAP.get(uuid);

        if (posData != null) {
            if (posData.world != null && !posData.world.getName().equals(world.getName())) {
                posData.p = new TreeMap<>();
            }

            Vector3[] l = posData.p.get(n);
            Vector3[] la = posData.p.get(n + 1);
            Vector3[] lb = posData.p.get(n - 1);

            if (l == null) {
                l = new Vector3[3];
                l[h] = location;
                posData.p.put(n, l);
            } else {
                if (h == 0 && l[h] != null) {
                    double x = location.getX() - l[h].getX();
                    double y = location.getY() - l[h].getY();
                    double z = location.getZ() - l[h].getZ();

                    if (l[1] != null) l[1] = l[1].add(x, y, z);
                    if (l[2] != null) l[2] = l[2].add(x, y, z);
                }

                l[h] = location;

                if (h == 1 && la != null && la[0] != null && l[0] != null) {
                    l[2] = posAutocompletion(l[0], l[1], l[2]);
                } else if (h == 2 && lb != null && lb[0] != null && l[0] != null) {
                    l[1] = posAutocompletion(l[0], l[2], l[1]);
                }

                posData.p.replace(n, l);
            }

            if (h == 0 && lb != null && lb[0] != null && lb[1] != null && lb[2] == null) {
                lb[2] = posAutocompletion(lb[0], lb[1], null);
                posData.p.replace(n - 1, lb);
            }

            posData.world = world;
            POS_MAP.replace(uuid, posData);
        } else {
            POS_MAP.put(uuid, new PosData(world, location, n, h));
        }

        player.printInfo(TextComponent.of(getMessage("messages.pos-set", posToString(n, h), location)));
    }

    private static Vector3 posAutocompletion(@NotNull Vector3 h0, @NotNull Vector3 hIN, Vector3 hOUT){
        double x = hIN.getX() - h0.getX();
        double y = hIN.getY() - h0.getY();
        double z = hIN.getZ() - h0.getZ();

        if (hOUT == null) {
            return h0.add(-x, -y, -z);
        } else {
            double[] newH = changeR(x, y, z, -lineLength(h0, hOUT));
            return h0.add(newH[0], newH[1], newH[2]);
        }
    }

    /**
     * 指定されたプレイヤーのposをposListから削除する。
     *
     * @param player posを削除するプレイヤー
     */
    public static void clearPos(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        PosData posData = POS_MAP.get(uuid);
        if (posData != null) {
            posData.p = new TreeMap<>();
            posData.world = null;
            POS_MAP.replace(uuid, posData);
            player.printInfo(TextComponent.of(getMessage("messages.pos-clear-all")));
        }
    }

    /**
     * 指定されたプレイヤーのposをposListから削除する。
     *
     * @param  player posを削除するプレイヤー
     * @param  n posの番号
     * @param  h posの種類
     * @throws AssertionError hが0以上2以下ではない場合
     */
    public static void clearPos(@NotNull Player player, int n, int h) {
        assert (h >= 0) && (h <= 2) : "h must be in the range 0 to 2.";

        UUID uuid = player.getUniqueId();
        PosData posData = POS_MAP.get(uuid);

        if (posData == null) return;
        Vector3[] l = posData.p.get(n);
        if (l == null) return;

        if (h == 0) {
            l = new Vector3[3];
        } else {
            l[h] = null;
        }

        posData.p.replace(n, l);
        POS_MAP.replace(uuid, posData);

        player.printInfo(TextComponent.of(getMessage("messages.pos-clear", posToString(n, h))));
    }

    /**
     * 指定されたプレイヤーのposをposListから取得する。
     *
     * @param player posを取得するプレイヤー
     * @return posListにプレイヤーのデータがあればプレイヤーのposを返す<br>無ければnullを返す
     */
    public static @Nullable NavigableMap<Integer, Vector3[]> getPos(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        PosData posData = POS_MAP.get(uuid);
        if (posData != null) {
            return posData.p;
        } else {
            return null;
        }
    }

    /**
     * 指定されたプレイヤーのposが保存されているワールドをposListから取得する。
     * @param player Worldを取得するプレイヤー
     * @return posListにプレイヤーのデータがあればプレイヤーのWorldを返す<br>無ければnullを返す
     */
    public static @Nullable World getWorld(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        PosData posData = POS_MAP.get(uuid);
        if (posData != null) {
            return posData.world;
        }
        return null;
    }

    /**
     * posListを取得する。
     *
     * @return posList
     */
    public static Map<UUID, PosData> getPosMap() {
        return POS_MAP;
    }

    private static @NotNull String posToString(int n, int h) {
        return switch (h) {
            case 1 -> getMessage("messages.pos-Nf", n);
            case 2 -> getMessage("messages.pos-Nb", n);
            default -> getMessage("messages.pos-N", n);
        };
    }
}