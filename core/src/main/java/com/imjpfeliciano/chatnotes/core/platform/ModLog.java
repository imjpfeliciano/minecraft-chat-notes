package com.imjpfeliciano.chatnotes.core.platform;

/**
 * Logging with the {@code {}} placeholders the existing calls already use, so a message written
 * for slf4j keeps its shape through the port.
 */
public interface ModLog {

    void info(String message, Object... args);

    void warn(String message, Object... args);

    void debug(String message, Object... args);
}
