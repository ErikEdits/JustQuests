package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** place_block: fires after a BlockItem successfully places its block. */
@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void justquests$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player) {
            BlockItem self = (BlockItem) (Object) this;
            FabricQuestHooks.onBlockPlace(player, self.getBlock());
        }
    }
}
