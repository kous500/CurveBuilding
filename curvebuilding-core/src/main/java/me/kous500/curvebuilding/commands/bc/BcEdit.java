package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;

import java.util.*;

import static me.kous500.curvebuilding.CurveBuilding.*;
import static me.kous500.curvebuilding.WorldeditAdapter.*;
import static me.kous500.curvebuilding.math.PosData.*;
import static me.kous500.curvebuilding.Util.*;

public final class BcEdit {
    private int width;
    private int length;
    private int height;
    private int nowLength = 0;
    private int changedBlocks = 0;
    private int maxChangeLimit;
    private Vector3 center;
    private Direction direction;
    private EditSession editSession;
    private final BcCommand argument;
    private final  NavigableMap<BlockVector3, NavigableMap<BlockVector3, Integer>> blockMap;

    private enum Direction {x, z}

    /**
     * 設定したposからベジエ曲線をブロックで生成する。
     *
     * @param player コマンドを実行したプレイヤー
     * @param argument bcコマンドの引数
     */
    public BcEdit(Player player, BcCommand argument) {
        this.argument = argument;
        blockMap = new TreeMap<>();

        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        LocalSession session = manager.get(player);

        World world = session.getSelectionWorld();

        try (EditSession editSession = session.createEditSession(player)) {
            this.editSession = editSession;

            Region region = session.getSelection(session.getSelectionWorld());
            width = region.getWidth();
            length = region.getLength();
            height = region.getHeight();
            center = adapt(region.getCenter());

            if (argument.isDirectionX) direction = Direction.x;
            else if (argument.isDirectionZ) direction = Direction.z;
            else if (width <= length) direction = Direction.x;
            else direction = Direction.z;

            NavigableMap<Integer, Vector3[]> posMap = getPos(player);
            World posWorld = getWorld(player);

            if (posMap == null || posMap.get(1) == null || posMap.get(1)[0] == null) throw new IncompletePosException();

            if (posWorld == null || !posWorld.equals(world)) throw new IncompleteRegionException();

            maxChangeLimit = session.getBlockChangeLimit();
            int configLimit = config.defaultMaxChangeLimit;
            if (configLimit >= 0 && (maxChangeLimit < 0 || maxChangeLimit > configLimit)) {
                maxChangeLimit = configLimit;
                if (!fawe) session.setBlockChangeLimit(configLimit);
            }
            if (0 < maxChangeLimit && maxChangeLimit < region.getVolume()) {
                throw new MaxChangedBlocksException (maxChangeLimit);
            }

            RegionBlocks regionBlocks = new RegionBlocks(editSession, region);

            for (int n = 1; n <= posMap.lastEntry().getKey(); n++) {
                Vector3[] p = posMap.get(n);

                if (p == null || p[0] == null) break;

                if (n > 1) {
                    Vector3[] bp = posMap.get(n - 1);
                    Vector3[] bezierPos = new Vector3[] {
                            bp[0],
                            bp[2] != null ? bp[2] : bp[0],
                            p[1] != null ? p[1] : p[0],
                            p[0]
                    };
                    editBC(bezierPos);
                }
            }

            for (Map.Entry<BlockVector3, NavigableMap<BlockVector3, Integer>> entry : blockMap.entrySet()) {
                BlockVector3 maxBlockVec = Collections.max(entry.getValue().entrySet(), Map.Entry.comparingByValue()).getKey();
                editSession.setBlock(adapt(entry.getKey()), regionBlocks.get(maxBlockVec));
            }

            player.printInfo(TextComponent.of(getMessage("messages.bc-changed", editSession.size())));
            player.printInfo(TextComponent.of(getMessage("messages.bc-length", (double) Math.round((float) nowLength * 100) / 100)));
        } catch (IncompleteRegionException e) {
            player.printError(TextComponent.of(getMessage("messages.incomplete-region")));
        } catch (IncompletePosException e) {
            player.printError(TextComponent.of(getMessage("messages.incomplete-pos")));
        } catch (MaxChangedBlocksException e) {
            player.printError(TextComponent.of(getMessage("messages.max-changed-blocks", maxChangeLimit)));
        } catch (MaxSetLengthException e) {
            player.printError(TextComponent.of(getMessage("messages.max-set-length", config.maxSetLength)));
        } finally {
            session.remember(editSession);
        }
    }

    private void editBC(Vector3[] selectionPos) throws MaxChangedBlocksException, MaxSetLengthException {
        if (selectionPos[0].distance(selectionPos[3]) > config.maxSetLength) throw new MaxSetLengthException();
        if (selectionPos[0].distance(selectionPos[1]) > config.maxSetLength) throw new MaxSetLengthException();
        if (selectionPos[3].distance(selectionPos[2]) > config.maxSetLength) throw new MaxSetLengthException();

        double selectionLength = bezierLength(selectionPos, selectionPos[0].distance(selectionPos[3]) * 20);
        double fineness = config.fineness * selectionLength;
        int length = nowLength + (int) bezierLength(selectionPos, fineness);
        double h = Math.floor(height / 2.0 - 0.5);

        if (length > config.maxSetLength) throw new MaxSetLengthException();

        for (int m = 0; m <= height - 1; m++) {
            Vector3 vec = center.add(0, m - h, 0);
            xz(selectionPos, m, fineness, vec);
        }

        nowLength = length;
    }
	
    private void xz(Vector3[] selectionPos, int m, double fineness, Vector3 vec) throws MaxChangedBlocksException {
        Vector3 posA = vec;
        Vector3 posB = vec;
        int n = argument.n;

        if (direction == Direction.x) {
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
    }
	
    private void set(Vector3[] selectionPos, int l, int m, int n, double fineness, Vector3 searchT) throws MaxChangedBlocksException {
        double xt1 = selectionPos[0].x();
        double yt1 = selectionPos[0].y();
        double zt1 = selectionPos[0].z();

        int L1 = argument.m;
        if (nowLength != 0 && n != 0) L1 += nowLength % n;

        double s = 0;
        double L = 0;
        BlockVector3 beforePosT = null;

        while (s <= 1) {
            PosCoordinates pos = pos(selectionPos, s);
            double xt = pos.xt;
            double yt = pos.yt;
            double zt = pos.zt;
            double r = pos.r;

            //通過距離近似計算
            L += Math.sqrt((xt - xt1) * (xt - xt1) + (yt - yt1) * (yt - yt1) + (zt - zt1) * (zt - zt1));
            xt1 = xt;
            yt1 = yt;
            zt1 = zt;

            BlockVector3 posT = BlockVector3.round(Vector3.at(xt, yt, zt).add(l * Math.cos(-r), m, l * Math.sin(-r)));

            if (!posT.equals(beforePosT) && L >= L1) {
                BlockVector3 blockCoordinate;
                if (direction == Direction.x) {
                    double a = floorE((((L + nowLength) % width) - (width / 2.0)) * 2, 0.01) / 2 + 0.5;
                    blockCoordinate = BlockVector3.floor(searchT.add(a, 0, 0));
                } else {
                    double a = floorE((((L + nowLength) % length) - (length / 2.0)) * 2, 0.01) / 2 + 0.5;
                    blockCoordinate = BlockVector3.floor(searchT.add(0 , 0, a));
                }

                if (!argument.air || editSession.getBlock(adapt(posT)).toString().equals("minecraft:air")) {
                    blockMap.computeIfAbsent(posT, k -> new TreeMap<>());
                    var nowBlock = blockMap.get(posT);
                    nowBlock.putIfAbsent(blockCoordinate, 0);
                    nowBlock.put(blockCoordinate, nowBlock.get(blockCoordinate) + 1);

                    changedBlocks++;
                }

                if (0 < maxChangeLimit && maxChangeLimit < changedBlocks) {
                    throw new MaxChangedBlocksException(maxChangeLimit);
                }

                L1 += n;
            }

            s += 1.0 / fineness;
            beforePosT = posT;
        }
    }

    private PosCoordinates pos(Vector3[] selectionPos, double t) {
        if (t == 0) t = 0.00001;

        double x0 = selectionPos[0].x();
        double y0 = selectionPos[0].y();
        double z0 = selectionPos[0].z();

        double x1 = selectionPos[1].x();
        double y1 = selectionPos[1].y();
        double z1 = selectionPos[1].z();

        double x2 = selectionPos[2].x();
        double y2 = selectionPos[2].y();
        double z2 = selectionPos[2].z();

        double x3 = selectionPos[3].x();
        double y3 = selectionPos[3].y();
        double z3 = selectionPos[3].z();

        double nt = 1 - t;
        double bernstein3_0 = nt * nt * nt;
        double bernstein3_1 = 3 * nt * nt * t;
        double bernstein3_2 = 3 * nt * t * t;
        double bernstein3_3 = t * t * t;

        double xt = bernstein3_0 * x0 + bernstein3_1 * x1 + bernstein3_2 * x2 + bernstein3_3 * x3;
        double yt = bernstein3_0 * y0 + bernstein3_1 * y1 + bernstein3_2 * y2 + bernstein3_3 * y3;
        double zt = bernstein3_0 * z0 + bernstein3_1 * z1 + bernstein3_2 * z2 + bernstein3_3 * z3;

        double dxt = 3 * t * t * (-x0 + 3 * x1 - 3 * x2 + x3) + 6 * t * (x0 - 2 * x1 + x2) + 3 * (-x0 + x1);
        double dzt = 3 * t * t * (-z0 + 3 * z1 - 3 * z2 + z3) + 6 * t * (z0 - 2 * z1 + z2) + 3 * (-z0 + z1);
        double r = Math.atan(dxt / dzt);

        if (Double.isNaN(r)) {
            if (dxt > 0) r = Math.PI / 2;
            else if (dxt < 0) r = -Math.PI / 2;
            else r = 0;
        }

        if (dzt < 0) r += Math.PI;

        return new PosCoordinates(xt, yt, zt, r);
    }

    private static class PosCoordinates {
        double xt;
        double yt;
        double zt;
        double r;

        PosCoordinates(double xt, double yt, double zt, double r) {
            this.xt = xt;
            this.yt = yt;
            this.zt = zt;
            this.r = r;
        }
    }

    /**
     * Posが完全に定義されていない場合に発生します。
     */
    static class IncompletePosException extends Throwable {}

    /**
     * 曲線の長さがmax-set-lengthを超えた場合に発生します。
     */
    static class MaxSetLengthException extends Throwable {}
}