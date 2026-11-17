# Agent notes

## Version priority

Minecraft **1.21.11 Fabric** is the primary version. It is the one played most.

Do new behaviour there first:

- Editor and screens: `shared/mc121`
- Version module: `versions/fabric-1.21.11`
- Playtest: `./gradlew :fabric-1.21.11:runClient`

When 1.21.11 works, copy the same behaviour to 26.1, 26.2, and 26.3:

- Screens: `shared/mc26` (one tree for all three)
- Modules: `versions/fabric-26.1`, `versions/fabric-26.2`, `versions/fabric-26.3`
- Playtest the copy once with `./gradlew :versions:fabric-26.3:runClient`

Keep version-independent logic in `core/`. It has no Minecraft imports and is covered by `./gradlew :core:test`. Do not start the 26.x screen copy until the 1.21.11 path has been played.

26.1, 26.2, and 26.3 compile the same `shared/mc26` tree, but mouse and keyboard differ. When a change touches either, playtest each client before the next. `unset JAVA_HOME` first so the Gradle daemon is Java 25.

## Formatting toolbar

The played editor is the screens, not the first sketch in `plans/Chat Notes Formatting Toolbar Plan.md`. Two agent rules hold the contract:

- `.cursor/rules/formatting-toolbar.mdc` — palette, `&e&l` order, pristine hex notes, layout, Paste, bold width
- `.cursor/rules/mc26-client-api.mdc` — official wideners, `gui.setScreen`, per-version shortcuts, 26.3 left click is button 1
