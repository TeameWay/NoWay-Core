package com.teameway.nowaycore.integration.jei.layout;

/**
 * 结构页排版
 *
 * <p>左上角单格显示选中方块，中部放结构预览，底部横排清单列出全部所需方块；高度固定，方块再多只影响横向滚动。</p>
 */
public record StructureLayout() {
    /**
     * 单例：结构页排版不含可变量。
     */
    public static final StructureLayout INSTANCE = new StructureLayout();

    public static final int PREVIEW_HEIGHT = 108;
    /**
     * 底部方块清单区域高度，含横向滚动条。
     */
    public static final int STRIP_HEIGHT = 24;

    /**
     * @return 页面总高度
     */
    public int height() {
        return JeiLayout.PANEL_PADDING * 2 + PREVIEW_HEIGHT + JeiLayout.BLOCK_GAP + STRIP_HEIGHT;
    }

    /**
     * @return 底部清单区域在内容区内的纵向位置
     */
    public int stripTop() {
        return PREVIEW_HEIGHT + JeiLayout.BLOCK_GAP;
    }
}
