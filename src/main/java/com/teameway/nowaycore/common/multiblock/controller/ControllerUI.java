package com.teameway.nowaycore.common.multiblock.controller;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.teameway.nowaycore.ldlib.slot.CompactItemSlot;
import com.teameway.nowaycore.recipe.view.RecipeView;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 控制器界面
 *
 * <p>整页 16:9，按 9:6 分左右两栏。左栏自上而下是机器名称、结构状态、机器形态、并行数、线程数，
 * 以及当前选中线程的配方信息；右栏是等高卡片纵向排列的线程列表，每张卡片显示该线程的配方、
 * 并行倍数与运行进度，点击后左栏切到该线程的配方。</p>
 *
 * <p>两侧都放在 {@link ScrollerView} 里：线程数由端口决定、配方原料条数由配方决定，两者都没有上限，
 * 固定高度加滚动比撑大窗口稳妥，卡片与槽位的尺寸也不会随内容多少变形。</p>
 *
 * <p>所有数据都来自控制器方块实体上同步过的字段，配方内容由客户端已有的配方索引反查，
 * 因此两端跑的是同一份构建代码，界面本身不持有服务端对象。</p>
 */
public class ControllerUI {
    /**
     * 局部样式：卡片底色、悬浮高亮与选中高亮
     *
     * <p>只作用于本界面的子树，不必为每个主题各写一份规则，也不会影响别处的同名类。</p>
     */
    private static final String STYLES = """
        .controller-thread-card {
          background: built-in(ui-mc:RECT_1);
        }
        .controller-thread-card:hover {
          background: built-in(ui-mc:RECT_3);
        }
        .controller-thread-card.controller-thread-selected {
          background: built-in(ui-mc:RECT_4);
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

        UIElement root = new UIElement()
            .layout(layout -> layout
                .width(ControllerLayout.WIDTH)
                .height(ControllerLayout.HEIGHT)
                .flexDirection(FlexDirection.ROW)
                .paddingAll(ControllerLayout.PADDING)
                // panel_bg 主题规则自带 gap-all，会把两栏顶出内容区；这里显式清零，
                // 使左右宽度之和正好等于内容宽度，分割位置永远落在 3:2 上
                .gapAll(0))
            .addClass("panel_bg")
            .addLocalStylesheet(STYLES);
        root.addChildren(view.leftColumn(), view.rightColumn());
        // 先按当前同步值铺一次，界面打开的第一帧就不是空的；之后由 tick 负责跟随变化
        view.refresh();
        root.addEventListener(UIEvents.TICK, event -> view.refresh());

        return new ModularUI(
            UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))),
            player
        );
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
     * <p>内容按百分比撑满可用宽度，而不是写死像素：纵向滚动条是随内容出现或消失的，
     * 写死宽度会让卡片在滚动条切换时忽宽忽窄。</p>
     *
     * @param content 内容元素
     * @return 滚动视图
     */
    private static ScrollerView scroll(UIElement content) {
        content.layout(layout -> layout.widthPercent(100));
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
            .widthPercent(100)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(ControllerLayout.LINE_GAP));
        private final UIElement cardList = new UIElement().layout(layout -> layout
            .widthPercent(100)
            .flexDirection(FlexDirection.COLUMN)
            .gapAll(ControllerLayout.CARD_SPACING));
        private final ScrollerView recipeScroll;
        private final ScrollerView threadScroll;
        private int selected = -1;
        private int slots = -1;
        private String rendered = "";

        private View(ControllerBlockEntity controller) {
            this.controller = controller;
            this.recipeScroll = scroll(this.recipeBody);
            this.threadScroll = scroll(this.cardList);
        }

        private UIElement leftColumn() {
            UIElement left = new UIElement().layout(layout -> layout
                .width(ControllerLayout.LEFT_WIDTH)
                .heightPercent(100)
                .flexDirection(FlexDirection.COLUMN)
                .gapAll(ControllerLayout.LINE_GAP));

            Label title = new Label();
            title.setText(this.controller.getBlockState().getBlock().getName());
            left.addChild(line(title));
            left.addChild(line(bound(() -> stateText(this.controller))));
            left.addChild(line(bound(() -> variantsText(this.controller))));
            left.addChild(line(bound(() -> Component.translatable(
                "gui.noway_core.controller.info.parallel", this.controller.parallelSlots()
            ))));
            left.addChild(line(bound(() -> Component.translatable(
                "gui.noway_core.controller.info.threads", this.controller.threadSlots()
            ))));
            this.recipeScroll.layout(layout -> layout
                .width(ControllerLayout.LEFT_WIDTH)
                .flex(1));
            left.addChild(this.recipeScroll);
            return left;
        }

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
            title.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));

            Label recipe = bound(() -> Component.translatable(
                "gui.noway_core.controller.thread.recipe", this.recipeText(slot)
            ));
            recipe.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));

            Label meta = bound(() -> Component.translatable(
                "gui.noway_core.controller.thread.meta",
                this.controller.batchAt(slot),
                percent(this.controller.progressAt(slot)),
                Math.max(0, this.controller.progressAt(slot) * this.controller.durationAt(slot)),
                this.controller.durationAt(slot)
            ));
            meta.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));

            card.addChildren(title, recipe, meta);
            return card;
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
            this.recipeBody.addChild(text("gui.noway_core.controller.recipe.inputs"));
            this.addItems(recipe.itemInputs());
            this.addFluids(recipe.fluidInputs());
            this.recipeBody.addChild(entry("gui.noway_core.controller.recipe.energy", recipe.energy()));
            this.recipeBody.addChild(entry("gui.noway_core.controller.recipe.power", this.power(recipe)));
            this.recipeBody.addChild(entry("gui.noway_core.controller.recipe.time", this.seconds(recipe.time())));
            this.recipeBody.addChild(entry("gui.noway_core.controller.recipe.parallel", this.controller.batchAt(this.selected)));
            this.recipeBody.addChild(text("gui.noway_core.controller.recipe.outputs"));
            this.addItems(recipe.itemOutputs());
            this.addFluids(recipe.fluidOutputs());
        }

        /**
         * 摆一组物品格
         *
         * <p>{@link CompactItemSlot} 默认挂的是不绑定菜单的 {@code LocalSlot}，因此这里只当展示格用：
         * 画图标与缩写数量，不接收点击，也不会往容器菜单里塞槽位。</p>
         *
         * @param entries 物品条目
         */
        private void addItems(List<RecipeView.ItemEntry> entries) {
            if (entries.isEmpty()) {
                return;
            }
            UIElement grid = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP));
            for (RecipeView.ItemEntry entry : entries) {
                CompactItemSlot slot = new CompactItemSlot();
                slot.setItem(entry.stack());
                slot.setAmount(entry.amount());
                slot.layout(layout -> layout.width(ControllerLayout.SLOT).height(ControllerLayout.SLOT));
                grid.addChild(slot);
            }
            this.recipeBody.addChild(grid);
        }

        /**
         * 摆一组流体格
         *
         * @param entries 流体条目
         */
        private void addFluids(List<RecipeView.FluidEntry> entries) {
            if (entries.isEmpty()) {
                return;
            }
            UIElement grid = new UIElement().layout(layout -> layout
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP));
            for (RecipeView.FluidEntry entry : entries) {
                FluidSlot slot = new FluidSlot();
                slot.setFluid(entry.stack());
                slot.layout(layout -> layout.width(ControllerLayout.SLOT).height(ControllerLayout.SLOT));
                grid.addChild(slot);
            }
            this.recipeBody.addChild(grid);
        }

        private Label entry(String key, Object argument) {
            Label label = new Label();
            label.setText(Component.translatable(key, argument));
            label.layout(layout -> layout.widthPercent(100).height(ControllerLayout.LINE));
            return label;
        }

        private Label text(String key) {
            Label label = new Label();
            label.setText(Component.translatable(key));
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
