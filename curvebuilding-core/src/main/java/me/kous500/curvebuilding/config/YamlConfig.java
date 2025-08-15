package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.MainInitializer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class YamlConfig {
    private final File configFile;
    private final Map<String, Object> configMap;
    private final YamlConfig defaultConfig;

    public static YamlConfig loadConfiguration(File file, ResourceType resourceType, MainInitializer mainInitializer) {
        ClassLoader classLoader = mainInitializer.getMainClassLoader();
        YamlConfig defaultConfig = loadDefaultConfig(resourceType, file, classLoader);

        if (file.exists()) {
            try (InputStream inputStream = new FileInputStream(file)) {
                return new YamlConfig(file, inputStream, defaultConfig);
            } catch (IOException e) {
                e.fillInStackTrace();
                return new YamlConfig(file, new LinkedHashMap<>(), defaultConfig);
            }
        } else {
            return new YamlConfig(file, new LinkedHashMap<>(), defaultConfig);
        }
    }

    private YamlConfig(File file, InputStream inputStream, YamlConfig defaultConfig) {
        this(file, loadMapFromStream(inputStream), defaultConfig);
    }

    private YamlConfig(File file, Map<String, Object> map, YamlConfig defaultConfig) {
        this.configFile = file;
        this.defaultConfig = defaultConfig;
        this.configMap = Optional.ofNullable(map).orElse(new LinkedHashMap<>());
    }

    private YamlConfig(InputStream resourceStream) {
        this.configFile = null;
        this.defaultConfig = null;
        this.configMap = loadMapFromStream(resourceStream);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadMapFromStream(InputStream stream) {
        if (stream == null) {
            return new LinkedHashMap<>();
        }
        Object loaded = new Yaml().load(stream);
        if (loaded instanceof Map) {
            return (Map<String, Object>) loaded;
        }
        return new LinkedHashMap<>();
    }

    private static YamlConfig loadDefaultConfig(ResourceType resourceType, File configFile, ClassLoader classLoader) {
        String resourceName = getResourceFileName(resourceType, configFile, classLoader);
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            return new YamlConfig(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load default resource: " + resourceName, e);
        }
    }

    public String getString(String path) {
        return get(path).map(Object::toString).orElseGet(() ->
                defaultConfig.get(path).map(Object::toString).orElse(null)
        );
    }

    public int getInteger(String path) {
        Object value = get(path).orElseGet(() -> defaultConfig.get(path).orElse(0));
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultConfig.get(path).map(v -> Integer.parseInt(v.toString())).orElse(0);
        }
    }

    public boolean getBoolean(String path) {
        Object value = get(path).orElseGet(() -> defaultConfig.get(path).orElse(false));
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(value.toString());
    }

    public Optional<Object> get(String path) {
        Object currentValue = this.configMap;
        String[] segments = path.split("\\.");
        for (String segment : segments) {
            if (currentValue instanceof Map) {
                currentValue = ((Map<?, ?>) currentValue).get(segment);
            } else {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(currentValue);
    }

    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] segments = path.split("\\.");
        Map<String, Object> current = this.configMap;

        for (int i = 0; i < segments.length - 1; i++) {
            String segment = segments[i];
            Object next = current.get(segment);

            Map<String, Object> nextMap;
            if (next instanceof Map) {
                nextMap = (Map<String, Object>) next;
            } else {
                nextMap = new LinkedHashMap<>();
                current.put(segment, nextMap);
            }
            current = nextMap;
        }

        current.put(segments[segments.length - 1], value);
    }

    public void save() {
        if (this.configFile == null) {
            return;
        }
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try (Writer writer = new FileWriter(this.configFile, StandardCharsets.UTF_8)) {
            yaml.dump(this.configMap, writer);
        } catch (IOException e) {
            e.fillInStackTrace();
        }
    }

    private static String getResourceFileName(ResourceType resourceType, File configFile, ClassLoader classLoader) {
        if (resourceType == ResourceType.config) {
            return "config.yml";
        } else {
            String fileName = "messages/en-US.yml";
            if (configFile != null) {
                Path messageFilePath = Paths.get(configFile.getPath());
                String prospectiveFileName = "messages/" + messageFilePath.getFileName().toString();
                if (classLoader.getResource(prospectiveFileName) != null) {
                    fileName = prospectiveFileName;
                }
            }
            return fileName;
        }
    }
}