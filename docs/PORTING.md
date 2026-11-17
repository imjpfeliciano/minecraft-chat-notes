# Porting Chat Notes

How to add a Minecraft version, and how to add a loader. The current modules are Fabric for 1.21.11, 26.1, 26.2, and 26.3.

## Add a version

Copy the newest module whose API is closest. For a 26.x version that is `versions/fabric-26.3/`. Leave `build/`, `.gradle/`, and `run/` behind.

Change these four values:

| Place | Key | Example |
|---|---|---|
| `versions/<module>/gradle.properties` | `minecraft_version` | `26.4` |
| `versions/<module>/gradle.properties` | `loader_version` | the loader Fabric lists for that version |
| `versions/<module>/gradle.properties` | `fabric_version` | the Fabric API build for that version |
| `src/main/resources/fabric.mod.json` | `depends.minecraft` | `~26.4`, or a range such as `>=26.1 <26.2` when one jar should cover patch releases |

`gradle.properties` in a subproject is not loaded automatically. The module `build.gradle` reads it. `mod_version` and `mod_name` stay in the root `gradle.properties`; do not give the new module its own copy. A subproject also needs two registrations:

- `include 'versions:<module>'` in the root `settings.gradle`
- `dependsOn ':versions:<module>:build'` on `buildAll`

Loom for the 26.x modules is `id 'net.fabricmc.fabric-loom' version '1.19.4'` written as a literal in the `plugins` block. Keep `loom_version` in the module `gradle.properties` equal to that literal. A 1.21.x module cannot be a subproject of this build: `fabric-loom-remap` 1.14 and `fabric-loom` 1.19 cannot share a buildscript classpath. Add it with `includeBuild`, the way `versions/fabric-1.21.11` is added, and give it its own `settings.gradle` that loads the root `gradle.properties`.

Compile the new module before launching a client:

```bash
./gradlew :versions:fabric-26.4:compileJava
```

Fix a compile break by adding a hook on the abstract screen or on `FabricChatNotes`, then implementing it in the version module. Do not copy `shared/mc26` or `shared/fabric26` into a second tree for one call. The hooks that already exist are:

- `openScreen(Screen)` on each concrete screen
- `chatScreenWith(String)` on the notes screen
- `FabricChatNotes.ScreenDisplay`, passed from `ChatNotesClient`, which is how the keybind shows the first screen

`new KeyMapping(name, InputConstants.KEY_K, KeyMapping.Category.MISC)` is the keybind constructor that compiles on 26.1, 26.2, and 26.3. It picks `KEYSYM` or `KEYBOARD` for the version being compiled.

Language files and `icon.png` are copied with the module. They are not shared yet.

Check the jar name is `ChatNotes-Fabric-mc<minecraft_version>-v<mod_version>.jar` and that `fabric.mod.json` inside it has the plain `mod_version`, without a `v` prefix. Then run `:versions:<module>:runClient` and walk the manual matrix in the port plan.

When the version is meant to ship, add its jar to the expected-file list in `.github/workflows/release.yml` and a row to the build matrix in `.github/workflows/build.yml`.

## Add a loader

A loader module supplies the four things Fabric supplies today:

- the mod entrypoint
- keybind registration
- the config directory (`config/chatnotes/notes.json`)
- a client tick hook that opens the notes screen while a player exists

It recompiles `core/` and `shared/mc26/` (or `shared/mc121/` for a 1.21 loader) as extra source directories. It does not depend on the core jar, and it does not compile `shared/fabric26` or `shared/fabric121`. Those packages are Fabric.

`shared/mc26` must stay free of `import net.fabricmc`. `CoreBoundaryTest` enforces that for `core/`, `shared/mc121`, and `shared/mc26`. A loader-specific call goes in the version module, behind one of the hooks above, rather than into the abstract screens.

No NeoForge or Forge module exists yet. The first one would be `versions/neoforge-26.3`, with its own mod metadata, entrypoint, and the same thin screens the Fabric 26.3 module has, pointed at whatever that loader uses to show a screen.
