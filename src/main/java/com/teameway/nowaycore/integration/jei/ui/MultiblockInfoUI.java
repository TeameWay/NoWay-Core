package com.teameway.nowaycore.integration.jei.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.integration.jei.layout.JeiLayout;
import com.teameway.nowaycore.integration.jei.layout.StructureLayout;
import com.teameway.nowaycore.ldlib.slot.CompactItemSlot;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * 结构信息页
 *
 * <p>左上角的单格显示玩家在预览里点中的方块；中部预览以控制器为中心；底部横排清单列出结构
 * 所需的全部方块与数量，种类多时横向滚动。</p>
 *
 * <p>清单与单格都注册成真正的 JEI 配方格，因此在 JEI 里对任意一种结构方块按 {@code R}/{@code U}
 * 都能查到对应的配方与用途，而不仅是查回结构本身。</p>
 */
public class MultiblockInfoUI {

    private MultiblockInfoUI() {
    }

    /**
     * 构建结构信息界面
     *
     * @param type   机器类型
     * @param blocks 结构所需方块及数量
     * @return 供 JEI 显示的界面
     */
    public static ModularUI create(MultiblockType type, Map<Block, Integer> blocks) {
        StructureLayout layout = StructureLayout.INSTANCE;
        int height = layout.height();
        int contentHeight = height - JeiLayout.PANEL_PADDING * 2;

        UIElement root = new UIElement()
            .layout(l -> l
                .width(JeiLayout.PANEL_WIDTH)
                .height(height)
                .paddingAll(JeiLayout.PANEL_PADDING))
            .addClass("panel_bg");

        UIElement content = new UIElement()
            .layout(l -> l.width(JeiLayout.CONTENT_WIDTH).height(contentHeight));
        root.addChild(content);

        CompactItemSlot selected = new CompactItemSlot();
        selected.layout(l -> l
            .positionType(TaffyPosition.ABSOLUTE)
            .left(0)
            .top(0)
            .width(JeiLayout.SLOT)
            .height(JeiLayout.SLOT));
        selected.xeiRecipeSlot(IngredientIO.INPUT, 1.0F);

        addScene(content, type, selected);
        content.addChild(selected);
        addStrip(content, blocks, layout.stripTop());

        ModularUI ui = ModularUI.of(UI.of(root, StylesheetManager.MC));
        ui.setDrawTooltips(true);
        return ui;
    }

    /**
     * 中部预览，点击方块时把该方块写进左上角的单格
     */
    private static void addScene(UIElement content, MultiblockType type, CompactItemSlot selected) {
        Scene scene = MultiblockPreview.create(type);
        if (scene == null) {
            Label missing = JeiLayout.label(Component.translatable("gui.noway_core.jei.no_structure"));
            missing.layout(l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(StructureLayout.PREVIEW_HEIGHT / 2)
                .width(JeiLayout.CONTENT_WIDTH));
            content.addChild(missing);
            return;
        }
        scene.setOnSelected((pos, facing) -> {
            Block block = MultiblockPreview.blockAt(type, pos);
            if (block != null) {
                selected.setItem(new ItemStack(block.asItem()));
            }
        });
        scene.layout(l -> l
            .positionType(TaffyPosition.ABSOLUTE)
            .left(0)
            .top(0)
            .width(JeiLayout.CONTENT_WIDTH)
            .height(StructureLayout.PREVIEW_HEIGHT));
        content.addChild(scene);
    }

    /**
     * 底部横排清单，横向滚动
     */
    private static void addStrip(UIElement content, Map<Block, Integer> blocks, int top) {
        ScrollerView strip = new ScrollerView();
        UIElement row = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(JeiLayout.SLOT_GAP));
        blocks.forEach((block, count) -> {
            CompactItemSlot slot = new CompactItemSlot();
            slot.setItem(new ItemStack(block.asItem(), count));
            slot.xeiRecipeSlot(IngredientIO.INPUT, 1.0F);
            slot.layout(l -> l.width(JeiLayout.SLOT).height(JeiLayout.SLOT));
            row.addChild(slot);
        });
        strip.addScrollViewChild(row);
        strip.viewPort(viewPort -> viewPort
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY))
                .layout(l -> l.paddingAll(0)))
            .viewContainer(viewContainer -> viewContainer.layout(l -> l.flexDirection(FlexDirection.ROW)))
            .scrollerStyle(style -> style
                .mode(ScrollerMode.HORIZONTAL)
                .horizontalScrollDisplay(ScrollDisplay.AUTO))
            .layout(l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .left(0)
                .top(top)
                .width(JeiLayout.CONTENT_WIDTH)
                .height(StructureLayout.STRIP_HEIGHT));
        content.addChild(strip);
    }
}