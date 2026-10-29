package me.kous500.curvebuilding.fabric.client.render;

import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.math.Vector3;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.NavigableMap;

import static me.kous500.curvebuilding.Util.*;
import static me.kous500.curvebuilding.fabric.CurveBuildingFabric.fabricConfig;

public class RenderPreview {
    public static PosData posData;

    /**
     * クライアントのPosDataからプレビューを描画します
     */
    public static void render(PreviewFrame frame) {
        if (posData == null) return;

        PreviewLayer lines = frame.layer(fabricConfig.lineRenderThroughWalls);
        renderBezier(posData.getMap(), fabricConfig.lineCurveColor, lines);
        for (int n : posData.getMap().keySet()) {
            renderControlLine(posData.getMap().get(n), lines);
        }

        PreviewLayer points = frame.layer(fabricConfig.posRenderThroughWalls);
        for (int n : posData.getMap().keySet()) {
            for (int h : new int[]{0, 1, 2}) {
                PosVector pos = PosVector.getInstance(posData.getMap(), n, h);
                if (pos != null) {
                    renderPos(pos, points);
                }
            }
        }
    }

    private static void renderBezier(NavigableMap<Integer, Vector3[]> pos, Color color, PreviewLayer render) {
        double totalLen = 0;
        for (Vector3[] segment : bezierSegments(pos)) {
            if (!isBezierSegmentWithin(segment, fabricConfig.lineRenderLength)) return;

            double length = bezierLength(segment, bezierFineness(segment, 5));
            totalLen += length;
            if (totalLen > fabricConfig.lineRenderLength) return;

            Vec3 bc = null;
            for (Vector3 point : bezierPoints(segment, Math.max(1, (int) Math.ceil(length * fabricConfig.lineRenderAccuracy)))) {
                Vec3 c = adaptVec(point);
                if (bc != null) render.addLine(color, bc, c);
                bc = c;
            }
        }
    }

    private static void renderControlLine(Vector3[] p, PreviewLayer render) {
        if (p == null || p[0] == null) return;

        if (p[1] != null && p[0].distance(p[1]) <= fabricConfig.lineRenderLength) {
            render.addLine(
                    fabricConfig.lineFColor,
                    adaptVec(p[0]).add(0.5, 0.5, 0.5),
                    adaptVec(p[1]).add(0.5, 0.5, 0.5)
            );
        }

        if (p[2] != null && p[0].distance(p[2]) <= fabricConfig.lineRenderLength) {
            render.addLine(
                    fabricConfig.lineBColor,
                    adaptVec(p[0]).add(0.5, 0.5, 0.5),
                    adaptVec(p[2]).add(0.5, 0.5, 0.5)
            );
        }
    }

    private static void renderPos(PosVector pos, PreviewLayer render) {
        Color colorLine = pos.getLineColor();
        Vec3 block = adaptVec(pos.vector);

        if (pos.h != 0) {
            render.addBlockCrossing(colorLine, block);
        }

        render.addBlockOutline(colorLine, block);
        render.addFilledBlock(pos.getFillColor(), block);
    }

    private static Vec3 adaptVec(Vector3 vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }
}
