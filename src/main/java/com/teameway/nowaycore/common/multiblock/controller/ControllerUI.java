package com.teameway.nowaycore.common.multiblock.controller;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.teameway.nowaycore.gui.NowayTheme;
import com.teameway.nowaycore.recipe.view.RecipeView;
import com.teameway.nowaycore.util.NumberFormatUtil;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 控制器界面
 *
 * <p>框架是左右两块，中间一道竖线。左块与玩家物品栏同宽，自上而下是机器信息块、
 * 一道横线、配方说明、又一道横线、玩家物品栏，每段之间都隔开；右块与整块内容区等高，
 * 是等高卡片纵向排列的线程列表，点击后左块切到该线程的配方。右块比左块高，
 * 因为它跨过了物品栏所在的那一段。</p>
 *
 * <p>框架左边框之外另有一条窄条，顶端放开关按钮，用来唤出端口信息栏。按钮不放进
 * {@code panel_bg} 里，是为了不跟面板内容抢位置；端口信息栏本身是可拖动的浮窗，默认隐藏。</p>
 *
 * <p>配方不用槽位而用文字：逐条列出「数量 × 名称」与它的平均速率，
 * 比一排图标多出数量与吞吐量两项信息，也能一眼看出哪一项是瓶颈。</p>
 *
 * <p>两侧都放在 {@link ScrollerView} 里：线程数由端口决定、配方原料条数由配方决定，两者都没有上限，
 * 固定高度加滚动比撑大窗口稳妥，行高也不会随内容多少变形。</p>
 *
 * <p>所有数据都来自控制器方块实体上同步过的字段，配方内容由客户端已有的配方索引反查，
 * 因此两端跑的是同一份构建代码，界面本身不持有服务端对象。</p>
 */
public class ControllerUI {
    /**
     * 局部样式：只压缩滚动条，配色全部来自 {@code lss/dark_paper.lss}
     *
     * <p>卡片三态与分隔线已写进主题文件，这里不再重复；同一份规则写两处，
     * 改主题时忘了一边就会半边生效。</p>
     *
     * <p>滚动条必须改窄：主题给纵向滚动条 5 像素、内容区再留 3 像素内边距，
     * 两块加起来吃掉 16 像素，而右栏总共才
     * {@value com.teameway.nowaycore.common.multiblock.controller.ControllerLayout#RIGHT_WIDTH} 像素。
     * 这里的数值与 {@code ControllerLayout.SCROLLER_WIDTH}、{@code VIEWPORT_PADDING} 是同一份事实，
     * 改一处必须改另一处。</p>
     */
    private static final String STYLES = """
        scroller-vertical:host {
          width: 4;
        }
        .__scroller_view_view-port__ {
          padding-all: 1;
          background: empty;
        }
        """;

    private ControllerUI() {
    }

    /**
     * 构建控制器界面
     *
     * @param controller 控制器方块实体
     * @param player     打开界面的玩家
     * @return 可显示的界面
     */
    public static ModularUI create(ControllerBlockEntity controller, Player player) {
        View view = new View(controller);

        // 外层容器只负责摆「左侧按钮窄条 + 面板」两段，自己不画背景；
        // 面板才是那块 panel_bg，因此按钮站在框架左边框的外侧
        UIElement root = new UIElement()
            .layout(layout -> layout
                .width(ControllerLayout.OUTER_WIDTH)
                .height(ControllerLayout.HEIGHT)
                .flexDirection(FlexDirection.ROW)
                .gapAll(0));
        UIElement frame = new UIElement()
            .layout(layout -> layout
                .width(ControllerLayout.WIDTH)
                .heightPercent(100)
                .flexDirection(FlexDirection.ROW)
                .paddingAll(ControllerLayout.PADDING)
                // panel_bg 主题规则自带 gap-all，会把两栏顶出内容区；左右两块要紧贴
                // 各自一半宽度，这里显式清零
                .gapAll(0))
            .addClass("panel_bg")
            .addLocalStylesheet(STYLES);
        // 左栏自身再分上下：上面是机器信息与配方，下面是物品栏，中间用横线隔开。
        // 右栏与整块内容区等高，于是它贯穿信息区与物品栏，不会被物品栏截断。
        UIElement leftSide = new UIElement()
            .layout(layout -> layout
                .width(ControllerLayout.LEFT_WIDTH)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(0));
        // 线与两侧的栏各留 DIVIDER_GAP，线本身只占 1 像素：分区感来自留白，不是来自线有多粗
        leftSide.addChildren(
            view.leftColumn(),
            horizontalDivider(),
            view.inventory()
        );
        frame.addChildren(
            leftSide,
            verticalDivider(),
            view.rightColumn()
        );
        root.addChildren(view.rail(), frame);
        // 端口面板浮在最上层：它要能被拖到任何位置，必须晚于其他内容加入，
        // 否则会被后加入的元素盖住。默认隐藏，点开关按钮才出现
        UIElement portPanel = PortPanel.create(controller);
        if (portPanel != null) {
            root.addChild(portPanel);
            view.attachPanel(portPanel);
        }
        // 先按当前同步值铺一次，界面打开的第一帧就不是空的；之后由 tick 负责跟随变化
        view.refresh();
        root.addEventListener(UIEvents.TICK, event -> view.refresh());

        return new ModularUI(NowayTheme.of(root), player);
    }

    /**
     * 左右两块之间的竖线
     *
     * <p>用一个空元素而不是给某一边加边框：边框会吃掉那一边的可用宽度，
     * 让滚动区的实际内容宽度与 {@code ControllerLayout} 里算好的数值对不上。</p>
     *
     * <p>左右各留 {@link ControllerLayout#DIVIDER_GAP}：线本身只有 1 像素，
     * 分区感来自它与两栏之间的距离，而不是线有多粗。</p>
     *
     * @return 竖线元素
     */
    private static UIElement verticalDivider() {
        return new UIElement()
            .layout(layout -> layout
                // 上下留白交给 margin 而不是给线加高：线贯穿整块内容区，只把左右撑开
                .width(ControllerLayout.DIVIDER_WIDTH)
                .heightPercent(100)
                .marginLeft(ControllerLayout.DIVIDER_GAP)
                .marginRight(ControllerLayout.DIVIDER_GAP))
            .addClass("controller-divider");
    }

    /**
     * 左栏内部的横线
     *
     * <p>与竖线共用同一个类，主题里那条规则只画底色，不关心方向。
     * 上下各留 {@link ControllerLayout#DIVIDER_GAP}，与竖线的左右留白一致。</p>
     *
     * @return 横线元素
     */
    private static UIElement horizontalDivider() {
        return new UIElement()
            .layout(layout -> layout
                .widthPercent(100)
                .height(ControllerLayout.DIVIDER_HEIGHT)
                .marginTop(ControllerLayout.DIVIDER_GAP)
                .marginBottom(ControllerLayout.DIVIDER_GAP))
            .addClass("controller-divider");
    }

    /**
     * 结构状态
     *
     * <p>由同步字段推导，不额外占一个字段：没有机器类型是未成型，有配方在跑是工作中，其余为空闲。</p>
     *
     * @param controller 控制器方块实体
     * @return 状态文本
     */
    private static Component stateText(ControllerBlockEntity controller) {
        if (controller.machineType().isEmpty()) {
            return Component.translatable("gui.noway_core.controller.state.unformed");
        }
        return Component.translatable(
            controller.working()
            ? "gui.noway_core.controller.state.working"
            : "gui.noway_core.controller.state.idle"
        );
    }

    /**
     * 机器形态
     *
     * <p>同步过来的是逗号分隔的完整 ID，这里只留路径部分：同一台机器的形态必然同命名空间，
     * 重复显示命名空间只会挤占宽度。</p>
     *
     * @param controller 控制器方块实体
     * @return 形态文本
     */
    private static Component variantsText(ControllerBlockEntity controller) {
        String raw = controller.variantText();
        if (raw.isEmpty()) {
            return Component.translatable("gui.noway_core.controller.info.variants", "-");
        }
        StringBuilder text = new StringBuilder();
        for (String part : raw.split(", ")) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(path(part));
        }
        return Component.translatable("gui.noway_core.controller.info.variants", text.toString());
    }

    /**
     * @param id 完整 ID
     * @return 去掉命名空间后的路径，解析失败时原样返回
     */
    private static String path(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed == null ? id : parsed.getPath();
    }

    /**
     * @param progress 0~1 的进度
     * @return 百分数文本，不带百分号
     */
    private static String percent(float progress) {
        return String.format(Locale.ROOT, "%.0f", progress * 100.0F);
    }

    private static Label bound(Supplier<Component> supplier) {
        Label label = new Label();
        label.bindDataSource(SupplierDataSource.of(supplier));
        return label;
    }

    private static Label line(Label label) {
        label.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));
        return label;
    }

    /**
     * 建一个只有纵向滚动的视图
     *
     * <p>宽度由调用方决定，这里不代设：配方预览按绝对坐标摆放，需要确切的像素宽度；
     * 线程卡片则是流式的，用百分比撑满即可。混在一处设置会互相覆盖。</p>
     *
     * @param content 内容元素
     * @return 滚动视图
     */
    private static ScrollerView scroll(UIElement content) {
        ScrollerView view = new ScrollerView();
        view.scrollerStyle(style -> style
            .mode(ScrollerMode.VERTICAL)
            .verticalScrollDisplay(ScrollDisplay.AUTO));
        view.addScrollViewChild(content);
        return view;
    }

    /**
     * 界面视图
     *
     * <p>持有两侧的引用与选中状态。选中项是纯客户端的展示状态，不必同步：别的玩家看到的是
     * 他们自己点开的线程，服务端也不参与渲染。</p>
     */
    private static final class View {
        private final ControllerBlockEntity controller;
        private final List<UIElement> cards = new ArrayList<>();
        private final UIElement recipeBody = new UIElement().layout(layout -> layout
            .width(ControllerLayout.LEFT_CONTENT)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(ControllerLayout.LINE_GAP));
        // 卡片是流式排布，用百分比撑满滚动视图里可用的宽度
        private final UIElement cardList = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(ControllerLayout.CARD_SPACING));
        private final ScrollerView recipeScroll;
        private final ScrollerView threadScroll;
        private int selected = -1;
        private int slots = -1;
        private String rendered = "";
        private @Nullable UIElement panel;

        private View(ControllerBlockEntity controller) {
            this.controller = controller;
            this.recipeScroll = scroll(this.recipeBody);
            this.threadScroll = scroll(this.cardList);
        }

        /**
         * @param amount 时间，单位 tick
         * @return 缩写后的 tick 数
         */
        private static String ticks(long amount) {
            return NumberFormatUtil.compact(amount);
        }

        /**
         * @param amount 时间，单位 tick
         * @return 缩写后的秒数
         */
        private static String seconds(long amount) {
            return NumberFormatUtil.compact(Math.round(amount / 20.0F));
        }

        /**
         * 让一行文字在卡片宽度内截断
         *
         * <p>右栏只有 {@value ControllerLayout#RIGHT_CONTENT} 像素，配方 ID 稍长就会溢出卡片。
         * {@code HIDE} 直接丢弃超出部分，比换行安全：换行会撑高卡片、挤乱上下行。</p>
         *
         * @param label 文本元素
         */
        private static void clip(Label label) {
            label.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));
            label.textStyle(style -> style.textWrap(TextWrap.HIDE));
            label.setOverflowVisible(false);
        }

        /**
         * 左栏左侧的窄条，只放一个开关按钮
         *
         * <p>按钮是正方形，点击切换端口信息栏的可见性。窄条宽度固定，
         * 面板宽度因此不随按钮状态变化。</p>
         *
         * @return 窄条元素
         */
        private UIElement rail() {
            // 顶端对齐：按钮要与面板上边缘齐平，而不是垂直居中
            UIElement rail = new UIElement().layout(layout -> layout
                .width(ControllerLayout.RAIL_WIDTH)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.CENTER)
                .paddingTop(ControllerLayout.PADDING)
                .gapAll(0));

            Button toggle = new Button();
            toggle.layout(layout -> layout
                .width(ControllerLayout.TOGGLE_SIZE)
                .height(ControllerLayout.TOGGLE_SIZE));
            toggle.setText(Component.literal("P"));
            toggle.style(style -> style.tooltips(
                Component.translatable("gui.noway_core.port_panel.toggle")
            ));
            toggle.setOnClick(event -> this.togglePanel());
            rail.addChild(toggle);
            return rail;
        }

        /**
         * 记下端口面板，供开关按钮控制
         *
         * @param panel 端口面板
         */
        private void attachPanel(UIElement panel) {
            this.panel = panel;
            // 默认关闭：端口列表是按需查看的辅助信息，一开界面就浮一层在上面会挡内容
            panel.setVisible(false);
        }

        /**
         * 切换端口面板的可见性
         *
         * <p>用可见性而不是增删元素：面板里有滚动状态与拖动位置，重建会把它弹回原位。</p>
         */
        private void togglePanel() {
            if (this.panel == null) {
                return;
            }
            boolean visible = this.panel.isVisible();
            this.panel.setVisible(!visible);
        }

        /**
         * 左栏上部：机器信息、横线、配方说明
         *
         * <p>高度固定为 {@link ControllerLayout#INFO_HEIGHT}，因为它下面还要接横线与物品栏，
         * 三段之和才是整块内容区的高度。</p>
         *
         * <p>机器信息五行的行数和行高都是定值，可以直接给它 {@link ControllerLayout#MACHINE_HEIGHT}；
         * 配方区再吃掉剩下的弹性空间，两边不必互相猜高度。</p>
         *
         * @return 信息栏元素
         */
        private UIElement leftColumn() {
            UIElement left = new UIElement().layout(layout -> layout
                .width(ControllerLayout.LEFT_WIDTH)
                .height(ControllerLayout.INFO_HEIGHT)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(0));

            left.addChildren(this.machineInfo(), horizontalDivider());
            this.recipeScroll.layout(layout -> layout
                .width(ControllerLayout.LEFT_WIDTH)
                .flex(1));
            left.addChild(this.recipeScroll);
            return left;
        }

        /**
         * 每 tick 校验一次：线程数变了重建卡片，选中线程换了配方就重画左栏
         *
         * <p>两件事都只在比对结果不同时才动手，滚动位置不会被无谓地重置。</p>
         */
        private void refresh() {
            // 服务端也会建一份同构界面（菜单在两端的构造路径相同），但只有客户端渲染，
            // 重建卡片与配方信息在服务端只是白费力气
            if (LDLib2.isServer()) {
                return;
            }
            int current = this.controller.threadSlots();
            if (current != this.slots) {
                this.rebuildCards(current);
            }
            // 线程数减少后原先选中的序号可能已经不存在，选中态必须跟着失效，
            // 否则左栏会继续显示一个已经不存在的线程的配方
            if (this.selected >= this.slots) {
                this.selected = -1;
                this.highlight();
            }
            String signature = this.controller.machineType() + '|' + this.selected + '|' + this.recipeId();
            if (!signature.equals(this.rendered)) {
                this.rendered = signature;
                this.rebuildRecipe();
            }
        }

        private void rebuildCards(int count) {
            this.slots = count;
            this.cards.clear();
            this.cardList.clearAllChildren();
            if (count == 0) {
                this.cardList.addChild(text("gui.noway_core.controller.thread.none"));
            }
            for (int slot = 0; slot < count; slot++) {
                UIElement card = this.card(slot);
                this.cards.add(card);
                this.cardList.addChild(card);
            }
            this.highlight();
        }

        /**
         * 机器信息：名称、状态、形态、并行、线程
         *
         * @return 信息块元素
         */
        private UIElement machineInfo() {
            UIElement info = new UIElement().layout(layout -> layout
                .width(ControllerLayout.LEFT_WIDTH)
                .height(ControllerLayout.MACHINE_HEIGHT)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(ControllerLayout.LINE_GAP));

            Label title = new Label();
            title.setText(this.controller.getBlockState().getBlock().getName());
            info.addChild(line(title));
            info.addChild(line(bound(() -> stateText(this.controller))));
            info.addChild(line(bound(() -> variantsText(this.controller))));
            info.addChild(line(bound(() -> Component.translatable(
                "gui.noway_core.controller.info.parallel", this.controller.parallelSlots()
            ))));
            info.addChild(line(bound(() -> Component.translatable(
                "gui.noway_core.controller.info.threads", this.controller.threadSlots()
            ))));
            return info;
        }

        /**
         * 右栏：线程卡片
         *
         * <p>与整块内容区等高，跨过左侧的信息栏与物品栏两段，因此卡片列表比左栏更高。</p>
         *
         * @return 线程栏元素
         */
        private UIElement rightColumn() {
            UIElement right = new UIElement().layout(layout -> layout
                .width(ControllerLayout.RIGHT_WIDTH)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN));
            right.addChild(this.threadScroll
                .layout(layout -> layout.width(ControllerLayout.RIGHT_WIDTH).heightPercent(100)));
            return right;
        }

        /**
         * 把 tick 换算成秒后缩写
         *
         * <p>20 tick 为一秒。秒数同样走 {@link NumberFormatUtil#compact(long)}：
         * 长耗时配方的秒数会到五位数，不缩写就会把整行撑出卡片。</p>
         *
         * @param ticks 时间，单位 tick
         * @return 秒数文本
         */

        /**
         * 玩家物品栏
         *
         * <p>{@code InventorySlots} 自己会在 {@code MUI_CHANGED} 时把九个一行的槽位绑到玩家背包上，
         * 所以这里只需要给它尺寸与位置。它内部已经带了三行主背包与一行快捷栏，高度固定，
         * 不能压缩，否则槽位会被裁掉。</p>
         *
         * <p>宽度与左栏一致，因此不需要居中，两者天然对齐。</p>
         *
         * @return 物品栏元素
         */
        private UIElement inventory() {
            UIElement slots = new InventorySlots();
            slots.layout(layout -> layout
                .width(ControllerLayout.INVENTORY_WIDTH)
                .height(ControllerLayout.INVENTORY_HEIGHT));
            return slots;
        }

        private UIElement card(int slot) {
            UIElement card = new UIElement()
                .layout(layout -> layout
                    .widthPercent(100)
                    .height(ControllerLayout.CARD_HEIGHT)
                    .flexDirection(FlexDirection.COLUMN)
                    .paddingAll(ControllerLayout.CARD_PADDING)
                    .gapAll(ControllerLayout.CARD_GAP))
                .addClass("controller-thread-card");
            card.addEventListener(UIEvents.CLICK, event -> this.select(slot));

            Label title = new Label();
            title.setText(Component.translatable("gui.noway_core.controller.thread.title", slot));
            clip(title);

            Label recipe = bound(() -> Component.translatable(
                "gui.noway_core.controller.thread.recipe", this.recipeText(slot)
            ));
            clip(recipe);

            Label parallel = bound(() -> Component.translatable(
                "gui.noway_core.controller.thread.parallel", this.controller.batchAt(slot)
            ));
            clip(parallel);

            Label progress = bound(() -> Component.translatable(
                "gui.noway_core.controller.thread.progress", percent(this.controller.progressAt(slot))
            ));
            clip(progress);

            Label tickRow = bound(() -> Component.translatable(
                "gui.noway_core.controller.thread.ticks",
                ticks(this.elapsed(slot)), ticks(this.controller.durationAt(slot))
            ));
            clip(tickRow);

            Label secondRow = bound(() -> Component.translatable(
                "gui.noway_core.controller.thread.seconds",
                seconds(this.elapsed(slot)), seconds(this.controller.durationAt(slot))
            ));
            clip(secondRow);

            card.addChildren(title, recipe, parallel, progress, tickRow, secondRow);
            return card;
        }

        /**
         * 当前线程已经跑了多少 tick
         *
         * <p>进度是 0~1 的比例，乘上周期长度才是 tick 数。取整而不是保留小数：
         * 卡片这一行要同时放 tick 与秒两个值，{@code 9.0} 里的 {@code .0} 只是占宽。</p>
         *
         * @param slot 线程序号
         * @return 已完成 tick 数
         */
        private long elapsed(int slot) {
            return Math.max(0L, Math.round(this.controller.progressAt(slot) * this.controller.durationAt(slot)));
        }

        private Component recipeText(int slot) {
            String id = this.controller.recipeIdAt(slot);
            return id.isEmpty()
                   ? Component.translatable("gui.noway_core.controller.state.idle")
                   : Component.literal(path(id));
        }

        private void select(int slot) {
            this.selected = this.selected == slot ? -1 : slot;
            this.highlight();
            this.rendered = "";
        }

        private void highlight() {
            for (int index = 0; index < this.cards.size(); index++) {
                if (index == this.selected) {
                    this.cards.get(index).addClass("controller-thread-selected");
                } else {
                    this.cards.get(index).removeClass("controller-thread-selected");
                }
            }
        }

        private String recipeId() {
            return this.selected < 0 ? "" : this.controller.recipeIdAt(this.selected);
        }

        /**
         * 重建左栏的配方信息
         *
         * <p>配方数据在客户端收到同步之后才有，界面开得更早时查不到，这里显示“未就绪”而不是留空。</p>
         */
        private void rebuildRecipe() {
            this.recipeBody.clearAllChildren();
            if (this.selected < 0) {
                this.recipeBody.addChild(text("gui.noway_core.controller.recipe.hint"));
                this.recipeBody.addChild(text("gui.noway_core.controller.recipe.hint_rate"));
                return;
            }
            if (this.controller.machineType().isEmpty()) {
                this.recipeBody.addChild(text("gui.noway_core.controller.recipe.hint"));
                return;
            }
            String recipeId = this.recipeId();
            if (recipeId.isEmpty()) {
                this.recipeBody.addChild(text("gui.noway_core.controller.state.idle"));
                return;
            }
            RecipeView recipe = RecipeView.find(this.controller.machineType(), recipeId);
            if (recipe == null) {
                this.recipeBody.addChild(text("gui.noway_core.controller.recipe.unavailable"));
                return;
            }
            this.addSection("gui.noway_core.controller.recipe.inputs");
            this.addItems(recipe.itemInputs(), recipe.time());
            this.addFluids(recipe.fluidInputs(), recipe.time());
            this.addSection("gui.noway_core.controller.recipe.outputs");
            this.addItems(recipe.itemOutputs(), recipe.time());
            this.addFluids(recipe.fluidOutputs(), recipe.time());
            this.recipeBody.addChild(text(
                "gui.noway_core.jei.time",
                this.seconds(recipe.time())
            ));
            this.recipeBody.addChild(text("gui.noway_core.jei.energy_total", recipe.energy()));
            this.recipeBody.addChild(text("gui.noway_core.jei.energy_rate", this.power(recipe)));
        }

        /**
         * 小节标题
         *
         * @param key 语言键
         */
        private void addSection(String key) {
            Label label = new Label();
            label.setText(Component.translatable(key));
            label.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));
            this.recipeBody.addChild(label);
        }

        /**
         * 一组物品的逐条说明
         *
         * <p>每条占一行，形如 {@code 192 × 石头}，数量是并行后的总量；速率补在行尾，
         * 这样在窄栏里也能一眼看出「一次要多少」与「每秒吃多少」。</p>
         *
         * @param entries 物品条目
         * @param time    单周期耗时，用于折算速率
         */
        private void addItems(List<RecipeView.ItemEntry> entries, int time) {
            for (RecipeView.ItemEntry entry : entries) {
                long total = entry.amount() * (long) this.batch();
                this.recipeBody.addChild(text(
                    "gui.noway_core.controller.recipe.line_item",
                    NumberFormatUtil.compact(total), entry.name()
                ));
                this.recipeBody.addChild(rate(total, time, false));
            }
        }

        /**
         * 一组流体的逐条说明
         *
         * @param entries 流体条目
         * @param time    单周期耗时，用于折算速率
         */
        private void addFluids(List<RecipeView.FluidEntry> entries, int time) {
            for (RecipeView.FluidEntry entry : entries) {
                long total = entry.amount() * (long) this.batch();
                this.recipeBody.addChild(text(
                    "gui.noway_core.controller.recipe.line_fluid",
                    NumberFormatUtil.fluidVolume(total), entry.name()
                ));
                this.recipeBody.addChild(rate(total, time, true));
            }
        }

        /**
         * 速率行：既给每 tick，也给每秒
         *
         * <p>两个都给是因为单看一个容易误判：tick 数在长耗时配方里会小到看不出来，
         * 秒数在短耗时配方里又会大到失真。</p>
         *
         * @param amount 一个周期的总量
         * @param time   周期耗时，单位 tick
         * @param fluid  是否为流体
         * @return 速率文本
         */
        private Label rate(long amount, int time, boolean fluid) {
            int ticks = Math.max(1, time);
            float perTick = amount / (float) ticks;
            float perSecond = perTick * 20.0F;
            String key = fluid
                         ? "gui.noway_core.controller.recipe.rate_fluid"
                         : "gui.noway_core.controller.recipe.rate_item";
            Label label = text(key, NumberFormatUtil.decimal(perTick), NumberFormatUtil.decimal(perSecond));
            // 速率是补充信息，缩进一档以便与主行区分
            label.layout(layout -> layout
                .widthPercent(100)
                .height(ControllerLayout.LINE)
                .paddingLeft(6));
            return label;
        }

        /**
         * @return 选中线程的并行倍数，未选中时为 1
         */
        private int batch() {
            return Math.max(1, this.selected < 0 ? 1 : this.controller.batchAt(this.selected));
        }

        private Label text(String key) {
            return text(key, new Object[0]);
        }

        /**
         * 生成一行同宽同高的文本
         *
         * @param key  语言键
         * @param args 格式化参数
         * @return 文本元素
         */
        private Label text(String key, Object... args) {
            Label label = new Label();
            label.setText(Component.translatable(key, args));
            label.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));
            return label;
        }

        /**
         * 耗能功率
         *
         * <p>按每 tick 消耗计并向上取整：整除后若显示 0，会与实际有消耗的事实自相矛盾。</p>
         *
         * @param recipe 配方
         * @return 每 tick 消耗，单位 FE/t
         */
        private long power(RecipeView recipe) {
            return recipe.energy() <= 0L
                   ? 0L
                   : Math.ceilDiv(recipe.energy(), Math.max(1, recipe.time()));
        }

        /**
         * @param ticks 耗时，单位 tick
         * @return 秒数文本，保留一位小数
         */
        private String seconds(int ticks) {
            return String.format(Locale.ROOT, "%.1f", ticks / 20.0F);
        }
    }
}
