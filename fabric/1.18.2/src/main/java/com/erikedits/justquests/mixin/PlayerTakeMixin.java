package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** collect_item: Player.take(entity, amount) fires when items are picked up. */
@Mixin(Player.class)
public class PlayerTakeMixin {
    @Inject(method = "take", at = @At("HEAD"))
    private void justquests$onTake(Entity entity, int amount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && entity instanceof ItemEntity item) {
            FabricQuestHooks.onItemPickup(player, item.getItem(), amount);
        }
    }
}
