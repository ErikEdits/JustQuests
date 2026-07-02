package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** smelt_item: FurnaceResultSlot.onTake fires when a smelted result is taken. */
@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void justquests$onSmelt(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            FabricQuestHooks.onSmelt(sp, stack);
        }
    }
}
