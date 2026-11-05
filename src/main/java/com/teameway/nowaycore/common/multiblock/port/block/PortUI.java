package com.teameway.nowaycore.common.multiblock.port.block;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.teameway.nowaycore.common.multiblock.port.EnergyPort;
import com.teameway.nowaycore.common.multiblock.port.FluidPort;
import com.teameway.nowaycore.common.multiblock.port.ItemPort;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPort;
import com.teameway.nowaycore.common.multiblock.port.block.entity.EnergyPortBlockEntity;
import com.teameway.nowaycore.gui.NowayTheme;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 端口界面
 *
 * <p>各端口共用标题与主体的骨架，物品、流体端口直接绑定端口的处理器。</p>
 */
public class PortUI {
    private static final int PADDING = 5;
    private static final int LINE = 10;
    private static final int SLOT = 18;
    private static final int GAP = 2;
    private static final int MIN_WIDTH = 120;

    private PortUI() {
    }

    /**
     * 构建端口界面
     */
    public static ModularUI create(MultiblockPort port, BlockUIMenuType.BlockUIHolder holder) {
        UIElement root = new UIElement()
            .layout(layout -> layout.flexDirection(FlexDirection.COLUMN).paddingAll(PADDING).gapAll(GAP))
            .addClass("panel_bg");

        Body body = switch (port.portKind()) {
            case ITEM_INPUT, ITEM_OUTPUT -> itemBody((ItemPort) port);
            case FLUID_INPUT, FLUID_OUTPUT -> fluidBody((FluidPort) port);
            case ENERGY_INPUT, ENERGY_OUTPUT -> energyBody((EnergyPort) port);
            case PARALLEL, THREAD -> controlBody(port);
        };

        Label title = new Label();
        title.setText(holder.blockState.getBlock().getName());
        title.layout(layout -> layout.widthPercent(100).height(LINE));

        root.addChildren(title, body.element());
        root.layout(layout -> layout
            .width(Math.max(MIN_WIDTH, body.width() + PADDING * 2))
            .height(PADDING * 2 + LINE + GAP + body.height()));

        return new ModularUI(
            NowayTheme.of(root),
            holder.player
        );
    }

    private static Body itemBody(ItemPort port) {
        ResourceHandler<ItemResource> handler = port.itemHandler();
        int size = handler.size();
        int cols = Math.min(3, Math.max(1, size));
        int rows = Math.max(1, (size + cols - 1) / cols);

        UIElement grid = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN).gapAll(GAP));
        for (int row = 0; row < rows; row++) {
            UIElement line = new UIElement().layout(layout -> layout
                .flexDirection(FlexDirection.ROW)
                .height(SLOT)
                .gapAll(GAP));
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                if (index >= size) {
                    line.addChild(new UIElement().layout(layout -> layout.width(SLOT).height(SLOT)));
                    continue;
                }
                ItemSlot slot = new ItemSlot();
                slot.bind(handler, index);
                line.addChild(slot);
            }
            grid.addChild(line);
        }

        Label info = new Label();
        info.setText(Component.translatable("gui.noway_core.port.slots", size));
        info.layout(layout -> layout.widthPercent(100).height(LINE));

        UIElement body = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN).gapAll(GAP));
        body.addChildren(grid, info);
        return new Body(body, cols * SLOT + Math.max(0, cols - 1) * GAP, rows * SLOT + Math.max(0, rows - 1) * GAP + GAP + LINE);
    }

    private static Body fluidBody(FluidPort port) {
        ResourceHandler<FluidResource> handler = port.fluidHandler();
        FluidSlot slot = new FluidSlot();
        slot.bind(handler, 0);
        slot.layout(layout -> layout.width(24).height(24));

        Label info = new Label();
        info.setText(Component.translatable("gui.noway_core.port.fluid", port.tier().fluidCapacity()));
        info.layout(layout -> layout.widthPercent(100).height(LINE));

        UIElement body = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN).gapAll(GAP));
        body.addChildren(slot, info);
        return new Body(body, 96, 24 + GAP + LINE);
    }

    private static Body energyBody(EnergyPort port) {
        EnergyHandler handler = port.energyHandler();
        long capacity = port.tier().energyCapacity();
        EnergyPortBlockEntity synced = port instanceof EnergyPortBlockEntity energy ? energy : null;

        ProgressBar bar = new ProgressBar();
        bar.setRange(0.0F, 1.0F);
        bar.bindDataSource(SupplierDataSource.of(() -> {
            if (capacity <= 0) {
                return 0.0F;
            }
            long stored = synced != null ? synced.energyStored() : handler.getAmountAsLong();
            return Math.min(1.0F, (float) ((double) stored / (double) capacity));
        }));
        bar.layout(layout -> layout.widthPercent(100));

        Label info = new Label();
        info.bindDataSource(SupplierDataSource.of(() -> {
            long stored = synced != null ? synced.energyStored() : handler.getAmountAsLong();
            return Component.translatable("gui.noway_core.port.energy", stored, capacity);
        }));
        info.layout(layout -> layout.widthPercent(100).height(LINE));

        UIElement body = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN).gapAll(GAP));
        body.addChildren(bar, info);
        return new Body(body, 96, 14 + GAP + LINE);
    }

    private static Body controlBody(MultiblockPort port) {
        Label info = new Label();
        info.setText(port.portKind() == com.teameway.nowaycore.common.multiblock.port.PortKind.THREAD
                     ? Component.translatable("gui.noway_core.port.threads", port.tier().threads())
                     : Component.translatable("gui.noway_core.port.parallels", port.tier().parallels()));
        info.layout(layout -> layout.widthPercent(100).height(LINE));

        UIElement body = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN));
        body.addChild(info);
        return new Body(body, 96, LINE);
    }

    /**
     * 主体尺寸，用于反推窗口大小
     */
    private record Body(UIElement element, int width, int height) {
    }
}