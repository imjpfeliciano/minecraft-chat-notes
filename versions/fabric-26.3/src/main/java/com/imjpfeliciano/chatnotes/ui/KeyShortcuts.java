package com.imjpfeliciano.chatnotes.ui;

import com.mojang.blaze3d.platform.InputConstants;

/** Ctrl or Cmd is held. 26.3 reads the SDL keyboard state and names Cmd {@code LGUI}. */
final class KeyShortcuts {

    private KeyShortcuts() {}

    static boolean shortcutDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL)
                || InputConstants.isKeyDown(InputConstants.KEY_LGUI)
                || InputConstants.isKeyDown(InputConstants.KEY_RGUI);
    }
}
