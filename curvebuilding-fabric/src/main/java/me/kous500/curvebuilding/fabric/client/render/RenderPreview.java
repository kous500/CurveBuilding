package me.kous500.curvebuilding.fabric.client.render;

import me.kous500.curvebuilding.curve.CurvePoint;
import me.kous500.curvebuilding.curve.PosData;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.preview.PreviewCurve;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.NavigableMap;

import static me.kous500.curvebuilding.fabric.CurveBuildingFabric.fabricConfig;

/**
 * Draws the curve, the points and the handles that the server sent last.
 */
public class RenderPreview {
    /**
     * The positions that the server sent last, set on the client thread, or null.
     */
    public static @Nullable PosData posData;

    /**
     * Draws the preview of the positions the server sent last.
     *
     * @param frame the frame to add the shapes to
     */
    public static void render(PreviewFrame frame) {
        PosData shown = posData;
        if (shown == null) return;
        NavigableMap<Integer, CurvePoint> positions = shown.points();

        PreviewLayer lines = frame.layer(fabricConfig.lineRenderThroughWalls);
        renderBezier(positions, fabricConfig.lineCurveColor, lines);
        for (CurvePoint point : positions.values()) {
            renderControlLine(point, lines);
        }

        PreviewLayer points = frame.layer(fabricConfig.posRenderThroughWalls);
        for (int n : positions.keySet()) {
            for (CurvePoint.Part part : CurvePoint.Part.values()) {
                PosVector pos = PosVector.getInstance(positions, n, part);
                if (pos != null) {
                    renderPos(pos, points);
                }
            }
        }
    }

    private static void renderBezier(NavigableMap<Integer, CurvePoint> pos, Color color, PreviewLayer render) {
        for (PreviewCurve.ShownSegment shown : PreviewCurve.segments(pos, fabricConfig.lineRenderLength, 5)) {
            int divisions = Math.max(1, (int) Math.ceil(shown.length() * fabricConfig.lineRenderAccuracy));
            Vec3 bc = null;
            for (Vector3 point : shown.segment().blockCenters(divisions)) {
                Vec3 c = adaptVec(point);
                if (bc != null) render.addLine(color, bc, c);
                bc = c;
            }
        }
    }

    private static void renderControlLine(CurvePoint p, PreviewLayer render) {
        if (p.point() == null) return;

        if (p.f() != null && p.point().distance(p.f()) <= fabricConfig.lineRenderLength) {
            render.addLine(
                    fabricConfig.lineFColor,
                    adaptVec(p.point()).add(0.5, 0.5, 0.5),
                    adaptVec(p.f()).add(0.5, 0.5, 0.5)
            );
        }

        if (p.b() != null && p.point().distance(p.b()) <= fabricConfig.lineRenderLength) {
            render.addLine(
                    fabricConfig.lineBColor,
                    adaptVec(p.point()).add(0.5, 0.5, 0.5),
                    adaptVec(p.b()).add(0.5, 0.5, 0.5)
            );
        }
    }

    private static void renderPos(PosVector pos, PreviewLayer render) {
        Color colorLine = pos.getLineColor();
        Vec3 block = adaptVec(pos.vector);

        if (pos.part != CurvePoint.Part.POINT) {
            render.addBlockCrossing(colorLine, block);
        }

        render.addBlockOutline(colorLine, block);
        render.addFilledBlock(pos.getFillColor(), block);
    }

    private static Vec3 adaptVec(Vector3 vec) {
        return new Vec3(vec.x(), vec.y(), vec.z());
    }
}
