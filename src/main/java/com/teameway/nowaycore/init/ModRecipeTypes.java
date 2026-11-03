package com.teameway.nowaycore.init;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.recipe.MultiblockRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> TYPE = DeferredRegister.create(Registries.RECIPE_TYPE, NoWayCore.MOD_ID);

    /**
     * 多方块配方类型
     */
    public static final DeferredHolder<RecipeType<?>, RecipeType<MultiblockRecipe>> MULTIBLOCK =
        registerType("multiblock");

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(String name) {
        return TYPE.register(name, RecipeType::simple);
    }

    public static void register(IEventBus bus) {
        TYPE.register(bus);
    }
}
