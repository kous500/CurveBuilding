package me.kous500.curvebuilding.fabric;

import me.kous500.curvebuilding.Console;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes to the game log through SLF4J, which Fabric Loader provides. */
final class FabricConsole implements Console {
    private static final Logger LOGGER = LoggerFactory.getLogger("CurveBuilding");

    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Override
    public void warning(String message) {
        LOGGER.warn(message);
    }

    @Override
    public void warning(String message, Throwable cause) {
        LOGGER.warn(message, cause);
    }
}
