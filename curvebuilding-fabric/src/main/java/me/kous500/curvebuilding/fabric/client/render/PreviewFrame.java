package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.world.phys.Vec3;

/**
 * The preview shapes of one frame, split into the part drawn normally and the part drawn through walls.
 */
public final class PreviewFrame {
    private final PreviewPrimitives normal;
    private final PreviewPrimitives throughWalls;
    private final PreviewLayer normalLayer;
    private final PreviewLayer throughWallsLayer;

    PreviewFrame(Vec3 cameraPosition) {
        normal = new PreviewPrimitives(cameraPosition, false);
        throughWalls = new PreviewPrimitives(cameraPosition, true);
        normalLayer = new PreviewLayer(normal);
        throughWallsLayer = new PreviewLayer(throughWalls);
    }

    /**
     * The layer to add shapes to.
     *
     * @param isThroughWalls whether the shapes are drawn through walls
     */
    public PreviewLayer layer(boolean isThroughWalls) {
        return isThroughWalls ? throughWallsLayer : normalLayer;
    }

    void draw(PreviewBackend backend) {
        normal.draw(backend);
        throughWalls.draw(backend);
    }
}
