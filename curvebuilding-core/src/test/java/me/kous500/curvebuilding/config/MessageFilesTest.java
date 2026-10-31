package me.kous500.curvebuilding.config;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks that every language file of the Bukkit and Fabric modules has the messages of {@link Message}, with the same
 * placeholders as en-US.yml.
 */
class MessageFilesTest {
    private static final Path BUKKIT_MESSAGES = Path.of("..", "curvebuilding-bukkit", "src", "main", "resources", "messages");
    private static final Path FABRIC_MESSAGES = Path.of("..", "curvebuilding-fabric", "src", "main", "resources", "messages");
    private static final String BASE_FILE = "en-US.yml";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");

    @TestFactory
    Stream<DynamicTest> everyLanguageHasSameKeysAsEnglish() {
        return Stream.of(BUKKIT_MESSAGES, FABRIC_MESSAGES).flatMap(dir -> {
            Set<String> expected = keys(dir.resolve(BASE_FILE));
            assertFalse(expected.isEmpty(), dir + " has no message keys");
            return languageFiles(dir).map(file -> DynamicTest.dynamicTest(
                    dir.getName(1) + "/" + file.getFileName(),
                    () -> assertEquals(expected, keys(file))
            ));
        });
    }

    @TestFactory
    Stream<DynamicTest> everyLanguageHasSamePlaceholdersAsEnglish() {
        return Stream.of(BUKKIT_MESSAGES, FABRIC_MESSAGES).flatMap(dir -> {
            Map<String, Set<String>> expected = placeholders(dir.resolve(BASE_FILE));
            return languageFiles(dir).map(file -> DynamicTest.dynamicTest(
                    dir.getName(1) + "/" + file.getFileName(),
                    () -> assertEquals(expected, placeholders(file))
            ));
        });
    }

    @Test
    void englishHasEveryMessageInTheCode() {
        Set<String> inCode = Arrays.stream(Message.values()).map(message -> message.path).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(inCode, keys(BUKKIT_MESSAGES.resolve(BASE_FILE)));
    }

    @Test
    void bukkitAndFabricHaveSameKeys() {
        assertEquals(keys(BUKKIT_MESSAGES.resolve(BASE_FILE)), keys(FABRIC_MESSAGES.resolve(BASE_FILE)));
    }

    private static Stream<Path> languageFiles(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".yml")).sorted().toList().stream();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list " + dir, e);
        }
    }

    private static Set<String> keys(Path file) {
        return new TreeSet<>(messages(file).keySet());
    }

    /** The placeholders in each message, such as {@code {0}}. */
    private static Map<String, Set<String>> placeholders(Path file) {
        Map<String, Set<String>> placeholders = new TreeMap<>();
        messages(file).forEach((key, message) -> {
            Set<String> found = new TreeSet<>();
            Matcher matcher = PLACEHOLDER.matcher(message);
            while (matcher.find()) found.add(matcher.group());
            placeholders.put(key, found);
        });
        return placeholders;
    }

    /** Every message in the file by its path, such as {@code messages.pos-set}. */
    private static Map<String, String> messages(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, String> messages = new TreeMap<>();
            collectMessages("", new Yaml().load(reader), messages);
            return messages;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    private static void collectMessages(String prefix, Object node, Map<String, String> messages) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
                collectMessages(key, entry.getValue(), messages);
            }
        } else {
            messages.put(prefix, String.valueOf(node));
        }
    }
}
