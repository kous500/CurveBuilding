package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.fabric.MainInitializer;
import me.kous500.curvebuilding.fabric.commands.ResourceType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ResourceFiles {
    public static ResourceFiles load(MainInitializer mainInitializer) {
        ResourceFiles resourceFiles = new ResourceFiles(mainInitializer);

        for (String language : new String[]{"en", "ja"}) {
            resourceFiles.create("messages/" + language + ".yml");
        }

        resourceFiles.create("config.yml");

        return resourceFiles;
    }

    private final MainInitializer mainInitializer;
    private final String DATA_FOLDER;
    private final ClassLoader classLoader;

    public ResourceFiles(MainInitializer mainInitializer) {
        this.mainInitializer = mainInitializer;
        this.DATA_FOLDER = mainInitializer.getDateFolder();
        this.classLoader = mainInitializer.getClassLoader();
    }

    public void create(String file) {
        try {
            final File configFile = new File(DATA_FOLDER + "/" + file);

            if (!configFile.exists()) {
                final InputStream inputStream = classLoader.getResourceAsStream(file);
                final File parentFile = configFile.getParentFile();

                if (parentFile != null) parentFile.mkdirs();

                if (inputStream != null) {
                    Files.copy(inputStream, configFile.toPath());
                } else configFile.createNewFile();
            }
        } catch (final IOException ignored) {}
    }

    public YamlConfig get(String file, ResourceType resourceType) {
        file = file.replace("%datafolder%", DATA_FOLDER);

        final File messageFile = new File(file);

        return messageFile.exists()
                ? YamlConfig.loadConfiguration(messageFile, resourceType, mainInitializer)
                : new YamlConfig();
    }
}