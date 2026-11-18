# Store / platform description

Copy-paste for Modrinth, CurseForge, GitHub About, etc. Keep the logo (`chat-notes-logo.png` / mod icon) and the gallery screenshots from `media/screenshots/`.

---

## Short summary (one line)

Personal library of chat snippets for Fabric — save `&`-coloured messages, copy them, or drop them into chat without auto-sending. Supports Minecraft 1.21.11, 26.1, 26.2, and 26.3.

## Short summary (Spanish)

Biblioteca personal de mensajes de chat para Fabric — guarda textos con códigos `&`, cópialos o ábreelos en el chat sin enviarlos solos. Compatible con Minecraft 1.21.11, 26.1, 26.2 y 26.3.

---

## Full description (English)

**Chat Notes** is a **client-only** Fabric mod for Minecraft **1.21.11**, **26.1** (including 26.1.1 and 26.1.2), **26.2**, and **26.3**.

Download the jar that matches your game version. One jar does not cover every version.

Build a personal library of chat snippets — shop ads, kits, announcements, greetings — usually with server-style **`&` colour codes**. Open the notes screen with **K** (rebindable), then:

- **Use** — opens chat with the exact text pre-filled so you can review before sending  
- **Copy** — puts the exact bytes on the system clipboard  
- **Add / Edit / Delete / Reorder** — manage as many notes as you want  
- **Search** — filter by title or text  
- **Groups** — single-level folders, filter the list, set a **default** group filter  
- **Format** — colour and style a message in the editor with the 16 vanilla chat colours, plus bold, italic, underline, strikethrough, obfuscated, and reset. Save stores a normal `&` string, such as `&e&l` for yellow and bold  
- **Coloured preview** — `&a`, `&#RRGGBB`, and similar codes are previewed in the UI  

### Important

- Does **not** need to be installed on the server  
- Never auto-sends chat messages  
- Does not change a note until you press Save. Formatting then stores the usual `&` codes. A note you leave alone stays as it is, including `&#RRGGBB` and `&x`. Line breaks are still collapsed to a space, because chat is single-line  
- Notes are stored **globally** in `config/chatnotes/notes.json` — same library on every world/server on that install  
- Vanilla chat will **not** colour `&` codes; your server’s chat plugin does that after you send  
- Prefer `&` codes over `§` — section-sign in chat can disconnect you (the mod warns if a note contains it)  

### Languages

- English (`en_us`)  
- Español México (`es_mx`)  
- Español España (`es_es`)  

UI follows the game language. Note contents are never translated.

### Requirements

Install the jar built for your Minecraft version, plus Fabric Loader and Fabric API:

| Minecraft | Jar |
| --- | --- |
| 1.21.11 | `ChatNotes-Fabric-mc1.21.11-v0.2.0.jar` |
| 26.1, 26.1.1, 26.1.2 | `ChatNotes-Fabric-mc26.1-v0.2.0.jar` |
| 26.2 | `ChatNotes-Fabric-mc26.2-v0.2.0.jar` |
| 26.3 | `ChatNotes-Fabric-mc26.3-v0.2.0.jar` |

---

## Descripción completa (Español)

**Chat Notes** es un mod de Fabric **solo cliente** para Minecraft **1.21.11**, **26.1** (incluye 26.1.1 y 26.1.2), **26.2** y **26.3**.

Descarga el jar que corresponda a tu versión del juego. Un solo jar no cubre todas las versiones.

Guarda una biblioteca personal de mensajes de chat — anuncios, kits, saludos — casi siempre con códigos de color **`&`**. Abre la pantalla con **K** (se puede cambiar) y luego:

- **Usar** — abre el chat con el texto ya escrito para que lo revises antes de enviarlo  
- **Copiar** — pone el texto exacto en el portapapeles  
- **Agregar / Editar / Eliminar / Reordenar**  
- **Buscar** y **Grupos** (carpetas + filtro + filtro predeterminado)  
- **Formato** — colorea y aplica estilo en el editor con los 16 colores de chat de vanilla, además de negrita, cursiva, subrayado, tachado, ofuscado y restablecer. Al guardar queda un texto `&` normal, como `&e&l` para amarillo y negrita  
- **Vista previa a color** de códigos `&` en la interfaz  

### Importante

- No hace falta instalarlo en el servidor  
- Nunca envía el mensaje solo  
- No cambia una nota hasta que pulsas Guardar. El formato guarda los códigos `&` de siempre. Una nota que no editas se queda igual, incluidos `&#RRGGBB` y `&x`. Los saltos de línea siguen uniéndose en un espacio, porque el chat es de una sola línea  
- Las notas son **globales** (`config/chatnotes/notes.json`) en esa instalación de Minecraft  
- El chat vanilla no colorea `&`; eso lo hace el plugin del servidor al enviar  
- Evita `§` en el chat (puede desconectarte); el mod avisa si una nota lo contiene  

### Idiomas

Inglés, Español (México) y Español (España). Solo traduce la UI, no el contenido de tus notas.

### Requisitos

Instala el jar de tu versión de Minecraft, más Fabric Loader y Fabric API:

| Minecraft | Jar |
| --- | --- |
| 1.21.11 | `ChatNotes-Fabric-mc1.21.11-v0.2.0.jar` |
| 26.1, 26.1.1, 26.1.2 | `ChatNotes-Fabric-mc26.1-v0.2.0.jar` |
| 26.2 | `ChatNotes-Fabric-mc26.2-v0.2.0.jar` |
| 26.3 | `ChatNotes-Fabric-mc26.3-v0.2.0.jar` |
