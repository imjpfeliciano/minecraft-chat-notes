package com.imjpfeliciano.chatnotes.fabric;

import com.imjpfeliciano.chatnotes.core.platform.ConfigPaths;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricConfigPaths implements ConfigPaths {

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
