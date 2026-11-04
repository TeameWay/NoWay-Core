package com.teameway.nowaycore.util;

/**
 * 长数字格式化
 *
 * <p>两种形式：按三位一档缩写成带单位的形式（{@code 114.5K}），以及按千位加逗号的完整数字
 * （{@code 1,145,141,919}）。缩写只做截断、不做四舍五入，因此显示值永远不大于真实值，
 * 不会让人误以为材料够用。</p>
 *
 * <p>缩写的单位依次是 K（千）、M（百万）、B（十亿）、T（万亿）、P（千万亿）、E（百亿亿），
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

    /**
     * 千位分隔符。
     */
    private static final char SEPARATOR = ',';

    private NumberFormatUtil() {
    }

    /**
     * 把数值缩写成带单位、保留一位小数的形式
     *
     * <p>整数部分之后保留一位小数：{@code 114514} 返回 {@code 114.5K}。小数位截断而不四舍五入，
     * 显示值依然不大于真实值。</p>
     *
     * <p>小数位为零时省掉小数点，{@code 114000} 返回 {@code 114K}：每个整数都拖一条
     * {@code .0} 只会多占像素，不携带任何信息。不足一千的数值没有单位，原样输出。</p>
     *
     * @param value 原始数值，可为负
     * @return 缩写结果，例如 {@code 114514} 返回 {@code 114.5K}，{@code 1000} 返回 {@code 1K}
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
        if (unit == 0) {
            return Long.toString(value);
        }
        long whole = value / divisor;
        // divisor 是 10 的整数次幂，先除以 10 再取模即可得到十分位，
        // 不必算 remainder * 10 / divisor ——后者在 divisor 接近 long 上限时会溢出
        long tenth = (value % divisor) / (divisor / 10);
        if (tenth == 0L) {
            return whole + UNITS[unit];
        }
        return whole + "." + tenth + UNITS[unit];
    }

    /**
     * 保留两位小数的普通数字
     *
     * <p>用于速率这类小数才有意义的值：整数部分超过一千时交给 {@link #compact(long)} 缩写，
     * 避免 {@code 12345.67/t} 这种既长又读不出量级的写法。</p>
     *
     * @param value 数值，可为负
     * @return 格式化结果
     */
    public static String decimal(float value) {
        if (Math.abs(value) >= STEP) {
            return compact((long) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    /**
     * 流体体积格式化
     *
     * <p>按量级切换单位：不足一桶用 {@code mB}；一桶以上用 {@code B}，保留一位小数；
     * 超过一千桶改用 {@link #compact(long)} 缩写成 {@code 1KB}、{@code 1MB} 这样的形式。
     * 换算同样只做截断，显示值不大于真实值。</p>
     *
     * <p>桶以上的后缀直接复用 {@code compact}：一千桶写作 {@code 1K} 再拼上 {@code B}，
     * 就是需求里说的 {@code 1KB}。它把十亿记作 {@code B}，因此十亿桶会显示成 {@code 1BB}，
     * 虽然拗口但单位含义不含糊——换成别的字母反而与既有的量级缩写冲突。</p>
     *
     * @param millibuckets 体积，单位 mB
     * @return 格式化结果，例如 {@code 128000} 返回 {@code 128 B}，{@code 1000000} 返回 {@code 1KB}
     */
    public static String fluidVolume(long millibuckets) {
        if (millibuckets < 0) {
            return millibuckets == Long.MIN_VALUE
                   ? "-" + fluidVolume(Long.MAX_VALUE)
                   : "-" + fluidVolume(-millibuckets);
        }
        if (millibuckets < STEP) {
            return millibuckets + " mB";
        }
        long buckets = millibuckets / STEP;
        long tenth = (millibuckets % STEP) / 100L;
        if (buckets < STEP) {
            return tenth == 0L ? buckets + " B" : buckets + "." + tenth + " B";
        }
        return compact(buckets) + "B";
    }

    /**
     * 千位分隔
     *
     * <p>不做本地化：分隔符恒为半角逗号，分组恒为三位，因此同一个数值在任何语言下渲染结果都一致。
     * {@code String.format("%,d", ...)} 依赖默认区域设置，德语等区域会换成句点，
     * 放在界面文本里会与小数点混淆，所以这里自己分组。</p>
     *
     * @param value 原始数值，可为负
     * @return 分组结果，例如 {@code 1145141919} 返回 {@code 1,145,141,919}
     */
    public static String grouped(long value) {
        String digits = Long.toString(value);
        int start = digits.charAt(0) == '-' ? 1 : 0;
        StringBuilder result = new StringBuilder(digits.length() + (digits.length() - start) / 3 + 1);
        if (start == 1) {
            result.append('-');
        }
        for (int index = start; index < digits.length(); index++) {
            // 从右往左每三位切一刀：剩余位数能被 3 整除的位置之前补一个分隔符
            if (index > start && (digits.length() - index) % 3 == 0) {
                result.append(SEPARATOR);
            }
            result.append(digits.charAt(index));
        }
        return result.toString();
    }
}
