package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loads the config and the message file from a data folder, with the defaults in a stand-in for the jar. */
class LoadResourcesTest {
    @TempDir
    Path temp;

    private Path jar;
    private Path dataFolder;
    private Config previousConfig;

    @BeforeEach
    void createJar() throws IOException {
        previousConfig = CurveBuilding.config;
        jar = temp.resolve("jar");
        dataFolder = temp.resolve("data");
        write(jar.resolve("config.yml"), "message-file: \"%datafolder%/messages/ja-JP.yml\"\ncurve:\n  fineness: 3\n");
        write(jar.resolve("messages/en-US.yml"), "messages:\n  pos-shift: Shifted\n  pos-remove: Removed {0}\n");
        write(jar.resolve("messages/ja-JP.yml"), "messages:\n  pos-shift: シフト\n  pos-remove: \"{0} を削除\"\n");
    }

    @AfterEach
    void restoreConfig() {
        CurveBuilding.config = previousConfig;
    }

    @Test
    void loadsTheConfigAndTheMessageFileItNames() throws IOException {
        // A message file in the data folder that misses a message, which then comes from the jar's file of that name
        write(dataFolder.resolve("messages/ja-JP.yml"), "messages:\n  pos-shift: 移動しました\n");

        Config config;
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            config = CurveBuilding.loadResources(dataFolder, classLoader, Config::new);
        }

        assertSame(config, CurveBuilding.config);
        assertEquals(3, config.fineness);
        assertTrue(Files.exists(dataFolder.resolve("config.yml")), "the default config is copied");
        assertEquals("移動しました", CurveBuilding.getMessage(Message.POS_SHIFT));
        assertEquals("2 を削除", CurveBuilding.getMessage(Message.POS_REMOVE, 2));
    }

    @Test
    void messagesMissingFromAFileTheJarDoesNotHaveComeFromEnUs() throws IOException {
        write(dataFolder.resolve("config.yml"), "message-file: \"%datafolder%/custom.yml\"\n");
        write(dataFolder.resolve("custom.yml"), "messages:\n  pos-shift: Moved\n");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            CurveBuilding.loadResources(dataFolder, classLoader, Config::new);
        }

        assertEquals("Moved", CurveBuilding.getMessage(Message.POS_SHIFT));
        assertEquals("Removed 2", CurveBuilding.getMessage(Message.POS_REMOVE, 2));
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
