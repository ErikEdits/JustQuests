package com.erikedits.justquests.storage;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Wires the WorldQuestStore into the server lifecycle:
 *  - load on start
 *  - flush to disk every 30s if dirty (loose save, Q47)
 *  - save + clear on stop
 */
public class ServerStorageEvents {
    private static final int SAVE_INTERVAL_TICKS = 600; // 30 seconds
    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        WorldQuestStore.load(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        WorldQuestStore.unload();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter >= SAVE_INTERVAL_TICKS) {
            tickCounter = 0;
            WorldQuestStore store = WorldQuestStore.get();
            if (store != null) {
                store.saveIfDirty();
            }
        }
    }
}
