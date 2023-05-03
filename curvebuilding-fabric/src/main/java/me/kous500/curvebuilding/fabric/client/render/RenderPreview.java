package me.kous500.curvebuilding.fabric.client.render;

import com.sk89q.worldedit.math.Vector3;
import me.kous500.curvebuilding.PosData;
import me.kous500.curvebuilding.fabric.client.PosVector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

import static me.kous500.curvebuilding.Util.*;

public class RenderPreview {
    static final double INCREASE = 0.001;
    static final Vec3d increaseVec = new Vec3d(INCREASE, INCREASE, INCREASE);
    private static final ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;

    public static void RenderPosData(MatrixStack matrix) {
        if (clientPlayer == null) return;

        PosData posData = PosData.getPosMap().get(clientPlayer.getUuid());

        if (posData == null) return;

        for (int n : posData.p.keySet()) {
            Vector3[] posN = posData.p.get(n);
            if (posN != null && posN[0] != null) {
                Vector3[] posBeforeN = posData.p.get(n - 1);
                if (posBeforeN != null && posBeforeN[0] != null) {
                    Vector3[] bezierPos = new Vector3[] {copyVector(posBeforeN[0]), copyVector(posBeforeN[2]), copyVector(posN[1]), copyVector(posN[0])};
                    if (bezierPos[1] == null) bezierPos[1] = bezierPos[0];
                    if (bezierPos[2] == null) bezierPos[2] = bezierPos[3];
                    renderBezier(matrix, bezierPos);
                }

                if (posN[1] != null) {
                    Render.renderLine(
                            matrix,
                            new Color(0xFFFF0077, true),
                            adaptVec(posN[0]).add(0.5, 0.5, 0.5),
                            adaptVec(posN[1]).add(0.5, 0.5, 0.5)
                    );
                }
                if (posN[2] != null) {
                    Render.renderLine(
                            matrix,
                            new Color(0xFF7700FF, true),
                            adaptVec(posN[0]).add(0.5, 0.5, 0.5),
                            adaptVec(posN[2]).add(0.5, 0.5, 0.5)
                    );
                }
            }

            for (int h : new int[]{0, 1, 2}) {
                PosVector pos = PosVector.getInstance(posData.p, n, h);
                if (pos != null) {
                    renderPos(matrix, pos);
                }
            }
        }

        Render.renderStackFilled(matrix);
    }

    private static void renderBezier(MatrixStack matrix, Vector3[] p) {
        if (p[0] == null || p[1] == null || p[2] == null || p[3] == null) return;

        double length = bezierLength(p, p[0].distance(p[3]) * 20);
        Vector3 bc = null;

        for (double i = 0; i <= 1; i += 1.0 / (length * 10)) {
            Vector3 c = bezierCoordinate(p, i);

            if (bc != null) {
                Render.renderLine(matrix, Color.ORANGE, adaptVec(c), adaptVec(bc));
            }

            bc = c;
        }
        Render.renderLine(matrix, Color.ORANGE, adaptVec(p[3]).add(0.5, 0.5, 0.5), adaptVec(bc));
    }

    public static void renderPos(MatrixStack matrix, PosVector pos) {
        Color colorLine = pos.getLineColor();
        Color colorFill = pos.getFillColor();
        Vec3d start = adaptVec(pos.vector).subtract(increaseVec);
        Vec3d dimensions = new Vec3d(1, 1, 1).add(increaseVec).add(increaseVec);

        if (pos.h != 0) {
            Render.renderCrossing(matrix, colorLine, start, dimensions);
        }
        Render.renderEdged(matrix, colorFill, colorLine, start, dimensions);
    }

    static Vec3d adaptVec(Vector3 vec) {
        return new Vec3d(vec.getX(), vec.getY(), vec.getZ());
    }
}
