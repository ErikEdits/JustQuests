package com.erikedits.justquests.commands;

import com.erikedits.justquests.text.Msg;
import com.erikedits.justquests.data.LocalizedText;
import com.erikedits.justquests.data.PlayerQuestData;
import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.perm.Perms;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class QuestCommand {
    private static final SuggestionProvider<CommandSourceStack> AVAILABLE_QUESTS = (ctx, builder) ->
        SharedSuggestionProvider.suggestResource(QuestManager.INSTANCE.getQuests().entrySet().stream()
            .filter(e -> !(ctx.getSource().getEntity() instanceof ServerPlayer p)
                || Perms.quest(p, e.getValue()))
            .map(Map.Entry::getKey), builder);

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

    private static final SuggestionProvider<CommandSourceStack> CLAIMABLE_QUESTS = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        WorldQuestStore store = WorldQuestStore.get();
        if (player != null && store != null) {
            PlayerQuestData data = store.peek(player.getUUID());
            if (data != null) {
                return SharedSuggestionProvider.suggestResource(data.pendingClaim.keySet(), builder);
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> CATEGORIES = (ctx, builder) -> {
        var cats = QuestManager.INSTANCE.getQuests().values().stream()
            .map(Quest::category).distinct().sorted().toList();
        return SharedSuggestionProvider.suggest(cats, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quest")
            .then(Commands.literal("list").requires(Perms.command("list")).executes(ctx -> list(ctx, null))
                .then(Commands.argument("category", StringArgumentType.word())
                    .suggests(CATEGORIES)
                    .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "category")))))
            .then(Commands.literal("categories").requires(Perms.command("categories")).executes(QuestCommand::categories))
            .then(Commands.literal("stats").requires(Perms.command("stats")).executes(QuestCommand::stats))
            .then(Commands.literal("leaderboard").requires(Perms.command("leaderboard")).executes(QuestCommand::leaderboard))
            .then(Commands.literal("progress").requires(Perms.command("progress")).executes(QuestCommand::progress))
            .then(Commands.literal("accept").requires(Perms.command("accept"))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(AVAILABLE_QUESTS)
                    .executes(ctx -> accept(ctx, ResourceLocationArgument.getId(ctx, "id")))))
            .then(Commands.literal("abandon").requires(Perms.command("abandon"))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(ACTIVE_QUESTS)
                    .executes(ctx -> abandon(ctx, ResourceLocationArgument.getId(ctx, "id")))))
            .then(Commands.literal("claim").requires(Perms.command("claim"))
                .executes(QuestCommand::claimAll)
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(CLAIMABLE_QUESTS)
                    .executes(ctx -> claim(ctx, ResourceLocationArgument.getId(ctx, "id"), 0))
                    .then(Commands.argument("reward", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> claim(ctx, ResourceLocationArgument.getId(ctx, "id"),
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "reward"))))))
            .then(Commands.literal("discord").requires(Perms.command("discord")).executes(QuestCommand::discord))
            .then(Commands.literal("reload")
                .requires(Perms.admin("reload"))
                .executes(QuestCommand::reload))
            .then(Commands.literal("reroll")
                .requires(Perms.admin("reroll"))
                .executes(QuestCommand::reroll))
            .then(Commands.literal("mainquests")
                .requires(Perms.admin("mainquests"))
                .executes(QuestCommand::mainQuestsStatus)
                .then(Commands.literal("on").executes(ctx -> setMainQuests(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> setMainQuests(ctx, false))))
            .then(Commands.literal("difficulty")
                .requires(Perms.admin("difficulty"))
                .executes(QuestCommand::difficultyShow)
                .then(Commands.literal("easy").executes(ctx -> difficultySet(ctx, "easy")))
                .then(Commands.literal("normal").executes(ctx -> difficultySet(ctx, "normal")))
                .then(Commands.literal("hard").executes(ctx -> difficultySet(ctx, "hard"))))
            .then(Commands.literal("generator")
                .requires(Perms.admin("generator"))
                .then(Commands.literal("status").executes(ctx -> generatorLines(ctx, com.erikedits.justquests.generator.GenV2.status())))
                .then(Commands.literal("stats").executes(ctx -> generatorLines(ctx, com.erikedits.justquests.generator.GenV2.statsText())))
                .then(Commands.literal("preview")
                    .executes(ctx -> generatorLines(ctx, com.erikedits.justquests.generator.GenV2.preview(5)))
                    .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20))
                        .executes(ctx -> generatorLines(ctx, com.erikedits.justquests.generator.GenV2.preview(
                            com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("explain")
                    .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(com.erikedits.justquests.generator.GenV2.servedIds(), b))
                        .executes(ctx -> generatorLines(ctx, com.erikedits.justquests.generator.GenV2.explain(ResourceLocationArgument.getId(ctx, "id"))))))
                .then(Commands.literal("release")
                    .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(com.erikedits.justquests.generator.GenV2.servedIds(), b))
                        .executes(ctx -> generatorLines(ctx, com.erikedits.justquests.generator.GenV2.forceRelease(ResourceLocationArgument.getId(ctx, "id")))))))
            .then(Commands.literal("test")
                .requires(Perms.admin("test"))
                .executes(QuestCommand::test))
            .then(Commands.literal("admin")
                .requires(Perms.admin("admin"))
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
            .filter(e -> viewer == null || Perms.quest(viewer, e.getValue()))
            .sorted(Comparator
                .comparing((Map.Entry<ResourceLocation, Quest> e) -> e.getValue().category(), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(e -> e.getValue().sort())
                .thenComparing(e -> e.getKey().toString()))
            .toList();

        if (entries.isEmpty()) {
            Component msg = category == null ? Msg.tr("justquests.list.none")
                : Msg.tr("justquests.list.none_category", category);
            src.sendSuccess(() -> msg, false);
            return 0;
        }

        src.sendSuccess(() -> (category == null
            ? Msg.tr("justquests.list.header") : Msg.tr("justquests.list.header_category", category)), false);

        String shownCategory = null;
        for (Map.Entry<ResourceLocation, Quest> entry : entries) {
            ResourceLocation id = entry.getKey();
            Quest quest = entry.getValue();

            // category header (only when listing everything, not when filtered)
            if (category == null && !quest.category().equals(shownCategory)) {
                shownCategory = quest.category();
                final String cat = shownCategory;
                src.sendSuccess(() -> Msg.tr("justquests.list.category", Msg.category(cat)), false);
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
                src.sendSuccess(() -> Msg.tr("justquests.list.locked", id, quest.title().get(lang), reqName), false);
                continue; // teaser only — hide goal and reward
            }

            Component repeatTag = quest.repeatable() ? Msg.tr("justquests.list.repeatable") : Msg.empty();
            Component claimTag = viewer == null ? Msg.empty() : com.erikedits.justquests.generator.GenV2.claimTag(id, viewer.getUUID());
            src.sendSuccess(() -> Msg.tr("justquests.list.entry", id, quest.title().get(lang)).append(repeatTag).append(claimTag).append(rewardTag(data, id)), false);
            String desc = quest.description().get(lang);
            if (!desc.isBlank()) {
                src.sendSuccess(() -> Msg.tr("justquests.list.description", desc), false);
            }

            MutableComponent goals = Msg.tr("justquests.list.goal");
            List<QuestObjective> objs = quest.objectives();
            for (int i = 0; i < objs.size(); i++) {
                if (i > 0) goals.append(Component.literal("§7, §f"));
                goals.append(objs.get(i).display());
            }
            src.sendSuccess(() -> goals, false);

            MutableComponent rewards = Msg.tr("justquests.list.reward");
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
            src.sendSuccess(() -> Msg.tr("justquests.list.none"), false);
            return 0;
        }
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Quest q : quests.values()) {
            counts.merge(q.category(), 1, Integer::sum);
        }
        src.sendSuccess(() -> Msg.tr("justquests.categories.header"), false);
        counts.forEach((cat, n) ->
            src.sendSuccess(() -> Msg.tr("justquests.categories.entry", Msg.category(cat), n, cat), false));
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

        src.sendSuccess(() -> Msg.tr("justquests.stats.header"), false);
        src.sendSuccess(() -> Msg.tr("justquests.stats.completed", completed, total, pct), false);
        src.sendSuccess(() -> Msg.tr("justquests.stats.active", active), false);

        if (data != null && !data.completed.isEmpty()) {
            Map<String, Integer> byCat = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (ResourceLocation id : data.completed.keySet()) {
                Quest q = QuestManager.INSTANCE.get(id);
                if (q != null) byCat.merge(q.category(), 1, Integer::sum);
            }
            if (!byCat.isEmpty()) {
                src.sendSuccess(() -> Msg.tr("justquests.stats.by_category"), false);
                byCat.forEach((c, n) -> src.sendSuccess(() -> Msg.tr("justquests.stats.category", Msg.category(c), n), false));
            }
            List<Long> times = data.completed.values().stream().filter(t -> t > 0L).sorted().toList();
            if (!times.isEmpty()) {
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String first = fmt.format(new java.util.Date(times.get(0)));
                String last = fmt.format(new java.util.Date(times.get(times.size() - 1)));
                src.sendSuccess(() -> Msg.tr("justquests.stats.dates", first, last), false);
            }
        }
        return 1;
    }

    private static int leaderboard(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) {
            src.sendSuccess(() -> Msg.tr("justquests.leaderboard.no_data"), false);
            return 0;
        }
        MinecraftServer server = src.getServer();
        List<Map.Entry<UUID, PlayerQuestData>> top = store.allPlayers().entrySet().stream()
            .filter(e -> !e.getValue().completed.isEmpty())
            .sorted((a, b) -> Integer.compare(b.getValue().completed.size(), a.getValue().completed.size()))
            .limit(10)
            .toList();
        if (top.isEmpty()) {
            src.sendSuccess(() -> Msg.tr("justquests.leaderboard.none"), false);
            return 0;
        }
        src.sendSuccess(() -> Msg.tr("justquests.leaderboard.header", top.size()), false);
        int rank = 0;
        for (Map.Entry<UUID, PlayerQuestData> e : top) {
            final int r = ++rank;
            final String name = nameFor(server, e.getKey());
            final int n = e.getValue().completed.size();
            src.sendSuccess(() -> Msg.tr("justquests.leaderboard.entry", r, name, n), false);
        }
        return top.size();
    }

    /** Resolves an online player's name, else a short UUID (cross-version safe). */
    private static String nameFor(MinecraftServer server, UUID id) {
        if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) return online.getName().getString();
        }
        return id.toString().substring(0, 8);
    }

    private static int progress(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String lang = player.clientInformation().language();
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        rewardsReady(ctx.getSource(), data, lang);

        if (data == null || data.active.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Msg.tr("justquests.progress.none"), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Msg.tr("justquests.progress.header"), false);

        for (Map.Entry<ResourceLocation, QuestProgress> entry : data.active.entrySet()) {
            Quest quest = QuestManager.INSTANCE.get(entry.getKey());
            if (quest == null) continue;

            ctx.getSource().sendSuccess(() ->
                Msg.tr("justquests.list.entry", entry.getKey(), quest.title().get(lang)), false);

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
            ctx.getSource().sendFailure(Msg.tr("justquests.error.unknown_quest", id));
            return 0;
        }
        if (!Perms.quest(player, quest)) {
            ctx.getSource().sendFailure(Msg.tr("justquests.accept.no_permission"));
            return 0;
        }

        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) {
            ctx.getSource().sendFailure(Msg.tr("justquests.error.storage"));
            return 0;
        }
        PlayerQuestData data = store.get(player.getUUID());
        String lang = player.clientInformation().language();

        if (data.isActive(id)) {
            ctx.getSource().sendFailure(Msg.tr("justquests.accept.already_active"));
            return 0;
        }
        if (data.isClaimable(id)) {
            ctx.getSource().sendFailure(Msg.tr("justquests.accept.claim_first", id));
            return 0;
        }

        // prerequisites must be completed first (Q28)
        for (ResourceLocation req : quest.requires()) {
            if (!data.isCompleted(req)) {
                Quest reqQuest = QuestManager.INSTANCE.get(req);
                String reqName = reqQuest != null ? reqQuest.title().get(lang) : req.toString();
                ctx.getSource().sendFailure(Msg.tr("justquests.accept.locked", reqName));
                return 0;
            }
        }

        // already completed: only re-acceptable if repeatable and off cooldown (Q26)
        if (data.isCompleted(id)) {
            if (!quest.repeatable()) {
                ctx.getSource().sendFailure(Msg.tr("justquests.accept.done"));
                return 0;
            }
            long cooldownMs = quest.cooldownHours().orElse(0) * 3600_000L;
            if (cooldownMs > 0) {
                long remaining = cooldownMs - (System.currentTimeMillis() - data.completed.getOrDefault(id, 0L));
                if (remaining > 0) {
                    ctx.getSource().sendFailure(Msg.tr("justquests.accept.cooldown", formatDuration(remaining)));
                    return 0;
                }
            }
        }

        Component denied = com.erikedits.justquests.generator.GenV2.claim(id, player.getUUID());
        if (denied != null) {
            ctx.getSource().sendFailure(denied);
            return 0;
        }
        data.accept(id);
        store.markDirty();
        com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
        ctx.getSource().sendSuccess(() ->
            Msg.tr("justquests.accept.ok", quest.title().get(lang)), false);
        return 1;
    }

    /** Human-readable remaining time, e.g. "2h 5m" or "3m". */
    private static Component formatDuration(long ms) {
        long totalMin = ms / 60000L;
        long h = totalMin / 60;
        long m = totalMin % 60;
        return h > 0 ? Msg.tr("justquests.time.hours_minutes", h, m) : Msg.tr("justquests.time.minutes", Math.max(1, m));
    }

    private static int abandon(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        if (data == null || !data.isActive(id)) {
            ctx.getSource().sendFailure(Msg.tr("justquests.abandon.not_active"));
            return 0;
        }

        data.abandon(id);
        store.markDirty();
        com.erikedits.justquests.generator.GenV2.abandoned(id, player.getUUID());
        com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
        ctx.getSource().sendSuccess(() ->
            Msg.tr("justquests.abandon.ok", id), false);
        return 1;
    }

    private static void genSay(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), false);
    }

    /** /quest claim <id> [n]: n picks the reward of a quest with a choice (1 = first option). */
    private static int claim(CommandContext<CommandSourceStack> ctx, ResourceLocation id, int pick) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        Quest quest = QuestManager.INSTANCE.get(id);
        if (data == null || quest == null || !data.isClaimable(id)) {
            ctx.getSource().sendFailure(Msg.tr("justquests.rewards.nothing"));
            return 0;
        }
        String lang = lang(ctx.getSource());
        com.erikedits.justquests.data.reward.ChoiceReward choice = com.erikedits.justquests.data.reward.ChoiceReward.of(quest);
        if (choice != null && (pick < 1 || pick > choice.options().size())) {
            showChoice(ctx.getSource(), id, quest.title().get(lang), choice);
            return 0;
        }
        com.erikedits.justquests.progress.QuestProgressService.claim(player, data, id, pick - 1);
        store.markDirty();
        com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
        say(ctx.getSource(), Msg.tr("justquests.rewards.claimed", quest.title().get(lang)));
        return 1;
    }

    /** /quest claim without an id: every waiting reward at once; quests with a choice ask for the pick. */
    private static int claimAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        WorldQuestStore store = WorldQuestStore.get();
        PlayerQuestData data = store == null ? null : store.peek(player.getUUID());
        String lang = lang(ctx.getSource());
        int n = 0;
        String last = "";
        java.util.List<ResourceLocation> choose = new java.util.ArrayList<>();
        if (data != null && !data.pendingClaim.isEmpty()) {
            for (ResourceLocation id : new java.util.ArrayList<>(data.pendingClaim.keySet())) {
                Quest quest = QuestManager.INSTANCE.get(id);
                if (quest != null && com.erikedits.justquests.data.reward.ChoiceReward.of(quest) != null) {
                    choose.add(id);
                } else if (com.erikedits.justquests.progress.QuestProgressService.claim(player, data, id)) {
                    n++;
                    last = quest.title().get(lang);
                }
            }
            store.markDirty();
            com.erikedits.justquests.network.QuestNetwork.syncProgress(player);
        }
        if (n > 0) {
            say(ctx.getSource(), n == 1 ? Msg.tr("justquests.rewards.claimed", last) : Msg.tr("justquests.rewards.claimed_all", n));
        }
        for (ResourceLocation id : choose) {
            Quest quest = QuestManager.INSTANCE.get(id);
            showChoice(ctx.getSource(), id, quest.title().get(lang), com.erikedits.justquests.data.reward.ChoiceReward.of(quest));
        }
        if (n == 0 && choose.isEmpty()) {
            ctx.getSource().sendFailure(Msg.tr("justquests.rewards.none"));
            return 0;
        }
        return n;
    }

    /** "Choose your reward for X:" and one clickable line per option. */
    private static void showChoice(CommandSourceStack src, ResourceLocation id, String title,
                                   com.erikedits.justquests.data.reward.ChoiceReward choice) {
        say(src, Msg.tr("justquests.rewards.pick", title));
        for (int i = 0; i < choice.options().size(); i++) {
            say(src, Msg.button(Msg.tr("justquests.rewards.option", i + 1, choice.options().get(i).display()),
                "/quest claim " + id + " " + (i + 1), Msg.tr("justquests.rewards.option_hover")));
        }
    }

    /** "Rewards ready: <quest> [Claim rewards]" for every finished quest whose rewards wait. */
    private static void rewardsReady(CommandSourceStack src, PlayerQuestData data, String lang) {
        if (data == null) return;
        for (ResourceLocation id : data.pendingClaim.keySet()) {
            Quest quest = QuestManager.INSTANCE.get(id);
            if (quest == null) continue;
            say(src, Msg.tr("justquests.rewards.ready", quest.title().get(lang)).append(Msg.lit(" "))
                .append(com.erikedits.justquests.progress.QuestProgressService.claimButton(id)));
        }
    }

    /** A clickable " [Claim rewards]" behind quests in /quest list whose rewards wait. */
    private static Component rewardTag(PlayerQuestData data, ResourceLocation id) {
        return data != null && data.isClaimable(id)
            ? Msg.lit(" ").append(com.erikedits.justquests.progress.QuestProgressService.claimButton(id)) : Msg.empty();
    }

    private static void say(CommandSourceStack src, Component msg) {
        src.sendSuccess(() -> msg, false);
    }

    private static void genFail(CommandSourceStack src, String msg) {
        src.sendFailure(Component.literal(msg));
    }

    private static int difficultyShow(CommandContext<CommandSourceStack> ctx) {
        say(ctx.getSource(), Msg.tr("justquests.difficulty.show", com.erikedits.justquests.storage.WorldSettings.difficulty()));
        return 1;
    }

    private static int difficultySet(CommandContext<CommandSourceStack> ctx, String value) {
        com.erikedits.justquests.storage.WorldSettings.setDifficulty(value);
        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
        if (server != null) com.erikedits.justquests.storage.WorldSettings.save(server);
        com.erikedits.justquests.generator.GenV2.reloadConfig();
        say(ctx.getSource(), Msg.tr("justquests.difficulty.set", value));
        return 1;
    }

    private static int generatorLines(CommandContext<CommandSourceStack> ctx, String text) {
        for (String line : text.split("\n")) genSay(ctx.getSource(), line.startsWith("§") ? line : "§7" + line);
        return 1;
    }

    private static int mainQuestsStatus(CommandContext<CommandSourceStack> ctx) {
        boolean on = com.erikedits.justquests.storage.WorldSettings.mainQuests();
        ctx.getSource().sendSuccess(() -> Msg.tr("justquests.mainquests.status", Msg.tr(on ? "justquests.on" : "justquests.off")), false);
        return 1;
    }

    private static int setMainQuests(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        com.erikedits.justquests.storage.WorldSettings.setMainQuests(enabled);
        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
        if (server != null) {
            com.erikedits.justquests.storage.WorldSettings.save(server);
            com.erikedits.justquests.network.QuestNetwork.syncAll(server);
        }
        ctx.getSource().sendSuccess(() -> (enabled ? Msg.tr("justquests.mainquests.enabled") : Msg.tr("justquests.mainquests.disabled")), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        com.erikedits.justquests.storage.WorldSettings.load(ctx.getSource().getServer());
        com.erikedits.justquests.generator.GenV2.reloadConfig();
        com.erikedits.justquests.storage.CustomQuestLoader.load();
        com.erikedits.justquests.network.QuestNetwork.syncAll(ctx.getSource().getServer());
        int count = QuestManager.INSTANCE.getQuests().size();
        ctx.getSource().sendSuccess(() -> Msg.tr("justquests.reload.done", count), false);
        return 1;
    }

    private static int reroll(CommandContext<CommandSourceStack> ctx) {
        int n = com.erikedits.justquests.generator.GenV2.reroll();
        if (n < 0) {
            ctx.getSource().sendSuccess(() -> Msg.tr("justquests.reroll.disabled"), false);
        } else {
            final int count = n;
            ctx.getSource().sendSuccess(() -> Msg.tr("justquests.reroll.done", count), false);
        }
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
        String name = target.getName().getString();
        if (data == null || (data.active.isEmpty() && data.completed.isEmpty())) {
            src.sendSuccess(() -> Msg.tr("justquests.admin.view_none", name), false);
            return 0;
        }
        src.sendSuccess(() -> Msg.tr("justquests.admin.view_header", name, data.active.size(), data.completed.size()), false);
        data.active.keySet().forEach(id -> src.sendSuccess(() -> Msg.tr("justquests.admin.view_active", id), false));
        data.completed.keySet().forEach(id -> src.sendSuccess(() -> Msg.tr("justquests.admin.view_done", id), false));
        return 1;
    }

    private static int adminResetAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String name = target.getName().getString();
        WorldQuestStore store = WorldQuestStore.get();
        if (store != null && store.has(target.getUUID())) {
            PlayerQuestData data = store.get(target.getUUID());
            com.erikedits.justquests.generator.GenV2.releaseAllFor(target.getUUID());
            data.active.clear();
            data.completed.clear();
            data.pendingClaim.clear();
            store.markDirty();
        }
        com.erikedits.justquests.network.QuestNetwork.syncProgress(target);
        ctx.getSource().sendSuccess(() -> Msg.tr("justquests.admin.reset_all", name), true);
        return 1;
    }

    private static int adminResetOne(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String name = target.getName().getString();
        WorldQuestStore store = WorldQuestStore.get();
        if (store != null) {
            PlayerQuestData data = store.peek(target.getUUID());
            if (data != null) {
                if (data.isActive(id)) com.erikedits.justquests.generator.GenV2.abandoned(id, target.getUUID());
                data.active.remove(id);
                data.completed.remove(id);
                if (data.pendingClaim.remove(id) != null) com.erikedits.justquests.generator.GenV2.rewardsClaimed(id, target.getUUID());
                store.markDirty();
            }
        }
        com.erikedits.justquests.network.QuestNetwork.syncProgress(target);
        ctx.getSource().sendSuccess(() -> Msg.tr("justquests.admin.reset_one", id, name), true);
        return 1;
    }

    private static int adminComplete(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        CommandSourceStack src = ctx.getSource();
        Quest quest = QuestManager.INSTANCE.get(id);
        if (quest == null) {
            src.sendFailure(Msg.tr("justquests.error.unknown_quest", id));
            return 0;
        }
        WorldQuestStore store = WorldQuestStore.get();
        if (store == null) {
            src.sendFailure(Msg.tr("justquests.error.storage"));
            return 0;
        }
        PlayerQuestData data = store.get(target.getUUID());
        com.erikedits.justquests.progress.QuestProgressService.finish(target, data, id, quest);
        store.markDirty();
        String name = target.getName().getString();
        com.erikedits.justquests.network.QuestNetwork.syncProgress(target);
        src.sendSuccess(() -> Msg.tr("justquests.admin.complete", id, name), true);
        return 1;
    }
}
