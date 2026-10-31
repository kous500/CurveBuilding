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

    public static Messages load(Path file, ClassLoader jar) {
        String sameName = "messages/" + file.getFileName();
        String defaults = jar.getResource(sameName) != null ? sameName : "messages/en-US.yml";
        if (!Files.exists(file)) {
            console.warning("The message file " + file + " does not exist, so the messages of " + defaults + " in the jar are used");
        }
        return new Messages(YamlConfig.load(file, jar, defaults));
    }

    /** {@code message} with {@code {0}}, {@code {1}}, ... replaced with {@code args}. */
    public String get(Message message, Object... args) {
        return messageReplace(file.getString(message.path), args);
    }
}
