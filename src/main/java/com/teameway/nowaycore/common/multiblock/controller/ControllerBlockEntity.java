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
import com.teameway.nowaycore.common.multiblock.port.MultiblockPort;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPortScanner;
import com.teameway.nowaycore.common.multiblock.port.MultiblockPortTable;
import com.teameway.nowaycore.common.multiblock.port.PortKind;
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
 * <p>成型后解析机器类型（定义 tag）并建立运行时；未成型时每 20 tick 向 anvillib 补查一次，
 * 因为重启后成型状态由 SavedData 恢复，不会重放成型事件。线程状态只同步进度、耗时、倍数与配方 ID，
 * 不传输任务对象。</p>
 */
public class ControllerBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    private static final int FORM_RETRY_INTERVAL = 20;
    private static final int SAVE_INTERVAL = 20;
    /**
     * 端口摘要各段之间的分隔符
     *
     * <p>坐标内部已用逗号分三段，这里必须换成竖线，否则解析无法按位次拆分。</p>
     */
    private static final char SEPARATOR = '|';
    /**
     * 解析端口摘要用的分隔正则
     *
     * <p>{@link String split} 接收正则，竖线在正则中表示分支，直接传 {@link #SEPARATOR} 会按单字符切分。</p>
     */
    private static final String SEPARATOR_REGEX = "\\|";

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
     * 结构里所有端口的摘要，一项一个端口
     *
     * <p>格式为 {@code 种类|等级|x,y,z}，只在 {@link #form} 与 {@link #unform} 时更新，成型后端口位置不再变化。
     * 使用 {@code String[]} 是因为 {@code BlockPos[]} 会走 {@code DirectArrayRef} 的元素级引用，
     * 当前 LDLib2 版本在这条路径上抛 NPE，编码成字符串同时保证顺序与长度一致。</p>
     */
    @DescSynced
    private String[] ports = new String[0];

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
        this.refreshPorts(table);
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
            this.clearPorts();
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
     * 把端口表编码成字符串数组，供客户端铺端口信息栏
     *
     * <p>托管同步对基本类型与 {@link BlockPos} 有现成编解码器，自定义记录类型要额外注册，
     * 为一个只读列表不值得，因此沿用 {@code String[]}。</p>
     *
     * @param table 端口表
     */
    private void refreshPorts(MultiblockPortTable table) {
        List<String> entries = new ArrayList<>();
        for (PortKind kind : PortKind.values()) {
            for (MultiblockPort port : table.ports(kind)) {
                // 只有方块实体才有位置；扫描器收集的都是方块实体，这里仍按接口取，避免强转
                if (port instanceof BlockEntity entity) {
                    BlockPos pos = entity.getBlockPos();
                    entries.add(kind.getSerializedName() + SEPARATOR + port.tier().name()
                                + SEPARATOR + pos.getX() + "," + pos.getY() + "," + pos.getZ());
                }
            }
        }
        this.ports = entries.toArray(String[]::new);
    }

    private void clearPorts() {
        this.ports = new String[0];
    }

    public BlockPos portPosition(int index) {
        return this.portAt(index).pos();
    }

    public PortKind portKind(int index) {
        return PortKind.byName(this.portAt(index).kind());
    }

    /**
     * @param index 序号
     * @return 该端口的等级名，例如 {@code basic}
     */
    public String portTier(int index) {
        return this.portAt(index).tier();
    }

    public int portCount() {
        return this.ports.length;
    }

    /**
     * 拆开一项端口摘要
     *
     * <p>格式由 {@link #refreshPorts} 写入，只有本类会产出，解析失败按数据损坏处理，
     * 退回到原点与物品输入，避免界面整体崩溃。</p>
     *
     * @param index 序号
     * @return 拆开后的三段
     */
    private PortEntry portAt(int index) {
        String raw = this.ports[index];
        String[] parts = raw.split(SEPARATOR_REGEX, 3);
        if (parts.length != 3) {
            return new PortEntry(PortKind.ITEM_INPUT.getSerializedName(), "", BlockPos.ZERO);
        }
        String[] coordinates = parts[2].split(",");
        if (coordinates.length != 3) {
            return new PortEntry(parts[0], parts[1], BlockPos.ZERO);
        }
        try {
            return new PortEntry(
                parts[0], parts[1], new BlockPos(
                Integer.parseInt(coordinates[0]),
                Integer.parseInt(coordinates[1]),
                Integer.parseInt(coordinates[2])
            )
            );
        } catch (NumberFormatException ignored) {
            return new PortEntry(parts[0], parts[1], BlockPos.ZERO);
        }
    }

    private record PortEntry(String kind, String tier, BlockPos pos) {
    }

    /**
     * 从运行时抓取线程状态快照，用于同步与界面展示
     *
     * <p>只在数组长度变化时重建，元素逐个覆盖，避免每 tick 分配临时对象。</p>
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

    public int threadSlots() {
        return this.threadSlots;
    }

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
     * 机器是否正在处理配方
     *
     * <p>状态由各字段推导，不单独同步：成型看机器类型，工作与否看是否有线程持有配方。</p>
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
