package com.erikedits.justquests;

import com.erikedits.justquests.commands.QuestCommand;
import com.erikedits.justquests.community.CommunityHints;
import com.erikedits.justquests.data.QuestManager;
import com.erikedits.justquests.event.FabricQuestHooks;
import com.erikedits.justquests.storage.CustomQuestLoader;
import com.erikedits.justquests.storage.WorldQuestStore;
import com.erikedits.justquests.storage.WorldSettings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;

/**
 * Fabric entry point. Wires the shared quest domain code to Fabric's loader:
 * reload listener, commands, server lifecycle/tick, and the game events.
 * Events that Fabric API doesn't provide (item pickup, craft, tame, breed,
 * consume, smelt, place, advancement) come in via mixins that call
 * {@link FabricQuestHooks}.
 */
public class JustQuestsFabric implements ModInitializer {
    private static final int SAVE_INTERVAL_TICKS = 600;   // 30 seconds
    private static final int CUSTOM_INTERVAL_TICKS = 60;  // 3 seconds
    private int saveCounter = 0;
    private int customCounter = 0;

    @Override
    public void onInitialize() {
        // Datapack quest reload listener
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(QuestManager.INSTANCE);

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
            QuestCommand.register(dispatcher));

        // Server lifecycle
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            WorldQuestStore.load(server);
            WorldSettings.load(server);      // load settings before readers
            CustomQuestLoader.init(server);
            CommunityHints.init(server);
            JustQuests.LOG.info("JustQuests loaded (Fabric)");
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            WorldQuestStore.unload();
            CustomQuestLoader.clear();
            CommunityHints.clear();
            WorldSettings.reset();
        });

        // Per-tick: periodic save, custom-quest reload check, reach polling
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++saveCounter >= SAVE_INTERVAL_TICKS) {
                saveCounter = 0;
                WorldQuestStore store = WorldQuestStore.get();
                if (store != null) store.saveIfDirty();
            }
            if (++customCounter >= CUSTOM_INTERVAL_TICKS) {
                customCounter = 0;
                CustomQuestLoader.tickCheck();
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % 20 == 0) FabricQuestHooks.onPlayerTickReach(player);
            }
        });

        // Login: one-time Discord welcome (0.1.5)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            CommunityHints.onLogin(handler.player));

        // mine_block
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (player instanceof ServerPlayer sp) {
                FabricQuestHooks.onBlockBreak(sp, state.getBlock());
            }
        });

        // visit_dimension
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
            FabricQuestHooks.onDimension(player, destination.dimension().location()));
    }
}
