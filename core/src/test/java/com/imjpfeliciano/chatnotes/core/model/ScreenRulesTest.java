package com.imjpfeliciano.chatnotes.core.model;

import com.imjpfeliciano.chatnotes.core.render.UiColors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenRulesTest {

    @Test
    void filterSentinelsRoundTrip() {
        assertEquals(GroupFilter.ALL, GroupFilter.fromStore(null));
        assertEquals(GroupFilter.UNGROUPED, GroupFilter.fromStore(""));
        assertEquals("group-1", GroupFilter.fromStore("group-1"));

        assertNull(GroupFilter.toStore(GroupFilter.ALL));
        assertEquals("", GroupFilter.toStore(GroupFilter.UNGROUPED));
        assertEquals("group-1", GroupFilter.toStore("group-1"));
    }

    @Test
    void reorderOnlyWhenTheFullListIsShowing() {
        assertTrue(GroupFilter.canReorder("", GroupFilter.ALL));
        assertTrue(GroupFilter.canReorder("   ", GroupFilter.ALL));
        assertFalse(GroupFilter.canReorder("kit", GroupFilter.ALL));
        assertFalse(GroupFilter.canReorder("", GroupFilter.UNGROUPED));
        assertFalse(GroupFilter.canReorder("", "group-1"));
    }

    @Test
    void missingGroupFallsBackToAllAndDefaultIsMarked() {
        assertEquals(GroupFilter.ALL, GroupFilter.resolve("gone", false));
        assertEquals("group-1", GroupFilter.resolve("group-1", true));
        assertEquals(GroupFilter.UNGROUPED, GroupFilter.resolve(GroupFilter.UNGROUPED, false));
        assertTrue(GroupFilter.isDefault(GroupFilter.ALL, null));
        assertTrue(GroupFilter.isDefault("group-1", "group-1"));
        assertFalse(GroupFilter.isDefault(GroupFilter.UNGROUPED, null));
    }

    @Test
    void newNoteInheritsTheFilteredGroupButAnEditKeepsItsOwn() {
        assertEquals("group-1", GroupFilter.initialGroup(true, null, "group-1"));
        assertEquals(GroupFilter.UNGROUPED, GroupFilter.initialGroup(true, null, GroupFilter.ALL));
        assertEquals(GroupFilter.UNGROUPED, GroupFilter.initialGroup(false, null, "group-1"));
        assertEquals("kept", GroupFilter.initialGroup(false, "kept", "group-1"));
        assertNull(GroupFilter.toGroupId(GroupFilter.UNGROUPED));
        assertEquals("group-1", GroupFilter.toGroupId("group-1"));
    }

    @Test
    void softLimitFlipsTheCounterToRed() {
        assertEquals(UiColors.LIGHT_GRAY, NoteRules.counterColor(256));
        assertEquals(UiColors.SOFT_RED, NoteRules.counterColor(257));
        assertFalse(NoteRules.overSoftLimit(256));
        assertTrue(NoteRules.overSoftLimit(257));
    }

    @Test
    void emptyListMessageDependsOnWhetherTheListIsFiltered() {
        assertEquals(NoteRules.EMPTY_KEY, NoteRules.emptyMessageKey("", GroupFilter.ALL));
        assertEquals(NoteRules.NO_MATCHES_KEY, NoteRules.emptyMessageKey("kit", GroupFilter.ALL));
        assertEquals(NoteRules.NO_MATCHES_KEY, NoteRules.emptyMessageKey("", GroupFilter.UNGROUPED));
    }

    @Test
    void pasteReplacesABlankFieldAndAppendsOtherwise() {
        assertEquals("clip", NoteRules.paste("", "clip"));
        assertEquals("clip", NoteRules.paste(null, "clip"));
        assertEquals("have clip", NoteRules.paste("have", "clip"));
        assertEquals("have", NoteRules.paste("have", ""));
    }

    @Test
    void groupBadgeIsTheNameOnlyWhenTheNoteHasALiveGroup() {
        assertNull(NoteRules.groupBadge(null, "Shop"));
        assertNull(NoteRules.groupBadge("  ", "Shop"));
        assertNull(NoteRules.groupBadge("gone", null));
        assertEquals("Shop", NoteRules.groupBadge("id", "Shop"));
    }

    @Test
    void accentColorIsStableForAGroupId() {
        assertEquals(GroupAccent.colorFor("abc"), GroupAccent.colorFor("abc"));
    }
}
