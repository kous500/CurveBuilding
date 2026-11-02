package me.kous500.curvebuilding;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server log, read by server owners. Write the messages in English, not with the message files: they are also
 * written while the message files are being read, and they get pasted into bug reports.
 */
public interface Console {
    /**
     * Logs {@code message} as information.
     *
     * @param message the message, in English
     */
    void info(String message);

    /**
     * Logs {@code message} as a warning, for something a server owner should look at.
     *
     * @param message the message, in English
     */
    void warning(String message);

    /**
     * Logs {@code message} as a warning, with the stack trace of {@code cause}.
     *
     * @param message the message, in English
     * @param cause what went wrong
     */
    void warning(String message, Throwable cause);

    /**
     * A console that writes to a Java logger.
     *
     * @param logger the logger to write to
     * @return the console
     */
    static Console of(Logger logger) {
        return new Console() {
            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void warning(String message) {
                logger.warning(message);
            }

            @Override
            public void warning(String message, Throwable cause) {
                logger.log(Level.WARNING, message, cause);
            }
        };
    }
}
