package com.erikedits.justquests.create;

import com.erikedits.justquests.data.objective.QuestObjective;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Adds one JustQuests objective type per Create machine. The mixins report what the machines make. */
@Mod(JustQuestsCreate.MOD_ID)
public final class JustQuestsCreate {
    public static final String MOD_ID = "justquests_create";

    public JustQuestsCreate(IEventBus modBus) {
        for (Machine machine : Machine.values()) {
            QuestObjective.register(machine.typeId(), MachineObjective.codec(machine));
        }
        Owners.ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Owners::placed);
    }
}
