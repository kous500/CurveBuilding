package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.YamlConfig;
import me.kous500.curvebuilding.message.Messages;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Function;

import static me.kous500.curvebuilding.log.Log.console;

/** Reads the files that every platform shares, and keeps the config that the commands read. */
public class CurveBuilding {
    /** The config read by {@link #loadResources}, which each platform calls when it starts, before it is read. */
    @SuppressWarnings("NotNullFieldNotInitialized")
    public static Config config;

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

        Messages.use(Messages.load(messageFile(loadedConfig.messageFilePath, dataFolder), jar));
        config = loadedConfig;
        return loadedConfig;
    }

    private static Path messageFile(@Nullable String setting, Path dataFolder) {
        Path english = dataFolder.resolve("messages/en-US.yml");
        if (setting == null) return english;
        try {
            return Path.of(setting.replace("%datafolder%", dataFolder.toString()));
        } catch (InvalidPathException e) {
            console().warning("message-file in config.yml is not a file path: " + setting + ", using " + english);
            return english;
        }
    }
}
