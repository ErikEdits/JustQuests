package com.erikedits.justquests.mixin;

import com.erikedits.justquests.progress.GoalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** brew_potion (0.6.0): a potion taken out of one of the brewing stand's three bottle slots. */
@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public class BrewingPotionSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void justquests$onBrewed(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) GoalEvents.brewed(sp, stack);
    }
}
