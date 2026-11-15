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
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Stand inside a structure (0.6.0): a village, a stronghold, an ancient city. "structure" is an id
 * or a #tag (#minecraft:village is any village). With a count above 1 every structure counts once:
 * "count": 3 means three different villages.
 */
public record FindStructureObjective(String structure, int count) implements QuestObjective {
    public static final String TYPE_ID = "justquests:find_structure";

    public static final MapCodec<FindStructureObjective> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.xmap(FindStructureObjective::ns, s -> s).fieldOf("structure").forGetter(FindStructureObjective::structure),
        Codec.INT.optionalFieldOf("count", 1).forGetter(FindStructureObjective::count)
    ).apply(instance, FindStructureObjective::new));

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    /** Every structure once when more than one is asked for. */
    @Override
    public boolean distinct() {
        return count > 1;
    }

    public boolean accepts(Holder<Structure> holder, ResourceLocation id) {
        if (structure.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(structure.substring(1));
            return tag != null && holder.is(TagKey.create(Registry.STRUCTURE_REGISTRY, tag));
        }
        return structure.equals(id.toString());
    }

    @Override
    public int requiredCount() {
        return count;
    }

    @Override
    public String displayName() {
        return "Find " + count + "x " + structure;
    }

    @Override
    public Component display() {
        return count == 1 ? Msg.tr("justquests.goal.find_structure", Msg.structure(structure))
            : Msg.tr("justquests.goal.find_structure_count", count, Msg.structure(structure));
    }

    @Override
    public Component goal() {
        return Msg.tr("justquests.goal.find_structure", Msg.structure(structure));
    }

    static String ns(String id) {
        boolean tag = id.startsWith("#");
        String bare = tag ? id.substring(1) : id;
        return (tag ? "#" : "") + (bare.indexOf(':') >= 0 ? bare : "minecraft:" + bare);
    }
}
