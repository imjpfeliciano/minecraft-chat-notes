package com.imjpfeliciano.chatnotes.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

/** Ctrl or Cmd is held. 26.2 character events do not carry modifiers. */
final class KeyShortcuts {

    private KeyShortcuts() {}

    static boolean shortcutDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_LSUPER)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RSUPER);
    }
}
