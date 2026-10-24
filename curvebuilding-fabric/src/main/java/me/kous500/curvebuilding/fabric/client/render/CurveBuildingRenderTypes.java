package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.client.renderer.RenderPipelines;
//? if >=26.1 {
/*import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Optional;
*///?} else {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
//?}

/**
 * Render types used to draw the preview.
 * RenderPipelines.DEBUG_FILLED_SNIPPET and the other non-public members used here are opened by curvebuilding.accesswidener.
 */
public final class CurveBuildingRenderTypes {
    //? if >=26.1 {
    /*static final RenderType LINES = create("lines", PrimitiveTopology.DEBUG_LINES, false);
    static final RenderType LINES_THROUGH_WALLS = create("lines_through_walls", PrimitiveTopology.DEBUG_LINES, true);
    static final RenderType FILLED = create("filled", PrimitiveTopology.QUADS, false);
    static final RenderType FILLED_THROUGH_WALLS = create("filled_through_walls", PrimitiveTopology.QUADS, true);
    *///?} else {
    static final RenderType LINES = create("lines", VertexFormat.Mode.DEBUG_LINES, false);
    static final RenderType LINES_THROUGH_WALLS = create("lines_through_walls", VertexFormat.Mode.DEBUG_LINES, true);
    static final RenderType FILLED = create("filled", VertexFormat.Mode.QUADS, false);
    static final RenderType FILLED_THROUGH_WALLS = create("filled_through_walls", VertexFormat.Mode.QUADS, true);
    //?}

    private CurveBuildingRenderTypes() {
    }

    /**
     * Creates the render types. Call while the client starts, so the pipelines are registered before they are compiled.
     */
    public static void init() {
    }

    //? if >=26.1 {
    /*private static RenderType create(String name, PrimitiveTopology topology, boolean isThroughWalls) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("curvebuilding", "pipeline/" + name))
                .withPrimitiveTopology(topology)
                .withCull(false);
        if (isThroughWalls) {
            builder.withDepthStencilState(Optional.empty());
        }

        RenderPipeline pipeline = RenderPipelines.register(builder.build());
        return RenderType.create("curvebuilding_" + name, RenderSetup.builder(pipeline).createRenderSetup());
    }
    *///?} else {
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
    //?}
}
