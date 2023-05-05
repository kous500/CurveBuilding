package me.kous500.curvebuilding.fabric.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.function.Consumer;

public abstract class Render {
    static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    static final BufferBuilder BUFFER = Tessellator.getInstance().getBuffer();

    /**
     * actionのレンダリング処理を実行します
     *
     * @param renderThroughWalls 壁を通過して描画を行うか
     * @param render 使用するレンダー
     * @param action レンダリング処理の内容
     */
    public static <T extends Render> void setRender(boolean renderThroughWalls, T render, RenderSetAction<T> action) {
        render.setThroughWalls(renderThroughWalls);
        action.run(render);
        render.rendering();
    }

    private static Vec3d transformVec3d(Vec3d in) {
        Camera camera = CLIENT.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        return in.subtract(camPos);
    }

    boolean renderThroughWalls;
    boolean currentIsBuilding = false;

    abstract void setThroughWalls(boolean renderThroughWalls);

    void rendering() {
        if (!currentIsBuilding) return;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(10.0f);
        RenderSystem.depthFunc(renderThroughWalls ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(BUFFER.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        currentIsBuilding = false;
    }

    void genericAABBRender(Matrix4f stack, Vec3d start, Vec3d dimensions, Color color, RenderAction action) {
        if (!currentIsBuilding) return;

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

        Consumer<BufferBuilder> runner = bufferBuilder -> action.run(bufferBuilder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, stack);
        runner.accept(BUFFER);
    }

    public interface RenderSetAction<T extends Render> {
        void run(T a);
    }

    interface RenderAction {
        void run(BufferBuilder buffer, float x, float y, float z, float x1, float y1, float z1, float red, float green, float blue, float alpha, Matrix4f matrix);
    }
}
