package com.erikedits.justquests.plugin.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * All quests the plugin knows, from four places:
 * <ul>
 *   <li>the quests bundled in the jar (the mod's main quests),</li>
 *   <li>quest datapacks in the main world's datapacks folder (data/&lt;ns&gt;/justquests/quests/),</li>
 *   <li>custom quests: plugins/JustQuests/custom-quests.json (the mod's format) and one quest per
 *       file in plugins/JustQuests/quests/; they reload by themselves when saved,</li>
 *   <li>generated quests (own namespace).</li>
 * </ul>
 * Custom quests override bundled and datapack quests with the same id. The main-quest switch hides
 * bundled and datapack quests, as in the mod.
 */
public final class QuestRegistry {
    private static final String DATAPACK_DIR = "justquests/quests/";

    private final Logger log;
    private final File jar;
    private final Path dataFolder;
    private final BooleanSupplier mainQuests;

    private Map<String, Quest> bundled = Map.of();
    private Map<String, Quest> datapack = Map.of();
    private Map<String, Quest> custom = Map.of();
    private Map<String, Quest> generated = Map.of();
    private long customStamp = Long.MIN_VALUE;

    public QuestRegistry(Logger log, File jar, Path dataFolder, BooleanSupplier mainQuests) {
        this.log = log;
        this.jar = jar;
        this.dataFolder = dataFolder;
        this.mainQuests = mainQuests;
    }

    public void loadAll() {
        bundled = loadBundled();
        datapack = loadDatapacks();
        reloadCustom();
        log.info("Loaded " + bundled.size() + " bundled, " + datapack.size() + " datapack and " + custom.size() + " custom quest(s)");
    }

    public void setGenerated(Map<String, Quest> quests) {
        generated = Map.copyOf(quests);
    }

    /** All visible quests: custom over datapack over bundled (those two only while main quests are on), plus generated. */
    public Map<String, Quest> all() {
        Map<String, Quest> merged = new HashMap<>();
        if (mainQuests.getAsBoolean()) {
            merged.putAll(bundled);
            merged.putAll(datapack);
        }
        merged.putAll(custom);
        merged.putAll(generated);
        return Collections.unmodifiableMap(merged);
    }

    public Quest get(String id) {
        Quest q = custom.get(id);
        if (q != null) return q;
        if (mainQuests.getAsBoolean()) {
            q = datapack.get(id);
            if (q != null) return q;
            q = bundled.get(id);
            if (q != null) return q;
        }
        return generated.get(id);
    }

    /** Every quest permission node, for registering them with the server. */
    public java.util.Set<String> permissions() {
        java.util.Set<String> out = new java.util.TreeSet<>();
        for (Map<String, Quest> map : java.util.List.of(bundled, datapack, custom, generated)) {
            for (Quest q : map.values()) {
                if (q.permission() != null) out.add(q.permission());
            }
        }
        return out;
    }

    // --- bundled -----------------------------------------------------------------------------

    private Map<String, Quest> loadBundled() {
        Map<String, Quest> out = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.startsWith("quests/") || !name.endsWith(".json")) continue;
                String id = "justquests:" + name.substring("quests/".length(), name.length() - 5);
                try (InputStream in = zip.getInputStream(e)) {
                    add(out, id, in, "bundled " + name);
                }
            }
        } catch (IOException e) {
            log.severe("Could not read the bundled quests: " + e.getMessage());
        }
        return out;
    }

    // --- datapacks ---------------------------------------------------------------------------

    private Map<String, Quest> loadDatapacks() {
        Map<String, Quest> out = new LinkedHashMap<>();
        if (Bukkit.getWorlds().isEmpty()) return out;
        World main = Bukkit.getWorlds().get(0);
        File packs = new File(main.getWorldFolder(), "datapacks");
        File[] list = packs.listFiles();
        if (list == null) return out;
        java.util.Arrays.sort(list);
        for (File pack : list) {
            try {
                if (pack.isDirectory()) readPackFolder(pack.toPath(), out);
                else if (pack.getName().endsWith(".zip")) readPackZip(pack, out);
            } catch (IOException e) {
                log.warning("Could not read datapack " + pack.getName() + ": " + e.getMessage());
            }
        }
        return out;
    }

    private void readPackFolder(Path pack, Map<String, Quest> out) throws IOException {
        Path data = pack.resolve("data");
        if (!Files.isDirectory(data)) return;
        try (Stream<Path> files = Files.walk(data)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                String rel = data.relativize(p).toString().replace('\\', '/');
                String id = datapackId(rel);
                if (id == null || !Files.isRegularFile(p)) continue;
                try (InputStream in = Files.newInputStream(p)) {
                    add(out, id, in, pack.getFileName() + "/data/" + rel);
                }
            }
        }
    }

    private void readPackZip(File pack, Map<String, Quest> out) throws IOException {
        try (ZipFile zip = new ZipFile(pack)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory() || !e.getName().startsWith("data/")) continue;
                String id = datapackId(e.getName().substring("data/".length()));
                if (id == null) continue;
                try (InputStream in = zip.getInputStream(e)) {
                    add(out, id, in, pack.getName() + "/" + e.getName());
                }
            }
        }
    }

    /** "ns/justquests/quests/path.json" -> "ns:path", else null. */
    private static String datapackId(String rel) {
        int slash = rel.indexOf('/');
        if (slash <= 0 || !rel.endsWith(".json")) return null;
        String rest = rel.substring(slash + 1);
        if (!rest.startsWith(DATAPACK_DIR)) return null;
        return (rel.substring(0, slash) + ":" + rest.substring(DATAPACK_DIR.length(), rest.length() - 5)).toLowerCase(Locale.ROOT);
    }

    // --- custom ------------------------------------------------------------------------------

    private Path customFile() {
        return dataFolder.resolve("custom-quests.json");
    }

    private Path questFolder() {
        return dataFolder.resolve("quests");
    }

    /** Called every few seconds: reloads the custom quests when one of their files changed. */
    public boolean checkCustom() {
        if (stamp() == customStamp) return false;
        reloadCustom();
        return true;
    }

    private long stamp() {
        long s = 0;
        try {
            Path f = customFile();
            if (Files.exists(f)) s = 31 * s + Files.getLastModifiedTime(f).toMillis();
            Path dir = questFolder();
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.walk(dir)) {
                    for (Path p : (Iterable<Path>) files::iterator) {
                        if (p.toString().endsWith(".json")) s = 31 * s + Files.getLastModifiedTime(p).toMillis() + p.hashCode();
                    }
                }
            }
        } catch (IOException ignored) {
            // try again on the next check
        }
        return s;
    }

    public int reloadCustom() {
        customStamp = stamp();
        Map<String, Quest> out = new LinkedHashMap<>();
        Path file = customFile();
        if (Files.exists(file)) {
            try {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                        String key = e.getKey();
                        if (key.startsWith("_") || !e.getValue().isJsonObject()) continue;
                        JsonObject obj = e.getValue().getAsJsonObject();
                        // blank template slots (no objectives) are skipped silently
                        if (!obj.has("objectives") || obj.getAsJsonArray("objectives").isEmpty()) continue;
                        String id = (key.contains(":") ? key : "justquests:" + key).toLowerCase(Locale.ROOT);
                        add(out, id, obj, "custom-quests.json");
                    }
                }
            } catch (Exception e) {
                log.severe("Could not read custom-quests.json: " + e.getMessage());
            }
        }
        Path dir = questFolder();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) files::iterator) {
                    if (!Files.isRegularFile(p) || !p.toString().endsWith(".json")) continue;
                    String rel = dir.relativize(p).toString().replace('\\', '/');
                    String id = ("justquests:" + rel.substring(0, rel.length() - 5)).toLowerCase(Locale.ROOT);
                    try (InputStream in = Files.newInputStream(p)) {
                        add(out, id, in, "quests/" + rel);
                    }
                }
            } catch (IOException e) {
                log.severe("Could not read the quests folder: " + e.getMessage());
            }
        }
        custom = out;
        return out.size();
    }

    // --- shared ------------------------------------------------------------------------------

    private static JsonObject read(InputStream in) {
        JsonElement e = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        if (!e.isJsonObject()) throw new IllegalArgumentException("not a JSON object");
        return e.getAsJsonObject();
    }

    private void add(Map<String, Quest> into, String id, JsonObject json, String where) {
        try {
            into.put(id, Quest.parse(id, json));
        } catch (RuntimeException e) {
            log.warning("Quest " + id + " (" + where + ") skipped: " + e.getMessage());
        }
    }

    private void add(Map<String, Quest> into, String id, InputStream in, String where) {
        JsonObject json;
        try {
            json = read(in);
        } catch (RuntimeException e) {
            log.warning("Quest " + id + " (" + where + ") skipped: not valid JSON (" + e.getMessage() + ")");
            return;
        }
        add(into, id, json, where);
    }
}
