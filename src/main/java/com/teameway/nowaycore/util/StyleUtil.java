package com.teameway.nowaycore.util;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class StyleUtil {

    public static Style colorFromRatio(double speed) {
        double ratio = getRatio(speed);

        int r = (int) (255d * (Math.clamp(2 - 2 * ratio, 0, 1)));
        int g = (int) (255d * (Math.clamp(2 * ratio, 0, 1)));
        int rgb = 0xFF000000 + (r << 16) + (g << 8);

        return Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(rgb));
    }

    private static double getRatio(double speed) {
        return (Math.sin(System.currentTimeMillis() * Math.PI / (speed * 50)) + 1.0) / 2.0;
    }
}
