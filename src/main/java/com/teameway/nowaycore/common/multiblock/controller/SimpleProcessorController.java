package com.teameway.nowaycore.common.multiblock.controller;

import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.SimpleController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 简易多方块机器的控制器
 *
 * <p>方块为 {@code noway_core:simple_controller}，结构定义 ID 为 {@code noway_core:simple_processor}，
 * 由 datagen 输出到 {@code data/noway_core/anvillib/definitions/simple_processor.json}。</p>
 */
public class SimpleProcessorController extends SimpleController {
    public SimpleProcessorController(Block block, Identifier definitionId) {
        super(block, definitionId);
    }

    @Override
    public void onFormed(Level level, MultiblockState state) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos pos = state.getControllerPos();
        serverLevel.sendParticles(
            ParticleTypes.END_ROD,
            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            16, 0.4, 0.4, 0.4, 0.02
        );
        serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (serverLevel.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
            controller.form(serverLevel, state);
        }
    }

    @Override
    public void onUnformed(Level level, MultiblockState state) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos pos = state.getControllerPos();
        serverLevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (serverLevel.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
            controller.unform();
        }
    }
}