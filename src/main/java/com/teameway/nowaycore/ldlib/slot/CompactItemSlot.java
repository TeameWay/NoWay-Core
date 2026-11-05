package com.teameway.nowaycore.ldlib.slot;

import com.lowdragmc.lowdraglib2.client.font.LDFonts;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.util.NumberFormatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 数量过长的物品槽
 *
 * <p>数量用 {@link NumberFormatUtil#compact(long)} 缩写后画在右下角，仍放不下就等比缩小，避免横穿槽位。</p>
 *
 * <p>真实数量用 {@link #setAmount(long)} 以 {@code long} 单独保存，不受堆栈 {@code count} 的
 * {@code int} 上限截断；缩写只保留量级，精确数量补进悬浮提示。</p>
 *
 * <p>本类也是 JEI 配方页里对应格子的唯一绘制者；XEI 方法全部返回 {@link CompactItemSlot}，
 * 登记原料之后仍可链式设置数量。</p>
 */
@LDLRegister(name = "compact-item-slot", group = "inventory", registry = "ldlib2:ui_element", modID = NoWayCore.MOD_ID)
public class CompactItemSlot extends ItemSlot {
    /**
     * 数量文字的右边缘，也是可用宽度：原版起点为 {@code 17 - 字宽}，可用总量为 17 像素。
     */
    private static final float TEXT_RIGHT = 17.0F;
    private static final float TEXT_TOP = 9.0F;
    /**
     * 数量文字的行高，按缩放比例回推顶边时使用。
     */
    private static final float TEXT_HEIGHT = 8.0F;

    /**
     * 单独指定的真实数量，负值表示沿用物品堆栈自带的数量。
     */
    private long amount = -1L;

    /**
     * 构造一个物品槽
     *
     * <p>注册名 {@code compact-item-slot} 命中不了主题里按元素名匹配的 {@code item-slot} 规则，
     * 因此挂上 {@code item-slot_bg} 类，让底色与悬浮高亮跟随当前主题。</p>
     */
    public CompactItemSlot() {
        addClass("item-slot_bg");
    }

    /**
     * 数量是否已被缩写，绘制与提示共用此判定
     *
     * @param count 真实数量
     * @return 缩写结果与真实数字不同时为 true
     */
    private static boolean isCompacted(long count) {
        return count > 1L && !NumberFormatUtil.compact(count).equals(Long.toString(count));
    }

    /**
     * 指定槽位要显示的数量
     *
     * @param amount 真实数量，可超出 {@code int} 范围
     * @return 自身，便于链式调用
     */
    public CompactItemSlot setAmount(long amount) {
        this.amount = Math.max(0L, amount);
        return this;
    }

    /**
     * 开启幽灵物品拖放
     *
     * @return 自身，便于链式调用
     */
    @Override
    public CompactItemSlot xeiPhantom() {
        super.xeiPhantom();
        return this;
    }

    /**
     * 登记成 XEI 配方原料
     *
     * @param io JEI 语义角色
     * @return 自身，便于链式调用
     */
    @Override
    public CompactItemSlot xeiRecipeIngredient(IngredientIO io) {
        JEISupport.recipeIngredient(this, io, this::ingredients);
        return this;
    }

    /**
     * 登记成带备选项的 XEI 配方原料
     *
     * <p>备选项通常是同一标签下的全部物品，数量统一取本格真实数量。</p>
     *
     * @param io               JEI 语义角色
     * @param allPossibleItems 该格可能显示的全部物品
     * @return 自身，便于链式调用
     */
    @Override
    public CompactItemSlot xeiRecipeIngredient(IngredientIO io, Supplier<Stream<ItemStack>> allPossibleItems) {
        JEISupport.recipeIngredient(this, io, () -> allPossibleItems.get().map(this::displayOnly));
        return this;
    }

    /**
     * 登记成 XEI 配方格，角色为 {@link IngredientIO#NONE}、概率为 1
     *
     * @return 自身，便于链式调用
     */
    @Override
    public CompactItemSlot xeiRecipeSlot() {
        return xeiRecipeSlot(IngredientIO.NONE, 1.0F);
    }

    /**
     * 登记成带概率的 XEI 配方格
     *
     * <p>只有走过本重载的槽位才会挂上命中测试与原料轮换，悬停可查看原料，{@code R}/{@code U} 才能查配方。
     * 概率文字由调用方追加到悬浮提示。</p>
     *
     * @param io     JEI 语义角色
     * @param chance 产出概率，仅用于 XEI 侧语义
     * @return 自身，便于链式调用
     */
    @Override
    public CompactItemSlot xeiRecipeSlot(IngredientIO io, float chance) {
        JEISupport.recipeSlot(this, io, this::ingredients);
        return this;
    }

    /**
     * 登记成带概率与备选项的 XEI 配方格
     *
     * @param io               JEI 语义角色
     * @param chance           产出概率，仅用于 XEI 侧语义
     * @param amount           单次产出数量，仅用于 XEI 侧语义
     * @param allPossibleItems 该格可能显示的全部物品
     * @return 自身，便于链式调用
     */
    @Override
    public CompactItemSlot xeiRecipeSlot(
        IngredientIO io,
        float chance,
        int amount,
        Supplier<Stream<ItemStack>> allPossibleItems
    ) {
        JEISupport.recipeSlot(this, io, () -> allPossibleItems.get().map(this::displayOnly));
        return this;
    }

    @Override
    public List<Component> getFullTooltipTexts(boolean withItemTooltips) {
        List<Component> tooltips = new ArrayList<>(super.getFullTooltipTexts(withItemTooltips));
        tooltips.add(Component.translatable("gui.noway_core.slot.count", NumberFormatUtil.grouped(amount))
            .withStyle(ChatFormatting.GRAY));
        return tooltips;
    }

    @Override
    protected void drawItemStack(IGUIContext context, ItemStack itemStack) {
        long count = this.count();
        if (!isCompacted(count) || !(context instanceof GUIContext guiContext)) {
            // 未缩写时原版已画好数量，不能再画第二遍
            super.drawItemStack(context, itemStack);
            return;
        }
        guiContext.graphics.item(itemStack, 0, 0);
        // 空串告诉原版不画数量，耐久条与冷却遮罩仍照常绘制
        guiContext.graphics.itemDecorations(guiContext.mc.font, itemStack, 0, 0, "");

        String text = NumberFormatUtil.compact(count);
        Font font = LDFonts.font();
        float width = font.width(text);
        float scale = Math.min(0.75F, TEXT_RIGHT / width);
        var pose = guiContext.pose;
        pose.pushPose();
        // 右边缘与下边缘固定，缩放后文字仍贴住格子右下角
        pose.translate(TEXT_RIGHT - width * scale, TEXT_TOP + TEXT_HEIGHT * (1.0F - scale));
        pose.scale(scale, scale);
        LDFonts.drawText(guiContext, font, text, 0, 0, -1, true);
        pose.popPose();
    }

    /**
     * @return 要显示的数量，未单独指定时取物品堆栈自带的数量
     */
    private long count() {
        return this.amount >= 0L ? this.amount : this.getValue().getCount();
    }

    /**
     * 交给 XEI 的原料
     *
     * @return 携带当前物品与数量的流
     */
    private Stream<ItemStack> ingredients() {
        return Stream.of(displayOnly(this.getValue()));
    }

    /**
     * 把真实数量压进物品堆栈的数量字段
     *
     * <p>堆栈只能表达 {@code int}，超出部分按上限截断，不退化成 0 或负数；XEI 侧的备选项同样走这里。</p>
     *
     * @param stack 槽位上的物品
     * @return 带上当前数量的副本
     */
    private ItemStack displayOnly(ItemStack stack) {
        return stack.isEmpty() ? stack : stack.copyWithCount(this.stackCount());
    }

    /**
     * @return 可以放进物品堆栈的数量，夹在 {@code 1} 与 {@code Integer.MAX_VALUE} 之间
     */
    private int stackCount() {
        return (int) Math.clamp(this.count(), 1L, Integer.MAX_VALUE);
    }
}