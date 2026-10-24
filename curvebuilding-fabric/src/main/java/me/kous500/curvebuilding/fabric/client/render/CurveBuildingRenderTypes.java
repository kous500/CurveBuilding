package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.client.renderer.RenderPipelines;
//? if >=26.1 {
/*import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
*///?} else if >=1.21.11 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
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
 * <p>
 * From 1.21.11, lines use the vanilla line shader, so line vertices need a normal and a line width (see LineRender).
 * From 26.1, the pipeline is created with reflection, because the pipeline classes moved from
 * com.mojang.blaze3d.pipeline (26.1 - 26.2) to com.mojang.renderpearl.api.pipeline (26.3) while their method
 * names stayed the same. This lets one jar run on all of them. Up to 1.21.11, the non-public members used here are
 * opened by curvebuilding.accesswidener instead.
 */
public final class CurveBuildingRenderTypes {
    //? if >=26.1 {
    /*private static final Logger LOGGER = LoggerFactory.getLogger(CurveBuildingRenderTypes.class);
    private static final String[] PIPELINE_CLASSES = {
            "com.mojang.renderpearl.api.pipeline.RenderPipeline",
            "com.mojang.blaze3d.pipeline.RenderPipeline"
    };

    static RenderType LINES;
    static RenderType LINES_THROUGH_WALLS;
    static RenderType FILLED;
    static RenderType FILLED_THROUGH_WALLS;

    static {
        try {
            LINES = create("lines", "LINES_SNIPPET", false);
            LINES_THROUGH_WALLS = create("lines_through_walls", "LINES_SNIPPET", true);
            FILLED = create("filled", "DEBUG_FILLED_SNIPPET", false);
            FILLED_THROUGH_WALLS = create("filled_through_walls", "DEBUG_FILLED_SNIPPET", true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LINES = LINES_THROUGH_WALLS = FILLED = FILLED_THROUGH_WALLS = null;
            LOGGER.error("Could not create the render pipelines for this Minecraft version. The curve preview is disabled.", e);
        }
    }
    *///?} else if >=1.21.11 {
    /*static final RenderType LINES = create("lines", RenderPipelines.LINES_SNIPPET, false);
    static final RenderType LINES_THROUGH_WALLS = create("lines_through_walls", RenderPipelines.LINES_SNIPPET, true);
    static final RenderType FILLED = create("filled", RenderPipelines.DEBUG_FILLED_SNIPPET, false);
    static final RenderType FILLED_THROUGH_WALLS = create("filled_through_walls", RenderPipelines.DEBUG_FILLED_SNIPPET, true);
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

    /**
     * Whether the render types could be created. When false, the preview is not drawn.
     */
    public static boolean isAvailable() {
        return LINES != null;
    }

    //? if >=26.1 {
    /*private static RenderType create(String name, String snippetField, boolean isThroughWalls) throws ReflectiveOperationException {
        Class<?> pipelineClass = findPipelineClass();
        Class<?> snippetClass = Class.forName(pipelineClass.getName() + "$Snippet");

        Field snippetValue = RenderPipelines.class.getDeclaredField(snippetField);
        snippetValue.setAccessible(true);
        Object snippets = Array.newInstance(snippetClass, 1);
        Array.set(snippets, 0, snippetValue.get(null));

        Object builder = pipelineClass.getMethod("builder", snippets.getClass()).invoke(null, snippets);
        Class<?> builderClass = builder.getClass();
        builderClass.getMethod("withLocation", Identifier.class)
                .invoke(builder, Identifier.fromNamespaceAndPath("curvebuilding", "pipeline/" + name));
        builderClass.getMethod("withCull", boolean.class).invoke(builder, false);
        if (isThroughWalls) {
            builderClass.getMethod("withDepthStencilState", Optional.class).invoke(builder, Optional.empty());
        }
        Object pipeline = builderClass.getMethod("build").invoke(builder);

        Method register = RenderPipelines.class.getDeclaredMethod("register", pipelineClass);
        register.setAccessible(true);
        pipeline = register.invoke(null, pipeline);

        RenderSetup setup = ((RenderSetup.RenderSetupBuilder) RenderSetup.class.getMethod("builder", pipelineClass).invoke(null, pipeline))
                .createRenderSetup();
        Method create = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
        create.setAccessible(true);
        return (RenderType) create.invoke(null, "curvebuilding_" + name, setup);
    }

    private static Class<?> findPipelineClass() throws ClassNotFoundException {
        for (String name : PIPELINE_CLASSES) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(String.join(" or ", PIPELINE_CLASSES));
    }
    *///?} else if >=1.21.11 {
    /*private static RenderType create(String name, RenderPipeline.Snippet snippet, boolean isThroughWalls) {
        RenderPipeline.Builder builder = RenderPipeline.builder(snippet)
                .withLocation(Identifier.fromNamespaceAndPath("curvebuilding", "pipeline/" + name))
                .withCull(false);
        if (isThroughWalls) {
            builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
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
