package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.Message;
import me.kous500.curvebuilding.config.Messages;

public class CurveBuilding {
    public static boolean fawe = false;
    public static Config config;
    public static Console console;
    private static Messages messages;

    public static void setResources(Config config, Messages messages) {
        CurveBuilding.config = config;
        CurveBuilding.messages = messages;
    }

    public static void setConsole(Console console) {
        CurveBuilding.console = console;
    }

    /**
     * The message from the message file set in the config, with {@code {0}}, {@code {1}}, ... replaced with {@code args}.
     * Before the files are loaded, its path instead.
     */
    public static String getMessage(Message message, Object... args) {
        return messages != null ? messages.get(message, args) : message.path;
    }
}
