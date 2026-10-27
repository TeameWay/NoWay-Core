package com.teameway.nowaycore.event;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.init.ModItems;
import com.teameway.nowaycore.util.ItemEntityUtil;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = NoWayCore.MOD_ID)
public class PlayerEvents {
    @SubscribeEvent
    public static void rightConcreteBlock(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (level.isClientSide()) return;
        var player = event.getEntity();
        if (!player.getMainHandItem().is(Tags.BlockItems.CONCRETES.item())
            || !player.getOffhandItem().is(Tags.BlockItems.CONCRETES.item())
            || !player.isShiftKeyDown()
        ) {
            return;
        }
        var blockPos = event.getPos();
        var blockState = level.getBlockState(blockPos);
        if (blockState.is(Tags.BlockItems.CONCRETES.block())) {
            ItemEntityUtil.dropItem(
                level,
                blockPos.above(),
                blockState.getBlock()
            );
        }
    }

    @SubscribeEvent
    public static void leftConcreteBlock(PlayerInteractEvent.LeftClickBlock event) {
        var level = event.getLevel();
        if (level.isClientSide()) return;
        var player = event.getEntity();
        if (!player.getMainHandItem().is(Tags.BlockItems.CONCRETES.item())
            || !player.getOffhandItem().is(Tags.BlockItems.CONCRETES.item())
            || !player.isShiftKeyDown()
        ) {
            return;
        }
        var blockPos = event.getPos();
        var blockState = level.getBlockState(blockPos);
        var belowPos = blockPos.below();
        var belowState = level.getBlockState(belowPos);
        if (blockState.is(Tags.BlockItems.CONCRETES.block()) && belowState.is(Tags.BlockItems.CONCRETES.block())) {
            level.setBlockAndUpdate(belowPos, Blocks.AIR.defaultBlockState());
            ItemEntityUtil.dropItem(
                level,
                belowPos,
                ModItems.CONCRETE_DUST
            );
        }
    }
}
