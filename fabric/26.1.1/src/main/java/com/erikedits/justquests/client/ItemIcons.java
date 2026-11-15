package com.erikedits.justquests.client;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
        String text = icon.trim(), props = "";
        int cut = cut(text);
        if (cut >= 0) {
            ItemStack exact = exact(text);
            if (exact != null && !exact.isEmpty()) return exact;
            props = text.substring(cut);
            text = text.substring(0, cut);
        }
        Identifier id = Identifier.tryParse(text);
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item == Items.AIR) return ItemStack.EMPTY;
        // the properties could not be read: the creative-inventory variant whose values they name (#26)
        ItemStack like = props.isEmpty() ? null : like(item, props);
        return like != null ? like : shown(item);
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
            ItemStack stack = new ItemParser(mc.level.registryAccess()).parse(reader).createItemStack(1);
            // the whole text must be read: anything left is the other version's syntax
            if (reader.canRead()) {
                failed(text, "not read to the end: " + reader.getRemaining());
                return null;
            }
            return stack;
        } catch (Exception e) {
            failed(text, String.valueOf(e.getMessage()));
            return null;
        }
    }

    private static final java.util.Set<String> FAILED = new java.util.HashSet<>();

    /** Once per icon text: why its properties could not be read (the icon then shows the variant that matches). */
    private static void failed(String text, String why) {
        if (FAILED.add(text)) {
            com.erikedits.justquests.JustQuests.LOG.warn("Quest icon {}: its properties could not be read ({}), showing the creative-inventory variant they name", text, why);
        }
    }

    /** The creative-inventory variant of the item whose properties hold every value the text names, or null (#26). */
    private static ItemStack like(Item item, String props) {
        java.util.List<String> values = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]+)\"|=\\s*([A-Za-z0-9_:./-]+)").matcher(props);
        while (m.find()) values.add(m.group(1) != null ? m.group(1) : m.group(2));
        Minecraft mc = Minecraft.getInstance();
        if (values.isEmpty() || mc.level == null || mc.player == null) return null;
        try {
            CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(),
                mc.options.operatorItemsTab().get() && mc.player.canUseGameMasterBlocks(), mc.level.registryAccess());
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                for (ItemStack stack : tab.getDisplayItems()) {
                    if (!stack.is(item)) continue;
                    String own = String.valueOf(stack.getComponentsPatch());
                    if (values.stream().allMatch(own::contains)) return stack.copyWithCount(1);
                }
            }
        } catch (RuntimeException e) {
            // no creative inventory to look in
        }
        return null;
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
