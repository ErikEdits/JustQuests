package com.erikedits.justquests.mixin;

import com.erikedits.justquests.progress.GoalEvents;
import net.minecraft.advancements.critereon.FishingRodHookedTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/** fish_item (0.6.0): the game tells its advancements what a fishing rod pulled in. */
@Mixin(FishingRodHookedTrigger.class)
public class FishingRodHookedTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void justquests$onFished(ServerPlayer player, ItemStack rod, FishingHook hook, Collection<ItemStack> caught, CallbackInfo ci) {
        GoalEvents.fished(player, caught);
    }
}
