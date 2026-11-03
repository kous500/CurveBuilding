package me.kous500.curvebuilding.message;

import me.kous500.curvebuilding.config.YamlConfig;

import java.nio.file.Files;
import java.nio.file.Path;

import static me.kous500.curvebuilding.CurveBuilding.console;

/**
 * The messages of a message file. A message missing from it comes from the file of the same name in the jar, or from
 * en-US.yml when the jar has no such file.
 */
public class Messages {
    private final YamlConfig file;

    private Messages(YamlConfig file) {
        this.file = file;
    }

    /**
     * Reads the message file {@code file}. When it does not exist, a warning is logged and the messages come from the
     * jar.
     *
     * @param file the message file
     * @param jar the class loader that has the default message files
     * @return the messages
     */
    public static Messages load(Path file, ClassLoader jar) {
        String sameName = "messages/" + file.getFileName();
        String defaults = jar.getResource(sameName) != null ? sameName : "messages/en-US.yml";
        if (!Files.exists(file)) {
            console.warning("The message file " + file + " does not exist, using " + defaults + " in the jar");
        }
        return new Messages(YamlConfig.load(file, jar, defaults));
    }

    /**
     * A message, with its placeholders filled in.
     *
     * @param message the message
     * @param args the values for {@code {0}}, {@code {1}}, ... in the message
     * @return the message with its values, or its path when neither the file nor the jar has it
     */
    public String get(Message message, Object... args) {
        String text = file.getString(message.path);
        return text != null ? replacePlaceholders(text, args) : message.path;
    }

    /**
     * {@code message} with {@code {0}}, {@code {1}}, ... replaced with {@code args} in that order. A placeholder
     * without an argument stays as it is.
     *
     * @param message the text
     * @param args the values for the placeholders
     * @return the text with the values
     */
    static String replacePlaceholders(String message, Object... args) {
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return message;
    }
}
