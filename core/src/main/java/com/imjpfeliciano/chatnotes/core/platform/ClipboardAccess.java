package com.imjpfeliciano.chatnotes.core.platform;

/** The system clipboard. Implementations talk to whatever the loader exposes. */
public interface ClipboardAccess {

    String get();

    void set(String text);
}
