package com.teameway.nowaycore.util;

/**
 * 长数字缩写
 *
 * <p>按三位一档把长数字换成带单位的形式，例如 114514 显示为 {@code 114K}，避免界面上出现一长串数字。
 * 换算只做截断、不做四舍五入，因此显示值永远不大于真实值，不会让人误以为材料够用。</p>
 *
 * <p>单位依次是 K（千）、M（百万）、B（十亿）、T（万亿）、P（千万亿）、E（百亿亿），
 * 覆盖 {@code long} 的全部取值范围；不足一千的数值原样输出。</p>
 */
public class NumberFormatUtil {
    /**
     * 每一档的单位，下标即档位，0 档不带单位。
     */
    private static final String[] UNITS = {
        "",
        "K",
        "M",
        "B",
        "T",
        "P",
        "E"
    };

    /**
     * 每档的倍数。
     */
    private static final long STEP = 1000L;

    private NumberFormatUtil() {
    }

    /**
     * 把数值缩写成带单位的形式
     *
     * @param value 原始数值，可为负
     * @return 缩写结果，例如 {@code 114514} 返回 {@code 114K}，{@code 999} 返回 {@code 999}
     */
    public static String compact(long value) {
        if (value < 0) {
            // Long.MIN_VALUE 取反会溢出，这里退化为它的绝对值减一，量级仍然正确
            return value == Long.MIN_VALUE ? "-" + compact(Long.MAX_VALUE) : "-" + compact(-value);
        }
        int unit = 0;
        long divisor = 1L;
        while (unit < UNITS.length - 1 && value / divisor >= STEP) {
            divisor *= STEP;
            unit++;
        }
        return (value / divisor) + UNITS[unit];
    }
}