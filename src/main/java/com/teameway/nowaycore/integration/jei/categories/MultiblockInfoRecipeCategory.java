package com.teameway.nowaycore.integration.jei.categories;

import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.init.ModRegistries;
import com.teameway.nowaycore.integration.jei.ui.MultiblockInfoUI;
import com.teameway.nowaycore.integration.jei.ui.MultiblockPreview;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 独立的多方块结构信息分类
 *
 * <p>与配方分类分开，只描述结构本身：预览由 {@link MultiblockInfoUI} 渲染，
 * 所需方块由 {@link MultiblockPreview#requiredBlocks} 统计后写进配方槽位，
 * 这样在 JEI 里查任意一个结构方块都能跳回该结构。</p>
 */
public class MultiblockInfoRecipeCategory extends ModularUIRecipeCategory<MultiblockInfoRecipeCategory.MultiblockInfo> {
    public static final IRecipeType<MultiblockInfo> RECIPE_TYPE = IRecipeType.create(NoWayCore.id("multiblock_info"), MultiblockInfo.class);

    private final @Nullable IDrawable icon;

    public MultiblockInfoRecipeCategory(IJeiHelpers helpers) {
        super(info -> MultiblockInfoUI.create(info.type(), info.blocks(), info.height()));
        this.icon = NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)
            .stream()
            .findFirst()
            .map(entry -> helpers.getGuiHelper()
                .createDrawableItemLike(
                    entry.get()
                        .controller()
                        .get().getBlock()
                        .asItem()
                )
            )
            .orElse(null);
    }

    @Override
    public IRecipeType<MultiblockInfo> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.noway_core.jei.multiblock_info");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public Identifier getIdentifier(MultiblockInfo recipe) {
        return recipe.id();
    }

    @Override
    public int getWidth() {
        return MultiblockInfoUI.WIDTH;
    }

    @Override
    public int getHeight() {
        return NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)
            .stream()
            .findFirst()
            .map(entry -> MultiblockInfoUI.heightFor(MultiblockPreview.requiredBlocks(entry.get())))
            .orElse(MultiblockInfoUI.EMPTY_HEIGHT);
    }

    /**
     * @param id     结构定义 ID
     * @param type   机器类型
     * @param blocks 结构所需方块及数量，按定义顺序排列
     * @param height 该结构界面高度
     */
    public record MultiblockInfo(Identifier id, MultiblockType type, Map<Block, Integer> blocks, int height) {
        /**
         * 按机器类型统计结构所需方块并算出界面高度。
         *
         * @param id   结构定义 ID
         * @param type 机器类型
         * @return 结构信息
         */
        public static MultiblockInfo of(Identifier id, MultiblockType type) {
            Map<Block, Integer> blocks = new LinkedHashMap<>(MultiblockPreview.requiredBlocks(type));
            return new MultiblockInfo(
                id, type, Collections.unmodifiableMap(blocks),
                MultiblockInfoUI.heightFor(blocks)
            );
        }
    }
}
