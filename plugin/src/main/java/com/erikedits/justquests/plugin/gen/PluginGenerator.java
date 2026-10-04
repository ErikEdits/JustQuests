package com.erikedits.justquests.plugin.gen;

import com.erikedits.justquests.generator.v2.QuestGeneratorV2;
import com.erikedits.justquests.generator.v2.api.ClaimResult;
import com.erikedits.justquests.generator.v2.api.ClaimState;
import com.erikedits.justquests.generator.v2.api.ClaimView;
import com.erikedits.justquests.generator.v2.api.Difficulty;
import com.erikedits.justquests.generator.v2.api.ExpiredClaim;
import com.erikedits.justquests.generator.v2.api.GeneratorConfig;
import com.erikedits.justquests.generator.v2.api.RewardOptions;
import com.erikedits.justquests.generator.v2.api.RotationResult;
import com.erikedits.justquests.generator.v2.api.StartResult;
import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.progress.GeneratedQuests;
import com.erikedits.justquests.plugin.progress.ProgressService;
import com.erikedits.justquests.plugin.quest.Economy;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The server plugin's quest generator, on the generator core the mod uses:
 * <ul>
 *   <li>the shared board (first come, first served) that grows with the players of the last 7 days,</li>
 *   <li>personal daily quests, weekly quests and the weekly server goal,</li>
 *   <li>better rewards: a choice of rewards, a reward multiplier, a lucky bonus, the personal streak
 *       bonus, loot on weekly quests and goals, and money through an economy plugin.</li>
 * </ul>
 * Everything runs on the server thread; failures are logged and never take the server down.
 */
public final class PluginGenerator implements GeneratedQuests {
    private final JustQuestsPlugin plugin;
    private final Path dir;
    private final BukkitHost host;
    private final PersonalQuests personal;
    private final WeeklyQuests weekly;
    private final ServerGoal goal;
    private final Random random = new Random();
    private QuestGeneratorV2 board;
    private int activePlayers = -1;
    private long activeAt;

    public PluginGenerator(JustQuestsPlugin plugin, Path dir) {
        this.plugin = plugin;
        this.dir = dir;
        this.host = new BukkitHost(plugin, dir);
        this.personal = new PersonalQuests(plugin, dir.resolve("personal.json"));
        this.weekly = new WeeklyQuests(plugin, dir.resolve("weekly.json"));
        this.goal = new ServerGoal(plugin, dir.resolve("goal.json"));
    }

    // --- life cycle ----------------------------------------------------------------------------

    /** Server start, after the quests and the player files are loaded. */
    public void start(Path modWorld) {
        try {
            takeOverModState(modWorld);
            Economy.configure(plugin.settings().moneyCommand, plugin.settings().moneyName);
            board = new QuestGeneratorV2(host, config());
            board.setRewardOptions(rewardOptions());
            Map<String, Set<UUID>> active = new HashMap<>();
            plugin.store().all().forEach((uuid, data) -> data.active.keySet().forEach(id -> {
                if (board.isGenerated(id)) active.computeIfAbsent(id, k -> new HashSet<>()).add(uuid);
            }));
            StartResult r = board.startWithHolders(active);
            dropFromPlayers(r.deadQuestIds());
            applyExpired(r.releasedClaims());
            personal.load();
            weekly.load();
            goal.load();
            renewals();
            for (Player p : Bukkit.getOnlinePlayers()) personal.ensure(p, board, difficulty(), this);
            register();
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Generator] failed to start; generated quests are off", e);
            board = null;
        }
    }

    /** Every minute: renewals, the board's rotation and claim expiry. */
    public void tick() {
        if (board == null) return;
        try {
            boolean changed = renewals();
            int size = boardSize();
            if (size != board.config().questsPerCycle()) board.updateConfig(config());
            RotationResult r = board.tick();
            applyExpired(r.expiredClaims());
            for (Player p : Bukkit.getOnlinePlayers()) changed |= personal.ensure(p, board, difficulty(), this);
            if (changed || r.changed()) register();
            personal.saveIfDirty();
            goal.saveIfDirty();
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Generator] tick failed", e);
        }
    }

    /** Weekly quests and the server goal of a new week. */
    private boolean renewals() {
        boolean changed = weekly.rollIfDue(board, difficulty(), this);
        goal.rollIfDue(weekly.currentWeek(), board, difficulty(), activePlayers(), this);
        return changed;
    }

    public void stop() {
        try {
            if (board != null) board.stop();
            personal.saveIfDirty();
            weekly.save();
            goal.save();
        } catch (RuntimeException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Generator] stop failed", e);
        }
        board = null;
    }

    /** After /quest reload or /quest difficulty. */
    public void reloadConfig() {
        if (board == null) return;
        Economy.configure(plugin.settings().moneyCommand, plugin.settings().moneyName);
        board.updateConfig(config());
        board.setRewardOptions(rewardOptions());
        register();
    }

    /** OP reroll of the board. @return -1 if the generator is off, else the number of quests on the board */
    public int reroll() {
        if (board == null || !board.config().enabled()) return -1;
        board.reroll();
        register();
        return board.servedQuests().size();
    }

    public boolean running() {
        return board != null;
    }

    public QuestGeneratorV2 board() {
        return board;
    }

    public void onJoin(Player p) {
        if (board == null) return;
        goal.onJoin(p);
        if (personal.ensure(p, board, difficulty(), this)) register();
    }

    /** Before the quest book opens: today's personal quests must be there. */
    public void ensurePersonal(Player p) {
        if (board != null && personal.ensure(p, board, difficulty(), this)) register();
    }

    // --- configuration ---------------------------------------------------------------------------

    private Difficulty difficulty() {
        return Difficulty.parse(plugin.settings().difficulty).orElse(Difficulty.NORMAL);
    }

    private RewardOptions rewardOptions() {
        return new RewardOptions(plugin.settings().rewardChoice, plugin.settings().rewardMultiplier);
    }

    private GeneratorConfig config() {
        var s = plugin.settings();
        return GeneratorConfig.builder()
            .enabled(s.generator)
            .maxPerCycle(s.boardMax)
            .questsPerCycle(boardSize())
            .difficulty(difficulty())
            .exclusiveClaims(s.exclusiveClaims)
            .oneActivePerPlayer(s.oneActivePerPlayer)
            .releaseOnAbandon(s.releaseOnAbandon)
            .claimExpiryHours(s.claimExpiryHours)
            .cycleHours(s.cycleHours)
            .cycleAnchorHour(s.cycleAnchorHour)
            .moddedShare(0.0)
            .adaptiveBalancing(s.adaptiveBalancing)
            .statsEnabled(s.generatorStats)
            .build();
    }

    /** baseQuests, plus one per perPlayers players of the last 7 days, at most maxQuests. */
    int boardSize() {
        var s = plugin.settings();
        int extra = s.boardPerPlayers <= 0 ? 0 : activePlayers() / s.boardPerPlayers;
        return Math.max(1, Math.min(s.boardMax, s.boardBase + extra));
    }

    /** Players online or seen in the last 7 days (counted at most every 5 minutes). */
    int activePlayers() {
        long now = System.currentTimeMillis();
        if (activePlayers >= 0 && now - activeAt < 300_000L) return activePlayers;
        long since = now - 7L * 24 * 3600 * 1000;
        Set<UUID> seen = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) seen.add(p.getUniqueId());
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getLastPlayed() >= since) seen.add(p.getUniqueId());
        }
        activePlayers = seen.size();
        activeAt = now;
        return activePlayers;
    }

    // --- quests ----------------------------------------------------------------------------------

    /**
     * A generated quest as the plugin keeps it: its category, money (when an economy command is
     * set), extra loot, and for goals no choice (the reward is paid without asking).
     */
    JsonObject augment(JsonObject source, double value, String category, String extraLoot, boolean noChoice) {
        JsonObject q = source.deepCopy();
        if (category != null) q.addProperty("category", category);
        JsonArray rewards = q.has("rewards") ? q.getAsJsonArray("rewards") : new JsonArray();
        if (noChoice) {
            JsonArray plain = new JsonArray();
            for (JsonElement r : rewards) {
                JsonObject o = r.getAsJsonObject();
                if (o.has("type") && o.get("type").getAsString().equals("justquests:choice") && o.has("options")
                    && o.getAsJsonArray("options").size() > 0) {
                    plain.add(o.getAsJsonArray("options").get(0));
                } else {
                    plain.add(o);
                }
            }
            rewards = plain;
        }
        if (extraLoot != null) {
            JsonObject loot = new JsonObject();
            loot.addProperty("type", "justquests:loot_table");
            loot.addProperty("loot_table", extraLoot);
            rewards.add(loot);
        }
        if (Economy.enabled() && plugin.settings().moneyPerValue > 0) {
            JsonObject money = new JsonObject();
            money.addProperty("type", "justquests:money");
            money.addProperty("amount", Math.max(1, (int) Math.round(value * plugin.settings().moneyPerValue)));
            rewards.add(money);
        }
        q.add("rewards", rewards);
        return q;
    }

    /** Hands the board, personal and weekly quests to the quest registry. */
    void register() {
        Map<String, Quest> map = new LinkedHashMap<>();
        if (board != null && board.config().enabled()) {
            board.servedQuests().forEach((id, json) -> put(map, id, augment(json, board.rewardValue(id), null, null, false)));
        }
        personal.all().forEach((id, json) -> put(map, id, json));
        weekly.all().forEach((id, json) -> put(map, id, json));
        plugin.quests().setGenerated(map);
        plugin.tracker().updateAll();
    }

    private void put(Map<String, Quest> into, String id, JsonObject json) {
        try {
            into.put(id, Quest.parse(id, json));
        } catch (RuntimeException e) {
            plugin.getLogger().warning("[Generator] quest " + id + " skipped: " + e.getMessage());
        }
    }

    /** Takes generated quests that no longer exist away from the players who had them active. */
    void dropFromPlayers(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Map.Entry<UUID, PlayerData> e : plugin.store().all().entrySet()) {
            boolean changed = false;
            for (String id : ids) {
                if (e.getValue().isActive(id)) {
                    e.getValue().abandon(id);
                    if (id.equals(e.getValue().pinned)) e.getValue().pinned = null;
                    changed = true;
                }
            }
            if (changed) plugin.store().markDirty(e.getKey());
        }
    }

    private void applyExpired(List<ExpiredClaim> expired) {
        if (expired == null || expired.isEmpty()) return;
        for (ExpiredClaim e : expired) {
            PlayerData data = plugin.store().peek(e.holder());
            if (data != null) {
                data.abandon(e.questId());
                plugin.store().markDirty(e.holder());
            }
            Player p = Bukkit.getPlayer(e.holder());
            if (p != null) p.spigot().sendMessage(Text.tr(Lang.of(p), "justquests.claim.expired", plugin.progress().title(e.questId(), Lang.of(p))));
        }
        plugin.tracker().updateAll();
    }

    /** The first start with a world that was played with the mod takes over its generator state. */
    private void takeOverModState(Path modWorld) {
        if (modWorld == null || Files.exists(dir.resolve("generator_v2.json"))) return;
        Path from = modWorld.resolve("generator_v2.json");
        if (!Files.exists(from)) return;
        try {
            Files.createDirectories(dir);
            for (String name : new String[]{"generator_v2.json", "generator_v2_stats.json"}) {
                Path f = modWorld.resolve(name);
                if (Files.exists(f)) Files.copy(f, dir.resolve(name));
            }
            Path overrides = modWorld.resolve("generator_v2");
            if (Files.isDirectory(overrides)) {
                try (Stream<Path> files = Files.walk(overrides)) {
                    for (Path f : (Iterable<Path>) files::iterator) {
                        Path to = dir.resolve("generator_v2").resolve(overrides.relativize(f).toString());
                        if (Files.isDirectory(f)) Files.createDirectories(to);
                        else if (!Files.exists(to)) Files.copy(f, to);
                    }
                }
            }
            plugin.getLogger().info("[Generator] took over the generator state of the mod's " + modWorld);
        } catch (IOException e) {
            plugin.getLogger().warning("[Generator] could not take over the mod's generator state: " + e.getMessage());
        }
    }

    // --- GeneratedQuests -------------------------------------------------------------------------

    @Override
    public BaseComponent claim(String id, UUID player, String lang) {
        if (board == null || !board.isGenerated(id)) return null;
        ClaimResult r = board.tryClaim(id, player);
        if (r == ClaimResult.CLAIMED_BY_OTHER) {
            String name = name(board.claim(id).holder());
            if (name != null) return Text.tr(lang, "justquests.claim.taken_by", name);
        }
        if (r == ClaimResult.DISABLED) return Text.tr(lang, "justquests.plugin.claim.disabled");
        return r.proceed() ? null : Text.tr(lang, "justquests.claim." + r.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void abandoned(String id, UUID player) {
        if (board == null || !board.isGenerated(id)) return;
        long rev = board.servedRevision();
        board.onAbandon(id, player);
        if (rev != board.servedRevision()) register();
    }

    @Override
    public void completed(String id, UUID player) {
        if (personal.isPersonal(id)) personal.completed(player, id);
        if (board != null && board.isGenerated(id)) board.onComplete(id, player);
    }

    @Override
    public void releaseAll(UUID player) {
        if (board == null) return;
        long rev = board.servedRevision();
        board.releaseAllFor(player);
        if (rev != board.servedRevision()) register();
    }

    @Override
    public BaseComponent tag(String id, UUID viewer, String lang) {
        ClaimView view = claimView(id);
        if (view == null) return null;
        if (view.holder().equals(viewer)) return view.state() == ClaimState.CLAIMED ? Text.tr(lang, "justquests.claim.tag_yours") : null;
        String name = name(view.holder());
        Object who = name != null ? name : Text.tr(lang, "justquests.another_player");
        return Text.tr(lang, view.state() == ClaimState.COMPLETED ? "justquests.claim.tag_completed" : "justquests.claim.tag_taken", who);
    }

    @Override
    public String bookLine(String id, UUID viewer, String lang) {
        if (personal.isPersonal(id)) return Text.legacy(lang, "justquests.plugin.personal.line", personal.streak(viewer));
        ClaimView view = claimView(id);
        if (view == null) return null;
        if (view.holder().equals(viewer)) return view.state() == ClaimState.CLAIMED ? Text.legacy(lang, "justquests.book.reserved") : null;
        String name = name(view.holder());
        String who = name != null ? name : Text.legacy(lang, "justquests.another_player");
        return Text.legacy(lang, view.state() == ClaimState.COMPLETED ? "justquests.book.completed_by" : "justquests.book.taken_by", who);
    }

    @Override
    public boolean takenByOther(String id, UUID viewer) {
        ClaimView view = claimView(id);
        return view != null && !view.holder().equals(viewer);
    }

    /** A board quest someone holds or finished (exclusive claims only), else null. */
    private ClaimView claimView(String id) {
        if (board == null || !board.config().exclusiveClaims() || !board.isGenerated(id)) return null;
        ClaimView view = board.claim(id);
        return view.state() == ClaimState.AVAILABLE || view.holder() == null ? null : view;
    }

    @Override
    public boolean visible(String id, UUID viewer) {
        if (!personal.isPersonal(id)) return true;
        UUID owner = personal.owner(id);
        return owner != null && owner.equals(viewer);
    }

    @Override
    public void progress(Player player, ProgressService.Test test) {
        goal.offer(player, test);
    }

    /** The lucky bonus: on generated quests, a chance of an extra item from config.yml's list. */
    @Override
    public void bonus(Player player, Quest quest) {
        String id = quest.id();
        boolean generated = personal.isPersonal(id) || weekly.isWeekly(id) || (board != null && board.isGenerated(id));
        if (!generated || random.nextDouble() >= plugin.settings().luckyChance) return;
        record Prize(Material item, int count, double weight) {}
        List<Prize> prizes = new ArrayList<>();
        double total = 0;
        for (String line : plugin.settings().lucky) {
            String[] parts = line.trim().split("\\s+");
            try {
                Material m = Registry.MATERIAL.get(NamespacedKey.fromString(parts[0].toLowerCase(Locale.ROOT)));
                int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                double weight = parts.length > 2 ? Double.parseDouble(parts[2]) : 1;
                if (m != null && m.isItem() && count > 0 && weight > 0) {
                    prizes.add(new Prize(m, count, weight));
                    total += weight;
                }
            } catch (RuntimeException ignored) {
                // a broken line is skipped
            }
        }
        if (prizes.isEmpty()) return;
        double roll = random.nextDouble() * total;
        Prize prize = prizes.get(prizes.size() - 1);
        for (Prize p : prizes) {
            roll -= p.weight();
            if (roll < 0) {
                prize = p;
                break;
            }
        }
        int max = prize.item().getMaxStackSize();
        for (int left = prize.count(); left > 0; left -= max) Reward.give(player, new ItemStack(prize.item(), Math.min(left, max)));
        String lang = Lang.of(player);
        player.spigot().sendMessage(Text.tr(lang, "justquests.plugin.lucky", prize.count(), Text.item(prize.item())));
        player.playSound(player.getLocation(), "minecraft:entity.player.levelup", org.bukkit.SoundCategory.MASTER, 0.8f, 1.4f);
    }

    // --- views for commands and the quest book ------------------------------------------------------

    public List<BaseComponent> goalLines(String lang, UUID viewer) {
        return goal.describe(lang, viewer);
    }

    public boolean goalActive() {
        return board != null && goal.active();
    }

    public int goalPercent() {
        return (int) Math.min(100, goal.total() * 100 / Math.max(1, goal.required()));
    }

    public boolean goalDone() {
        return goal.done();
    }

    public Material goalIcon() {
        Quest q = goal.quest();
        return q == null ? Material.BEACON : q.objectives().get(0).icon();
    }

    public int streak(UUID player) {
        return personal.streak(player);
    }

    /** Multi-line generator status for /quest generator status. */
    public String status() {
        if (board == null) return "§cThe quest generator is not running.";
        return board.status() + "\n§7Board: " + board.servedQuests().size() + " of " + boardSize()
            + " (players of the last 7 days: " + activePlayers() + ")"
            + "\n§7Weekly quests: " + weekly.all().size() + ", server goal: "
            + (goal.active() ? goal.total() + "/" + goal.required() + (goal.done() ? " (reached)" : "") : "none")
            + "\n§7Personal quests: " + personal.all().size() + " held by players";
    }

    public String stats() {
        return board == null ? "§cThe quest generator is not running." : board.stats().toText();
    }

    public String explain(String id) {
        return board == null ? "§cThe quest generator is not running." : board.explain(id);
    }

    public List<String> boardIds() {
        return board == null ? List.of() : new ArrayList<>(board.servedQuests().keySet());
    }

    /** /quest generator preview: the next board, nothing changes. */
    public String preview(int count) {
        if (board == null) return "§cThe quest generator is not running.";
        StringBuilder sb = new StringBuilder("§7Preview of the next board (nothing changes):");
        for (JsonObject q : board.preview(count)) {
            JsonElement t = q.get("title");
            String title = t == null ? "?" : t.isJsonObject() && t.getAsJsonObject().has("en_us")
                ? t.getAsJsonObject().get("en_us").getAsString() : t.getAsString();
            sb.append("\n§f").append(title).append(" §8- §7").append(q.get("objectives"));
        }
        return sb.toString();
    }

    /** /quest generator release: frees a board quest and takes it from its holder. */
    public String release(String id) {
        if (board == null) return "§cThe quest generator is not running.";
        if (!board.isGenerated(id)) return "§c" + id + " is not a board quest.";
        var former = board.forceRelease(id);
        if (former.isEmpty()) return "§7" + id + " was not taken by anyone.";
        PlayerData data = plugin.store().peek(former.get());
        if (data != null) {
            data.abandon(id);
            plugin.store().markDirty(former.get());
        }
        Player p = Bukkit.getPlayer(former.get());
        if (p != null) {
            p.spigot().sendMessage(Text.tr(Lang.of(p), "justquests.claim.released_by_op", plugin.progress().title(id, Lang.of(p))));
            plugin.tracker().update(p);
        }
        register();
        return "§aReleased " + id + ".";
    }

    /** /quest test: the core's own checks plus served == registered. Empty = healthy. */
    public List<String> selfTest() {
        List<String> problems = new ArrayList<>();
        if (board == null) {
            problems.add("generator not running");
            return problems;
        }
        problems.addAll(board.selfTest());
        Set<String> served = new HashSet<>(board.servedQuests().keySet());
        Set<String> registered = new HashSet<>();
        for (String id : plugin.quests().all().keySet()) {
            if (board.isGenerated(id)) registered.add(id);
        }
        if (board.config().enabled() && !registered.equals(served)) {
            problems.add("registered board quests (" + registered.size() + ") differ from the board (" + served.size() + ")");
        }
        return problems;
    }

    private static String name(UUID id) {
        if (id == null) return null;
        OfflinePlayer p = Bukkit.getOfflinePlayer(id);
        return p.getName();
    }

    /** A lore line "Streak: 3 days (+30%)", or null without a streak. */
    public BaseComponent streakLine(String lang, UUID player) {
        int s = personal.streak(player);
        if (s <= 0) return null;
        int pct = (int) Math.round(100 * Math.min(plugin.settings().streakMax, s * plugin.settings().streakBonus));
        return Text.tr(lang, "justquests.plugin.streak", s, pct);
    }
}
