package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** craft_item: ResultSlot.onTake fires when a crafting result is taken. */
@Mixin(ResultSlot.class)
public class ResultSlotMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void justquests$onCraft(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            FabricQuestHooks.onCraft(sp, stack);
        }
    }
}
