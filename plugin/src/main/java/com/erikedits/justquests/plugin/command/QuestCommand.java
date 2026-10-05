package com.erikedits.justquests.plugin.command;

import com.erikedits.justquests.plugin.Community;
import com.erikedits.justquests.plugin.JustQuestsPlugin;
import com.erikedits.justquests.plugin.data.PlayerData;
import com.erikedits.justquests.plugin.gui.MainMenu;
import com.erikedits.justquests.plugin.progress.ProgressService;
import com.erikedits.justquests.plugin.progress.QuestStatus;
import com.erikedits.justquests.plugin.quest.Objective;
import com.erikedits.justquests.plugin.quest.Quest;
import com.erikedits.justquests.plugin.quest.Reward;
import com.erikedits.justquests.plugin.text.Lang;
import com.erikedits.justquests.plugin.text.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /quest (also /quests and /jq): without arguments it opens the quest book; the subcommands are the
 * mod's, plus book, track and bossbar. Player subcommands need justquests.command.&lt;name&gt;
 * (everyone by default), operator ones justquests.admin.&lt;name&gt; (operators by default).
 */
public final class QuestCommand implements TabExecutor {
    public static final List<String> PLAYER = List.of("open", "list", "categories", "stats", "leaderboard", "progress",
        "accept", "abandon", "claim", "track", "bossbar", "book", "goal", "discord");
    public static final List<String> ADMIN = List.of("reload", "reroll", "mainquests", "difficulty", "generator", "test", "admin");

    private final JustQuestsPlugin plugin;

    public QuestCommand(JustQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    private static void say(CommandSender s, BaseComponent msg) {
        if (s instanceof Player p) p.spigot().sendMessage(msg);
        else s.sendMessage(msg.toLegacyText());
    }

    private void say(CommandSender s, String key, Object... args) {
        say(s, Text.tr(Lang.of(s), key, args));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "open" : args[0].toLowerCase(Locale.ROOT);
        boolean admin = ADMIN.contains(sub);
        if (!PLAYER.contains(sub) && !admin) {
            help(sender, label);
            return true;
        }
        if (!plugin.allowed(sender, (admin ? "justquests.admin." : "justquests.command.") + sub, admin)) {
            say(sender, "justquests.plugin.no_permission");
            return true;
        }
        String[] rest = java.util.Arrays.copyOfRange(args, Math.min(1, args.length), args.length);
        switch (sub) {
            case "open" -> withPlayer(sender, p -> new MainMenu(plugin, p).open());
            case "list" -> list(sender, rest.length > 0 ? rest[0] : null);
            case "categories" -> categories(sender);
            case "stats" -> withPlayer(sender, p -> stats(p));
            case "leaderboard" -> leaderboard(sender);
            case "progress" -> withPlayer(sender, p -> progress(p));
            case "accept" -> withPlayer(sender, p -> {
                if (rest.length < 1) say(p, "justquests.plugin.usage", "/" + label + " accept <quest>");
                else say(p, plugin.progress().accept(p, id(rest[0])).message());
            });
            case "abandon" -> withPlayer(sender, p -> {
                if (rest.length < 1) say(p, "justquests.plugin.usage", "/" + label + " abandon <quest>");
                else say(p, plugin.progress().abandon(p, id(rest[0])).message());
            });
            case "claim" -> withPlayer(sender, p -> {
                if (rest.length < 1) claimAll(p);
                else claim(p, id(rest[0]), rest.length > 1 ? parseInt(rest[1]) : 0);
            });
            case "track" -> withPlayer(sender, p -> track(p, rest.length > 0 ? id(rest[0]) : null));
            case "bossbar" -> withPlayer(sender, p -> {
                PlayerData d = plugin.store().get(p.getUniqueId());
                d.bossbar = rest.length > 0 ? rest[0].equalsIgnoreCase("on") : !d.bossbar;
                plugin.store().markDirty(p.getUniqueId());
                plugin.tracker().update(p);
                say(p, d.bossbar ? "justquests.plugin.bossbar.on" : "justquests.plugin.bossbar.off");
            });
            case "book" -> withPlayer(sender, p -> say(p, plugin.book().give(p) ? "justquests.plugin.book.given" : "justquests.plugin.book.has"));
            case "goal" -> {
                if (!plugin.generator().goalActive()) say(sender, "justquests.plugin.goal.none");
                else plugin.generator().goalLines(Lang.of(sender), sender instanceof Player p ? p.getUniqueId() : null).forEach(l -> say(sender, l));
            }
            case "discord" -> say(sender, Community.discord(Lang.of(sender)));
            case "reroll" -> {
                int n = plugin.generator().reroll();
                if (n < 0) say(sender, "justquests.plugin.reroll.disabled");
                else say(sender, "justquests.reroll.done", n);
            }
            case "difficulty" -> {
                if (rest.length == 0 || !List.of("easy", "normal", "hard").contains(rest[0].toLowerCase(Locale.ROOT))) {
                    say(sender, "justquests.difficulty.show", plugin.settings().difficulty);
                } else {
                    plugin.setDifficulty(rest[0].toLowerCase(Locale.ROOT));
                    say(sender, "justquests.difficulty.set", plugin.settings().difficulty);
                }
            }
            case "generator" -> generator(sender, label, rest);
            case "reload" -> {
                int n = plugin.reload();
                say(sender, "justquests.plugin.reload.done", n);
            }
            case "mainquests" -> mainQuests(sender, rest);
            case "test" -> plugin.selfTest().forEach(line -> say(sender, Text.lit(line)));
            case "admin" -> admin(sender, label, rest);
            default -> help(sender, label);
        }
        return true;
    }

    private void withPlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (sender instanceof Player p) action.accept(p);
        else say(sender, "justquests.plugin.players_only");
    }

    /** "first_steps" -> "justquests:first_steps". */
    private static String id(String s) {
        s = s.toLowerCase(Locale.ROOT);
        return s.contains(":") ? s : "justquests:" + s;
    }

    /** The quests a player sees (not other players' personal quests); all of them for the console. */
    private Collection<Quest> quests(CommandSender sender) {
        return sender instanceof Player p ? plugin.visibleQuests(p) : plugin.quests().all().values();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void help(CommandSender s, String label) {
        String lang = Lang.of(s);
        say(s, Text.tr(lang, "justquests.plugin.help.header"));
        for (String sub : PLAYER) {
            if (plugin.allowed(s, "justquests.command." + sub, false)) {
                say(s, Text.lit("§e/" + label + " " + sub + " §7- " + Text.legacy(lang, "justquests.plugin.help." + sub)));
            }
        }
        for (String sub : ADMIN) {
            if (plugin.allowed(s, "justquests.admin." + sub, true)) {
                say(s, Text.lit("§6/" + label + " " + sub + " §7- " + Text.legacy(lang, "justquests.plugin.help." + sub)));
            }
        }
    }

    // --- player subcommands -------------------------------------------------------------------

    private void list(CommandSender src, String category) {
        String lang = Lang.of(src);
        Player viewer = src instanceof Player p ? p : null;
        PlayerData data = viewer == null ? null : plugin.store().peek(viewer.getUniqueId());
        List<Quest> entries = quests(src).stream()
            .filter(q -> category == null || q.category().equalsIgnoreCase(category))
            .sorted(Comparator.comparing(Quest::category, String.CASE_INSENSITIVE_ORDER).thenComparingInt(Quest::sort).thenComparing(Quest::id))
            .collect(Collectors.toList());
        if (entries.isEmpty()) {
            say(src, category == null ? Text.tr(lang, "justquests.list.none") : Text.tr(lang, "justquests.list.none_category", category));
            return;
        }
        say(src, category == null ? Text.tr(lang, "justquests.list.header") : Text.tr(lang, "justquests.list.header_category", category));
        String shown = null;
        for (Quest q : entries) {
            if (category == null && !q.category().equals(shown)) {
                shown = q.category();
                say(src, Text.tr(lang, "justquests.list.category", plugin.categoryName(lang, shown)));
            }
            String missing = plugin.progress().missingRequirement(data, q);
            if (data != null && missing != null) {
                say(src, Text.tr(lang, "justquests.list.locked", q.id(), q.title().get(lang), plugin.progress().title(missing, lang)));
                continue;
            }
            TextComponent line = new TextComponent("");
            line.addExtra(Text.tr(lang, "justquests.list.entry", q.id(), q.title().get(lang)));
            if (q.repeatable()) line.addExtra(Text.tr(lang, "justquests.list.repeatable"));
            if (viewer != null) {
                BaseComponent tag = plugin.generated().tag(q.id(), viewer.getUniqueId(), lang);
                if (tag != null) line.addExtra(tag);
            }
            if (data != null && data.isClaimable(q.id())) {
                line.addExtra(" ");
                line.addExtra(plugin.progress().claimButton(lang, q.id()));
            } else if (viewer != null && QuestStatus.of(plugin, viewer.getUniqueId(), data, q) == QuestStatus.AVAILABLE) {
                line.addExtra(" ");
                line.addExtra(Text.button(Text.tr(lang, "justquests.plugin.accept_button"), "/quest accept " + q.id(),
                    Text.tr(lang, "justquests.plugin.gui.click_accept")));
            }
            say(src, line);
            String desc = q.description().get(lang);
            if (!desc.isBlank()) say(src, Text.tr(lang, "justquests.list.description", desc));
            BaseComponent goals = Text.tr(lang, "justquests.list.goal");
            for (int i = 0; i < q.objectives().size(); i++) {
                if (i > 0) goals.addExtra(Text.lit("§7, §f"));
                goals.addExtra(q.objectives().get(i).display(lang));
            }
            say(src, goals);
            BaseComponent rewards = Text.tr(lang, "justquests.list.reward");
            for (int i = 0; i < q.rewards().size(); i++) {
                if (i > 0) rewards.addExtra(Text.lit("§7, §a"));
                rewards.addExtra(q.rewards().get(i).display(lang));
            }
            say(src, rewards);
        }
    }

    private void categories(CommandSender src) {
        String lang = Lang.of(src);
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Quest q : quests(src)) counts.merge(q.category(), 1, Integer::sum);
        if (counts.isEmpty()) {
            say(src, "justquests.list.none");
            return;
        }
        say(src, "justquests.categories.header");
        counts.forEach((cat, n) -> say(src, Text.tr(lang, "justquests.categories.entry", plugin.categoryName(lang, cat), n, cat)));
    }

    private void stats(Player player) {
        String lang = Lang.of(player);
        PlayerData data = plugin.store().peek(player.getUniqueId());
        Collection<Quest> visible = plugin.visibleQuests(player);
        int total = visible.size();
        int completed = MainMenu.completedOf(data, visible);
        int active = data == null ? 0 : data.active.size();
        int pct = total > 0 ? Math.min(100, completed * 100 / total) : 0;
        say(player, "justquests.stats.header");
        say(player, "justquests.stats.completed", completed, total, pct);
        say(player, "justquests.stats.active", active);
        if (data != null && !data.completed.isEmpty()) {
            Map<String, Integer> byCat = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String id : data.completed.keySet()) {
                Quest q = plugin.quests().get(id);
                if (q != null) byCat.merge(q.category(), 1, Integer::sum);
            }
            if (!byCat.isEmpty()) {
                say(player, "justquests.stats.by_category");
                byCat.forEach((c, n) -> say(player, Text.tr(lang, "justquests.stats.category", plugin.categoryName(lang, c), n)));
            }
            List<Long> times = data.completed.values().stream().filter(t -> t > 0L).sorted().toList();
            if (!times.isEmpty()) {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
                say(player, "justquests.stats.dates", fmt.format(new Date(times.get(0))), fmt.format(new Date(times.get(times.size() - 1))));
            }
        }
    }

    private void leaderboard(CommandSender src) {
        List<Map.Entry<UUID, PlayerData>> top = plugin.store().all().entrySet().stream()
            .filter(e -> !e.getValue().completed.isEmpty())
            .sorted((a, b) -> Integer.compare(b.getValue().completed.size(), a.getValue().completed.size()))
            .limit(10).toList();
        if (top.isEmpty()) {
            say(src, "justquests.leaderboard.none");
            return;
        }
        say(src, "justquests.leaderboard.header", top.size());
        int rank = 0;
        for (Map.Entry<UUID, PlayerData> e : top) {
            say(src, "justquests.leaderboard.entry", ++rank, name(e.getKey()), e.getValue().completed.size());
        }
    }

    private static String name(UUID id) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(id);
        return p.getName() != null ? p.getName() : id.toString().substring(0, 8);
    }

    private void progress(Player player) {
        String lang = Lang.of(player);
        PlayerData data = plugin.store().peek(player.getUniqueId());
        rewardsReady(player, data, lang);
        if (data == null || data.active.isEmpty()) {
            say(player, "justquests.progress.none");
            return;
        }
        say(player, "justquests.progress.header");
        for (String id : data.active.keySet()) {
            Quest quest = plugin.quests().get(id);
            if (quest == null) continue;
            say(player, Text.tr(lang, "justquests.list.entry", id, quest.title().get(lang)));
            for (int i = 0; i < quest.objectives().size(); i++) {
                Objective obj = quest.objectives().get(i);
                int have = Math.min(data.progress(id, i), obj.requiredCount());
                TextComponent line = new TextComponent("");
                line.addExtra(Text.lit("  " + (have >= obj.requiredCount() ? "§a✓" : "§7" + have + "/" + obj.requiredCount()) + " §f"));
                line.addExtra(obj.display(lang));
                say(player, line);
            }
        }
    }

    private void rewardsReady(Player player, PlayerData data, String lang) {
        if (data == null) return;
        for (String id : data.pendingClaim.keySet()) {
            Quest quest = plugin.quests().get(id);
            if (quest == null) continue;
            TextComponent line = new TextComponent("");
            line.addExtra(Text.tr(lang, "justquests.rewards.ready", quest.title().get(lang)));
            line.addExtra(" ");
            line.addExtra(plugin.progress().claimButton(lang, id));
            say(player, line);
        }
    }

    private void claim(Player player, String id, int pick) {
        ProgressService.Result r = plugin.progress().claimOne(player, id, pick);
        if (r.message() != null) {
            say(player, r.message());
            return;
        }
        Quest quest = plugin.quests().get(id);
        showChoice(player, quest, Reward.Choice.of(quest));
    }

    private void claimAll(Player player) {
        String lang = Lang.of(player);
        PlayerData data = plugin.store().peek(player.getUniqueId());
        int n = 0;
        String last = "";
        List<Quest> choose = new ArrayList<>();
        if (data != null) {
            for (String id : new ArrayList<>(data.pendingClaim.keySet())) {
                Quest quest = plugin.quests().get(id);
                if (quest != null && Reward.Choice.of(quest) != null) {
                    choose.add(quest);
                } else if (plugin.progress().claim(player, data, id, -1) && quest != null) {
                    n++;
                    last = quest.title().get(lang);
                }
            }
            plugin.tracker().update(player);
        }
        if (n > 0) say(player, n == 1 ? Text.tr(lang, "justquests.rewards.claimed", last) : Text.tr(lang, "justquests.rewards.claimed_all", n));
        for (Quest q : choose) showChoice(player, q, Reward.Choice.of(q));
        if (n == 0 && choose.isEmpty()) say(player, "justquests.rewards.none");
    }

    /** "Choose your reward for X:" and one clickable line per option. */
    private void showChoice(Player player, Quest quest, Reward.Choice choice) {
        String lang = Lang.of(player);
        say(player, Text.tr(lang, "justquests.rewards.pick", quest.title().get(lang)));
        for (int i = 0; i < choice.options().size(); i++) {
            say(player, Text.button(Text.tr(lang, "justquests.rewards.option", i + 1, choice.options().get(i).display(lang)),
                "/quest claim " + quest.id() + " " + (i + 1), Text.tr(lang, "justquests.rewards.option_hover")));
        }
    }

    private void track(Player player, String id) {
        String lang = Lang.of(player);
        PlayerData data = plugin.store().get(player.getUniqueId());
        if (id == null) {
            String shown = plugin.tracker().shown(data);
            if (shown != null) say(player, Text.tr(lang, "justquests.plugin.track.ok", plugin.progress().title(shown, lang)));
            else say(player, "justquests.progress.none");
            return;
        }
        if (!data.isActive(id)) {
            say(player, "justquests.plugin.track.not_active");
            return;
        }
        data.pinned = id;
        data.bossbar = true;
        plugin.store().markDirty(player.getUniqueId());
        plugin.tracker().update(player);
        say(player, Text.tr(lang, "justquests.plugin.track.ok", plugin.progress().title(id, lang)));
    }

    // --- operator subcommands -----------------------------------------------------------------

    private void generator(CommandSender src, String label, String[] rest) {
        String sub = rest.length == 0 ? "status" : rest[0].toLowerCase(Locale.ROOT);
        String text = switch (sub) {
            case "status" -> plugin.generator().status();
            case "stats" -> plugin.generator().stats();
            case "preview" -> plugin.generator().preview(rest.length > 1 ? Math.max(1, Math.min(20, parseInt(rest[1]))) : 5);
            case "explain" -> rest.length > 1 ? plugin.generator().explain(id(rest[1])) : null;
            case "release" -> rest.length > 1 ? plugin.generator().release(id(rest[1])) : null;
            default -> null;
        };
        if (text == null) {
            say(src, "justquests.plugin.usage", "/" + label + " generator status|stats|preview [n]|explain <id>|release <id>");
            return;
        }
        for (String line : text.split("\n")) say(src, Text.lit(line.startsWith("§") ? line : "§7" + line));
    }

    private void mainQuests(CommandSender src, String[] rest) {
        String lang = Lang.of(src);
        if (rest.length == 0) {
            say(src, Text.tr(lang, "justquests.mainquests.status", Text.tr(lang, plugin.settings().mainQuests ? "justquests.on" : "justquests.off")));
            return;
        }
        boolean on = rest[0].equalsIgnoreCase("on");
        plugin.setMainQuests(on);
        say(src, on ? "justquests.mainquests.enabled" : "justquests.mainquests.disabled");
    }

    private void admin(CommandSender src, String label, String[] rest) {
        if (rest.length < 2) {
            say(src, "justquests.plugin.usage", "/" + label + " admin view|reset|complete <player> [quest]");
            return;
        }
        Player target = Bukkit.getPlayerExact(rest[1]);
        if (target == null) {
            say(src, "justquests.plugin.unknown_player", rest[1]);
            return;
        }
        String name = target.getName();
        UUID uuid = target.getUniqueId();
        switch (rest[0].toLowerCase(Locale.ROOT)) {
            case "view" -> {
                PlayerData data = plugin.store().peek(uuid);
                if (data == null || (data.active.isEmpty() && data.completed.isEmpty())) {
                    say(src, "justquests.admin.view_none", name);
                    return;
                }
                say(src, "justquests.admin.view_header", name, data.active.size(), data.completed.size());
                data.active.keySet().forEach(id -> say(src, "justquests.admin.view_active", id));
                data.completed.keySet().forEach(id -> say(src, "justquests.admin.view_done", id));
            }
            case "reset" -> {
                PlayerData data = plugin.store().peek(uuid);
                if (rest.length > 2) {
                    String id = id(rest[2]);
                    if (data != null) {
                        if (data.isActive(id)) plugin.generated().abandoned(id, uuid);
                        data.active.remove(id);
                        data.completed.remove(id);
                        if (data.pendingClaim.remove(id) != null) plugin.generated().rewardsClaimed(id, uuid);
                        if (id.equals(data.pinned)) data.pinned = null;
                        plugin.store().markDirty(uuid);
                    }
                    say(src, "justquests.admin.reset_one", id, name);
                } else {
                    if (data != null) {
                        plugin.generated().releaseAll(uuid);
                        data.active.clear();
                        data.completed.clear();
                        data.pendingClaim.clear();
                        data.pinned = null;
                        plugin.store().markDirty(uuid);
                    }
                    say(src, "justquests.admin.reset_all", name);
                }
                plugin.tracker().update(target);
            }
            case "complete" -> {
                if (rest.length < 3) {
                    say(src, "justquests.plugin.usage", "/" + label + " admin complete <player> <quest>");
                    return;
                }
                String id = id(rest[2]);
                Quest quest = plugin.quests().get(id);
                if (quest == null) {
                    say(src, "justquests.error.unknown_quest", id);
                    return;
                }
                plugin.progress().finish(target, plugin.store().get(uuid), id, quest);
                plugin.store().markDirty(uuid);
                plugin.tracker().update(target);
                say(src, "justquests.admin.complete", id, name);
            }
            default -> say(src, "justquests.plugin.usage", "/" + label + " admin view|reset|complete <player> [quest]");
        }
    }

    // --- tab completion -----------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : PLAYER) if (plugin.allowed(sender, "justquests.command." + s, false)) out.add(s);
            for (String s : ADMIN) if (plugin.allowed(sender, "justquests.admin." + s, true)) out.add(s);
        } else {
            String sub = args[0].toLowerCase(Locale.ROOT);
            Player p = sender instanceof Player pl ? pl : null;
            PlayerData data = p == null ? null : plugin.store().peek(p.getUniqueId());
            if (args.length == 2) {
                switch (sub) {
                    case "accept" -> {
                        for (Quest q : quests(sender)) {
                            if (p == null || QuestStatus.of(plugin, p.getUniqueId(), data, q) == QuestStatus.AVAILABLE) out.add(q.id());
                        }
                    }
                    case "abandon", "track" -> { if (data != null) out.addAll(data.active.keySet()); }
                    case "claim" -> { if (data != null) out.addAll(data.pendingClaim.keySet()); }
                    case "list" -> quests(sender).stream().map(Quest::category).distinct().sorted().forEach(out::add);
                    case "mainquests", "bossbar" -> out.addAll(List.of("on", "off"));
                    case "difficulty" -> out.addAll(List.of("easy", "normal", "hard"));
                    case "generator" -> out.addAll(List.of("status", "stats", "preview", "explain", "release"));
                    case "admin" -> { if (plugin.allowed(sender, "justquests.admin.admin", true)) out.addAll(List.of("view", "reset", "complete")); }
                    default -> { }
                }
            } else if (sub.equals("generator") && args.length == 3 && List.of("explain", "release").contains(args[1].toLowerCase(Locale.ROOT))) {
                out.addAll(plugin.generator().boardIds());
            } else if (sub.equals("admin") && args.length == 3) {
                for (Player online : Bukkit.getOnlinePlayers()) out.add(online.getName());
            } else if (sub.equals("admin") && args.length == 4) {
                out.addAll(plugin.quests().all().keySet());
            }
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String s : out) {
            if (s.toLowerCase(Locale.ROOT).startsWith(last) || s.toLowerCase(Locale.ROOT).startsWith("justquests:" + last)) filtered.add(s);
        }
        filtered.sort(null);
        return filtered;
    }
}
