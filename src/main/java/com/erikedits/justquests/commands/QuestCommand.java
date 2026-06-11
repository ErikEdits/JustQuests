package com.erikedits.justquests.commands;

import com.erikedits.justquests.data.Quest;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.data.objective.QuestObjective;
import com.erikedits.justquests.player.PlayerQuests;
import com.erikedits.justquests.player.QuestProgress;
import com.erikedits.justquests.registry.ModAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

public class QuestCommand {
    private static final SuggestionProvider<CommandSourceStack> AVAILABLE_QUESTS = (ctx, builder) ->
        SharedSuggestionProvider.suggestResource(QuestManager.INSTANCE.getQuests().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> ACTIVE_QUESTS = (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            return SharedSuggestionProvider.suggestResource(
                player.getData(ModAttachments.PLAYER_QUESTS).active().keySet(), builder);
        }
        return builder.buildFuture();
    };

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quest")
            .then(Commands.literal("list").executes(QuestCommand::list))
            .then(Commands.literal("progress").executes(QuestCommand::progress))
            .then(Commands.literal("accept")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(AVAILABLE_QUESTS)
                    .executes(ctx -> accept(ctx, ResourceLocationArgument.getId(ctx, "id")))))
            .then(Commands.literal("abandon")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests(ACTIVE_QUESTS)
                    .executes(ctx -> abandon(ctx, ResourceLocationArgument.getId(ctx, "id")))))
            .then(Commands.literal("reload")
                .requires(src -> src.hasPermission(2))
                .executes(QuestCommand::reload)));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Map<ResourceLocation, Quest> quests = QuestManager.INSTANCE.getQuests();
        if (quests.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7No quests defined."), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("§eAvailable quests:"), false);
        quests.forEach((id, quest) -> src.sendSuccess(() ->
            Component.literal("§f - §b" + id + " §7— §f" + quest.title()), false));
        return quests.size();
    }

    private static int progress(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerQuests data = player.getData(ModAttachments.PLAYER_QUESTS);

        if (data.active().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7You have no active quests."), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§eActive quests:"), false);

        for (Map.Entry<ResourceLocation, QuestProgress> entry : data.active().entrySet()) {
            Quest quest = QuestManager.INSTANCE.get(entry.getKey());
            if (quest == null) continue;

            ctx.getSource().sendSuccess(() ->
                Component.literal("§b" + entry.getKey() + " §7— §f" + quest.title()), false);

            QuestProgress prog = entry.getValue();
            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                int current = Math.min(prog.get(i), obj.requiredCount());
                int needed = obj.requiredCount();
                String bar = current >= needed ? "§a✓" : "§7" + current + "/" + needed;
                final int idx = i;
                ctx.getSource().sendSuccess(() ->
                    Component.literal("  " + bar + " §f" + quest.objectives().get(idx).displayName()), false);
            }
        }
        return data.active().size();
    }

    private static int accept(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        Quest quest = QuestManager.INSTANCE.get(id);
        if (quest == null) {
            ctx.getSource().sendFailure(Component.literal("§cUnknown quest: " + id));
            return 0;
        }

        PlayerQuests data = player.getData(ModAttachments.PLAYER_QUESTS);
        if (data.isCompleted(id)) {
            ctx.getSource().sendFailure(Component.literal("§cYou already completed this quest."));
            return 0;
        }
        if (data.isActive(id)) {
            ctx.getSource().sendFailure(Component.literal("§cQuest is already active."));
            return 0;
        }

        data.accept(id);
        ctx.getSource().sendSuccess(() ->
            Component.literal("§a✓ Accepted: " + quest.title()), false);
        return 1;
    }

    private static int abandon(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        PlayerQuests data = player.getData(ModAttachments.PLAYER_QUESTS);
        if (!data.isActive(id)) {
            ctx.getSource().sendFailure(Component.literal("§cQuest is not active."));
            return 0;
        }

        data.abandon(id);
        ctx.getSource().sendSuccess(() ->
            Component.literal("§7Abandoned quest: " + id), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("§eUse /reload to reload quest data from datapacks."), false);
        return 1;
    }
}
