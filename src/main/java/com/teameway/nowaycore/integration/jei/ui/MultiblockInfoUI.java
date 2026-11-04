package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * 结构信息分类的预览界面
 *
 * <p>上半部分是结构所需方块，按每行三格排列并显示数量；下半部分是三维预览。
 * 方块清单按内容计算高度并保持固定尺寸，预览区域占用剩余空间，
 * 因此方块种类增多时清单向上扩展，不会被预览挤掉后面的行。</p>
 */
public class MultiblockInfoUI {
    public static final int WIDTH = 176;
    /**
     * 结构缺失时使用的界面高度。
     */
    public static final int EMPTY_HEIGHT = 120;
    private static final int LINE = 10;
    private static final int SLOT = 18;
    private static final int GAP = 3;
    private static final int PANEL_PADDING = 5;
    private static final int COLUMNS = 3;
    private static final int PREVIEW_HEIGHT = 96;

    private MultiblockInfoUI() {
    }

    /**
     * 计算该结构界面所需的高度
     *
     * @param blocks 结构所需方块及数量
     * @return 界面高度
     */
    public static int heightFor(Map<Block, Integer> blocks) {
        return PANEL_PADDING * 2 + LINE + GAP + PREVIEW_HEIGHT + GAP + listHeight(blocks.size());
    }

    /**
     * @param type   机器类型
     * @param blocks 结构所需方块及数量
     * @param height 该结构界面高度，由 {@link #heightFor} 算出
     * @return 供 JEI 显示的界面
     */
    public static ModularUI create(MultiblockType type, Map<Block, Integer> blocks, int height) {
        UIElement root = new UIElement()
            .layout(layout -> layout
                .width(WIDTH)
                .height(height)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(GAP))
            .addClass("panel_bg");

        Label title = new Label();
        title.setText(type.controller().get().getBlock().getName());
        title.layout(layout -> layout.widthPercent(100).height(LINE));

        root.addChildren(title, preview(type), blockList(blocks));
        return ModularUI.of(UI.of(
            root, List.of(
                StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)
            )
        ));
    }

    private static int listHeight(int kinds) {
        int rows = (kinds + COLUMNS - 1) / COLUMNS;
        return rows * SLOT + Math.max(0, rows - 1);
    }

    private static UIElement preview(MultiblockType type) {
        UIElement container = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .height(PREVIEW_HEIGHT)
            .flexDirection(FlexDirection.COLUMN)
            .alignItems(AlignItems.CENTER)
            .justifyContent(AlignContent.CENTER));
        Scene scene = MultiblockPreview.create(type);
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

    /**
     * 方块清单，每行三格，高度由 {@link #listHeight} 给出，不参与剩余空间分配。
     */
    private static UIElement blockList(Map<Block, Integer> blocks) {
        UIElement row = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .height(listHeight(blocks.size()))
            .flexDirection(FlexDirection.ROW)
            .wrap(FlexWrap.WRAP)
            .gapAll(1));
        blocks.forEach((block, count) -> {
            ItemSlot slot = new ItemSlot();
            slot.setItem(new ItemStack(block.asItem(), count));
            slot.xeiRecipeIngredient(IngredientIO.INPUT);
            row.addChild(slot);
        });
        return row;
    }
}