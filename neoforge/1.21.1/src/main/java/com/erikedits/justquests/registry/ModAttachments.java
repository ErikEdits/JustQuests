package com.erikedits.justquests.registry;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.player.PlayerQuests;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JustQuests.MOD_ID);

    public static final Supplier<AttachmentType<PlayerQuests>> PLAYER_QUESTS =
        ATTACHMENTS.register("player_quests", () ->
            AttachmentType.serializable(PlayerQuests::new)
                .copyOnDeath()
                .build());
}
