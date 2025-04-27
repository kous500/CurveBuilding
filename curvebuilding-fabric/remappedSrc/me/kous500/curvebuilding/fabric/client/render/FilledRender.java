package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

public class FilledRender extends Render {
    private static final FilledRender FILLED_RENDER = new FilledRender();

    public static FilledRender getInstance() {
        return FILLED_RENDER;
    }

    @Override
    void setRender(Matrix4f matrix, boolean isThroughWalls) {
        this.matrix = matrix;
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        this.isThroughWalls = isThroughWalls;
        currentIsBuilding = true;
    }

    /**
     * 塗りつぶされたブロックを描画します
     *
     * @param color      塗りつぶす色
     * @param start      始点の座標
     * @param dimensions 大きさ
     */
    public void renderFilled(Color color, Vec3d start, Vec3d dimensions) {
        genericAABBRender(
                start,
                dimensions,
                color,
                buffer,
                (buffer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha) -> {
                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);

                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);

                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);

                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);

                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha);

                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha);
                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha);
                }
        );
    }
}
