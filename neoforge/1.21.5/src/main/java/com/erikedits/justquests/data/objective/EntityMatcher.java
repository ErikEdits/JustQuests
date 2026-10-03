package com.erikedits.justquests.data.objective;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Matches an entity type by id (`minecraft:zombie`) or by tag (`#minecraft:skeletons`), for
 * kill_mob, tame_animal and breed_animal. A single id is still checked against the registry when
 * the quest loads.
 */
public interface EntityMatcher {
    boolean matches(EntityType<?> type);

    /** Plain id/tag string, for logs and diagnostics. */
    String label();

    /** Player-facing name; a single type localizes via its vanilla key. */
    Component name();

    Codec<EntityMatcher> CODEC = Codec.either(TagKey.hashedCodec(Registries.ENTITY_TYPE), BuiltInRegistries.ENTITY_TYPE.byNameCodec()).xmap(
        either -> either.map(Tag::new, Single::new),
        matcher -> matcher instanceof Tag t
            ? Either.<TagKey<EntityType<?>>, EntityType<?>>left(t.tag())
            : Either.<TagKey<EntityType<?>>, EntityType<?>>right(((Single) matcher).type()));

    record Single(EntityType<?> type) implements EntityMatcher {
        @Override
        public boolean matches(EntityType<?> t) {
            return t == type;
        }

        @Override
        public String label() {
            return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        }

        @Override
        public Component name() {
            return type.getDescription();
        }
    }

    record Tag(TagKey<EntityType<?>> tag) implements EntityMatcher {
        @Override
        public boolean matches(EntityType<?> t) {
            return t.is(tag);
        }

        @Override
        public String label() {
            return "#" + tag.location();
        }

        @Override
        public Component name() {
            return Component.literal("#" + tag.location());
        }
    }
}
