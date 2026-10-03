package com.erikedits.justquests.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric client entry point: receives the quest sync, registers the quest book key
 * (default J) and the HUD tracker key (H), opens {@link QuestScreen} and draws
 * {@link QuestHud}.
 */
public class JustQuestsFabricClient implements ClientModInitializer {
    private static KeyMapping openQuests;
    private static KeyMapping toggleHud;

    @Override
    public void onInitializeClient() {
        // Receive server -> client quest sync and cache it for the quest book.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            com.erikedits.justquests.network.QuestSyncPayload.TYPE,
            (payload, context) -> context.client().execute(
                () -> com.erikedits.justquests.network.ClientQuestData.accept(payload.json())));

        // Drop the last server's quests on disconnect, so the book never shows stale data.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
            (handler, client) -> client.execute(com.erikedits.justquests.network.ClientQuestData::clear));

        openQuests = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.justquests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.misc"));
        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.justquests.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.misc"));

        // Quest tracker on top of the in-game HUD.
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, tick) -> QuestHud.render(graphics));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openQuests.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new QuestScreen());
                }
            }
            while (toggleHud.consumeClick()) QuestHud.toggle();
        });
    }
}
