package com.teameway.nowaycore.integration.jei;

import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/**
 * 多方块配方分类
 *
 * <p>每个机器类型一个分类：标题与图标都取自该类型的控制器方块，界面由
 * {@link MultiblockRecipeUI} 按配方现场搭建。</p>
 */
final class MultiblockRecipeCategory extends ModularUIRecipeCategory<CompiledRecipe> {
    private final IRecipeType<CompiledRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;

    MultiblockRecipeCategory(IJeiHelpers helpers, IRecipeType<CompiledRecipe> recipeType, MultiblockType type) {
        super(recipe -> MultiblockRecipeUI.create(recipe, type));
        this.recipeType = recipeType;
        Block block = type.controller().get().getBlock();
        this.title = Component.translatable(block.getDescriptionId());
        this.icon = helpers.getGuiHelper().createDrawableItemLike(block.asItem());
    }

    @Override
    public IRecipeType<CompiledRecipe> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return MultiblockRecipeUI.WIDTH;
    }

    @Override
    public int getHeight() {
        return MultiblockRecipeUI.HEIGHT;
    }
}
