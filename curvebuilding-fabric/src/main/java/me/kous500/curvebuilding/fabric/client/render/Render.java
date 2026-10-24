package me.kous500.curvebuilding.fabric.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
//? if >=26.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.ArrayList;
import java.util.List;
*///?} else if >=1.21.11 {
/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
*///?} else {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//?}

import java.awt.*;
import java.util.function.Consumer;

public abstract class Render {
    static final Minecraft CLIENT = Minecraft.getInstance();
    //? if >=26.1 {
    /*private static SubmitNodeCollector collector;
    private static PoseStack poseStack;
    private static Vec3 cameraPos = Vec3.ZERO;

    // Sets where the preview of the current frame is submitted to. Called before each frame's RenderPosData.
    // The camera position comes from the frame's render state, because the camera getter of GameRenderer
    // differs between 26.x versions (getMainCamera / mainCamera).
    public static void beginFrame(SubmitNodeCollector frameCollector, PoseStack framePoseStack, Vec3 frameCameraPos) {
        collector = frameCollector;
        poseStack = framePoseStack;
        cameraPos = frameCameraPos;
    }
    *///?} else {
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE = MultiBufferSource.immediate(new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE));
    //?}

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
        //? if >=26.1 {
        /*Vec3 camPos = cameraPos;
        *///?} else if >=1.21.11 {
        /*Camera camera = CLIENT.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        *///?} else {
        Camera camera = CLIENT.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        //?}
        return in.subtract(camPos);
    }

    boolean currentIsBuilding = false;
    VertexConsumer buffer;
    Matrix4f matrix;
    //? if >=26.1 {
    /*// Geometry is submitted now and drawn later by Minecraft, so the vertex writes are replayed then.
    private final List<Consumer<VertexConsumer>> pending = new ArrayList<>();
    private RenderType pendingType;
    *///?}

    /**
     * Returns the render type to draw with.
     *
     * @param isThroughWalls whether to draw through walls
     */
    abstract RenderType renderType(boolean isThroughWalls);

    void setRender(Matrix4f matrix, boolean isThroughWalls) {
        //? if >=26.1 {
        /*pendingType = renderType(isThroughWalls);
        pending.clear();
        *///?} else {
        this.matrix = matrix;
        buffer = BUFFER_SOURCE.getBuffer(renderType(isThroughWalls));
        //?}
        currentIsBuilding = true;
    }

    void rendering() {
        if (!currentIsBuilding) return;

        //? if >=26.1 {
        /*if (collector != null && !pending.isEmpty()) {
            List<Consumer<VertexConsumer>> runners = List.copyOf(pending);
            collector.submitCustomGeometry(poseStack, pendingType, (pose, consumer) -> {
                matrix = pose.pose();
                runners.forEach(runner -> runner.accept(consumer));
            });
        }
        pending.clear();
        *///?} else {
        BUFFER_SOURCE.endBatch();
        //?}
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
        //? if >=26.1 {
        /*pending.add(runner);
        *///?} else {
        runner.accept(buffer);
        //?}
    }

    public interface RenderSetAction<T extends Render> {
        void run(T a);
    }

    interface RenderAction {
        void run(VertexConsumer buffer, float x, float y, float z, float x1, float y1, float z1, float red, float green, float blue, float alpha);
    }
}
