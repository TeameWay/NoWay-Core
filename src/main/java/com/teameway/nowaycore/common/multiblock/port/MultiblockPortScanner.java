package com.teameway.nowaycore.common.multiblock.port;

import com.teameway.nowaycore.NoWayCore;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 端口扫描
 *
 * <p>只在成型/部件变化时调用，不要每 tick 扫。</p>
 */
public class MultiblockPortScanner {
    private MultiblockPortScanner() {
    }

    /**
     * 扫描结构中所有端口
     *
     * @param level         世界
     * @param controllerPos 控制器位置
     * @param definition    结构定义
     * @return 端口表
     */
    public static MultiblockPortTable scan(Level level, BlockPos controllerPos, MultiblockDefinition definition) {
        List<MultiblockPort> ports = new ArrayList<>();
        for (BlockPos pos : definition.toGlobal(controllerPos).keySet()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MultiblockPort port) {
                ports.add(port);
            }
        }
        MultiblockPortTable table = MultiblockPortTable.of(ports);
        NoWayCore.LOGGER.debug(
            "Scanned multiblock at {}: {} port(s), {} thread(s), {} parallel(s)",
            controllerPos, table.portCount(), table.threads(), table.parallels()
        );
        return table;
    }
}