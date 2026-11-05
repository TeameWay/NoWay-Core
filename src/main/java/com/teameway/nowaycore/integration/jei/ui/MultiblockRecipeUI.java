package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.teameway.nowaycore.gui.NowayTheme;
import com.teameway.nowaycore.integration.jei.layout.JeiLayout;
import com.teameway.nowaycore.integration.jei.layout.RecipeLayout;
import com.teameway.nowaycore.integration.jei.ui.widget.Arrow;
import com.teameway.nowaycore.ldlib.slot.CompactItemSlot;
import com.teameway.nowaycore.recipe.CompiledRecipe;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 配方页
 *
 * <p>中部放输入整体、箭头与输出整体，底部三行数值；两个整体与箭头的中心在内容区内等距分布。</p>
 *
 * <p>整体内部先排物品槽，两种槽都有时另起一行再排流体槽，各自从左往右、从上往下填格。</p>
 *
 * <p>所有元素用绝对坐标摆放：JEI 按元素局部坐标反查槽位，坐标只由下标决定，不会随相邻元素漂移。</p>
 */
public class MultiblockRecipeUI {
    /**
     * 物品组与流体组之间的纵向间距，与 {@link RecipeContents} 的高度计算保持一致。
     */
    private static final int GROUP_GAP = 1;

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

        ModularUI ui = ModularUI.of(NowayTheme.of(root));
        ui.setDrawTooltips(true);
        return ui;
    }

    /**
     * 摆放一个整体：先物品组，两种都有时另起一行再排流体组。
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
            y += JeiLayout.gridHeight(items.size(), columns) + GROUP_GAP;
        }
        if (!fluids.isEmpty()) {
            place(content, fluids, left, y, columns);
        }
    }

    /**
     * 按网格逐个摆放槽位
     *
     * <p>从左往右、从上往下填格：前几行填满，末行靠左，空位留在该行右侧。</p>
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

    /**
     * 建物品槽
     *
     * <p>必须用 {@code xeiRecipeSlot}：JEI 会把登记的槽位交给 LDLib2 的部件做命中测试，该部件只认领可交互的槽位，只登记原料时鼠标悬停无反应，{@code R}/{@code U} 也查不到。</p>
     *
     * @param entries 物品槽内容
     * @return 槽位元素
     */
    private static List<UIElement> toItemSlots(List<RecipeContents.SlotItem> entries) {
        List<UIElement> slots = new ArrayList<>(entries.size());
        for (RecipeContents.SlotItem entry : entries) {
            CompactItemSlot slot = new CompactItemSlot();
            slot.setItem(entry.stack());
            slot.setAmount(entry.amount());
            slot.xeiRecipeSlot(entry.io(), entry.chance());
            if (entry.chance() < 1.0F) {
                slot.style(style -> style.appendTooltips(chanceTooltip(entry.chance())));
            }
            slots.add(slot);
        }
        return slots;
    }

    /**
     * 建流体槽
     *
     * @param entries 流体槽内容
     * @return 槽位元素
     * @see #toItemSlots(List)
     */
    private static List<UIElement> toFluidSlots(List<RecipeContents.SlotFluid> entries) {
        List<UIElement> slots = new ArrayList<>(entries.size());
        for (RecipeContents.SlotFluid entry : entries) {
            FluidSlot slot = new FluidSlot();
            slot.setFluid(entry.stack());
            slot.xeiRecipeSlot(entry.io(), entry.chance());
            if (entry.chance() < 1.0F) {
                slot.style(style -> style.appendTooltips(chanceTooltip(entry.chance())));
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