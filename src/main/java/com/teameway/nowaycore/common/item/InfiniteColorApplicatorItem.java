package com.teameway.nowaycore.common.item;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalBlockPos;
import appeng.block.networking.CableBusBlock;
import appeng.block.paint.PaintSplotchesBlock;
import appeng.blockentity.misc.PaintSplotchesBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.BlockRecolorer;
import appeng.items.tools.powered.ColorApplicatorItem;
import appeng.util.Platform;
import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.util.StyleUtil;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class InfiniteColorApplicatorItem extends ColorApplicatorItem implements HeldItemUIMenuType.HeldItemUI {

    public InfiniteColorApplicatorItem(Properties props) {
        super(props.stacksTo(1));
    }

    @RPCPacket(modId = NoWayCore.MOD_ID, value = "set_color")
    private static void setColor(RPCSender sender, AEColor color) {
        var player = sender.asPlayer();
        if (player == null) {
            return;
        }
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof InfiniteColorApplicatorItem) {
                stack.set(AEComponents.SELECTED_COLOR, color);
            }
        }
    }

    private static ItemStack getColorItem(AEColor color) {
        var dye = color.dye;
        return dye != null ? new ItemStack(Items.DYE.pick(dye)) : AEItems.FLUIX_CRYSTAL.stack();
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
        BlockState from,
        BlockState to,
        Property<T> property
    ) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.isShiftKeyDown()) {
                HeldItemUIMenuType.openUI(serverPlayer, usedHand);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var stack = context.getItemInHand();
        var side = context.getClickedFace();
        var player = context.getPlayer();
        if (player == null && level instanceof ServerLevel serverLevel) {
            player = Platform.getFakePlayer(serverLevel, null);
        }

        if (player != null && !Platform.hasPermissions(new DimensionalBlockPos(level, pos), player)) {
            return InteractionResult.FAIL;
        }

        var color = this.getColor(stack);

        if (color == AEColor.TRANSPARENT) {
            if (player != null
                && level.getBlockEntity(pos) instanceof IColorableBlockEntity colorableBlockEntity
                && colorableBlockEntity.getColor() != AEColor.TRANSPARENT
                && colorableBlockEntity.recolourBlock(side, AEColor.TRANSPARENT, player)) {
                return InteractionResult.SUCCESS;
            }

            var paintPos = pos.relative(side);
            if (level.getBlockState(paintPos).getBlock() instanceof PaintSplotchesBlock
                && level.getBlockEntity(paintPos) instanceof PaintSplotchesBlockEntity paintSplotches) {
                paintSplotches.cleanSide(side.getOpposite());
                return InteractionResult.SUCCESS;
            }
        }

        if (this.recolourBlock(level.getBlockState(pos).getBlock(), side, level, pos, color, player)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult interactLivingEntity(
        ItemStack is,
        Player player,
        LivingEntity target,
        InteractionHand usedHand
    ) {
        if (this.getColor(is) == AEColor.TRANSPARENT) {
            return InteractionResult.PASS;
        }

        return super.interactLivingEntity(is, player, target, usedHand);
    }

    @Override
    public int getBytes(ItemStack cellItem) {
        return 114514;
    }

    @Override
    public int getTotalTypes(ItemStack cellItem) {
        return 19;
    }

    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> lines,
        TooltipFlag tooltipFlags
    ) {
        lines.accept(Component.translatable("tooltip.noway_core.infinite_color_applicator.1").withStyle(StyleUtil.colorFromRatio(200)));
        lines.accept(Component.translatable("tooltip.noway_core.infinite_color_applicator.2").withStyle(StyleUtil.colorFromRatio(275)));
        lines.accept(Component.translatable("tooltip.noway_core.infinite_color_applicator.3").withStyle(StyleUtil.colorFromRatio(250)));

        super.appendHoverText(stack, context, tooltipDisplay, lines, tooltipFlags);
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.empty();
    }

    @Override
    public void onWheel(ItemStack is, boolean up) {
    }

    @Override
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder) {
        var slotSize = 18;
        var gap = 3;
        var padding = 4;
        var slotsPerRow = 9;
        var rowWidth = slotsPerRow * slotSize + (slotsPerRow - 1) * gap;
        var rootWidth = rowWidth + padding * 2;
        var rootHeight = padding * 2 + 9 + gap * 2 + slotSize * 2;

        var root = new UIElement().layout(layout -> layout
            .width(rootWidth)
            .height(rootHeight)
            .paddingAll(padding)
            .gapAll(gap)
            .flexDirection(FlexDirection.COLUMN)
        ).addClass("panel_bg");

        root.addChild(new Label().setText(Component.translatable("item.noway_core.infinite_color_applicator")));

        var colors = AEColor.values();
        for (int rowIndex = 0; rowIndex * slotsPerRow < colors.length; rowIndex++) {
            var row = new UIElement().layout(layout -> layout
                .gapAll(gap)
                .flexDirection(FlexDirection.ROW)
            );
            for (int column = 0; column < slotsPerRow; column++) {
                var index = rowIndex * slotsPerRow + column;
                if (index >= colors.length) {
                    break;
                }
                var slot = new ItemSlot();
                var color = colors[index];
                slot.setItem(getColorItem(color));
                slot.addEventListener(
                    UIEvents.CLICK, _ -> {
                        holder.player.getItemInHand(holder.hand).set(AEComponents.SELECTED_COLOR, color);
                        RPCPacketDistributor.rpcToServer("set_color", color);
                    }
                );
                row.addChild(slot);
            }
            root.addChild(row);
        }

        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MODERN)), holder.player);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    private boolean recolourBlock(
        Block block,
        Direction side,
        Level level,
        BlockPos pos,
        AEColor color,
        @Nullable Player player
    ) {
        var state = level.getBlockState(pos);

        var recolored = BlockRecolorer.recolor(block, color);
        if (recolored != block) {
            var newState = recolored.defaultBlockState();
            for (var property : newState.getProperties()) {
                newState = copyProperty(state, newState, property);
            }
            return level.setBlockAndUpdate(pos, newState);
        }

        if (block instanceof CableBusBlock cableBusBlock && player != null && color.dye != null) {
            return cableBusBlock.recolorBlock(level, pos, side, color.dye, player);
        }

        if (level.getBlockEntity(pos) instanceof IColorableBlockEntity colorableBlockEntity
            && colorableBlockEntity.getColor() != color) {
            return colorableBlockEntity.recolourBlock(side, color, player);
        }

        return false;
    }

    @Override
    public boolean isStorageCell(ItemStack i) {
        return false;
    }

    @Override
    public double getAEMaxPower(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    @Override
    public double getAECurrentPower(ItemStack is) {
        return Long.MAX_VALUE;
    }
}
