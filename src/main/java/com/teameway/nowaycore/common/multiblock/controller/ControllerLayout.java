package com.teameway.nowaycore.common.multiblock.controller;

/**
 * 控制器界面度量
 *
 * <p>整页固定 16:9（{@value #WIDTH}×{@value #HEIGHT}），内容区按 9:6 分成左右两栏：
 * {@link #LEFT_WIDTH} 占九份、{@link #RIGHT_WIDTH} 占六份，两者相加正好等于 {@link #CONTENT_WIDTH}，
 * 不需要再留间隙，所以无论在哪个分辨率下分割位置都落在同一个比例上。</p>
 *
 * <p>两侧都用纵向滚动承载可能溢出的内容：线程数由端口决定，配方原料条数由配方决定，两者都没有上限，
 * 固定高度加滚动比撑大窗口稳妥，卡片与槽位的尺寸也不会随内容多少而变形。</p>
 */
public class ControllerLayout {
    /**
     * 面板宽度，16 的整数倍，配合 {@link #HEIGHT} 得到精确的 16:9。
     */
    public static final int WIDTH = 400;
    /**
     * 面板高度。
     */
    public static final int HEIGHT = 225;
    /**
     * {@code panel_bg} 施加的内边距。
     */
    public static final int PADDING = 5;
    /**
     * 内边距之内的宽度。
     */
    public static final int CONTENT_WIDTH = WIDTH - PADDING * 2;
    /**
     * 左栏宽度，占内容区的九份。
     */
    public static final int LEFT_WIDTH = CONTENT_WIDTH * 9 / 15;
    /**
     * 右栏宽度，占内容区的六份。
     */
    public static final int RIGHT_WIDTH = CONTENT_WIDTH - LEFT_WIDTH;
    /**
     * 一行文本的高度，与 {@code Label} 的默认行高一致。
     */
    public static final int LINE = 10;
    /**
     * 相邻信息行之间的间距。
     */
    public static final int LINE_GAP = 1;
    /**
     * 线程卡片的内边距。
     */
    public static final int CARD_PADDING = 2;
    /**
     * 卡片内相邻行的间距。
     */
    public static final int CARD_GAP = 2;
    /**
     * 卡片之间的间距。
     */
    public static final int CARD_SPACING = 2;
    /**
     * 卡片高度：上下内边距、三行文字与两道行距
     *
     * <p>三行依次是线程标题、配方 ID、并行与运行进度，进度用文字而不是进度条：
     * 主题里的进度条靠贴图边框绘制，压到卡片能留出的高度后边框就吃满了，填充部分反而看不见。</p>
     */
    public static final int CARD_HEIGHT = CARD_PADDING * 2 + LINE * 3 + CARD_GAP * 2;
    /**
     * 槽位边长，与 {@code ItemSlot} 的默认尺寸一致。
     */
    public static final int SLOT = 18;

    private ControllerLayout() {
    }
}
