package com.erikedits.justquests.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric client entry point: registers the quest-book keybind (default J) and
 * opens the shared {@link QuestScreen}. Singleplayer for now (the screen reads
 * quest data directly) — same interim GUI as the NeoForge build.
 */
public class JustQuestsFabricClient implements ClientModInitializer {
    private static KeyMapping openQuests;

    @Override
    public void onInitializeClient() {
        // Receive server -> client quest sync and cache it for the quest book.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            com.erikedits.justquests.network.QuestSyncPayload.TYPE,
            (payload, context) -> context.client().execute(
                () -> com.erikedits.justquests.network.ClientQuestData.accept(payload.json())));

        openQuests = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.justquests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.misc"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openQuests.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new QuestScreen());
                }
            }
        });
    }
}
