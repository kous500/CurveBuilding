package me.kous500.curvebuilding.config;

import java.nio.file.Files;
import java.nio.file.Path;

import static me.kous500.curvebuilding.CurveBuilding.console;
import static me.kous500.curvebuilding.Util.messageReplace;

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
            console.warning("The message file " + file + " does not exist, so the messages of " + defaults + " in the jar are used");
        }
        return new Messages(YamlConfig.load(file, jar, defaults));
    }

    /**
     * A message, with its placeholders filled in.
     *
     * @param message the message
     * @param args the values for {@code {0}}, {@code {1}}, ... in the message
     * @return the message with its values
     */
    public String get(Message message, Object... args) {
        return messageReplace(file.getString(message.path), args);
    }
}
