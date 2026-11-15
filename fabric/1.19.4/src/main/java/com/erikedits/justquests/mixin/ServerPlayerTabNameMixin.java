package com.erikedits.justquests.mixin;

import com.erikedits.justquests.badge.NameSign;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The sign of the player's badge before their name in the tab list (0.6.0). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTabNameMixin {
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void justquests$badgeSign(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(NameSign.tabName((ServerPlayer) (Object) this, cir.getReturnValue()));
    }
}
