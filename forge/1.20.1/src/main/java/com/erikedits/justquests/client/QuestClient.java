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
 * Client-only: a key (default J) that opens the quest book screen (1.20.1 has
 * GuiGraphics, so the interim GUI is available here). Interim GUI — replaced by
 * the community-voted design.
 */
public final class QuestClient {
    public static final KeyMapping OPEN_QUESTS = new KeyMapping(
        "key.justquests.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.misc");

    private QuestClient() {}

    @Mod.EventBusSubscriber(modid = JustQuests.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_QUESTS);
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
        }
    }
}
