package com.teameway.nowaycore.integration.jei.layout;

/**
 * 结构页排版
 *
 * <p>三段式：左上角单格显示点击选中的方块，中部整块区域放结构预览，底部横排滚动条列出
 * 结构所需的全部方块。高度固定，方块再多也只影响底部横向滚动，不改变页面尺寸。</p>
 */
public record StructureLayout() {
    /**
     * 单例：结构页排版不含可变量。
     */
    public static final StructureLayout INSTANCE = new StructureLayout();

    /**
     * 结构预览区域高度。
     */
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
