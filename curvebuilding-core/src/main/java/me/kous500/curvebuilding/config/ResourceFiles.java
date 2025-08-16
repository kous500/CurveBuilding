package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.MainInitializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceFiles {
    public static void setup(MainInitializer mainInitializer) {
        ResourceFiles resourceFiles = new ResourceFiles(mainInitializer);
        resourceFiles.copyResourceDirectory("messages");
        resourceFiles.create("config.yml");
    }

    private final String DATA_FOLDER;
    private final ClassLoader classLoader;

    private ResourceFiles(MainInitializer mainInitializer) {
        this.DATA_FOLDER = mainInitializer.getConfigPass();
        this.classLoader = mainInitializer.getMainClassLoader();
    }

    private void copyResourceDirectory(String resourceDir) {
        try {
            URI uri = Objects.requireNonNull(classLoader.getResource(resourceDir), "Resource directory not found: " + resourceDir).toURI();
            Path resourcePath;

            if ("jar".equals(uri.getScheme())) {
                try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    resourcePath = fileSystem.getPath(resourceDir);
                    try (Stream<Path> walk = Files.walk(resourcePath, 1)) {
                        walk.filter(Files::isRegularFile)
                                .forEach(path -> {
                                    String relativePath = resourceDir + "/" + path.getFileName().toString();
                                    create(relativePath);
                                });
                    }
                }
            } else {
                resourcePath = Paths.get(uri);
                try (Stream<Path> walk = Files.walk(resourcePath, 1)) {
                    walk.filter(Files::isRegularFile)
                            .forEach(path -> {
                                String relativePath = resourceDir + "/" + path.getFileName().toString();
                                create(relativePath);
                            });
                }
            }
        } catch (IOException | URISyntaxException | NullPointerException e) {
            System.err.println("Failed to copy resource directory: " + resourceDir);
            e.fillInStackTrace();
        }
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