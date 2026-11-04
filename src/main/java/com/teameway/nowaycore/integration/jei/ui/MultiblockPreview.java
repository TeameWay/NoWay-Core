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
import org.joml.Vector3f;
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
 * <p>把定义里的每个偏移量映射成具体方块状态：一份写进 LDLib2 的虚拟世界用于渲染，一份按位置
 * 保存成快照用于点击查询。</p>
 *
 * <p>写入虚拟世界时整体抬升 {@value #BASE_Y} 格。虚拟世界按主世界的世界高度建立，负 Y 层的方块
 * 写进去后读回是空气，只有非负层能保留；抬升后所有层都落在可写区间内，视觉上无差别。</p>
 *
 * <p>结果按定义 ID 缓存：同一定义的多个界面共用一份虚拟世界与快照，只重建各自的相机。</p>
 *
 * <p>注意：定义在 {@code /reload} 后变化时缓存不会自动失效。</p>
 */
public class MultiblockPreview {
    /**
     * 写入虚拟世界时的整体 Y 偏移，让控制器落在该高度上。
     */
    private static final int BASE_Y = 64;
    /**
     * 相机取景留白系数，数值越大结构在画面中越小。
     */
    private static final float CAMERA_RANGE = 1.35F;

    private static final Map<Identifier, Structure> STRUCTURES = new LinkedHashMap<>();

    private MultiblockPreview() {
    }

    /**
     * 为该机器的结构定义创建预览场景，相机对准控制器方块
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
            .useCacheBuffer()
            .setClipContext(ClipContext.Block.VISUAL, ClipContext.Fluid.SOURCE_ONLY);
        // autoCamera 会把相机对准包围盒中心，这里改为对准控制器，保证结构以控制器为视觉中心
        scene.setRenderedCore(structure.positions(), null, false);
        scene.setCenter(new Vector3f(0.5F, BASE_Y + 0.5F, 0.5F));
        scene.setZoom(structure.zoom());
        return scene;
    }

    /**
     * 统计结构所需的方块及数量
     *
     * @param machine 机器类型
     * @return 方块与数量，定义缺失时返回空映射
     */
    public static Map<Block, Integer> requiredBlocks(MultiblockType machine) {
        Structure structure = structureOf(machine);
        return structure == null ? Map.of() : structure.blocks();
    }

    /**
     * 查询某个位置上的方块
     *
     * @param machine 机器类型
     * @param pos     场景中的方块位置，由 {@link Scene} 的选中回调给出
     * @return 方块，该位置没有定义时返回 {@code null}
     */
    public static @Nullable Block blockAt(MultiblockType machine, BlockPos pos) {
        Structure structure = structureOf(machine);
        return structure == null ? null : structure.blockAt().get(pos);
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
        Map<BlockPos, Block> blockAt = new LinkedHashMap<>();
        Map<Block, Integer> counts = new LinkedHashMap<>();
        int maxX = 1;
        int maxY = 1;
        int maxZ = 1;
        for (Map.Entry<Vec3i, BlockStatePredicate> entry : entries) {
            List<BlockState> states = entry.getValue().getStatesCache();
            if (states.isEmpty()) {
                continue;
            }
            Vec3i offset = entry.getKey();
            BlockPos pos = new BlockPos(offset.getX(), BASE_Y + offset.getY(), offset.getZ());
            BlockState state = states.getFirst();
            world.setBlockAndUpdate(pos, state);
            positions.add(pos);
            blockAt.put(pos, state.getBlock());
            counts.merge(state.getBlock(), 1, Integer::sum);
            maxX = Math.max(maxX, Math.abs(offset.getX()) + 1);
            maxY = Math.max(maxY, Math.abs(offset.getY()) + 1);
            maxZ = Math.max(maxZ, Math.abs(offset.getZ()) + 1);
        }
        if (positions.isEmpty()) {
            return null;
        }
        // 以最大跨度计算取景，相机对准控制器后结构两侧都在画面内
        float span = Math.max(maxX, Math.max(maxY, maxZ)) * 2.0F;
        float zoom = span * CAMERA_RANGE;
        return new Structure(
            world,
            List.copyOf(positions),
            Collections.unmodifiableMap(blockAt),
            Collections.unmodifiableMap(counts),
            zoom
        );
    }

    /**
     * 结构定义解析后的快照，渲染与点击查询共用同一份结果。
     *
     * @param world     已填充方块的虚拟世界
     * @param positions 全部方块位置，已抬升到虚拟世界的可写高度
     * @param blockAt   位置到方块的映射，键与 {@code positions} 一致
     * @param blocks    方块及数量
     * @param zoom      相机取景范围
     */
    private record Structure(
        TrackedDummyWorld world,
        List<BlockPos> positions,
        Map<BlockPos, Block> blockAt,
        Map<Block, Integer> blocks,
        float zoom
    ) {
    }
}