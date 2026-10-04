package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * collect_item: an item pickup ends in LivingEntity.take(entity, amount).
 * Player does not override take() on any supported version, so the injection
 * must target LivingEntity; targeting Player finds no method and, with
 * defaultRequire=1, fails when the class loads.
 */
@Mixin(LivingEntity.class)
public class LivingEntityTakeMixin {
    @Inject(method = "take(Lnet/minecraft/world/entity/Entity;I)V", at = @At("HEAD"))
    private void justquests$onTake(Entity entity, int amount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && entity instanceof ItemEntity item) {
            FabricQuestHooks.onItemPickup(player, item.getItem(), amount);
        }
    }
}
