package com.teameway.nowaycore.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teameway.nowaycore.init.ModRecipeSerializers;
import com.teameway.nowaycore.init.ModRecipeTypes;
import com.teameway.nowaycore.resource.ResourceIngredient;
import com.teameway.nowaycore.resource.ResourceOutput;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 多方块机器配方
 *
 * <p>只描述需要什么、产出什么、耗时多久，匹配由机器侧的索引完成，
 * {@link #matches(RecipeInput, Level)} 恒为 {@code false}。</p>
 *
 * @param machine  机器类型 ID
 * @param variants 允许的形态标签，空表示该类型所有形态
 * @param time     单周期耗时，单位 tick
 * @param priority 同输入多配方时的排序优先级，越大越先
 */
public record MultiblockRecipe(
    Identifier machine,
    List<Identifier> variants,
    int time,
    int priority,
    List<ResourceIngredient> inputs,
    List<ResourceOutput> outputs
) implements Recipe<RecipeInput> {
    public static final MapCodec<MultiblockRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("machine").forGetter(MultiblockRecipe::machine),
        Identifier.CODEC.listOf().optionalFieldOf("variants", List.of()).forGetter(MultiblockRecipe::variants),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(MultiblockRecipe::time),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(MultiblockRecipe::priority),
        ResourceIngredient.CODEC.listOf().fieldOf("inputs").forGetter(MultiblockRecipe::inputs),
        ResourceOutput.CODEC.listOf().fieldOf("outputs").forGetter(MultiblockRecipe::outputs)
    ).apply(instance, MultiblockRecipe::new));

    public static final Codec<MultiblockRecipe> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockRecipe> STREAM_CODEC =
        StreamCodecUtil.codec2Stream(CODEC);

    public MultiblockRecipe {
        variants = List.copyOf(variants);
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        if (time <= 0) {
            throw new IllegalArgumentException("time must be positive");
        }
    }

    /**
     * 机器匹配由索引层完成，这里不参与原版合成匹配。
     *
     * @return 恒为 {@code false}
     */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    /**
     * 不在原版合成界面展示产物，恒返回空物品栈。
     */
    @Override
    public ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    /**
     * 机器配方不进配方书，标记为 special 以跳过原版的可放置检查。
     *
     * @return 恒为 {@code true}
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "multiblock";
    }

    @Override
    public RecipeSerializer<? extends MultiblockRecipe> getSerializer() {
        return ModRecipeSerializers.MULTIBLOCK.get();
    }

    @Override
    public RecipeType<? extends MultiblockRecipe> getType() {
        return ModRecipeTypes.MULTIBLOCK.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.createFromOptionals(NonNullList.create());
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
