package com.teameway.nowaycore.integration.jei;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多方块结构预览
 *
 * <p>把定义里的每个偏移量映射成具体方块状态，塞进 LDLib2 的虚拟世界，再交给 {@link Scene} 渲染。
 * 虚拟世界按定义 ID 缓存：同一定义的多个配方界面共用一份，只重建相机的取景范围。</p>
 *
 * <p>注意：定义在 {@code /reload} 后变化时缓存不会自动失效，界面重开时仍显示旧结构。</p>
 */
final class MultiblockPreview {
    private static final Map<Identifier, TrackedDummyWorld> WORLDS = new HashMap<>();

    private MultiblockPreview() {
    }

    /**
     * 为该机器的结构定义创建预览场景
     *
     * @param machine 机器类型
     * @return 场景，定义缺失或为空时返回 {@code null}
     */
    static @Nullable Scene create(MultiblockType machine) {
        Identifier definitionId = machine.controller().get().getDefinitionId();
        TrackedDummyWorld world = WORLDS.computeIfAbsent(definitionId, MultiblockPreview::buildWorld);
        if (world == null || world.getFilledBlocks().isEmpty()) {
            return null;
        }
        List<BlockPos> blocks = world.getFilledBlocks().longStream().mapToObj(BlockPos::of).toList();
        Scene scene = new Scene();
        scene.createScene(world)
            .setTickWorld(true)
            .setRenderedCore(blocks)
            .useCacheBuffer()
            .setClipContext(ClipContext.Block.VISUAL, ClipContext.Fluid.SOURCE_ONLY);
        return scene;
    }

    private static @Nullable TrackedDummyWorld buildWorld(Identifier definitionId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        ResourceKey<MultiblockDefinition> key = ResourceKey.create(LibRegistries.DEFINITIONS_KEY, definitionId);
        MultiblockDefinition definition = minecraft.level.registryAccess()
            .lookupOrThrow(LibRegistries.DEFINITIONS_KEY)
            .get(key)
            .map(Holder.Reference::value)
            .orElse(null);
        if (definition == null) {
            return null;
        }
        TrackedDummyWorld world = new TrackedDummyWorld();
        for (Map.Entry<Vec3i, ? extends dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate> entry : definition.definition()
            .entrySet()) {
            List<BlockState> states = entry.getValue().getStatesCache();
            if (states.isEmpty()) {
                continue;
            }
            Vec3i offset = entry.getKey();
            world.setBlockAndUpdate(new BlockPos(offset.getX(), offset.getY(), offset.getZ()), states.getFirst());
        }
        return world;
    }
}
