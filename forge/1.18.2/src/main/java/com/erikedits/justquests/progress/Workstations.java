package com.erikedits.justquests.progress;

import com.erikedits.justquests.data.objective.AnvilItemObjective;
import com.erikedits.justquests.data.objective.CraftItemObjective;
import com.erikedits.justquests.data.objective.EnchantItemObjective;
import com.erikedits.justquests.data.objective.GrindstoneItemObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The workstations that raise no statistic and fire no crafting event (0.5.2, #25): what a player
 * takes out of an anvil, a grindstone, a loom or a cartography table.
 * <ul>
 * <li>Anvil and grindstone: the loader reports the take (an event, on Fabric a mixin) while the
 *     items put in still lie there. A take by shift-click has already moved the result into the
 *     inventory then, so it comes from what the menu showed the tick before.</li>
 * <li>Loom and cartography table: a take uses up one of each of the two inputs at once, which a
 *     player moving items by hand can't do. Looked at once a tick while the menu is open.</li>
 * </ul>
 * The smithing table raises "Times Crafted" and counts for craft_item with the stonecutter in
 * {@link StatObjectives}.
 */
public final class Workstations {
    /** What lay in the open menu last tick. */
    private record Seen(AbstractContainerMenu menu, ItemStack first, ItemStack second, ItemStack result) {}

    private static final Map<UUID, Seen> SEEN = new HashMap<>();

    private Workstations() {}

    /** The result slot of a workstation menu, or -1. Its two inputs are slots 0 and 1. */
    private static int resultSlot(AbstractContainerMenu menu) {
        if (menu instanceof AnvilMenu || menu instanceof GrindstoneMenu || menu instanceof CartographyTableMenu) return 2;
        if (menu instanceof LoomMenu) return 3;
        return -1;
    }

    /** Every server tick, per player. */
    public static void tick(ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        int out = resultSlot(menu);
        if (out < 0) {
            SEEN.remove(player.getUUID());
            return;
        }
        ItemStack first = menu.getSlot(0).getItem();
        ItemStack second = menu.getSlot(1).getItem();
        Seen last = SEEN.get(player.getUUID());
        if (last != null && last.menu() == menu && (menu instanceof LoomMenu || menu instanceof CartographyTableMenu)) {
            int takes = Math.min(used(last.first(), first), used(last.second(), second));
            if (takes > 0 && !last.result().isEmpty()) crafted(player, last.result(), takes * last.result().getCount());
        }
        SEEN.put(player.getUUID(), new Seen(menu, first.copy(), second.copy(), menu.getSlot(out).getItem().copy()));
    }

    /** How many of an input went away since last tick (0 if it was swapped for another item). */
    private static int used(ItemStack before, ItemStack now) {
        if (before.isEmpty()) return 0;
        if (now.isEmpty()) return before.getCount();
        return before.sameItem(now) ? Math.max(0, before.getCount() - now.getCount()) : 0;
    }

    /** The result just taken: the one the loader saw, else (moved by shift-click) last tick's. */
    private static ItemStack taken(ServerPlayer player, ItemStack output) {
        if (!output.isEmpty()) return output.copy();
        Seen last = SEEN.get(player.getUUID());
        return last != null && last.menu() == player.containerMenu ? last.result().copy() : ItemStack.EMPTY;
    }

    /** craft_item for a loom or cartography table result. */
    private static void crafted(ServerPlayer player, ItemStack made, int n) {
        QuestProgressService.advance(player, QuestProgressService.kind(made),
            obj -> obj instanceof CraftItemObjective c && c.matches(made) ? n : 0);
    }

    /** An anvil result was taken; left and right still lie in the anvil. */
    public static void anvil(ServerPlayer player, ItemStack left, ItemStack right, ItemStack output) {
        ItemStack result = taken(player, output);
        if (result.isEmpty()) return;
        ItemStack before = left.copy();
        ItemStack with = right.copy();
        QuestProgressService.advance(player, QuestProgressService.kind(result), obj -> {
            if (obj instanceof AnvilItemObjective a) return a.accepts(result, with) ? result.getCount() : 0;
            if (obj instanceof EnchantItemObjective e) {
                return e.anvil() && e.accepts(result) && EnchantItemObjective.raised(before, result) ? 1 : 0;
            }
            return 0;
        });
    }

    /** A grindstone result was taken; top and bottom still lie in the grindstone. */
    public static void grindstone(ServerPlayer player, ItemStack top, ItemStack bottom, ItemStack output) {
        ItemStack result = taken(player, output);
        if (result.isEmpty()) return;
        ItemStack t = top.copy();
        ItemStack b = bottom.copy();
        QuestProgressService.advance(player, QuestProgressService.kind(result),
            obj -> obj instanceof GrindstoneItemObjective g && g.accepts(result, t, b) ? result.getCount() : 0);
    }

    /** The player whose open grindstone holds these items (the loader event doesn't say who took it). */
    public static ServerPlayer grindstoneUser(MinecraftServer server, ItemStack top, ItemStack bottom) {
        if (server == null) return null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof GrindstoneMenu menu
                    && ((!top.isEmpty() && menu.getSlot(0).getItem() == top) || (!bottom.isEmpty() && menu.getSlot(1).getItem() == bottom))) {
                return player;
            }
        }
        return null;
    }

    /** Server stop. */
    public static void clear() {
        SEEN.clear();
    }
}
