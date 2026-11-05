package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machine;
import com.erikedits.justquests.create.Machines;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(MillstoneBlockEntity.class)
abstract class MillstoneMixin {
    @ModifyExpressionValue(method = "process", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/kinetics/millstone/MillingRecipe;rollResults(Lnet/minecraft/util/RandomSource;)Ljava/util/List;"))
    private List<ItemStack> justquests_create$milled(List<ItemStack> results) {
        Machines.made(Machine.MILL, (BlockEntity) (Object) this, results);
        return results;
    }
}
