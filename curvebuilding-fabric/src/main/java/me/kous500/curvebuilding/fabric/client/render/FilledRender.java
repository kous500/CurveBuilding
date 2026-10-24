package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class FilledRender extends Render {
    private static final FilledRender FILLED_RENDER = new FilledRender();

    public static FilledRender getInstance() {
        return FILLED_RENDER;
    }

    @Override
    RenderType renderType(boolean isThroughWalls) {
        return isThroughWalls ? CurveBuildingRenderTypes.FILLED_THROUGH_WALLS : CurveBuildingRenderTypes.FILLED;
    }

    /**
     * 塗りつぶされたブロックを描画します
     *
     * @param color      塗りつぶす色
     * @param start      始点の座標
     * @param dimensions 大きさ
     */
    public void renderFilled(Color color, Vec3 start, Vec3 dimensions) {
        genericAABBRender(
                start,
                dimensions,
                color,
                buffer,
                (buffer, x1, y1, z1, x2, y2, z2, red, green, blue, alpha) -> {
                    buffer.addVertex(matrix, x1, y2, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y2, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y2, z1).setColor(red, green, blue, alpha);

                    buffer.addVertex(matrix, x1, y1, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y1, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y2, z2).setColor(red, green, blue, alpha);

                    buffer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y1, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y1, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y2, z1).setColor(red, green, blue, alpha);

                    buffer.addVertex(matrix, x2, y2, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y1, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y2, z1).setColor(red, green, blue, alpha);

                    buffer.addVertex(matrix, x1, y2, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y1, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y2, z2).setColor(red, green, blue, alpha);

                    buffer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y1, z1).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x2, y1, z2).setColor(red, green, blue, alpha);
                    buffer.addVertex(matrix, x1, y1, z2).setColor(red, green, blue, alpha);
                }
        );
    }
}
