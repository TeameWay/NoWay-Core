package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多方块结构预览
 *
 * <p>把定义里的每个偏移量映射成具体方块状态，一份写入 LDLib2 的虚拟世界用于渲染，
 * 一份按位置保存为快照用于统计所需方块。两者都来自同一份定义，避免依赖虚拟世界回读：
 * {@link TrackedDummyWorld} 会按代理世界读取方块状态，无代理时读到的是虚拟区块之外的空气，
 * 直接回读会漏掉结构的大多数层。</p>
 *
 * <p>结果按定义 ID 缓存：同一定义的多个界面共用一份虚拟世界，只重建相机的取景范围。</p>
 *
 * <p>注意：定义在 {@code /reload} 后变化时缓存不会自动失效，界面重开时仍显示旧结构。</p>
 */
public class MultiblockPreview {
    private static final Map<Identifier, Structure> STRUCTURES = new LinkedHashMap<>();

    private MultiblockPreview() {
    }

    /**
     * 为该机器的结构定义创建预览场景
     *
     * @param machine 机器类型
     * @return 场景，定义缺失或为空时返回 {@code null}
     */
    public static @Nullable Scene create(MultiblockType machine) {
        Structure structure = structureOf(machine);
        if (structure == null) {
            return null;
        }
        Scene scene = new Scene();
        scene.createScene(structure.world())
            .setTickWorld(true)
            .setRenderedCore(structure.positions())
            .useCacheBuffer()
            .setClipContext(ClipContext.Block.VISUAL, ClipContext.Fluid.SOURCE_ONLY);
        return scene;
    }

    /**
     * 统计结构所需的方块及数量
     *
     * <p>数量按定义里每个偏移量解析出的方块状态逐位置累加，同一个方块合并为一项，
     * 顺序取定义中自上而下的首次出现顺序。</p>
     *
     * @param machine 机器类型
     * @return 方块与数量，定义缺失时返回空映射
     */
    public static Map<Block, Integer> requiredBlocks(MultiblockType machine) {
        Structure structure = structureOf(machine);
        return structure == null ? Map.of() : structure.blocks();
    }

    private static @Nullable Structure structureOf(MultiblockType machine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Identifier definitionId = machine.controller().get().getDefinitionId();
        Structure cached = STRUCTURES.get(definitionId);
        if (cached != null) {
            return cached;
        }
        Structure built = build(definitionId, minecraft);
        if (built != null) {
            STRUCTURES.put(definitionId, built);
        }
        return built;
    }

    private static @Nullable Structure build(Identifier definitionId, Minecraft minecraft) {
        ResourceKey<MultiblockDefinition> key = ResourceKey.create(LibRegistries.DEFINITIONS_KEY, definitionId);
        MultiblockDefinition definition = minecraft.level.registryAccess()
            .lookupOrThrow(LibRegistries.DEFINITIONS_KEY)
            .get(key)
            .map(Holder.Reference::value)
            .orElse(null);
        if (definition == null) {
            return null;
        }

        List<Map.Entry<Vec3i, BlockStatePredicate>> entries = new ArrayList<>(definition.definition().entrySet());
        entries.sort(Comparator
            .comparingInt((Map.Entry<Vec3i, BlockStatePredicate> entry) -> entry.getKey().getY())
            .thenComparingInt(entry -> entry.getKey().getZ())
            .thenComparingInt(entry -> entry.getKey().getX()));

        TrackedDummyWorld world = new TrackedDummyWorld();
        List<BlockPos> positions = new ArrayList<>(entries.size());
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<Vec3i, BlockStatePredicate> entry : entries) {
            List<BlockState> states = entry.getValue().getStatesCache();
            if (states.isEmpty()) {
                continue;
            }
            Vec3i offset = entry.getKey();
            BlockPos pos = new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            world.setBlockAndUpdate(pos, states.getFirst());
            positions.add(pos);
            counts.merge(states.getFirst().getBlock(), 1, Integer::sum);
        }
        if (positions.isEmpty()) {
            return null;
        }
        // 临时诊断
        java.util.Map<Integer, Integer> yHist = new java.util.TreeMap<>();
        int readable = 0;
        int airInWorld = 0;
        for (BlockPos pos : positions) {
            yHist.merge(pos.getY(), 1, Integer::sum);
            BlockState read = world.getBlockState(pos);
            if (read.isAir()) {
                airInWorld++;
            } else {
                readable++;
            }
        }
        com.teameway.nowaycore.NoWayCore.LOGGER.info(
            "JEI structure {}: entries={} positions={} yHist={} readable={} air={}",
            definitionId, entries.size(), positions.size(), yHist, readable, airInWorld
        );
        return new Structure(world, List.copyOf(positions), Collections.unmodifiableMap(counts));
    }

    /**
     * 结构定义解析后的快照，渲染与统计共用同一份结果。
     *
     * @param world     已填充方块的虚拟世界
     * @param positions 全部方块位置
     * @param blocks    方块及数量
     */
    private record Structure(TrackedDummyWorld world, List<BlockPos> positions, Map<Block, Integer> blocks) {
    }
}