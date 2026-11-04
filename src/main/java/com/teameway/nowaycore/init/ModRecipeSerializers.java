package com.teameway.nowaycore.init;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.recipe.MultiblockRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER = DeferredRegister.create(
        Registries.RECIPE_SERIALIZER,
        NoWayCore.MOD_ID
    );

    /**
     * 多方块配方序列化器
     */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MultiblockRecipe>> MULTIBLOCK =
        SERIALIZER.register(
            "multiblock",
            () -> new RecipeSerializer<>(MultiblockRecipe.MAP_CODEC, MultiblockRecipe.STREAM_CODEC)
        );

    public static void register(IEventBus bus) {
        SERIALIZER.register(bus);
    }
}
