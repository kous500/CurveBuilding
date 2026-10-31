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

import static me.kous500.curvebuilding.CurveBuilding.console;

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
                FileSystem fileSystem;
                boolean opened;
                try {
                    fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    opened = true;
                } catch (FileSystemAlreadyExistsException e) {
                    // Fabric Loader keeps the mod jar open. Reuse it and leave it open, or the loader breaks.
                    fileSystem = FileSystems.getFileSystem(uri);
                    opened = false;
                }

                try {
                    resourcePath = fileSystem.getPath(resourceDir);
                    try (Stream<Path> walk = Files.walk(resourcePath, 1)) {
                        walk.filter(Files::isRegularFile)
                                .forEach(path -> {
                                    String relativePath = resourceDir + "/" + path.getFileName().toString();
                                    create(relativePath);
                                });
                    }
                } finally {
                    if (opened) fileSystem.close();
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
            console.warning("Could not copy the default files in " + resourceDir + " from the jar", e);
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
            console.warning("Could not copy the default " + resourcePath + " from the jar into " + DATA_FOLDER, e);
        } catch (NullPointerException e) {
            console.warning(e.getMessage());
        }
    }
}