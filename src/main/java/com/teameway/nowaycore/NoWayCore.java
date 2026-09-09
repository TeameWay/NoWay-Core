package com.teameway.nowaycore;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NoWayCore.MOD_ID)
public class NoWayCore {
    public static final String MOD_ID = "noway_core";
    public static final String MOD_NAME = "Noway Core";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public NoWayCore(IEventBus modBus, ModContainer modContainer) {

    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NoWayCore.MOD_ID, path);
    }
}
