package com.teameway.nowaycore.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

/**
 * 配方产出项
 *
 * <p>JSON 形状（{@code type} 决定其余字段）：</p>
 * <pre>
 * { "type": "item",   "item": "minecraft:iron_ingot", "count": 2, "chance": 1.0 }
 * { "type": "fluid",  "fluid": "minecraft:water", "amount": 500, "chance": 0.5 }
 * { "type": "energy", "amount": 1000 }
 * </pre>
 *
 * <p>数量使用 {@link ContextIntProvider}，支持常量/均匀/二项分布；{@code chance} 在
 * {@code (0, 1]} 区间。物品数量不在 {@link ItemStackTemplate} 上表达，避免其 1~99 的限制。</p>
 */
public sealed interface ResourceOutput {
    /**
     * 多态 codec，按 {@code type} 分发
     */
    Codec<ResourceOutput> CODEC = ResourceKind.CODEC.dispatch(
        "type",
        ResourceOutput::kind,
        kind -> switch (kind) {
            case ITEM -> Items.MAP_CODEC;
            case FLUID -> Fluids.MAP_CODEC;
            case ENERGY -> Energy.MAP_CODEC;
        }
    );

    /**
     * @return 资源种类
     */
    ResourceKind kind();

    /**
     * @return 数量提供器
     */
    ContextIntProvider amount();

    /**
     * @return 产出概率，{@code (0, 1]}
     */
    float chance();

    /**
     * 物品产出
     *
     * @param template 物品模板（数量恒为 1，组件由模板携带）
     * @param amount   数量提供器
     * @param chance   产出概率
     */
    record Items(ItemStackTemplate template, ContextIntProvider amount, float chance) implements ResourceOutput {
        public static final MapCodec<Items> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKind.CODEC.optionalFieldOf("type", ResourceKind.ITEM).forGetter(_ -> ResourceKind.ITEM),
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(Items::template),
            CodecUtil.INT_NUMBER_PROVIDER.optionalFieldOf("count", new ConstantValue(1)).forGetter(Items::amount),
            ExtraCodecs.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(Items::chance)
        ).apply(instance, (_, template, amount, chance) -> new Items(template, amount, chance)));


        public Items {
            template = new ItemStackTemplate(template.item(), 1, template.components());
            if (chance <= 0.0F) {
                throw new IllegalArgumentException("chance must be greater than 0");
            }
        }

        @Override
        public ResourceKind kind() {
            return ResourceKind.ITEM;
        }
    }

    /**
     * 流体产出
     *
     * @param fluid  流体
     * @param amount 数量提供器，单位 mB
     * @param chance 产出概率
     */
    record Fluids(Holder<Fluid> fluid, ContextIntProvider amount, float chance) implements ResourceOutput {
        public static final MapCodec<Fluids> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKind.CODEC.optionalFieldOf("type", ResourceKind.FLUID).forGetter(_ -> ResourceKind.FLUID),
            BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("fluid").forGetter(Fluids::fluid),
            CodecUtil.INT_NUMBER_PROVIDER.optionalFieldOf("amount", new ConstantValue(1000)).forGetter(Fluids::amount),
            ExtraCodecs.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(Fluids::chance)
        ).apply(instance, (_, fluid, amount, chance) -> new Fluids(fluid, amount, chance)));


        public Fluids {
            if (chance <= 0.0F) {
                throw new IllegalArgumentException("chance must be greater than 0");
            }
        }

        @Override
        public ResourceKind kind() {
            return ResourceKind.FLUID;
        }
    }

    /**
     * 能量产出
     *
     * @param amount 数量提供器，单位 FE
     * @param chance 产出概率
     */
    record Energy(ContextIntProvider amount, float chance) implements ResourceOutput {
        public static final MapCodec<Energy> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKind.CODEC.optionalFieldOf("type", ResourceKind.ENERGY).forGetter(_ -> ResourceKind.ENERGY),
            CodecUtil.INT_NUMBER_PROVIDER.optionalFieldOf("amount", new ConstantValue(0)).forGetter(Energy::amount),
            ExtraCodecs.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(Energy::chance)
        ).apply(instance, (_, amount, chance) -> new Energy(amount, chance)));


        public Energy {
            if (chance <= 0.0F) {
                throw new IllegalArgumentException("chance must be greater than 0");
            }
        }

        @Override
        public ResourceKind kind() {
            return ResourceKind.ENERGY;
        }
    }
}
