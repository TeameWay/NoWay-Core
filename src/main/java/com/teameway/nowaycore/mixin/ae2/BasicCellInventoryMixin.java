package com.teameway.nowaycore.mixin.ae2;

import appeng.me.cells.BasicCellInventory;
import com.teameway.nowaycore.common.item.InfiniteColorApplicatorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasicCellInventory.class)
public class BasicCellInventoryMixin {
    @Inject(method = "isCell", at = @At("HEAD"), cancellable = true)
    private static void nowaycore$infiniteColorApplicatorIsNotACell(
        ItemStack input,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (input != null && input.getItem() instanceof InfiniteColorApplicatorItem) {
            cir.setReturnValue(false);
        }
    }
}
