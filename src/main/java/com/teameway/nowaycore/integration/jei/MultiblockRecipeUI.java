package com.teameway.nowaycore.integration.jei;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
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

/**
 * JEI 配方界面
 *
 * <p>用 LDLib2 的元素搭出与游戏内一致的外观：左边输入、右边输出，流体槽与物品槽分开排列，
 * 底部显示耗时与能量。JEI 通过元素位置反查槽位，因此这里只管摆放，不做任何数据绑定。</p>
 */
final class MultiblockRecipeUI {
    static final int WIDTH = 176;
    static final int HEIGHT = 208;
    private static final int LINE = 10;

    private MultiblockRecipeUI() {
    }

    /**
     * 构建配方界面
     *
     * @param recipe  编译后的配方
     * @param machine 配方所属的机器类型，用于取结构定义
     * @return 供 JEI 显示的界面
     */
    static ModularUI create(CompiledRecipe recipe, MultiblockType machine) {
        List<ItemStack> itemInputs = new ArrayList<>();
        List<FluidStack> fluidInputs = new ArrayList<>();
        long energyInput = 0L;
        List<ItemStack> itemOutputs = new ArrayList<>();
        List<Float> itemChances = new ArrayList<>();
        List<FluidStack> fluidOutputs = new ArrayList<>();
        List<Float> fluidChances = new ArrayList<>();
        long energyOutput = 0L;

        for (ResourceIngredient ingredient : recipe.recipe().inputs()) {
            switch (ingredient) {
                case ResourceIngredient.Items items -> itemInputs.addAll(inputItems(items));
                case ResourceIngredient.Fluids fluids -> fluidInputs.addAll(inputFluids(fluids));
                case ResourceIngredient.Energy energy -> energyInput += energy.amount();
            }
        }
        for (ResourceOutput output : recipe.recipe().outputs()) {
            switch (output) {
                case ResourceOutput.Items items -> {
                    itemOutputs.add(itemOutput(items));
                    itemChances.add(items.chance());
                }
                case ResourceOutput.Fluids fluids -> {
                    fluidOutputs.add(FluidResource.of(fluids.fluid()).toStack(constant(fluids.amount())));
                    fluidChances.add(fluids.chance());
                }
                case ResourceOutput.Energy energy -> energyOutput += constant(energy.amount());
            }
        }

        UIElement root = new UIElement()
            .layout(layout -> layout
                .width(WIDTH)
                .height(HEIGHT)
                .flexDirection(FlexDirection.COLUMN)
                .paddingAll(4)
                .gapAll(3))
            .addClass("panel_bg");

        UIElement body = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .height(52)
            .flexDirection(FlexDirection.ROW)
            .gapAll(4));

        UIElement inputs = column(76);
        inputs.addChildren(
            slotRow(itemInputs),
            fluidRow(fluidInputs)
        );

        UIElement arrow = new UIElement().layout(layout -> layout
            .width(18)
            .heightPercent(100)
            .flexDirection(FlexDirection.COLUMN)
            .justifyContent(AlignContent.CENTER));
        arrow.addChild(new UIElement()
            .layout(layout -> layout.width(18).height(18))
            .style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR)));

        UIElement outputs = column(76);
        outputs.addChildren(
            outputRow(itemOutputs, itemChances),
            fluidOutputRow(fluidOutputs, fluidChances)
        );

        body.addChildren(inputs, arrow, outputs);

        UIElement footer = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .height(LINE)
            .flexDirection(FlexDirection.ROW)
            .gapAll(8));

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

        root.addChildren(body, preview(machine), footer);

        ModularUI ui = ModularUI.of(UI.of(
            root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))
        ));
        ui.setDrawTooltips(true);
        return ui;
    }

    private static UIElement preview(MultiblockType machine) {
        UIElement container = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flex(1)
            .flexDirection(FlexDirection.COLUMN)
            .alignItems(AlignItems.CENTER));
        Scene scene = MultiblockPreview.create(machine);
        if (scene != null) {
            scene.layout(layout -> layout.widthPercent(100).heightPercent(100));
            container.addChild(scene);
            return container;
        }
        Label missing = new Label();
        missing.setText(Component.translatable("gui.noway_core.jei.no_structure"));
        missing.layout(layout -> layout.height(LINE));
        container.addChild(missing);
        return container;
    }

    private static UIElement column(int width) {
        return new UIElement().layout(layout -> layout
            .width(width)
            .heightPercent(100)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(2));
    }

    private static UIElement slotRow(List<ItemStack> stacks) {
        UIElement row = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flexDirection(FlexDirection.ROW)
            .wrap(FlexWrap.WRAP)
            .gapAll(1));
        for (ItemStack stack : stacks) {
            ItemSlot slot = new ItemSlot();
            slot.setItem(stack);
            slot.xeiRecipeIngredient(IngredientIO.INPUT);
            row.addChild(slot);
        }
        return row;
    }

    private static UIElement outputRow(List<ItemStack> stacks, List<Float> chances) {
        UIElement row = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flexDirection(FlexDirection.ROW)
            .wrap(FlexWrap.WRAP)
            .gapAll(1));
        for (int i = 0; i < stacks.size(); i++) {
            ItemSlot slot = new ItemSlot();
            slot.setItem(stacks.get(i));
            slot.xeiRecipeIngredient(IngredientIO.OUTPUT);
            float chance = chances.get(i);
            if (chance < 1.0F) {
                slot.xeiRecipeSlot(IngredientIO.OUTPUT, chance);
            }
            row.addChild(slot);
        }
        return row;
    }

    private static UIElement fluidRow(List<FluidStack> fluids) {
        UIElement row = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flexDirection(FlexDirection.ROW)
            .wrap(FlexWrap.WRAP)
            .gapAll(1));
        for (FluidStack fluid : fluids) {
            FluidSlot slot = new FluidSlot();
            slot.setFluid(fluid);
            slot.xeiRecipeIngredient(IngredientIO.INPUT);
            row.addChild(slot);
        }
        return row;
    }

    private static UIElement fluidOutputRow(List<FluidStack> fluids, List<Float> chances) {
        UIElement row = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flexDirection(FlexDirection.ROW)
            .wrap(FlexWrap.WRAP)
            .gapAll(1));
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            FluidSlot slot = new FluidSlot();
            slot.setFluid(fluid);
            slot.xeiRecipeIngredient(IngredientIO.OUTPUT);
            float chance = chances.get(i);
            if (chance < 1.0F) {
                slot.xeiRecipeSlot(IngredientIO.OUTPUT, chance);
            }
            row.addChild(slot);
        }
        return row;
    }

    private static List<ItemStack> inputItems(ResourceIngredient.Items items) {
        List<ItemStack> stacks = new ArrayList<>();
        int count = items.predicate().count();
        items.predicate().items().ifPresent(set -> {
            for (Holder<Item> holder : set) {
                stacks.add(new ItemStack(holder.value(), count));
            }
        });
        return stacks;
    }

    private static List<FluidStack> inputFluids(ResourceIngredient.Fluids fluids) {
        List<FluidStack> resources = new ArrayList<>();
        int amount = fluids.ingredient().amount();
        for (Holder<Fluid> holder : fluids.ingredient().ingredient().fluids()) {
            resources.add(FluidResource.of(holder).toStack(amount));
        }
        return resources;
    }

    private static ItemStack itemOutput(ResourceOutput.Items items) {
        return ItemResource.of(items.template()).toStack(constant(items.amount()));
    }

    private static int constant(ContextIntProvider provider) {
        return provider instanceof ConstantValue(int value1) ? value1 : 1;
    }
}
