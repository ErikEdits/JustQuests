package com.erikedits.justquests.client;

import com.erikedits.justquests.JustQuests;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only: the quest book key (default J), the HUD tracker key (H) and the
 * hook that draws the tracker. The book and the tracker read the synced quest data
 * (ClientQuestData), so they work in singleplayer and on servers.
 */
public final class QuestClient {
    public static final KeyMapping OPEN_QUESTS = new KeyMapping(
        "key.justquests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyMapping.Category.MISC);
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
        "key.justquests.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyMapping.Category.MISC);

    private QuestClient() {}

    @EventBusSubscriber(modid = JustQuests.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_QUESTS);
            event.register(TOGGLE_HUD);
        }
    }

    @EventBusSubscriber(modid = JustQuests.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent
        static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (OPEN_QUESTS.consumeClick()) {
                mc.setScreen(new QuestScreen());
            }
            while (TOGGLE_HUD.consumeClick()) QuestHud.toggle();
        }

        /** Draws the quest tracker on top of the in-game HUD. */
        @SubscribeEvent
        static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
            QuestHud.render(event.getGuiGraphics());
        }

        /** Forget the last server's quests on disconnect, so the book never shows stale data. */
        @SubscribeEvent
        static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            com.erikedits.justquests.network.ClientQuestData.clear();
        }
    }
}
