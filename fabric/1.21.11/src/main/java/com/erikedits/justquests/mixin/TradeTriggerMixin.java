package com.erikedits.justquests.mixin;

import com.erikedits.justquests.progress.GoalEvents;
import net.minecraft.advancements.criterion.TradeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** trade_villager (0.6.0): the game tells its advancements about every trade with a villager or the wandering trader. */
@Mixin(TradeTrigger.class)
public class TradeTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void justquests$onTrade(ServerPlayer player, AbstractVillager trader, ItemStack bought, CallbackInfo ci) {
        GoalEvents.traded(player, trader, bought);
    }
}
