package me.kous500.curvebuilding.fabric.client.render;


import me.kous500.curvebuilding.math.CurvePoint;
import me.kous500.curvebuilding.math.Vector3;

import java.awt.*;
import java.util.NavigableMap;

import static me.kous500.curvebuilding.fabric.CurveBuildingFabric.fabricConfig;

public class PosVector {
    public static PosVector getInstance(NavigableMap<Integer, CurvePoint> p, int n, CurvePoint.Part part) {
        if (p == null || n <= 0) return null;

        CurvePoint point = p.get(n);
        if (point == null || point.get(part) == null) return null;

        return new PosVector(p, n, part);
    }

    public final Vector3 vector;
    public final boolean isStartPos;
    public final boolean isEndPos;
    public final int n;
    public final CurvePoint.Part part;

    private PosVector(NavigableMap<Integer, CurvePoint> p, int n, CurvePoint.Part part) {
        vector = p.get(n).get(part);
        isStartPos = n == 1;
        isEndPos = n == p.lastEntry().getKey();
        this.n = n;
        this.part = part;
    }

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
