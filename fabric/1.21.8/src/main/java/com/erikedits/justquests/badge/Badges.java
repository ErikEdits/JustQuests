package com.erikedits.justquests.badge;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.Badge;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.storage.WorldSettings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The badges (0.6.0): where they come from, the rules of the mod's own ones, and giving them.
 * <ul>
 * <li>Datapacks: data/&lt;ns&gt;/justquests/badges/&lt;path&gt;.json is the badge &lt;ns&gt;:&lt;path&gt;; the mod's own
 *     eight are in its built-in datapack. A pack that wants its badges as roles on the JustQuests Discord
 *     adds data/&lt;ns&gt;/justquests/pack.json (see {@link com.erikedits.justquests.discord.DiscordLink}).</li>
 * <li>A world's custom-quests.json: its "badges" section, read by CustomQuestLoader.</li>
 * </ul>
 * The mod's own badges are earned by rules ({@link #check}); every other badge by the justquests:badge
 * reward of a quest, or by /quest admin badge give.
 */
public final class Badges {
    public static final ResourceLocation FIRST_QUEST = own("first_quest");
    public static final ResourceLocation QUESTS_10 = own("quests_10");
    public static final ResourceLocation QUESTS_50 = own("quests_50");
    public static final ResourceLocation QUESTS_100 = own("quests_100");
    public static final ResourceLocation QUESTS_500 = own("quests_500");
    public static final ResourceLocation CHAPTER_COMPLETE = own("chapter_complete");
    public static final ResourceLocation DAILY_STREAK_7 = own("daily_streak_7");
    public static final ResourceLocation COMMUNITY_GOAL = own("community_goal");
    /** A category is a chapter for chapter_complete when it has at least this many quests. */
    public static final int CHAPTER_MIN = 3;

    private static Map<ResourceLocation, Badge> datapack = new HashMap<>();
    private static Map<ResourceLocation, Badge> custom = new HashMap<>();
    /** pack.json of each datapack namespace that has one (for the Discord request of its badges). */
    private static Map<String, JsonObject> packs = new HashMap<>();
    /** The "pack" section of custom-quests.json, if any. */
    private static JsonObject customPack;

    private Badges() {}

    private static ResourceLocation own(String path) {
        return ResourceLocation.tryParse(JustQuests.MOD_ID + ":" + path);
    }

    /** Reads every datapack badge and pack.json; called when the quests are (re)loaded. */
    public static void load(ResourceManager resources) {
        Map<ResourceLocation, Badge> loaded = new HashMap<>();
        Map<String, JsonObject> packInfo = new HashMap<>();
        Map<ResourceLocation, Resource> found = resources.listResources("justquests/badges", rl -> rl.getPath().endsWith(".json"));
        found.forEach((file, resource) -> {
            String path = file.getPath().substring("justquests/badges/".length(), file.getPath().length() - ".json".length());
            ResourceLocation id = ResourceLocation.tryParse(file.getNamespace() + ":" + path);
            if (id == null) return;
            try (Reader in = resource.openAsReader()) {
                Badge.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(in))
                    .resultOrPartial(err -> JustQuests.LOG.error("Badge {} invalid: {}", id, err))
                    .ifPresent(b -> loaded.put(id, b));
            } catch (Exception e) {
                JustQuests.LOG.error("Badge {} could not be read: {}", id, e.getMessage());
            }
        });
        for (String ns : loaded.keySet().stream().map(ResourceLocation::getNamespace).distinct().toList()) {
            resources.getResource(ResourceLocation.tryParse(ns + ":justquests/pack.json")).ifPresent(r -> {
                try (Reader in = r.openAsReader()) {
                    JsonElement json = JsonParser.parseReader(in);
                    if (json.isJsonObject()) packInfo.put(ns, json.getAsJsonObject());
                } catch (Exception e) {
                    JustQuests.LOG.error("{}:justquests/pack.json could not be read: {}", ns, e.getMessage());
                }
            });
        }
        datapack = loaded;
        packs = packInfo;
        JustQuests.LOG.info("Loaded {} badge(s)", loaded.size());
    }

    /** The badges and the "pack" section of a world's custom-quests.json. */
    public static void setCustom(Map<ResourceLocation, Badge> badges, JsonObject pack) {
        custom = badges;
        customPack = pack;
    }

    /** Every badge, custom ones over datapack ones of the same id. */
    public static Map<ResourceLocation, Badge> all() {
        Map<ResourceLocation, Badge> merged = new HashMap<>(datapack);
        merged.putAll(custom);
        return Collections.unmodifiableMap(merged);
    }

    public static Badge get(ResourceLocation id) {
        Badge b = custom.get(id);
        return b != null ? b : datapack.get(id);
    }

    /** Every badge sorted for the page: by sort, then by id. */
    public static List<Map.Entry<ResourceLocation, Badge>> sorted() {
        List<Map.Entry<ResourceLocation, Badge>> list = new ArrayList<>(all().entrySet());
        list.sort(Comparator.<Map.Entry<ResourceLocation, Badge>>comparingInt(e -> e.getValue().sort())
            .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    /** The pack.json of each namespace (datapacks) plus "custom" for custom-quests.json's "pack" section. */
    public static Map<String, JsonObject> packs() {
        Map<String, JsonObject> all = new LinkedHashMap<>(packs);
        if (customPack != null) all.put("custom", customPack);
        return all;
    }

    // ---- earning

    /**
     * Gives the mod's own badges the player has earned by now: after a quest is finished and when they
     * join (a world from before 0.6.0 gets them for what was done before).
     */
    public static void check(ServerPlayer player) {
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        if (data == null) return;
        int done = data.completed.size();
        if (done >= 1) give(player, FIRST_QUEST);
        if (done >= 10) give(player, QUESTS_10);
        if (done >= 50) give(player, QUESTS_50);
        if (done >= 100) give(player, QUESTS_100);
        if (done >= 500) give(player, QUESTS_500);
        if (!data.badges.containsKey(CHAPTER_COMPLETE) && chapterDone(player, data)) give(player, CHAPTER_COMPLETE);
    }

    /** Whether every quest of some category (a chapter: at least CHAPTER_MIN quests, not generated) is done. */
    private static boolean chapterDone(ServerPlayer player, PlayerQuestData data) {
        Map<String, List<ResourceLocation>> chapters = new HashMap<>();
        QuestManager.INSTANCE.getQuests().forEach((id, q) -> {
            if ("generated".equals(q.category()) || !com.erikedits.justquests.perm.Perms.quest(player, q)) return;
            chapters.computeIfAbsent(q.category(), k -> new ArrayList<>()).add(id);
        });
        for (List<ResourceLocation> ids : chapters.values()) {
            if (ids.size() >= CHAPTER_MIN && ids.stream().allMatch(data::isCompleted)) return true;
        }
        return false;
    }

    /** Gives a badge; false if the player has it already or it doesn't exist. */
    public static boolean give(ServerPlayer player, ResourceLocation id) {
        Badge badge = get(id);
        WorldQuestStore store = WorldQuestStore.get();
        if (badge == null || store == null) {
            if (badge == null) JustQuests.LOG.warn("Unknown badge {} (no data/{}/justquests/badges/{}.json)", id, id.getNamespace(), id.getPath());
            return false;
        }
        PlayerQuestData data = store.get(player.getUUID());
        if (data.badges.containsKey(id)) return false;
        data.badges.put(id, System.currentTimeMillis());
        store.markDirty();
        changed(player);
        return true;
    }

    /** Takes a badge away (admin); false if the player doesn't have it. */
    public static boolean take(ServerPlayer player, ResourceLocation id) {
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        if (data == null || data.badges.remove(id) == null) return false;
        store.markDirty();
        changed(player);
        return true;
    }

    /** The player's badges changed: their name sign, their quest book and their Discord roles follow. */
    public static void changed(ServerPlayer player) {
        NameSign.refresh(player);
        com.erikedits.justquests.discord.DiscordLink.badgesChanged(player);
        com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
    }

    // ---- the sign before the name

    /** The badge whose sign stands before the player's name: their choice, else the newest; null for none. */
    public static ResourceLocation shown(PlayerQuestData data) {
        if (data == null || data.badges.isEmpty()) return null;
        if (data.shownBadge.isPresent()) {
            if ("none".equals(data.shownBadge.get())) return null;
            ResourceLocation id = ResourceLocation.tryParse(data.shownBadge.get());
            if (id != null && data.badges.containsKey(id) && get(id) != null) return id;
        }
        return data.badges.entrySet().stream()
            .filter(e -> get(e.getKey()) != null)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);
    }

    /** The coloured sign and a space for before the player's name (its tooltip names the badge), or null. */
    public static MutableComponent nameSign(UUID player) {
        if (!WorldSettings.badgeInName()) return null;
        WorldQuestStore store = WorldQuestStore.get();
        ResourceLocation id = shown(store == null ? null : store.peek(player));
        Badge badge = id == null ? null : get(id);
        if (badge == null) return null;
        // an empty root, so that the name appended to it keeps its own colour
        MutableComponent sign = com.erikedits.justquests.text.Msg.hover(
            Component.literal(badge.sign()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(badge.rgb()))),
            Component.literal(badge.name().getDefault()));
        return Component.empty().append(sign).append(" ");
    }

    /** The badges for the client's quest book (full sync). */
    public static JsonObject json() {
        JsonObject out = new JsonObject();
        for (Map.Entry<ResourceLocation, Badge> e : sorted()) {
            Badge.CODEC.encodeStart(JsonOps.INSTANCE, e.getValue()).result().ifPresent(j -> out.add(e.getKey().toString(), j));
        }
        return out;
    }

    /** The player's badges, newest first, that still exist. */
    public static List<ResourceLocation> earned(PlayerQuestData data) {
        if (data == null) return List.of();
        return data.badges.entrySet().stream()
            .filter(e -> get(e.getKey()) != null)
            .sorted(Map.Entry.<ResourceLocation, Long>comparingByValue().reversed())
            .map(Map.Entry::getKey).toList();
    }
}
