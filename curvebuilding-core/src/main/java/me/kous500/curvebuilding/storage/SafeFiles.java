package me.kous500.curvebuilding.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static me.kous500.curvebuilding.log.Log.console;

/** Writes files so that a failure does not leave half a file, and names the backups of files. */
public final class SafeFiles {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private SafeFiles() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Writes {@code text} in UTF-8 to a temporary file next to {@code file} first, and then moves it to {@code file}.
     * The folder is created when needed.
     *
     * @param file the file to write
     * @param text its new text
     * @return whether the file was written; a failure is logged
     */
    public static boolean write(Path file, String text) {
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

    /**
     * A file in {@code folder} for a backup of {@code file}, named after it and the time, such as
     * {@code config-2026-11-15_10-30-00.yml}, with {@code -1}, {@code -2} and so on before the extension when that
     * file is already there.
     *
     * @param file the file to back up
     * @param folder the folder of the backups
     * @param clock the clock to take the time from
     * @return a file in {@code folder} that does not exist yet
     */
    public static Path backupPath(Path file, Path folder, Clock clock) {
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
}
