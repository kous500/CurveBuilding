package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.Console;
import me.kous500.curvebuilding.CurveBuilding;
import me.kous500.curvebuilding.RecordingConsole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads a config file with mistakes in it, with the defaults in a stand-in for the jar. */
class YamlConfigTest {
    @TempDir
    Path temp;

    private Path jar;
    private Path file;
    private Console previousConsole;
    private RecordingConsole console;

    @BeforeEach
    void setUp() throws IOException {
        previousConsole = CurveBuilding.console;
        console = new RecordingConsole();
        CurveBuilding.setConsole(console);

        jar = temp.resolve("jar");
        file = temp.resolve("config.yml");
        Files.createDirectories(jar);
        Files.writeString(jar.resolve("config.yml"), """
                text: default
                number: 3
                flag: true
                color: 0xFF00FFFF
                """, StandardCharsets.UTF_8);
    }

    @AfterEach
    void restoreConsole() {
        CurveBuilding.setConsole(previousConsole);
    }

    @Test
    void validValuesAreUsedWithoutWarnings() throws IOException {
        YamlConfig config = load("""
                text: 12
                number: "5"
                flag: "FALSE"
                color: 0x80FF0000
                """);

        assertEquals("12", config.getString("text"));
        assertEquals(5, config.getInteger("number"));
        assertFalse(config.getBoolean("flag"));
        assertEquals(0x80FF0000, config.getArgb("color"));
        assertEquals(0xFF00FFFF, load("").getArgb("color"), "a default above the range of an int");
        assertEquals(0, console.entries.size(), String.valueOf(console.entries));
    }

    @Test
    void missingValuesAreTheDefaultsWithoutWarnings() throws IOException {
        YamlConfig config = load("other: 1\n");

        assertEquals("default", config.getString("text"));
        assertEquals(3, config.getInteger("number"));
        assertTrue(config.getBoolean("flag"));
        assertEquals(0, config.getInteger("not.in.the.defaults"));
        assertEquals(0, console.entries.size(), String.valueOf(console.entries));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "2.7", "3000000000", "true", "[1, 2]"})
    void invalidIntegerIsTheDefaultWithOneWarning(String value) throws IOException {
        YamlConfig config = load("number: " + value + "\n");

        assertEquals(3, config.getInteger("number"));
        assertEquals(3, config.getInteger("number"));
        assertOneWarning("number");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "abc", "\"yes please\""})
    void invalidBooleanIsTheDefaultWithOneWarning(String value) throws IOException {
        assertTrue(load("flag: " + value + "\n").getBoolean("flag"));
        assertOneWarning("flag");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0x1FFFFFFFF", "-0x100000000", "red"})
    void invalidColorIsTheDefaultWithOneWarning(String value) throws IOException {
        assertEquals(0xFF00FFFF, load("color: " + value + "\n").getArgb("color"));
        assertOneWarning("color");
    }

    @Test
    void textThatIsAListIsTheDefaultWithOneWarning() throws IOException {
        assertEquals("default", load("text: [a, b]\n").getString("text"));
        assertOneWarning("text");
    }

    @Test
    void brokenYamlIsLoggedWithWhereItIsAndTheDefaultsAreUsed() throws IOException {
        YamlConfig config = load("number: [5\nflag: false\n");

        assertEquals(3, config.getInteger("number"));
        assertTrue(config.getBoolean("flag"));
        RecordingConsole.Entry entry = assertOneWarning(file.toString());
        assertTrue(entry.message().contains("line"), entry.message());
    }

    @Test
    void fileWithoutKeysAndValuesIsLoggedAndTheDefaultsAreUsed() throws IOException {
        assertEquals(3, load("just some text\n").getInteger("number"));
        assertOneWarning(file.toString());
    }

    @Test
    void emptyFileIsNotLogged() throws IOException {
        assertEquals(3, load("").getInteger("number"));
        assertEquals(0, console.entries.size(), String.valueOf(console.entries));
    }

    private YamlConfig load(String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, null)) {
            return YamlConfig.load(file, classLoader, "config.yml");
        }
    }

    private RecordingConsole.Entry assertOneWarning(String containing) {
        assertEquals(1, console.entries.size(), String.valueOf(console.entries));
        RecordingConsole.Entry entry = console.entries.get(0);
        assertEquals("warning", entry.level());
        assertTrue(entry.message().contains(containing), entry.message());
        return entry;
    }
}
