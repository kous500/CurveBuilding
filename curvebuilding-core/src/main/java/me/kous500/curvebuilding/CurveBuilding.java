package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.Message;
import me.kous500.curvebuilding.config.Messages;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.ResourceType;
import me.kous500.curvebuilding.config.YamlConfig;

import java.io.File;
import java.util.function.Function;
import java.util.logging.Logger;

public class CurveBuilding {
    public static boolean fawe = false;
    public static Config config;
    /** The log of the platform. Set it first, as reading the files can already write to it. */
    public static Console console = Console.of(Logger.getLogger("CurveBuilding"));
    private static Messages messages;

    /**
     * Copies the default files that are missing into the data folder, then reads config.yml and the message file it
     * names, and uses them from now on.
     *
     * @param readConfig makes the config of the platform from config.yml
     * @return the config read
     */
    public static <C extends Config> C loadResources(MainInitializer initializer, Function<YamlConfig, C> readConfig) {
        ResourceFiles.setup(initializer);

        File configFile = new File(initializer.getConfigPass(), "config.yml");
        C loadedConfig = readConfig.apply(YamlConfig.loadConfiguration(configFile, ResourceType.config, initializer));

        File messageFile = new File(loadedConfig.messageFilePath.replace("%datafolder%", initializer.getConfigPass()));
        messages = new Messages(YamlConfig.loadConfiguration(messageFile, ResourceType.message, initializer));
        config = loadedConfig;
        return loadedConfig;
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
