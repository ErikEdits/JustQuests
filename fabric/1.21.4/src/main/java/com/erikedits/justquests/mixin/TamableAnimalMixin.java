package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** tame_animal: TamableAnimal.tame(player) fires when a wolf/cat/etc. is tamed. */
@Mixin(TamableAnimal.class)
public class TamableAnimalMixin {
    @Inject(method = "tame", at = @At("TAIL"))
    private void justquests$onTame(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            TamableAnimal self = (TamableAnimal) (Object) this;
            FabricQuestHooks.onTame(sp, self.getType());
        }
    }
}
