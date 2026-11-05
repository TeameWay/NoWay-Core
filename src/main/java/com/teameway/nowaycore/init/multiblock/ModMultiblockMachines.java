package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.MultiblockMachine;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.neoforged.fml.common.EventBusSubscriber;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

/**
 * 多方块机器与控制器的注册
 *
 * <p>控制器进入 {@code noway_core:multiblock_controllers}，机器声明进入 {@code noway_core:multiblocks}，
 * datagen 直接遍历后者输出定义 JSON。</p>
 */
@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class ModMultiblockMachines {

    public static final RegistryEntry<MultiblockMachine, MultiblockMachine> SIMPLE_PROCESSOR = REGISTRUM.multiblock(
        "simple_processor",
        () -> MultiblockMachine.builder(ModMultiblockControllers.SIMPLE_PROCESSOR_CONTROLLER)
            .definition(() -> ModMultiblockDefinitions.SIMPLE_PROCESSOR)
            .build()
    ).register();

    public static void register() {
    }
}
