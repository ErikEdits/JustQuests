package com.erikedits.justquests.create.mixin;

import com.erikedits.justquests.create.Machine;
import com.erikedits.justquests.create.Machines;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SpoutBlockEntity.class)
abstract class SpoutMixin {
    @ModifyExpressionValue(method = "whenItemHeld", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;fillItem(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack justquests_create$filled(ItemStack result) {
        Machines.made(Machine.FILL, (BlockEntity) (Object) this, List.of(result));
        return result;
    }
}
