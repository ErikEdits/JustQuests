# Cross-loader event handling

How JustQuests stays portable across NeoForge, Fabric and Forge — and what
to do when a loader doesn't have an event the mod needs.

## The principle

The objective-tracking core is **loader-agnostic**:
- `QuestProgressService.advance(player, test)` — the increment + completion
  + reward logic. Knows nothing about any loader's events.
- The objective types (`KillMobObjective`, `PlaceBlockObjective`, …) — plain
  data + a type-specific match. No loader API.

Only a thin **event bridge** per loader is loader-specific. On NeoForge
that's `PlayerQuestEvents` (subscribes to NeoForge events and calls
`advance`). For a Fabric port, a parallel bridge subscribes to Fabric's
callbacks and calls the **same** `advance`. So porting = rewrite the
bridge only; the whole quest engine is shared.

## When a loader lacks an event

Some events exist on one loader but not another (or behave differently).
Two fallbacks, both already used here:

1. **Poll instead of subscribe.** If there's no clean event, check the
   condition on a timer. Example in this repo: `reach_level` and
   `reach_location` have no good "level changed"/"moved" event, so the
   tick bridge checks them once a second per player
   (`PlayerTickEvent.Post`, throttled). Tick/loop hooks exist on every
   loader, so this pattern is fully portable.

2. **Detect it in the mod (mixin / direct hook).** If neither a clean
   event nor a pollable state exists, the mod adds its own detection via a
   mixin into the relevant vanilla method, then calls `advance`. This is
   the loader-independent way to "build the event into the mod" — the mod
   recognizes the situation itself instead of relying on the loader.

Rule of thumb: prefer a loader event → else poll → else mixin. Whatever
the source, it ends in the same `QuestProgressService.advance` call, so
behavior stays identical across loaders.

## Status (2026-06-19)

Objective types and how they're currently fed (NeoForge bridge):
- collect_item → ItemEntityPickupEvent
- kill_mob → LivingDeathEvent
- place_block → BlockEvent.EntityPlaceEvent
- craft_item → PlayerEvent.ItemCraftedEvent
- tame_animal → AnimalTameEvent
- gain_advancement → AdvancementEvent.AdvancementEarnEvent
- visit_dimension → PlayerEvent.PlayerChangedDimensionEvent
- reach_level, reach_location → PlayerTickEvent.Post (polled, portable)
- enchant_item → **pending**: no clean cross-version enchant event; will
  be added via poll or mixin (see rule above) rather than a fragile guess.
