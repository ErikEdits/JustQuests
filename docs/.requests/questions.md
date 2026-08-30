1 question
How do i disable the main quests.
Right now there isn't a dedicated toggle for that.

`settings.json` only covers announcement/display stuff (`discordWelcome`, `announceCompletions`, `completionSound`, `completionToast`) — nothing for turning the built-in quests themselves on/off.

You *can* override any built-in quest by putting a custom quest with the same id in `custom-quests.json` (custom always beats built-in on a matching id) — but that only replaces its content, it doesn't remove it from `/quest list`. An empty override gets skipped entirely too, so there's no "blank it out" trick.

TL;DR: fully disabling/hiding built-in quests isn't possible yet — it's planned as part of upcoming permission/difficulty settings, just not shipped. For now the closest thing is overriding the ones you don't want with your own version.
