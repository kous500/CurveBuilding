package me.kous500.curvebuilding.fabric.client.render.backend;

//? if >=1.21 && <1.21.8 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
//? if >=1.21.4 {
/^import net.minecraft.client.renderer.CoreShaders;
^///?} else {
/^import net.minecraft.client.renderer.GameRenderer;
^///?}
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

// Preview backend for Minecraft 1.21 - 1.21.4, which do not have RenderPipelines yet.
// Draws straight to the frame buffer with the position-color shader at the end of the world render
// (WorldRenderEvents.LAST), after translucent terrain such as water. Fabric API has already turned the model view
// matrix of RenderSystem toward the camera by then, so the vertices, relative to the camera, need no other pose.
// Lines are 1 pixel wide debug lines; through walls, the depth test always passes.
public final class TesselatorPreviewBackend implements PreviewBackend {
    private static final Matrix4f POSE = new Matrix4f();

    @Override
    public void init(Runnable onFrame) {
        WorldRenderEvents.LAST.register(context -> onFrame.run());
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

    private static void draw(VertexFormat.Mode mode, boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        BufferBuilder buffer = Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
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
                buffer.addVertex(POSE, x1, y1, z1).setColor(red, green, blue, alpha);
                buffer.addVertex(POSE, x2, y2, z2).setColor(red, green, blue, alpha);
            }

            @Override
            public void quadVertex(float x, float y, float z, float red, float green, float blue, float alpha) {
                buffer.addVertex(POSE, x, y, z).setColor(red, green, blue, alpha);
            }
        });
        // Null when nothing was written
        MeshData mesh = buffer.build();
        if (mesh == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(isThroughWalls ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
        //? if >=1.21.4 {
        /^RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        ^///?} else {
        /^RenderSystem.setShader(GameRenderer::getPositionColorShader);
        ^///?}
        BufferUploader.drawWithShader(mesh);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
*///?}
