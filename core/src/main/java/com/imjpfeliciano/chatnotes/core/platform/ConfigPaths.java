package com.imjpfeliciano.chatnotes.core.platform;

import java.nio.file.Path;

/** Where the loader keeps config. Notes live at {@code configDir()/chatnotes/notes.json}. */
public interface ConfigPaths {

    Path configDir();
}
