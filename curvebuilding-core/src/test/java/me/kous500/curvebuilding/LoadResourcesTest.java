package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.log.Console;
import me.kous500.curvebuilding.log.Log;
import me.kous500.curvebuilding.message.Message;
import me.kous500.curvebuilding.message.Messages;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loads the config and the message file from a data folder, with the defaults in a stand-in for the jar. */
class LoadResourcesTest {
    @TempDir
    Path temp;

    private Path jar;
    private Path dataFolder;
    private Console previousConsole;
    private RecordingConsole console;

    @BeforeEach
    void createJar() throws IOException {
        previousConsole = Log.console();
        console = new RecordingConsole();
        Log.setConsole(console);
        jar = temp.resolve("jar");
        dataFolder = temp.resolve("data");
        write(jar.resolve("config.yml"), "message-file: \"%datafolder%/messages/ja-JP.yml\"\ncurve:\n  fineness: 3\n");
        write(jar.resolve("messages/en-US.yml"), "messages:\n  pos-shift: Shifted\n  pos-remove: Removed {0}\n");
        write(jar.resolve("messages/ja-JP.yml"), "messages:\n  pos-shift: シフト\n  pos-remove: \"{0} を削除\"\n");
    }

    @AfterEach
    void restoreConsole() {
        Log.setConsole(previousConsole);
    }

    @Test
    void loadsTheConfigAndTheMessageFileItNames() throws IOException {
        // A message file in the data folder that misses a message, which then comes from the jar's file of that name
        write(dataFolder.resolve("messages/ja-JP.yml"), "messages:\n  pos-shift: 移動しました\n");

        Config config;
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            config = CurveBuilding.loadResources(dataFolder, classLoader, Config::new);
        }

        assertEquals(3, config.fineness);
        assertTrue(Files.exists(dataFolder.resolve("config.yml")), "the default config is copied");
        assertEquals("移動しました", Messages.getMessage(Message.POS_SHIFT));
        assertEquals("2 を削除", Messages.getMessage(Message.POS_REMOVE, 2));
    }

    @Test
    void messagesMissingFromAFileTheJarDoesNotHaveComeFromEnUs() throws IOException {
        write(dataFolder.resolve("config.yml"), "message-file: \"%datafolder%/custom.yml\"\n");
        write(dataFolder.resolve("custom.yml"), "messages:\n  pos-shift: Moved\n");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            CurveBuilding.loadResources(dataFolder, classLoader, Config::new);
        }

        assertEquals("Moved", Messages.getMessage(Message.POS_SHIFT));
        assertEquals("Removed 2", Messages.getMessage(Message.POS_REMOVE, 2));
        assertEquals(0, console.entries.size(), String.valueOf(console.entries));
    }

    @Test
    void missingMessageFileIsLoggedAndTheJarsFileIsUsed() throws IOException {
        write(dataFolder.resolve("config.yml"), "message-file: \"%datafolder%/ja-JP.yml\"\n");

        load();

        assertEquals("シフト", Messages.getMessage(Message.POS_SHIFT));
        assertOneWarning("ja-JP.yml");
    }

    @Test
    void messageFileThatIsNotAPathIsLoggedAndEnglishIsUsed() throws IOException {
        write(dataFolder.resolve("config.yml"), "message-file: \"%datafolder%/a\\0.yml\"\n");

        load();

        assertEquals("Shifted", Messages.getMessage(Message.POS_SHIFT));
        assertOneWarning("message-file");
    }

    private void load() throws IOException {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            CurveBuilding.loadResources(dataFolder, classLoader, Config::new);
        }
    }

    private void assertOneWarning(String containing) {
        assertEquals(1, console.entries.size(), String.valueOf(console.entries));
        assertEquals("warning", console.entries.get(0).level());
        assertTrue(console.entries.get(0).message().contains(containing), console.entries.get(0).message());
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
