package com.erikedits.justquests.client;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * The item stacks drawn as icons (0.5.3, #26). Some items only get their picture from a property:
 * Automobility's front attachments or automobiles stay blank as a plain item.
 * <ul>
 * <li>A quest's "icon" can carry properties the way /give writes them, such as
 *     {@code automobility:front_attachment[automobility:automobile_part="..."]}.</li>
 * <li>An item that the creative inventory lists only as variants, never plain, is shown as its
 *     first variant, so such icons have a picture without any properties.</li>
 * </ul>
 */
public final class ItemIcons {
    private static final Map<Item, ItemStack> SHOWN = new HashMap<>();
    private static Object level;

    private ItemIcons() {}

    /** A quest's "icon" field as a stack, or empty when it names no known item. */
    public static ItemStack parse(String icon) {
        String text = icon.trim();
        int cut = cut(text);
        if (cut >= 0) {
            ItemStack exact = exact(text);
            if (exact != null && !exact.isEmpty()) return exact;
            text = text.substring(0, cut);
        }
        ResourceLocation id = ResourceLocation.tryParse(text);
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        return item == Items.AIR ? ItemStack.EMPTY : shown(item);
    }

    /** Where the properties start ('[' for components, '{' for NBT in older versions), or -1. */
    private static int cut(String text) {
        int a = text.indexOf('[');
        int b = text.indexOf('{');
        if (a < 0) return b;
        return b < 0 ? a : Math.min(a, b);
    }

    /** The /give text, read by the game's own item parser; null when it can't be read. */
    private static ItemStack exact(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        try {
            StringReader reader = new StringReader(text);
            ItemParser.ItemResult result = new ItemParser(mc.level.registryAccess()).parse(reader);
            // the whole text must be read: anything left is the other version's syntax
            if (reader.canRead()) return null;
            return new ItemStack(result.item(), 1, result.components());
        } catch (Exception e) {
            return null;
        }
    }

    /** The item itself, or its first creative-inventory variant when the plain item is not listed. */
    public static ItemStack shown(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level) {
            SHOWN.clear();
            level = mc.level;
        }
        return SHOWN.computeIfAbsent(item, ItemIcons::find).copy();
    }

    private static ItemStack find(Item item) {
        ItemStack plain = new ItemStack(item);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return plain;
        try {
            CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),
                mc.options.operatorItemsTab().get() && mc.player.canUseGameMasterBlocks(), mc.level.registryAccess());
            ItemStack variant = null;
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (!stack.is(item)) continue;
                    if (ItemStack.isSameItemSameComponents(stack, plain)) return plain;
                    if (variant == null) variant = stack.copyWithCount(1);
                }
            }
            return variant != null ? variant : plain;
        } catch (RuntimeException e) {
            return plain;
        }
    }
}
