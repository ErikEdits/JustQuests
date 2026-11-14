package com.erikedits.justquests.network;

import com.erikedits.justquests.JustQuests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The text of a quest sync, packed (0.6.0). A server may send at most 1 MiB to a player in one
 * packet, and a big quest list with descriptions in several languages is more than that as plain
 * JSON. Packed with gzip it is about a tenth, so even thousands of quests fit. Small syncs (progress
 * only) stay plain; a packed one starts with "gz:", which JSON never does.
 */
public final class SyncText {
    private static final String PACKED = "gz:";
    /** The most text one sync may carry (the limit of the payload's string). */
    public static final int MAX = 1048576;

    private SyncText() {}

    /** The text to send for this JSON. */
    public static String pack(String json) {
        if (json.length() < 4096) return json;
        ByteArrayOutputStream out = new ByteArrayOutputStream(json.length() / 6);
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String text = PACKED + Base64.getEncoder().encodeToString(out.toByteArray());
        if (text.length() > MAX) {
            JustQuests.LOG.error("The quest list is too big to send to a player even packed ({} characters, at most {}): "
                + "the quest book stays empty. Fewer or shorter quests fix it.", text.length(), MAX);
        }
        return text;
    }

    /** The JSON of a received text, packed or not. */
    public static String unpack(String text) {
        if (!text.startsWith(PACKED)) return text;
        byte[] raw = Base64.getDecoder().decode(text.substring(PACKED.length()));
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw))) {
            return new String(gz.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
