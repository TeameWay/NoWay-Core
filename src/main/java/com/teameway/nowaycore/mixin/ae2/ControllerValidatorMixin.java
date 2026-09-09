package com.teameway.nowaycore.mixin.ae2;

import appeng.me.pathfinding.ControllerValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ControllerValidator.class)
public class ControllerValidatorMixin {
    @ModifyConstant(method = "visitNode", constant = @Constant(intValue = 7))
    private int nowaycore$maxControllerSize(int constant) {
        return 16;
    }
}
