package me.kous500.curvebuilding.commands.config;

import me.kous500.curvebuilding.RecordingConsole;
import me.kous500.curvebuilding.commands.config.ConfigCommand.Reply;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.ConfigFile;
import me.kous500.curvebuilding.log.Console;
import me.kous500.curvebuilding.log.Log;
import me.kous500.curvebuilding.message.Message;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Shows and sets the values of a config file, with config.yml in a stand-in for the jar. */
class ConfigCommandTest {
    private static final String TEMPLATE = """
            # Message file path
            message-file: "%datafolder%/messages/en-US.yml"

            curve:
              # Fineness of the curve
              fineness: 10
              max-set-length: 10000
              max-pos-value: 100
              default-max-change-limit: -1
            color:
              pos: 0x00FFFF
            """;

    @TempDir
    Path temp;

    private Path file;
    private URLClassLoader jar;
    private ConfigCommand command;
    private int reloads;
    private Console previousConsole;

    @BeforeEach
    void setUp() throws IOException {
        previousConsole = Log.console();
        Log.setConsole(new RecordingConsole());

        Path jarFolder = temp.resolve("jar");
        write(jarFolder.resolve("config.yml"), TEMPLATE);
        jar = new URLClassLoader(new URL[]{jarFolder.toUri().toURL()}, null);
        file = temp.resolve("data/config.yml");
        Path backups = temp.resolve("data/backups");
        ConfigFile configFile = new ConfigFile(file, jar, backups, Config::new);
        command = new ConfigCommand(configFile, List.of("curve", "color"), () -> reloads++);
    }

    @AfterEach
    void tearDown() throws IOException {
        jar.close();
        Log.setConsole(previousConsole);
    }

    @Test
    void keysAreThoseOfTheSections() {
        assertEquals(List.of(
                "curve.fineness",
                "curve.max-set-length",
                "curve.max-pos-value",
                "curve.default-max-change-limit",
                "color.pos"), command.keys());
    }

    @Test
    void valueIsShownAsItIsWritten() throws IOException {
        write(file, TEMPLATE.replace("fineness: 10", "fineness: 20").replace("0x00FFFF", "0X123456"));

        assertReply(Message.CONFIG_VALUE, false, List.of("curve.fineness", "20"), "curve.fineness");
        assertReply(Message.CONFIG_VALUE, false, List.of("color.pos", "0X123456"), "color.pos");
        assertEquals(0, reloads);
    }

    @Test
    void valueThatTheFileDoesNotHaveIsTheDefault() {
        assertReply(Message.CONFIG_VALUE, false, List.of("curve.fineness", "10"), "curve.fineness");
    }

    @Test
    void valueIsWrittenAndTheFilesAreReadAgain() throws IOException {
        write(file, TEMPLATE);

        assertReply(Message.CONFIG_SET, false, List.of("curve.fineness", "20"), "curve.fineness", "20");

        assertEquals(TEMPLATE.replace("fineness: 10", "fineness: 20"), Files.readString(file));
        assertEquals(1, reloads);
    }

    @Test
    void valueIsWrittenAsItIsTyped() throws IOException {
        write(file, TEMPLATE);

        assertReply(Message.CONFIG_SET, false, List.of("color.pos", "0xFF0000"), "color.pos", "0xFF0000");

        assertEquals(TEMPLATE.replace("0x00FFFF", "0xFF0000"), Files.readString(file));
    }

    @Test
    void defaultSetsTheDefault() throws IOException {
        write(file, TEMPLATE.replace("fineness: 10", "fineness: 20"));

        assertReply(Message.CONFIG_SET, false, List.of("curve.fineness", "10"), "curve.fineness", "default");

        assertEquals(TEMPLATE, Files.readString(file));
    }

    @Test
    void negativeChangeLimitMeansNoLimit() throws IOException {
        write(file, TEMPLATE);

        assertReply(Message.CONFIG_SET, false, List.of("curve.default-max-change-limit", "-5"),
                "curve.default-max-change-limit", "-5");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "abc", "2.5", "null", "[1]", "{a: 1}", "3000000000"})
    void invalidValueIsNotWritten(String value) throws IOException {
        String content = TEMPLATE.replace("fineness: 10", "fineness: 20");
        write(file, content);

        assertReply(Message.CONFIG_INVALID_VALUE, true, List.of("curve.fineness", value), "curve.fineness", value);

        assertEquals(content, Files.readString(file));
        assertEquals(0, reloads);
    }

    @ParameterizedTest
    @ValueSource(strings = {"message-file", "curve.finess", "curve", "particles.pos.density"})
    void keyOutsideTheSectionsIsUnknown(String key) {
        assertReply(Message.CONFIG_UNKNOWN_KEY, true, List.of(key), key, "1");
        assertReply(Message.CONFIG_UNKNOWN_KEY, true, List.of(key), key);
    }

    @Test
    void valueThatCannotBeSavedIsReported() throws IOException {
        // A file in place of the backup folder, and a file that needs a backup before it is rewritten
        write(temp.resolve("data/backups"), "");
        write(file, "curve:\n  fineness: 20\n");

        assertReply(Message.CONFIG_NOT_SAVED, true, List.of(), "curve.fineness", "30");

        assertEquals("curve:\n  fineness: 20\n", Files.readString(file));
        assertEquals(0, reloads);
    }

    @Test
    void withoutAKeyOrWithTooManyArgumentsThereIsNoReply() {
        assertNull(command.run(new String[0]));
        assertNull(command.run(new String[]{"curve.fineness", "1", "2"}));
    }

    @Test
    void keysAndValuesAreSuggested() throws IOException {
        write(file, TEMPLATE.replace("fineness: 10", "fineness: 20"));

        assertEquals(command.keys(), command.complete(new String[]{""}));
        assertEquals(List.of("curve.fineness"), command.complete(new String[]{"curve.f"}));
        assertEquals(List.of("20", "default"), command.complete(new String[]{"curve.fineness", ""}));
        assertEquals(List.of("default"), command.complete(new String[]{"curve.fineness", "d"}));
        assertEquals(List.of(), command.complete(new String[]{"message-file", ""}));
        assertEquals(List.of(), command.complete(new String[]{"curve.fineness", "1", ""}));
    }

    private void assertReply(Message message, boolean error, List<Object> args, String... input) {
        Reply reply = command.run(input);
        assertNotNull(reply);
        assertEquals(message, reply.message());
        assertEquals(error, reply.error());
        assertEquals(args, List.of(reply.args()));
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
