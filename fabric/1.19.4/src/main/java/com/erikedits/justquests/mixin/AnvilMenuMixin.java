package com.erikedits.justquests.mixin;

import com.erikedits.justquests.progress.Workstations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** anvil_item and enchant_item "via": "anvil": the result is taken, the two inputs still lie in the anvil. */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void justquests$onTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
            Workstations.anvil(sp, menu.getSlot(0).getItem(), menu.getSlot(1).getItem(), stack);
        }
    }
}
