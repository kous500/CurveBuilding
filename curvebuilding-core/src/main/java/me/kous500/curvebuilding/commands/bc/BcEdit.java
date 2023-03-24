package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.kous500.curvebuilding.CurveBuilding.config;
import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.PosData.*;
import static me.kous500.curvebuilding.Util.*;

public final class BcEdit {
    private int width;
    private int length;
    private int height;
    private int nowLength = 0;
    private Vector3 center;
    private String direction;
    private List<BlockVector3> notSet = new ArrayList<>();
    private final BcCommand argument;
    private EditSession editSession;

    /**
     * 設定したposからベジエ曲線または直線をブロックで生成する。
     *
     * @param player コマンドを実行したプレイヤー
     * @param argument bcコマンドの引数
     */
    public BcEdit(Player player, @NotNull BcCommand argument) {
        this.argument = argument;

        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        LocalSession session = manager.get(player);

        World world = session.getSelectionWorld();

        try (EditSession editSession = session.createEditSession(player)) {
            this.editSession = editSession;
            try {
                //FAWEの場合は実行されない
                editSession.setBlockChangeLimit(session.getBlockChangeLimit());
            } catch (NoSuchMethodError ignored) {}

            Region region = session.getSelection(session.getSelectionWorld());
            width = region.getWidth();
            length = region.getLength();
            height = region.getHeight();
            center = region.getCenter();

            if (argument.isDirectionX) direction = "x";
            else if (argument.isDirectionZ) direction = "z";
            else if (width <= length) direction = "x";
            else direction = "z";

            NavigableMap<Integer, Vector3[]> posMap = getPos(player);
            World posWorld = getWorld(player);

            if (posMap == null || posMap.get(1) == null || posMap.get(1)[0] == null) throw new IncompletePosException();

            if (posWorld == null || !posWorld.equals(world)) throw new IncompleteRegionException();

            for (int n = 1; n <= posMap.lastEntry().getKey(); n++) {
                Vector3[] p = posMap.get(n);

                if (p == null) break;

                if (n > 1) {
                    Vector3[] bp = posMap.get(n - 1);
                    Vector3[] bezierPos = new Vector3[] {copyVector(bp[0]), copyVector(bp[2]), copyVector(p[1]), copyVector(p[0])};
                    if (bezierPos[1] == null) bezierPos[1] = bezierPos[0];
                    if (bezierPos[2] == null)  bezierPos[2] =  bezierPos[0];
                    editBC(bezierPos);
                }
            }

            player.printInfo(TextComponent.of(getMessage("messages.bc-changed", editSession.size())));
            player.printInfo(TextComponent.of(getMessage("messages.bc-length", (double) Math.round(nowLength * 100) / 100)));
        } catch (IncompleteRegionException e) {
            player.printError(TextComponent.of(getMessage("messages.incomplete-region")));
        } catch (IncompletePosException e) {
            player.printError(TextComponent.of(getMessage("messages.incomplete-pos")));
        } catch (MaxChangedBlocksException e) {
            player.printError(TextComponent.of(getMessage("messages.max-changed-blocks", session.getBlockChangeLimit())));
        } finally {
            session.remember(editSession);
        }
    }

    private void editBC(Vector3[] selectionPos) throws MaxChangedBlocksException {
        double selectionLength = bezierLength(selectionPos, selectionPos[0].distance(selectionPos[3]) * 20);
        double fineness = config.fineness * selectionLength;
        double h = Math.floor(height / 2.0 - 0.5);
        notSet = new ArrayList<>();

        xz(selectionPos, height, fineness, center.add(0, height - h, 0));

        for (int m = 0; m <= height - 1; m++) {
            Vector3 vec = center.add(0, m - h, 0);
            xz(selectionPos, m, fineness, vec);
        }

        nowLength += (int) bezierLength(selectionPos, fineness);
    }

    private @NotNull Map<String, Double> pos(Vector3 @NotNull [] selectionPos, double t) {
        double x0 = selectionPos[0].getX();
        double y0 = selectionPos[0].getY();
        double z0 = selectionPos[0].getZ();

        double x1 = selectionPos[1].getX();
        double y1 = selectionPos[1].getY();
        double z1 = selectionPos[1].getZ();

        double x2 = selectionPos[2].getX();
        double y2 = selectionPos[2].getY();
        double z2 = selectionPos[2].getZ();

        double x3 = selectionPos[3].getX();
        double y3 = selectionPos[3].getY();
        double z3 = selectionPos[3].getZ();

        double xt = (1-t)*(1-t)*(1-t)*x0 + 3*(1-t)*(1-t)*t*x1 + 3*(1-t)*t*t*x2 + t*t*t*x3;
        double yt = (1-t)*(1-t)*(1-t)*y0 + 3*(1-t)*(1-t)*t*y1 + 3*(1-t)*t*t*y2 + t*t*t*y3;
        double zt = (1-t)*(1-t)*(1-t)*z0 + 3*(1-t)*(1-t)*t*z1 + 3*(1-t)*t*t*z2 + t*t*t*z3;
        double dxt = 3 * t * t * (-x0 + 3 * x1 - 3 * x2 + x3) + 6 * t * (x0 - 2 * x1 + x2) + 3 * (-x0 + x1);
        //double dyt = 3*t*t*(-y0+3*y1-3*y2+y3) + 6*t*(y0-2*y1+y2) + 3*(-y0+y1);
        double dzt = 3 * t * t * (-z0 + 3 * z1 - 3 * z2 + z3) + 6 * t * (z0 - 2 * z1 + z2) + 3 * (-z0 + z1);
        double r = Math.atan(dxt / dzt);

        if (argument.rtm) {
            yt = (1-t)*y0 + t*y3 - 0.3;
        }

        Map<String, Double> values = new HashMap<>();
        values.put("xt", xt);
        values.put("yt", yt);
        values.put("zt", zt);
        values.put("r", r);
        return values;
    }

    private void set(Vector3 @NotNull [] selectionPos, int l, int m, int n, double fineness, Vector3 searchT) throws MaxChangedBlocksException {
        double xt1 = selectionPos[0].getX();
        double yt1 = selectionPos[0].getY();
        double zt1 = selectionPos[0].getZ();

        int L1 = argument.m;
        if (nowLength != 0 && n != 0) L1 += nowLength % n;

        double s = 0;
        double L = 0;
        BlockVector3 beforePosT = null;

        while (s <= 1) {
            Map<String, Double> pos = pos(selectionPos, s);
            double xt = pos.get("xt");
            double yt = pos.get("yt");
            double zt = pos.get("zt");
            double r = pos.get("r");

            //通過距離近似計算
            L += Math.sqrt((xt - xt1)*(xt - xt1) + (yt - yt1)*(yt - yt1) + (zt - zt1)*(zt - zt1));
            xt1 = xt;
            yt1 = yt;
            zt1 = zt;

            BaseBlock idT;
            if (direction.equals("x")) {
                double a = floorE((((L + nowLength) % width) - (width / 2.0)) * 2, 0.01) / 2 + 0.5;
                idT = editSession.getFullBlock(floorVector(searchT.add(a, 0, 0)));
            } else {
                double a = floorE((((L + nowLength) % length) - (length / 2.0)) * 2, 0.01) / 2 + 0.5;
                idT = editSession.getFullBlock(floorVector(searchT.add(0 , 0, a)));
            }

            Vector3 vecPosT = Vector3.at(xt, yt, zt);
            BlockVector3 posT = roundVector(vecPosT.add(l*Math.cos(-r), m, l*Math.sin(-r)));

            BlockVector3 afterPosT = null;
            if (L >= L1) {
                Map<String, Double> aPos = pos(selectionPos, s + 1.0 / fineness);
                double axt = aPos.get("xt");
                double ayt = aPos.get("yt");
                double azt = aPos.get("zt");
                double ar = aPos.get("r");
                Vector3 vecAfterPosT = Vector3.at(axt, ayt, azt);
                afterPosT = roundVector(vecAfterPosT.add(l*Math.cos(-ar), m, l*Math.sin(-ar)));
            }

            if (L >= L1 ||
                    ((beforePosT != null && beforePosT.getX() == posT.getX() && beforePosT.getY() != posT.getY() && beforePosT.getZ() == posT.getZ()) ||
                    (afterPosT != null && afterPosT.getX() == posT.getX() && afterPosT.getY() != posT.getY() && afterPosT.getZ() == posT.getZ()))) {
                if (m == height) {
                    notSet.add(posT);
                } else if (m != height -1 || !notSet.contains(posT)) {
                    if (argument.air) {
                        String selectionBlock = editSession.getBlock(posT).toString();
                        if (selectionBlock.equals("minecraft:air")) editSession.setBlock(posT, idT);
                    } else {
                        editSession.setBlock(posT, idT);
                    }
                }

                L1 += n;
            }

            s += 1.0 / fineness;
            beforePosT = posT;
        }
    }

    private void xz(Vector3[] selectionPos, int m, double fineness, Vector3 vec) throws MaxChangedBlocksException {
        Vector3 posA = vec;
        Vector3 posB = vec;
        int n = argument.n;

        if (direction.equals("x")) {
            for (int l = 0; l <= length / 2; l++) {
                if (l != length / 2 || length % 2 != 0) {
                    set(selectionPos, l, m, n, fineness, posA);
                    posA = posA.add(0, 0, -1);
                }

                set(selectionPos, -l, m, n, fineness, posB);
                posB = posB.add(0, 0, 1);
            }
        } else {
            for (int l = 0; l <= width / 2; l++) {
                set(selectionPos, l, m, n, fineness, posA);
                posA = posA.add(1, 0, 0);

                if (l != width / 2 || width % 2 != 0) {
                    set(selectionPos, -l, m, n, fineness, posB);
                    posB = posB.add(-1, 0, 0);
                }
            }
        }

        //中心を再設置
        if (argument.n == 0 && !config.tCenter) n = 1;
        set(selectionPos, 0, m, n, fineness, vec);
    }
}