package com.erikedits.justquests.data.reward;

import com.erikedits.justquests.JustQuests;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Gives the player items rolled from a loot table (random reward, Q29). */
public record LootTableReward(ResourceLocation table) implements QuestReward {
    public static final String TYPE_ID = "justquests:loot_table";

    public static final MapCodec<LootTableReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("loot_table").forGetter(LootTableReward::table)
    ).apply(instance, LootTableReward::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void grant(ServerPlayer player) {
        try {
            ServerLevel level = player.serverLevel();
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, table);
            LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(key);
            LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .create(LootContextParamSets.GIFT);
            lootTable.getRandomItems(params, stack -> {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            });
        } catch (Exception e) {
            JustQuests.LOG.error("Loot-table reward failed for {}", table, e);
        }
    }

    @Override
    public String displayName() {
        return "Loot: " + table;
    }

    @Override
    public Component display() {
        return Component.literal(displayName());
    }
}
