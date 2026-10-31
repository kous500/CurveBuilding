package me.kous500.curvebuilding.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static me.kous500.curvebuilding.CurveBuilding.console;

/** Copies the default files that the data folder does not have yet out of the jar. */
public final class ResourceFiles {
    private ResourceFiles() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void copyDefaults(Path dataFolder, ClassLoader jar) {
        copyDirectory(dataFolder, jar, "messages");
        copy(dataFolder, jar, "config.yml");
    }

    private static void copyDirectory(Path dataFolder, ClassLoader jar, String directory) {
        try {
            URL url = jar.getResource(directory);
            if (url == null) {
                throw new FileNotFoundException(directory + " is not in the jar");
            }
            URI uri = url.toURI();
            if (!"jar".equals(uri.getScheme())) {
                copyFiles(dataFolder, jar, directory, Path.of(uri));
                return;
            }

            FileSystem fileSystem;
            boolean opened;
            try {
                fileSystem = FileSystems.newFileSystem(uri, Map.of());
                opened = true;
            } catch (FileSystemAlreadyExistsException e) {
                // Fabric Loader keeps the mod jar open. Reuse it and leave it open, or the loader breaks.
                fileSystem = FileSystems.getFileSystem(uri);
                opened = false;
            }
            try {
                copyFiles(dataFolder, jar, directory, fileSystem.getPath(directory));
            } finally {
                if (opened) fileSystem.close();
            }
        } catch (IOException | URISyntaxException e) {
            console.warning("Could not copy the default files in " + directory + " from the jar", e);
        }
    }

    /** Copies the files in {@code path}, the directory {@code directory} of the jar. */
    private static void copyFiles(Path dataFolder, ClassLoader jar, String directory, Path path) throws IOException {
        try (Stream<Path> files = Files.list(path)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> copy(dataFolder, jar, directory + "/" + file.getFileName()));
        }
    }

    private static void copy(Path dataFolder, ClassLoader jar, String resource) {
        Path destination = dataFolder.resolve(resource);
        if (Files.exists(destination)) {
            return;
        }
        try (InputStream stream = jar.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new FileNotFoundException(resource + " is not in the jar");
            }
            Files.createDirectories(destination.getParent());
            Files.copy(stream, destination);
        } catch (IOException e) {
            console.warning("Could not copy the default " + resource + " from the jar into " + dataFolder, e);
        }
    }
}
