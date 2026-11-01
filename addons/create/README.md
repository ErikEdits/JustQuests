# JustQuests Create Addon

Quest objectives for Create's machines in JustQuests. What it does for players and pack makers is in
[docs/create-addon-description.md](../../docs/create-addon-description.md).

## How it works

- `JustQuestsCreate` registers one objective type per `Machine` with `QuestObjective.register`, the
  hook JustQuests has had since 0.4.6.
- The mixins report what a machine made to `Machines.made`, which counts it for the player that
  `Owners` finds.
- `Owners` keeps the player who placed a machine in a data attachment on its block entity.
- The mixins are not required and do not stop the game if a target changes in another Create version.
  The objective then simply does not count.

## Building

The addon is a build of its own, so the main build never needs Create.

1. Build the JustQuests jar it compiles against, from the repository root:

   ```bash
   ./gradlew :neoforge-1_21_1:build
   ```

2. Put the Create jar, and the jars inside it (`META-INF/jarjar`), into `addons/create/libs/`. The folder
   is not in git.

3. Build the addon and copy the jar and the changelog to `Desktop/Justquests/create addon/`:

   ```bash
   ./gradlew -p addons/create exportJars
   ```

For a dev run with `runClient` or `runServer`, put the Create and JustQuests jars into
`neoforge/1.21.1/run/client/mods` and `run/server/mods`.
