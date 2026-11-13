package com.erikedits.justquests.mixin;

import com.erikedits.justquests.progress.Workstations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * grindstone_item: a click on the grindstone's result slot that used up what lay in it. The result
 * slot is an anonymous class whose name differs between versions, so the click is watched instead.
 */
@Mixin(AbstractContainerMenu.class)
public class GrindstoneClickMixin {
    @Unique
    private ItemStack[] justquests$grind;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void justquests$beforeClick(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        justquests$grind = null;
        if ((Object) this instanceof GrindstoneMenu menu && slot == 2 && player instanceof ServerPlayer && menu.getSlot(2).hasItem()) {
            justquests$grind = new ItemStack[] {
                menu.getSlot(0).getItem().copy(), menu.getSlot(1).getItem().copy(), menu.getSlot(2).getItem().copy()};
        }
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void justquests$afterClick(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        ItemStack[] before = justquests$grind;
        justquests$grind = null;
        if (before == null) return;
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        // a take empties both input slots; a click that couldn't take the result leaves them
        if (menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty()) {
            Workstations.grindstone((ServerPlayer) player, before[0], before[1], before[2]);
        }
    }
}
