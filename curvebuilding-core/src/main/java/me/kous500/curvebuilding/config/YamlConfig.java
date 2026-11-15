package me.kous500.curvebuilding.config;

import org.jspecify.annotations.Nullable;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static me.kous500.curvebuilding.log.Log.console;

/**
 * The values of a YAML file, with those missing from it taken from a default file in the jar. A value of the wrong
 * type or out of its range is logged once as a warning, and the default is used instead.
 */
public class YamlConfig {
    /**
     * A value that could not be used.
     *
     * @param found the value in the file
     * @param problem what is wrong with it, to follow "is" or "was", such as {@code not an integer}
     */
    record InvalidValue(Object found, String problem) {
    }

    private final Path file;
    private final Map<?, ?> values;
    private final Map<?, ?> defaults;
    private final boolean logsInvalidValues;
    private final Map<String, InvalidValue> invalidValues = Collections.synchronizedMap(new LinkedHashMap<>());

    private YamlConfig(Path file, Map<?, ?> values, Map<?, ?> defaults, boolean logsInvalidValues) {
        this.file = file;
        this.values = values;
        this.defaults = defaults;
        this.logsInvalidValues = logsInvalidValues;
    }

    /**
     * Reads {@code file}, with the resource {@code defaultResource} of {@code jar} for the values missing from it. A
     * file that does not exist, or cannot be read, has no values of its own.
     *
     * @param file the YAML file
     * @param jar the class loader that has the default file
     * @param defaultResource the name of the default file in the jar
     * @return the values
     */
    public static YamlConfig load(Path file, ClassLoader jar, String defaultResource) {
        return new YamlConfig(file, readFile(file), readResource(jar, defaultResource), true);
    }

    /**
     * The values {@code values} of {@code file}, with {@code defaults} for those missing. The invalid values are kept
     * for {@link #invalidValues} instead of being logged.
     *
     * @param file the YAML file, named in the descriptions of the values
     * @param values the values of the file
     * @param defaults the default values
     * @return the values
     */
    static YamlConfig withoutWarnings(Path file, Map<?, ?> values, Map<?, ?> defaults) {
        return new YamlConfig(file, values, defaults, false);
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
            console().warning("Could not read " + file + ", so the defaults are used", e);
            return Map.of();
        } catch (YAMLException e) {
            // The message says where in the file the error is; the stack trace would not help
            console().warning("Could not read " + file + " as YAML, so the defaults are used:\n" + e.getMessage());
            return Map.of();
        }

        if (loaded instanceof Map<?, ?> map) {
            return map;
        }
        if (loaded != null) {
            console().warning(file + " does not hold keys and values, so the defaults are used");
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

    /**
     * A value as text.
     *
     * @param path the keys of the value, separated by dots
     * @return the value, the default, or null when neither is there
     */
    public @Nullable String getString(String path) {
        return get(path, "text", YamlConfig::toText, value -> true, null);
    }

    /**
     * A value as an integer.
     *
     * @param path the keys of the value, separated by dots
     * @return the value, the default, or 0 when neither is there
     */
    public int getInteger(String path) {
        return get(path, "an integer", YamlConfig::toInteger, value -> true, 0);
    }

    /**
     * A value as an integer from {@code min} to {@code max}. A value out of that range is invalid, like one of the
     * wrong type.
     *
     * @param path the keys of the value, separated by dots
     * @param min the lowest value allowed
     * @param max the highest value allowed
     * @return the value, the default, or the allowed value nearest to 0 when neither is there
     */
    public int getInteger(String path, int min, int max) {
        int fallback = Math.clamp(0, min, max);
        return get(path, "an integer", YamlConfig::toInteger, value -> value >= min && value <= max, fallback);
    }

    /**
     * A color with its alpha, such as {@code 0x80FF0000}, which is above the range of an int as a number.
     *
     * @param path the keys of the value, separated by dots
     * @return the color as an int, the default, or 0 when neither is there
     */
    public int getArgb(String path) {
        return get(path, "a color such as 0xFFFFFFFF", YamlConfig::toArgb, value -> true, 0);
    }

    /**
     * A value as true or false.
     *
     * @param path the keys of the value, separated by dots
     * @return the value, the default, or false when neither is there
     */
    public boolean getBoolean(String path) {
        return get(path, "true or false", YamlConfig::toBoolean, value -> true, false);
    }

    /**
     * The values read so far that could not be used, by their paths, in the order they were first read.
     *
     * @return a copy of the invalid values
     */
    Map<String, InvalidValue> invalidValues() {
        synchronized (invalidValues) {
            return new LinkedHashMap<>(invalidValues);
        }
    }

    /**
     * The value at {@code path}, with its keys separated by dots, made by {@code convert}, which returns null for a
     * value of the wrong type. Without a value in the file, with one of the wrong type, or with one that
     * {@code allowed} rejects, the default; without a default, {@code fallback}.
     */
    private <T extends @Nullable Object> T get(
            String path,
            String expected,
            Function<Object, @Nullable T> convert,
            Predicate<T> allowed,
            T fallback) {
        Optional<Object> value = find(values, path);
        if (value.isPresent()) {
            Object found = value.get();
            T converted = convert.apply(found);
            if (converted != null && allowed.test(converted)) return converted;
            String problem = converted == null ? "not " + expected : "out of range";
            if (invalidValues.putIfAbsent(path, new InvalidValue(found, problem)) == null && logsInvalidValues) {
                console().warning(path + " in " + file + " is " + problem + ": " + found + ", using the default");
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

    private static @Nullable String toText(Object value) {
        return value instanceof Map || value instanceof Collection ? null : value.toString();
    }

    private static @Nullable Integer toInteger(Object value) {
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

    private static @Nullable Integer toArgb(Object value) {
        if (value instanceof Integer argb) {
            return argb;
        }
        if (value instanceof Long argb && argb >= 0 && argb <= 0xFFFFFFFFL) {
            return (int) (long) argb;
        }
        return null;
    }

    private static @Nullable Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false"))) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }
}
