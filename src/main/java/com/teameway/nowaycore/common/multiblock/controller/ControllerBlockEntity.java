package com.teameway.nowaycore.common.multiblock.controller;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teameway.nowaycore.NoWayCore;
import com.teameway.nowaycore.common.multiblock.MultiblockType;
import com.teameway.nowaycore.common.multiblock.machine.MachineJob;
import com.teameway.nowaycore.common.multiblock.machine.MachineRuntime;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPortScanner;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPortTable;
import com.teameway.nowaycore.init.ModRegistries;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 控制器方块实体
 *
 * <p>成型后解析出机器类型（定义 tag）并建立运行时；未成型时每 20 tick 询问一次 anvillib 的管理器
 * （重启后成型状态由 SavedData 恢复，不会重新触发成型事件，所以需要主动补查）。</p>
 *
 * <p>线程状态通过 LDLib2 的托管字段同步给客户端：只同步展示需要的进度、耗时、倍数和配方 ID，
 * 不搬运任务对象本身。</p>
 */
public class ControllerBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    private static final int FORM_RETRY_INTERVAL = 20;
    private static final int SAVE_INTERVAL = 20;

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    private final List<MachineJob> pendingJobs = new ArrayList<>();
    private @Nullable MachineRuntime runtime;
    private int retryCounter;
    private int saveCounter;

    @DescSynced
    private int threadSlots;
    @DescSynced
    private int parallelSlots;
    @DescSynced
    private int[] threadProgress = new int[0];
    @DescSynced
    private int[] threadDuration = new int[0];
    @DescSynced
    private int[] threadBatch = new int[0];
    @DescSynced
    private String[] threadRecipe = new String[0];
    @DescSynced
    private String machineType = "";
    @DescSynced
    private String variantText = "";

    /**
     * @param type  方块实体类型（注册时由构建器传入）
     * @param pos   位置
     * @param state 方块状态
     */
    public ControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return this.syncStorage;
    }

    /**
     * 服务端每 tick 调用
     *
     * @param level 服务端世界
     */
    public void serverTick(ServerLevel level) {
        MachineRuntime current = this.runtime;
        if (current != null) {
            current.tick(level);
            this.refreshStatus();
            if (++this.saveCounter >= SAVE_INTERVAL) {
                this.saveCounter = 0;
                this.setChanged();
            }
            return;
        }
        if (++this.retryCounter < FORM_RETRY_INTERVAL) {
            return;
        }
        this.retryCounter = 0;
        MultiblockState state = dev.anvilcraft.lib.v2.multiblock.dynamic.DynamicMultiblockManager.get(level)
            .getAt(this.worldPosition);
        if (state != null && state.isFormed()) {
            this.form(level, state);
        }
    }

    /**
     * 结构成型时建立运行时
     *
     * @param level 服务端世界
     * @param state 多方块状态
     */
    public void form(ServerLevel level, MultiblockState state) {
        Holder.Reference<MultiblockDefinition> holder = state.getDefinition(level.registryAccess());
        RegistryEntry<MultiblockType, MultiblockType> type = this.resolveType(holder);
        if (type == null) {
            NoWayCore.LOGGER.warn(
                "Multiblock at {} has no matching type tag; controller stays idle", this.worldPosition
            );
            return;
        }
        MultiblockPortTable table = MultiblockPortScanner.scan(level, this.worldPosition, holder.value());
        MachineRuntime created = new MachineRuntime(type.getId(), holder);
        created.formed(table);
        if (!this.pendingJobs.isEmpty()) {
            created.restore(this.pendingJobs);
            this.pendingJobs.clear();
        }
        this.runtime = created;
        this.machineType = type.getId().toString();
        this.refreshStatus();
        this.setChanged();
        this.sync(true);
    }

    private @Nullable RegistryEntry<MultiblockType, MultiblockType> resolveType(
        Holder.Reference<MultiblockDefinition> definition
    ) {
        for (RegistryEntry<MultiblockType, MultiblockType> candidate : NoWayCore.REGISTRUM.getAll(ModRegistries.MULTIBLOCK_TYPES)) {
            if (definition.is(TagKey.create(LibRegistries.DEFINITIONS_KEY, candidate.getId()))) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 结构解散时清理运行时
     */
    public void unform() {
        MachineRuntime current = this.runtime;
        if (current != null) {
            current.unformed();
            this.runtime = null;
            this.machineType = "";
            this.refreshStatus();
            this.setChanged();
            this.sync(true);
        }
    }

    /**
     * @return 机器运行时，未成型返回 {@code null}
     */
    public @Nullable MachineRuntime runtime() {
        return this.runtime;
    }

    /**
     * 从运行时抓取一份线程状态快照，用于同步与界面展示
     *
     * <p>数组长度变化时才重建数组，元素逐个覆盖，避免每 tick 制造垃圾。</p>
     */
    private void refreshStatus() {
        MachineRuntime current = this.runtime;
        List<MachineJob> jobs = current != null ? current.jobs() : List.of();
        int slots = jobs.size();
        if (this.threadProgress.length != slots) {
            this.threadProgress = new int[slots];
            this.threadDuration = new int[slots];
            this.threadBatch = new int[slots];
            this.threadRecipe = new String[slots];
        }
        for (int slot = 0; slot < slots; slot++) {
            MachineJob job = jobs.get(slot);
            if (job == null) {
                this.threadProgress[slot] = -1;
                this.threadDuration[slot] = 0;
                this.threadBatch[slot] = 0;
                this.threadRecipe[slot] = "";
            } else {
                this.threadProgress[slot] = job.progress();
                this.threadDuration[slot] = job.duration();
                this.threadBatch[slot] = job.batch();
                this.threadRecipe[slot] = job.recipeId().toString();
            }
        }
        this.threadSlots = slots;
        this.parallelSlots = current != null ? current.parallels() : 0;
        this.variantText = current == null
                           ? ""
                           : current.variants().stream().map(Identifier::toString).sorted().collect(Collectors.joining(", "));
    }

    /**
     * @return 线程数
     */
    public int threadSlots() {
        return this.threadSlots;
    }

    /**
     * @return 并行倍数上限
     */
    public int parallelSlots() {
        return this.parallelSlots;
    }

    /**
     * @return 机器类型 ID，未成型为空串
     */
    public String machineType() {
        return this.machineType;
    }

    /**
     * @return 当前形态，逗号分隔
     */
    public String variantText() {
        return this.variantText;
    }

    /**
     * @param slot 线程序号
     * @return 进度，0~1
     */
    public float progressAt(int slot) {
        if (slot < 0 || slot >= this.threadProgress.length) {
            return 0.0F;
        }
        int duration = this.threadDuration[slot];
        int progress = this.threadProgress[slot];
        if (duration <= 0 || progress <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, progress / (float) duration);
    }

    /**
     * @param slot 线程序号
     * @return 当前配方的 ID，空闲时为空串
     */
    public String recipeIdAt(int slot) {
        if (slot < 0 || slot >= this.threadRecipe.length) {
            return "";
        }
        String recipe = this.threadRecipe[slot];
        return recipe == null ? "" : recipe;
    }

    /**
     * @param slot 线程序号
     * @return 当前配方的并行倍数，空闲时为 0
     */
    public int batchAt(int slot) {
        return slot < 0 || slot >= this.threadBatch.length ? 0 : this.threadBatch[slot];
    }

    /**
     * @param slot 线程序号
     * @return 当前配方的单周期耗时，空闲时为 0
     */
    public int durationAt(int slot) {
        return slot < 0 || slot >= this.threadDuration.length ? 0 : this.threadDuration[slot];
    }

    /**
     * 判断机器是否正在处理任意一条配方
     *
     * <p>结构状态由这三个字段推导，不再单独同步：成型看机器类型，工作与否看有没有线程在跑配方。</p>
     *
     * @return 至少一个线程持有配方时为 true
     */
    public boolean working() {
        for (String recipe : this.threadRecipe) {
            if (recipe != null && !recipe.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int maxSlot = -1;
        List<SavedJob> saved = new ArrayList<>();
        for (SavedJob entry : input.listOrEmpty("jobs", SavedJob.CODEC)) {
            saved.add(entry);
            maxSlot = Math.max(maxSlot, entry.slot());
        }
        if (maxSlot < 0) {
            return;
        }
        this.pendingJobs.clear();
        for (int slot = 0; slot <= maxSlot; slot++) {
            this.pendingJobs.add(null);
        }
        for (SavedJob entry : saved) {
            this.pendingJobs.set(entry.slot(), new MachineJob(entry.recipe(), entry.batch(), entry.progress()));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        List<MachineJob> jobs = this.runtime != null ? this.runtime.jobs() : this.pendingJobs;
        ValueOutput.TypedOutputList<SavedJob> list = output.list("jobs", SavedJob.CODEC);
        for (int slot = 0; slot < jobs.size(); slot++) {
            MachineJob job = jobs.get(slot);
            if (job != null) {
                list.add(new SavedJob(slot, job.recipeId(), job.batch(), job.progress()));
            }
        }
    }

    private record SavedJob(int slot, Identifier recipe, int batch, int progress) {
        private static final Codec<SavedJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(SavedJob::slot),
            Identifier.CODEC.fieldOf("recipe").forGetter(SavedJob::recipe),
            Codec.INT.optionalFieldOf("batch", 1).forGetter(SavedJob::batch),
            Codec.INT.optionalFieldOf("progress", 0).forGetter(SavedJob::progress)
        ).apply(instance, SavedJob::new));
    }
}
