package com.teameway.nowaycore.common.multiblock.port;

/**
 * 多方块端口
 *
 * <p>由端口方块实体实现；控制器成型时扫描结构并收集。物品/流体/能量端口另外实现
 * {@link ItemPort}、{@link FluidPort}、{@link EnergyPort} 以暴露能力。</p>
 */
public interface MultiblockPort {
    /**
     * @return 端口种类
     */
    PortKind portKind();

    /**
     * @return 端口等级
     */
    PortTier tier();

    /**
     * @return 提供的线程增量，非线程仓返回 0
     */
    default int threadsDelta() {
        return this.tier().threads();
    }

    /**
     * @return 提供的并行增量，非并行仓返回 0
     */
    default int parallelsDelta() {
        return this.tier().parallels();
    }
}
