package com.imjpfeliciano.chatnotes.fabric;

import com.imjpfeliciano.chatnotes.core.platform.ModLog;
import org.slf4j.Logger;

final class Slf4jModLog implements ModLog {

    private final Logger logger;

    Slf4jModLog(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    @Override
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }
}
