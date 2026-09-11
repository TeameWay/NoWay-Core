package com.teameway.nowaycore.common.item;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.AEColor;
import appeng.items.tools.powered.ColorApplicatorItem;
import com.teameway.nowaycore.util.StyleUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class InfiniteColorApplicatorItem extends ColorApplicatorItem {

    private static final List<AEColor> PALETTE = Stream.concat(
        AEColor.VALID_COLORS.stream(),
        Stream.of(AEColor.TRANSPARENT)
    ).toList();

    public InfiniteColorApplicatorItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public boolean consumeColor(ItemStack applicator, AEColor color, boolean simulate) {
        return true;
    }

    @Override
    public AEColor getColor(ItemStack itemStack) {
        var selected = itemStack.get(AEComponents.SELECTED_COLOR);
        return selected != null ? selected : PALETTE.getFirst();
    }

    @Override
    public void cycleColors(ItemStack itemStack, @Nullable AEColor currentColor, int offset) {
        var index = currentColor == null ? 0 : PALETTE.indexOf(currentColor);
        if (index < 0) {
            index = 0;
        }
        itemStack.set(AEComponents.SELECTED_COLOR, PALETTE.get(Math.floorMod(index + offset, PALETTE.size())));
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> lines,
        TooltipFlag tooltipFlags
    ) {
        lines.accept(Component.translatable("tooltip.noway_core.infinite_color_applicator.1").withStyle(StyleUtil.colorFromRatio(500)));
        lines.accept(Component.translatable("tooltip.noway_core.infinite_color_applicator.2").withStyle(StyleUtil.colorFromRatio(400)));
        lines.accept(Component.translatable("tooltip.noway_core.infinite_color_applicator.3").withStyle(StyleUtil.colorFromRatio(300)));

        super.appendHoverText(stack, context, tooltipDisplay, lines, tooltipFlags);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.empty();
    }

    @Override
    public int getBytes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getTotalTypes(ItemStack cellItem) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isBlackListed(ItemStack cellItem, AEKey requestedAddition) {
        return true;
    }

    @Override
    public double getIdleDrain() {
        return 0;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.empty();
    }

    @Override
    public void setActiveColor(ItemStack applicator, @Nullable AEColor color) {
        applicator.set(AEComponents.SELECTED_COLOR, color);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public double injectAEPower(ItemStack stack, double amount, Actionable mode) {
        return 0;
    }

    @Override
    public double extractAEPower(ItemStack stack, double amount, Actionable mode) {
        return amount;
    }

    @Override
    public double getAECurrentPower(ItemStack is) {
        return Integer.MAX_VALUE;
    }
}
