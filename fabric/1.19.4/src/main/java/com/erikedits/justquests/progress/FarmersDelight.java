package com.erikedits.justquests.progress;

import com.erikedits.justquests.JustQuests;
import com.erikedits.justquests.data.objective.CookItemObjective;
import com.erikedits.justquests.data.objective.CraftItemObjective;
import com.erikedits.justquests.data.objective.CutItemObjective;
import com.erikedits.justquests.data.objective.RichSoilObjective;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Farmer's Delight (#22, 0.6.0), built in without depending on it: its blocks are known by their class
 * names and read by reflection, so nothing happens unless it is installed. A player's right-click on a
 * cutting board, cooking pot, feast, stove or placed skillet is looked at again a tick later (what the
 * block did with it); food put on a stove or skillet is watched until it is cooked; a cooking pot's
 * output is watched while its window is open; a skillet in the hand while it is used; and organic
 * compost a player put down until it turns into rich soil (kept in justquests/fd-compost.json).
 */
public final class FarmersDelight {
    private static final String FD = "vectorwing.farmersdelight.";
    private static final int CUT = 0, POT = 1, FEAST = 2, STOVE = 3, SKILLET = 4;

    /** A right-click on a Farmer's Delight block, looked at again a tick later. */
    private record Pending(int kind, BlockPos pos, ItemStack before, ItemStack tool, List<ItemStack> slots, int servings,
                           Map<Item, Integer> inventory, long at) {}

    /** Food a player put on a stove (its slot) or a placed skillet (slot -1), until it is cooked. */
    private record Cooking(String dim, BlockPos pos, int slot, String via, ItemStack raw, long since) {}

    /** The output of an open cooking pot window and how many of that meal the player held. */
    private record PotView(ItemStack output, int held) {}

    private static final Map<UUID, List<Pending>> PENDING = new HashMap<>();
    private static final Map<UUID, List<Cooking>> COOKING = new HashMap<>();
    private static final Map<UUID, Map<Item, Integer>> FLIPPING = new HashMap<>();
    private static final Map<UUID, PotView> POT_OPEN = new HashMap<>();
    /** Dropped food already counted (a stove's food can be seen by two looks). */
    private static final Set<UUID> COUNTED = new HashSet<>();
    /** Organic compost players put down: "dimension|x|y|z" -> player. */
    private static final Map<String, UUID> COMPOST = new HashMap<>();
    private static Path compostFile;
    private static final Map<String, Method> METHODS = new HashMap<>();
    private static boolean failed;

    private FarmersDelight() {}

    // --- what a right-click did ------------------------------------------------------------------

    /** A right-click on a block with the main hand (before the block reacts). */
    public static void clicked(ServerPlayer player, BlockPos pos, Block block, ItemStack held) {
        Level level = player.level;
        BlockEntity be = level.getBlockEntity(pos);
        if (!is(block, "") && (be == null || !is(be, ""))) return;   // not a Farmer's Delight block
        long now = level.getGameTime();
        Pending p = null;
        try {
            if (is(be, "CuttingBoardBlockEntity")) {
                p = new Pending(CUT, pos.immutable(), stack(call(be, "getStoredItem")), held.copy(), null, 0, null, now);
            } else if (is(be, "CookingPotBlockEntity")) {
                p = new Pending(POT, pos.immutable(), stack(call(be, "getMeal")), held.copy(), null, 0, inventory(player), now);
            } else if (stove(be)) {
                p = new Pending(STOVE, pos.immutable(), ItemStack.EMPTY, held.copy(), slots(be), 0, null, now);
            } else if (is(be, "SkilletBlockEntity")) {
                p = new Pending(SKILLET, pos.immutable(), stack(call(be, "getStoredStack")), held.copy(), null, 0, null, now);
            } else if (is(block, "FeastBlock")) {
                p = new Pending(FEAST, pos.immutable(), ItemStack.EMPTY, held.copy(), null, servings(level.getBlockState(pos)),
                    inventory(player), now);
            }
        } catch (Exception e) {
            failed(e);
        }
        if (p != null) PENDING.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(p);
    }

    /** A block a player placed: organic compost is watched until it turns into rich soil. */
    public static void placed(ServerPlayer player, BlockPos pos, Block block) {
        if (!is(block, "OrganicCompostBlock")) return;
        load(player);
        COMPOST.put(key(player.level, pos), player.getUUID());
        save();
    }

    /** Every server tick, per player. */
    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        Level level = player.level;
        long now = level.getGameTime();
        List<Pending> list = PENDING.get(id);
        if (list != null) {
            for (Iterator<Pending> it = list.iterator(); it.hasNext(); ) {
                Pending p = it.next();
                if (now - p.at() < 1) continue;   // a tick later: the block has done its part
                it.remove();
                try {
                    resolve(player, level, p);
                } catch (Exception e) {
                    failed(e);
                }
            }
            if (list.isEmpty()) PENDING.remove(id);
        }
        try {
            flipping(player);
            potWindow(player);
            if (player.tickCount % 10 == 0) cooking(player, level, now);
            if (player.tickCount % 100 == 0) compost(player);
        } catch (Exception e) {
            failed(e);
        }
    }

    private static void resolve(ServerPlayer player, Level level, Pending p) throws Exception {
        BlockEntity be = level.getBlockEntity(p.pos());
        switch (p.kind()) {
            case CUT -> {
                if (!is(be, "CuttingBoardBlockEntity") || p.before().isEmpty()) return;
                ItemStack now = stack(call(be, "getStoredItem"));
                int used = same(now, p.before()) ? p.before().getCount() - now.getCount() : p.before().getCount();
                if (used <= 0) return;
                // what the board made drops next to it; nothing dropped: the player only took the item back
                List<ItemStack> made = dropped(level, p.pos(), 2);
                if (made.isEmpty()) return;
                ItemStack cut = p.before(), tool = p.tool();
                QuestProgressService.advance(player, QuestProgressService.kind(cut),
                    obj -> obj instanceof CutItemObjective c && c.accepts(cut, tool) ? used : 0);
                for (ItemStack r : made) crafted(player, r);
            }
            case POT -> {
                // a meal served into the bowl (or other container) in the hand
                if (p.before().isEmpty() || !is(be, "CookingPotBlockEntity")) return;
                ItemStack now = stack(call(be, "getMeal"));
                int less = p.before().getCount() - (same(now, p.before()) ? now.getCount() : 0);
                int got = gained(p.inventory(), inventory(player)).getOrDefault(p.before().getItem(), 0);
                int n = Math.min(less, got);
                if (n > 0) cooked(player, withCount(p.before(), n), "cooking_pot", true);
            }
            case FEAST -> {
                BlockState state = level.getBlockState(p.pos());
                // the last serving can leave a plate or nothing at all
                int servings = is(state.getBlock(), "FeastBlock") ? servings(state) : 0;
                if (servings >= p.servings()) return;
                for (Map.Entry<Item, Integer> e : gained(p.inventory(), inventory(player)).entrySet()) {
                    cooked(player, new ItemStack(e.getKey(), e.getValue()), "feast", false);
                }
            }
            case STOVE -> {
                if (!stove(be)) return;
                List<ItemStack> now = slots(be);
                for (int i = 0; i < now.size(); i++) {
                    boolean had = i < p.slots().size() && !p.slots().get(i).isEmpty();
                    if (!had && !now.get(i).isEmpty()) watch(player, level, p.pos(), i, "stove", now.get(i));
                }
            }
            case SKILLET -> {
                if (!is(be, "SkilletBlockEntity")) return;
                ItemStack now = stack(call(be, "getStoredStack"));
                if (!now.isEmpty() && (p.before().isEmpty() || now.getCount() > p.before().getCount())) watch(player, level, p.pos(), -1, "skillet", now);
            }
            default -> { }
        }
    }

    // --- cooking that takes time ------------------------------------------------------------------

    private static void watch(ServerPlayer player, Level level, BlockPos pos, int slot, String via, ItemStack raw) {
        List<Cooking> list = COOKING.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        list.removeIf(c -> c.pos().equals(pos) && c.slot() == slot);
        list.add(new Cooking(dim(level), pos, slot, via, raw.copy(), level.getGameTime()));
    }

    /** Twice a second: food that left a stove or skillet the player filled was cooked; it dropped next to it. */
    private static void cooking(ServerPlayer player, Level level, long now) throws Exception {
        List<Cooking> list = COOKING.get(player.getUUID());
        if (list == null) return;
        List<Cooking> next = new ArrayList<>();
        for (Iterator<Cooking> it = list.iterator(); it.hasNext(); ) {
            Cooking c = it.next();
            if (now - c.since() > 20 * 60 * 20) {   // twenty minutes: no longer cooking
                it.remove();
                continue;
            }
            if (!dim(level).equals(c.dim()) || !level.isLoaded(c.pos())) continue;
            BlockEntity be = level.getBlockEntity(c.pos());
            ItemStack in;
            if (c.slot() >= 0) {
                List<ItemStack> s = stove(be) ? slots(be) : List.of();
                in = c.slot() < s.size() ? s.get(c.slot()) : ItemStack.EMPTY;
            } else {
                in = is(be, "SkilletBlockEntity") ? stack(call(be, "getStoredStack")) : ItemStack.EMPTY;
            }
            boolean same = same(in, c.raw());
            if (same && in.getCount() >= c.raw().getCount()) continue;   // still cooking
            for (ItemStack r : dropped(level, c.pos(), 12)) {
                if (!same(r, c.raw())) cooked(player, r, c.via(), false);
            }
            it.remove();
            if (same) next.add(new Cooking(c.dim(), c.pos(), c.slot(), c.via(), in.copy(), now));   // a skillet cooks one at a time
        }
        list.addAll(next);
        if (list.isEmpty()) COOKING.remove(player.getUUID());
    }

    /** A skillet in the hand: what the player got when they stopped using it. */
    private static void flipping(ServerPlayer player) {
        boolean using = player.isUsingItem() && is(player.getUseItem().getItem(), "SkilletItem");
        if (using) {
            FLIPPING.putIfAbsent(player.getUUID(), inventory(player));
            return;
        }
        Map<Item, Integer> before = FLIPPING.remove(player.getUUID());
        if (before == null) return;
        for (Map.Entry<Item, Integer> e : gained(before, inventory(player)).entrySet()) {
            cooked(player, new ItemStack(e.getKey(), e.getValue()), "skillet", false);
        }
    }

    /** An open cooking pot window: meals taken out of its output slot by this player. */
    private static void potWindow(ServerPlayer player) throws Exception {
        AbstractContainerMenu menu = player.containerMenu;
        if (!is(menu, "CookingPotMenu")) {
            POT_OPEN.remove(player.getUUID());
            return;
        }
        int index = outputSlot(menu);
        ItemStack output = menu.getSlot(index).getItem().copy();
        PotView last = POT_OPEN.get(player.getUUID());
        Item meal = last != null && !last.output().isEmpty() ? last.output().getItem() : output.getItem();
        int held = count(player, meal);
        POT_OPEN.put(player.getUUID(), new PotView(output, count(player, output.getItem())));
        if (last == null || last.output().isEmpty()) return;
        int less = last.output().getCount() - (same(output, last.output()) ? output.getCount() : 0);
        int n = Math.min(less, held - last.held());
        if (n > 0) cooked(player, withCount(last.output(), n), "cooking_pot", true);
    }

    /** The pot's output slot: INDEX_OUTPUT, or 8 in older Farmer's Delight versions that have no such name (0.6.1). */
    private static int outputSlot(AbstractContainerMenu menu) {
        return OUTPUT_SLOT.computeIfAbsent(menu.getClass(), c -> {
            try {
                return c.getField("INDEX_OUTPUT").getInt(null);
            } catch (ReflectiveOperationException e) {
                return 8;
            }
        });
    }

    private static final Map<Class<?>, Integer> OUTPUT_SLOT = new HashMap<>();

    // --- rich soil --------------------------------------------------------------------------------

    /** Every five seconds: the player's compost that turned into rich soil. */
    private static void compost(ServerPlayer player) {
        load(player);
        if (COMPOST.isEmpty()) return;
        Level level = player.level;
        String d = dim(level) + "|";
        boolean changed = false;
        int made = 0;
        for (Iterator<Map.Entry<String, UUID>> it = COMPOST.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, UUID> e = it.next();
            if (!e.getValue().equals(player.getUUID()) || !e.getKey().startsWith(d)) continue;
            String[] p = e.getKey().split("\\|");
            BlockPos pos = new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            if (!level.isLoaded(pos)) continue;
            Block block = level.getBlockState(pos).getBlock();
            if (is(block, "OrganicCompostBlock")) continue;   // still composting
            String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
            if (id.startsWith("farmersdelight:rich_soil")) made++;
            it.remove();
            changed = true;
        }
        if (changed) save();
        int n = made;
        if (n > 0) QuestProgressService.advance(player, obj -> obj instanceof RichSoilObjective ? n : 0);
    }

    private static String key(Level level, BlockPos pos) {
        return dim(level) + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    /** Reads justquests/fd-compost.json of the world the first time it is needed. */
    private static void load(ServerPlayer player) {
        if (compostFile != null) return;
        var server = player.server;
        if (server == null) return;
        compostFile = server.getWorldPath(LevelResource.ROOT).resolve("justquests").resolve("fd-compost.json");
        COMPOST.clear();
        try {
            if (Files.exists(compostFile)) {
                JsonObject o = JsonParser.parseString(Files.readString(compostFile)).getAsJsonObject();
                o.entrySet().forEach(e -> COMPOST.put(e.getKey(), UUID.fromString(e.getValue().getAsString())));
            }
        } catch (Exception e) {
            JustQuests.LOG.warn("Could not read {}: {}", compostFile, e.getMessage());
        }
    }

    private static void save() {
        if (compostFile == null) return;
        JsonObject o = new JsonObject();
        COMPOST.forEach((k, v) -> o.addProperty(k, v.toString()));
        try {
            Files.createDirectories(compostFile.getParent());
            com.erikedits.justquests.storage.SafeFiles.write(compostFile, o.toString());
        } catch (Exception e) {
            JustQuests.LOG.warn("Could not write {}: {}", compostFile, e.getMessage());
        }
    }

    // --- counting ---------------------------------------------------------------------------------

    /** cook_item for a meal made the way via says; pot meals count for craft_item too. */
    private static void cooked(ServerPlayer player, ItemStack meal, String via, boolean craft) {
        int n = meal.getCount();
        QuestProgressService.advance(player, QuestProgressService.kind(meal),
            obj -> obj instanceof CookItemObjective c && c.accepts(meal, via) ? n : 0);
        if (craft) crafted(player, meal);
    }

    /** craft_item: what a cutting board or cooking pot made. */
    private static void crafted(ServerPlayer player, ItemStack made) {
        int n = made.getCount();
        QuestProgressService.advance(player, QuestProgressService.kind(made),
            obj -> obj instanceof CraftItemObjective c && c.matches(made) ? n : 0);
    }

    // --- helpers ------------------------------------------------------------------------------------

    /** Item entities that appeared next to the block in the last maxAge ticks, each counted once. */
    private static List<ItemStack> dropped(Level level, BlockPos pos, int maxAge) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5), e -> e.getAge() <= maxAge)) {
            if (!COUNTED.add(e.getUUID())) continue;
            out.add(e.getItem().copy());
        }
        if (COUNTED.size() > 1000) COUNTED.clear();
        return out;
    }

    /** Item -> how many the player has, the stack on the cursor included. */
    private static Map<Item, Integer> inventory(ServerPlayer player) {
        Map<Item, Integer> out = new HashMap<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) out.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) out.merge(carried.getItem(), carried.getCount(), Integer::sum);
        return out;
    }

    private static int count(ServerPlayer player, Item item) {
        return inventory(player).getOrDefault(item, 0);
    }

    private static Map<Item, Integer> gained(Map<Item, Integer> before, Map<Item, Integer> after) {
        Map<Item, Integer> out = new HashMap<>();
        after.forEach((item, n) -> {
            int more = n - before.getOrDefault(item, 0);
            if (more > 0) out.put(item, more);
        });
        return out;
    }

    /** A stove: AbstractStoveBlockEntity since Farmer's Delight 1.3, StoveBlockEntity alone before (0.6.1). */
    private static boolean stove(Object o) {
        return is(o, "AbstractStoveBlockEntity") || is(o, "StoveBlockEntity");
    }

    /** The slots of a stove (an item handler of Farmer's Delight). */
    private static List<ItemStack> slots(Object stove) throws Exception {
        Object items;
        try {
            items = call(stove, "getItems");
        } catch (NoSuchMethodException e) {
            items = call(stove, "getInventory");   // Farmer's Delight before 1.3
        }
        int n = (Integer) call(items, "getSlots");
        List<ItemStack> out = new ArrayList<>();
        Method get = items.getClass().getMethod("getStackInSlot", int.class);
        for (int i = 0; i < n; i++) out.add(((ItemStack) get.invoke(items, i)).copy());
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int servings(BlockState state) throws Exception {
        Object property = call(state.getBlock(), "getServingsProperty");
        return (Integer) state.getValue((net.minecraft.world.level.block.state.properties.Property) property);
    }

    private static String dim(Level level) {
        return String.valueOf(level.dimension().location());
    }

    /** The same item (the version-safe form of ItemStack.isSameItem). */
    private static boolean same(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem();
    }

    private static ItemStack withCount(ItemStack stack, int n) {
        ItemStack out = stack.copy();
        out.setCount(n);
        return out;
    }

    private static ItemStack stack(Object o) {
        return o instanceof ItemStack s ? s.copy() : ItemStack.EMPTY;
    }

    /** Whether the object is (or extends) the Farmer's Delight class of that simple name; "" is any of its classes. */
    private static boolean is(Object o, String simpleName) {
        if (o == null) return false;
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().startsWith(FD) && (simpleName.isEmpty() || c.getSimpleName().equals(simpleName))) return true;
        }
        return false;
    }

    private static Object call(Object o, String name) throws Exception {
        String key = o.getClass().getName() + "#" + name;
        Method m = METHODS.get(key);
        if (m == null) {
            m = o.getClass().getMethod(name);
            METHODS.put(key, m);
        }
        return m.invoke(o);
    }

    private static void failed(Exception e) {
        if (!failed) JustQuests.LOG.warn("Farmer's Delight goals: something did not work as expected", e);
        failed = true;
    }

    /** Server stop. */
    public static void clear() {
        PENDING.clear();
        COOKING.clear();
        FLIPPING.clear();
        POT_OPEN.clear();
        COUNTED.clear();
        COMPOST.clear();
        compostFile = null;
    }
}
