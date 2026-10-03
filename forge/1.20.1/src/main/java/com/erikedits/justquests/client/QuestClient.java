package com.erikedits.justquests.client;

import com.erikedits.justquests.JustQuests;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only: the quest book key (default J), the HUD tracker key (H) and the
 * hook that draws the tracker. The book and the tracker read the synced quest data
 * (ClientQuestData), so they work in singleplayer and on servers.
 */
public final class QuestClient {
    public static final KeyMapping OPEN_QUESTS = new KeyMapping(
        "key.justquests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.misc");
    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
        "key.justquests.hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.misc");

    private QuestClient() {}

    @Mod.EventBusSubscriber(modid = JustQuests.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_QUESTS);
            event.register(TOGGLE_HUD);
        }
    }

    @Mod.EventBusSubscriber(modid = JustQuests.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent
        static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (OPEN_QUESTS.consumeClick()) {
                mc.setScreen(new QuestScreen());
            }
            while (TOGGLE_HUD.consumeClick()) QuestHud.toggle();
        }

        /** Draws the quest tracker on top of the in-game HUD. */
        @SubscribeEvent
        static void onRenderGui(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
            QuestHud.render(event.getGuiGraphics());
        }

        /** Forget the last server's quests on disconnect, so the book never shows stale data. */
        @SubscribeEvent
        static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            com.erikedits.justquests.network.ClientQuestData.clear();
        }
    }
}
