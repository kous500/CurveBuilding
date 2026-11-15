package me.kous500.curvebuilding.config;

import me.kous500.curvebuilding.config.YamlConfig.InvalidValue;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static me.kous500.curvebuilding.log.Log.console;

/**
 * Rewrites a YAML file in the layout of its template in the jar: the keys, their order and the comments come from the
 * template, the values from the file. A value that the file does not have, or that is invalid, is the default of the
 * template. Keys that the template does not have are left out.
 */
public final class YamlRewriter {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private YamlRewriter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Rewrites {@code file} in the layout of the resource {@code template} of {@code jar}, when that changes more than
     * its line breaks. The file is first copied into {@code backupFolder}, named with the time, such as
     * {@code config-2026-11-15_10-30-00.yml}. A file that is not YAML with keys and values is moved there, and the
     * template is written in its place. A file that does not exist, or is empty, gets the template without a backup.
     * The values that the file did not have, the invalid ones and the backups are logged. When the backup or the
     * writing fails, it is logged and the file is left as it is. A file that cannot be read is left as it is for
     * {@link YamlConfig#load} to log.
     *
     * <p>The template holds mappings and single values only.
     *
     * @param file the YAML file
     * @param jar the class loader that has the template
     * @param template the name of the template in the jar
     * @param backupFolder the folder for the backups, created when needed
     * @param read reads the values of the file as the platform does, so that the values it cannot use are known
     */
    public static void rewrite(
            Path file,
            ClassLoader jar,
            String template,
            Path backupFolder,
            Consumer<YamlConfig> read) {
        rewrite(file, jar, template, backupFolder, read, Clock.systemDefaultZone());
    }

    /**
     * {@link #rewrite(Path, ClassLoader, String, Path, Consumer)} with the backups named after the time of
     * {@code clock}.
     */
    static void rewrite(
            Path file,
            ClassLoader jar,
            String template,
            Path backupFolder,
            Consumer<YamlConfig> read,
            Clock clock) {
        String templateText = readTemplate(jar, template);
        if (templateText == null) return;
        Yaml yaml = yaml();
        if (!(yaml.compose(new StringReader(templateText)) instanceof MappingNode layout)) return;
        String defaultText = serialize(yaml, layout);

        String text;
        try {
            text = Files.exists(file) ? readText(file) : "";
        } catch (IOException e) {
            // YamlConfig logs the file that cannot be read
            return;
        }
        if (text.isBlank()) {
            write(file, defaultText);
            return;
        }

        Object values;
        try {
            values = new Yaml().load(text);
        } catch (YAMLException e) {
            moveAside(file, backupFolder, clock, defaultText, "could not be read as YAML", e.getMessage());
            return;
        }
        if (values != null && !(values instanceof Map)) {
            moveAside(file, backupFolder, clock, defaultText, "does not hold keys and values", null);
            return;
        }

        MappingNode fileNodes;
        try {
            fileNodes = yaml.compose(new StringReader(text)) instanceof MappingNode mapping ? mapping : emptyMapping();
        } catch (YAMLException e) {
            console().warning("Could not read the comments of " + file + ", so it was not rewritten", e);
            return;
        }

        Map<?, ?> defaults = new Yaml().load(templateText) instanceof Map<?, ?> map ? map : Map.of();
        YamlConfig config = YamlConfig.withoutWarnings(file, values == null ? Map.of() : (Map<?, ?>) values, defaults);
        read.accept(config);

        List<Runnable> logs = new ArrayList<>();
        putValues(layout, fileNodes, "", config.invalidValues(), file, logs);
        String rewritten = serialize(yaml, layout);
        if (rewritten.equals(text.replace("\r\n", "\n"))) return;

        Path backup;
        try {
            backup = backupPath(file, backupFolder, clock);
            Files.createDirectories(backupFolder);
            Files.copy(file, backup);
        } catch (IOException e) {
            console().warning("Could not copy " + file + " into " + backupFolder + ", so it was not rewritten", e);
            return;
        }
        if (!write(file, rewritten)) return;
        logs.forEach(Runnable::run);
        console().info(file + " was rewritten. The previous file is saved as " + backup);
    }

    /**
     * Puts the values of {@code fileNodes} into {@code layout}, and adds a log for each value that is missing or
     * invalid, which leaves the default there.
     */
    private static void putValues(
            MappingNode layout,
            MappingNode fileNodes,
            String parentPath,
            Map<String, InvalidValue> invalidValues,
            Path file,
            List<Runnable> logs) {
        List<NodeTuple> entries = layout.getValue();
        for (int i = 0; i < entries.size(); i++) {
            NodeTuple entry = entries.get(i);
            String key = ((ScalarNode) entry.getKeyNode()).getValue();
            String path = parentPath + key;
            Node defaultNode = entry.getValueNode();
            Node fileNode = find(fileNodes, key);

            if (defaultNode instanceof MappingNode defaultMapping) {
                MappingNode fileMapping = fileNode instanceof MappingNode mapping ? mapping : emptyMapping();
                putValues(defaultMapping, fileMapping, path + ".", invalidValues, file, logs);
                continue;
            }
            if (!(defaultNode instanceof ScalarNode defaultValue)) continue;

            String defaultText = defaultValue.getValue();
            if (fileNode == null || fileNode instanceof ScalarNode scalar && scalar.getTag().equals(Tag.NULL)) {
                logs.add(() -> console().info(
                        file + " did not have " + path + ", so the default " + defaultText + " was written"));
                continue;
            }
            InvalidValue invalid = invalidValues.get(path);
            if (invalid == null && !(fileNode instanceof ScalarNode)) {
                invalid = new InvalidValue(fileNode.getNodeId().name().toLowerCase(), "not a single value");
            }
            if (invalid != null) {
                String problem = invalid.problem() + ": " + invalid.found();
                logs.add(() -> console().warning(
                        path + " in " + file + " was " + problem + ", so the default " + defaultText + " was written"));
                continue;
            }

            // The value as it is written, so that its quotes and the 0x of a color stay
            ScalarNode fileValue = (ScalarNode) fileNode;
            ScalarNode value = new ScalarNode(
                    fileValue.getTag(), fileValue.getValue(), null, null, fileValue.getScalarStyle());
            value.setInLineComments(defaultValue.getInLineComments());
            entries.set(i, new NodeTuple(entry.getKeyNode(), value));
        }
    }

    /** The value of {@code key}; the last one when the key is there twice, as SnakeYAML reads the values. */
    private static @Nullable Node find(MappingNode mapping, String key) {
        Node found = null;
        for (NodeTuple entry : mapping.getValue()) {
            if (entry.getKeyNode() instanceof ScalarNode keyNode && keyNode.getValue().equals(key)) {
                found = entry.getValueNode();
            }
        }
        return found;
    }

    private static MappingNode emptyMapping() {
        return new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
    }

    /** Moves {@code file}, which cannot be read as keys and values, into the backup folder, and writes the template. */
    private static void moveAside(
            Path file,
            Path backupFolder,
            Clock clock,
            String defaultText,
            String problem,
            @Nullable String details) {
        String where = details == null ? "" : ":\n" + details;
        Path backup;
        try {
            backup = backupPath(file, backupFolder, clock);
            Files.createDirectories(backupFolder);
            Files.move(file, backup);
        } catch (IOException e) {
            console().warning(file + " " + problem + ", and could not be moved into " + backupFolder + where, e);
            return;
        }
        console().warning(file + " " + problem + ", so it was moved to " + backup + " and the defaults were written"
                + where);
        write(file, defaultText);
    }

    /** A file in {@code folder} that does not exist yet, named after {@code file} and the time. */
    private static Path backupPath(Path file, Path folder, Clock clock) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        String time = LocalDateTime.now(clock).format(BACKUP_TIME);

        Path backup = folder.resolve(base + "-" + time + extension);
        for (int i = 1; Files.exists(backup); i++) {
            backup = folder.resolve(base + "-" + time + "-" + i + extension);
        }
        return backup;
    }

    /**
     * Writes {@code text} to a temporary file first and then moves it to {@code file}, so that a failure does not
     * leave half a file.
     *
     * @return whether the file was written; a failure is logged
     */
    private static boolean write(Path file, String text) {
        try {
            Path folder = file.toAbsolutePath().getParent();
            Files.createDirectories(folder);
            Path temporary = Files.createTempFile(folder, file.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temporary, text, StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return true;
        } catch (IOException e) {
            console().warning("Could not write " + file, e);
            return false;
        }
    }

    private static @Nullable String readTemplate(ClassLoader jar, String template) {
        try (InputStream stream = jar.getResourceAsStream(template)) {
            return stream == null ? null : readText(stream.readAllBytes());
        } catch (IOException e) {
            console().warning("Could not read " + template + " in the jar", e);
            return null;
        }
    }

    private static String readText(Path file) throws IOException {
        return readText(Files.readAllBytes(file));
    }

    /** The text of a YAML file, in the encoding that SnakeYAML reads it with: UTF-8 without a byte order mark. */
    private static String readText(byte[] bytes) throws IOException {
        StringWriter text = new StringWriter();
        try (Reader reader = new UnicodeReader(new ByteArrayInputStream(bytes))) {
            reader.transferTo(text);
        }
        return text.toString();
    }

    private static String serialize(Yaml yaml, Node node) {
        StringWriter text = new StringWriter();
        yaml.serialize(node, text);
        // SnakeYAML 2.7 indents the blank lines inside a nested mapping
        return text.toString().replaceAll("(?m)[ \\t]+$", "");
    }

    /** A YAML that keeps the comments and writes each value on one line, as the templates are written. */
    private static Yaml yaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        dumperOptions.setSplitLines(false);
        dumperOptions.setWidth(Integer.MAX_VALUE);
        return new Yaml(
                new SafeConstructor(loaderOptions),
                new Representer(dumperOptions),
                dumperOptions,
                loaderOptions);
    }
}
