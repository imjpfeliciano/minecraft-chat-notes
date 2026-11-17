# Chat Notes

<p align="center">
  <img src="chat-notes-logo.png" alt="Chat Notes logo" width="160" height="160" />
</p>

<p align="center">
  <strong>Personal chat snippet library for Fabric</strong> — save <code>&</code>-coloured messages, copy them, or drop them into chat without auto-sending.
</p>

A client-only Fabric mod for Minecraft **1.21.11**. It keeps a personal library of chat snippets (typically messages with `&`-style color codes generated elsewhere) so you can **Copy** one to the system clipboard or **Use** it, which opens the chat box with the text already filled in, ready for you to review and send.

The mod never needs to be installed on a server, sends nothing over the network by itself, and never modifies the text you stored.

## Status

Phase 5 groups in progress: single-level folders with filter, create/rename/delete, and per-note group assignment. See [plans/](plans) for the full roadmap.

## Screenshots

Capture guide and exact filenames: [media/screenshots/SCREENSHOTS.md](media/screenshots/SCREENSHOTS.md).

| Notes list |
|:---:|
| ![Notes list](media/screenshots/01-notes-list.png) |

| Search / group filter |
|:---:|
| ![Search and filter](media/screenshots/02-search-and-filter.png) |

| Note editor |
|:---:|
| ![Note editor](media/screenshots/03-note-editor.png) |

| Use → chat pre-filled |
|:---:|
| ![Use in chat](media/screenshots/04-use-in-chat.gif) |

| Groups |
|:---:|
| ![Groups screen](media/screenshots/05-groups-screen.png) |

| Copy |
|:---:|
| ![Copy](media/screenshots/06-copy-clipboard.png) |

## Store listing

Ready-to-paste Modrinth / CurseForge / GitHub blurb (EN + ES): [media/STORE_DESCRIPTION.md](media/STORE_DESCRIPTION.md).

**One-liner:** Personal library of chat snippets for Fabric — save `&`-coloured messages, copy them, or drop them into chat without auto-sending.

## Features

- **Use** / **Copy** without auto-send  
- Add, edit, delete, reorder, search  
- Groups + default filter  
- Coloured `&` / hex preview in the UI  
- Global storage (`config/chatnotes/notes.json`) across all worlds/servers on that install  
- UI languages: English, Español (México), Español (España)

## Toolchain

These are the exact versions the project is built against. Check [fabricmc.net/develop](https://fabricmc.net/develop) before bumping any of them.

- Minecraft `1.21.11`
- Fabric Loader `0.18.2`
- Fabric API `0.139.4+1.21.11`
- Fabric Loom `1.14-SNAPSHOT`
- Gradle `9.8.2` (via the wrapper, which provisions its own JDK 25 for the daemon)
- Java `21` (Gradle toolchain; the build does not depend on your default `java`)
- Mappings: **official Mojang mappings** (`loom.officialMojangMappings()`), not Yarn

## Building and running

```bash
./gradlew buildAll                        # builds :core and every version module
./gradlew :core:test                      # version-independent unit tests, no Minecraft needed
./gradlew :fabric-1.21.11:build           # one version module
./gradlew :fabric-1.21.11:runClient       # launches a dev client with the mod loaded
```

Jars land in `versions/<module>/build/libs/`, named `chatnotes-<version>+<minecraft_version>.jar`.
The dev client writes its game files to `versions/<module>/run/`, which is git-ignored.

## Layout

```
chat-notes-logo.png          # project / store logo
media/
├── STORE_DESCRIPTION.md     # Modrinth / CurseForge copy
└── screenshots/             # gallery PNGs (see SCREENSHOTS.md)
core/                        # version-independent logic, unit tested without Minecraft
versions/fabric-1.21.11/
├── build.gradle             # Loom config + per-version toolchain
├── src/main/java/com/imjpfeliciano/chatnotes/
│   ├── ChatNotesClient.java
│   ├── data/
│   ├── input/
│   ├── ui/
│   └── util/
└── src/main/resources/
    ├── fabric.mod.json
    └── assets/chatnotes/    # icon.png + lang (en_us, es_mx, es_es)
docs/ARCHITECTURE.md
```

No mixins. The mod is built entirely on public Minecraft and Fabric API surfaces.

## Languages

UI strings follow the game language (Options → Language):

- `en_us` — English
- `es_mx` — Español (México)
- `es_es` — Español (España)

Stored note titles/text are never translated; only the mod UI is.

Ampersand colour codes are previewed in the UI only. Vanilla chat does not colour `&` codes — your multiplayer server’s chat plugin does that after you send.

Notes are stored **once globally** at `config/chatnotes/notes.json` (under the game directory; for `runClient` that is `run/config/…`). The same library is available on every singleplayer world and multiplayer server — nothing is stored per server.

## License

MIT, see [LICENSE](LICENSE).
