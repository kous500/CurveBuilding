package me.kous500.curvebuilding;

import me.kous500.curvebuilding.log.Console;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Keeps what is written to the console, for tests. */
@NullMarked
public class RecordingConsole implements Console {
    public record Entry(String level, String message, @Nullable Throwable cause) {
    }

    public final List<Entry> entries = new ArrayList<>();

    @Override
    public void info(String message) {
        entries.add(new Entry("info", message, null));
    }

    @Override
    public void warning(String message) {
        entries.add(new Entry("warning", message, null));
    }

    @Override
    public void warning(String message, Throwable cause) {
        entries.add(new Entry("warning", message, cause));
    }
}
