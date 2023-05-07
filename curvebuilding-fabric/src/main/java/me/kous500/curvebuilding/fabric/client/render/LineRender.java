package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class LineRender extends Render {
    private static final LineRender LINE_RENDER = new LineRender();

    public static LineRender getInstance() {
        return LINE_RENDER;
    }

    @Override
    void setThroughWalls(boolean renderThroughWalls) {
        if (!BUFFER.isBuilding()) {
            BUFFER.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            this.renderThroughWalls = renderThroughWalls;
            currentIsBuilding = true;
        }
    }

    /**
     * 直方体の中に交差した線を描画します
     *
     * @param stack      MatrixStack
     * @param color      線の色
     * @param start      始点の座標
     * @param dimensions 大きさ
     */
    public void addCrossing(MatrixStack stack, Color color, Vec3d start, Vec3d dimensions) {
        Vec3d end = start.add(dimensions);
        double x1 = start.x;
        double y1 = start.y;
        double z1 = start.z;
        double x2 = end.x;
        double y2 = end.y;
        double z2 = end.z;

        addLine(stack, color, new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2));
        addLine(stack, color, new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z1));
        addLine(stack, color, new Vec3d(x1, y2, z1), new Vec3d(x2, y1, z2));
        addLine(stack, color, new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z2));
    }

    /**
     * ブロックの輪郭を描画します
     *
     * @param stack      MatrixStack
     * @param color      線の色
     * @param start      始点の座標
     * @param dimensions 大きさ
     */
    public void addOutline(MatrixStack stack, Color color, Vec3d start, Vec3d dimensions) {
        genericAABBRender(
                stack.peek().getPositionMatrix(),
                start,
                dimensions,
                color,
                (buffer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, matrix) -> {
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
                }
        );
    }

    /**
     * 線を描画します
     *
     * @param matrices MatrixStack
     * @param color    線の色
     * @param start    始点の座標
     * @param end      終点の座標
     */
    public void addLine(MatrixStack matrices, Color color, Vec3d start, Vec3d end) {
        if (!currentIsBuilding) return;

        genericAABBRender(
                matrices.peek().getPositionMatrix(),
                start,
                end.subtract(start),
                color,
                (buffer, x, y, z, x1, y1, z1, red, green, blue, alpha, matrix) -> {
                    buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                }
        );
    }
}