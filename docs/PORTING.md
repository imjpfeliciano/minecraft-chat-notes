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

No NeoForge or Forge module exists yet. `shared/mc26` is ready for one: `CoreBoundaryTest` fails if that tree, or `shared/mc121`, contains `net.fabricmc` or `net.neoforged` anywhere in a source file. A loader module recompiles `core/` and `shared/mc26/` as extra source directories. It does not depend on the core jar, and it does not compile `shared/fabric26`.

`versions/neoforge-26.3` would supply only the loader-specific pieces. Everything else is already shared.

| Fabric class today | What NeoForge 26.3 supplies instead | Shared type it sits on |
|---|---|---|
| `ChatNotesClient` | The NeoForge client entrypoint. It opens the store, seeds the demo note when the file is empty, and registers the keybind. `FabricChatNotes.start` is not reusable. | `NoteStore.open` |
| `FabricConfigPaths` | The NeoForge config directory, still resolving `chatnotes/notes.json` under it. | `ConfigPaths` |
| `Slf4jModLog` | The NeoForge logger, with the same `{}` placeholders. | `ModLog` |
| `KeyBindings` | Key registration and an in-world client tick. The default stays **K** under Miscellaneous. The tick still ignores the title screen, where `player` is null. | none; this stays in the loader module |
| `ChatNotesClient`'s screen lambda | `minecraft.gui.setScreen`, the same call Fabric 26.3 uses. | `ScreenDisplay` is the Fabric name for this hook; the NeoForge entrypoint calls the vanilla method directly |
| `fabric.mod.json`, lang, icon | `neoforge.mods.toml` plus a copy of the lang files and icon. The mod id stays `chatnotes`. | none |

`MinecraftClipboard` already implements `ClipboardAccess` over `Minecraft.keyboardHandler`, so the NeoForge module uses that class as it stands.

The abstract screens in `shared/mc26` are reused unchanged. The five concrete screens are a copy of `versions/fabric-26.3`, with one edit: `NotesScreen`, `NoteEditScreen`, and `GroupsScreen` currently obtain `UiPlatform` from `FabricChatNotes.platform()`. The NeoForge copies call whatever holds that platform for NeoForge. `openScreen` stays `minecraft.gui.setScreen`, and `chatScreenWith` stays `new ChatScreen(text, false)`.

That is the whole module. Do not add it until a NeoForge toolchain for 26.3 is chosen.
