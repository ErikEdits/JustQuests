package com.erikedits.justquests;

import com.erikedits.justquests.commands.QuestCommand;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.player.PlayerQuestEvents;
import com.erikedits.justquests.registry.ModAttachments;
import com.erikedits.justquests.storage.ServerStorageEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(JustQuests.MOD_ID)
public class JustQuests {
    public static final String MOD_ID = "justquests";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public JustQuests(IEventBus modEventBus) {
        ModAttachments.ATTACHMENTS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::onReload);
        NeoForge.EVENT_BUS.addListener(QuestCommand::onRegisterCommands);
        NeoForge.EVENT_BUS.register(new PlayerQuestEvents());
        NeoForge.EVENT_BUS.register(new ServerStorageEvents());

        LOG.info("JustQuests loaded");
    }

    private void onReload(AddReloadListenerEvent event) {
        event.addListener(QuestManager.INSTANCE);
    }
}
