package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.teameway.nowaycore.integration.jei.layout.JeiLayout;
import com.teameway.nowaycore.recipe.CompiledRecipe;
import com.teameway.nowaycore.resource.ResourceIngredient;
import com.teameway.nowaycore.resource.ResourceOutput;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方拆解结果
 *
 * <p>把多态输入输出统一成物品列表、流体列表与能量数值，排版计算与界面搭建共用同一份结果。</p>
 *
 * <p>输入侧与输出侧各自是一个整体：先排物品槽，两种槽都有时另起一行再排流体槽，各自从左往右、从上往下填格。</p>
 *
 * @param itemInputs   物品输入，未消耗的输入按催化剂处理
 * @param fluidInputs  流体输入
 * @param itemOutputs  物品产出
 * @param fluidOutputs 流体产出
 * @param energyInput  单次执行消耗的能量，单位 FE
 */
public record RecipeContents(
    List<SlotItem> itemInputs,
    List<SlotFluid> fluidInputs,
    List<SlotItem> itemOutputs,
    List<SlotFluid> fluidOutputs,
    long energyInput
) {
    private static final int GROUP_GAP = 1;

    /**
     * 按 JEI 的排布拆解配方
     *
     * @param compiled 编译后的配方
     * @return 拆解结果
     */
    public static RecipeContents of(CompiledRecipe compiled) {
        List<SlotItem> itemInputs = new ArrayList<>();
        List<SlotFluid> fluidInputs = new ArrayList<>();
        List<SlotItem> itemOutputs = new ArrayList<>();
        List<SlotFluid> fluidOutputs = new ArrayList<>();
        long energyInput = 0L;

        for (ResourceIngredient ingredient : compiled.recipe().inputs()) {
            switch (ingredient) {
                case ResourceIngredient.Items items -> itemInputs.addAll(inputItems(items));
                case ResourceIngredient.Fluids fluids -> fluidInputs.addAll(inputFluids(fluids));
                case ResourceIngredient.Energy energy -> energyInput += energy.amount();
            }
        }
        for (ResourceOutput output : compiled.recipe().outputs()) {
            switch (output) {
                case ResourceOutput.Items items -> {
                    int amount = constant(items.amount());
                    itemOutputs.add(new SlotItem(
                        ItemResource.of(items.template()).toStack(amount),
                        amount,
                        IngredientIO.OUTPUT,
                        items.chance()
                    ));
                }
                case ResourceOutput.Fluids fluids -> fluidOutputs.add(new SlotFluid(
                    FluidResource.of(fluids.fluid()).toStack(constant(fluids.amount())),
                    IngredientIO.OUTPUT,
                    fluids.chance()
                ));
                case ResourceOutput.Energy ignored -> {
                    // 产出的能量不进配方页，这里只展示输入侧的能量消耗
                }
            }
        }
        return new RecipeContents(itemInputs, fluidInputs, itemOutputs, fluidOutputs, energyInput);
    }

    private static int columns(int items, int fluids, int maxColumns) {
        int widest = Math.max(items, fluids);
        return Math.max(1, Math.min(maxColumns, widest));
    }

    private static int blockHeight(int items, int fluids, int columns) {
        int height = 0;
        if (items > 0) {
            height += JeiLayout.gridHeight(items, columns);
        }
        if (fluids > 0) {
            height += (height > 0 ? GROUP_GAP : 0) + JeiLayout.gridHeight(fluids, columns);
        }
        return height;
    }

    /**
     * @param maxColumns 分类允许的最大列数
     * @return 输入整体所需的列数，至少 1
     */
    public int inputColumns(int maxColumns) {
        return columns(this.itemInputs.size(), this.fluidInputs.size(), maxColumns);
    }

    /**
     * @param maxColumns 分类允许的最大列数
     * @return 输出整体所需的列数，至少 1
     */
    public int outputColumns(int maxColumns) {
        return columns(this.itemOutputs.size(), this.fluidOutputs.size(), maxColumns);
    }

    /**
     * @param maxColumns 分类允许的最大列数
     * @return 输入整体的宽度
     */
    public int inputWidth(int maxColumns) {
        return JeiLayout.gridWidth(inputColumns(maxColumns));
    }

    /**
     * @param maxColumns 分类允许的最大列数
     * @return 输出整体的宽度
     */
    public int outputWidth(int maxColumns) {
        return JeiLayout.gridWidth(outputColumns(maxColumns));
    }

    /**
     * @param maxColumns 分类允许的最大列数
     * @return 输入整体的高度，物品组与流体组上下相接
     */
    public int inputHeight(int maxColumns) {
        return blockHeight(this.itemInputs.size(), this.fluidInputs.size(), inputColumns(maxColumns));
    }

    /**
     * @param maxColumns 分类允许的最大列数
     * @return 输出整体的高度
     */
    public int outputHeight(int maxColumns) {
        return blockHeight(this.itemOutputs.size(), this.fluidOutputs.size(), outputColumns(maxColumns));
    }

    private static List<SlotItem> inputItems(ResourceIngredient.Items items) {
        List<SlotItem> stacks = new ArrayList<>();
        long count = items.input().count();
        IngredientIO io = items.consumed() ? IngredientIO.INPUT : IngredientIO.CATALYST;
        items.input().items().ifPresent(set -> {
            for (Holder<Item> holder : set) {
                stacks.add(new SlotItem(new ItemStack(holder.value(), stackCount(count)), count, io, 1.0F));
            }
        });
        return stacks;
    }

    /**
     * 把 {@code long} 需求量压进物品堆栈的数量字段
     *
     * <p>堆栈数量是 {@code int}，超出范围会截断成负数并让槽位判空；真实数量由 {@link SlotItem#amount()} 另行携带。</p>
     *
     * @param count 需求量
     * @return 可以放进堆栈的数量，至少 1
     */
    private static int stackCount(long count) {
        return (int) Math.clamp(count, 1L, Integer.MAX_VALUE);
    }

    private static List<SlotFluid> inputFluids(ResourceIngredient.Fluids fluids) {
        List<SlotFluid> resources = new ArrayList<>();
        int amount = fluids.ingredient().amount();
        IngredientIO io = fluids.consumed() ? IngredientIO.INPUT : IngredientIO.CATALYST;
        for (Holder<Fluid> holder : fluids.ingredient().ingredient().fluids()) {
            resources.add(new SlotFluid(FluidResource.of(holder).toStack(amount), io, 1.0F));
        }
        return resources;
    }

    private static int constant(ContextIntProvider provider) {
        return provider instanceof ConstantValue(int value) ? value : 1;
    }

    /**
     * @param stack  槽位展示的物品，数量已压进 {@code int}
     * @param amount 真实需求量，可能超出堆栈范围
     * @param io     JEI 语义角色，未消耗的输入为催化剂
     * @param chance 产出概率，输入恒为 1
     */
    public record SlotItem(ItemStack stack, long amount, IngredientIO io, float chance) {
    }

    /**
     * @param stack  槽位展示的流体
     * @param io     JEI 语义角色，未消耗的输入为催化剂
     * @param chance 产出概率，输入恒为 1
     */
    public record SlotFluid(FluidStack stack, IngredientIO io, float chance) {
    }
}