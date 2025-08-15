package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.MainInitializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

public class ResourceFiles {
    public static void setup(MainInitializer mainInitializer) {
        ResourceFiles resourceFiles = new ResourceFiles(mainInitializer);
        for (String language : new String[]{"en-US", "es-ES", "es-419", "ja-JP", "zh-Hant", "zh-CN"}) {
            resourceFiles.create("messages/" + language + ".yml");
        }
        resourceFiles.create("config.yml");
    }

    private final String DATA_FOLDER;
    private final ClassLoader classLoader;

    private ResourceFiles(MainInitializer mainInitializer) {
        this.DATA_FOLDER = mainInitializer.getConfigPass();
        this.classLoader = mainInitializer.getMainClassLoader();
    }

    private void create(String resourcePath) {
        try {
            File destinationFile = new File(DATA_FOLDER, resourcePath);
            if (!destinationFile.exists()) {
                InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
                Objects.requireNonNull(inputStream, "Resource not found in JAR: " + resourcePath);
                File parentDir = destinationFile.getParentFile();
                if (parentDir != null) {
                    parentDir.mkdirs();
                }
                Files.copy(inputStream, destinationFile.toPath());
                inputStream.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to create resource file: " + resourcePath);
            e.fillInStackTrace();
        } catch (NullPointerException e) {
            System.err.println(e.getMessage());
        }
    }
}