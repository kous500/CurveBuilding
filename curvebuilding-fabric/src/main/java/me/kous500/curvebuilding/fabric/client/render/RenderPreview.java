package me.kous500.curvebuilding.fabric.client.render;

import com.sk89q.worldedit.math.Vector3;
import me.kous500.curvebuilding.PosData;
import me.kous500.curvebuilding.fabric.client.PosVector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayDeque;

import static me.kous500.curvebuilding.fabric.FabricCurveBuilding.fabricConfig;
import static me.kous500.curvebuilding.fabric.client.render.Render.adaptVec;

public class RenderPreview {
    static boolean isError = false;
    static final double INCREASE = 0.001;
    static final Vec3d increaseVec = new Vec3d(INCREASE, INCREASE, INCREASE);
    private static final ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
    private static ArrayDeque<RenderFilledItem> filledQueue = new ArrayDeque<>();

    /**
     * クライアントのPosDataからプレビューを描画します
     *
     * @param matrix MatrixStack
     */
    public static void RenderPosData(MatrixStack matrix) {
        if (clientPlayer == null) return;

        PosData posData = PosData.getPosMap().get(clientPlayer.getUuid());

        if (posData == null) return;


        if (isError) return;
        try {
            Render.bezierRender(
                    matrix,
                    posData.p,
                    new Color(fabricConfig.lineCurveColor, true),
                    fabricConfig.lineRenderThroughWalls
            );

            for (int n : posData.p.keySet()) {
                renderControlLine(matrix, posData.p.get(n));

                for (int h : new int[]{0, 1, 2}) {
                    PosVector pos = PosVector.getInstance(posData.p, n, h);
                    if (pos != null) {
                        renderPos(matrix, pos);
                    }
                }
            }

            for (RenderFilledItem render : filledQueue) {
                Render.renderFilled(matrix, render.color, render.start, render.dimensions, fabricConfig.posRenderThroughWalls);
            }
            filledQueue = new ArrayDeque<>();
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            isError = true;
            e.printStackTrace();
        }
    }

    private static void renderControlLine(MatrixStack matrix, Vector3[] p) {
        if (p == null || p[0] == null) return;

        if (p[1] != null && p[0].distance(p[1]) <= fabricConfig.lineRenderLength) {
            Render.renderLine(
                    matrix,
                    new Color(fabricConfig.lineFColor, true),
                    adaptVec(p[0]).add(0.5, 0.5, 0.5),
                    adaptVec(p[1]).add(0.5, 0.5, 0.5),
                    fabricConfig.lineRenderThroughWalls
            );
        }

        if (p[2] != null && p[0].distance(p[2]) <= fabricConfig.lineRenderLength) {
            Render.renderLine(
                    matrix,
                    new Color(fabricConfig.lineBColor, true),
                    adaptVec(p[0]).add(0.5, 0.5, 0.5),
                    adaptVec(p[2]).add(0.5, 0.5, 0.5),
                    fabricConfig.lineRenderThroughWalls
            );
        }
    }

    private static void renderPos(MatrixStack matrix, PosVector pos) {
        Color colorLine = pos.getLineColor();
        Color colorFill = pos.getFillColor();
        Vec3d start = adaptVec(pos.vector).subtract(increaseVec);
        Vec3d dimensions = new Vec3d(1, 1, 1).add(increaseVec).add(increaseVec);

        if (pos.h != 0) {
            Render.renderCrossing(matrix, colorLine, start, dimensions, fabricConfig.posRenderThroughWalls);
        }

        Render.renderOutline(matrix, colorLine, start, dimensions, fabricConfig.posRenderThroughWalls);
        filledQueue.add(new RenderFilledItem(colorFill, start, dimensions));
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
