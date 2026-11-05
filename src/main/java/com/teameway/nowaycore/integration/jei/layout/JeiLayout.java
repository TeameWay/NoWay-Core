package com.teameway.nowaycore.integration.jei.layout;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.network.chat.Component;

/**
 * JEI 页面公共度量
 *
 * <p>两个页面共用同一套尺寸常量，避免内边距与行高彼此错位；槽位按像素绝对定位，这里的数值即最终渲染位置。</p>
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
    public static final int CONTENT_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2;
    public static final int SLOT = 18;
    public static final int SLOT_GAP = 0;
    public static final int BLOCK_GAP = 4;
    public static final int LINE = 10;
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
