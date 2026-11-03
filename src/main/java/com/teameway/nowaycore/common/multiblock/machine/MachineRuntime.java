package com.teameway.nowaycore.common.multiblock.machine;

import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPortTable;
import com.teameway.nowaycore.common.recipe.CompiledRecipe;
import com.teameway.nowaycore.common.recipe.MultiblockRecipeIndex;
import com.teameway.nowaycore.common.recipe.TypeIndex;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 机器运行时
 *
 * <p>每个线程一个 job 槽；空闲线程每 tick 尝试匹配，运行中的线程只推进进度。并行倍数在任务开始时
 * 确定，执行时再按当时的实际输入复核，材料不够就缩小批次或放弃任务。</p>
 */
public final class MachineRuntime {
    private static final int BLOCKED_COOLDOWN = 20;
    private static final int FAILED_COOLDOWN = 100;

    private final Identifier machine;
    private final Holder.Reference<MultiblockDefinition> definition;
    private final List<MachineJob> jobs = new ArrayList<>();
    private Set<Identifier> variants = Set.of();
    private long indexVersion = -1L;
    private int parallels = 1;
    private MultiblockPortTable ports;
    private ReservationLedger ledger;

    /**
     * @param machine    机器类型
     * @param definition 当前结构定义
     */
    public MachineRuntime(Identifier machine, Holder.Reference<MultiblockDefinition> definition) {
        this.machine = machine;
        this.definition = definition;
    }

    /**
     * 结构成型
     *
     * @param table 端口表
     */
    public void formed(MultiblockPortTable table) {
        this.ports = table;
        this.parallels = table.parallels();
        this.ledger = new ReservationLedger(table.itemInputs().size(), table.fluidInputs().size());
        while (this.jobs.size() < table.threads()) {
            this.jobs.add(null);
        }
        while (this.jobs.size() > table.threads()) {
            this.jobs.remove(this.jobs.size() - 1);
        }
        NoWayCore.LOGGER.debug(
            "Machine {} formed: {} thread(s), {} parallel(s)", this.machine, table.threads(), table.parallels()
        );
    }

    /**
     * 结构解散
     */
    public void unformed() {
        this.ports = null;
        this.ledger = null;
        this.jobs.clear();
        NoWayCore.LOGGER.debug("Machine {} unformed", this.machine);
    }

    /**
     * 服务端每 tick 调用
     *
     * @param level 服务端世界
     */
    public void tick(ServerLevel level) {
        MultiblockPortTable table = this.ports;
        ReservationLedger reservations = this.ledger;
        if (table == null || reservations == null) {
            return;
        }
        MultiblockRecipeIndex index = MultiblockRecipeIndex.current();
        if (index == null) {
            return;
        }

        this.refreshVariants(index);
        MachineResources resources = MachineResources.of(table);
        reservations.clear();

        for (int slot = 0; slot < this.jobs.size(); slot++) {
            MachineJob job = this.jobs.get(slot);
            if (job == null) {
                RecipeMatcher.Match match = RecipeMatcher.findBest(
                    index, this.machine, this.variants, resources, reservations, table.parallels()
                );
                if (match == null) {
                    continue;
                }
                this.jobs.set(slot, new MachineJob(match.recipe(), match.batch()));
                reservations.reserve(match.allocation());
                NoWayCore.LOGGER.debug(
                    "Machine {} thread {} started {} x{}", this.machine, slot, match.recipe().id(), match.batch()
                );
                continue;
            }

            CompiledRecipe recipe = job.resolve(index, this.machine);
            if (recipe == null || !this.supports(recipe)) {
                if (recipe != null) {
                    NoWayCore.LOGGER.debug(
                        "Machine {} thread {} dropped {}: variants {} no longer satisfy {}",
                        this.machine, slot, recipe.id(), this.variants, recipe.variants()
                    );
                }
                this.jobs.set(slot, null);
                continue;
            }
            if (!job.advance(recipe.time())) {
                continue;
            }

            RecipeMatcher.@Nullable Match match = this.allocate(recipe, job.batch(), resources, reservations);
            if (match == null) {
                this.jobs.set(slot, null);
                NoWayCore.LOGGER.debug("Machine {} thread {} lost its inputs, recipe dropped", this.machine, slot);
                continue;
            }

            CraftResult result = RecipeExecutor.craft(
                level, table, resources, match.allocation(), recipe.recipe().outputs(), match.batch(), level.getRandom()
            );
            NoWayCore.LOGGER.debug(
                "Machine {} thread {} crafted {} x{}: {}", this.machine, slot, recipe.id(), match.batch(), result
            );
            switch (result) {
                case SUCCESS -> {
                    this.jobs.set(slot, null);
                    resources = MachineResources.of(table);
                }
                case OUTPUT_BLOCKED -> job.block(BLOCKED_COOLDOWN);
                case MISSING_INPUT -> this.jobs.set(slot, null);
                case FAILED -> job.block(FAILED_COOLDOWN);
            }
        }
    }

    /**
     * 判断配方要求的形态是否都在机器当前形态里
     *
     * @param recipe 配方
     * @return 是否满足
     */
    private boolean supports(CompiledRecipe recipe) {
        return this.variants.containsAll(recipe.variants());
    }

    private void refreshVariants(MultiblockRecipeIndex index) {
        if (this.indexVersion == index.version()) {
            return;
        }
        this.indexVersion = index.version();
        TypeIndex type = index.type(this.machine);
        if (type == null) {
            this.variants = Set.of();
            return;
        }
        Set<Identifier> collected = new HashSet<>();
        for (CompiledRecipe recipe : type.recipes()) {
            for (Identifier variant : recipe.variants()) {
                if (this.definition.is(TagKey.create(LibRegistries.DEFINITIONS_KEY, variant))) {
                    collected.add(variant);
                }
            }
        }
        this.variants = Set.copyOf(collected);
        NoWayCore.LOGGER.debug("Machine {} variants: {}", this.machine, this.variants);
    }

    private RecipeMatcher.@Nullable Match allocate(
        CompiledRecipe recipe,
        int batch,
        MachineResources resources,
        ReservationLedger reservations
    ) {
        for (int repeats = batch; repeats >= 1; repeats--) {
            ResourceAllocation allocation = RecipeMatcher.plan(recipe, resources, reservations, repeats);
            if (allocation != null) {
                return new RecipeMatcher.Match(recipe, repeats, allocation);
            }
        }
        return null;
    }

    /**
     * 读档恢复任务（允许空槽位）
     *
     * @param saved 按槽位排列的任务列表
     */
    public void restore(List<MachineJob> saved) {
        this.jobs.clear();
        int threads = this.ports != null ? this.ports.threads() : saved.size();
        for (int slot = 0; slot < threads; slot++) {
            this.jobs.add(slot < saved.size() ? saved.get(slot) : null);
        }
        NoWayCore.LOGGER.debug("Machine {} restored {} job slot(s)", this.machine, this.jobs.size());
    }

    /**
     * @return 线程任务列表（只读，可能含空槽位）
     */
    public List<MachineJob> jobs() {
        return java.util.Collections.unmodifiableList(new java.util.ArrayList<>(this.jobs));
    }

    /**
     * @return 机器类型
     */
    public Identifier machine() {
        return this.machine;
    }

    /**
     * @return 当前并行倍数上限
     */
    public int parallels() {
        return this.parallels;
    }

    /**
     * @return 机器当前形态
     */
    public Set<Identifier> variants() {
        return this.variants;
    }
}