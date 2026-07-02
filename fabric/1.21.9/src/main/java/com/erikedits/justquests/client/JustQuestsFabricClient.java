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
        openQuests = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.justquests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, net.minecraft.client.KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openQuests.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new QuestScreen());
                }
            }
        });
    }
}
