package com.erikedits.justquests.registry;

import com.erikedits.justquests.JustQuests;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * No attachments here: the only one (the v0.1 NBT migration holder) was
 * dropped from 1.21.6+, where NeoForge removed INBTSerializable. The v0.1
 * migration only ever applied to 1.21.1 anyway.
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JustQuests.MOD_ID);
}
