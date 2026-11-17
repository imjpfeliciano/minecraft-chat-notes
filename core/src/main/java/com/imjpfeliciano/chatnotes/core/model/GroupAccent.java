package com.imjpfeliciano.chatnotes.core.model;

/** Soft folder colours, cycled from the group id so a row keeps its colour across restarts. */
public final class GroupAccent {

    private static final int[] COLORS = {
            0xFF5B8DEF,
            0xFF3FAE7A,
            0xFFE0A03A,
            0xFFC86BDB,
            0xFFE25B5B,
            0xFF4DB6C6
    };

    private GroupAccent() {}

    public static int colorFor(String groupId) {
        int index = groupId == null ? 0 : Math.floorMod(groupId.hashCode(), COLORS.length);
        return COLORS[index];
    }
}
