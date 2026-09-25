1 question
How do i disable the main quests.

Answered in 0.2.4: there is now a dedicated toggle.

- In-game (OP): `/quest mainquests off` hides the built-in "main" quests; `/quest mainquests on` brings them back; `/quest mainquests` shows the current state. The change saves to the world's `settings.json` and updates everyone live.
- Or edit `settings.json` directly: set `"mainQuests": false` (default `true`), then `/quest reload` (or rejoin).

When off, the quests bundled with the mod are hidden from `/quest list` and the quest book. Your `custom-quests.json` quests and the generated quests are unaffected — so you can turn the built-ins off and run a pack made entirely of your own quests.

(Before 0.2.4 there was no toggle; the only workaround was overriding individual built-in quests by id in `custom-quests.json`, which replaced their content but didn't remove them from the list.)
