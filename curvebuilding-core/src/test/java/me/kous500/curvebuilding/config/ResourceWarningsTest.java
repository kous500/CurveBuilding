package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.Console;
import me.kous500.curvebuilding.CurveBuilding;
import me.kous500.curvebuilding.MainInitializer;
import me.kous500.curvebuilding.RecordingConsole;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The files that cannot be read or copied are logged as warnings, with what went wrong. */
class ResourceWarningsTest {
    @TempDir
    Path temp;

    private Path jar;
    private Path dataFolder;
    private Console previousConsole;
    private RecordingConsole console;

    @BeforeEach
    void setUp() throws IOException {
        previousConsole = CurveBuilding.console;
        console = new RecordingConsole();
        CurveBuilding.setConsole(console);

        jar = temp.resolve("jar");
        dataFolder = temp.resolve("data");
        Files.createDirectories(jar);
        Files.writeString(jar.resolve("config.yml"), "curve:\n  fineness: 3\n", StandardCharsets.UTF_8);
    }

    @AfterEach
    void restoreConsole() {
        CurveBuilding.setConsole(previousConsole);
    }

    @Test
    void unreadableFileIsLoggedAndTheDefaultsAreUsed() throws IOException {
        // A directory in place of the file cannot be opened for reading
        Path configFile = Files.createDirectories(dataFolder.resolve("config.yml"));

        YamlConfig config;
        try (URLClassLoader classLoader = classLoader()) {
            config = YamlConfig.loadConfiguration(configFile.toFile(), ResourceType.config, initializer(classLoader));
        }

        assertEquals(3, config.getInteger("curve.fineness"));
        assertEquals(1, console.entries.size(), String.valueOf(console.entries));
        RecordingConsole.Entry entry = console.entries.get(0);
        assertEquals("warning", entry.level());
        assertTrue(entry.message().contains(configFile.toString()), entry.message());
        assertNotNull(entry.cause());
    }

    @Test
    void missingDefaultFilesInTheJarAreLogged() throws IOException {
        // The jar has config.yml but no messages folder
        try (URLClassLoader classLoader = classLoader()) {
            ResourceFiles.setup(initializer(classLoader));
        }

        assertTrue(Files.exists(dataFolder.resolve("config.yml")), "the files that exist are still copied");
        assertEquals(1, console.entries.size(), String.valueOf(console.entries));
        RecordingConsole.Entry entry = console.entries.get(0);
        assertEquals("warning", entry.level());
        assertTrue(entry.message().contains("messages"), entry.message());
        assertNotNull(entry.cause());
    }

    private URLClassLoader classLoader() throws IOException {
        return new URLClassLoader(new URL[]{jar.toUri().toURL()}, null);
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
}
