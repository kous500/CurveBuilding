package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.Message;
import me.kous500.curvebuilding.config.Messages;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.YamlConfig;

import java.nio.file.Path;
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
     * @param jar the class loader that has the default files
     * @param readConfig makes the config of the platform from config.yml
     * @return the config read
     */
    public static <C extends Config> C loadResources(Path dataFolder, ClassLoader jar, Function<YamlConfig, C> readConfig) {
        ResourceFiles.copyDefaults(dataFolder, jar);

        C loadedConfig = readConfig.apply(YamlConfig.load(dataFolder.resolve("config.yml"), jar, "config.yml"));

        Path messageFile = Path.of(loadedConfig.messageFilePath.replace("%datafolder%", dataFolder.toString()));
        messages = Messages.load(messageFile, jar);
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
