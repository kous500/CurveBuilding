package me.kous500.curvebuilding.config;

import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static me.kous500.curvebuilding.CurveBuilding.console;

/** The values of a YAML file, with those missing from it taken from a default file in the jar. */
public class YamlConfig {
    private final Map<?, ?> values;
    private final Map<?, ?> defaults;

    private YamlConfig(Map<?, ?> values, Map<?, ?> defaults) {
        this.values = values;
        this.defaults = defaults;
    }

    /**
     * Reads {@code file}, with the resource {@code defaultResource} of {@code jar} for the values missing from it. A file
     * that does not exist, or cannot be read, has no values of its own.
     */
    public static YamlConfig load(Path file, ClassLoader jar, String defaultResource) {
        return new YamlConfig(readFile(file), readResource(jar, defaultResource));
    }

    private static Map<?, ?> readFile(Path file) {
        if (!Files.exists(file)) {
            return Map.of();
        }
        try {
            // Read the bytes first, so that an error reading the file is not wrapped by the YAML parser
            return parse(new ByteArrayInputStream(Files.readAllBytes(file)));
        } catch (IOException e) {
            console.warning("Could not read " + file + ", so the defaults are used", e);
            return Map.of();
        }
    }

    private static Map<?, ?> readResource(ClassLoader jar, String resource) {
        try (InputStream stream = jar.getResourceAsStream(resource)) {
            return stream != null ? parse(stream) : Map.of();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load default resource: " + resource, e);
        }
    }

    private static Map<?, ?> parse(InputStream stream) {
        return new Yaml().load(stream) instanceof Map<?, ?> map ? map : Map.of();
    }

    public String getString(String path) {
        return get(path).map(Object::toString).orElseGet(() ->
                find(defaults, path).map(Object::toString).orElse(null)
        );
    }

    public int getInteger(String path) {
        Object value = get(path).orElseGet(() -> find(defaults, path).orElse(0));
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return find(defaults, path).map(v -> Integer.parseInt(v.toString())).orElse(0);
        }
    }

    public boolean getBoolean(String path) {
        Object value = get(path).orElseGet(() -> find(defaults, path).orElse(false));
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(value.toString());
    }

    /** The value at {@code path}, with its keys separated by dots, in the file itself only. */
    private Optional<Object> get(String path) {
        return find(values, path);
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
}
