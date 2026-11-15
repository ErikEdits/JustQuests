package com.erikedits.justquests.plugin.badge;

import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.quest.LocalizedText;
import com.erikedits.justquests.plugin.quest.Quest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Badges in the plugin (0.6.0), as in the mod: the mod's own eight (bundled, earned by rules), the badges
 * of quest datapacks (data/&lt;ns&gt;/justquests/badges/), and the "badges" section of custom-quests.json
 * (given by the justquests:badge reward). The sign of the shown badge stands before the player's name in
 * the tab list and the chat; chat plugins can place it themselves with %justquests_badge%.
 */
public final class PluginBadges implements Listener {
    public static final String FIRST_QUEST = "justquests:first_quest", QUESTS_10 = "justquests:quests_10",
        QUESTS_50 = "justquests:quests_50", QUESTS_100 = "justquests:quests_100", QUESTS_500 = "justquests:quests_500",
        CHAPTER_COMPLETE = "justquests:chapter_complete";
    /** A category is a chapter for chapter_complete when it has at least this many quests. */
    public static final int CHAPTER_MIN = 3;

    /** One badge. */
    public record Badge(String id, LocalizedText name, LocalizedText description, LocalizedText how, String icon,
                        String color, String symbol, boolean discord, boolean secret, int sort) {
        static Badge parse(String id, JsonObject o) {
            return new Badge(id, LocalizedText.parse(o.get("name")),
                o.has("description") ? LocalizedText.parse(o.get("description")) : LocalizedText.EMPTY,
                o.has("how") ? LocalizedText.parse(o.get("how")) : LocalizedText.EMPTY,
                o.has("icon") ? o.get("icon").getAsString() : "",
                o.has("color") ? o.get("color").getAsString() : "#F2C14E",
                o.has("symbol") ? o.get("symbol").getAsString() : "★",
                o.has("discord") && o.get("discord").getAsBoolean(),
                o.has("secret") && o.get("secret").getAsBoolean(),
                o.has("sort") ? o.get("sort").getAsInt() : 100);
        }

        public ChatColor chatColor() {
            try {
                return ChatColor.of(color.startsWith("#") ? color : "#" + color);
            } catch (IllegalArgumentException e) {
                return ChatColor.GOLD;
            }
        }

        public String sign() {
            return symbol.isEmpty() ? "★" : symbol.substring(0, symbol.offsetByCodePoints(0, 1));
        }

        public Material material() {
            Material m = icon.isEmpty() ? null : Material.matchMaterial(icon);
            return m != null && m.isItem() ? m : Material.NETHER_STAR;
        }
    }

    private static PluginBadges instance;

    private final JustQuestsPlugin plugin;
    private Map<String, Badge> bundled = Map.of(), datapack = Map.of(), custom = Map.of();
    private Map<String, JsonObject> packs = Map.of();
    private JsonObject customPack;

    public PluginBadges(JustQuestsPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /** The badges of the running plugin (for the justquests:badge reward). */
    public static PluginBadges get() {
        return instance;
    }

    // ---- loading

    /** Reads every badge: bundled, datapacks of the main world, custom-quests.json. */
    public void load() {
        Map<String, Badge> b = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(plugin.jarFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory() || !e.getName().startsWith("badges/") || !e.getName().endsWith(".json")) continue;
                String id = "justquests:" + e.getName().substring("badges/".length(), e.getName().length() - 5);
                try (InputStream in = zip.getInputStream(e)) {
                    read(b, id, in);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not read the bundled badges: " + e.getMessage());
        }
        bundled = b;
        Map<String, Badge> d = new LinkedHashMap<>();
        Map<String, JsonObject> p = new HashMap<>();
        readDatapacks(d, p);
        datapack = d;
        packs = p;
        readCustom();
        plugin.getLogger().info("Loaded " + all().size() + " badge(s)");
    }

    private void read(Map<String, Badge> out, String id, InputStream in) {
        try {
            JsonElement j = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (j.isJsonObject()) out.put(id, Badge.parse(id, j.getAsJsonObject()));
        } catch (Exception e) {
            plugin.getLogger().warning("Badge " + id + " invalid: " + e.getMessage());
        }
    }

    private void readDatapacks(Map<String, Badge> out, Map<String, JsonObject> packInfo) {
        if (Bukkit.getWorlds().isEmpty()) return;
        World main = Bukkit.getWorlds().get(0);
        File[] list = new File(main.getWorldFolder(), "datapacks").listFiles();
        if (list == null) return;
        java.util.Arrays.sort(list);
        for (File pack : list) {
            try {
                if (pack.isDirectory()) {
                    Path data = pack.toPath().resolve("data");
                    if (!Files.isDirectory(data)) continue;
                    try (Stream<Path> files = Files.walk(data)) {
                        for (Path f : (Iterable<Path>) files::iterator) {
                            if (!Files.isRegularFile(f)) continue;
                            try (InputStream in = Files.newInputStream(f)) {
                                entry(data.relativize(f).toString().replace('\\', '/'), in, out, packInfo);
                            }
                        }
                    }
                } else if (pack.getName().endsWith(".zip")) {
                    try (ZipFile zip = new ZipFile(pack)) {
                        Enumeration<? extends ZipEntry> entries = zip.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry e = entries.nextElement();
                            if (e.isDirectory() || !e.getName().startsWith("data/")) continue;
                            try (InputStream in = zip.getInputStream(e)) {
                                entry(e.getName().substring("data/".length()), in, out, packInfo);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not read the badges of datapack " + pack.getName() + ": " + e.getMessage());
            }
        }
    }

    /** "ns/justquests/badges/path.json" is a badge, "ns/justquests/pack.json" the pack's Discord details. */
    private void entry(String rel, InputStream in, Map<String, Badge> out, Map<String, JsonObject> packInfo) throws IOException {
        int slash = rel.indexOf('/');
        if (slash <= 0 || !rel.endsWith(".json")) return;
        String ns = rel.substring(0, slash), rest = rel.substring(slash + 1);
        if (rest.startsWith("justquests/badges/")) {
            read(out, ns + ":" + rest.substring("justquests/badges/".length(), rest.length() - 5), in);
        } else if (rest.equals("justquests/pack.json")) {
            JsonElement j = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (j.isJsonObject()) packInfo.put(ns, j.getAsJsonObject());
        }
    }

    /**
     * The "badges" and "pack" sections of plugins/JustQuests/custom-quests.json and (0.6.0) of the
     * files in plugins/JustQuests/quests/ that are written like it.
     */
    public void readCustom() {
        Map<String, Badge> c = new LinkedHashMap<>();
        JsonObject pack = null;
        java.util.List<Path> files = new java.util.ArrayList<>();
        files.add(plugin.getDataFolder().toPath().resolve("custom-quests.json"));
        Path dir = plugin.getDataFolder().toPath().resolve("quests");
        if (Files.isDirectory(dir)) {
            try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
                walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json")).sorted().forEach(files::add);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not read the quests folder: " + e.getMessage());
            }
        }
        for (Path file : files) {
            JsonObject own = readCustom(file, c);
            if (own != null) pack = own;
        }
        custom = c;
        customPack = pack;
    }

    /** The "badges" of one file into c; returns its "pack" or null. */
    private JsonObject readCustom(Path file, Map<String, Badge> c) {
        JsonObject pack = null;
        try {
            if (Files.exists(file)) {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) {
                    JsonObject o = root.getAsJsonObject();
                    if (o.has("badges") && o.get("badges").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("badges").entrySet()) {
                            if (!e.getValue().isJsonObject()) continue;
                            String id = e.getKey().contains(":") ? e.getKey() : "justquests:" + e.getKey();
                            try {
                                c.put(id.toLowerCase(Locale.ROOT), Badge.parse(id.toLowerCase(Locale.ROOT), e.getValue().getAsJsonObject()));
                            } catch (Exception ex) {
                                plugin.getLogger().warning("Custom badge " + id + " invalid: " + ex.getMessage());
                            }
                        }
                    }
                    if (o.has("pack") && o.get("pack").isJsonObject()) pack = o.getAsJsonObject("pack");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read the badges of " + file.getFileName() + ": " + e.getMessage());
        }
        return pack;
    }

    public Map<String, Badge> all() {
        Map<String, Badge> m = new LinkedHashMap<>(bundled);
        m.putAll(datapack);
        m.putAll(custom);
        return Collections.unmodifiableMap(m);
    }

    public Badge badge(String id) {
        return all().get(id);
    }

    /** Every badge in page order: by sort, then id. */
    public List<Badge> sorted() {
        List<Badge> list = new ArrayList<>(all().values());
        list.sort(Comparator.comparingInt(Badge::sort).thenComparing(Badge::id));
        return list;
    }

    /** pack.json of each datapack namespace, plus "custom" for custom-quests.json's "pack" section. */
    public Map<String, JsonObject> packs() {
        Map<String, JsonObject> m = new LinkedHashMap<>(packs);
        if (customPack != null) m.put("custom", customPack);
        return m;
    }

    // ---- earning

    /** Gives the mod's own badges the player has earned by now (after a quest, and when they join). */
    public void check(Player player) {
        PlayerData data = plugin.store().peek(player.getUniqueId());
        if (data == null) return;
        int done = data.completed.size();
        if (done >= 1) give(player, FIRST_QUEST);
        if (done >= 10) give(player, QUESTS_10);
        if (done >= 50) give(player, QUESTS_50);
        if (done >= 100) give(player, QUESTS_100);
        if (done >= 500) give(player, QUESTS_500);
        if (!data.badges.containsKey(CHAPTER_COMPLETE) && chapterDone(data)) give(player, CHAPTER_COMPLETE);
    }

    private boolean chapterDone(PlayerData data) {
        Map<String, List<String>> chapters = new HashMap<>();
        for (Map.Entry<String, Quest> e : plugin.quests().all().entrySet()) {
            String cat = e.getValue().category();
            if (cat == null || cat.equals("generated") || cat.equals("personal") || cat.equals("weekly")) continue;
            chapters.computeIfAbsent(cat, k -> new ArrayList<>()).add(e.getKey());
        }
        for (List<String> ids : chapters.values()) {
            if (ids.size() >= CHAPTER_MIN && ids.stream().allMatch(data::isCompleted)) return true;
        }
        return false;
    }

    public boolean give(Player player, String id) {
        if (badge(id) == null) {
            plugin.getLogger().warning("Unknown badge " + id);
            return false;
        }
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (data.badges.containsKey(id)) return false;
        data.badges.put(id, System.currentTimeMillis());
        plugin.store().markDirty(player.getUniqueId());
        changed(player);
        return true;
    }

    public boolean take(Player player, String id) {
        PlayerData data = plugin.store().peek(player.getUniqueId());
        if (data == null || data.badges.remove(id) == null) return false;
        plugin.store().markDirty(player.getUniqueId());
        changed(player);
        return true;
    }

    /** Sets the badge before the name: an id the player has, or "none". */
    public boolean show(Player player, String which) {
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (!which.equals("none") && (!data.badges.containsKey(which) || badge(which) == null)) return false;
        data.shownBadge = which;
        plugin.store().markDirty(player.getUniqueId());
        applyName(player);
        return true;
    }

    private void changed(Player player) {
        applyName(player);
        if (plugin.discord() != null) plugin.discord().badgesChanged(player);
    }

    /** The badges the player has (that still exist), newest first. */
    public List<String> earned(PlayerData data) {
        if (data == null) return List.of();
        return data.badges.entrySet().stream().filter(e -> badge(e.getKey()) != null)
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).map(Map.Entry::getKey).toList();
    }

    // ---- the sign before the name

    /** The badge before the player's name: their choice, else the newest; null for none. */
    public Badge shown(PlayerData data) {
        if (data == null || data.badges.isEmpty()) return null;
        if (data.shownBadge != null) {
            if (data.shownBadge.equals("none")) return null;
            if (data.badges.containsKey(data.shownBadge) && badge(data.shownBadge) != null) return badge(data.shownBadge);
        }
        List<String> e = earned(data);
        return e.isEmpty() ? null : badge(e.get(0));
    }

    /** The coloured sign and a space, as legacy text ("" for none or when badgeInName is off). */
    public String sign(UUID player) {
        if (!plugin.settings().badgeInName) return "";
        Badge b = shown(plugin.store().peek(player));
        return b == null ? "" : b.chatColor() + b.sign() + " " + ChatColor.RESET;
    }

    /** The tab list name with the sign before it. */
    public void applyName(Player player) {
        String sign = sign(player.getUniqueId());
        player.setPlayerListName(sign.isEmpty() ? null : sign + player.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        String sign = sign(e.getPlayer().getUniqueId());
        // the format's "%" are placeholders; the sign has none
        if (!sign.isEmpty()) e.setFormat(sign + e.getFormat());
    }
}
