package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Raise a statistic of the game's statistics screen (0.6.0): walk 1000 blocks, jump 50 times, play
 * for 60 minutes, sleep 5 times. Only what happens after the quest was taken counts. Distances count
 * in blocks and times in minutes; everything else as the statistic counts it.
 */
public record StatObjective(ResourceLocation stat, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:stat";

    public static final MapCodec<StatObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.comapFlatMap(StatObjective::read, ResourceLocation::toString).fieldOf("stat").forGetter(StatObjective::stat),
        Codec.INT.fieldOf("count").forGetter(StatObjective::count)
    ).apply(instance, StatObjective::new));

    /** A statistic of the statistics screen's General list; anything else makes the quest invalid (with a log line). */
    private static com.mojang.serialization.DataResult<ResourceLocation> read(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.indexOf(':') >= 0 ? id : "minecraft:" + id);
        if (rl == null) return com.mojang.serialization.DataResult.error(() -> "not a statistic id: " + id);
        if (!com.erikedits.justquests.progress.StatObjectives.known(rl)) {
            return com.mojang.serialization.DataResult.error(() -> "unknown statistic " + rl + " (use an id of the statistics screen's General list, like minecraft:jump)");
        }
        return com.mojang.serialization.DataResult.success(rl);
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** How many raw statistic points make one counted unit: 100 cm a block, 1200 ticks a minute. */
    public int unit() {
        String p = stat.getPath();
        if (p.endsWith("_one_cm")) return 100;
        if (p.equals("play_time") || p.equals("play_one_minute") || p.equals("total_world_time") || p.startsWith("time_since_")
            || p.equals("sneak_time")) return 1200;
        return 1;
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Statistic " + stat + " +" + count;
    }

    /** The game's own name of the statistic ("Distance Walked"). */
    private Component name() {
        return Component.translatable("stat." + stat.getNamespace() + "." + stat.getPath());
    }

    private String units() {
        return unit() == 100 ? "_blocks" : unit() == 1200 ? "_minutes" : "";
    }

    @Override
    public Component display() {
        return Msg.tr("justquests.goal.stat_count" + units(), name(), count);
    }

    @Override
    public Component goal() {
        return Msg.tr("justquests.goal.stat" + units(), name());
    }
}
