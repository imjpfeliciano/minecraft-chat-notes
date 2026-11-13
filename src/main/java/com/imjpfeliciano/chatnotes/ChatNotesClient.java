package com.imjpfeliciano.chatnotes;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatNotesClient implements ClientModInitializer {

    public static final String MOD_ID = "chatnotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Chat Notes] Client init complete.");
    }
}
