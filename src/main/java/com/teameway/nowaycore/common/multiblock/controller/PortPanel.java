package com.teameway.nowaycore.common.multiblock.controller;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.port.PortKind;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * 端口信息栏
 *
 * <p>列出结构里所有端口，一张卡片一个。拖动由 {@link WindowDragHelper#setDragMove} 接管，
 * 它把屏幕坐标下的位移换算进元素本地坐标系，父元素带缩放或偏移时也不会跑偏；
 * 手写换算容易漏掉这层变换，表现为面板跟不上鼠标。</p>
 *
 * <p>位置使用绝对坐标，面板才能被拖离原位。</p>
 */
public class PortPanel {
    /**
     * 滚动条与内容区的瘦身样式
     *
     * <p>主题默认纵向滚动条 5 像素、内容区内边距 3 像素，合计吃掉 16 像素；
     * 面板只有 160 出头宽，压到 4 与 1 之后卡片才有足够宽度。</p>
     */
    private static final String SCROLL_STYLES = """
        scroller-vertical:host {
          width: 4;
        }
        .__scroller_view_view-port__ {
          padding-all: 1;
          background: empty;
        }
        """;

    /**
     * 卡片高度：两行文字加内边距，等级放不下，挪进了悬浮提示。
     */
    private static final int CARD_HEIGHT = 24;
    private static final int CARD_SPACING = 2;
    private static final int PADDING = 3;
    private static final int TITLE_HEIGHT = 12;
    private static final int LINE = 10;
    private static final int PANEL_WIDTH = 162;
    /**
     * 面板最大高度，超出时内部滚动。
     */
    private static final int MAX_HEIGHT = 150;

    private PortPanel() {
    }

    /**
     * 打开指定端口界面
     *
     * <p>客户端不能直接调用 {@code BlockUIMenuType.openUI}，它只接受 {@code ServerPlayer}，
     * 因此把坐标发回服务端代为打开。</p>
     *
     * @param pos 端口位置
     */
    @RPCPacket(modId = NoWayCore.MOD_ID, value = "open_port")
    private static void openPort(RPCSender sender, BlockPos pos) {
        if (sender.asPlayer() instanceof ServerPlayer player) {
            BlockUIMenuType.openUI(player, pos);
        }
    }

    /**
     * 构建端口信息栏
     *
     * @param controller 控制器方块实体
     * @return 面板元素，没有端口时返回 {@code null}
     */
    public static UIElement create(ControllerBlockEntity controller) {
        int count = controller.portCount();
        if (count == 0) {
            return null;
        }
        int bodyHeight = count * CARD_HEIGHT + (count - 1) * CARD_SPACING;
        int height = Math.min(MAX_HEIGHT, TITLE_HEIGHT + bodyHeight + PADDING * 2);

        UIElement panel = new UIElement()
            .layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                // 起点在开关按钮正下方，左边缘取窄条宽度，面板紧贴按钮右侧展开，横向不压住按钮
                .left(ControllerLayout.RAIL_WIDTH)
                .top(ControllerLayout.PADDING + ControllerLayout.TOGGLE_SIZE + ControllerLayout.TOGGLE_GAP)
                .width(PANEL_WIDTH)
                .height(height)
                .flexDirection(FlexDirection.COLUMN)
                .paddingAll(PADDING))
            .addClass("port-panel");

        UIElement title = new UIElement()
            .layout(layout -> layout
                .widthPercent(100)
                .height(TITLE_HEIGHT)
                .flexDirection(FlexDirection.ROW))
            .addClass("port-panel-title");
        Label titleText = new Label();
        titleText.setText(Component.translatable("gui.noway_core.port_panel.title", count));
        titleText.layout(layout -> layout.widthPercent(100).height(TITLE_HEIGHT));
        title.addChild(titleText);
        panel.addChild(title);

        // 只有标题栏能拖动，卡片自己还要接收点击，整块都能拖会与点击冲突。
        WindowDragHelper.setDragMove(title, panel, null, null);
        // 助手本身不夹边界，拖过头会把面板留在可视区外；这个监听排在它之后，只需把越界维度拉回。
        title.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, event -> clamp(panel, height));

        // 卡片放进滚动视图，端口数超出可见高度时不会被裁掉
        UIElement list = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(0));
        for (int index = 0; index < count; index++) {
            list.addChild(card(controller, index));
        }
        list.layout(layout -> layout.widthPercent(100));
        ScrollerView scroll = new ScrollerView();
        scroll.scrollerStyle(style -> style
            .mode(ScrollerMode.VERTICAL)
            .verticalScrollDisplay(ScrollDisplay.AUTO));
        scroll.addScrollViewChild(list);
        scroll.layout(layout -> layout.widthPercent(100).flex(1));
        // 瘦身样式只挂在面板子树上，不影响别处
        scroll.addLocalStylesheet(SCROLL_STYLES);
        panel.addChild(scroll);
        return panel;
    }

    /**
     * 把面板拉回可视区
     *
     * <p>{@code getLayoutX/Y} 才是 {@code left/top} 的当前值，{@code getPositionX/Y} 还叠着父元素偏移，
     * 拿它写 {@code left} 会越挪越偏。</p>
     *
     * @param panel  面板
     * @param height 面板高度
     */
    private static void clamp(UIElement panel, int height) {
        var mui = panel.getModularUI();
        if (mui == null) {
            return;
        }
        float currentX = panel.getLayoutX();
        float currentY = panel.getLayoutY();
        // 上限按整个界面算，下限留出窄条，否则面板会压住开关按钮
        float maxX = Math.max(0.0F, mui.getScreenWidth() - PANEL_WIDTH);
        float maxY = Math.max(0.0F, mui.getScreenHeight() - height);
        float clampedX = Mth.clamp(currentX, ControllerLayout.RAIL_WIDTH, maxX);
        float clampedY = Mth.clamp(currentY, 0.0F, maxY);
        if (clampedX == currentX && clampedY == currentY) {
            return;
        }
        panel.layout(layout -> layout.left(clampedX).top(clampedY));
    }

    /**
     * 让一行文字占满卡片宽度、超长时截断
     *
     * <p>{@code adaptiveWidth} 必须关闭，否则文本宽度会以 IMPORTANT 优先级写成元素宽度，
     * 元素撑大后溢出卡片，{@code HIDE} 不再生效。</p>
     *
     * @param label 文本元素
     */
    private static void clip(Label label) {
        label.textStyle(style -> style
            .adaptiveWidth(false)
            .textWrap(TextWrap.HIDE));
        label.setOverflowVisible(false);
        label.layout(layout -> layout.widthPercent(100).height(LINE));
    }

    /**
     * 一张端口卡片
     *
     * <p>第一行是端口种类，第二行是坐标；悬浮提示说明如何打开它的界面。</p>
     *
     * @param controller 控制器
     * @param index      端口序号
     * @return 卡片元素
     */
    private static UIElement card(ControllerBlockEntity controller, int index) {
        BlockPos pos = controller.portPosition(index);
        PortKind kind = controller.portKind(index);

        // 与线程卡片同一套排法：纵向两行，名称一行、坐标一行，各行独享整宽，避免横向争宽度导致溢出。
        UIElement card = new UIElement()
            .layout(layout -> layout
                .widthPercent(100)
                .height(CARD_HEIGHT)
                .flexDirection(FlexDirection.COLUMN)
                .paddingAll(2)
                .gapAll(0)
                .marginBottom(CARD_SPACING))
            .addClass("port-panel-card");

        Label name = new Label();
        name.setText(Component.translatable("gui.noway_core.port_kind." + kind.getSerializedName()));
        clip(name);

        Label where = new Label();
        // 坐标之间不留空格，可省 12 像素宽度
        where.setText(Component.literal(pos.getX() + "," + pos.getY() + "," + pos.getZ()));
        clip(where);

        card.addChildren(name, where);
        // 等级与操作提示放进悬浮提示，卡片一行放不下种类、等级与坐标
        card.style(style -> style.tooltips(
            Component.translatable("gui.noway_core.port_panel.tier", controller.portTier(index)),
            Component.translatable("gui.noway_core.port_panel.tooltip")
        ));
        card.addEventListener(
            UIEvents.CLICK, event -> {
                if (event.isShiftDown()) {
                    RPCPacketDistributor.rpcToServer("open_port", pos);
                    event.stopPropagation();
                }
            }
        );
        return card;
    }

}
