package com.erikedits.justquests.progress;

import com.erikedits.justquests.data.objective.BrewPotionObjective;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * brew_potion (0.6.1): only bottles a brewing stand really brewed count. The game's own trigger fires for
 * every bottle taken out, so a potion put in and taken out again, or a water bottle, counted too.
 * <p>
 * While a player has a brewing stand open, its slots are looked at every tick and kept per stand (also
 * while it is closed). A bottle that turned into something else while the ingredient went down was
 * brewed; a bottle the player put in or swapped was not. Brewing that finished while the stand was closed
 * is seen when it is opened again. Works the same on every loader: no mixin into the stand is needed.
 */
public final class Brewing {
    /** What a stand held when it was last looked at: bottles 0-2 and the ingredient (3), and which bottles were brewed. */
    private static final class Seen {
        final ItemStack[] slots = new ItemStack[4];
        final boolean[] brewed = new boolean[3];
    }

    private static final Map<Object, Seen> STANDS = new HashMap<>();

    private Brewing() {}

    /** Every server tick, per player: the brewing stand they have open. */
    public static void tick(ServerPlayer player) {
        Container stand = stand(player);
        if (stand != null) look(stand);
    }

    /**
     * A bottle was taken out of the brewing stand the player has open: whether a brew made it. Each brewed
     * bottle counts once.
     */
    public static boolean taken(ServerPlayer player, ItemStack bottle) {
        Container stand = stand(player);
        Seen seen = stand == null ? null : STANDS.get(key(stand));
        if (seen == null || bottle.isEmpty()) return false;
        boolean brewingNow = used(seen.slots[3], stand.getItem(3));
        for (int i = 0; i < 3; i++) {
            ItemStack was = seen.slots[i];
            if (was == null || was.isEmpty() || same(was, stand.getItem(i))) continue;   // not the slot it came from
            boolean brewed;
            if (samePotion(was, bottle)) {
                brewed = seen.brewed[i];
            } else {
                // the brew finished this very tick and the bottle was taken at once
                brewed = brewingNow;
            }
            seen.brewed[i] = false;
            seen.slots[i] = stand.getItem(i).copy();
            if (brewed) return true;
        }
        return false;
    }

    private static Container stand(ServerPlayer player) {
        return player.containerMenu instanceof BrewingStandMenu menu ? menu.getSlot(0).container : null;
    }

    /** A stand by where it is, so it keeps its state when its chunk is loaded again. */
    private static Object key(Container stand) {
        if (stand instanceof BlockEntity be && be.getLevel() != null) {
            return be.getLevel().dimension().location() + "|" + be.getBlockPos().asLong();
        }
        return stand;
    }

    private static void look(Container stand) {
        Object key = key(stand);
        Seen seen = STANDS.get(key);
        ItemStack[] now = new ItemStack[4];
        for (int i = 0; i < 4; i++) now[i] = stand.getItem(i).copy();
        if (seen == null) {
            seen = new Seen();
            STANDS.put(key, seen);
        } else {
            boolean brewing = used(seen.slots[3], now[3]);
            for (int i = 0; i < 3; i++) {
                ItemStack was = seen.slots[i];
                if (now[i].isEmpty()) seen.brewed[i] = false;
                else if (was != null && !same(was, now[i])) seen.brewed[i] = brewing && !was.isEmpty();
            }
        }
        System.arraycopy(now, 0, seen.slots, 0, 4);
        if (STANDS.size() > 4096) STANDS.clear();   // a safety net: one entry per stand ever opened
    }

    /** Whether the ingredient went down (a brew uses one; what is left can be a bottle). */
    private static boolean used(ItemStack before, ItemStack after) {
        if (before == null || before.isEmpty()) return false;
        return after.isEmpty() || after.getItem() != before.getItem() || after.getCount() < before.getCount();
    }

    /** The same item, potion and count. */
    private static boolean same(ItemStack a, ItemStack b) {
        return a.getCount() == b.getCount() && samePotion(a, b);
    }

    /** The same item with the same potion (a water bottle and a potion of swiftness differ). */
    private static boolean samePotion(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
        return a.getItem() == b.getItem() && Objects.equals(BrewPotionObjective.potionOf(a), BrewPotionObjective.potionOf(b));
    }

    /** Server stop. */
    public static void clear() {
        STANDS.clear();
    }
}
