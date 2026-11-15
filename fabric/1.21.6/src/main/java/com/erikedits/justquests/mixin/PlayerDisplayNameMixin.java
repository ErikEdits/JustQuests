package com.erikedits.justquests.mixin;

import com.erikedits.justquests.badge.NameSign;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The sign of the player's badge before their name in the chat and other messages (0.6.0). */
@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void justquests$badgeSign(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof ServerPlayer player) cir.setReturnValue(NameSign.decorate(player, cir.getReturnValue()));
    }
}
