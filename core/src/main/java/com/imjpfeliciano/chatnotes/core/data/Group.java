package com.imjpfeliciano.chatnotes.core.data;

import java.util.UUID;

/**
 * A single-level folder. The UI for groups arrives in a later phase, but they are part of the
 * schema from day one so no file migration is ever needed.
 */
public record Group(String id, String name, int order) {

    public Group {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Group id must not be blank");
        }
        name = name == null ? "" : name;
    }

    public static Group create(String name, int order) {
        return new Group(UUID.randomUUID().toString(), name == null ? "" : name.trim(), order);
    }

    public Group withName(String name) {
        return new Group(id, name == null ? "" : name.trim(), order);
    }

    public Group withOrder(int order) {
        return new Group(id, name, order);
    }
}
