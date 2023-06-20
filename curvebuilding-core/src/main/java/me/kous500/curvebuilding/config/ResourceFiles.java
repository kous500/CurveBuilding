package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.MainInitializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ResourceFiles {
    public static ResourceFiles load(MainInitializer mainInitializer) {
        ResourceFiles resourceFiles = new ResourceFiles(mainInitializer);

        for (String language : new String[]{"en", "ja", "zh-Hant"}) {
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
        this.DATA_FOLDER = mainInitializer.getConfigPass();
        this.classLoader = mainInitializer.getMainClassLoader();
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

    public YamlConfig get(String fileName, ResourceType resourceType) {
        fileName = fileName.replace("%datafolder%", DATA_FOLDER);

        final File file = new File(fileName);

        return file.exists()
                ? YamlConfig.loadConfiguration(file, resourceType, mainInitializer)
                : new YamlConfig(resourceType);
    }
}