package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * JEI 页面公共度量
 *
 * <p>两个页面共用同一套尺寸常量，避免各写一份内边距、行高后彼此错位。
 * 所有槽位都按像素绝对定位摆放，因此这里的数值就是最终渲染位置。</p>
 */
public class JeiLayout {
    /**
     * 面板总宽度，与 JEI 分类宽度一致。
     */
    public static final int PANEL_WIDTH = 176;
    /**
     * {@code panel_bg} 施加的内边距。
     */
    public static final int PANEL_PADDING = 5;
    /**
     * 内边距之内的可用宽度。
     */
    public static final int CONTENT_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2;
    /**
     * 一个槽位的边长。
     */
    public static final int SLOT = 18;
    /**
     * 网格内相邻槽位的间距。
     */
    public static final int SLOT_GAP = 1;
    /**
     * 不同区块之间的间距。
     */
    public static final int BLOCK_GAP = 4;
    /**
     * 一行文本的高度。
     */
    public static final int LINE = 10;
    /**
     * 相邻文本行的间距。
     */
    public static final int LINE_GAP = 1;
    /**
     * 箭头图标宽度，取自 JEI 配方页的箭头贴图。
     */
    public static final int ARROW_WIDTH = 22;
    /**
     * 箭头图标高度，取自 JEI 配方页的箭头贴图。
     */
    public static final int ARROW_HEIGHT = 16;

    private JeiLayout() {
    }

    /**
     * 创建固定行高的文本
     *
     * @param text 文本内容
     * @return 文本元素
     */
    public static Label label(Component text) {
        Label label = new Label();
        label.setText(text);
        label.layout(layout -> layout.widthPercent(100).height(LINE));
        return label;
    }

    /**
     * @return 页面使用的样式表
     */
    public static List<Stylesheet> stylesheets() {
        return List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC));
    }

    /**
     * 计算一组槽位排成网格后的高度
     *
     * @param count   槽位数量
     * @param columns 每行列数
     * @return 高度，空集合返回 0
     */
    public static int gridHeight(int count, int columns) {
        int rows = (count + columns - 1) / columns;
        return rows == 0 ? 0 : rows * SLOT + (rows - 1) * SLOT_GAP;
    }

    /**
     * 计算一组槽位排成网格后的宽度
     *
     * @param columns 每行列数
     * @return 宽度
     */
    public static int gridWidth(int columns) {
        return columns * SLOT + (columns - 1) * SLOT_GAP;
    }
}
