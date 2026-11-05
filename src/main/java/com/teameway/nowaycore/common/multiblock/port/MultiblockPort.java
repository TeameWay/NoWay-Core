package com.teameway.nowaycore.common.multiblock.port;

/**
 * 多方块端口
 *
 * <p>由端口方块实体实现，成型时被扫描收集；资源端口另实现 {@link ItemPort}、{@link FluidPort}、{@link EnergyPort}。</p>
 */
public interface MultiblockPort {
    PortKind portKind();

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
