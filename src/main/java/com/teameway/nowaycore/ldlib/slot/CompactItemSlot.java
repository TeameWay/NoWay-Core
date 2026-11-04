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
 * <p>原版把数量原样画在右下角，数量上万时会横穿好几个槽位。这里把数量换成
 * {@link NumberFormatUtil#compact(long)} 的缩写结果，缩写后仍放不下就等比缩小，保证收在格子内。</p>
 *
 * <p>数量可以单独设置，不必等于物品堆栈自带的数字：堆栈的 {@code count} 只有 {@code int}，
 * 需求上万亿的配方塞进去会被截断成负数、整个堆栈判空。用 {@link #setAmount(long)} 记下真实数量，
 * 槽位照样显示这个物品，数字则用 {@code long} 完整表达。</p>
 *
 * <p>缩写只保留量级，精确数量补进悬浮提示，界面上干净、查证时也不丢信息。</p>
 *
 * <p>JEI 配方页把已登记的槽位全部交给 LDLib2 的元素绘制，因此本类既是界面上的格子，
 * 也是 JEI 里那格的唯一绘制者，不会和原版数量叠在一起。</p>
 *
 * <p>XEI 相关方法全部以本类为返回类型重写，调用方拿到的始终是 {@link CompactItemSlot}，
 * 可以继续链式设置数量，不会在登记原料之后退化成普通的 {@link ItemSlot}。</p>
 */
@LDLRegister(name = "compact-item-slot", group = "inventory", registry = "ldlib2:ui_element", modID = NoWayCore.MOD_ID)
public class CompactItemSlot extends ItemSlot {
    /**
     * 数量文字的右边缘，也是它可用的全部宽度：原版起点为 {@code 17 - 字宽}，
     * 因此从内容区左边缘到这里正好是 17 像素。
     */
    private static final float TEXT_RIGHT = 17.0F;
    /**
     * 数量文字的顶边，与原版一致：原版起点为 9。
     */
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
     * <p>样式表按元素名匹配元素：{@code item-slot { background: ... }} 这类规则只会落到注册名为
     * {@code item-slot} 的元素上。本类的注册名是 {@code compact-item-slot}，命中不了那条规则，
     * 于是底色退回 {@link ItemSlot} 构造器里写死的默认值，和同一页里的流体槽放在一起格格不入。</p>
     *
     * <p>{@code item-slot_bg} 是 LDLib2 为「不是槽位、但需要槽位底色」的元素预留的类，每个主题都把它
     * 与 {@code item-slot} 写在同一条规则里。挂上这个类，底色、悬浮高亮就跟着当前主题走，
     * 不必为每个主题各抄一份规则，也不必改动注册名。</p>
     */
    public CompactItemSlot() {
        addClass("item-slot_bg");
    }

    /**
     * 数量是否已被缩写掉，绘制与提示两处都以它为准，两者永远一致
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
     * <p>备选项通常是同一个标签下的全部物品，数量统一按本格的真实数量给出。</p>
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
     * <p>这个重载是“可交互”的分水岭：只有走过它的槽位才会被 LDLib2 挂上命中测试与
     * 原料轮换，鼠标移上去才认得出格子里是什么，{@code R}/{@code U} 才查得到配方与用途。
     * 概率文字仍由调用方追加到悬浮提示里。</p>
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
        // 始终显示数量
        tooltips.add(Component.translatable("gui.noway_core.slot.count", NumberFormatUtil.grouped(amount))
            .withStyle(ChatFormatting.GRAY));
        return tooltips;
    }

    @Override
    protected void drawItemStack(IGUIContext context, ItemStack itemStack) {
        long count = this.count();
        if (!isCompacted(count) || !(context instanceof GUIContext guiContext)) {
            // 未缩写时原版路径已经画好了数量，这里不能再画第二遍
            super.drawItemStack(context, itemStack);
            return;
        }
        guiContext.graphics.item(itemStack, 0, 0);
        // 空串等于告诉原版不要画数量，耐久条与冷却遮罩仍照常绘制
        guiContext.graphics.itemDecorations(guiContext.mc.font, itemStack, 0, 0, "");

        String text = NumberFormatUtil.compact(count);
        Font font = LDFonts.font();
        float width = font.width(text);
        float scale = Math.min(0.75F, TEXT_RIGHT / width);
        var pose = guiContext.pose;
        pose.pushPose();
        // 右边缘与下边缘固定，缩放后文字仍然贴着格子右下角
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
     * <p>本类的真实数量是 {@code long}，物品堆栈只能表达 {@code int}：超出部分按 {@code int} 上限截断，
     * 不会退化成 0 或负数。XEI 侧的备选项也走这里，因此它们与格子上显示的数量一致。</p>
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