package com.teameway.nowaycore.common.multiblock.port;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * 能量端口
 *
 * <p>方块实体应把同一个 {@link EnergyHandler} 注册到 {@code Capabilities.Energy.BLOCK}。</p>
 */
public interface EnergyPort extends MultiblockPort {
    /**
     * @return 能量处理器
     */
    EnergyHandler energyHandler();
}
