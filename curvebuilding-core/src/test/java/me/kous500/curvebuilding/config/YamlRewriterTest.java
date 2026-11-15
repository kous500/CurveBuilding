package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.RecordingConsole;
import me.kous500.curvebuilding.log.Console;
import me.kous500.curvebuilding.log.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rewrites a config file in the layout of a template in a stand-in for the jar. */
class YamlRewriterTest {
    private static final String TEMPLATE = """
            # Message file path
            message-file: "%datafolder%/messages/en-US.yml"

            curve:
              # Fineness of the curve
              fineness: 10
              # Maximum value of pos
              max-pos-value: 100
              # If this value is -1, it means unlimited
              default-max-change-limit: -1
            color: 0x00FFFF
            """;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T10:30:00Z"), ZoneOffset.UTC);
    private static final String BACKUP_NAME = "config-2026-11-15_10-30-00.yml";

    @TempDir
    Path temp;

    private Path jar;
    private Path file;
    private Path backups;
    private Console previousConsole;
    private RecordingConsole console;

    @BeforeEach
    void setUp() throws IOException {
        previousConsole = Log.console();
        console = new RecordingConsole();
        Log.setConsole(console);

        jar = temp.resolve("jar");
        file = temp.resolve("data/config.yml");
        backups = temp.resolve("data/backups");
        write(jar.resolve("config.yml"), TEMPLATE);
    }

    @AfterEach
    void restoreConsole() {
        Log.setConsole(previousConsole);
    }

    @Test
    void missingFileGetsTheTemplateWithoutABackup() throws IOException {
        rewrite();

        assertEquals(TEMPLATE, Files.readString(file));
        assertFalse(Files.exists(backups));
        assertEquals(List.of(), console.entries);
    }

    @Test
    void fileInTheLayoutOfTheTemplateIsLeftAsItIs() throws IOException {
        String content = TEMPLATE.replace("fineness: 10", "fineness: 20").replace("\n", "\r\n");
        write(file, content);

        rewrite();

        assertEquals(content, Files.readString(file), "only the line breaks differ");
        assertFalse(Files.exists(backups));
        assertEquals(List.of(), console.entries);
    }

    @Test
    void missingValuesAreWrittenWithTheCommentsOfTheTemplate() throws IOException {
        String content = """
                message-file: "%datafolder%/messages/ja-JP.yml"
                curve:
                  fineness: 20
                  default-max-change-limit: -1
                color: 0x00FFFF
                """;
        write(file, content);

        rewrite();

        assertEquals(TEMPLATE.replace("en-US", "ja-JP").replace("fineness: 10", "fineness: 20"),
                Files.readString(file));
        assertEquals(content, Files.readString(backups.resolve(BACKUP_NAME)));
        assertLogs(
                "info: " + file + " did not have curve.max-pos-value, so the default 100 was written",
                "info: " + file + " was rewritten. The previous file is saved as " + backups.resolve(BACKUP_NAME));
    }

    @Test
    void commentsAndKeysThatTheTemplateDoesNotHaveAreLeftOut() throws IOException {
        write(file, """
                # My settings
                message-file: '%datafolder%/messages/ja-JP.yml'
                curve:
                  # Smoother
                  fineness: 20   # raised
                  finess: 30
                  max-pos-value: 100
                  default-max-change-limit: -1
                color: 0X123456
                my-key: 1
                """);

        rewrite();

        String expected = TEMPLATE
                .replace("\"%datafolder%/messages/en-US.yml\"", "'%datafolder%/messages/ja-JP.yml'")
                .replace("fineness: 10", "fineness: 20")
                .replace("0x00FFFF", "0X123456");
        assertEquals(expected, Files.readString(file), "the values are kept as they are written");
        assertLogs("info: " + file + " was rewritten. The previous file is saved as " + backups.resolve(BACKUP_NAME));
    }

    @Test
    void invalidValuesAreTheDefaultsWithAWarning() throws IOException {
        String content = TEMPLATE.replace("fineness: 10", "fineness: abc");
        write(file, content.replace("max-pos-value: 100", "max-pos-value: 0"));

        rewrite();

        assertEquals(TEMPLATE, Files.readString(file));
        assertLogs(
                "warning: curve.fineness in " + file + " was not an integer: abc, so the default 10 was written",
                "warning: curve.max-pos-value in " + file + " was out of range: 0, so the default 100 was written",
                "info: " + file + " was rewritten. The previous file is saved as " + backups.resolve(BACKUP_NAME));
    }

    @Test
    void negativeChangeLimitMeansNoLimitAndStays() throws IOException {
        String content = TEMPLATE.replace("default-max-change-limit: -1", "default-max-change-limit: -2");
        write(file, content);

        rewrite();

        assertEquals(content, Files.readString(file));
        assertEquals(List.of(), console.entries);
    }

    @Test
    void emptyValueIsMissing() throws IOException {
        write(file, TEMPLATE.replace("fineness: 10", "fineness:"));

        rewrite();

        assertEquals(TEMPLATE, Files.readString(file));
        assertTrue(console.entries.getFirst().message().contains("did not have curve.fineness"),
                String.valueOf(console.entries));
    }

    @Test
    void fileThatIsNotYamlIsMovedAside() throws IOException {
        String content = "curve:\n  fineness: [10\n";
        write(file, content);

        rewrite();

        assertEquals(TEMPLATE, Files.readString(file));
        assertEquals(content, Files.readString(backups.resolve(BACKUP_NAME)));
        assertEquals(1, console.entries.size(), String.valueOf(console.entries));
        String message = console.entries.getFirst().message();
        assertTrue(message.startsWith(file + " could not be read as YAML, so it was moved to "
                + backups.resolve(BACKUP_NAME) + " and the defaults were written"), message);
        assertTrue(message.contains("line"), "says where the error is: " + message);
    }

    @Test
    void fileWithoutKeysAndValuesIsMovedAside() throws IOException {
        write(file, "- a\n- b\n");

        rewrite();

        assertEquals(TEMPLATE, Files.readString(file));
        assertEquals("- a\n- b\n", Files.readString(backups.resolve(BACKUP_NAME)));
        assertLogs("warning: " + file + " does not hold keys and values, so it was moved to "
                + backups.resolve(BACKUP_NAME) + " and the defaults were written");
    }

    @Test
    void backupsOfTheSameSecondAreNumbered() throws IOException {
        write(backups.resolve(BACKUP_NAME), "older");
        write(file, "curve:\n  fineness: 20\n");

        rewrite();

        assertEquals("older", Files.readString(backups.resolve(BACKUP_NAME)));
        assertEquals("curve:\n  fineness: 20\n",
                Files.readString(backups.resolve("config-2026-11-15_10-30-00-1.yml")));
    }

    @Test
    void fileIsNotRewrittenWithoutABackup() throws IOException {
        // A file in place of the backup folder
        write(backups, "");
        String content = "curve:\n  fineness: 20\n";
        write(file, content);

        rewrite();

        assertEquals(content, Files.readString(file));
        assertEquals(1, console.entries.size(), String.valueOf(console.entries));
        assertTrue(console.entries.getFirst().message().endsWith("so it was not rewritten"),
                console.entries.getFirst().message());
    }

    /** The default files of the platforms stay as they are, so that a new installation gets no backups. */
    @TestFactory
    Stream<DynamicTest> defaultFilesAreAlreadyInTheirLayout() {
        List<Path> defaults;
        try (Stream<Path> files = Stream.of("curvebuilding-bukkit", "curvebuilding-fabric")
                .map(module -> Path.of("..", module, "src", "main", "resources"))
                .flatMap(YamlRewriterTest::yamlFiles)) {
            defaults = files.toList();
        }
        return defaults.stream().map(resource -> DynamicTest.dynamicTest(
                resource.getName(1) + "/" + resource.getFileName(),
                () -> {
                    String content = Files.readString(resource, StandardCharsets.UTF_8);
                    Path jarFolder = temp.resolve(resource.getName(1).toString());
                    write(jarFolder.resolve("default.yml"), content);
                    Path copy = Files.createDirectories(temp.resolve("copy")).resolve(resource.getFileName());
                    write(copy, content);

                    try (URLClassLoader classLoader = classLoader(jarFolder)) {
                        YamlRewriter.rewrite(copy, classLoader, "default.yml", backups, config -> { }, CLOCK);
                    }

                    assertEquals(content, Files.readString(copy, StandardCharsets.UTF_8));
                    assertFalse(Files.exists(backups));
                }));
    }

    private static Stream<Path> yamlFiles(Path resources) {
        try (Stream<Path> files = Files.walk(resources)) {
            return files.filter(path -> path.toString().endsWith(".yml"))
                    .filter(path -> !path.getFileName().toString().equals("plugin.yml"))
                    .sorted()
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list " + resources, e);
        }
    }

    private void rewrite() throws IOException {
        Consumer<YamlConfig> read = Config::new;
        try (URLClassLoader classLoader = classLoader(jar)) {
            YamlRewriter.rewrite(file, classLoader, "config.yml", backups, read, CLOCK);
        }
    }

    private void assertLogs(String... expected) {
        List<String> logs = console.entries.stream().map(entry -> entry.level() + ": " + entry.message()).toList();
        assertEquals(List.of(expected), logs);
    }

    private static URLClassLoader classLoader(Path folder) throws IOException {
        return new URLClassLoader(new URL[]{folder.toUri().toURL()}, null);
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
