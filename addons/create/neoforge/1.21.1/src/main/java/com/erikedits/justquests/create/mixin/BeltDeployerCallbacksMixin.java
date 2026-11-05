package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machine;
import com.erikedits.justquests.create.Machines;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.deployer.BeltDeployerCallbacks;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** A deployer applying its item to one on a belt or depot. */
@Mixin(BeltDeployerCallbacks.class)
abstract class BeltDeployerCallbacksMixin {
    @ModifyExpressionValue(method = "activate", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/foundation/recipe/RecipeApplier;applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;"))
    private static List<ItemStack> justquests_create$deployed(List<ItemStack> results, @Local(argsOnly = true) DeployerBlockEntity deployer) {
        Machines.made(Machine.DEPLOY, deployer, results);
        return results;
    }
}
