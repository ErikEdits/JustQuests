package com.erikedits.justquests;

import com.erikedits.justquests.commands.QuestCommand;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.player.PlayerQuestEvents;
import com.erikedits.justquests.storage.ServerStorageEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge entry point. Forge 1.20.1 shares NeoForge's event model (it forked at
 * 1.20.1), so the loader glue mirrors the NeoForge build with net.minecraftforge
 * packages; the quest domain code is the shared Mojmap code.
 */
@Mod(JustQuests.MOD_ID)
public class JustQuests {
    public static final String MOD_ID = "justquests";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public JustQuests() {
        MinecraftForge.EVENT_BUS.addListener(this::onReload);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.register(new PlayerQuestEvents());
        MinecraftForge.EVENT_BUS.register(new ServerStorageEvents());
        LOG.info("JustQuests loaded (Forge)");
    }

    private void onReload(AddReloadListenerEvent event) {
        event.addListener(QuestManager.INSTANCE);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        QuestCommand.register(event.getDispatcher());
    }
}
