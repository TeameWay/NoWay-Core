package com.teameway.nowaycore.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teameway.nowaycore.resource.ingredient.LongItemIngredient;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * 配方输入项
 *
 * <p>匹配语义统一为可用量大于等于需求量；{@code consume} 为 {@code false} 时只校验存在、不扣除，
 * 用于催化剂与模具。能量是标量资源，恒为消耗。</p>
 */
public sealed interface ResourceIngredient {
    /**
     * 多态 codec，按 {@code type} 分发
     */
    Codec<ResourceIngredient> CODEC = ResourceKind.CODEC.dispatch(
        "type",
        ResourceIngredient::kind,
        kind -> switch (kind) {
            case ITEM -> Items.MAP_CODEC;
            case FLUID -> Fluids.MAP_CODEC;
            case ENERGY -> Energy.MAP_CODEC;
        }
    );

    ResourceKind kind();

    /**
     * @return 匹配成功后是否扣除
     */
    boolean consumed();

    /**
     * 物品输入
     *
     * @param input    物品集合、数量与组件匹配，数量上限是 {@code long}
     * @param consumed 是否消耗
     */
    record Items(LongItemIngredient input, boolean consumed) implements ResourceIngredient {
        public static final MapCodec<Items> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKind.CODEC.optionalFieldOf("type", ResourceKind.ITEM).forGetter(_ -> ResourceKind.ITEM),
            LongItemIngredient.CODEC.fieldOf("items").forGetter(Items::input),
            Codec.BOOL.optionalFieldOf("consume", true).forGetter(Items::consumed)
        ).apply(instance, (_, input, consumed) -> new Items(input, consumed)));


        @Override
        public ResourceKind kind() {
            return ResourceKind.ITEM;
        }
    }

    /**
     * 流体输入，匹配规则使用 NeoForge 的 {@link SizedFluidIngredient}
     *
     * @param ingredient 流体原料（支持 tag 与数量）
     * @param consumed   是否消耗
     */
    record Fluids(SizedFluidIngredient ingredient, boolean consumed) implements ResourceIngredient {
        public static final MapCodec<Fluids> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKind.CODEC.optionalFieldOf("type", ResourceKind.FLUID).forGetter(_ -> ResourceKind.FLUID),
            SizedFluidIngredient.CODEC.fieldOf("fluid").forGetter(Fluids::ingredient),
            Codec.BOOL.optionalFieldOf("consume", true).forGetter(Fluids::consumed)
        ).apply(instance, (_, ingredient, consumed) -> new Fluids(ingredient, consumed)));


        @Override
        public ResourceKind kind() {
            return ResourceKind.FLUID;
        }
    }

    /**
     * 能量输入（标量，恒消耗）
     *
     * @param amount 需求量，单位 FE
     */
    record Energy(long amount) implements ResourceIngredient {
        public static final MapCodec<Energy> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKind.CODEC.optionalFieldOf("type", ResourceKind.ENERGY).forGetter(_ -> ResourceKind.ENERGY),
            ExtraCodecs.POSITIVE_LONG.fieldOf("amount").forGetter(Energy::amount)
        ).apply(instance, (_, amount) -> new Energy(amount)));


        @Override
        public ResourceKind kind() {
            return ResourceKind.ENERGY;
        }

        @Override
        public boolean consumed() {
            return true;
        }
    }
}
