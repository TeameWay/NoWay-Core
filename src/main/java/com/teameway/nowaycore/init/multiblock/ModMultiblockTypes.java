package com.teameway.nowaycore.init.multiblock;

import com.teameway.nowaycore.common.multiblock.MultiblockType;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;

import static com.teameway.nowaycore.NoWayCore.REGISTRUM;

/**
 * 机器类型注册
 */
public class ModMultiblockTypes {
    /**
     * 简易处理器类型
     */
    public static final RegistryEntry<MultiblockType, MultiblockType> SIMPLE_PROCESSOR =
        REGISTRUM.multiblockType(
            "simple_processor",
            () -> new MultiblockType(ModMultiblockControllers.SIMPLE_PROCESSOR_CONTROLLER)
        ).register();

    public static void register() {
    }
}
