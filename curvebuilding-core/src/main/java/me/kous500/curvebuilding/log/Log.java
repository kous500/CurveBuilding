package me.kous500.curvebuilding.log;

import java.util.logging.Logger;

/** The log that everything is written to: a Java logger until the platform sets its own. */
public final class Log {
    private static Console console = Console.of(Logger.getLogger("CurveBuilding"));

    private Log() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * The log of the platform.
     *
     * @return the log set last
     */
    public static Console console() {
        return console;
    }

    /**
     * Makes {@code console} the log that everything is written to. Set it first, as reading the files can already
     * write to it.
     *
     * @param console the log of the platform
     */
    public static void setConsole(Console console) {
        Log.console = console;
    }
}
