package com.teameway.nowaycore.common.command.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 把世界里的一块区域导出成 anvillib 的多方块定义
 *
 * <p>两种输出共用同一份区域数据：{@link ExportFormat#JSON} 走 {@link MultiblockDefinition#CODEC}，
 * 与 datagen 产物完全同构；{@link ExportFormat#JAVA} 生成可直接粘贴到
 * {@code ModMultiblockDefinitions} 的链式代码。</p>
 */
public final class MultiblockExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<Block, String> VANILLA_BLOCKS = indexBlocks();
    private static final Map<Property<?>, String> PROPERTY_CONSTANTS = indexProperties();
    private static final String NEW_LINE = System.lineSeparator();
    private static final char QUOTE = '"';

    private MultiblockExporter() {
    }

    /**
     * 导出选区内的多方块结构
     *
     * @param level         世界
     * @param from1         选区角点之一
     * @param from2         选区角点之二
     * @param controllerPos 控制器位置，必须位于选区内
     * @param format        输出格式
     * @return 文件内容
     */
    public static String export(Level level, BlockPos from1, BlockPos from2, BlockPos controllerPos, ExportFormat format) {
        BlockPos min = new BlockPos(
            Math.min(from1.getX(), from2.getX()),
            Math.min(from1.getY(), from2.getY()),
            Math.min(from1.getZ(), from2.getZ())
        );
        BlockPos max = new BlockPos(
            Math.max(from1.getX(), from2.getX()),
            Math.max(from1.getY(), from2.getY()),
            Math.max(from1.getZ(), from2.getZ())
        );
        if (!contains(min, max, controllerPos)) {
            throw new IllegalArgumentException("Controller position must be inside the selection");
        }
        if (!level.isLoaded(min) || !level.isLoaded(max)) {
            throw new IllegalStateException("Selection is not fully loaded");
        }
        BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.isAir()) {
            throw new IllegalStateException("Controller position is air");
        }

        Map<Vec3i, BlockState> parts = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            parts.put(
                new Vec3i(pos.getX() - controllerPos.getX(), pos.getY() - controllerPos.getY(), pos.getZ() - controllerPos.getZ()),
                state
            );
        }

        List<Map.Entry<Vec3i, BlockState>> entries = new ArrayList<>(parts.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<Vec3i, BlockState> entry) -> entry.getKey().getY())
            .thenComparingInt(entry -> entry.getKey().getZ())
            .thenComparingInt(entry -> entry.getKey().getX()));
        return format == ExportFormat.JSON ? renderJson(level, entries) : renderJava(controllerState, entries);
    }

    private static boolean contains(BlockPos min, BlockPos max, BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY() && pos.getY() <= max.getY() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private static String renderJson(Level level, List<Map.Entry<Vec3i, BlockState>> entries) {
        Map<Vec3i, BlockStatePredicate> definition = new LinkedHashMap<>();
        for (Map.Entry<Vec3i, BlockState> entry : entries) {
            definition.put(entry.getKey(), predicate(entry.getValue()));
        }
        MultiblockDefinition multiblock = new MultiblockDefinition(definition);
        JsonElement json = MultiblockDefinition.CODEC.codec()
            .encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), multiblock)
            .getOrThrow();
        return GSON.toJson(json) + NEW_LINE;
    }

    private static String renderJava(BlockState controllerState, List<Map.Entry<Vec3i, BlockState>> entries) {
        String indent = "        ";
        StringBuilder out = new StringBuilder();
        out.append("    public static final MultiblockDefinition EXPORTED_MULTIBLOCK = MultiblockDefinition.builder()").append(NEW_LINE);
        out.append(indent).append("// controller at local coordinate (0, 0, 0)").append(NEW_LINE);
        out.append(indent).append(".addController(").append(predicateSource(controllerState, indent)).append(")").append(NEW_LINE);

        int lastY = Integer.MIN_VALUE;
        for (Map.Entry<Vec3i, BlockState> entry : entries) {
            Vec3i pos = entry.getKey();
            if (pos.getX() == 0 && pos.getY() == 0 && pos.getZ() == 0) {
                continue;
            }
            if (pos.getY() != lastY) {
                lastY = pos.getY();
                out.append(indent).append("// y = ").append(lastY).append(NEW_LINE);
            }
            BlockState state = entry.getValue();
            String unsupported = unsupportedProperties(state);
            if (!unsupported.isEmpty()) {
                out.append(indent)
                    .append("// note: property ")
                    .append(unsupported)
                    .append(" has no constant in BlockStateProperties, matching the whole block")
                    .append(NEW_LINE);
            }
            out.append(indent)
                .append(".add(new Vec3i(")
                .append(pos.getX())
                .append(", ")
                .append(pos.getY())
                .append(", ")
                .append(pos.getZ())
                .append("), ")
                .append(predicateSource(state, indent))
                .append(")")
                .append(NEW_LINE);
        }
        out.append("        .build();").append(NEW_LINE);
        return out.toString();
    }

    private static BlockStatePredicate predicate(BlockState state) {
        BlockStatePredicate.Builder builder = BlockStatePredicate.builder().of(state.getBlock());
        applyProperties(builder, state);
        return builder.build();
    }

    private static void applyProperties(BlockStatePredicate.Builder builder, BlockState state) {
        for (Property<?> property : changedProperties(state)) {
            applyProperty(builder, state, property);
        }
    }

    private static <T extends Comparable<T>> void applyProperty(
        BlockStatePredicate.Builder builder,
        BlockState state,
        Property<T> property
    ) {
        builder.with(property, state.getValue(property));
    }

    private static List<Property<?>> changedProperties(BlockState state) {
        BlockState defaults = state.getBlock().defaultBlockState();
        List<Property<?>> changed = new ArrayList<>();
        for (Property<?> property : state.getProperties()) {
            if (!Objects.equals(state.getValue(property), defaults.getValue(property))) {
                changed.add(property);
            }
        }
        return changed;
    }

    private static String unsupportedProperties(BlockState state) {
        List<String> parts = new ArrayList<>();
        for (Property<?> property : changedProperties(state)) {
            if (PROPERTY_CONSTANTS.containsKey(property)) {
                continue;
            }
            parts.add(property.getName() + "=" + state.getValue(property));
        }
        return String.join(", ", parts);
    }

    private static String predicateSource(BlockState state, String indent) {
        String block = blockSource(state.getBlock());
        List<Property<?>> known = new ArrayList<>();
        for (Property<?> property : changedProperties(state)) {
            if (PROPERTY_CONSTANTS.containsKey(property)) {
                known.add(property);
            }
        }
        if (known.isEmpty()) {
            return block;
        }
        StringBuilder source = new StringBuilder("BlockStatePredicate.builder()").append(NEW_LINE);
        source.append(indent).append("    .of(").append(block).append(")").append(NEW_LINE);
        for (Property<?> property : known) {
            source.append(indent)
                .append("    .with(BlockStateProperties.")
                .append(PROPERTY_CONSTANTS.get(property))
                .append(", ")
                .append(valueSource(state, property))
                .append(")")
                .append(NEW_LINE);
        }
        source.append(indent).append("    .build()");
        return source.toString();
    }

    private static String blockSource(Block block) {
        String vanilla = VANILLA_BLOCKS.get(block);
        if (vanilla != null) {
            return vanilla;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return "BuiltInRegistries.BLOCK.getValue(Identifier.parse(" + QUOTE + id + QUOTE + "))";
    }

    private static String valueSource(BlockState state, Property<?> property) {
        return valueSource(state.getValue(property));
    }

    private static String valueSource(Comparable<?> value) {
        return switch (value) {
            case Boolean bool -> bool.toString();
            case Integer integer -> integer.toString();
            case Enum<?> enumValue -> enumValue.getDeclaringClass().getName().replace('$', '.') + "." + enumValue.name();
            default -> QUOTE + String.valueOf(value) + QUOTE;
        };
    }

    private static Map<Block, String> indexBlocks() {
        Map<Block, String> names = new HashMap<>();
        for (Field field : Blocks.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Block.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                names.put((Block) field.get(null), "Blocks." + field.getName());
            } catch (IllegalAccessException ignored) {
                // 公开常量不会走到这里
            }
        }
        return Map.copyOf(names);
    }

    private static Map<Property<?>, String> indexProperties() {
        Map<Property<?>, String> names = new HashMap<>();
        for (Field field : BlockStateProperties.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Property.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                names.put((Property<?>) field.get(null), field.getName());
            } catch (IllegalAccessException ignored) {
                // 公开常量不会走到这里
            }
        }
        return Map.copyOf(names);
    }
}
