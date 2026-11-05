package com.teameway.nowaycore.common.multiblock.machine;

/**
 * 配方执行结果
 *
 * <p>区分材料不足与输出堵塞，机器状态机据此决定重新搜索配方还是原地等待。</p>
 */
public enum CraftResult {
    /**
     * 执行成功
     */
    SUCCESS,
    /**
     * 输入在事务期间被别处拿走，需要重新匹配
     */
    MISSING_INPUT,
    /**
     * 输出池放不下，机器应进入堵塞状态
     */
    OUTPUT_BLOCKED,
    /**
     * 执行过程抛异常
     */
    FAILED
}
