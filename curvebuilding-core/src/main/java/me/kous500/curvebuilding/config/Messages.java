package me.kous500.curvebuilding.config;

import static me.kous500.curvebuilding.Util.messageReplace;

/**
 * The messages of a message file. A message missing from it comes from the file of the same name in the jar, or from
 * en-US.yml when the jar has no such file.
 */
public class Messages {
    private final YamlConfig file;

    public Messages(YamlConfig file) {
        this.file = file;
    }

    /** {@code message} with {@code {0}}, {@code {1}}, ... replaced with {@code args}. */
    public String get(Message message, Object... args) {
        return messageReplace(file.getString(message.path), args);
    }
}
