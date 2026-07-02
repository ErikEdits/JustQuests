package com.erikedits.justquests.registry;

import com.erikedits.justquests.JustQuests;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Data attachments. The v0.1 per-player quest attachment was dropped once
 * progress moved to per-world storage (and it isn't needed on 1.20.4, which
 * never shipped v0.1). The empty register is kept so the mod-bus wiring
 * stays in place for future attachments.
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JustQuests.MOD_ID);
}
