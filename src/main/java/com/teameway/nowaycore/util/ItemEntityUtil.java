package com.teameway.nowaycore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class ItemEntityUtil {
    public static void dropItemStacks(Level level, BlockPos pos, NonNullList<ItemStack> itemStacks) {
        Containers.dropContents(level, pos, itemStacks);
    }

    public static void dropItemStack(Level level, BlockPos pos, ItemStack itemStack) {
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack);
    }

    public static void dropItem(Level level, BlockPos pos, ItemLike itemLike) {
        dropItemStack(level, pos, new ItemStack(itemLike));
    }
}
