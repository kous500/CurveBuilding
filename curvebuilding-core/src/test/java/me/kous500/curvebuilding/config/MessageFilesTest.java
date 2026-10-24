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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks that every language file of the Bukkit and Fabric modules has the same keys as en-US.yml.
 */
class MessageFilesTest {
    private static final Path BUKKIT_MESSAGES = Path.of("..", "curvebuilding-bukkit", "src", "main", "resources", "messages");
    private static final Path FABRIC_MESSAGES = Path.of("..", "curvebuilding-fabric", "src", "main", "resources", "messages");
    private static final String BASE_FILE = "en-US.yml";

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
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Set<String> keys = new TreeSet<>();
            collectKeys("", new Yaml().load(reader), keys);
            return keys;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    private static void collectKeys(String prefix, Object node, Set<String> keys) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
                collectKeys(key, entry.getValue(), keys);
            }
        } else {
            keys.add(prefix);
        }
    }
}
