package com.imjpfeliciano.chatnotes.core.model;

/**
 * The notes screen's group filter. The store speaks {@code null} (all), {@code ""} (ungrouped)
 * or a group id; the UI speaks the sentinels below so a cycle button can hold them.
 */
public final class GroupFilter {

    /** Sentinel for "show every note". */
    public static final String ALL = "__all__";
    /** Sentinel for notes with no group. */
    public static final String UNGROUPED = "__ungrouped__";

    private GroupFilter() {}

    /** Store uses null/"" /id; UI uses {@link #ALL} / {@link #UNGROUPED} / id. */
    public static String fromStore(String storeFilter) {
        if (storeFilter == null) {
            return ALL;
        }
        if (storeFilter.isEmpty()) {
            return UNGROUPED;
        }
        return storeFilter;
    }

    public static String toStore(String uiFilter) {
        if (ALL.equals(uiFilter)) {
            return null;
        }
        if (UNGROUPED.equals(uiFilter)) {
            return "";
        }
        return uiFilter;
    }

    /** Reordering only makes sense over the full, unfiltered list. */
    public static boolean canReorder(String searchQuery, String uiFilter) {
        return (searchQuery == null || searchQuery.isBlank()) && ALL.equals(uiFilter);
    }

    public static boolean isDefault(String uiFilter, String storeFilter) {
        return uiFilter != null && uiFilter.equals(fromStore(storeFilter));
    }

    /**
     * A filter pointing at a group that no longer exists falls back to {@link #ALL}.
     * {@code groupExists} is ignored for the two sentinels.
     */
    public static String resolve(String uiFilter, boolean groupExists) {
        if (uiFilter == null || ALL.equals(uiFilter) || UNGROUPED.equals(uiFilter)) {
            return uiFilter == null ? ALL : uiFilter;
        }
        return groupExists ? uiFilter : ALL;
    }

    /**
     * The group a note starts in. Editing keeps the note's own group (or ungrouped). Creating
     * from a filtered group view defaults the new note into that group.
     */
    public static String initialGroup(boolean creating, String existingGroupId, String parentFilter) {
        if (!creating && existingGroupId != null && !existingGroupId.isBlank()) {
            return existingGroupId;
        }
        if (creating
                && parentFilter != null
                && !ALL.equals(parentFilter)
                && !UNGROUPED.equals(parentFilter)) {
            return parentFilter;
        }
        return UNGROUPED;
    }

    /** {@code null} clears the group, which is how the store stores "ungrouped". */
    public static String toGroupId(String uiGroup) {
        return UNGROUPED.equals(uiGroup) ? null : uiGroup;
    }
}
