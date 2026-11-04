package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import com.teameway.nowaycore.resource.ResourceIngredient;
import com.teameway.nowaycore.resource.ResourceOutput;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
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
import java.util.Locale;
import java.util.function.Function;

/**
 * JEI 配方界面
 *
 * <p>沿用 GTM 配方页的三段式结构：左输入列、中间箭头、右输出列，列内材料按每行三格排布，
 * 耗时与能量放在底部文字行。分类高度由 {@link #heightFor} 按该分类最大的配方算出，
 * 槽位多的分类自动变高，槽位少的分类不留空白。</p>
 */
public class MultiblockRecipeUI {
    public static final int WIDTH = 176;
    private static final int GAP = 4;
    private static final int LINE = 10;
    /**
     * {@code panel_bg} 样式类施加的内边距，界面高度计算需要计入。
     */
    private static final int PANEL_PADDING = 5;
    private static final int SLOT = 18;
    private static final int GROUP_GAP = 2;
    private static final int COLUMN_WIDTH = SLOT * 3 + 2;
    private static final int ARROW_SIZE = 12;

    private MultiblockRecipeUI() {
    }

    /**
     * 计算该分类显示这些配方所需的高度
     *
     * @param recipes 该分类的配方
     * @return 界面高度
     */
    public static int heightFor(List<CompiledRecipe> recipes) {
        int body = SLOT;
        for (CompiledRecipe recipe : recipes) {
            Contents contents = Contents.of(recipe);
            body = Math.max(
                body, Math.max(
                    columnHeight(contents.itemInputs(), contents.fluidInputs()),
                    columnHeight(contents.itemOutputs(), contents.fluidOutputs())
                )
            );
        }
        return PANEL_PADDING * 2 + body + GAP + LINE;
    }

    /**
     * 构建配方界面
     *
     * @param recipe 编译后的配方
     * @param height 分类高度，由 {@link #heightFor} 统一算出
     * @return 供 JEI 显示的界面
     */
    public static ModularUI create(CompiledRecipe recipe, int height) {
        Contents contents = Contents.of(recipe);

        UIElement root = new UIElement()
            .layout(layout -> layout
                .width(WIDTH)
                .height(height)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(GAP))
            .addClass("panel_bg");

        UIElement body = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flex(1)
            .flexDirection(FlexDirection.ROW)
            .alignItems(AlignItems.CENTER)
            .justifyContent(AlignContent.SPACE_BETWEEN));

        body.addChildren(
            column(contents.itemInputs(), contents.fluidInputs()),
            arrow(),
            column(contents.itemOutputs(), contents.fluidOutputs())
        );

        root.addChildren(body, footer(recipe, contents.energyInput(), contents.energyOutput()));

        ModularUI ui = ModularUI.of(UI.of(
            root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))
        ));
        ui.setDrawTooltips(true);
        return ui;
    }

    private static int columnHeight(List<SlotItem> items, List<SlotFluid> fluids) {
        int height = 0;
        if (!items.isEmpty()) {
            height += rows(items.size()) * SLOT;
        }
        if (!fluids.isEmpty()) {
            height += (height > 0 ? GROUP_GAP : 0) + rows(fluids.size()) * SLOT;
        }
        return height;
    }

    private static int rows(int count) {
        return (count + 2) / 3;
    }

    /**
     * 一列材料：物品在上、流体在下，空分组不占位。
     */
    private static UIElement column(List<SlotItem> items, List<SlotFluid> fluids) {
        UIElement column = new UIElement().layout(layout -> layout
            .width(COLUMN_WIDTH)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(GROUP_GAP));
        if (!items.isEmpty()) {
            column.addChild(grid(items, MultiblockRecipeUI::itemSlot));
        }
        if (!fluids.isEmpty()) {
            column.addChild(grid(fluids, MultiblockRecipeUI::fluidSlot));
        }
        return column;
    }

    /**
     * 把一组槽位按每行三格排列，宽度恰好容纳三格，第四格起换行。
     */
    private static <T> UIElement grid(List<T> values, Function<T, UIElement> slotFactory) {
        UIElement row = new UIElement().layout(layout -> layout
            .width(COLUMN_WIDTH)
            .flexDirection(FlexDirection.ROW)
            .wrap(FlexWrap.WRAP)
            .gapAll(1));
        for (T value : values) {
            row.addChild(slotFactory.apply(value));
        }
        return row;
    }

    private static UIElement arrow() {
        return new UIElement()
            .layout(layout -> layout
                .width(SLOT)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER))
            .addChild(new UIElement()
                .layout(layout -> layout.width(ARROW_SIZE).height(ARROW_SIZE))
                .style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR)));
    }

    /**
     * 物品槽，{@code chance} 小于 1 时作为概率槽注册并把概率写进提示。
     */
    private static ItemSlot itemSlot(SlotItem entry) {
        ItemSlot slot = new ItemSlot();
        slot.setItem(entry.stack());
        if (entry.chance() < 1.0F) {
            slot.xeiRecipeSlot(entry.io(), entry.chance());
            slot.style(style -> style.appendTooltips(chanceTooltip(entry.chance())));
        } else {
            slot.xeiRecipeIngredient(entry.io());
        }
        return slot;
    }

    /**
     * 流体槽，{@code chance} 小于 1 时作为概率槽注册并把概率写进提示。
     */
    private static FluidSlot fluidSlot(SlotFluid entry) {
        FluidSlot slot = new FluidSlot();
        slot.setFluid(entry.stack());
        if (entry.chance() < 1.0F) {
            slot.xeiRecipeSlot(entry.io(), entry.chance());
            slot.style(style -> style.appendTooltips(chanceTooltip(entry.chance())));
        } else {
            slot.xeiRecipeIngredient(entry.io());
        }
        return slot;
    }

    private static UIElement footer(CompiledRecipe recipe, long energyInput, long energyOutput) {
        UIElement footer = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .height(LINE)
            .flexDirection(FlexDirection.ROW)
            .alignItems(AlignItems.CENTER)
            .gapAll(6));

        Label time = new Label();
        time.setText(Component.translatable(
            "gui.noway_core.jei.time", String.format(Locale.ROOT, "%.1f", recipe.time() / 20.0F)
        ));
        time.layout(layout -> layout.height(LINE));
        footer.addChild(time);

        if (energyInput > 0 || energyOutput > 0) {
            Label energy = new Label();
            energy.setText(Component.translatable(
                "gui.noway_core.jei.energy", energyInput > 0 ? energyInput : energyOutput
            ));
            energy.layout(layout -> layout.height(LINE));
            footer.addChild(energy);
        }
        return footer;
    }

    private static int constant(ContextIntProvider provider) {
        return provider instanceof ConstantValue(int value1) ? value1 : 1;
    }

    private static Component chanceTooltip(float chance) {
        return Component.translatable(
            "gui.noway_core.jei.chance", String.format(Locale.ROOT, "%.1f%%", chance * 100.0F)
        );
    }

    /**
     * 配方拆出的四组槽位与两种能量，高度计算与界面搭建共用同一份结果。
     *
     * @param itemInputs   物品输入
     * @param fluidInputs  流体输入
     * @param itemOutputs  物品产出
     * @param fluidOutputs 流体产出
     * @param energyInput  输入能量合计
     * @param energyOutput 产出能量合计
     */
    private record Contents(
        List<SlotItem> itemInputs,
        List<SlotFluid> fluidInputs,
        List<SlotItem> itemOutputs,
        List<SlotFluid> fluidOutputs,
        long energyInput,
        long energyOutput
    ) {
        static Contents of(CompiledRecipe compiled) {
            List<SlotItem> itemInputs = new ArrayList<>();
            List<SlotFluid> fluidInputs = new ArrayList<>();
            long energyInput = 0L;
            List<SlotItem> itemOutputs = new ArrayList<>();
            List<SlotFluid> fluidOutputs = new ArrayList<>();
            long energyOutput = 0L;

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
                    case ResourceOutput.Energy energy -> energyOutput += constant(energy.amount());
                }
            }
            return new Contents(itemInputs, fluidInputs, itemOutputs, fluidOutputs, energyInput, energyOutput);
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
    }

    /**
     * @param stack  槽位展示的物品
     * @param io     JEI 语义角色，未消耗的输入为催化剂
     * @param chance 产出概率，输入恒为 1
     */
    private record SlotItem(ItemStack stack, IngredientIO io, float chance) {
    }

    /**
     * @param stack  槽位展示的流体
     * @param io     JEI 语义角色，未消耗的输入为催化剂
     * @param chance 产出概率，输入恒为 1
     */
    private record SlotFluid(FluidStack stack, IngredientIO io, float chance) {
    }
}