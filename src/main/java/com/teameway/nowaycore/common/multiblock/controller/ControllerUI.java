package com.teameway.nowaycore.common.multiblock.controller;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 控制器界面
 *
 * <p>用 LDLib2 的声明式元素搭建：标题、类型与形态、线程与并行数，以及每个线程一行的进度条。
 * 所有数据都来自控制器方块实体上同步过的字段，界面本身不持有运行时对象，因此两端可用同一份代码。</p>
 */
public class ControllerUI {
    private static final int WIDTH = 236;
    private static final int PADDING = 5;
    private static final int LINE = 10;
    private static final int ROW = 15;

    /**
     * 构建控制器界面
     *
     * @param controller 控制器方块实体
     * @param player     打开界面的玩家
     * @return 可显示的界面
     */
    public static ModularUI create(ControllerBlockEntity controller, Player player) {
        int slots = controller.threadSlots();
        int rows = Math.max(1, slots);
        int height = PADDING * 2 + LINE * 3 + rows * (ROW + 2) + 4;

        UIElement root = new UIElement()
            .layout(layout -> layout
                .width(WIDTH)
                .height(height)
                .flexDirection(FlexDirection.COLUMN)
                .paddingAll(PADDING)
                .gapAll(2))
            .addClass("panel_bg");

        root.addChild(title(controller));
        root.addChild(statusLine(controller));
        root.addChild(portsLine(controller));
        root.addChild(threadList(controller, slots));

        return new ModularUI(
            UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))),
            player
        );
    }

    private static Label title(ControllerBlockEntity controller) {
        Label label = new Label();
        label.setText(controller.getBlockState().getBlock().getName());
        label.layout(layout -> layout.widthPercent(100).height(LINE));
        return label;
    }

    private static Label statusLine(ControllerBlockEntity controller) {
        Label label = new Label();
        label.bindDataSource(SupplierDataSource.of(() -> {
            String type = controller.machineType();
            if (type.isEmpty()) {
                return Component.translatable("gui.noway_core.controller.unformed");
            }
            String variants = controller.variantText();
            return Component.translatable(
                "gui.noway_core.controller.status", type, variants.isEmpty() ? "-" : variants
            );
        }));
        label.layout(layout -> layout.widthPercent(100).height(LINE));
        return label;
    }

    private static Label portsLine(ControllerBlockEntity controller) {
        Label label = new Label();
        label.bindDataSource(SupplierDataSource.of(() -> Component.translatable(
            "gui.noway_core.controller.ports", controller.threadSlots(), controller.parallelSlots()
        )));
        label.layout(layout -> layout.widthPercent(100).height(LINE));
        return label;
    }

    private static UIElement threadList(ControllerBlockEntity controller, int slots) {
        UIElement list = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flex(1)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(2));
        if (slots <= 0) {
            Label empty = new Label();
            empty.setText(Component.translatable("gui.noway_core.controller.idle"));
            empty.layout(layout -> layout.widthPercent(100).height(LINE));
            list.addChild(empty);
            return list;
        }
        for (int slot = 0; slot < slots; slot++) {
            list.addChild(threadRow(controller, slot));
        }
        return list;
    }

    private static UIElement threadRow(ControllerBlockEntity controller, int slot) {
        UIElement row = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .height(ROW)
            .flexDirection(FlexDirection.ROW)
            .alignItems(AlignItems.CENTER)
            .gapAll(3));

        Label index = new Label();
        index.setText(Component.translatable("gui.noway_core.controller.thread", slot));
        index.layout(layout -> layout.width(24).height(LINE));
        index.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));

        ProgressBar bar = new ProgressBar();
        bar.setRange(0.0F, 1.0F);
        bar.bindDataSource(SupplierDataSource.of(() -> controller.progressAt(slot)));
        bar.layout(layout -> layout.flex(1));

        Label status = new Label();
        status.bindDataSource(SupplierDataSource.of(() -> controller.statusAt(slot)));
        status.layout(layout -> layout.width(110).height(LINE));

        row.addChildren(index, bar, status);
        return row;
    }
}
