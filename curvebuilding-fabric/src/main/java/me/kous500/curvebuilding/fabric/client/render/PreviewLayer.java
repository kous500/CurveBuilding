package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.world.phys.Vec3;

import java.awt.*;

/**
 * The shapes of the preview that are drawn the same way: either all through walls or none.
 * Each shape is made of lines and quads; shapes can be added in any order.
 */
public final class PreviewLayer {
    // Boxes around a block are this much larger on each side, so that they do not flicker against the block's faces.
    private static final double BLOCK_MARGIN = 0.001;
    // Corner i of a box: bit 0 picks the larger x, bit 1 the larger y, bit 2 the larger z.
    private static final int[][] EDGES = {
            {0, 1}, {1, 5}, {5, 4}, {4, 0},
            {2, 3}, {3, 7}, {7, 6}, {6, 2},
            {0, 2}, {1, 3}, {4, 6}, {5, 7}
    };
    private static final int[][] DIAGONALS = {{0, 7}, {4, 3}, {2, 5}, {1, 6}};
    private static final int[][] FACES = {
            {2, 6, 7, 3}, {4, 5, 7, 6}, {1, 3, 7, 5},
            {0, 2, 3, 1}, {0, 4, 6, 2}, {0, 1, 5, 4}
    };

    private final PreviewPrimitives primitives;

    PreviewLayer(PreviewPrimitives primitives) {
        this.primitives = primitives;
    }

    /**
     * 線を描画します
     *
     * @param color    線の色
     * @param start    始点の座標
     * @param end      終点の座標
     */
    public void addLine(Color color, Vec3 start, Vec3 end) {
        primitives.line(color, start, end);
    }

    /**
     * Draws the outline of the block at {@code block}.
     *
     * @param block the block's minimum corner
     */
    public void addBlockOutline(Color color, Vec3 block) {
        Vec3[] corners = blockCorners(block);
        for (int[] edge : EDGES) primitives.line(color, corners[edge[0]], corners[edge[1]]);
    }

    /**
     * Draws lines between the opposite corners of the block at {@code block}.
     *
     * @param block the block's minimum corner
     */
    public void addBlockCrossing(Color color, Vec3 block) {
        Vec3[] corners = blockCorners(block);
        for (int[] diagonal : DIAGONALS) primitives.line(color, corners[diagonal[0]], corners[diagonal[1]]);
    }

    /**
     * Fills the block at {@code block}.
     *
     * @param block the block's minimum corner
     */
    public void addFilledBlock(Color color, Vec3 block) {
        Vec3[] corners = blockCorners(block);
        for (int[] face : FACES) primitives.quad(color, corners[face[0]], corners[face[1]], corners[face[2]], corners[face[3]]);
    }

    private static Vec3[] blockCorners(Vec3 block) {
        Vec3 min = block.subtract(BLOCK_MARGIN, BLOCK_MARGIN, BLOCK_MARGIN);
        Vec3 max = block.add(1 + BLOCK_MARGIN, 1 + BLOCK_MARGIN, 1 + BLOCK_MARGIN);
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vec3(
                    (i & 1) == 0 ? min.x : max.x,
                    (i & 2) == 0 ? min.y : max.y,
                    (i & 4) == 0 ? min.z : max.z
            );
        }
        return corners;
    }
}
