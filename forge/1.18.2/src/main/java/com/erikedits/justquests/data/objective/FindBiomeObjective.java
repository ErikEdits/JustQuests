package com.erikedits.justquests.data.objective;

import com.erikedits.justquests.text.Msg;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Stand in a biome (0.6.0). "biome" is an id or a #tag (#minecraft:is_forest is any forest). With a
 * count above 1 every biome counts once: "#minecraft:is_overworld" with "count": 10 means ten
 * different biomes.
 */
public record FindBiomeObjective(String biome, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:find_biome";

    public static final MapCodec<FindBiomeObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.xmap(FindStructureObjective::ns, s -> s).fieldOf("biome").forGetter(FindBiomeObjective::biome),
        Codec.INT.optionalFieldOf("count", 1).forGetter(FindBiomeObjective::count)
    ).apply(instance, FindBiomeObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** Every biome once when more than one is asked for. */
    @Override
    public boolean distinct() {
        return count > 1;
    }

    public boolean accepts(Holder<Biome> holder, ResourceLocation id) {
        if (biome.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(biome.substring(1));
            return tag != null && holder.is(TagKey.create(Registry.BIOME_REGISTRY, tag));
        }
        return biome.equals(id.toString());
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Find " + count + "x " + biome;
    }

    @Override
    public Component display() {
        return count == 1 ? Msg.tr("justquests.goal.find_biome", Msg.biome(biome))
            : Msg.tr("justquests.goal.find_biome_count", count, Msg.biome(biome));
    }

    @Override
    public Component goal() {
        return Msg.tr("justquests.goal.find_biome", Msg.biome(biome));
    }
}
