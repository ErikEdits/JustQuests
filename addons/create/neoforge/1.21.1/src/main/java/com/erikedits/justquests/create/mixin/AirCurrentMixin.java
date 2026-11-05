package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machines;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Says which fan is working while its air current processes items ({@link FanProcessingMixin}). */
@Mixin(AirCurrent.class)
abstract class AirCurrentMixin {
    @Shadow
    @Final
    public IAirCurrentSource source;

    @Inject(method = {"tickAffectedEntities", "tickAffectedHandlers"}, at = @At("HEAD"))
    private void justquests_create$start(CallbackInfo ci) {
        if (source instanceof BlockEntity be && be.getLevel() != null && !be.getLevel().isClientSide) Machines.fan(be);
    }

    @Inject(method = {"tickAffectedEntities", "tickAffectedHandlers"}, at = @At("RETURN"))
    private void justquests_create$end(CallbackInfo ci) {
        if (source instanceof BlockEntity be && be.getLevel() != null && !be.getLevel().isClientSide) Machines.fan(null);
    }
}
