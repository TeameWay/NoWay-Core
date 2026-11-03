package com.teameway.nowaycore.common.multiblock.machine;

import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import com.teameway.nowaycore.common.recipe.MultiblockRecipeIndex;
import com.teameway.nowaycore.common.recipe.TypeIndex;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * 一个线程上的任务
 *
 * <p>只保存配方 ID、进度和并行倍数；读档后配方对象按 ID 从索引惰性找回，索引尚未构建时任务会
 * 自动放弃。缓存的配方绑定索引版本，配方重载后会重新解析，避免继续执行旧数据。</p>
 */
public final class MachineJob {
    private final Identifier recipeId;
    private final int batch;
    private int progress;
    private int cooldown;
    private @Nullable CompiledRecipe recipe;
    private long recipeVersion = -1L;
    private int duration;

    /**
     * @param recipeId 配方 ID
     * @param batch    并行倍数
     * @param progress 已进行的 tick 数
     */
    public MachineJob(Identifier recipeId, int batch, int progress) {
        this.recipeId = recipeId;
        this.batch = Math.max(1, batch);
        this.progress = Math.max(0, progress);
    }

    /**
     * @param recipe 已解析的配方
     * @param batch  并行倍数
     */
    public MachineJob(CompiledRecipe recipe, int batch) {
        this(recipe.id(), batch, 0);
        this.recipe = recipe;
        this.recipeVersion = -1L;
        this.duration = recipe.time();
    }

    /**
     * @return 配方 ID
     */
    public Identifier recipeId() {
        return this.recipeId;
    }

    /**
     * @return 并行倍数
     */
    public int batch() {
        return this.batch;
    }

    /**
     * @return 已进行 tick 数
     */
    public int progress() {
        return this.progress;
    }

    /**
     * @return 当前配方的单周期耗时，尚未解析时为 0
     */
    public int duration() {
        return this.duration;
    }

    /**
     * 按 ID 解析配方，索引版本变化时重新查找
     *
     * @param index   配方索引
     * @param machine 机器类型
     * @return 配方，找不到返回 {@code null}
     */
    public @Nullable CompiledRecipe resolve(MultiblockRecipeIndex index, Identifier machine) {
        if (this.recipe != null && this.recipeVersion == index.version()) {
            return this.recipe;
        }
        TypeIndex type = index.type(machine);
        if (type == null) {
            this.recipe = null;
            return null;
        }
        for (CompiledRecipe candidate : type.recipes()) {
            if (candidate.id().equals(this.recipeId)) {
                this.recipe = candidate;
                this.recipeVersion = index.version();
                this.duration = candidate.time();
                return candidate;
            }
        }
        this.recipe = null;
        return null;
    }

    /**
     * 推进一个 tick
     *
     * @param time 配方总时间
     * @return 是否完成
     */
    public boolean advance(int time) {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.progress++;
        return this.progress >= time;
    }

    /**
     * 进入冷却，期间不推进进度
     *
     * @param ticks 冷却 tick 数
     */
    public void block(int ticks) {
        this.cooldown = ticks;
        this.progress = Math.max(0, this.progress - 1);
    }
}
