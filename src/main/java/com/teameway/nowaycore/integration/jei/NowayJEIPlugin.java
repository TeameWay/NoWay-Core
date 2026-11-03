package com.teameway.nowaycore.integration.jei;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import com.teameway.nowaycore.common.recipe.MultiblockRecipeIndex;
import com.teameway.nowaycore.common.recipe.TypeIndex;
import com.teameway.nowaycore.init.ModRegistries;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI 集成
 *
 * <p>分类按机器类型注册；配方直接取客户端配方索引里的编译结果，与服务端同一份数据，
 * 因此参考排序（优先级）与机器实际选择配方的顺序一致。</p>
 */
@JeiPlugin
public final class NowayJEIPlugin implements IModPlugin {
    private static final Map<Identifier, IRecipeType<CompiledRecipe>> TYPES = new HashMap<>();

    private static IRecipeType<CompiledRecipe> typeOf(Identifier machine) {
        return TYPES.computeIfAbsent(machine, id -> IRecipeType.create(id, CompiledRecipe.class));
    }

    @Override
    public Identifier getPluginUid() {
        return NoWayCore.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        for (RegistryEntry<MultiblockType, MultiblockType> entry : NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)) {
            MultiblockRecipeCategory category = new MultiblockRecipeCategory(helpers, typeOf(entry.getId()), entry.get());
            registration.addRecipeCategories(category);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MultiblockRecipeIndex index = MultiblockRecipeIndex.current();
        if (index == null) {
            return;
        }
        for (RegistryEntry<MultiblockType, MultiblockType> entry : NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)) {
            TypeIndex type = index.type(entry.getId());
            if (type == null) {
                continue;
            }
            List<CompiledRecipe> recipes = type.recipes();
            registration.addRecipes(typeOf(entry.getId()), recipes);
        }
    }
}
