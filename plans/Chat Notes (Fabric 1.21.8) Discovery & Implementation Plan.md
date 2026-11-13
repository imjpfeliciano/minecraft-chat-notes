# Chat Notes (Fabric 1.21.8): Discovery & Implementation Plan

*Working title: "Chat Notes" (mod id `chatnotes`, placeholder, rename freely). Audience: a coding agent that will implement the mod. Status: ready to start Phase 0.*

## 1. Summary

A **client-only Fabric mod for Minecraft 1.21.8 (primary) and 1.21.11 (second target)** that works as an in-game repository of text snippets. The user pastes in messages generated elsewhere (typically colored/formatted chat text), stores as many as they like, and later picks one from a list to **copy to the clipboard** or **drop straight into the chat box** while playing.

The mod never needs to be installed on a server, sends nothing over the network by itself, and does not interpret or modify the stored text.

## 2. Goals and non-goals

**Goals (v1)**

- Create, edit, delete and reorder notes (a note = optional title + message text).
- Add a note by pasting from the system clipboard.
- One-click **Copy** (to system clipboard) and **Use** (open chat with the text pre-filled, ready to send).
- Unlimited notes, persisted between sessions in a plain JSON file.
- Search/filter the list.
- Keybinding to open the notes screen.

**Planned next (v1.x, data model must already support it)**

- Groups (folders) to organise notes.
- Notes panel inside the chat screen itself.
- Import/export of the JSON file.
- Colored preview of the stored text in the list.

**Non-goals for now**

- Generating or editing colored text inside the mod.
- Server-side component, cloud sync, multiplayer sharing.
- Loaders other than Fabric, versions other than 1.21.8 and 1.21.11 (keep code easy to port, see risks).

## 3. Discovery findings

These are the technical facts that shape the design. The agent should re-verify anything marked *(verify)* against the actual 1.21.8 sources and mappings, since class and method names change between versions.

1. **Client-only is straightforward.** Declare `"environment": "client"` in `fabric.mod.json` and register through the `client` entrypoint (`ClientModInitializer`). Loom's split environment source sets (`splitEnvironmentSourceSets()`) are optional but keep client code from touching server classes.
2. **Chat message limit is 256 characters.** Longer text gets truncated in the chat input, and servers reject longer messages. The mod should show a live character counter and a warning above 256, but must still allow saving longer text (the user may paste into other places).
3. **The `§` character cannot be sent in chat.** The vanilla text field strips `§` (section sign) from pasted/typed text, and servers kick for it. So formats that use `§` codes cannot work through chat. Formats that survive are ones that use plain ASCII markers that a *server* then translates, for example `&a`, `&#RRGGBB`, `&x&R&R&G&G&B&B`, or MiniMessage-style `<gradient:...>` tags, depending on the server's plugins. The mod must store text byte-for-byte and never rewrite it. **Confirmed by the user: they currently use only ampersand codes (example: &lVENDO &e&lKIT ZEUS &a&l550K), which are plain ASCII and pass through chat fine.**
4. **Clipboard access is built in.** `MinecraftClient.getInstance().keyboard.setClipboard(String)` and `.getClipboard()` *(verify names)* are enough, with no extra libraries.
5. **Pre-filling chat is easy.** `ChatScreen` has a constructor that takes initial text, so `client.setScreen(new ChatScreen(text))` opens chat with the note already typed. This avoids relying on the user pressing Ctrl+V. *(verify the constructor signature for 1.21.8)*. Sending directly (`networkHandler.sendChatMessage`) is possible but is intentionally **not** a v1 feature: it removes the chance to review the message before it goes out.
6. **Keybindings.** `KeyBindingHelper.registerKeyBinding` (Fabric API). In 1.21.8 the category is still a plain translation-key string; in 1.21.9+ it became a `KeyBinding.Category` object. Isolate this in one class so porting is a one-file change.
7. **UI widgets.** Build a custom `Screen` with a `TextFieldWidget` for search, a scrollable entry list (`ElementListWidget` / `AlwaysSelectedEntryListWidget`, *verify which exists in 1.21.8*), and `ButtonWidget`s. `TextFieldWidget` has a small default max length (32), so call `setMaxLength(...)` explicitly for note text.
8. **Storage location.** `FabricLoader.getInstance().getConfigDir()` gives the config folder. Store `chatnotes/notes.json` there. Gson ships with Minecraft, so no dependency is needed.
9. **Dependencies.** Fabric API only (modules used: key-binding, screen, lifecycle events, and optionally client command). No Cloth Config or Mod Menu in v1 to keep the jar small and the port cost low. Mod Menu integration can be a later optional extra.
10. **Build stack.** Java 21, Gradle wrapper, Fabric Loom, Fabric Loader and Fabric API versions matching 1.21.8. Generate the project from the official Fabric template generator (fabricmc.net/develop) and use the versions it proposes rather than hard-coding any from this document.

**Version targets.** 1.21.8 is the primary target and the user also wants 1.21.11. Both are obfuscated Minecraft versions, and 1.21.11 is the last one: Fabric stops updating Yarn after it and recommends moving to Mojang Mappings (see [Fabric's 1.21.11 announcement](https://fabricmc.net/2025/12/05/12111.html)). So use **Mojang mappings** (optionally layered with Parchment for parameter names) for both targets, which gives one set of names for both versions and an easier move to 26.1 later. Use the Loom, Loader and Fabric API versions the template generator proposes for each Minecraft version. Class names in this section are written Yarn-style; the Mojang equivalents are `Minecraft` (MinecraftClient), `EditBox` (TextFieldWidget), `Button` (ButtonWidget) and `ContainerObjectSelectionList` (ElementListWidget). Verify the rest. Expect API differences between the two targets, notably key binding categories and screen input handling, which changed after 1.21.8 *(verify)*. Keep those differences inside `KeyBindings`, `ClipboardUtil` and `ui/` so the data layer is shared.

## 4. Key decisions (defaults the agent should follow)

| Topic | Decision | Reason |
| --- | --- | --- |
| Mappings | Mojang mappings for both 1.21.8 and 1.21.11 | Yarn ends at 1.21.11 and Fabric recommends Mojang mappings |
| Storage | Single JSON file in the config dir, global (not per world/server) | Notes are personal and reusable everywhere |
| "Use" action | Pre-fill chat input, never auto-send | Safety and user control |
| Text handling | Store exactly as pasted; only normalise line breaks to a single space or reject them | Chat is single-line; avoid corrupting codes |
| Grouping | Schema supports it from day one (`groupId`), UI arrives in Phase 3 | Avoids a migration later |
| Dependencies | Fabric API only | Simplicity |
| License | MIT unless the user says otherwise | Common for Fabric mods |

## 5. Architecture

Suggested package layout (replace `dev.example` with the real group id):

- `ChatNotesClient`: `ClientModInitializer`. Loads the store, registers the keybinding and tick handler.
- `data/Note`, `data/Group`, `data/NoteStore`: model plus load/save/CRUD API. No Minecraft UI imports here, so it can be unit tested.
- `data/NoteStoreFile`: JSON (de)serialisation, schema versioning, atomic writes.
- `ui/NotesScreen`: main list, search box, buttons (Add, Edit, Delete, Copy, Use).
- `ui/NoteEditScreen`: title field, text field, "Paste from clipboard" button, char counter, Save/Cancel.
- `ui/ConfirmDeleteScreen` (or vanilla `ConfirmScreen`).
- `input/KeyBindings`: the only class that touches key binding registration (port isolation).
- `util/ClipboardUtil`: thin wrapper over the Minecraft clipboard calls.

**Persistence rules**

- Load once at client start, keep in memory, save after every mutation.
- Write to a temp file then atomically move over `notes.json`; keep one `notes.json.bak` copy.
- If the file is corrupt, rename it to `notes.corrupt-<timestamp>.json`, start empty, and log a warning. Never silently discard data.
- Do file IO off the render thread if it ever becomes noticeable; for typical sizes a synchronous save is acceptable.

## 6. Data model (schema version 1)

```json
{
  "schemaVersion": 1,
  "groups": [
    { "id": "uuid", "name": "Shop ads", "order": 0 }
  ],
  "notes": [
    {
      "id": "uuid",
      "title": "Welcome message",
      "text": "&#FF8800Hello &#00FFAAworld",
      "groupId": null,
      "order": 0,
      "createdAt": "2026-11-13T12:00:00Z",
      "updatedAt": "2026-11-13T12:00:00Z"
    }
  ]
}
```

- `title` is optional; if empty, the list shows a truncated preview of `text`.
- `groupId` is nullable. v1 UI may ignore it, but it must round-trip through load/save.
- Unknown JSON fields must be preserved or ignored without crashing (forward compatibility).

## 7. UX specification

**Notes screen** (opened by keybinding, default `K`, rebindable)

- Top: search field (filters by title and text, case-insensitive) and an **Add note** button.
- Body: scrollable list. Each row shows the title (or preview), a dimmed one-line preview of the text, and the char count if over 256 (in red).
- Selecting a row enables: **Use** (pre-fill chat), **Copy**, **Edit**, **Delete**, **Move up/down**.
- Double-click a row = **Use**.
- Esc closes the screen.

**Note editor**

- Title field (max \~64 chars), text field (large max length, e.g. 4096), live counter `n / 256`.
- **Paste from clipboard** button fills the text field from the system clipboard. Normal Ctrl+V in the field must also work.
- Save is disabled when text is empty.

**Localisation**

- All strings through translation keys; ship `en_us.json`. Spanish (`es_mx`) can be added later.

## 8. Implementation plan

### Phase 0: Project bootstrap

- Generate the Fabric project for 1.21.8 (Java 21, Mojang mappings, Fabric API), client-only environment.
- Replace sample mixins with none. This mod should need **zero mixins** in v1.
- Confirm `./gradlew runClient` launches, and `./gradlew build` produces a jar.
- Add README, LICENSE, `.gitignore`, and a basic GitHub Actions build workflow (optional).

**Done when:** the dev client starts with the mod listed and a log line from the initializer.

### Phase 1: Storage layer (no UI)

- Implement `Note`, `Group`, `NoteStore`, JSON file handling, atomic save, corrupt-file recovery.
- Unit tests (JUnit) for: round trip, unknown fields, corrupt file, ordering, CRUD.

**Done when:** tests pass with `./gradlew test` and no Minecraft classes are imported in `data/`.

### Phase 2: MVP UI

- Keybinding that opens `NotesScreen`.
- List, search, add/edit/delete, reorder.
- **Copy** and **Use** actions, char counter, paste button.
- Translation file.

**Done when:** the acceptance checklist in section 9 passes in a real client, in singleplayer and on a multiplayer server.

### Phase 3: Groups

- Group management (create, rename, delete with a choice: move notes to "Ungrouped" or delete them).
- Group filter tabs or collapsible sections in the list; move a note to a group from the editor.
- Keep it single-level (no nested groups) unless the user asks.

### Phase 4: Chat integration and polish

- Button or side panel inside `ChatScreen` (via Fabric `ScreenEvents.AFTER_INIT`) to browse notes and insert into the chat field without leaving chat. This may require an accessor mixin or class tweaker (access widener) for the chat input field; keep it isolated in its own class.
- Import/export JSON.
- Colored preview, which can be pulled forward into Phase 2 because the user's format is plain ampersand codes, rendering `&` codes and hex codes in the list.
- Optional Mod Menu entry.

### Phase 4.5: Port to 1.21.11

- Add 1.21.11 as a second build target (Gradle property set or branch; decide at the start of this phase based on how many files differ).
- Fix API differences only in `KeyBindings`, `ClipboardUtil` and `ui/`.
- Re-run the Phase 2 acceptance checklist on 1.21.11.
- Publish one jar per Minecraft version.

### Phase 5: Release

- Mod icon, `fabric.mod.json` metadata, changelog, Modrinth/CurseForge description.
- Port checklist for future Minecraft versions (see risks).

## 9. Acceptance checklist (Phase 2)

1. Pressing the keybinding in-world opens the notes screen; Esc closes it.
2. A note pasted from the system clipboard is saved, appears in the list, and survives a full game restart.
3. **Copy** puts the exact original text on the clipboard (verify by pasting into an external app, character for character).
4. **Use** opens chat with the exact text pre-filled and does not send it.
5. Text containing `&`, `#`, `<`, `>` and emoji/unicode round-trips unchanged. A `§` in input is either stored and flagged with a visible warning, or rejected with a clear message (agent proposes, user confirms).
6. 500+ notes: list scrolls smoothly, search responds instantly.
7. Deleting a note asks for confirmation.
8. Corrupting `notes.json` by hand does not crash the game and the bad file is preserved.
9. The mod loads on a client with no server-side counterpart and on a dedicated server is not required.

## 10. Risks and mitigations

- **`§` stripping** (section 3): users may expect `§` formats to work. Mitigate with a clear warning and docs; confirm the user's actual format first.
- **Version churn:** Minecraft's UI and key binding APIs change between releases (for example key binding categories in 1.21.9). Mitigate by isolating version-sensitive code in `KeyBindings`, `ClipboardUtil` and `ui/`.
- **Chat length limit:** 256 chars may be too short for some generated messages. Mitigate with the counter and warning; the mod cannot lift this limit.
- **Server rules:** some servers limit chat spam or formatting; the mod never auto-sends, so responsibility stays with the user.
- **Data loss:** mitigated by atomic writes, backup file, and corrupt-file preservation.

## 11. Open questions for the product owner

The agent should proceed with the defaults in brackets and flag any assumption it makes in the README.

1. Answered by the user: their tool outputs ampersand codes such as &l, &e and &a. Remaining question: should other formats be handled later (`&a`, `&#RRGGBB`, `&x&...`, MiniMessage tags, `§`)? \[store verbatim, warn on `§`\]
2. Preferred mod name and mod id? \[`Chat Notes` / `chatnotes`\]
3. Default keybinding? \[`K`, rebindable\]
4. Should a note be allowed to contain multiple lines? \[no: collapse line breaks to a single space\]
5. Should groups be single-level folders or free-form tags? \[single-level folders\]
6. Distribution target (private use, Modrinth, CurseForge) and license? \[MIT, undecided\]

## 12. Instructions for the implementing agent

- Work phase by phase and stop for review at the end of each phase; do not start Phase 3+ before Phase 2 is accepted.
- Prefer no mixins and no extra dependencies. If one is needed, explain why in the PR/commit message.
- Keep Minecraft-independent logic (`data/`) free of Minecraft imports and covered by tests.
- Verify every API name marked *(verify)* against the 1.21.8 mappings before relying on it; do not guess signatures.
- Use the versions proposed by the Fabric template generator for Loom, Loader and Fabric API (mappings are Mojang, see section 4), and record them in the README.
- Never auto-send chat messages and never modify stored text.
- Report back after each phase with: what was built, what was tested, deviations from this plan, and open questions.

## 13. Reference material

The official [Fabric developer guides for 1.21.11](https://docs.fabricmc.net/1.21.11/develop/) are the agent's main reference. They are written for 1.21.11, so anything used for the 1.21.8 build must be checked against 1.21.8 sources and mappings.

| Need | Page |
| --- | --- |
| Project setup | [Creating a Project](https://docs.fabricmc.net/1.21.11/develop/getting-started/creating-a-project), [Project Structure](https://docs.fabricmc.net/1.21.11/develop/getting-started/project-structure), [Launching the Game](https://docs.fabricmc.net/1.21.11/develop/getting-started/launching-the-game) |
| Notes screen and editor | [Custom Screens](https://docs.fabricmc.net/1.21.11/develop/rendering/gui/custom-screens), [Custom Widgets](https://docs.fabricmc.net/1.21.11/develop/rendering/gui/custom-widgets), [Drawing to the GUI](https://docs.fabricmc.net/1.21.11/develop/rendering/gui-graphics) |
| Keybinding | [Key Mappings](https://docs.fabricmc.net/1.21.11/develop/key-mappings) |
| Optional client command (for example `/notes`) | [Creating Commands](https://docs.fabricmc.net/1.21.11/develop/commands/basics) |
| Screen and lifecycle events (Phase 4 chat button) | [Events](https://docs.fabricmc.net/1.21.11/develop/events) |
| Translation keys | [Text and Translations](https://docs.fabricmc.net/1.21.11/develop/text-and-translations) |
| Access to protected fields (Phase 4) | [Class Tweakers](https://docs.fabricmc.net/1.21.11/develop/class-tweakers/), [Access Widening](https://docs.fabricmc.net/1.21.11/develop/class-tweakers/access-widening) |
| Tests | [Automated Testing](https://docs.fabricmc.net/1.21.11/develop/automatic-testing) |
| Mappings and porting | [Porting to 1.21.11](https://docs.fabricmc.net/1.21.11/develop/porting/), [Migrating Mappings](https://docs.fabricmc.net/1.21.11/develop/porting/mappings/) |
| Working example mod | [ExampleMod source for 1.21.11](https://github.com/FabricMC/fabric-docs/tree/main/reference/1.21.11) |

The docs site also lists newer Minecraft versions (26.1.2 and 26.2). They are out of scope for this plan.
