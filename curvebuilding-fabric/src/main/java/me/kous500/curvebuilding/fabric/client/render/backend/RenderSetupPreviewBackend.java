package me.kous500.curvebuilding.fabric.client.render.backend;

//? if >=1.21.11 && <26.1 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kous500.curvebuilding.fabric.client.render.PreviewBackend;
import me.kous500.curvebuilding.fabric.client.render.PreviewBuffer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.Consumer;

// Preview backend for Minecraft 1.21.11, where render types are built from a RenderSetup.
// Draws with its own buffer source at the end of the world render (WorldRenderEvents.END_MAIN), after translucent
// terrain such as water, so the usual depth test is enough.
// Lines use the vanilla line shader, so each line vertex needs a normal and a line width.
// The snippets, RenderPipelines.register and RenderType.create are opened by curvebuilding.accesswidener.
public final class RenderSetupPreviewBackend implements PreviewBackend {
    private static final float LINE_WIDTH = 1.0f;

    private final MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE));
    private RenderType lines;
    private RenderType linesThroughWalls;
    private RenderType quads;
    private RenderType quadsThroughWalls;
    private Matrix4f pose = new Matrix4f();

    @Override
    public void init(Runnable onFrame) {
        lines = create("lines", RenderPipelines.LINES_SNIPPET, false);
        linesThroughWalls = create("lines_through_walls", RenderPipelines.LINES_SNIPPET, true);
        quads = create("filled", RenderPipelines.DEBUG_FILLED_SNIPPET, false);
        quadsThroughWalls = create("filled_through_walls", RenderPipelines.DEBUG_FILLED_SNIPPET, true);

        WorldRenderEvents.END_MAIN.register(context -> {
            pose = context.matrices().last().pose();
            onFrame.run();
        });
    }

    @Override
    public Vec3 cameraPosition() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().position();
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
            public void line(float x1, float y1, float z1, float x2, float y2, float z2, float red, float green, float blue, float alpha) {
                float[] normal = LineNormals.normal(x1, y1, z1, x2, y2, z2);
                consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha).setNormal(normal[0], normal[1], normal[2]).setLineWidth(LINE_WIDTH);
                consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha).setNormal(normal[0], normal[1], normal[2]).setLineWidth(LINE_WIDTH);
            }

            @Override
            public void quadVertex(float x, float y, float z, float red, float green, float blue, float alpha) {
                consumer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
            }
        });
        bufferSource.endBatch();
    }

    private static RenderType create(String name, RenderPipeline.Snippet snippet, boolean isThroughWalls) {
        RenderPipeline.Builder builder = RenderPipeline.builder(snippet)
                .withLocation(Identifier.fromNamespaceAndPath("curvebuilding", "pipeline/" + name))
                .withCull(false);
        if (isThroughWalls) {
            builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
        }

        RenderPipeline pipeline = RenderPipelines.register(builder.build());
        return RenderType.create("curvebuilding_" + name, RenderSetup.builder(pipeline).createRenderSetup());
    }
}
*///?}
