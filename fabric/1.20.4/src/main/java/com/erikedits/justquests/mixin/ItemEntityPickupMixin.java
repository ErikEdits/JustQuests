package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * collect_item: what a player picks up from the ground. The game moves the stack into the
 * inventory before LivingEntity.take() runs, which empties it, so the stack is remembered first and
 * what left it is counted afterwards (a full inventory may take only part of it).
 */
@Mixin(ItemEntity.class)
public class ItemEntityPickupMixin {
    @Unique
    private ItemStack justquests$before = ItemStack.EMPTY;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void justquests$beforeTouch(Player player, CallbackInfo ci) {
        justquests$before = player instanceof ServerPlayer ? ((ItemEntity) (Object) this).getItem().copy() : ItemStack.EMPTY;
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void justquests$afterTouch(Player player, CallbackInfo ci) {
        ItemStack before = justquests$before;
        justquests$before = ItemStack.EMPTY;
        if (!(player instanceof ServerPlayer serverPlayer) || before.isEmpty()) return;
        ItemEntity self = (ItemEntity) (Object) this;
        // taken whole, the item entity is gone (its stack gets the old count back); else the rest lies there
        int left = self.isRemoved() ? 0 : self.getItem().getCount();
        int picked = before.getCount() - left;
        if (picked > 0) FabricQuestHooks.onItemPickup(serverPlayer, before, picked);
    }
}
