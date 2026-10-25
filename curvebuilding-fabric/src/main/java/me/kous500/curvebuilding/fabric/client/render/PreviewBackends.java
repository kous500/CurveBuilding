package me.kous500.curvebuilding.fabric.client.render;

//? if >=26.1 {
import me.kous500.curvebuilding.fabric.client.render.backend.SubmitPreviewBackend;
//?} else if >=1.21.11 {
/*import me.kous500.curvebuilding.fabric.client.render.backend.RenderSetupPreviewBackend;
*///?} else {
/*import me.kous500.curvebuilding.fabric.client.render.backend.LegacyPreviewBackend;
*///?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Picks the preview backend for the Minecraft version and runs the preview with it. This is the only place in the
 * preview code that branches by version; the version-specific code itself is in the backend package.
 */
public final class PreviewBackends {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewBackends.class);
    //? if >=26.1 {
    private static final PreviewBackend BACKEND = new SubmitPreviewBackend();
    //?} else if >=1.21.11 {
    /*private static final PreviewBackend BACKEND = new RenderSetupPreviewBackend();
    *///?} else {
    /*private static final PreviewBackend BACKEND = new LegacyPreviewBackend();
    *///?}

    private static boolean isFailed = false;

    private PreviewBackends() {
    }

    /**
     * Sets up the backend so that every frame {@code renderer} adds the preview shapes, which are then drawn.
     * If setting up or drawing fails, for example because the running Minecraft version is not compatible, the error
     * is logged once and the preview stays off.
     */
    public static void init(Consumer<PreviewFrame> renderer) {
        try {
            BACKEND.init(() -> {
                if (isFailed) return;

                try {
                    PreviewFrame frame = new PreviewFrame(BACKEND.cameraPosition());
                    renderer.accept(frame);
                    frame.draw(BACKEND);
                } catch (RuntimeException | LinkageError e) {
                    isFailed = true;
                    LOGGER.error("Could not draw the curve preview. The curve preview is disabled.", e);
                }
            });
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("Could not set up the curve preview. The curve preview is disabled.", e);
        }
    }
}
