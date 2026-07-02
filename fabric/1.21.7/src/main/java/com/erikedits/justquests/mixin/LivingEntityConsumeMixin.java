package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** consume_item: completeUsingItem fires when a player finishes eating/drinking. */
@Mixin(LivingEntity.class)
public class LivingEntityConsumeMixin {
    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void justquests$onConsume(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            FabricQuestHooks.onConsume(player, player.getUseItem());
        }
    }
}
