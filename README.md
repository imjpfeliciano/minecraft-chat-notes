# Chat Notes

A client-only Fabric mod for Minecraft **1.21.11**. It keeps a personal library of chat snippets (typically messages with `&`-style color codes generated elsewhere) so you can **Copy** one to the system clipboard or **Use** it, which opens the chat box with the text already filled in, ready for you to review and send.

The mod never needs to be installed on a server, sends nothing over the network by itself, and never modifies the text you stored.

## Status

Phase 4 polish in progress: coloured `&`-code list previews, `§` warnings, and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). See [plans/](plans) for the full roadmap.

## Toolchain

These are the exact versions the project is built against. Check [fabricmc.net/develop](https://fabricmc.net/develop) before bumping any of them.

- Minecraft `1.21.11`
- Fabric Loader `0.18.2`
- Fabric API `0.139.4+1.21.11`
- Fabric Loom `1.14-SNAPSHOT`
- Gradle `9.2.1` (via the wrapper)
- Java `21` (Gradle toolchain; the build does not depend on your default `java`)
- Mappings: **official Mojang mappings** (`loom.officialMojangMappings()`), not Yarn

## Building and running

```bash
./gradlew build      # produces build/libs/chatnotes-<version>.jar
./gradlew runClient  # launches a dev client with the mod loaded
./gradlew test       # runs the unit tests for the storage layer
```

The dev client writes its game files to `run/`, which is git-ignored.

## Layout

```
src/main/java/com/imjpfeliciano/chatnotes/
├── ChatNotesClient.java   # ClientModInitializer: loads store, registers keybind
├── data/                  # Note / Group / NoteStore / NoteStoreFile (no Minecraft imports)
├── input/KeyBindings.java # default K; only place that touches KeyMapping
├── ui/NotesScreen.java    # list, search, Copy / Use / Edit / Delete / reorder
├── ui/NoteEditScreen.java # create / edit a note
└── util/                 # ClipboardUtil, AmpersandFormat (preview)
docs/ARCHITECTURE.md
src/main/resources/
├── fabric.mod.json
└── assets/chatnotes/      # icon + lang/en_us.json
```

No mixins. The mod is built entirely on public Minecraft and Fabric API surfaces.

Ampersand colour codes are previewed in the UI only. Vanilla chat does not colour `&` codes — your multiplayer server’s chat plugin does that after you send.

Notes are stored at `config/chatnotes/notes.json` (under the game directory; for `runClient` that is `run/config/…`).

## License

MIT, see [LICENSE](LICENSE).
