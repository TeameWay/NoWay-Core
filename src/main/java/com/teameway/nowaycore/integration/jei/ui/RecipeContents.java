package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.teameway.nowaycore.common.recipe.CompiledRecipe;
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
 * <p>把配方里的多态输入输出统一成物品列表、流体列表与能量数值，排版层只关心数量与尺寸，
 * 不需要再判断资源种类。排版计算与界面搭建共用同一份拆解结果，避免两处判断不一致。</p>
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
    /**
     * 物品组与流体组之间的纵向间距。
     */
    private static final int GROUP_GAP = 2;

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
                case ResourceOutput.Items items -> itemOutputs.add(new SlotItem(
                    ItemResource.of(items.template()).toStack(constant(items.amount())),
                    IngredientIO.OUTPUT,
                    items.chance()
                ));
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

    private static int columnHeight(int items, int fluids, int columns) {
        int height = 0;
        if (items > 0) {
            height += JeiLayout.gridHeight(items, columns);
        }
        if (fluids > 0) {
            height += (height > 0 ? GROUP_GAP : 0) + JeiLayout.gridHeight(fluids, columns);
        }
        return height;
    }

    private static List<SlotItem> inputItems(ResourceIngredient.Items items) {
        List<SlotItem> stacks = new ArrayList<>();
        int count = items.predicate().count();
        IngredientIO io = items.consumed() ? IngredientIO.INPUT : IngredientIO.CATALYST;
        items.predicate().items().ifPresent(set -> {
            for (Holder<Item> holder : set) {
                stacks.add(new SlotItem(new ItemStack(holder.value(), count), io, 1.0F));
            }
        });
        return stacks;
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
     * @return 输入侧全部格子数，物品与流体合并计算
     */
    public int inputCells() {
        return this.itemInputs.size() + this.fluidInputs.size();
    }

    /**
     * @return 输出侧全部格子数
     */
    public int outputCells() {
        return this.itemOutputs.size() + this.fluidOutputs.size();
    }

    /**
     * @param columns 每行列数
     * @return 输入侧在该列数下的高度
     */
    public int inputHeight(int columns) {
        return columnHeight(this.itemInputs.size(), this.fluidInputs.size(), columns);
    }

    /**
     * @param columns 每行列数
     * @return 输出侧在该列数下的高度
     */
    public int outputHeight(int columns) {
        return columnHeight(this.itemOutputs.size(), this.fluidOutputs.size(), columns);
    }

    /**
     * @param stack  槽位展示的物品
     * @param io     JEI 语义角色，未消耗的输入为催化剂
     * @param chance 产出概率，输入恒为 1
     */
    public record SlotItem(ItemStack stack, IngredientIO io, float chance) {
    }

    /**
     * @param stack  槽位展示的流体
     * @param io     JEI 语义角色，未消耗的输入为催化剂
     * @param chance 产出概率，输入恒为 1
     */
    public record SlotFluid(FluidStack stack, IngredientIO io, float chance) {
    }
}