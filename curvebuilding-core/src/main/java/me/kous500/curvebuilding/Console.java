package me.kous500.curvebuilding;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The server log, read by server owners. Write the messages in English, not with the message files: they are also
 * written while the message files are being read, and they get pasted into bug reports.
 */
public interface Console {
    void info(String message);

    void warning(String message);

    void warning(String message, Throwable cause);

    /** A console that writes to {@code logger}. */
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
