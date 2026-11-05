package com.teameway.nowaycore.recipe.view;

import com.teameway.nowaycore.recipe.CompiledRecipe;
import com.teameway.nowaycore.recipe.MultiblockRecipeIndex;
import com.teameway.nowaycore.recipe.TypeIndex;
import com.teameway.nowaycore.resource.ResourceIngredient;
import com.teameway.nowaycore.resource.ResourceOutput;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方展示数据
 *
 * <p>把 {@link CompiledRecipe} 的多态输入输出展平成界面可用的列表。数量保留 {@code long}，
 * 堆栈只用来取名称与图标，真实数值由 {@link ItemEntry#amount()} 给出。</p>
 *
 * @param energy 单次执行消耗的能量，单位 FE
 * @param time   单周期耗时，单位 tick
 */
public record RecipeView(
    List<ItemEntry> itemInputs,
    List<FluidEntry> fluidInputs,
    List<ItemEntry> itemOutputs,
    List<FluidEntry> fluidOutputs,
    long energy,
    int time
) {
    /**
     * 按机器类型与配方 ID 查找配方
     *
     * <p>客户端索引在配方同步之后才建立，未就绪时返回 {@code null} 由调用方处理。</p>
     *
     * @param machine  机器类型 ID，未成型时为空串
     * @param recipeId 配方 ID
     * @return 展示数据，索引或配方不存在时返回 {@code null}
     */
    public static @Nullable RecipeView find(String machine, String recipeId) {
        if (machine.isEmpty() || recipeId.isEmpty()) {
            return null;
        }
        Identifier machineId = Identifier.tryParse(machine);
        if (machineId == null) {
            return null;
        }
        MultiblockRecipeIndex index = MultiblockRecipeIndex.current();
        if (index == null) {
            return null;
        }
        TypeIndex type = index.type(machineId);
        if (type == null) {
            return null;
        }
        for (CompiledRecipe recipe : type.recipes()) {
            if (recipe.id().toString().equals(recipeId)) {
                return of(recipe);
            }
        }
        return null;
    }

    /**
     * 展平一条配方
     *
     * @param compiled 编译后的配方
     * @return 展示数据
     */
    public static RecipeView of(CompiledRecipe compiled) {
        List<ItemEntry> itemInputs = new ArrayList<>();
        List<FluidEntry> fluidInputs = new ArrayList<>();
        List<ItemEntry> itemOutputs = new ArrayList<>();
        List<FluidEntry> fluidOutputs = new ArrayList<>();
        long energy = 0L;

        for (ResourceIngredient ingredient : compiled.recipe().inputs()) {
            switch (ingredient) {
                case ResourceIngredient.Items items -> {
                    long count = items.input().count();
                    items.input().items().ifPresent(set -> {
                        for (Holder<Item> holder : set) {
                            itemInputs.add(new ItemEntry(
                                new ItemStack(holder.value(), stackCount(count)), count
                            ));
                        }
                    });
                }
                case ResourceIngredient.Fluids fluids -> {
                    int amount = fluids.ingredient().amount();
                    for (Holder<Fluid> holder : fluids.ingredient().ingredient().fluids()) {
                        fluidInputs.add(new FluidEntry(FluidResource.of(holder).toStack(amount), amount));
                    }
                }
                case ResourceIngredient.Energy entry -> energy += entry.amount();
            }
        }
        for (ResourceOutput output : compiled.recipe().outputs()) {
            switch (output) {
                case ResourceOutput.Items items -> {
                    int amount = constant(items.amount());
                    itemOutputs.add(new ItemEntry(
                        ItemResource.of(items.template()).toStack(Math.max(1, amount)), amount
                    ));
                }
                case ResourceOutput.Fluids fluids -> {
                    int amount = constant(fluids.amount());
                    fluidOutputs.add(new FluidEntry(
                        FluidResource.of(fluids.fluid()).toStack(Math.max(1, amount)), amount
                    ));
                }
                case ResourceOutput.Energy ignored -> {
                    // 产出的能量不计入展示，界面只关心配方消耗
                }
            }
        }
        return new RecipeView(itemInputs, fluidInputs, itemOutputs, fluidOutputs, energy, compiled.time());
    }

    /**
     * 把 {@code long} 需求量压进物品堆栈的数量字段
     *
     * <p>堆栈数量是 {@code int}，超出范围会截断成负数并判空，故夹在有效区间内；
     * 真实数量由 {@link ItemEntry#amount()} 携带。</p>
     *
     * @param amount 需求量
     * @return 可以放进堆栈的数量，夹在 {@code 1} 与 {@link Integer#MAX_VALUE} 之间
     */
    private static int stackCount(long amount) {
        return (int) Math.clamp(amount, 1L, Integer.MAX_VALUE);
    }

    private static int constant(ContextIntProvider provider) {
        return provider instanceof ConstantValue(int value) ? value : 1;
    }

    /**
     * @param stack  槽位展示的物品，数量已被压进 {@code int}
     * @param amount 真实需求量或产出量，可能超出堆栈能表达的范围
     */
    public record ItemEntry(ItemStack stack, long amount) {
        public String name() {
            return this.stack.getHoverName().getString();
        }
    }

    /**
     * @param stack  流体堆栈，用于取名称与图标
     * @param amount 真实需求量或产出量，单位 mB
     */
    public record FluidEntry(FluidStack stack, long amount) {
        public String name() {
            return this.stack.getHoverName().getString();
        }
    }
}
