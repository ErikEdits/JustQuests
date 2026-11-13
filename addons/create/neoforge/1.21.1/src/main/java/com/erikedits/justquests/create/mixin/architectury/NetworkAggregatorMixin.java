package com.erikedits.justquests.create.mixin.architectury;

import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;

/**
 * Not about Create: a guard for big modpacks (#15). Architectury keeps the packets mods register in
 * plain HashMaps. NeoForge constructs mods in parallel, and many mods register their packets right
 * there, so two of them can write at the same moment and an entry gets lost. Joining a world then
 * fails with "Invalid player data" and "codec is null" in the log (seen with Stellaris). This wraps
 * the maps in synchronized ones while the class loads, before any mod can register.
 * Only applied when Architectury is installed, see {@code ArchitecturyMixins}.
 */
@Mixin(targets = "dev.architectury.impl.NetworkAggregator", remap = false)
abstract class NetworkAggregatorMixin {
    @Shadow @Final @Mutable public static Map<Object, Object> C2S_TYPE;
    @Shadow @Final @Mutable public static Map<Object, Object> S2C_TYPE;
    @Shadow @Final @Mutable public static Map<Object, Object> C2S_RECEIVER;
    @Shadow @Final @Mutable public static Map<Object, Object> S2C_RECEIVER;
    @Shadow @Final @Mutable public static Map<Object, Object> C2S_CODECS;
    @Shadow @Final @Mutable public static Map<Object, Object> S2C_CODECS;
    @Shadow @Final @Mutable public static Map<Object, Object> C2S_TRANSFORMERS;
    @Shadow @Final @Mutable public static Map<Object, Object> S2C_TRANSFORMERS;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void justquests_create$threadSafe(CallbackInfo ci) {
        C2S_TYPE = Collections.synchronizedMap(C2S_TYPE);
        S2C_TYPE = Collections.synchronizedMap(S2C_TYPE);
        C2S_RECEIVER = Collections.synchronizedMap(C2S_RECEIVER);
        S2C_RECEIVER = Collections.synchronizedMap(S2C_RECEIVER);
        C2S_CODECS = Collections.synchronizedMap(C2S_CODECS);
        S2C_CODECS = Collections.synchronizedMap(S2C_CODECS);
        C2S_TRANSFORMERS = Collections.synchronizedMap(C2S_TRANSFORMERS);
        S2C_TRANSFORMERS = Collections.synchronizedMap(S2C_TRANSFORMERS);
        LoggerFactory.getLogger("JustQuests Create Addon")
            .info("Made Architectury's packet maps safe for parallel mod loading");
    }
}
