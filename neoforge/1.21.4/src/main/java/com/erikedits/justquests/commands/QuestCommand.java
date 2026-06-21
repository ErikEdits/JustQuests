package com.erikedits.justquests.commands;

import com.erikedits.justquests.data.LocalizedText;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.data.reward.QuestReward;
import com.erikedits.justquests.diagnostics.SelfTest;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class QuestCommand {
    private static final SuggestionProvider<CommandSourceStack> AVAILABLE_QUESTS = (ctx, builder) ->
        SharedSuggestionProvider.suggestResource(QuestManager.INSTANCE.getQuests().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> ACTIVE_QUESTS = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        WorldQuestStore store = WorldQuestStore.get();
        if (player != null && store != null) {
            PlayerQuestData data = store.peek(player.getUUID());
            if (data != null) {
                return SharedSuggestionProvider.suggestResource(data.active.keySet(), builder);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> CATEGORIES = (ctx, builder) -> {
        var cats = QuestManager.INSTANCE.getQuests().values().stream()
            .map(Quest::category).distinct().sorted().toList();
        return SharedSuggestionProvider.suggest(cats, builder);
    };

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quest")
            .then(Commands.literal("list").executes(ctx -> list(ctx, null))
                .then(Commands.argument("category", StringArgumentType.word())
                    .suggests(CATEGORIES)
                    .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "category")))))
            .then(Commands.literal("categories").executes(QuestCommand::categories))
            .then(Commands.literal("stats").executes(QuestCommand::stats))
            .then(Commands.literal("leaderboard").executes(QuestCommand::leaderboard))
            .then(Commands.literal("progress").executes(QuestCommand::progress))
            .then(Commands.literal("accept")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(AVAILABLE_QUESTS)
                    .executes(ctx -> accept(ctx, ResourceLocationArgument.getId(ctx, "id")))))
            .then(Commands.literal("abandon")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(ACTIVE_QUESTS)
                    .executes(ctx -> abandon(ctx, ResourceLocationArgument.getId(ctx, "id")))))
            .then(Commands.literal("discord").executes(QuestCommand::discord))
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(2))
                .executes(QuestCommand::reload))
            .then(Commands.literal("test")
                .requires(src -> src.hasPermission(2))
                .executes(QuestCommand::test))
            .then(Commands.literal("admin")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("view")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(QuestCommand::adminView)))
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(QuestCommand::adminResetAll)
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                            .suggests(AVAILABLE_QUESTS)
                            .executes(ctx -> adminResetOne(ctx, ResourceLocationArgument.getId(ctx, "id"))))))
                .then(Commands.literal("complete")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                            .suggests(AVAILABLE_QUESTS)
                            .executes(ctx -> adminComplete(ctx, ResourceLocationArgument.getId(ctx, "id"))))))));
    }

    /** The client language of the command source, or English for the console. */
    private static String lang(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        return player != null ? player.clientInformation().language() : LocalizedText.DEFAULT_LANG;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, String category) {
        CommandSourceStack src = ctx.getSource();
        String lang = lang(src);
        // the caller's progress, so locked (prerequisite) quests can be teased
        PlayerQuestData self = null;
        ServerPlayer viewer = src.getPlayer();
        if (viewer != null) {
            WorldQuestStore store = WorldQuestStore.get();
            if (store != null) self = store.peek(viewer.getUUID());
        }
        final PlayerQuestData data = self;

        Map<ResourceLocation, Quest> quests = QuestManager.INSTANCE.getQuests();
        // sorted by category, then per-quest sort weight, then id (stable order)
        List<Map.Entry<ResourceLocation, Quest>> entries = quests.entrySet().stream()
            .filter(e -> category == null || e.getValue().category().equalsIgnoreCase(category))
            .sorted(Comparator
                .comparing((Map.Entry<ResourceLocation, Quest> e) -> e.getValue().category(), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(e -> e.getValue().sort())
                .thenComparing(e -> e.getKey().toString()))
            .toList();

        if (entries.isEmpty()) {
            String msg = category == null ? "§7No quests defined."
                : "§7No quests in category '" + category + "'. Try /quest categories.";
            src.sendSuccess(() -> Component.literal(msg), false);
            return 0;
        }

        src.sendSuccess(() -> Component.literal(category == null
            ? "§eAvailable quests:" : "§eQuests in §f" + category + "§e:"), false);

        String shownCategory = null;
        for (Map.Entry<ResourceLocation, Quest> entry : entries) {
            ResourceLocation id = entry.getKey();
            Quest quest = entry.getValue();

            // category header (only when listing everything, not when filtered)
            if (category == null && !quest.category().equals(shownCategory)) {
                shownCategory = quest.category();
                final String cat = shownCategory;
                src.sendSuccess(() -> Component.literal("§6§l" + cat), false);
            }

            // locked teaser: a prerequisite isn't completed yet (Q28)
            ResourceLocation missing = null;
            if (data != null) {
                for (ResourceLocation req : quest.requires()) {
                    if (!data.isCompleted(req)) { missing = req; break; }
                }
            }
            if (missing != null) {
                Quest reqQuest = QuestManager.INSTANCE.get(missing);
                String reqName = reqQuest != null ? reqQuest.title().get(lang) : missing.toString();
                src.sendSuccess(() -> Component.literal("§8" + id + " §7— §8" + quest.title().get(lang)
                    + " §c[locked: needs " + reqName + "]"), false);
                continue; // teaser only — hide goal and reward
            }

            String repeatTag = quest.repeatable() ? " §d(repeatable)" : "";
            src.sendSuccess(() -> Component.literal("§b" + id + " §7— §f" + quest.title().get(lang)
                + repeatTag), false);
            String desc = quest.description().get(lang);
            if (!desc.isBlank()) {
                src.sendSuccess(() -> Component.literal("  §7§o" + desc), false);
            }

            MutableComponent goals = Component.literal("  §6Goal: §f");
            List<QuestObjective> objs = quest.objectives();
            for (int i = 0; i < objs.size(); i++) {
                if (i > 0) goals.append(Component.literal("§7, §f"));
                goals.append(objs.get(i).display());
            }
            src.sendSuccess(() -> goals, false);

            MutableComponent rewards = Component.literal("  §6Reward: §a");
            List<QuestReward> rs = quest.rewards();
            for (int i = 0; i < rs.size(); i++) {
                if (i > 0) rewards.append(Component.literal("§7, §a"));
                rewards.append(rs.get(i).display());
            }
            src.sendSuccess(() -> rewards, false);
        }
        return entries.size();
    }

    private static int categories(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Map<ResourceLocation, Quest> quests = QuestManager.INSTANCE.getQuests();
        if (quests.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7No quests defined."), false);
            return 0;
        }
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Quest q : quests.values()) {
            counts.merge(q.category(), 1, Integer::sum);
        }
        src.sendSuccess(() -> Component.literal("§eCategories:"), false);
        counts.forEach((cat, n) ->
            src.sendSuccess(() -> Component.literal("§6" + cat + " §7(" + n + ") §8— /quest list " + cat), false));
        return counts.size();
    }

    private static int stats(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CommandSourceStack src = ctx.getSource();
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());

        int total = QuestManager.INSTANCE.getQuests().size();
        int completed = data == null ? 0 : data.completed.size();
        int active = data == null ? 0 : data.active.size();
        // cap at 100: a player may have completed quests that are no longer loaded
        int pct = total > 0 ? Math.min(100, completed * 100 / total) : 0;

        src.sendSuccess(() -> Component.literal("§eYour quest stats:"), false);
        src.sendSuccess(() -> Component.literal("  §7Completed: §f" + completed + "§7/§f" + total + " §7(" + pct + "%)"), false);
        src.sendSuccess(() -> Component.literal("  §7Active: §f" + active), false);

        if (data != null && !data.completed.isEmpty()) {
            Map<String, Integer> byCat = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (ResourceLocation id : data.completed.keySet()) {
                Quest q = QuestManager.INSTANCE.get(id);
                if (q != null) byCat.merge(q.category(), 1, Integer::sum);
            }
            if (!byCat.isEmpty()) {
                src.sendSuccess(() -> Component.literal("  §7By category:"), false);
                byCat.forEach((c, n) -> src.sendSuccess(() -> Component.literal("    §6" + c + "§7: §f" + n), false));
            }
            List<Long> times = data.completed.values().stream().filter(t -> t > 0L).sorted().toList();
            if (!times.isEmpty()) {
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String first = fmt.format(new java.util.Date(times.get(0)));
                String last = fmt.format(new java.util.Date(times.get(times.size() - 1)));
                src.sendSuccess(() -> Component.literal("  §7First: §f" + first + " §7Last: §f" + last), false);
            }
        }
        return 1;
    }

    private static int leaderboard(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) {
            src.sendSuccess(() -> Component.literal("§7No quest data yet."), false);
            return 0;
        }
        MinecraftServer server = src.getServer();
        List<Map.Entry<UUID, PlayerQuestData>> top = store.allPlayers().entrySet().stream()
            .filter(e -> !e.getValue().completed.isEmpty())
            .sorted((a, b) -> Integer.compare(b.getValue().completed.size(), a.getValue().completed.size()))
            .limit(10)
            .toList();
        if (top.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7No completed quests yet."), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("§eQuest leaderboard (top " + top.size() + "):"), false);
        int rank = 0;
        for (Map.Entry<UUID, PlayerQuestData> e : top) {
            final int r = ++rank;
            final String name = nameFor(server, e.getKey());
            final int n = e.getValue().completed.size();
            src.sendSuccess(() -> Component.literal("  §6#" + r + " §f" + name + " §7— §f" + n + " §7done"), false);
        }
        return top.size();
    }

    /** Resolves a player's name from the profile cache / online list, else a short UUID. */
    private static String nameFor(MinecraftServer server, UUID id) {
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) return online.getGameProfile().getName();
            if (server.getProfileCache() != null) {
                var prof = server.getProfileCache().get(id);
                if (prof.isPresent()) return prof.get().getName();
            }
        }
        return id.toString().substring(0, 8);
    }

    private static int progress(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String lang = player.clientInformation().language();
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());

        if (data == null || data.active.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7You have no active quests."), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§eActive quests:"), false);

        for (Map.Entry<ResourceLocation, QuestProgress> entry : data.active.entrySet()) {
            Quest quest = QuestManager.INSTANCE.get(entry.getKey());
            if (quest == null) continue;

            ctx.getSource().sendSuccess(() ->
                Component.literal("§b" + entry.getKey() + " §7— §f" + quest.title().get(lang)), false);

            QuestProgress prog = entry.getValue();
            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                int current = Math.min(prog.get(i), obj.requiredCount());
                int needed = obj.requiredCount();
                String bar = current >= needed ? "§a✓" : "§7" + current + "/" + needed;
                final int idx = i;
                ctx.getSource().sendSuccess(() ->
                    Component.literal("  " + bar + " §f").append(quest.objectives().get(idx).display()), false);
            }
        }
        return data.active.size();
    }

    private static int accept(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        Quest quest = QuestManager.INSTANCE.get(id);
        if (quest == null) {
            ctx.getSource().sendFailure(Component.literal("§cUnknown quest: " + id));
            return 0;
        }

        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) {
            ctx.getSource().sendFailure(Component.literal("§cQuest storage is not ready yet."));
            return 0;
        }
        PlayerQuestData data = store.get(player.getUUID());
        String lang = player.clientInformation().language();

        if (data.isActive(id)) {
            ctx.getSource().sendFailure(Component.literal("§cQuest is already active."));
            return 0;
        }

        // prerequisites must be completed first (Q28)
        for (ResourceLocation req : quest.requires()) {
            if (!data.isCompleted(req)) {
                Quest reqQuest = QuestManager.INSTANCE.get(req);
                String reqName = reqQuest != null ? reqQuest.title().get(lang) : req.toString();
                ctx.getSource().sendFailure(Component.literal("§cLocked — finish first: §f" + reqName));
                return 0;
            }
        }

        // already completed: only re-acceptable if repeatable and off cooldown (Q26)
        if (data.isCompleted(id)) {
            if (!quest.repeatable()) {
                ctx.getSource().sendFailure(Component.literal("§cYou already completed this quest."));
                return 0;
            }
            long cooldownMs = quest.cooldownHours().orElse(0) * 3600_000L;
            if (cooldownMs > 0) {
                long remaining = cooldownMs - (System.currentTimeMillis() - data.completed.getOrDefault(id, 0L));
                if (remaining > 0) {
                    ctx.getSource().sendFailure(Component.literal(
                        "§cOn cooldown — " + formatDuration(remaining) + " left."));
                    return 0;
                }
            }
        }

        data.accept(id);
        store.markDirty();
        ctx.getSource().sendSuccess(() ->
            Component.literal("§a✓ Accepted: " + quest.title().get(lang)), false);
        return 1;
    }

    /** Human-readable remaining time, e.g. "2h 5m" or "3m". */
    private static String formatDuration(long ms) {
        long totalMin = ms / 60000L;
        long h = totalMin / 60;
        long m = totalMin % 60;
        return h > 0 ? h + "h " + m + "m" : Math.max(1, m) + "m";
    }

    private static int abandon(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        if (data == null || !data.isActive(id)) {
            ctx.getSource().sendFailure(Component.literal("§cQuest is not active."));
            return 0;
        }

        data.abandon(id);
        store.markDirty();
        ctx.getSource().sendSuccess(() ->
            Component.literal("§7Abandoned quest: " + id), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        com.erikedits.justquests.storage.CustomQuestLoader.load();
        int count = QuestManager.INSTANCE.getQuests().size();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "§aReloaded custom quests. §7Total quests: " + count
            + " §8(use /reload for datapack quests)"), false);
        return 1;
    }

    private static int test(CommandContext<CommandSourceStack> ctx) {
        String summary = SelfTest.run(ctx.getSource());
        ctx.getSource().sendSuccess(() -> Component.literal(summary), false);
        return 1;
    }

    private static int discord(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(
            com.erikedits.justquests.community.CommunityHints::discordMessage, false);
        return 1;
    }

    // --- admin (OP, permission level 2) ---------------------------------

    private static int adminView(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        CommandSourceStack src = ctx.getSource();
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(target.getUUID());
        String name = target.getGameProfile().getName();
        if (data == null || (data.active.isEmpty() && data.completed.isEmpty())) {
            src.sendSuccess(() -> Component.literal("§7" + name + " has no quest progress."), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("§e" + name + " §7— active: §f" + data.active.size()
            + " §7completed: §f" + data.completed.size()), false);
        data.active.keySet().forEach(id -> src.sendSuccess(() -> Component.literal("  §b" + id + " §7(active)"), false));
        data.completed.keySet().forEach(id -> src.sendSuccess(() -> Component.literal("  §a" + id + " §7(done)"), false));
        return 1;
    }

    private static int adminResetAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String name = target.getGameProfile().getName();
        WorldQuestStore store = WorldQuestStore.get();
        if (store != null && store.has(target.getUUID())) {
            PlayerQuestData data = store.get(target.getUUID());
            data.active.clear();
            data.completed.clear();
            data.pendingClaim.clear();
            store.markDirty();
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§aReset all quest progress for " + name + "."), true);
        return 1;
    }

    private static int adminResetOne(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String name = target.getGameProfile().getName();
        WorldQuestStore store = WorldQuestStore.get();
        if (store != null) {
            PlayerQuestData data = store.peek(target.getUUID());
            if (data != null) {
                data.active.remove(id);
                data.completed.remove(id);
                store.markDirty();
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§aReset " + id + " for " + name + "."), true);
        return 1;
    }

    private static int adminComplete(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        CommandSourceStack src = ctx.getSource();
        Quest quest = QuestManager.INSTANCE.get(id);
        if (quest == null) {
            src.sendFailure(Component.literal("§cUnknown quest: " + id));
            return 0;
        }
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) {
            src.sendFailure(Component.literal("§cQuest storage is not ready yet."));
            return 0;
        }
        PlayerQuestData data = store.get(target.getUUID());
        data.complete(id);
        for (QuestReward reward : quest.rewards()) {
            reward.grant(target);
        }
        store.markDirty();
        String name = target.getGameProfile().getName();
        src.sendSuccess(() -> Component.literal("§aForce-completed " + id + " for " + name + " (rewards granted)."), true);
        return 1;
    }
}
