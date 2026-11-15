package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.ConfigFile;
import me.kous500.curvebuilding.config.ResourceFiles;
import me.kous500.curvebuilding.config.YamlConfig;
import me.kous500.curvebuilding.config.YamlRewriter;
import me.kous500.curvebuilding.message.Messages;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Function;

import static me.kous500.curvebuilding.log.Log.console;

/** Reads the files that every platform shares. */
public final class CurveBuilding {
    private static final String CONFIG = "config.yml";
    private static final String BACKUPS = "backups";

    private CurveBuilding() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Copies the default files that are missing into the data folder, then rewrites config.yml and the message file it
     * names in the layout of their default files (see
     * {@link YamlRewriter#rewrite(Path, ClassLoader, String, Path, java.util.function.Consumer)}), with the backups
     * in {@code backups} of the data folder, and reads them. The messages are used from now on; the platform keeps the
     * config and gives it to the commands.
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
        Path backups = dataFolder.resolve(BACKUPS);

        Path configFile = dataFolder.resolve(CONFIG);
        YamlRewriter.rewrite(configFile, jar, CONFIG, backups, readConfig::apply);
        C loadedConfig = readConfig.apply(YamlConfig.load(configFile, jar, CONFIG));

        Path messageFile = messageFile(loadedConfig.messageFilePath, dataFolder);
        Messages.use(Messages.load(messageFile, jar, backups.resolve("messages")));
        return loadedConfig;
    }

    /**
     * The config file of the data folder, as {@link #loadResources} reads it, to read and set its values.
     *
     * @param dataFolder the folder of the config and the message files
     * @param jar the class loader that has the default files
     * @param readConfig makes the config of the platform from config.yml
     * @return the config file
     */
    public static ConfigFile configFile(
            Path dataFolder,
            ClassLoader jar,
            Function<YamlConfig, ? extends Config> readConfig) {
        return new ConfigFile(dataFolder.resolve(CONFIG), jar, dataFolder.resolve(BACKUPS), readConfig::apply);
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
