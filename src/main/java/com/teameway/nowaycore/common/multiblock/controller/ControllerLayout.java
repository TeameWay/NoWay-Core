package com.teameway.nowaycore.common.multiblock.controller;

/**
 * 控制器界面度量
 *
 * <p>横向分三段：左块 {@value #LEFT_WIDTH}，一道 {@value #DIVIDER_WIDTH} 像素的分隔线，
 * 右栏 {@value #RIGHT_WIDTH}。左块与玩家物品栏同宽，纵向由机器信息与配方说明接物品栏；
 * 右栏高度等于这两段之和，滚动区从头贯到底。</p>
 *
 * <p>宽度紧张，{@link ControllerUI} 用局部样式把滚动条压到 {@value #SCROLLER_WIDTH}、
 * 内边距压到 {@value #VIEWPORT_PADDING}，这里的常数与那份样式必须同步修改，
 * 否则测量的可用宽度对不上实际渲染宽度。</p>
 */
public class ControllerLayout {
    public static final int INVENTORY_COLUMNS = 9;
    /**
     * 槽位边长，与 {@code ItemSlot} 的默认尺寸一致。
     */
    public static final int SLOT = 18;
    /**
     * {@code InventorySlots} 在快捷栏上方固定的间距。
     */
    public static final int HOTBAR_GAP = 5;
    public static final int INVENTORY_WIDTH = INVENTORY_COLUMNS * SLOT;
    /**
     * 玩家物品栏的高度：三行主背包、一道间距、一行快捷栏。
     */
    public static final int INVENTORY_HEIGHT = 4 * SLOT + HOTBAR_GAP;
    /**
     * {@code panel_bg} 施加的内边距。
     */
    public static final int PADDING = 5;
    /**
     * 左栏宽度，与物品栏对齐。
     */
    public static final int LEFT_WIDTH = INVENTORY_WIDTH;
    /**
     * 右栏宽度。
     *
     * <p>卡片是一行一条的配方 ID，比左栏内文宽一些，否则会被截得太狠。</p>
     */
    public static final int RIGHT_WIDTH = 149;
    /**
     * 左右两块之间竖线的宽度
     *
     * <p>线本身只占 1 像素，留白由 {@link #DIVIDER_GAP} 提供；线画粗会读成一条色带。</p>
     */
    public static final int DIVIDER_WIDTH = 1;
    /**
     * 左栏内部横线的厚度
     *
     * <p>与竖线分开命名：一个量宽，一个量高，合成一个常数会让
     * {@code .width(DIVIDER)} 与 {@code .height(DIVIDER)} 含义混淆。</p>
     */
    public static final int DIVIDER_HEIGHT = 1;
    /**
     * 端口信息栏开关按钮的边长
     *
     * <p>正方形，放在左栏左侧的窄条里。它只承载一个图标，横条会让人以为还有文字。</p>
     */
    public static final int TOGGLE_SIZE = 14;
    public static final int TOGGLE_GAP = 2;
    /**
     * 面板左侧窄条的宽度，等于按钮宽加两侧留白
     *
     * <p>窄条在 {@code panel_bg} 之外，按钮站在框架左边框外侧，因此不计入 {@link #WIDTH}，
     * 由 {@link #OUTER_WIDTH} 另行加上。</p>
     */
    public static final int RAIL_WIDTH = TOGGLE_SIZE + TOGGLE_GAP * 2;
    /**
     * 线的单侧留白
     *
     * <p>一道分隔实际占 {@code DIVIDER_GAP * 2 + 1} 像素。没有留白时线紧贴文字与槽位边框，
     * 看起来像内容溢出的裁切线。</p>
     */
    public static final int DIVIDER_GAP = 3;
    public static final int WIDTH = PADDING * 2 + LEFT_WIDTH
                                    + DIVIDER_GAP * 2 + DIVIDER_WIDTH
                                    + RIGHT_WIDTH;
    /**
     * 含左侧按钮窄条的总宽度
     *
     * <p>界面居中按这个宽度算，否则按钮会把可视内容整体推得偏右。</p>
     */
    public static final int OUTER_WIDTH = RAIL_WIDTH + WIDTH;
    /**
     * 左栏上部机器信息与配方这一段的高度
     *
     * <p>再加上横线与物品栏才是内容区高度，所以比信息本身少算一道线。</p>
     */
    public static final int INFO_HEIGHT = 138;
    /**
     * 一行文本的高度，与 {@code Label} 的默认行高一致。
     */
    public static final int LINE = 10;
    public static final int LINE_GAP = 1;
    /**
     * 机器信息区的高度：五行文字与四道行距
     *
     * <p>五行依次是名称、状态、形态、并行、线程，行数固定，因此可以直接算出高度，
     * 让配方区用剩余的弹性空间。</p>
     */
    public static final int MACHINE_HEIGHT = 5 * LINE + 4 * LINE_GAP;
    /**
     * 面板高度：左栏上段、一道横线的留白与线、物品栏，再加两侧内边距。
     *
     * <p>横线的上下留白是 margin，同样占内容区高度；漏掉它会让左栏比内容区高出
     * {@code DIVIDER_GAP * 2}，把物品栏挤出面板。</p>
     */
    public static final int HEIGHT = PADDING * 2
                                     + INFO_HEIGHT + DIVIDER_GAP * 2 + DIVIDER_HEIGHT
                                     + INVENTORY_HEIGHT;
    public static final int CONTENT_WIDTH = WIDTH - PADDING * 2;
    public static final int CONTENT_HEIGHT = HEIGHT - PADDING * 2;
    /**
     * 纵向滚动条的宽度，由 {@link ControllerUI} 的局部样式施加。
     */
    public static final int SCROLLER_WIDTH = 4;
    /**
     * 滚动视图内容区的内边距，由 {@link ControllerUI} 的局部样式施加。
     */
    public static final int VIEWPORT_PADDING = 1;
    public static final int SCROLL_CHROME = SCROLLER_WIDTH + VIEWPORT_PADDING * 2;
    public static final int LEFT_CONTENT = LEFT_WIDTH - SCROLL_CHROME;
    public static final int RIGHT_CONTENT = RIGHT_WIDTH - SCROLL_CHROME;
    public static final int CARD_PADDING = 2;
    public static final int CARD_GAP = 1;
    public static final int CARD_SPACING = 2;
    /**
     * 卡片行数：线程标题、配方 ID、并行倍数、进度、已完成 tick、已完成秒数
     */
    public static final int CARD_LINES = 6;
    /**
     * 卡片高度：上下内边距、{@link #CARD_LINES} 行文字与相应的行距
     *
     * <p>每项单独一行，不会因为一行放不下而在运行过程中被截掉一截。</p>
     */
    public static final int CARD_HEIGHT = CARD_PADDING * 2 + LINE * CARD_LINES + CARD_GAP * (CARD_LINES - 1);
    /**
     * 配方说明里速率行的缩进，与主行区分层级。
     */
    public static final int INDENT = 6;

    private ControllerLayout() {
    }
}
