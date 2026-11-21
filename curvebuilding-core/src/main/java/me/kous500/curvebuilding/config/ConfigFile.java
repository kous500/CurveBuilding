package me.kous500.curvebuilding.config;

import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The config file in the data folder, to read and set one value at a time. Its keys and their defaults are those of
 * config.yml in the jar.
 */
public final class ConfigFile {
    /** What became of a new value. */
    public enum SetResult {
        /** The value was written. */
        SET,
        /** The value cannot be used there, so nothing was written. */
        INVALID,
        /** The file could not be written; that is logged. */
        NOT_SAVED
    }

    private static final String TEMPLATE = "config.yml";

    private final Path file;
    private final ClassLoader jar;
    private final Path backupFolder;
    private final Consumer<YamlConfig> read;

    /**
     * The config file {@code file}.
     *
     * @param file the config file
     * @param jar the class loader that has config.yml
     * @param backupFolder the folder for the copy of the file before it is rewritten
     * @param read reads the values of the file as the platform does, so that the values it cannot use are known
     */
    public ConfigFile(Path file, ClassLoader jar, Path backupFolder, Consumer<YamlConfig> read) {
        this.file = file;
        this.jar = jar;
        this.backupFolder = backupFolder;
        this.read = read;
    }

    /**
     * The paths of the values of config.yml in the jar, such as {@code curve.max-set-length}, in the order of the file.
     *
     * @return the paths, or an empty list when the jar has no config.yml
     */
    public List<String> keys() {
        List<String> keys = new ArrayList<>();
        if (template() instanceof MappingNode mapping) {
            addKeys(mapping, "", keys);
        }
        return keys;
    }

    /**
     * The value at {@code path}, as it is written in the file.
     *
     * @param path the keys of the value, separated by dots
     * @return the value, the default when the file does not have it or cannot be read, or null when config.yml in the
     *         jar does not have it either
     */
    public @Nullable String value(String path) {
        Node root;
        try {
            root = Files.exists(file) ? new Yaml().compose(new StringReader(YamlRewriter.readText(file))) : null;
        } catch (IOException | YAMLException e) {
            root = null;
        }
        String value = text(find(root, path));
        return value != null ? value : defaultValue(path);
    }

    /**
     * The default of the value at {@code path}, as it is written in config.yml in the jar.
     *
     * @param path the keys of the value, separated by dots
     * @return the default, or null when config.yml in the jar does not have it
     */
    public @Nullable String defaultValue(String path) {
        return text(find(template(), path));
    }

    /**
     * Writes {@code value} at {@code path} when the platform can use it, as it is written, such as {@code 0xFF0000}.
     * The file is rewritten in the layout of config.yml in the jar (see
     * {@link YamlRewriter#rewrite(Path, ClassLoader, String, Path, Consumer, Map)}).
     *
     * @param path the keys of the value, separated by dots, one of {@link #keys}
     * @param value the new value, as YAML
     * @return whether it was written
     */
    public SetResult set(String path, String value) {
        if (!keys().contains(path)) return SetResult.INVALID;
        Object parsed;
        try {
            parsed = new Yaml().load(value);
        } catch (YAMLException e) {
            return SetResult.INVALID;
        }
        if (parsed == null || parsed instanceof Map || parsed instanceof Collection) return SetResult.INVALID;

        Map<?, ?> values = fileValues();
        String templateText = YamlRewriter.readTemplate(jar, TEMPLATE);
        Object defaults = templateText == null ? null : new Yaml().load(templateText);
        YamlConfig config = YamlConfig.withoutWarnings(
                file,
                withValue(values, path.split("\\."), 0, parsed),
                defaults instanceof Map<?, ?> map ? map : Map.of());
        read.accept(config);
        if (config.invalidValues().containsKey(path)) return SetResult.INVALID;

        boolean written = YamlRewriter.rewrite(file, jar, TEMPLATE, backupFolder, read, Map.of(path, value));
        return written ? SetResult.SET : SetResult.NOT_SAVED;
    }

    /** The values of the file, or none when it cannot be read as keys and values. */
    private Map<?, ?> fileValues() {
        try {
            if (!Files.exists(file)) return Map.of();
            return new Yaml().load(YamlRewriter.readText(file)) instanceof Map<?, ?> map ? map : Map.of();
        } catch (IOException | YAMLException e) {
            return Map.of();
        }
    }

    /** A copy of {@code values} with {@code value} at the keys from {@code keys[index]} on. */
    private static Map<Object, @Nullable Object> withValue(Map<?, ?> values, String[] keys, int index, Object value) {
        Map<Object, @Nullable Object> copy = new LinkedHashMap<>(values);
        String key = keys[index];
        if (index == keys.length - 1) {
            copy.put(key, value);
        } else {
            Map<?, ?> child = copy.get(key) instanceof Map<?, ?> map ? map : Map.of();
            copy.put(key, withValue(child, keys, index + 1, value));
        }
        return copy;
    }

    private @Nullable Node template() {
        String text = YamlRewriter.readTemplate(jar, TEMPLATE);
        return text == null ? null : new Yaml().compose(new StringReader(text));
    }

    private static void addKeys(MappingNode mapping, String parentPath, List<String> keys) {
        for (NodeTuple entry : mapping.getValue()) {
            if (!(entry.getKeyNode() instanceof ScalarNode key)) continue;
            String path = parentPath + key.getValue();
            if (entry.getValueNode() instanceof MappingNode child) {
                addKeys(child, path + ".", keys);
            } else {
                keys.add(path);
            }
        }
    }

    /** The node at {@code path} under {@code root}, the last one of a key that is there twice. */
    private static @Nullable Node find(@Nullable Node root, String path) {
        Node node = root;
        for (String key : path.split("\\.")) {
            if (!(node instanceof MappingNode mapping)) return null;
            Node found = null;
            for (NodeTuple entry : mapping.getValue()) {
                if (entry.getKeyNode() instanceof ScalarNode keyNode && keyNode.getValue().equals(key)) {
                    found = entry.getValueNode();
                }
            }
            node = found;
        }
        return node;
    }

    private static @Nullable String text(@Nullable Node node) {
        return node instanceof ScalarNode scalar && !scalar.getTag().equals(Tag.NULL) ? scalar.getValue() : null;
    }
}
