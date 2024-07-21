package me.kous500.curvebuilding.fabric.client.render;

import com.sk89q.worldedit.math.Vector3;
import me.kous500.curvebuilding.math.PosData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.NavigableMap;

import static me.kous500.curvebuilding.Util.*;
import static me.kous500.curvebuilding.fabric.CurveBuildingFabric.fabricConfig;

public class RenderPreview {
    public static PosData posData;
    private static boolean isError = false;
    private static ArrayDeque<RenderFilledItem> filledQueue = new ArrayDeque<>();
    private static final double INCREASE = 0.001;
    private static final Vec3d INCREASE_VEC = new Vec3d(INCREASE, INCREASE, INCREASE);

    /**
     * クライアントのPosDataからプレビューを描画します
     *
     * @param matrix MatrixStack
     */
    public static void RenderPosData(MatrixStack matrix) {
        if (isError || posData == null) return;

        try {
            Render.setRender(fabricConfig.lineRenderThroughWalls, LineRender.getInstance(), render -> {
                renderBezier(matrix, posData.p, fabricConfig.lineCurveColor, render);

                for (int n : posData.p.keySet()) {
                    renderControlLine(matrix, posData.p.get(n), render);
                }
            });

            Render.setRender(fabricConfig.posRenderThroughWalls, LineRender.getInstance(), render -> {
                for (int n : posData.p.keySet()) {
                    for (int h : new int[]{0, 1, 2}) {
                        PosVector pos = PosVector.getInstance(posData.p, n, h);
                        if (pos != null) {
                            renderPos(matrix, pos, render);
                        }
                    }
                }
            });

            Render.setRender(fabricConfig.posRenderThroughWalls, FilledRender.getInstance(), render -> {
                for (RenderFilledItem renderItem : filledQueue) {
                    render.renderFilled(matrix, renderItem.color, renderItem.start, renderItem.dimensions);
                }

                filledQueue = new ArrayDeque<>();
            });
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            isError = true;
            e.printStackTrace();
        }
    }

    private static void renderBezier(MatrixStack matrices, NavigableMap<Integer, Vector3[]> pos, Color color, LineRender render) {
        if (pos == null) return;

        double totalLen = 0;
        for (int n : pos.keySet()) {
            if (n > 1) {
                totalLen = bezierBuild(
                        matrices,
                        pos.get(n),
                        pos.get(n - 1),
                        color,
                        totalLen,
                        render
                );
            }
            if (totalLen > fabricConfig.lineRenderLength) return;
        }
    }

    private static double bezierBuild(MatrixStack matrices, Vector3[] p, Vector3[] bp, Color color, double totalLen, LineRender render) {
        if (p == null || p[0] == null || bp == null || bp[0] == null) return fabricConfig.lineRenderLength + 1;

        Vector3[] bezierPos = new Vector3[] {copyVector(bp[0]), copyVector(bp[2]), copyVector(p[1]), copyVector(p[0])};
        if (bezierPos[1] == null) bezierPos[1] = bezierPos[0];
        if (bezierPos[2] == null) bezierPos[2] = bezierPos[3];

        if (bezierPos[0].distance(bezierPos[3]) > fabricConfig.lineRenderLength) return fabricConfig.lineRenderLength + 1;
        if (bezierPos[0].distance(bezierPos[1]) > fabricConfig.lineRenderLength) return fabricConfig.lineRenderLength + 1;
        if (bezierPos[3].distance(bezierPos[2]) > fabricConfig.lineRenderLength) return fabricConfig.lineRenderLength + 1;

        double length = bezierLength(bezierPos, bezierPos[0].distance(bezierPos[3]) * 5);
        totalLen += length;
        if (totalLen > fabricConfig.lineRenderLength) return totalLen;

        Vec3d bc = null;
        for (double i = 0; i <= 1; i += 1.0 / (length * fabricConfig.lineRenderAccuracy)) {
            Vec3d c = adaptVec(bezierCoordinate(bezierPos, i));
            if (bc != null) render.addLine(matrices, color, c, bc);
            bc = c;
        }

        render.addLine(matrices, color, bc, adaptVec(bezierPos[3].add(0.5, 0.5, 0.5)));

        return totalLen;
    }

    private static void renderControlLine(MatrixStack matrix, Vector3[] p, LineRender render) {
        if (p == null || p[0] == null) return;

        if (p[1] != null && p[0].distance(p[1]) <= fabricConfig.lineRenderLength) {
            render.addLine(
                    matrix,
                    fabricConfig.lineFColor,
                    adaptVec(p[0]).add(0.5, 0.5, 0.5),
                    adaptVec(p[1]).add(0.5, 0.5, 0.5)
            );
        }

        if (p[2] != null && p[0].distance(p[2]) <= fabricConfig.lineRenderLength) {
            render.addLine(
                    matrix,
                    fabricConfig.lineBColor,
                    adaptVec(p[0]).add(0.5, 0.5, 0.5),
                    adaptVec(p[2]).add(0.5, 0.5, 0.5)
            );
        }
    }

    private static void renderPos(MatrixStack matrix, PosVector pos, LineRender render) {
        Color colorLine = pos.getLineColor();
        Color colorFill = pos.getFillColor();
        Vec3d start = adaptVec(pos.vector).subtract(INCREASE_VEC);
        Vec3d dimensions = new Vec3d(1, 1, 1).add(INCREASE_VEC).add(INCREASE_VEC);

        if (pos.h != 0) {
            render.addCrossing(matrix, colorLine, start, dimensions);
        }

        render.addOutline(matrix, colorLine, start, dimensions);
        filledQueue.add(new RenderFilledItem(colorFill, start, dimensions));
    }

    private static Vec3d adaptVec(Vector3 vec) {
        return new Vec3d(vec.getX(), vec.getY(), vec.getZ());
    }

    private static class RenderFilledItem {
        Color color;
        Vec3d start;
        Vec3d dimensions;

        RenderFilledItem(Color color, Vec3d start, Vec3d dimensions) {
            this.color = color;
            this.start = start;
            this.dimensions = dimensions;
        }
    }
}
