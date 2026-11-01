package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machines;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Items in a fan's air current, lying in the world or on a belt: washed, smoked, blasted, haunted. */
@Mixin(FanProcessing.class)
abstract class FanProcessingMixin {
    @ModifyExpressionValue(
        method = {
            "applyProcessing(Lnet/minecraft/world/entity/item/ItemEntity;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Z",
            "applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;"
        },
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;process(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Ljava/util/List;"))
    private static List<ItemStack> justquests_create$processed(List<ItemStack> results, @Local(argsOnly = true) FanProcessingType type) {
        if (results != null) Machines.fanned(type, results);
        return results;
    }
}
