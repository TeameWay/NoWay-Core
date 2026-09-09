package com.teameway.nowaycore.init;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.levelgen.NeoOverWorldChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class ModLevelGen {
    public static final Identifier NEO_OVERWORLD_CHUNK_GENERATOR_ID = NoWayCore.id("neo_overworld");

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModLevelGen::registerChunkGenerators);
    }

    private static void registerChunkGenerators(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, NEO_OVERWORLD_CHUNK_GENERATOR_ID, () -> NeoOverWorldChunkGenerator.CODEC);
    }
}
