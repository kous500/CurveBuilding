package me.kous500.curvebuilding.fabric.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Render types used to draw the preview.
 * RenderPipelines.DEBUG_FILLED_SNIPPET and the other non-public members used here are opened by curvebuilding.accesswidener.
 */
final class CurveBuildingRenderTypes {
    static final RenderType LINES = create("lines", VertexFormat.Mode.DEBUG_LINES, false);
    static final RenderType LINES_THROUGH_WALLS = create("lines_through_walls", VertexFormat.Mode.DEBUG_LINES, true);
    static final RenderType FILLED = create("filled", VertexFormat.Mode.QUADS, false);
    static final RenderType FILLED_THROUGH_WALLS = create("filled_through_walls", VertexFormat.Mode.QUADS, true);

    private CurveBuildingRenderTypes() {
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
