package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machines;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/** A mixer or press working on a basin: the recipe's outputs when the basin really takes them. */
@Mixin(BasinRecipe.class)
abstract class BasinRecipeMixin {
    @WrapOperation(
        method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;acceptOutputs(Ljava/util/List;Ljava/util/List;Z)Z"))
    private static boolean justquests_create$outputs(BasinBlockEntity basin, List<ItemStack> items, List<FluidStack> fluids, boolean simulate, Operation<Boolean> original) {
        List<ItemStack> made = new ArrayList<>();
        if (!simulate) items.forEach(stack -> made.add(stack.copy()));
        boolean accepted = original.call(basin, items, fluids, simulate);
        if (accepted && !simulate) Machines.basin(basin, made);
        return accepted;
    }
}
