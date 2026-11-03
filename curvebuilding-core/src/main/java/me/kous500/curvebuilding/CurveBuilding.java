package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.YamlConfig;
import me.kous500.curvebuilding.message.Message;
import me.kous500.curvebuilding.message.Messages;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.logging.Logger;

/** What the commands of every platform share: the config, the messages of the message file and the log. */
public class CurveBuilding {
    /** The config read by {@link #loadResources}, which each platform calls when it starts, before it is read. */
    @SuppressWarnings("NotNullFieldNotInitialized")
    public static Config config;
    /** The log of the platform. Set it first, as reading the files can already write to it. */
    public static Console console = Console.of(Logger.getLogger("CurveBuilding"));
    private static @Nullable Messages messages;

    /**
     * Copies the default files that are missing into the data folder, then reads config.yml and the message file it
     * names, and uses them from now on.
     *
     * @param dataFolder the folder of the config and the message files
     * @param jar the class loader that has the default files
     * @param readConfig makes the config of the platform from config.yml
     * @param <C> the config of the platform
     * @return the config read
     */
    public static <C extends Config> C loadResources(
            Path dataFolder,
            ClassLoader jar,
            Function<YamlConfig, C> readConfig) {
        ResourceFiles.copyDefaults(dataFolder, jar);

        C loadedConfig = readConfig.apply(YamlConfig.load(dataFolder.resolve("config.yml"), jar, "config.yml"));

        messages = Messages.load(messageFile(loadedConfig.messageFilePath, dataFolder), jar);
        config = loadedConfig;
        return loadedConfig;
    }

    private static Path messageFile(@Nullable String setting, Path dataFolder) {
        Path english = dataFolder.resolve("messages/en-US.yml");
        if (setting == null) return english;
        try {
            return Path.of(setting.replace("%datafolder%", dataFolder.toString()));
        } catch (InvalidPathException e) {
            console.warning("message-file in config.yml is not a file path: " + setting + ", using " + english);
            return english;
        }
    }

    /**
     * Makes {@code console} the log that everything is written to.
     *
     * @param console the log of the platform
     */
    public static void setConsole(Console console) {
        CurveBuilding.console = console;
    }

    /**
     * A message from the message file set in the config.
     *
     * @param message the message
     * @param args the values for {@code {0}}, {@code {1}}, ... in the message
     * @return the message with its values, or its path before the files are loaded
     */
    public static String getMessage(Message message, Object... args) {
        return messages != null ? messages.get(message, args) : message.path;
    }
}
