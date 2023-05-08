package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.MainInitializer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static me.kous500.curvebuilding.config.ResourceType.config;

public class YamlConfig {
    private static MainInitializer mainInitializer;
    private static ClassLoader classLoader;

    public static YamlConfig loadConfiguration(File file, ResourceType resourceType, MainInitializer mainInitializer) {
        YamlConfig.mainInitializer = mainInitializer;
        YamlConfig.classLoader = mainInitializer.getMainClassLoader();

        try {
            return new YamlConfig(file, resourceType);
        } catch (FileNotFoundException e) {
            return new YamlConfig(resourceType);
        }
    }

    private final Map<String, Object> configMap;
    private File configFile;
    private ResourceType resourceType;

    public YamlConfig(ResourceType resourceType) {
        this.configMap = new HashMap<>(){};
        this.resourceType = resourceType;
    }

    public YamlConfig(File file, ResourceType resourceType) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        FileInputStream inputStream = new FileInputStream(file);
        this.configMap = yaml.load(inputStream);
        this.configFile = file;
        this.resourceType = resourceType;
    }

    private YamlConfig(InputStream inputStream) {
        Yaml yaml = new Yaml();
        this.configMap = yaml.load(inputStream);
    }

    public String getString(String path) {
        Object value = get(path, getDefault(path));

        return value != null ? value.toString() : getDefault(path).toString();
    }

    public int getInteger(String path, int minValue, int maxValue) {
        int value = getInteger(path);

        return value < minValue  || value > maxValue ? (int) getDefault(path) : value;
    }

    public int getInteger(String path, int minValue) {
        int value = getInteger(path);

        return value < minValue ? (int) getDefault(path) : value;
    }

    public int getInteger(String path) {
        Object value = get(path, getDefault(path));

        if (value instanceof Integer) {
            return (int) value;
        } else if (value != null) {
            try {
                return (int) Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {}
        }

        return (int) Long.parseLong(getDefault(path).toString());
    }

    public Boolean getBoolean(String path) {
        boolean def = Boolean.parseBoolean(getDefault(path).toString());
        Object value = get(path, def);

        return value == null ? def : Boolean.parseBoolean(value.toString());
    }

    public Object get(String path, Object def) {
        Object value = get(path);

        if (value == null) {
            try {
                set(path, def);
            } catch (IOException e) {
                new ResourceFiles(mainInitializer).create(getResourceFileName());
            }

            return def;
        } else {
            return value;
        }
    }

    public Object get(String path) {
        Object value = this.configMap;
        String[] segments = path.split("\\.");

        for (String segment : segments) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(segment);
            } else {
                value = null;
            }
        }

        return value;
    }

    private Object getDefault(String path) {
        String resourceFile = getResourceFileName();

        YamlConfig defaultFile = new YamlConfig(classLoader.getResourceAsStream(resourceFile));
        return defaultFile.get(path);
    }

    private String getResourceFileName() {
        if (resourceType == config) {
            return "config.yml";
        } else {
            final String defFile = "messages/en.yml";

            String fileName;
            if (configFile != null) {
                Path messageFilePass = Paths.get(configFile.getPath());
                fileName = "messages/" + messageFilePass.getFileName().toString();
            } else {
                fileName = defFile;
            }

            if (classLoader.getResource(fileName) != null) {
                return fileName;
            } else {
                return defFile;
            }
        }
    }

    private void set(String path, Object value) throws IOException {
        String[] segments = path.split("\\.");
        Map<String, Object> current = this.configMap;
        for (int i = 0; i < segments.length - 1; i++) {
            if (!current.containsKey(segments[i])) {
                current.put(segments[i], new LinkedHashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(segments[i]);
        }
        current.put(segments[segments.length - 1], value);

        if (this.configFile == null) return;

        FileWriter writer = new FileWriter(this.configFile, StandardCharsets.UTF_8);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        yaml.dump(this.configMap, writer);
        writer.close();
    }
}
