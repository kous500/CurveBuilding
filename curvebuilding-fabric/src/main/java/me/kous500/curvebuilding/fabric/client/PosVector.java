package me.kous500.curvebuilding.fabric.client;

import com.sk89q.worldedit.math.Vector3;

import java.awt.*;
import java.util.NavigableMap;

import static me.kous500.curvebuilding.fabric.FabricCurveBuilding.fabricConfig;

public class PosVector {
    public static PosVector getInstance(NavigableMap<Integer, Vector3[]> p, int n, int h) {
        if (p == null || n <= 0 || h < 0 || h > 2) return null;

        Vector3[] vector3s = p.get(n);
        if (vector3s == null || vector3s.length != 3 || vector3s[h] == null) return null;

        return new PosVector(p, n, h);
    }

    public final Vector3 vector;
    public final boolean isStartPos;
    public final boolean isEndPos;
    public final int n;
    public final int h;

    private PosVector(NavigableMap<Integer, Vector3[]> p, int n, int h) {
        vector = p.get(n)[h];
        isStartPos = n == 1;
        isEndPos = n == p.lastEntry().getKey();
        this.n = n;
        this.h = h;
    }

    public Color getLineColor() {
        if (h == 1) {
            return new Color(fabricConfig.posFLineColor, true);
        } else if (h == 2) {
            return new Color(fabricConfig.posBLineColor, true);
        } else {
            if (isStartPos) {
                return new Color(fabricConfig.posStartLineColor, true);
            } else if (isEndPos) {
                return new Color(fabricConfig.posEndLineColor, true);
            } else {
                return new Color(fabricConfig.posLineColor, true);
            }
        }
    }

    public Color getFillColor() {
        if (h == 1) {
            return new Color(fabricConfig.posFFilledColor, true);
        } else if (h == 2) {
            return new Color(fabricConfig.posBFilledColor, true);
        } else {
            if (isStartPos) {
                return new Color(fabricConfig.posStartFilledColor, true);
            } else if (isEndPos) {
                return new Color(fabricConfig.posEndFilledColor, true);
            } else {
                return new Color(fabricConfig.posFilledColor, true);
            }
        }
    }
}
