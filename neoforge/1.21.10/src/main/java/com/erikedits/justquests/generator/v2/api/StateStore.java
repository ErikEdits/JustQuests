package com.erikedits.justquests.generator.v2.api;

import java.util.List;
import java.util.Optional;

/**
 * File access restricted to {@code <world>/justquests/}. File names may contain {@code '/'} for
 * sub-directories (e.g. {@code generator_v2/balance.json}); implementations must reject names that
 * escape the directory ({@code ..}, absolute paths).
 */
public interface StateStore {
    /**
     * Reads {@code <world>/justquests/<fileName>}.
     *
     * @param fileName relative file name
     * @return the UTF-8 content; empty if missing or unreadable
     */
    Optional<String> read(String fileName);

    /**
     * Writes atomically (temp file + move), creating parent directories. Never throws; logs on failure.
     *
     * @param fileName relative file name
     * @param content  UTF-8 content
     */
    void write(String fileName, String content);

    /**
     * Lists the regular files directly inside a sub-directory of {@code <world>/justquests/}.
     * Used to discover optional world-level override profiles ({@code generator_v2/profiles}).
     * Optional: the default returns an empty list, in which case only the index file
     * {@code generator_v2/profiles/index.json} is used.
     *
     * @param directory relative directory name, e.g. {@code "generator_v2/profiles"}
     * @return file names (without directory), sorted; empty if missing
     */
    default List<String> list(String directory) {
        return List.of();
    }
}
