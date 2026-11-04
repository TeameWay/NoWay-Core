package com.teameway.nowaycore.integration.jei.ui.widget;

import com.lowdragmc.lowdraglib2.gui.texture.VanillaSpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.teameway.nowaycore.integration.jei.layout.JeiLayout;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.resources.Identifier;

/**
 * 配方页中间的箭头
 *
 * <p>使用 JEI 配方页自带的箭头贴图。两张贴图都注册在 {@code minecraft:gui} 图集里：
 * {@code jei:recipe_arrow} 是空心箭头，{@code jei:recipe_arrow_filled} 是实心箭头。</p>
 *
 * <p>动画由两层叠成：底层铺空心箭头，上层铺实心箭头并放进一个可剪裁的容器。
 * 容器宽度随界面 tick 从 0 增长到整张箭头宽度，于是实心箭头看起来是从左往右填满的。</p>
 */
public class Arrow {
    /**
     * JEI 的空心配方箭头，位于 {@code minecraft:gui} 图集。
     */
    private static final Identifier SPRITE_EMPTY = Identifier.fromNamespaceAndPath("jei", "recipe_arrow");
    /**
     * JEI 的实心配方箭头，位于 {@code minecraft:gui} 图集。
     */
    private static final Identifier SPRITE_FILLED = Identifier.fromNamespaceAndPath("jei", "recipe_arrow_filled");

    private Arrow() {
    }

    /**
     * 创建带动画的箭头
     *
     * @param duration 填满所需的 tick 数，与配方耗时一致时动画正好在完工时铺满
     * @return 箭头元素，尺寸与 JEI 贴图一致
     */
    public static UIElement create(int duration) {
        UIElement root = new UIElement().layout(layout -> layout
            .width(JeiLayout.ARROW_WIDTH)
            .height(JeiLayout.ARROW_HEIGHT));

        root.addChild(sprite(SPRITE_EMPTY)
            .layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .width(JeiLayout.ARROW_WIDTH)
                .height(JeiLayout.ARROW_HEIGHT)));

        // 这一层负责剪裁：宽度就是实心箭头露出来的部分
        UIElement mask = new UIElement().layout(layout -> layout
            .positionType(TaffyPosition.ABSOLUTE)
            .left(0)
            .top(0)
            .width(0)
            .height(JeiLayout.ARROW_HEIGHT));
        mask.setOverflowVisible(false);
        mask.addChild(sprite(SPRITE_FILLED)
            .layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(0)
                .width(JeiLayout.ARROW_WIDTH)
                .height(JeiLayout.ARROW_HEIGHT)));
        root.addChild(mask);

        animate(mask, Math.max(1, duration));
        return root;
    }

    /**
     * 把箭头放到容器水平中线上
     *
     * @param container 容器
     * @param top       箭头在容器内的纵向位置
     * @param duration  填满所需的 tick 数
     * @return 箭头元素，供调用方按需追加子元素
     */
    public static UIElement centerIn(UIElement container, float top, int duration) {
        UIElement arrow = create(duration);
        arrow.layout(layout -> layout
            .positionType(TaffyPosition.ABSOLUTE)
            .left(JeiLayout.CONTENT_WIDTH / 2.0F - JeiLayout.ARROW_WIDTH / 2.0F)
            .top(top));
        container.addChild(arrow);
        return arrow;
    }

    /**
     * 按 tick 推进填充宽度，铺满后从头再来
     *
     * <p>JEI 会缓存配方界面，界面对象可能被反复复用；循环播放才能在每次翻到这条配方时
     * 都看到动画，而不是只有第一次。</p>
     *
     * @param mask     剪裁层
     * @param duration 填满所需的 tick 数
     */
    private static void animate(UIElement mask, int duration) {
        int[] elapsed = {0};
        mask.addEventListener(
            UIEvents.TICK, event -> {
                elapsed[0]++;
                if (elapsed[0] > duration) {
                    elapsed[0] = 1;
                }
                float width = JeiLayout.ARROW_WIDTH * (elapsed[0] / (float) duration);
                mask.layout(layout -> layout.width(width));
            }
        );
    }

    private static UIElement sprite(Identifier sprite) {
        UIElement element = new UIElement();
        element.style(style -> style.backgroundTexture(VanillaSpriteTexture.of(sprite)));
        return element;
    }
}
