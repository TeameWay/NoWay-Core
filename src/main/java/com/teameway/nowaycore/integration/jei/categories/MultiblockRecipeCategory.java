package com.teameway.nowaycore.integration.jei.categories;

import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.integration.jei.layout.JeiLayout;
import com.teameway.nowaycore.integration.jei.layout.RecipeLayout;
import com.teameway.nowaycore.integration.jei.ui.MultiblockRecipeUI;
import com.teameway.nowaycore.recipe.CompiledRecipe;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 多方块配方分类
 *
 * <p>每个机器类型一个分类，标题与图标取自控制器方块；同分类内的配方共用一份 {@link RecipeLayout}，切换配方时列宽、箭头位置与页面高度不变。</p>
 */
public class MultiblockRecipeCategory extends ModularUIRecipeCategory<CompiledRecipe> {
    private final IRecipeType<CompiledRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final RecipeLayout layout;

    /**
     * @param helpers       JEI 助手
     * @param recipeType    该分类的配方类型
     * @param type          机器类型
     * @param sampleRecipes 该分类的配方，用于确定统一排版
     */
    public MultiblockRecipeCategory(
        IJeiHelpers helpers,
        IRecipeType<CompiledRecipe> recipeType,
        MultiblockType type,
        List<CompiledRecipe> sampleRecipes
    ) {
        super(recipe -> MultiblockRecipeUI.create(recipe, MultiblockRecipeUI.layoutFor(sampleRecipes)));
        this.recipeType = recipeType;
        this.layout = MultiblockRecipeUI.layoutFor(sampleRecipes);
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
    public Identifier getIdentifier(CompiledRecipe recipe) {
        return recipe.id();
    }

    @Override
    public int getWidth() {
        return JeiLayout.PANEL_WIDTH;
    }

    @Override
    public int getHeight() {
        return this.layout.height();
    }
}