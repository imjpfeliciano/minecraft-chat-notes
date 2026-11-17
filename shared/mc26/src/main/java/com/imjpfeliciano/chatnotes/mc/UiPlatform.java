package com.imjpfeliciano.chatnotes.mc;

import com.imjpfeliciano.chatnotes.core.platform.ClipboardAccess;
import com.imjpfeliciano.chatnotes.core.platform.ModLog;

/** The platform pieces a screen needs that are not the Minecraft graphics types themselves. */
public record UiPlatform(ClipboardAccess clipboard, ModLog log) {}
