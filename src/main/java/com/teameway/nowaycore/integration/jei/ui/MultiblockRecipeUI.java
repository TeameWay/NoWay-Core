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
 * <p>排版为三段：中部输入列、箭头、输出列，底部三行数值。两列紧贴箭头，箭头中心固定落在
 * 面板水平中线上，因此输入输出天然对称；槽位少的一侧向箭头靠拢而不是整体居中。</p>
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

        UIElement root = new UIElement()
            .layout(l -> l
                .width(JeiLayout.PANEL_WIDTH)
                .height(height)
                .paddingAll(JeiLayout.PANEL_PADDING))
            .addClass("panel_bg");

        UIElement content = new UIElement()
            .layout(l -> l.width(JeiLayout.CONTENT_WIDTH).height(contentHeight));
        root.addChild(content);

        int columns = layout.columns();
        int inputHeight = contents.inputHeight(columns);
        int outputHeight = contents.outputHeight(columns);

        addColumn(
            content,
            toItemSlots(contents.itemInputs()),
            toFluidSlots(contents.fluidInputs()),
            layout.inputLeft(),
            layout.columnTop(inputHeight),
            columns,
            true
        );
        addColumn(
            content,
            toItemSlots(contents.itemOutputs()),
            toFluidSlots(contents.fluidOutputs()),
            layout.outputLeft(),
            layout.columnTop(outputHeight),
            columns,
            false
        );
        Arrow.centerIn(content, layout.arrowTop(), recipe.time());

        addTexts(content, recipe, contents.energyInput(), layout.textTop());

        ModularUI ui = ModularUI.of(UI.of(root, JeiLayout.stylesheets()));
        ui.setDrawTooltips(true);
        return ui;
    }

    /**
     * 摆放一列：物品组在上、流体组在下，空组不占位
     *
     * <p>每一行都朝箭头一侧对齐，不足一行时空位留在外侧。</p>
     *
     * @param hugEnd 是否靠箭头一侧对齐；输入列在箭头左侧，靠右对齐，输出列相反
     */
    private static void addColumn(
        UIElement content,
        List<UIElement> items,
        List<UIElement> fluids,
        int left,
        int top,
        int columns,
        boolean hugEnd
    ) {
        int y = top;
        if (!items.isEmpty()) {
            place(content, items, left, y, columns, hugEnd);
            y += JeiLayout.gridHeight(items.size(), columns) + 2;
        }
        if (!fluids.isEmpty()) {
            place(content, fluids, left, y, columns, hugEnd);
        }
    }

    /**
     * 按网格逐个摆放槽位
     *
     * <p>每一行都朝箭头一侧对齐：不足一行时，靠箭头的那一端先占满，空出来的格子留在外侧。
     * 这样槽位少的配方也不会跑到另一条配方的槽位位置上。</p>
     *
     * @param hugEnd 是否靠右侧对齐，输入列为 {@code true}
     */
    private static void place(
        UIElement content,
        List<UIElement> slots,
        int left,
        int top,
        int columns,
        boolean hugEnd
    ) {
        for (int index = 0; index < slots.size(); index++) {
            int row = index / columns;
            int inRow = Math.min(columns, slots.size() - row * columns);
            int column = index % columns;
            if (hugEnd) {
                column += columns - inRow;
            }
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