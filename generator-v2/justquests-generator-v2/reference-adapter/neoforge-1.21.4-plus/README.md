# Notes — NeoForge / Fabric 1.21.2 – 1.21.11

**REFERENCE ONLY — NOT COMPILED.** No separate files: start from
[`../neoforge-1.21.1/`](../neoforge-1.21.1/) (or [`../fabric-1.21.1/`](../fabric-1.21.1/)) and apply
the changes below. The JustQuests 1.21.4 – 1.21.11 trees already use `BuiltInRegistries.ITEM.getValue(..)`
and `ResourceLocation.parse(..)`, which confirms the first rows. The method names come from Mojang's
mappings of 1.21.2+. Check them against the mappings of the tree you compile.

## Registry lookups

`Registry.get(ResourceLocation)` returns an `Optional<Holder.Reference<T>>` from 1.21.2. The element
itself comes from `getValue`:

```java
private <T extends FeatureElement> boolean enabled(Registry<T> reg, String id) {
    ResourceLocation r = rl(id);
    if (r == null || !reg.containsKey(r)) return false;
    return reg.getValue(r).isEnabled(server.getWorldData().enabledFeatures());
}

// maxStackSize / englishName / isConsumable: BuiltInRegistries.ITEM.getValue(r) instead of .get(r)
```

## Tags

`Registry.getTag(TagKey)` is `Registry.get(TagKey)` from 1.21.2:

```java
Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, r));
```

## Recipes

1.21.2 split recipes into server-side data and client "displays". There is no longer a result item
that works for every recipe type. Answer `UNKNOWN`: the catalog already says which items are crafted
or smelted, and the existence checks keep missing items out.

```java
@Override public TriState hasCraftingRecipe(String itemId) { return TriState.UNKNOWN; }
@Override public TriState hasSmeltingRecipe(String outputItemId) { return TriState.UNKNOWN; }
```

To keep the recipe check anyway, the server's `RecipeManager#recipeMap().byType(RecipeType.CRAFTING)`
returns the `RecipeHolder`s. Read results only for types you know (`ShapedRecipe`, `ShapelessRecipe`,
`AbstractCookingRecipe`), and fall back to `UNKNOWN` for anything else.

## Use animation

`UseAnim` was renamed to `ItemUseAnimation`, and it comes from the stack's `CONSUMABLE` component:

```java
ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(r));
return stack.has(DataComponents.CONSUMABLE) ? TriState.YES : TriState.NO;
```

`TriState.UNKNOWN` is fine as well; food items in the catalog carry their own `consume_item` data.

## Unchanged

Advancements (`AdvancementHolder`), feature flags, world path, seed, dimensions, the store, the
validator (`Quest.CODEC`), the capabilities and the log stay as in the 1.21.1 adapter. The glue class
(`GenV2`) needs no change.

## Checklist after porting

1. The mod's self-test (`INTEGRATION.md` §7) passes: `gen.selfTest()` returns an empty list.
2. `/quest generator status` lists the expected profiles and ends with the running Minecraft version
   and loader (for example `Minecraft 1.21.4 (neoforge)`).
3. On 1.21.2/1.21.3 pale oak never shows up in `/quest generator preview 50`, even in a world with the
   winter-drop experiment enabled, because the catalog's `since: 1.21.4` rejects it. On 1.21.4+ it can
   appear.
