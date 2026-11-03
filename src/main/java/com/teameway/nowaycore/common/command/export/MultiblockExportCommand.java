package com.teameway.nowaycore.common.command.export;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 服务端命令 {@code /no_way export}
 *
 * <p>需要 OP 4 级权限；读取选区方块并导出多方块定义，文件写入服务端目录下的 {@code exports/}。</p>
 */
public final class MultiblockExportCommand {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String EXPORT_DIR = "exports";

    private MultiblockExportCommand() {
    }

    /**
     * 注册 {@code /no_way export} 命令
     *
     * @param dispatcher 命令分发器
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("no_way")
            .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
            .then(Commands.literal("export")
                .then(Commands.argument("from1", BlockPosArgument.blockPos())
                    .then(Commands.argument("from2", BlockPosArgument.blockPos())
                        .then(Commands.argument("controllerPos", BlockPosArgument.blockPos())
                            .executes(context -> export(context, ExportFormat.JAVA))
                            .then(Commands.literal("java").executes(context -> export(context, ExportFormat.JAVA)))
                            .then(Commands.literal("json").executes(context -> export(context, ExportFormat.JSON))))))));
    }

    private static int export(CommandContext<CommandSourceStack> context, ExportFormat format) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos from1 = BlockPosArgument.getLoadedBlockPos(context, "from1");
        BlockPos from2 = BlockPosArgument.getLoadedBlockPos(context, "from2");
        BlockPos controllerPos = BlockPosArgument.getLoadedBlockPos(context, "controllerPos");

        String content;
        try {
            content = MultiblockExporter.export(level, from1, from2, controllerPos, format);
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("Export failed: " + exception.getMessage()));
            return 0;
        }

        Path directory = FMLPaths.GAMEDIR.get().resolve(EXPORT_DIR);
        Path target = directory.resolve("multiblock-" + FILE_TIME.format(LocalDateTime.now()) + "." + format.extension());
        try {
            Files.createDirectories(directory);
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Failed to write " + target + ": " + exception.getMessage()));
            return 0;
        }

        source.sendSuccess(
            () -> Component.literal("Exported " + format.name().toLowerCase(Locale.ROOT) + " to " + target.toAbsolutePath()),
            false
        );
        return 1;
    }
}
