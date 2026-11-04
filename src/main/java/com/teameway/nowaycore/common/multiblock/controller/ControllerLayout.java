package com.teameway.nowaycore.common.multiblock.controller;

/**
 * 控制器界面度量
 *
 * <p>整页横向分三段：左块 {@value #LEFT_WIDTH}，一道 {@value #DIVIDER_WIDTH} 像素的分隔线，
 * 右栏 {@value #RIGHT_WIDTH}。左块与玩家物品栏同宽；纵向上左块是「机器信息 + 配方说明」，
 * 物品栏紧接在它下面；右栏的高度等于这两段之和，也就是整块内容区，
 * 于是右侧那片滚动区从头贯到底，不会被物品栏截断。</p>
 *
 * <p>宽度这么窄，滚动视图自身的滚动条与内边距就显得很贵。{@link ControllerUI} 用局部样式
 * 把滚动条压到 {@value #SCROLLER_WIDTH}、内边距压到 {@value #VIEWPORT_PADDING}
 * （paper 主题原本是 5 与 3），这里的常数与那份样式必须同步修改，
 * 否则测量的可用宽度对不上实际渲染宽度。</p>
 */
public class ControllerLayout {
    /**
     * 物品栏一行的槽位数，与原版一致。
     */
    public static final int INVENTORY_COLUMNS = 9;
    /**
     * 槽位边长，与 {@code ItemSlot} 的默认尺寸一致。
     */
    public static final int SLOT = 18;
    /**
     * {@code InventorySlots} 在自己的快捷栏上方固定的间距。
     */
    public static final int HOTBAR_GAP = 5;
    /**
     * 玩家物品栏的宽度。
     */
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
     * <p>只放线程卡片，每张卡片三行文本。给得比左栏内文更宽是因为卡片要排下配方 ID：
     * 左栏是整句说明，能容忍折行，卡片是一行一条，宽一点才不会被截得太狠。</p>
     */
    public static final int RIGHT_WIDTH = 149;
    /**
     * 左右两块之间竖线的宽度
     *
     * <p>线本身只有 1 像素，留白由 {@link #DIVIDER_GAP} 提供。把线画粗会让它读成一条色带，
     * 而分区靠的是线两侧的距离，不是线本身有多重。</p>
     */
    public static final int DIVIDER_WIDTH = 1;
    /**
     * 左栏内部横线的厚度
     *
     * <p>与竖线分开命名：一个量的是宽、一个量的是高。
     * 合成一个常数会让 {@code .width(DIVIDER)} 与 {@code .height(DIVIDER)}
     * 读起来同样合理而实际含义不同。</p>
     */
    public static final int DIVIDER_HEIGHT = 1;
    /**
     * 端口信息栏开关按钮的边长
     *
     * <p>正方形，放在左栏左侧那一窄条里。做成正方形而不是长条，是因为它只承载一个图标，
     * 横条会让人以为还有文字。</p>
     */
    public static final int TOGGLE_SIZE = 14;
    /**
     * 开关按钮与面板之间的间距
     */
    public static final int TOGGLE_GAP = 2;
    /**
     * 面板左侧那一窄条的宽度，等于按钮宽加两侧留白
     *
     * <p>窄条在 {@code panel_bg} 之外：按钮要站在框架左边框外侧，而不是压在面板里。
     * 因此它不计入 {@link #WIDTH}，而是由 {@link #OUTER_WIDTH} 另行加上。</p>
     */
    public static final int RAIL_WIDTH = TOGGLE_SIZE + TOGGLE_GAP * 2;
    /**
     * 线的单侧留白
     *
     * <p>线与两侧的栏之间各留这么多像素再来画线，因此一道分隔实际占
     * {@code DIVIDER_GAP * 2 + 1} 像素。没有留白时线紧贴文字与槽位边框，
     * 看起来像内容溢出的裁切线而不是分区线。</p>
     */
    public static final int DIVIDER_GAP = 3;
    /**
     * 面板宽度。
     */
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
     * 左栏上部「机器信息 + 横线 + 配方」这一整段的高度
     *
     * <p>它再加上横线与物品栏才是内容区高度，所以比信息本身少算一道线。</p>
     */
    public static final int INFO_HEIGHT = 138;
    /**
     * 一行文本的高度，与 {@code Label} 的默认行高一致。
     */
    public static final int LINE = 10;
    /**
     * 相邻信息行之间的间距。
     */
    public static final int LINE_GAP = 1;
    /**
     * 机器信息区的高度：五行文字与四道行距
     *
     * <p>行数是固定的（名称、状态、形态、并行、线程），因此可以直接算出高度，
     * 让配方区用剩余的弹性空间，而不是把两者的高度都绑在一起。</p>
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
    /**
     * 内边距之内的宽度。
     */
    public static final int CONTENT_WIDTH = WIDTH - PADDING * 2;
    /**
     * 内边距之内的高度。
     */
    public static final int CONTENT_HEIGHT = HEIGHT - PADDING * 2;
    /**
     * 纵向滚动条的宽度，由 {@link ControllerUI} 的局部样式施加。
     */
    public static final int SCROLLER_WIDTH = 4;
    /**
     * 滚动视图内容区的内边距，由 {@link ControllerUI} 的局部样式施加。
     */
    public static final int VIEWPORT_PADDING = 1;
    /**
     * 滚动视图总共吃掉的宽度。
     */
    public static final int SCROLL_CHROME = SCROLLER_WIDTH + VIEWPORT_PADDING * 2;
    /**
     * 左栏内部可用宽度。
     */
    public static final int LEFT_CONTENT = LEFT_WIDTH - SCROLL_CHROME;
    /**
     * 右栏内部可用宽度。
     */
    public static final int RIGHT_CONTENT = RIGHT_WIDTH - SCROLL_CHROME;
    /**
     * 线程卡片的内边距。
     */
    public static final int CARD_PADDING = 2;
    /**
     * 卡片内相邻行的间距。
     */
    public static final int CARD_GAP = 1;
    /**
     * 卡片之间的间距。
     */
    public static final int CARD_SPACING = 2;
    /**
     * 卡片行数：线程标题、配方 ID、并行倍数、进度、已完成 tick、已完成秒数
     */
    public static final int CARD_LINES = 6;
    /**
     * 卡片高度：上下内边距、五行文字与四道行距
     *
     * <p>六行依次是线程标题、配方 ID、并行倍数、进度、tick 进度、秒进度。
     * 原先挤在一行里的三项拆开之后，每项都有自己的位置，也不会因为一行放不下
     * 而在运行过程中被截掉一截。</p>
     */
    public static final int CARD_HEIGHT = CARD_PADDING * 2 + LINE * CARD_LINES + CARD_GAP * (CARD_LINES - 1);
    /**
     * 配方说明里速率行的缩进，用来与主行区分层级。
     */
    public static final int INDENT = 6;

    private ControllerLayout() {
    }
}
