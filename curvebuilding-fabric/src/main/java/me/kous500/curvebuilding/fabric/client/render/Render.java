package me.kous500.curvebuilding.fabric.client.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.function.Consumer;

public abstract class Render {
    static final Minecraft CLIENT = Minecraft.getInstance();
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE = MultiBufferSource.immediate(new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE));

    /**
     * actionのレンダリング処理を実行します
     *
     * @param renderThroughWalls 壁を通過して描画を行うか
     * @param render 使用するレンダー
     * @param action レンダリング処理の内容
     */
    public static <T extends Render> void setRender(Matrix4f matrix, boolean renderThroughWalls, T render, RenderSetAction<T> action) {
        render.setRender(matrix, renderThroughWalls);
        action.run(render);
        render.rendering();
    }

    private static Vec3 transformVec3d(Vec3 in) {
        Camera camera = CLIENT.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        return in.subtract(camPos);
    }

    boolean currentIsBuilding = false;
    VertexConsumer buffer;
    Matrix4f matrix;

    /**
     * Returns the render type to draw with.
     *
     * @param isThroughWalls whether to draw through walls
     */
    abstract RenderType renderType(boolean isThroughWalls);

    void setRender(Matrix4f matrix, boolean isThroughWalls) {
        this.matrix = matrix;
        buffer = BUFFER_SOURCE.getBuffer(renderType(isThroughWalls));
        currentIsBuilding = true;
    }

    void rendering() {
        if (!currentIsBuilding) return;

        BUFFER_SOURCE.endBatch();
        currentIsBuilding = false;
    }

    void genericAABBRender(Vec3 start, Vec3 dimensions, Color color, VertexConsumer buffer, RenderAction action) {
        if (!currentIsBuilding) return;

        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;
        Vec3 vec3d = transformVec3d(start);
        Vec3 end = vec3d.add(dimensions);
        float x1 = (float) vec3d.x;
        float y1 = (float) vec3d.y;
        float z1 = (float) vec3d.z;
        float x2 = (float) end.x;
        float y2 = (float) end.y;
        float z2 = (float) end.z;

        Consumer<VertexConsumer> runner = bufferBuilder -> action.run(bufferBuilder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha);
        runner.accept(buffer);
    }

    public interface RenderSetAction<T extends Render> {
        void run(T a);
    }

    interface RenderAction {
        void run(VertexConsumer buffer, float x, float y, float z, float x1, float y1, float z1, float red, float green, float blue, float alpha);
    }
}
