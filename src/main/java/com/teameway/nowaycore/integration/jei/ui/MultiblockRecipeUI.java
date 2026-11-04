package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import com.teameway.nowaycore.integration.jei.layout.RecipeLayout;
import com.teameway.nowaycore.integration.jei.ui.widget.Arrow;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 配方页
 *
 * <p>排版为三段：中部“输入整体、箭头、输出整体”，底部三行数值。输入与输出各自是一个整体，
 * 整体宽度由自身内容决定；两个整体的中心与箭头中心在内容区宽度内等距分布，
 * 因此某一侧槽位偏少时只是整体变窄，画面仍然平衡。</p>
 *
 * <p>整体内部先排物品槽，两种槽都有时另起一行再排流体槽，每种槽都从左往右、从上往下填格。</p>
 *
 * <p>所有元素都用绝对坐标摆放：JEI 通过元素的局部坐标反查槽位，绝对定位让坐标只由下标决定，
 * 不会因为相邻元素变化而漂移。</p>
 */
public class MultiblockRecipeUI {

    private MultiblockRecipeUI() {
    }

    /**
     * 计算该分类的统一排版
     *
     * @param recipes 该分类的配方
     * @return 排版
     */
    public static RecipeLayout layoutFor(List<CompiledRecipe> recipes) {
        List<RecipeContents> contents = new ArrayList<>(recipes.size());
        for (CompiledRecipe recipe : recipes) {
            contents.add(RecipeContents.of(recipe));
        }
        return RecipeLayout.of(contents);
    }

    /**
     * 构建配方界面
     *
     * @param recipe 编译后的配方
     * @param layout 该分类的统一排版
     * @return 供 JEI 显示的界面
     */
    public static ModularUI create(CompiledRecipe recipe, RecipeLayout layout) {
        RecipeContents contents = RecipeContents.of(recipe);
        int height = layout.height();
        int contentHeight = height - JeiLayout.PANEL_PADDING * 2;
        int maxColumns = layout.columns();

        UIElement root = new UIElement()
            .layout(l -> l
                .width(JeiLayout.PANEL_WIDTH)
                .height(height)
                .paddingAll(JeiLayout.PANEL_PADDING))
            .addClass("panel_bg");

        UIElement content = new UIElement()
            .layout(l -> l.width(JeiLayout.CONTENT_WIDTH).height(contentHeight));
        root.addChild(content);

        int inputColumns = contents.inputColumns(maxColumns);
        int outputColumns = contents.outputColumns(maxColumns);
        int inputHeight = contents.inputHeight(maxColumns);
        int outputHeight = contents.outputHeight(maxColumns);

        addBlock(
            content,
            toItemSlots(contents.itemInputs()),
            toFluidSlots(contents.fluidInputs()),
            layout.blockLeft(contents.inputWidth(maxColumns), true),
            layout.blockTop(inputHeight),
            inputColumns
        );
        addBlock(
            content,
            toItemSlots(contents.itemOutputs()),
            toFluidSlots(contents.fluidOutputs()),
            layout.blockLeft(contents.outputWidth(maxColumns), false),
            layout.blockTop(outputHeight),
            outputColumns
        );
        Arrow.centerIn(content, layout.arrowTop(), recipe.time());

        addTexts(content, recipe, contents.energyInput(), layout.textTop());

        ModularUI ui = ModularUI.of(UI.of(root, JeiLayout.stylesheets()));
        ui.setDrawTooltips(true);
        return ui;
    }

    /**
     * 摆放一个整体：先物品组，两种都有时另起一行再排流体组
     */
    private static void addBlock(
        UIElement content,
        List<UIElement> items,
        List<UIElement> fluids,
        int left,
        int top,
        int columns
    ) {
        int y = top;
        if (!items.isEmpty()) {
            place(content, items, left, y, columns);
            y += JeiLayout.gridHeight(items.size(), columns) + 2;
        }
        if (!fluids.isEmpty()) {
            place(content, fluids, left, y, columns);
        }
    }

    /**
     * 按网格逐个摆放槽位
     *
     * <p>从左往右、从上往下依次填格：前几行填满，末行靠左，空位留在该行右侧。</p>
     */
    private static void place(
        UIElement content,
        List<UIElement> slots,
        int left,
        int top,
        int columns
    ) {
        for (int index = 0; index < slots.size(); index++) {
            int row = index / columns;
            int column = index % columns;
            float x = left + column * (JeiLayout.SLOT + JeiLayout.SLOT_GAP);
            float y = top + row * (JeiLayout.SLOT + JeiLayout.SLOT_GAP);
            UIElement slot = slots.get(index);
            slot.layout(l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .left(x)
                .top(y)
                .width(JeiLayout.SLOT)
                .height(JeiLayout.SLOT));
            content.addChild(slot);
        }
    }

    /**
     * 底部三行：耗时、总耗能、耗能功率
     */
    private static void addTexts(UIElement content, CompiledRecipe recipe, long energy, int top) {
        // 功率按每 tick 消耗计，向上取整，避免整除后显示 0 而实际有消耗
        long power = energy <= 0 ? 0L : Math.ceilDiv(energy, Math.max(1, recipe.time()));

        Label time = JeiLayout.label(Component.translatable(
            "gui.noway_core.jei.time", String.format(Locale.ROOT, "%.1f", recipe.time() / 20.0F)
        ));
        Label total = JeiLayout.label(Component.translatable("gui.noway_core.jei.energy_total", energy));
        Label rate = JeiLayout.label(Component.translatable("gui.noway_core.jei.energy_rate", power));

        int line = JeiLayout.LINE + JeiLayout.LINE_GAP;
        labelAt(content, time, top);
        labelAt(content, total, top + line);
        labelAt(content, rate, top + line * 2);
    }

    private static void labelAt(UIElement content, Label label, int top) {
        label.layout(l -> l
            .positionType(TaffyPosition.ABSOLUTE)
            .left(0)
            .top(top)
            .width(JeiLayout.CONTENT_WIDTH)
            .height(JeiLayout.LINE));
        content.addChild(label);
    }

    private static List<UIElement> toItemSlots(List<RecipeContents.SlotItem> entries) {
        List<UIElement> slots = new ArrayList<>(entries.size());
        for (RecipeContents.SlotItem entry : entries) {
            ItemSlot slot = new ItemSlot();
            slot.setItem(entry.stack());
            if (entry.chance() < 1.0F) {
                slot.xeiRecipeSlot(entry.io(), entry.chance());
                slot.style(style -> style.appendTooltips(chanceTooltip(entry.chance())));
            } else {
                slot.xeiRecipeIngredient(entry.io());
            }
            slots.add(slot);
        }
        return slots;
    }

    private static List<UIElement> toFluidSlots(List<RecipeContents.SlotFluid> entries) {
        List<UIElement> slots = new ArrayList<>(entries.size());
        for (RecipeContents.SlotFluid entry : entries) {
            FluidSlot slot = new FluidSlot();
            slot.setFluid(entry.stack());
            if (entry.chance() < 1.0F) {
                slot.xeiRecipeSlot(entry.io(), entry.chance());
                slot.style(style -> style.appendTooltips(chanceTooltip(entry.chance())));
            } else {
                slot.xeiRecipeIngredient(entry.io());
            }
            slots.add(slot);
        }
        return slots;
    }

    private static Component chanceTooltip(float chance) {
        return Component.translatable(
            "gui.noway_core.jei.chance", String.format(Locale.ROOT, "%.1f%%", chance * 100.0F)
        );
    }
}
