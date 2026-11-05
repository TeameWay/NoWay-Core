package com.teameway.nowaycore.common.multiblock.port.block;

import com.teameway.nowaycore.common.multiblock.port.PortKind;
import com.teameway.nowaycore.common.multiblock.port.PortTier;
import com.teameway.nowaycore.init.multiblock.ModMultiblockPorts;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 控制端口方块（并行仓 / 线程仓），不承载资源，只贡献数值
 */
public class ControlPortBlock extends AbstractPortBlock {
    public ControlPortBlock(Properties properties, PortKind portKind, PortTier tier) {
        super(properties, portKind, tier);
    }

    @Override
    protected BlockEntityEntry<? extends BlockEntity> blockEntityEntry() {
        return ModMultiblockPorts.CONTROL_PORT_TYPE;
    }
}
