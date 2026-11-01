package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machine;
import com.erikedits.justquests.create.Machines;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrushingWheelControllerBlockEntity.class)
abstract class CrushingWheelControllerMixin {
    @Inject(method = "applyRecipe", at = @At("TAIL"))
    private void justquests_create$crushed(CallbackInfo ci) {
        CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity) (Object) this;
        Machines.made(Machine.CRUSH, self, Machines.results(self.inventory));
    }
}
