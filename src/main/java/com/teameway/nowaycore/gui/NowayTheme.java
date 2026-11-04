package com.teameway.nowaycore.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.teameway.nowaycore.NoWayCore;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 界面主题
 *
 * <p>全模组统一用 {@code lss/dark_paper.lss}：从 LDLib2 自带的 paper 主题翻转出来的深色版，
 * 选择器与 paper 完全一致，只换了配色，因此任何吃 paper 的组件在这里也都有样式。</p>
 *
 * <p>集中在一个入口而不是每个界面各写一遍：主题改一处就够，也不会出现某个界面漏改、
 * 白底黑字与深色界面混在一起的情况。</p>
 */
public class NowayTheme {

    private static final Identifier DARK_PAPER = NoWayCore.id("lss/dark_paper.lss");

    private NowayTheme() {
    }

    /**
     * 给界面套上主题
     *
     * @param root 界面根元素
     * @return 带主题的界面
     */
    public static UI of(UIElement root) {
        return UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(DARK_PAPER)));
    }
}
