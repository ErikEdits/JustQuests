package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machine;
import com.erikedits.justquests.create.Machines;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(MechanicalCrafterBlockEntity.class)
abstract class MechanicalCrafterMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler;tryToApplyRecipe(Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/crafter/RecipeGridHandler$GroupedItems;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack justquests_create$crafted(ItemStack result) {
        if (result != null) Machines.made(Machine.MECHANICAL_CRAFT, (BlockEntity) (Object) this, List.of(result));
        return result;
    }
}
