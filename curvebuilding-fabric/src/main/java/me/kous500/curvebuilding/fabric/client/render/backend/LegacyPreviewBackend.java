package me.kous500.curvebuilding.fabric.client.render.backend;

//? if >=1.21.8 && <1.21.11 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
//? if >=1.21.10 {
/^import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
^///?} else {
/^import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
^///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.Consumer;

// Preview backend for Minecraft 1.21.5 - 1.21.10.
// Draws with its own buffer source after translucent terrain such as water, so the usual depth test is enough: in
// WorldRenderEvents.END_MAIN from 1.21.10, and before it in WorldRenderEvents.LAST, where the model view matrix of
// RenderSystem already faces the camera, so the vertices, relative to the camera, need no other pose. Lines are 1 pixel
// wide debug lines.
// RenderPipelines.DEBUG_FILLED_SNIPPET and RenderType.create are opened by curvebuilding.accesswidener.
@SuppressWarnings("NotNullFieldNotInitialized") // init sets the render types before anything is drawn
public final class LegacyPreviewBackend implements PreviewBackend {
    private final MultiBufferSource.BufferSource bufferSource =
            MultiBufferSource.immediate(new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE));
    private RenderType lines;
    private RenderType linesThroughWalls;
    private RenderType quads;
    private RenderType quadsThroughWalls;
    private Matrix4f pose = new Matrix4f();

    @Override
    public void init(Runnable onFrame) {
        lines = create("lines", VertexFormat.Mode.DEBUG_LINES, false);
        linesThroughWalls = create("lines_through_walls", VertexFormat.Mode.DEBUG_LINES, true);
        quads = create("filled", VertexFormat.Mode.QUADS, false);
        quadsThroughWalls = create("filled_through_walls", VertexFormat.Mode.QUADS, true);

        //? if >=1.21.10 {
        /^WorldRenderEvents.END_MAIN.register(context -> {
            pose = context.matrices().last().pose();
            onFrame.run();
        });
        ^///?} else {
        /^WorldRenderEvents.LAST.register(context -> onFrame.run());
        ^///?}
    }

    @Override
    public Vec3 cameraPosition() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    @Override
    public void drawLines(boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        draw(isThroughWalls ? linesThroughWalls : lines, writer);
    }

    @Override
    public void drawQuads(boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        draw(isThroughWalls ? quadsThroughWalls : quads, writer);
    }

    private void draw(RenderType renderType, Consumer<PreviewBuffer> writer) {
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        writer.accept(new PreviewBuffer() {
            @Override
            public void line(
                    float x1,
                    float y1,
                    float z1,
                    float x2,
                    float y2,
                    float z2,
                    float red,
                    float green,
                    float blue,
                    float alpha) {
                consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
                consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
            }

            @Override
            public void quadVertex(float x, float y, float z, float red, float green, float blue, float alpha) {
                consumer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
            }
        });
        bufferSource.endBatch();
    }

    private static RenderType create(String name, VertexFormat.Mode mode, boolean isThroughWalls) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(ResourceLocation.fromNamespaceAndPath("curvebuilding", "pipeline/" + name))
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, mode)
                .withCull(false);
        if (isThroughWalls) {
            builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
        }

        return RenderType.create(
                "curvebuilding_" + name,
                RenderType.SMALL_BUFFER_SIZE,
                builder.build(),
                RenderType.CompositeState.builder().createCompositeState(false)
        );
    }
}
*///?}
