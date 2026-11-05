package com.teameway.nowaycore.integration.jei;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.init.ModRegistries;
import com.teameway.nowaycore.integration.jei.categories.MultiblockInfoRecipeCategory;
import com.teameway.nowaycore.integration.jei.categories.MultiblockRecipeCategory;
import com.teameway.nowaycore.recipe.CompiledRecipe;
import com.teameway.nowaycore.recipe.MultiblockRecipeIndex;
import com.teameway.nowaycore.recipe.TypeIndex;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI 集成
 *
 * <p>分类按机器类型注册；配方取客户端配方索引里的编译结果，与服务端同源，排序与机器选择配方一致。</p>
 */
@JeiPlugin
public class NowayJEIPlugin implements IModPlugin {
    private static final Map<Identifier, IRecipeType<CompiledRecipe>> TYPES = new HashMap<>();

    private static IRecipeType<CompiledRecipe> typeOf(Identifier machine) {
        return TYPES.computeIfAbsent(machine, id -> IRecipeType.create(id, CompiledRecipe.class));
    }

    /**
     * 取某机器类型当前的配方列表，索引尚未建立时返回空列表
     *
     * @param machine 机器类型 ID
     * @return 已按优先级排序的配方
     */
    private static List<CompiledRecipe> recipesOf(Identifier machine) {
        MultiblockRecipeIndex index = MultiblockRecipeIndex.current();
        if (index == null) {
            return List.of();
        }
        TypeIndex type = index.type(machine);
        return type == null ? List.of() : type.recipes();
    }

    @Override
    public Identifier getPluginUid() {
        return NoWayCore.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        registration.addRecipeCategories(new MultiblockInfoRecipeCategory(helpers));
        for (RegistryEntry<MultiblockType, MultiblockType> entry : NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)) {
            List<CompiledRecipe> recipes = recipesOf(entry.getId());
            MultiblockRecipeCategory category = new MultiblockRecipeCategory(
                helpers, typeOf(entry.getId()), entry.get(), recipes
            );
            registration.addRecipeCategories(category);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            MultiblockInfoRecipeCategory.RECIPE_TYPE,
            NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)
                .stream()
                .map(entry -> MultiblockInfoRecipeCategory.MultiblockInfo.of(entry.getId(), entry.get()))
                .toList()
        );
        for (RegistryEntry<MultiblockType, MultiblockType> entry : NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)) {
            List<CompiledRecipe> recipes = recipesOf(entry.getId());
            if (recipes.isEmpty()) {
                continue;
            }
            registration.addRecipes(typeOf(entry.getId()), recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (RegistryEntry<MultiblockType, MultiblockType> entry : NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)) {
            var stack = entry.get().controller().get().getBlock().asItem().getDefaultInstance();
            registration.addCraftingStation(typeOf(entry.getId()), stack);
            registration.addCraftingStation(MultiblockInfoRecipeCategory.RECIPE_TYPE, stack);
        }
    }
}