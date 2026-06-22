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
 * Client-only: a key (default J) that opens the quest book screen. The
 * screen reads quest data directly (works in singleplayer; multiplayer sync
 * comes with the v0.2 GUI design). Interim GUI — replaced by the voted design.
 */
public final class QuestClient {
    public static final KeyMapping OPEN_QUESTS = new KeyMapping(
        "key.justquests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyMapping.Category.MISC);

    private QuestClient() {}

    @EventBusSubscriber(modid = JustQuests.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_QUESTS);
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
        }
    }
}
