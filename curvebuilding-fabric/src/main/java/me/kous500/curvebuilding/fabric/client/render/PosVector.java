package me.kous500.curvebuilding.fabric.client.render;

import me.kous500.curvebuilding.math.CurvePoint;
import me.kous500.curvebuilding.math.Vector3;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.NavigableMap;

import static me.kous500.curvebuilding.fabric.CurveBuildingFabric.fabricConfig;

/**
 * A point or a handle to draw in the preview, with what decides its colors.
 */
public class PosVector {
    /**
     * The part {@code part} of point {@code n}, to draw.
     *
     * @param p the points by their number
     * @param n the point number
     * @param part the point or one of its handles
     * @return the part to draw, or null when it is not set
     */
    public static @Nullable PosVector getInstance(NavigableMap<Integer, CurvePoint> p, int n, CurvePoint.Part part) {
        if (n <= 0) return null;

        CurvePoint point = p.get(n);
        Vector3 vector = point != null ? point.get(part) : null;
        if (vector == null) return null;

        return new PosVector(vector, n, part, n == p.lastKey());
    }

    public final Vector3 vector;
    public final boolean isStartPos;
    public final boolean isEndPos;
    public final int n;
    public final CurvePoint.Part part;

    private PosVector(Vector3 vector, int n, CurvePoint.Part part, boolean isEndPos) {
        this.vector = vector;
        isStartPos = n == 1;
        this.isEndPos = isEndPos;
        this.n = n;
        this.part = part;
    }

    /**
     * The color of the outline, from the config: by handle, or for a point by whether it starts or ends the curve.
     *
     * @return the color
     */
    public Color getLineColor() {
        if (part == CurvePoint.Part.F) {
            return fabricConfig.posFLineColor;
        } else if (part == CurvePoint.Part.B) {
            return fabricConfig.posBLineColor;
        } else {
            if (isStartPos) {
                return fabricConfig.posStartLineColor;
            } else if (isEndPos) {
                return fabricConfig.posEndLineColor;
            } else {
                return fabricConfig.posLineColor;
            }
        }
    }

    /**
     * The color that fills the block, from the config: by handle, or for a point by whether it starts or ends the
     * curve.
     *
     * @return the color
     */
    public Color getFillColor() {
        if (part == CurvePoint.Part.F) {
            return fabricConfig.posFFilledColor;
        } else if (part == CurvePoint.Part.B) {
            return fabricConfig.posBFilledColor;
        } else {
            if (isStartPos) {
                return fabricConfig.posStartFilledColor;
            } else if (isEndPos) {
                return fabricConfig.posEndFilledColor;
            } else {
                return fabricConfig.posFilledColor;
            }
        }
    }
}
