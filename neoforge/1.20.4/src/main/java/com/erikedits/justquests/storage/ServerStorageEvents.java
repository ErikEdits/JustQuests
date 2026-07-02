package com.erikedits.justquests.storage;

import com.erikedits.justquests.community.CommunityHints;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Wires the WorldQuestStore into the server lifecycle:
 *  - load on start
 *  - flush to disk every 30s if dirty (loose save, Q47)
 *  - save + clear on stop
 */
public class ServerStorageEvents {
    private static final int SAVE_INTERVAL_TICKS = 600;   // 30 seconds
    private static final int CUSTOM_INTERVAL_TICKS = 60;  // 3 seconds
    private int tickCounter = 0;
    private int customCounter = 0;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        WorldQuestStore.load(event.getServer());
        WorldSettings.load(event.getServer());   // load settings before readers
        CustomQuestLoader.init(event.getServer());
        CommunityHints.init(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        WorldQuestStore.unload();
        CustomQuestLoader.clear();
        CommunityHints.clear();
        WorldSettings.reset();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter >= SAVE_INTERVAL_TICKS) {
            tickCounter = 0;
            WorldQuestStore store = WorldQuestStore.get();
            if (store != null) {
                store.saveIfDirty();
            }
        }
        // auto-reload custom quests if the file changed (Q32)
        if (++customCounter >= CUSTOM_INTERVAL_TICKS) {
            customCounter = 0;
            CustomQuestLoader.tickCheck();
        }
    }
}
