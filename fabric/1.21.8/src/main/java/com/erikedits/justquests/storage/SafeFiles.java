package com.erikedits.justquests.storage;

import com.erikedits.justquests.JustQuests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The world's JSON files are never lost (0.6.1): they are written through a temp file, so a crash in the
 * middle never leaves half a file, and a file that can't be read is copied aside before anything writes
 * over it. Before, a progress.json with one typo (edited by hand) was read as empty and the next autosave
 * replaced it - with all progress gone.
 */
public final class SafeFiles {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SafeFiles() {}

    /** Writes the text through a temp file next to it, then moves it over the file. */
    public static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, text.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * A file that could not be read (or only partly): a copy is kept as name.corrupt-time.json, so whatever
     * is written to the file next takes nothing away. Logged as an error with why.
     */
    public static void keepBroken(Path file, String why) {
        try {
            if (file == null || !Files.exists(file)) return;
            String name = file.getFileName().toString();
            String base = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
            Path copy = file.resolveSibling(base + ".corrupt-" + LocalDateTime.now().format(STAMP) + ".json");
            Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
            JustQuests.LOG.error("{} could not be read ({}). A copy is kept as {}: fix it and put it back with the server stopped.",
                name, why, copy.getFileName());
        } catch (IOException e) {
            JustQuests.LOG.error("{} could not be read ({}), and no copy could be made: {}", file, why, e.getMessage());
        }
    }
}
