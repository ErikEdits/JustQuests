package com.erikedits.justquests.data.objective;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Matches a block by id (`minecraft:stone`) or by tag (`#minecraft:logs`), for mine_block and
 * place_block. A single id is still checked against the registry when the quest loads.
 */
public interface BlockMatcher {
    boolean matches(Block block);

    /** Plain id/tag string, for logs and diagnostics. */
    String label();

    /** Player-facing name; a single block localizes via its vanilla key. */
    Component name();

    Codec<BlockMatcher> CODEC = Codec.either(TagKey.hashedCodec(Registry.BLOCK_REGISTRY), Registry.BLOCK.byNameCodec()).xmap(
        either -> either.map(Tag::new, Single::new),
        matcher -> matcher instanceof Tag t
            ? Either.<TagKey<Block>, Block>left(t.tag()) : Either.<TagKey<Block>, Block>right(((Single) matcher).block()));

    record Single(Block block) implements BlockMatcher {
        @Override
        public boolean matches(Block b) {
            return b == block;
        }

        @Override
        public String label() {
            return Registry.BLOCK.getKey(block).toString();
        }

        @Override
        public Component name() {
            return block.getName();
        }
    }

    record Tag(TagKey<Block> tag) implements BlockMatcher {
        @Override
        public boolean matches(Block b) {
            return b.defaultBlockState().is(tag);
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
