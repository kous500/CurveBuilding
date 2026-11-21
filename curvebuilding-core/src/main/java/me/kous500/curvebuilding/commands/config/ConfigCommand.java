package me.kous500.curvebuilding.commands.config;

import me.kous500.curvebuilding.config.ConfigFile;
import me.kous500.curvebuilding.message.Message;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The subcommand {@code config} of {@code /curvebuilding}, which shows a value of config.yml or sets it, for every
 * platform: {@code config <key>} shows it, {@code config <key> <value>} sets it, and {@code config <key> default}
 * sets its default.
 */
public final class ConfigCommand {
    /** The value that sets the default of a key. */
    public static final String DEFAULT = "default";

    /**
     * What to tell the sender of the command.
     *
     * @param message the message
     * @param error whether it is an error
     * @param args the values for {@code {0}}, {@code {1}}, ... in the message
     */
    public record Reply(Message message, boolean error, Object... args) {
    }

    private final ConfigFile file;
    private final List<String> sections;
    private final Runnable reload;

    /**
     * The subcommand for the keys of {@code file} in {@code sections}.
     *
     * @param file the config file
     * @param sections the first keys of the values that can be set, such as {@code curve}
     * @param reload reads the files again after a value is set
     */
    public ConfigCommand(ConfigFile file, List<String> sections, Runnable reload) {
        this.file = file;
        this.sections = List.copyOf(sections);
        this.reload = reload;
    }

    /**
     * The keys that can be shown and set: those of config.yml in the jar in the sections of this command.
     *
     * @return the keys, such as {@code curve.max-set-length}, in the order of config.yml
     */
    public List<String> keys() {
        return file.keys().stream()
                .filter(key -> sections.stream().anyMatch(section -> key.startsWith(section + ".")))
                .toList();
    }

    /**
     * Runs the subcommand. A new value is written to the config file, and then the files are read again.
     *
     * @param args the arguments after {@code config}
     * @return what to tell the sender, or null when there is no key or there are too many arguments
     */
    public @Nullable Reply run(String[] args) {
        if (args.length < 1 || args.length > 2) return null;
        String key = args[0];
        if (!keys().contains(key)) return new Reply(Message.CONFIG_UNKNOWN_KEY, true, key);
        if (args.length == 1) return new Reply(Message.CONFIG_VALUE, false, key, Objects.toString(file.value(key)));

        String value = args[1].equals(DEFAULT) ? Objects.toString(file.defaultValue(key)) : args[1];
        return switch (file.set(key, value)) {
            case SET -> {
                reload.run();
                yield new Reply(Message.CONFIG_SET, false, key, value);
            }
            case INVALID -> new Reply(Message.CONFIG_INVALID_VALUE, true, key, args[1]);
            case NOT_SAVED -> new Reply(Message.CONFIG_NOT_SAVED, true);
        };
    }

    /**
     * Suggestions for the last of {@code args}, the argument being typed: the keys, then the value of the key and
     * {@code default}.
     *
     * @param args the arguments after {@code config} typed so far; the last one may be empty
     * @return the suggestions that start with what is typed, or an empty list
     */
    public List<String> complete(String[] args) {
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            candidates.addAll(keys());
        } else if (args.length == 2 && keys().contains(args[0])) {
            candidates.add(Objects.toString(file.value(args[0])));
            candidates.add(DEFAULT);
        }
        String typed = args.length == 0 ? "" : args[args.length - 1];
        return candidates.stream().filter(candidate -> candidate.startsWith(typed)).distinct().toList();
    }
}
