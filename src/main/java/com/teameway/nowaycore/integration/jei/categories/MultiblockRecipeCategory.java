package com.teameway.nowaycore.integration.jei.categories;

import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import com.teameway.nowaycore.integration.jei.ui.MultiblockRecipeUI;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * 多方块配方分类
 *
 * <p>每个机器类型一个分类：标题与图标取自该类型的控制器方块，界面由 {@link MultiblockRecipeUI}
 * 按配方现场搭建。结构预览由 {@link MultiblockInfoRecipeCategory} 单独承载，这里只显示配方本身。</p>
 */
public class MultiblockRecipeCategory extends ModularUIRecipeCategory<CompiledRecipe> {
    private final IRecipeType<CompiledRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final int height;

    public MultiblockRecipeCategory(
        IJeiHelpers helpers, IRecipeType<CompiledRecipe> recipeType,
        MultiblockType type, int height
    ) {
        super(recipe -> MultiblockRecipeUI.create(recipe, height));
        this.recipeType = recipeType;
        Block block = type.controller().get().getBlock();
        this.title = Component.translatable(block.getDescriptionId());
        this.icon = helpers.getGuiHelper().createDrawableItemLike(block.asItem());
        this.height = height;
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
    public Identifier getIdentifier(CompiledRecipe recipe) {
        return recipe.id();
    }

    @Override
    public int getWidth() {
        return MultiblockRecipeUI.WIDTH;
    }

    @Override
    public int getHeight() {
        return this.height;
    }
}