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
 * <p>全模组统一用 {@code lss/dark_paper.lss}：由 LDLib2 自带的 paper 主题翻转出的深色版，
 * 选择器与 paper 一致，吃 paper 的组件在这里同样有样式。</p>
 *
 * <p>主题集中在此入口设置，界面不会漏套样式。</p>
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
