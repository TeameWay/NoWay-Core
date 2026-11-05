package com.teameway.nowaycore;

import com.teameway.nowaycore.datagen.ModDatagen;
import com.teameway.nowaycore.init.ModCreativeTabs;
import com.teameway.nowaycore.init.ModItems;
import com.teameway.nowaycore.init.ModLevelGen;
import com.teameway.nowaycore.init.ModRecipeSerializers;
import com.teameway.nowaycore.init.ModRecipeTypes;
import com.teameway.nowaycore.init.block.ModBlockEntities;
import com.teameway.nowaycore.init.block.ModBlocks;
import com.teameway.nowaycore.init.multiblock.ModMultiblockControllerBlockEntities;
import com.teameway.nowaycore.init.multiblock.ModMultiblockControllerBlocks;
import com.teameway.nowaycore.init.multiblock.ModMultiblockControllers;
import com.teameway.nowaycore.init.multiblock.ModMultiblockMachines;
import com.teameway.nowaycore.init.multiblock.ModMultiblockPorts;
import com.teameway.nowaycore.init.multiblock.ModMultiblockTypes;
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
    public static NowayRegistrum REGISTRUM = NowayRegistrum.create(NoWayCore.MOD_ID);

    public NoWayCore(IEventBus eventBus, ModContainer modContainer) {
        ModCreativeTabs.register();
        ModItems.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModRecipeTypes.register(eventBus);
        ModRecipeSerializers.register(eventBus);

        // multiblock
        ModMultiblockControllerBlocks.register();
        ModMultiblockControllerBlockEntities.register();
        ModMultiblockControllers.register();
        ModMultiblockMachines.register();
        ModMultiblockPorts.register();
        ModMultiblockTypes.register();

        ModLevelGen.register(eventBus);
        ModDatagen.configureDataGen();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NoWayCore.MOD_ID, path);
    }
}