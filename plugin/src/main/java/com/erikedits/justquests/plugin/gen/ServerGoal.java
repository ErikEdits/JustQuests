package com.erikedits.justquests.plugin.gen;

import com.erikedits.justquests.generator.v2.QuestGeneratorV2;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.GeneratedQuest;
import com.erikedits.justquests.generator.v2.api.SetRequest;
import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.progress.ProgressService;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The weekly server goal: one big objective everyone works on together, sized by the players who
 * played in the last 7 days. Matching actions of every player count by themselves; 25, 50, 75 and
 * 100 % are announced. Everyone who helped gets the same reward; players who are offline when the
 * goal is reached get it on their next join.
 */
final class ServerGoal {
    /**
     * Server goals are about mining, gathering, fighting, breeding and smelting, not about crafting
     * the same item thousands of times: every other objective type is left out.
     */
    static final Set<String> EXCLUDED = Set.of("craft_item", "place_block", "consume_item", "use_item", "enchant_item",
        "tame_animal", "reach_level", "reach_location", "visit_dimension", "gain_advancement");
    /** Middle of the normal difficulty's play time per quest, in minutes. */
    private static final double BASE_MINUTES = 14.0;
    /** Reward value each helper gets (about an hour of play). */
    private static final double REWARD_VALUE = 80.0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final JustQuestsPlugin plugin;
    private final Path file;
    private final Fireworks fireworks;
    private long week = -1;
    private JsonObject json;
    private Quest quest;
    private long total;
    private int milestones;
    private boolean done;
    private final Map<UUID, Long> contributions = new LinkedHashMap<>();
    /** helpers who were offline when a goal was reached -> the weeks of those goals */
    private final Map<UUID, Set<Long>> pending = new LinkedHashMap<>();
    /** the rewards of reached goals while a helper still waits for them, by week */
    private final Map<Long, JsonArray> pendingRewards = new LinkedHashMap<>();
    private boolean dirty;

    ServerGoal(JustQuestsPlugin plugin, Path file, Fireworks fireworks) {
        this.plugin = plugin;
        this.file = file;
        this.fireworks = fireworks;
    }

    boolean active() {
        return quest != null && plugin.settings().serverGoal;
    }

    Quest quest() {
        return quest;
    }

    long total() {
        return total;
    }

    long required() {
        return quest == null ? 0 : quest.objectives().get(0).requiredCount();
    }

    boolean done() {
        return done;
    }

    long contribution(UUID player) {
        return contributions.getOrDefault(player, 0L);
    }

    int helpers() {
        return contributions.size();
    }

    long week() {
        return week;
    }

    /** A new goal when a new week began; true if there is one. */
    boolean rollIfDue(long now, QuestGeneratorV2 gen, Difficulty difficulty, int activePlayers, PluginGenerator owner) {
        if (!plugin.settings().serverGoal || now == week) return false;
        int players = Math.max(1, activePlayers);
        double minutes = players * (double) plugin.settings().goalMinutesPerPlayer;
        double budget = Math.max(0.01, Math.min(1.0, REWARD_VALUE / (minutes * 1.2)));
        long seed = GenTime.mix(GenTime.mix(BukkitHost.mainWorld() == null ? 0 : BukkitHost.mainWorld().getSeed(), now), 7L);
        List<GeneratedQuest> made = gen.generateSet(new SetRequest(seed, 1, difficulty, minutes / BASE_MINUTES, budget,
            true, true, null, Set.of(), EXCLUDED));
        week = now;
        total = 0;
        milestones = 0;
        done = false;
        contributions.clear();
        if (made.isEmpty()) {
            json = null;
            quest = null;
            plugin.getLogger().warning("[Generator] no server goal could be generated this week");
        } else {
            json = owner.augment(made.get(0).json(), made.get(0).rewardValue(), "goal", "minecraft:chests/end_city_treasure", true);
            quest = parse(json);
            plugin.getLogger().info("[Generator] server goal for " + players + " player(s): "
                + (quest == null ? "invalid" : quest.objectives().get(0).label()));
        }
        dirty = true;
        save();
        return quest != null;
    }

    private Quest parse(JsonObject j) {
        try {
            return Quest.parse("justquests:goal/" + week, j);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("[Generator] server goal invalid: " + e.getMessage());
            return null;
        }
    }

    /** Every game event: what it adds to the goal (not in creative or spectator mode, not from NPCs). */
    void offer(Player player, ProgressService.Test test) {
        if (!active() || done) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.hasMetadata("NPC")) return;   // Citizens and similar mark their fake players so
        Objective obj = quest.objectives().get(0);
        int n = test.amount(obj);
        if (n <= 0) return;
        long add = Math.min(n, required() - total);
        if (add <= 0) return;
        total += add;
        contributions.merge(player.getUniqueId(), add, Long::sum);
        dirty = true;
        int reached = (int) Math.min(4, total * 4 / Math.max(1, required()));
        while (milestones < reached) {
            milestones++;
            announce(milestones * 25);
        }
        if (total >= required()) finish();
    }

    private void announce(int percent) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String lang = Lang.of(p);
            p.spigot().sendMessage(Text.tr(lang, percent >= 100 ? "justquests.plugin.goal.reached" : "justquests.plugin.goal.milestone",
                percent, quest.objectives().get(0).display(lang)));
        }
    }

    private void finish() {
        done = true;
        for (UUID id : contributions.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                reward(p, quest.rewards());
            } else {
                pending.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(week);
                pendingRewards.put(week, json.getAsJsonArray("rewards").deepCopy());
            }
        }
        if (plugin.settings().goalFireworks) fireworks.celebrate();
        dirty = true;
        save();
    }

    /** Pays a helper's goal reward: the same for everyone who helped. */
    private void reward(Player p, List<Reward> rewards) {
        for (Reward r : rewards) r.grant(p);
        String lang = Lang.of(p);
        p.spigot().sendMessage(Text.tr(lang, "justquests.plugin.goal.rewarded"));
        p.playSound(p.getLocation(), "minecraft:ui.toast.challenge_complete", SoundCategory.MASTER, 1f, 1f);
    }

    /** A helper who was offline when a goal was reached gets that goal's reward now. */
    void onJoin(Player p) {
        Set<Long> weeks = pending.remove(p.getUniqueId());
        if (weeks == null) return;
        for (long w : weeks) {
            JsonArray rewards = pendingRewards.get(w);
            if (rewards == null) continue;
            List<Reward> list = new ArrayList<>();
            for (JsonElement e : rewards) {
                try {
                    list.add(Reward.parse(e.getAsJsonObject()));
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("[Generator] a server goal reward could not be read: " + ex.getMessage());
                }
            }
            reward(p, list);
        }
        // rewards nobody waits for any more are dropped
        pendingRewards.keySet().removeIf(w -> pending.values().stream().noneMatch(s -> s.contains(w)));
        dirty = true;
    }

    /** Lines about the goal for /quest goal and the quest book. */
    List<BaseComponent> describe(String lang, UUID viewer) {
        List<BaseComponent> out = new ArrayList<>();
        if (!active()) {
            out.add(Text.tr(lang, "justquests.plugin.goal.none"));
            return out;
        }
        long req = required();
        int pct = (int) Math.min(100, total * 100 / Math.max(1, req));
        out.add(Text.tr(lang, "justquests.plugin.goal.objective", quest.objectives().get(0).display(lang)));
        out.add(Text.lit(bar(pct) + " §f" + total + "/" + req + " §7(" + pct + "%)"));
        if (viewer != null) out.add(Text.tr(lang, "justquests.plugin.goal.yours", contribution(viewer)));
        out.add(Text.tr(lang, "justquests.plugin.goal.helpers", helpers()));
        out.add(done ? Text.tr(lang, "justquests.plugin.goal.done") : Text.tr(lang, "justquests.plugin.goal.left", timeLeft(lang)));
        out.add(Text.tr(lang, "justquests.plugin.goal.rewards"));
        for (Reward r : quest.rewards()) {
            BaseComponent line = Text.lit(" §a");
            line.addExtra(r.display(lang));
            out.add(line);
        }
        return out;
    }

    /** "3d 4h" (or "25m") until the goal's week ends. */
    BaseComponent timeLeft(String lang) {
        return duration(lang, GenTime.weekLeft(week, plugin.settings().weeklyHour));
    }

    private static String bar(int pct) {
        int filled = Math.max(0, Math.min(20, pct / 5));
        return "§a" + "■".repeat(filled) + "§8" + "■".repeat(20 - filled);
    }

    private static BaseComponent duration(String lang, Duration d) {
        long days = d.toDays();
        long hours = d.minusDays(days).toHours();
        return days > 0 ? Text.tr(lang, "justquests.plugin.goal.days_hours", days, hours)
            : ProgressService.duration(lang, Math.max(60_000L, d.toMillis()));
    }

    // --- persistence -------------------------------------------------------------------------

    void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            week = root.has("week") ? root.get("week").getAsLong() : -1;
            json = root.has("quest") && root.get("quest").isJsonObject() ? root.getAsJsonObject("quest") : null;
            quest = json == null ? null : parse(json);
            total = root.has("total") ? root.get("total").getAsLong() : 0;
            milestones = root.has("milestones") ? root.get("milestones").getAsInt() : 0;
            done = root.has("done") && root.get("done").getAsBoolean();
            if (root.has("contributions")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("contributions").entrySet()) {
                    contributions.put(UUID.fromString(e.getKey()), e.getValue().getAsLong());
                }
            }
            if (root.has("pendingRewards")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("pendingRewards").entrySet()) {
                    pendingRewards.put(Long.parseLong(e.getKey()), e.getValue().getAsJsonArray());
                }
            }
            if (root.has("pending") && root.get("pending").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("pending").entrySet()) {
                    Set<Long> weeks = new LinkedHashSet<>();
                    for (JsonElement w : e.getValue().getAsJsonArray()) weeks.add(w.getAsLong());
                    pending.put(UUID.fromString(e.getKey()), weeks);
                }
            } else if (root.has("pending") && json != null && json.has("rewards")) {
                // the first version kept only the players: they wait for this week's goal
                for (JsonElement e : root.getAsJsonArray("pending")) {
                    pending.computeIfAbsent(UUID.fromString(e.getAsString()), k -> new LinkedHashSet<>()).add(week);
                }
                if (!pending.isEmpty()) pendingRewards.put(week, json.getAsJsonArray("rewards").deepCopy());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Generator] could not read " + file.getFileName() + ": " + e.getMessage());
        }
    }

    void saveIfDirty() {
        if (dirty) save();
    }

    void save() {
        JsonObject root = new JsonObject();
        root.addProperty("week", week);
        if (json != null) root.add("quest", json);
        root.addProperty("total", total);
        root.addProperty("milestones", milestones);
        root.addProperty("done", done);
        JsonObject c = new JsonObject();
        contributions.forEach((k, v) -> c.addProperty(k.toString(), v));
        root.add("contributions", c);
        JsonObject p = new JsonObject();
        pending.forEach((id, weeks) -> {
            JsonArray a = new JsonArray();
            weeks.forEach(a::add);
            p.add(id.toString(), a);
        });
        root.add("pending", p);
        JsonObject pr = new JsonObject();
        pendingRewards.forEach((w, rewards) -> pr.add(String.valueOf(w), rewards));
        root.add("pendingRewards", pr);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
            dirty = false;
        } catch (Exception e) {
            plugin.getLogger().warning("[Generator] could not save " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
