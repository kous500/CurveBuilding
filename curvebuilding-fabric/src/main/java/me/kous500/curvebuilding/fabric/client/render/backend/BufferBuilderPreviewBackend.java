package me.kous500.curvebuilding.fabric.client.render.backend;

//? if <1.21 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

// Preview backend for Minecraft 1.20 - 1.20.4, where a BufferBuilder is started and ended by hand.
// Draws straight to the frame buffer with the position-color shader at the end of the world render
// (WorldRenderEvents.LAST), after translucent terrain such as water. Minecraft has already restored the model view
// matrix of RenderSystem by then, so the camera rotation comes from the pose stack of the event.
// Lines are 1 pixel wide debug lines; through walls, the depth test always passes.
public final class BufferBuilderPreviewBackend implements PreviewBackend {
    private Matrix4f pose = new Matrix4f();

    @Override
    public void init(Runnable onFrame) {
        WorldRenderEvents.LAST.register(context -> {
            pose = context.matrixStack().last().pose();
            onFrame.run();
        });
    }

    @Override
    public Vec3 cameraPosition() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    @Override
    public void drawLines(boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        draw(VertexFormat.Mode.DEBUG_LINES, isThroughWalls, writer);
    }

    @Override
    public void drawQuads(boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        draw(VertexFormat.Mode.QUADS, isThroughWalls, writer);
    }

    private void draw(VertexFormat.Mode mode, boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(mode, DefaultVertexFormat.POSITION_COLOR);
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
                buffer.vertex(pose, x1, y1, z1).color(red, green, blue, alpha).endVertex();
                buffer.vertex(pose, x2, y2, z2).color(red, green, blue, alpha).endVertex();
            }

            @Override
            public void quadVertex(float x, float y, float z, float red, float green, float blue, float alpha) {
                buffer.vertex(pose, x, y, z).color(red, green, blue, alpha).endVertex();
            }
        });
        BufferBuilder.RenderedBuffer rendered = buffer.end();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(isThroughWalls ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(rendered);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
*///?}
