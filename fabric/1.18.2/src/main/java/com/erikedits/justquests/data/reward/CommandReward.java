package com.erikedits.justquests.data.reward;

import com.erikedits.justquests.JustQuests;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Runs a server command as the reward. Because it can run any command, it
 * covers economy payouts, effects, etc. through whatever plugin/mod is
 * present, with no direct dependency (Q52). "{player}" in the command is
 * replaced with the player's name; the command runs as the player (@s) at
 * permission level 4.
 */
public record CommandReward(String command) implements QuestReward {
    public static final String TYPE_ID = "justquests:command";

    public static final MapCodec<CommandReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("command").forGetter(CommandReward::command)
    ).apply(instance, CommandReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        MinecraftServer server = player.getLevel().getServer();
        if (server == null) return;
        String cmd = command.replace("{player}", player.getName().getString());
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        try {
            CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
            server.getCommands().performCommand(source, cmd);
        } catch (Exception e) {
            JustQuests.LOG.error("Command reward failed: /{}", cmd, e);
        }
    }

    @Override
    public String displayName() {
        return "Run: /" + command;
    }

    @Override
    public Component display() {
        return new net.minecraft.network.chat.TextComponent(displayName());
    }
}
