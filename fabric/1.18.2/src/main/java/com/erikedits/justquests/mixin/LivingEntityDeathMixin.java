package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** kill_mob (1.18.2: no ServerLivingEntityEvents.AFTER_DEATH — hook die()). */
@Mixin(LivingEntity.class)
public class LivingEntityDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void justquests$onDeath(DamageSource source, CallbackInfo ci) {
        if (source.getEntity() instanceof ServerPlayer killer) {
            FabricQuestHooks.onKill(killer, ((LivingEntity) (Object) this).getType());
        }
    }
}
