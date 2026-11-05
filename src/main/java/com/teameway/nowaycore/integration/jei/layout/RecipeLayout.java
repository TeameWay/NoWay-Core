package com.teameway.nowaycore.integration.jei.layout;

import com.teameway.nowaycore.integration.jei.ui.RecipeContents;

import java.util.List;

/**
 * 配方页排版
 *
 * <p>输入整体、箭头与输出整体的中心在内容区内等距分布，间距由较宽的一侧决定，一侧槽位少时只是整体变窄。</p>
 *
 * <p>所有坐标相对面板内容区左上角，供绝对定位直接使用。</p>
 *
 * @param columns   该分类单侧允许的最大列数
 * @param inputWidth  输入整体在最宽配方下的宽度
 * @param outputWidth 输出整体在最宽配方下的宽度
 * @param bodyHeight  中部区域高度，取两侧整体高度与箭头高度的最大值
 */
public record RecipeLayout(int columns, int inputWidth, int outputWidth, int bodyHeight) {
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
     * 整体之间允许的最小间隔，避免槽位贴到箭头或页面边缘。
     */
    private static final int MIN_GAP = 4;

    /**
     * 按该分类全部配方计算统一排版
     *
     * <p>列数取所有配方里最宽的一侧，再用最终列数重算高度：列数改变会改变换行位置，按 1 列估算会偏小。</p>
     *
     * @param recipes 该分类的配方
     * @return 排版
     */
    public static RecipeLayout of(List<RecipeContents> recipes) {
        int widest = 1;
        int inColumns = 1;
        int outColumns = 1;
        for (RecipeContents recipe : recipes) {
            inColumns = Math.max(inColumns, recipe.inputColumns(MAX_COLUMNS));
            outColumns = Math.max(outColumns, recipe.outputColumns(MAX_COLUMNS));
            widest = Math.max(
                widest, Math.max(
                    recipe.itemInputs().size() + recipe.fluidInputs().size(),
                    recipe.itemOutputs().size() + recipe.fluidOutputs().size()
                )
            );
        }
        int columns = Math.max(1, Math.min(MAX_COLUMNS, widest));

        int body = JeiLayout.ARROW_HEIGHT;
        for (RecipeContents recipe : recipes) {
            body = Math.max(body, Math.max(recipe.inputHeight(columns), recipe.outputHeight(columns)));
        }
        return new RecipeLayout(
            columns,
            JeiLayout.gridWidth(inColumns),
            JeiLayout.gridWidth(outColumns),
            body
        );
    }

    /**
     * @return 整页高度
     */
    public int height() {
        return JeiLayout.PANEL_PADDING * 2 + BODY_OFFSET + this.bodyHeight + TEXT_GAP
               + JeiLayout.LINE * TEXT_LINES + JeiLayout.LINE_GAP * (TEXT_LINES - 1);
    }

    /**
     * 三者中心之间的间距
     *
     * <p>取能同时满足两侧约束的最大值：靠页边的一侧至少留 {@link #MIN_GAP}，另一侧按较宽整体折算。</p>
     *
     * @return 中心间距
     */
    public int centerGap() {
        // 箭头中心到页边的可用距离，扣掉较宽整体的半宽与最小间隔
        int half = (int) Math.ceil(Math.max(this.inputWidth, this.outputWidth) / 2.0);
        int limit = JeiLayout.CONTENT_WIDTH / 2 - half - MIN_GAP;
        return Math.max(0, limit);
    }

    /**
     * 箭头左边缘，始终落在内容区水平中线上
     *
     * @return 左边缘
     */
    public int arrowLeft() {
        return JeiLayout.CONTENT_WIDTH / 2 - JeiLayout.ARROW_WIDTH / 2;
    }

    /**
     * 按整体宽度算出它应放的左边缘，使其中心与箭头中心相差一个 {@link #centerGap()}
     *
     * @param width  整体实际宽度
     * @param before 是否位于箭头左侧
     * @return 左边缘
     */
    public int blockLeft(int width, boolean before) {
        int center = JeiLayout.CONTENT_WIDTH / 2;
        int offset = this.centerGap();
        if (before) {
            return center - offset - width / 2;
        }
        return center + offset - width / 2;
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
     * @param blockHeight 整体实际高度
     * @return 整体的纵向位置，整体高度不一致时保持垂直居中
     */
    public int blockTop(int blockHeight) {
        return BODY_OFFSET + Math.max(0, (this.bodyHeight - blockHeight) / 2);
    }
}
