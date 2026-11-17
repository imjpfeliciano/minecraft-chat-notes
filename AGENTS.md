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
