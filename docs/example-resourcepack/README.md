# JustQuests example resource pack

`JustQuests-Example` restyles JustQuests as a brown "Adventure Journal" and uses every resource pack
feature of JustQuests 0.5.0 once. Copy the folder into `.minecraft/resourcepacks/` and turn it on in
Options → Resource Packs.

| What | File |
|---|---|
| The quest book | `textures/gui/quest_book/book.png` |
| Colours and the background behind the book | `textures/gui/quest_book/settings.json` |
| Quest rows and a picture behind group headers | `textures/gui/quest_book/rows/` |
| A gold line between the rows | `textures/gui/quest_book/separator.png` |
| A progress bar in 10 steps | `textures/gui/quest_book/progress/state_1.png` … `state_10.png` |
| All buttons | `textures/gui/buttons/` |
| An animated star symbol | `textures/gui/icons/glyphs/star.png` + `star.png.mcmeta` |
| An icon for the `examples` category | `textures/gui/icons/categories/examples.png` |
| A picture for the quest `justquests:ex_chain_1` | `textures/gui/icons/quests/justquests/ex_chain_1.png` |
| A tracker frame with progress bars | `textures/gui/tracker/tracker.png` + `settings.json` |
| The popups | `textures/gui/popup/` |
| Sounds from the game for accepting, unlocking, claiming and turning pages | `sounds.json` |
| Minecraft's "uniform" font for the book, tracker and popups | `font/book.json` |
| The book's title | `lang/en_us.json`, `lang/de_de.json` |

The category icon and the quest picture fit the quests of the [example datapack](../example-datapack).

`pack.mcmeta` is made for Minecraft 1.21.1. Other versions show a warning but still load the pack.

All folders, settings and sound IDs are explained in the wiki:
[Resource Packs](https://github.com/ErikEdits/JustQuests/wiki/Resource-Packs).
