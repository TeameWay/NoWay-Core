package com.teameway.nowaycore.resource.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 物品原料
 *
 * <p>形状与 anvillib 的 {@code ItemIngredientPredicate} 一致：{@code items}（物品或标签，省略表示任意物品）、
 * {@code count}（需求量）、{@code components}（组件匹配）。区别只在 {@code count} 用 {@link Long} 表示，
 * 因此配方可以要求远超 32 位上限的数量，例如 1145141919810 个石头。</p>
 *
 * @param items      物品集合，空表示任意物品
 * @param count      需求量，正整数
 * @param components 数据组件匹配
 */
public record LongItemIngredient(Optional<HolderSet<Item>> items, long count, DataComponentMatchers components) {
    /**
     * 编解码器
     */
    public static final Codec<LongItemIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegistryCodecs.holderSet(Registries.ITEM).optionalFieldOf("items").forGetter(LongItemIngredient::items),
        ExtraCodecs.POSITIVE_LONG.optionalFieldOf("count", 1L).forGetter(LongItemIngredient::count),
        DataComponentMatchers.CODEC.codec().optionalFieldOf("components", DataComponentMatchers.ANY)
            .forGetter(LongItemIngredient::components)
    ).apply(instance, LongItemIngredient::new));

    public LongItemIngredient {
        if (count <= 0L) {
            throw new IllegalArgumentException("count must be positive");
        }
    }

    /**
     * 只比对物品与组件，不比对数量
     *
     * <p>数量由调用方按“可用量 ≥ 需求量”自行判断，这样同一种物品可以分散在多个槽位里。</p>
     *
     * @param stack 待比对的物品堆栈
     * @return 物品与组件是否匹配
     */
    public boolean testIgnoreCount(ItemStack stack) {
        if (this.items.isPresent() && !this.items.get().contains(stack.typeHolder())) {
            return false;
        }
        return this.components.test(stack);
    }
}
