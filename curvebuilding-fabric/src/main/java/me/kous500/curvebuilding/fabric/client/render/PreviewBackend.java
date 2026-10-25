package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Draws the preview with the rendering API of one range of Minecraft versions.
 * Every version-specific part of the preview is in an implementation in the backend package;
 * {@link PreviewBackends} picks the one for the running version.
 */
public interface PreviewBackend {
    /**
     * Creates what the backend needs and registers the world render event that runs {@code onFrame} every frame.
     * {@link #drawLines} and {@link #drawQuads} are only called from {@code onFrame}. If the backend cannot be set up,
     * it logs why and never runs {@code onFrame}. Called once while the client starts.
     */
    void init(Runnable onFrame);

    /**
     * Camera position of the current frame. The preview geometry is relative to it.
     */
    Vec3 cameraPosition();

    /**
     * Draws the lines written by {@code writer}.
     *
     * @param isThroughWalls whether to draw through walls
     */
    void drawLines(boolean isThroughWalls, Consumer<PreviewBuffer> writer);

    /**
     * Draws the quads written by {@code writer}.
     *
     * @param isThroughWalls whether to draw through walls
     */
    void drawQuads(boolean isThroughWalls, Consumer<PreviewBuffer> writer);
}
