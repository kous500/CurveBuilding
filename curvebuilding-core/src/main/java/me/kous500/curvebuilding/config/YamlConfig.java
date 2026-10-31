package me.kous500.curvebuilding.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static me.kous500.curvebuilding.CurveBuilding.console;

/**
 * The values of a YAML file, with those missing from it taken from a default file in the jar. A value of the wrong
 * type is logged once as a warning, and the default is used instead.
 */
public class YamlConfig {
    private final Path file;
    private final Map<?, ?> values;
    private final Map<?, ?> defaults;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    private YamlConfig(Path file, Map<?, ?> values, Map<?, ?> defaults) {
        this.file = file;
        this.values = values;
        this.defaults = defaults;
    }

    /**
     * Reads {@code file}, with the resource {@code defaultResource} of {@code jar} for the values missing from it. A file
     * that does not exist, or cannot be read, has no values of its own.
     */
    public static YamlConfig load(Path file, ClassLoader jar, String defaultResource) {
        return new YamlConfig(file, readFile(file), readResource(jar, defaultResource));
    }

    private static Map<?, ?> readFile(Path file) {
        if (!Files.exists(file)) {
            return Map.of();
        }

        Object loaded;
        try {
            // Read the bytes first, so that an error reading the file is not wrapped by the YAML parser
            loaded = new Yaml().load(new ByteArrayInputStream(Files.readAllBytes(file)));
        } catch (IOException e) {
            console.warning("Could not read " + file + ", so the defaults are used", e);
            return Map.of();
        } catch (YAMLException e) {
            // The message says where in the file the error is; the stack trace would not help
            console.warning("Could not read " + file + " as YAML, so the defaults are used:\n" + e.getMessage());
            return Map.of();
        }

        if (loaded instanceof Map<?, ?> map) {
            return map;
        }
        if (loaded != null) {
            console.warning(file + " does not hold keys and values, so the defaults are used");
        }
        return Map.of();
    }

    private static Map<?, ?> readResource(ClassLoader jar, String resource) {
        try (InputStream stream = jar.getResourceAsStream(resource)) {
            return stream != null && new Yaml().load(stream) instanceof Map<?, ?> map ? map : Map.of();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load default resource: " + resource, e);
        }
    }

    public String getString(String path) {
        return get(path, "text", YamlConfig::toText, null);
    }

    public int getInteger(String path) {
        return get(path, "an integer", YamlConfig::toInteger, 0);
    }

    /** A color with its alpha, such as {@code 0x80FF0000}, which is above the range of an int as a number. */
    public int getArgb(String path) {
        return get(path, "a color such as 0xFFFFFFFF", YamlConfig::toArgb, 0);
    }

    public boolean getBoolean(String path) {
        return get(path, "true or false", YamlConfig::toBoolean, false);
    }

    /**
     * The value at {@code path}, with its keys separated by dots, made by {@code convert}, which returns null for a value
     * of the wrong type. Without a value in the file, or with a wrong one, the default; without a default, {@code fallback}.
     */
    private <T> T get(String path, String expected, Function<Object, T> convert, T fallback) {
        Optional<Object> value = find(values, path);
        if (value.isPresent()) {
            T converted = convert.apply(value.get());
            if (converted != null) return converted;
            if (warned.add(path)) {
                console.warning(path + " in " + file + " is not " + expected + ": " + value.get() + ", so the default is used");
            }
        }
        return find(defaults, path).map(convert).orElse(fallback);
    }

    private static Optional<Object> find(Map<?, ?> map, String path) {
        Object currentValue = map;
        for (String segment : path.split("\\.")) {
            if (currentValue instanceof Map<?, ?> currentMap) {
                currentValue = currentMap.get(segment);
            } else {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(currentValue);
    }

    private static String toText(Object value) {
        return value instanceof Map || value instanceof Collection ? null : value.toString();
    }

    private static Integer toInteger(Object value) {
        try {
            if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
                return new BigInteger(value.toString()).intValueExact();
            }
            if (value instanceof String text) {
                return Integer.parseInt(text);
            }
        } catch (ArithmeticException | NumberFormatException e) {
            // Out of the range of an int, or not a number
        }
        return null;
    }

    private static Integer toArgb(Object value) {
        if (value instanceof Integer argb) {
            return argb;
        }
        if (value instanceof Long argb && argb >= 0 && argb <= 0xFFFFFFFFL) {
            return (int) (long) argb;
        }
        return null;
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false"))) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }
}
