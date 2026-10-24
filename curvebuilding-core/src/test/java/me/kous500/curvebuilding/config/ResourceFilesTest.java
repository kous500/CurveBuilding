package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.MainInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Copies the default config and message files out of a mod jar, as on a real server or client.
 */
class ResourceFilesTest {
    @TempDir
    Path temp;

    private Path jar;
    private Path dataFolder;

    @BeforeEach
    void createJar() throws IOException {
        jar = temp.resolve("mod.jar");
        dataFolder = temp.resolve("config");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
            write(out, "config.yml", "message-file: en-US.yml\n");
            out.putNextEntry(new ZipEntry("messages/"));
            out.closeEntry();
            write(out, "messages/en-US.yml", "messages:\n  test: Test\n");
            write(out, "messages/ja-JP.yml", "messages:\n  test: テスト\n");
        }
    }

    @Test
    void copiesFilesFromJar() throws IOException {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            ResourceFiles.setup(initializer(classLoader));
        }

        assertCopied();
        // A file system opened only for copying is closed again
        assertThrows(FileSystemNotFoundException.class, () -> FileSystems.getFileSystem(jarUri()));
    }

    @Test
    void copiesFilesWhenJarIsAlreadyOpen() throws IOException {
        // Fabric Loader keeps the zip file system of the mod jar open while the game runs
        try (FileSystem loaderFileSystem = FileSystems.newFileSystem(jarUri(), Collections.emptyMap());
             URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            ResourceFiles.setup(initializer(classLoader));

            assertCopied();
            assertTrue(loaderFileSystem.isOpen(), "the file system opened by the loader must stay open");
        }
    }

    private void assertCopied() throws IOException {
        assertEquals("message-file: en-US.yml\n", Files.readString(dataFolder.resolve("config.yml")));
        assertTrue(Files.exists(dataFolder.resolve("messages/en-US.yml")));
        assertEquals("messages:\n  test: テスト\n", Files.readString(dataFolder.resolve("messages/ja-JP.yml"), StandardCharsets.UTF_8));
    }

    private URI jarUri() {
        return URI.create("jar:" + jar.toUri());
    }

    private MainInitializer initializer(ClassLoader classLoader) {
        return new MainInitializer() {
            @Override
            public ClassLoader getMainClassLoader() {
                return classLoader;
            }

            @Override
            public String getConfigPass() {
                return dataFolder.toString();
            }
        };
    }

    private static void write(ZipOutputStream out, String name, String content) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
