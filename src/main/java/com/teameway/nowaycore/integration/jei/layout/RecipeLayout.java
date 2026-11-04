package com.teameway.nowaycore.integration.jei.layout;

import com.teameway.nowaycore.integration.jei.ui.JeiLayout;
import com.teameway.nowaycore.integration.jei.ui.RecipeContents;

import java.util.List;

/**
 * 配方页排版
 *
 * <p>中部把“输入列、箭头、输出列”当作一整行，行内四段间距——左右页边、两处箭头间隔——全部相等，
 * 于是箭头自然落在面板水平中线上，两侧槽位严格对称。列宽由该分类最大的槽位数量决定，
 * 槽位少的一侧不会因为列窄而偏向一边，只是整行重新等分。</p>
 *
 * <p>所有坐标相对面板内容区左上角，供绝对定位直接使用。</p>
 *
 * @param columns    每侧列数，1~3
 * @param bodyHeight 中部区域高度，取两侧列高与箭头高度的最大值
 */
public record RecipeLayout(int columns, int bodyHeight) {
    /**
     * 每侧最多三列。
     */
    public static final int MAX_COLUMNS = 3;
    /**
     * 底部固定三行：耗时、总耗能、耗能功率。
     */
    public static final int TEXT_LINES = 3;
    /**
     * 中部内容块与底部文字之间的纵向间距。
     */
    private static final int TEXT_GAP = 24;
    /**
     * 中部整行相对内容区顶部的下移量，用于让槽位与箭头视觉上更居中。
     */
    private static final int BODY_OFFSET = 6;
    /**
     * 一整行里被等分的间距数量：左页边、两处箭头间隔、右页边。
     */
    private static final int ROW_GAPS = 4;

    /**
     * 按该分类全部配方计算统一排版
     *
     * <p>列数先按最大格子数确定，再用最终列数重算高度：列数不同会改变换行位置，
     * 若先用 1 列估算高度，最终布局会偏小。</p>
     *
     * @param recipes 该分类的配方
     * @return 排版
     */
    public static RecipeLayout of(List<RecipeContents> recipes) {
        int widest = 1;
        for (RecipeContents recipe : recipes) {
            widest = Math.max(widest, Math.max(recipe.inputCells(), recipe.outputCells()));
        }
        int columns = Math.max(1, Math.min(MAX_COLUMNS, widest));

        int body = JeiLayout.ARROW_HEIGHT;
        for (RecipeContents recipe : recipes) {
            body = Math.max(body, Math.max(recipe.inputHeight(columns), recipe.outputHeight(columns)));
        }
        return new RecipeLayout(columns, body);
    }

    /**
     * @return 整页高度
     */
    public int height() {
        return JeiLayout.PANEL_PADDING * 2 + BODY_OFFSET + this.bodyHeight + TEXT_GAP
               + JeiLayout.LINE * TEXT_LINES + JeiLayout.LINE_GAP * (TEXT_LINES - 1);
    }

    /**
     * @return 一列的宽度
     */
    public int columnWidth() {
        return this.columns * JeiLayout.SLOT + (this.columns - 1) * JeiLayout.SLOT_GAP;
    }

    /**
     * 行内统一间距
     *
     * <p>内容区宽度减去两列与箭头后，余量由四段间距平分；整除的零头由 {@link #rowOffset()}
     * 均摊到两侧，保证左右仍然对称。</p>
     *
     * @return 间距
     */
    public int gap() {
        return Math.max(0, (JeiLayout.CONTENT_WIDTH - 2 * this.columnWidth() - JeiLayout.ARROW_WIDTH) / ROW_GAPS);
    }

    /**
     * @return 整行实际占用的宽度
     */
    public int rowWidth() {
        return 2 * this.columnWidth() + JeiLayout.ARROW_WIDTH + ROW_GAPS * this.gap();
    }

    /**
     * @return 整行左侧的额外偏移，用于吃掉整除余数
     */
    public int rowOffset() {
        return Math.max(0, (JeiLayout.CONTENT_WIDTH - this.rowWidth()) / 2);
    }

    /**
     * @return 输入列左边缘
     */
    public int inputLeft() {
        return this.rowOffset() + this.gap();
    }

    /**
     * @return 箭头左边缘，恰好位于两列正中间
     */
    public int arrowLeft() {
        return this.inputLeft() + this.columnWidth() + this.gap();
    }

    /**
     * @return 输出列左边缘
     */
    public int outputLeft() {
        return this.arrowLeft() + JeiLayout.ARROW_WIDTH + this.gap();
    }

    /**
     * @return 箭头在内容区内的纵向位置
     */
    public int arrowTop() {
        return BODY_OFFSET + (this.bodyHeight - JeiLayout.ARROW_HEIGHT) / 2;
    }

    /**
     * @return 底部文字块在内容区内的纵向位置
     */
    public int textTop() {
        return BODY_OFFSET + this.bodyHeight + TEXT_GAP;
    }

    /**
     * @param columnHeight 该列实际高度
     * @return 该列的纵向位置，列高不一致时保持垂直居中
     */
    public int columnTop(int columnHeight) {
        return BODY_OFFSET + Math.max(0, (this.bodyHeight - columnHeight) / 2);
    }
}
