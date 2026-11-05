package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machine;
import com.erikedits.justquests.create.Machines;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A saw with an item on it. A package it opens is not sawing, so that does not count. */
@Mixin(SawBlockEntity.class)
abstract class SawMixin {
    @Unique
    private boolean justquests_create$unpacking;

    @Inject(method = "applyRecipe", at = @At("HEAD"))
    private void justquests_create$before(CallbackInfo ci) {
        justquests_create$unpacking = PackageItem.isPackage(((SawBlockEntity) (Object) this).inventory.getStackInSlot(0));
    }

    @Inject(method = "applyRecipe", at = @At("TAIL"))
    private void justquests_create$sawed(CallbackInfo ci) {
        SawBlockEntity self = (SawBlockEntity) (Object) this;
        if (!justquests_create$unpacking) Machines.made(Machine.SAW, self, Machines.results(self.inventory));
    }
}
