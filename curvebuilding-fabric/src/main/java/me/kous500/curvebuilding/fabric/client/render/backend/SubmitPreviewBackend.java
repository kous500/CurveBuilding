package me.kous500.curvebuilding.fabric.client.render.backend;

//? if >=26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kous500.curvebuilding.fabric.client.render.PreviewBackend;
import me.kous500.curvebuilding.fabric.client.render.PreviewBuffer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Preview backend for Minecraft 26.1 and newer. One jar runs on 26.1 - 26.3.
//
// The preview is drawn after translucent terrain such as water. Drawn before it, the preview either hides the
// translucent blocks behind it (when it writes depth) or is drawn over by them (when it does not).
// SubmitNodeCollector.submitCustomGeometry draws before translucent terrain, so instead:
// - 26.2 and newer: the geometry is submitted to the afterTerrain phase in LevelRenderEvents.COLLECT_SUBMITS and
//   drawn by Minecraft later, so the vertices are recorded now and written when Minecraft draws them.
// - 26.1 has no afterTerrain phase but still draws through buffer sources, so the geometry is drawn right away in
//   LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN. Buffer sources are gone in 26.3, so they are used with reflection.
//
// Improved transparency works differently by version:
// - 26.1 - 26.2 draw translucent things into separate targets and merge them by depth afterwards; the main target
//   counts as opaque, so lines there would hide the water behind them. The preview is drawn into the item entity
//   target, which is merged like the others and falls back to the main target when improved transparency is off.
//   OutputTarget is gone in 26.3, so it is set with reflection.
// - 26.3 draws every translucent phase, afterTerrain included, as one order-independent (OIT) phase, which needs OIT
//   pipelines on each render type. The preview uses the vanilla OIT pipelines for lines and debug boxes, and through
//   walls the same pipelines without the depth test. (The always-on-top phase does not help: it only runs in frames
//   that have always-on-top gizmos.)
//
// The pipeline classes moved from com.mojang.blaze3d.pipeline (26.1 - 26.2) to com.mojang.renderpearl.api.pipeline
// (26.3) while their method names stayed the same, so the pipelines are created with reflection, which also reaches
// the non-public snippets, RenderPipelines.register and RenderType.create.
// Every pipeline sets a translucent color target: LINES_SNIPPET has none in 26.3, and a pipeline without one fails
// when it is drawn ("color attachment count must match pipeline color target state count").
public final class SubmitPreviewBackend implements PreviewBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitPreviewBackend.class);
    private static final String[] PIPELINE_CLASSES = {
            "com.mojang.renderpearl.api.pipeline.RenderPipeline",
            "com.mojang.blaze3d.pipeline.RenderPipeline"
    };
    private static final float LINE_WIDTH = 1.0f;
    private static final boolean HAS_OIT = classExists("net.minecraft.client.renderer.oit.OitPipelineSet");

    private RenderType lines;
    private RenderType linesThroughWalls;
    private RenderType quads;
    private RenderType quadsThroughWalls;
    private ImmediateDraw immediateDraw;
    private SubmitNodeCollector collector;
    private LevelRenderContext immediateContext;
    private PoseStack poseStack;
    private Vec3 cameraPosition = Vec3.ZERO;

    @Override
    public void init(Runnable onFrame) {
        boolean hasAfterTerrainPhase = hasAfterTerrainPhase();
        try {
            lines = create("lines", true, false);
            linesThroughWalls = create("lines_through_walls", true, true);
            quads = create("filled", false, false);
            quadsThroughWalls = create("filled_through_walls", false, true);
            if (!hasAfterTerrainPhase) immediateDraw = new ImmediateDraw();
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("Could not create the render pipelines for this Minecraft version. The curve preview is disabled.", e);
            return;
        }

        if (hasAfterTerrainPhase) {
            LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
                collector = context.submitNodeCollector();
                render(context, onFrame);
                collector = null;
            });
        } else {
            LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
                immediateContext = context;
                render(context, onFrame);
                immediateContext = null;
            });
        }
    }

    private void render(LevelRenderContext context, Runnable onFrame) {
        poseStack = context.poseStack();
        cameraPosition = context.levelState().cameraRenderState.pos;
        onFrame.run();
    }

    @Override
    public Vec3 cameraPosition() {
        return cameraPosition;
    }

    @Override
    public void drawLines(boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        draw(isThroughWalls ? linesThroughWalls : lines, true, writer);
    }

    @Override
    public void drawQuads(boolean isThroughWalls, Consumer<PreviewBuffer> writer) {
        draw(isThroughWalls ? quadsThroughWalls : quads, false, writer);
    }

    // Each vertex is {x, y, z, red, green, blue, alpha, normal x, normal y, normal z}
    private void draw(RenderType renderType, boolean isLines, Consumer<PreviewBuffer> writer) {
        if (collector == null && immediateContext == null) return;

        List<float[]> vertices = new ArrayList<>();
        writer.accept(new PreviewBuffer() {
            @Override
            public void line(float x1, float y1, float z1, float x2, float y2, float z2, float red, float green, float blue, float alpha) {
                float[] normal = LineNormals.normal(x1, y1, z1, x2, y2, z2);
                vertices.add(new float[]{x1, y1, z1, red, green, blue, alpha, normal[0], normal[1], normal[2]});
                vertices.add(new float[]{x2, y2, z2, red, green, blue, alpha, normal[0], normal[1], normal[2]});
            }

            @Override
            public void quadVertex(float x, float y, float z, float red, float green, float blue, float alpha) {
                vertices.add(new float[]{x, y, z, red, green, blue, alpha, 0, 0, 0});
            }
        });
        if (vertices.isEmpty()) return;

        SubmitNodeCollector.CustomGeometryRenderer geometry = (pose, consumer) -> {
            for (float[] v : vertices) {
                VertexConsumer vertex = consumer.addVertex(pose.pose(), v[0], v[1], v[2]).setColor(v[3], v[4], v[5], v[6]);
                if (isLines) vertex.setNormal(v[7], v[8], v[9]).setLineWidth(LINE_WIDTH);
            }
        };
        if (immediateContext != null) {
            immediateDraw.draw(immediateContext, renderType, poseStack.last(), geometry);
        } else {
            // Fabric passes Minecraft's SubmitNodeStorage, the only collector with phases to choose from
            AfterTerrainPhase.submit((SubmitNodeStorage) collector, poseStack, renderType, geometry);
        }
    }

    private static boolean hasAfterTerrainPhase() {
        try {
            SubmitNodeCollection.class.getField("afterTerrain");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, SubmitPreviewBackend.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // Separate class so that 26.1, which lacks CustomFeatureRenderer.Submit and afterTerrain, never loads this code.
    private static final class AfterTerrainPhase {
        static void submit(SubmitNodeStorage storage, PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer geometry) {
            storage.order(0).afterTerrain.submit(new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, geometry));
        }
    }

    // Separate class so that 26.1 - 26.2, which lack OIT, never load this code.
    private static final class Oit {
        // Through walls: built like the vanilla sets, from the non-public OIT snippets, without the depth test.
        static void setPipelines(RenderSetup.RenderSetupBuilder setup, String name, boolean isLines, boolean isThroughWalls) throws ReflectiveOperationException {
            if (!isThroughWalls) {
                setup.setOitPipelines(isLines ? RenderPipelines.OIT_LINES_TRANSLUCENT : RenderPipelines.OIT_DEBUG_FILLED_BOX);
                return;
            }

            Field snippet = RenderPipelines.class.getDeclaredField(isLines ? "OIT_LINES_SNIPPET" : "OIT_DEBUG_FILLED_SNIPPET");
            snippet.setAccessible(true);
            OitPipelineSet pipelines = OitPipelineSet.builder("curvebuilding_" + name, RenderPipeline.builder((RenderPipeline.Snippet) snippet.get(null)))
                    .withoutDepthTest()
                    .build();
            Method register = RenderPipelines.class.getDeclaredMethod("register", OitPipelineSet.class);
            register.setAccessible(true);
            setup.setOitPipelines((OitPipelineSet) register.invoke(null, pipelines));
        }
    }

    // Draws through the buffer source of LevelRenderContext, which exists only in 26.1.
    private static final class ImmediateDraw {
        private final Method contextBufferSource;
        private final Method getBuffer;
        private final Method endBatch;

        ImmediateDraw() throws ReflectiveOperationException {
            Class<?> bufferSourceClass = Class.forName("net.minecraft.client.renderer.MultiBufferSource$BufferSource");
            contextBufferSource = LevelRenderContext.class.getMethod("bufferSource");
            getBuffer = bufferSourceClass.getMethod("getBuffer", RenderType.class);
            endBatch = bufferSourceClass.getMethod("endBatch", RenderType.class);
        }

        void draw(LevelRenderContext context, RenderType renderType, PoseStack.Pose pose, SubmitNodeCollector.CustomGeometryRenderer geometry) {
            try {
                Object bufferSource = contextBufferSource.invoke(context);
                geometry.render(pose, (VertexConsumer) getBuffer.invoke(bufferSource, renderType));
                endBatch.invoke(bufferSource, renderType);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static RenderType create(String name, boolean isLines, boolean isThroughWalls) throws ReflectiveOperationException {
        Class<?> pipelineClass = findPipelineClass();
        Class<?> snippetClass = Class.forName(pipelineClass.getName() + "$Snippet");

        Field snippetValue = RenderPipelines.class.getDeclaredField(isLines ? "LINES_SNIPPET" : "DEBUG_FILLED_SNIPPET");
        snippetValue.setAccessible(true);
        Object snippets = Array.newInstance(snippetClass, 1);
        Array.set(snippets, 0, snippetValue.get(null));

        Object builder = pipelineClass.getMethod("builder", snippets.getClass()).invoke(null, snippets);
        Class<?> builderClass = builder.getClass();
        builderClass.getMethod("withLocation", Identifier.class)
                .invoke(builder, Identifier.fromNamespaceAndPath("curvebuilding", "pipeline/" + name));
        builderClass.getMethod("withCull", boolean.class).invoke(builder, false);
        Object colorTargetState = translucentColorTargetState(pipelineClass);
        builderClass.getMethod("withColorTargetState", colorTargetState.getClass()).invoke(builder, colorTargetState);
        // Otherwise the snippet's depth state is kept: lines test and write depth, filled quads only test it.
        if (isThroughWalls) {
            Object depthStencilState = noDepthTest(pipelineClass);
            builderClass.getMethod("withDepthStencilState", depthStencilState.getClass()).invoke(builder, depthStencilState);
        }
        Object pipeline = builderClass.getMethod("build").invoke(builder);

        Method register = RenderPipelines.class.getDeclaredMethod("register", pipelineClass);
        register.setAccessible(true);
        pipeline = register.invoke(null, pipeline);

        RenderSetup.RenderSetupBuilder setup = (RenderSetup.RenderSetupBuilder) RenderSetup.class.getMethod("builder", pipelineClass).invoke(null, pipeline);
        if (HAS_OIT) {
            Oit.setPipelines(setup, name, isLines, isThroughWalls);
        } else {
            setItemEntityTarget(setup);
        }
        Method create = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
        create.setAccessible(true);
        return (RenderType) create.invoke(null, "curvebuilding_" + name, setup.createRenderSetup());
    }

    private static void setItemEntityTarget(RenderSetup.RenderSetupBuilder setup) throws ReflectiveOperationException {
        Class<?> outputTargetClass = Class.forName("net.minecraft.client.renderer.rendertype.OutputTarget");
        Object itemEntityTarget = outputTargetClass.getField("ITEM_ENTITY_TARGET").get(null);
        RenderSetup.RenderSetupBuilder.class.getMethod("setOutputTarget", outputTargetClass).invoke(setup, itemEntityTarget);
    }

    private static Object translucentColorTargetState(Class<?> pipelineClass) throws ReflectiveOperationException {
        Class<?> colorTargetClass = Class.forName(pipelineClass.getPackageName() + ".ColorTargetState");
        Class<?> blendFunctionClass = Class.forName(pipelineClass.getPackageName() + ".BlendFunction");
        Object translucent = blendFunctionClass.getField("TRANSLUCENT").get(null);
        return colorTargetClass.getConstructor(blendFunctionClass).newInstance(translucent);
    }

    // DepthStencilState(CompareOp.ALWAYS_PASS, false). CompareOp is in a different package in 26.1 - 26.2.
    private static Object noDepthTest(Class<?> pipelineClass) throws ReflectiveOperationException {
        Class<?> depthStencilClass = Class.forName(pipelineClass.getPackageName() + ".DepthStencilState");
        for (Constructor<?> constructor : depthStencilClass.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 2 && parameters[0].isEnum() && parameters[1] == boolean.class) {
                return constructor.newInstance(enumConstant(parameters[0], "ALWAYS_PASS"), false);
            }
        }
        throw new NoSuchMethodException(depthStencilClass.getName() + "(CompareOp, boolean)");
    }

    private static Object enumConstant(Class<?> enumClass, String name) throws NoSuchFieldException {
        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) return constant;
        }
        throw new NoSuchFieldException(enumClass.getName() + "." + name);
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
}
//?}
