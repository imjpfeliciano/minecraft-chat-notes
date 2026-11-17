package com.imjpfeliciano.chatnotes.core.render;

/** Resolves a translation key against the language the game is currently showing. */
public interface Translator {

    String tr(String key, Object... args);
}
