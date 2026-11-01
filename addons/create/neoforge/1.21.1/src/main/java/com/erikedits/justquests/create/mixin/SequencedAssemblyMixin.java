package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machines;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The last step of a sequenced assembly hands out the finished item instead of the next unfinished
 * one. The machine doing the step reports its result right after, which then counts as assembled too.
 */
@Mixin(SequencedAssemblyRecipe.class)
abstract class SequencedAssemblyMixin {
    @ModifyReturnValue(method = "advance", at = @At("RETURN"))
    private ItemStack justquests_create$advanced(ItemStack result) {
        if (!result.has(AllDataComponents.SEQUENCED_ASSEMBLY)) Machines.assembled(result);
        return result;
    }
}
