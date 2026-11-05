package com.teameway.nowaycore.common.multiblock.port;

import com.teameway.nowaycore.NoWayCore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 端口方块注册表
 *
 * <p>登记方块对应的端口种类与等级，供扫描、UI、JEI 与调试命令查询；方块实体仍需实现
 * {@link MultiblockPort} 及对应的资源接口。</p>
 */
public class PortRegistry {
    private static final Map<Block, Descriptor> DESCRIPTORS = new ConcurrentHashMap<>();

    private PortRegistry() {
    }

    /**
     * 注册一个端口方块
     *
     * @param block 方块
     * @param kind  端口种类
     * @param tier  端口等级
     */
    public static void register(Block block, PortKind kind, PortTier tier) {
        Descriptor previous = DESCRIPTORS.put(block, new Descriptor(kind, tier));
        if (previous != null) {
            throw new IllegalStateException("Port block already registered: " + block);
        }
        NoWayCore.LOGGER.debug(
            "Registered multiblock port kind={} tier={} slots={} fluid={} energy={} threads={} parallels={}",
            kind, tier.name(), tier.slots(), tier.fluidCapacity(), tier.energyCapacity(), tier.threads(), tier.parallels()
        );
    }

    /**
     * 查询方块的端口描述
     *
     * @param state 方块状态
     * @return 描述，不是端口时返回 {@code null}
     */
    public static Descriptor descriptor(BlockState state) {
        return DESCRIPTORS.get(state.getBlock());
    }

    /**
     * 判断方块是否为已注册端口
     *
     * @param state 方块状态
     * @return 是否为端口
     */
    public static boolean isPort(BlockState state) {
        return DESCRIPTORS.containsKey(state.getBlock());
    }

    /**
     * 端口描述
     *
     * @param kind 端口种类
     * @param tier 端口等级
     */
    public record Descriptor(PortKind kind, PortTier tier) {
    }
}