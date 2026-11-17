package com.imjpfeliciano.chatnotes.mc;

import com.imjpfeliciano.chatnotes.core.render.Translator;
import net.minecraft.network.chat.Component;

public final class MinecraftTranslator implements Translator {

    public static final MinecraftTranslator INSTANCE = new MinecraftTranslator();

    private MinecraftTranslator() {}

    @Override
    public String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}
