package me.kous500.curvebuilding.fabric.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sk89q.worldedit.math.Vector3;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.NavigableMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.kous500.curvebuilding.Util.*;
import static me.kous500.curvebuilding.Util.copyVector;
import static me.kous500.curvebuilding.fabric.FabricCurveBuilding.fabricConfig;

public class Render {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * 直方体の中に交差した線を描画します
     *
     * @param stack      MatrixStack
     * @param color      線の色
     * @param start      始点の座標
     * @param dimensions 大きさ
     */
    public static void renderCrossing(MatrixStack stack, Color color, Vec3d start, Vec3d dimensions, boolean renderThroughWalls) {
        Vec3d end = start.add(dimensions);
        double x1 = start.x;
        double y1 = start.y;
        double z1 = start.z;
        double x2 = end.x;
        double y2 = end.y;
        double z2 = end.z;

        renderLine(stack, color, new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z2), renderThroughWalls);
        renderLine(stack, color, new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z1), renderThroughWalls);
        renderLine(stack, color, new Vec3d(x1, y2, z1), new Vec3d(x2, y1, z2), renderThroughWalls);
        renderLine(stack, color, new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z2), renderThroughWalls);
    }

    /**
     * ブロックの輪郭を描画します
     *
     * @param stack      MatrixStack
     * @param color      線の色
     * @param start      始点の座標
     * @param dimensions 大きさ
     */
    public static void renderOutline(MatrixStack stack, Color color, Vec3d start, Vec3d dimensions, boolean renderThroughWalls) {
        Matrix4f m = stack.peek().getPositionMatrix();
        genericAABBRender(VertexFormat.DrawMode.DEBUG_LINES,
                GameRenderer::getPositionColorProgram,
                m,
                start,
                dimensions,
                color,
                renderThroughWalls,
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
     * 塗りつぶされたブロックを描画します
     *
     * @param stack      MatrixStack
     * @param color      塗りつぶす色
     * @param start      始点の座標
     * @param dimensions 大きさ
     */
    public static void renderFilled(MatrixStack stack, Color color, Vec3d start, Vec3d dimensions, boolean renderThroughWalls) {
        Matrix4f s = stack.peek().getPositionMatrix();
        genericAABBRender(VertexFormat.DrawMode.QUADS,
                GameRenderer::getPositionColorProgram,
                s,
                start,
                dimensions,
                color,
                renderThroughWalls,
                (buffer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, matrix) -> {
                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();

                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
                });
    }

    /**
     * AAABBBラインを描画します
     *
     * @param matrices MatrixStack
     * @param color    線の色
     * @param start    始点の座標
     * @param end      終点の座標
     */
    public static void renderLine(MatrixStack matrices, Color color, Vec3d start, Vec3d end, boolean renderThroughWalls) {
        Matrix4f s = matrices.peek().getPositionMatrix();
        genericAABBRender(VertexFormat.DrawMode.DEBUG_LINES,
                GameRenderer::getPositionColorProgram,
                s,
                start,
                end.subtract(start),
                color,
                renderThroughWalls,
                (buffer, x, y, z, x1, y1, z1, red, green, blue, alpha, matrix) -> {
                    buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
                    buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
                });
    }

    public static void bezierRender(MatrixStack matrices, NavigableMap<Integer, Vector3[]> pos, Color color, boolean renderThroughWalls) {
        if (pos == null) return;

        useBuffer(VertexFormat.DrawMode.DEBUG_LINES, GameRenderer::getPositionColorProgram, renderThroughWalls, bufferBuilder -> {
            double totalLen = 0;
            for (int n : pos.keySet()) {
                totalLen = bezierBuild(matrices.peek().getPositionMatrix(), pos.get(n), pos.get(n - 1), color, bufferBuilder, totalLen);
                if (totalLen > fabricConfig.lineRenderLength) return;
            }
        });
    }

    private static double bezierBuild(Matrix4f stack, Vector3[] p, Vector3[] bp, Color color, BufferBuilder bufferBuilder, double totalLen) {
        if (p == null || p[0] == null || bp == null || bp[0] == null) return 0;

        Vector3[] bezierPos = new Vector3[] {copyVector(bp[0]), copyVector(bp[2]), copyVector(p[1]), copyVector(p[0])};
        if (bezierPos[1] == null) bezierPos[1] = bezierPos[0];
        if (bezierPos[2] == null) bezierPos[2] = bezierPos[3];

        double length = bezierLength(bezierPos, bezierPos[0].distance(bezierPos[3]) * 5);
        totalLen += length;
        if (totalLen > fabricConfig.lineRenderLength) return totalLen;

        Vec3d bc = null;
        for (double i = 0; i <= 1; i += 1.0 / (length * fabricConfig.lineRenderAccuracy)) {
            Vec3d c = transformVec3d(adaptVec(bezierCoordinate(bezierPos, i)));
            setBezierVertex(stack, c, bc, color, bufferBuilder);
            bc = c;
        }

        setBezierVertex(stack, bc, transformVec3d(adaptVec(bezierPos[3].add(0.5, 0.5, 0.5))), color, bufferBuilder);

        return totalLen;
    }

    private static void setBezierVertex(Matrix4f stack, Vec3d c, Vec3d bc, Color color, BufferBuilder bufferBuilder) {
        if (c == null || bc == null) return;

        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;
        float x1 = (float) c.getX();
        float y1 = (float) c.getY();
        float z1 = (float) c.getZ();
        float x2 = (float) bc.getX();
        float y2 = (float) bc.getY();
        float z2 = (float) bc.getZ();

        bufferBuilder.vertex(stack, x1, y1, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(stack, x2, y2, z2).color(red, green, blue, alpha).next();
    }

    private static void genericAABBRender(VertexFormat.DrawMode mode, Supplier<ShaderProgram> shader, Matrix4f stack, Vec3d start, Vec3d dimensions, Color color, boolean renderThroughWalls, RenderAction action) {
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;
        Vec3d vec3d = transformVec3d(start);
        Vec3d end = vec3d.add(dimensions);
        float x1 = (float) vec3d.x;
        float y1 = (float) vec3d.y;
        float z1 = (float) vec3d.z;
        float x2 = (float) end.x;
        float y2 = (float) end.y;
        float z2 = (float) end.z;
        useBuffer(mode, shader, renderThroughWalls, bufferBuilder -> action.run(bufferBuilder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, stack));
    }

    public static Vec3d adaptVec(Vector3 vec) {
        return new Vec3d(vec.getX(), vec.getY(), vec.getZ());
    }

    private static Vec3d transformVec3d(Vec3d in) {
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        return in.subtract(camPos);
    }

    private static void useBuffer(VertexFormat.DrawMode mode, Supplier<ShaderProgram> shader, boolean renderThroughWalls, Consumer<BufferBuilder> runner) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        buffer.begin(mode, VertexFormats.POSITION_COLOR);

        runner.accept(buffer);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(10.0f);
        RenderSystem.depthFunc(renderThroughWalls ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);

        RenderSystem.setShader(shader);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }

    interface RenderAction {
        void run(BufferBuilder buffer, float x, float y, float z, float x1, float y1, float z1, float red, float green, float blue, float alpha, Matrix4f matrix);
    }
}
