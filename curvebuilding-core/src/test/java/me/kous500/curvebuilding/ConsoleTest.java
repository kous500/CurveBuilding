package me.kous500.curvebuilding;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConsoleTest {
    @Test
    void writesToAJavaLogger() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        List<LogRecord> records = new ArrayList<>();
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });

        Console console = Console.of(logger);
        IOException cause = new IOException("test");
        console.info("info");
        console.warning("warning");
        console.warning("warning with a cause", cause);

        assertEquals(List.of(Level.INFO, Level.WARNING, Level.WARNING), records.stream().map(LogRecord::getLevel).toList());
        assertEquals(List.of("info", "warning", "warning with a cause"), records.stream().map(LogRecord::getMessage).toList());
        assertNull(records.get(1).getThrown());
        assertSame(cause, records.get(2).getThrown());
    }
}
