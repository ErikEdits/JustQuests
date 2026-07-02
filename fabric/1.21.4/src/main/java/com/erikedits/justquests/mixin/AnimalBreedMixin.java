package com.erikedits.justquests.mixin;

import com.erikedits.justquests.event.FabricQuestHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** breed_animal: spawnChildFromBreeding fires when two animals produce a baby. */
@Mixin(Animal.class)
public class AnimalBreedMixin {
    @Inject(method = "spawnChildFromBreeding", at = @At("HEAD"))
    private void justquests$onBreed(ServerLevel level, Animal partner, CallbackInfo ci) {
        Animal self = (Animal) (Object) this;
        if (self.getLoveCause() instanceof ServerPlayer player) {
            FabricQuestHooks.onBreed(player, self.getType());
        }
    }
}
