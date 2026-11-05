package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machine;
import com.erikedits.justquests.create.Machines;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** A press on an item in the world or on a belt. Over a basin it is {@link BasinRecipeMixin}. */
@Mixin(MechanicalPressBlockEntity.class)
abstract class MechanicalPressMixin {
    @Inject(method = "onItemPressed", at = @At("HEAD"))
    private void justquests_create$pressed(ItemStack result, CallbackInfo ci) {
        Machines.made(Machine.PRESS, (BlockEntity) (Object) this, List.of(result));
    }
}
