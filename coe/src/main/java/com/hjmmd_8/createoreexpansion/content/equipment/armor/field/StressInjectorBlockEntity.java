package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>应力注入器方块实体</b>（{@code GeneratingKineticBlockEntity} 子类）—— 用户 2026-10-01 规格 §4.2 的
 * "方案 A"：被赋能时<b>真的给应力</b>，停手立刻静止。
 *
 * <h2>容量：我们自己的表，不借 Create 给手摇曲柄的容量</h2>
 * <p>Create 的应力口径是"<b>每 RPM 容量 × |生成转速|</b>"（{@code KineticNetwork#getActualCapacityOf}
 * 与护目镜的 {@code capacity × RPM → SU} 都是这个算法）。所以要精确给出 8192/16384/32768 SU，
 * 我们固定转速 {@link #SPEED_RPM} = 32 RPM，并按等级取
 * {@code 每RPM容量 = stressSu / 32} = <b>256 / 512 / 1024</b>
 * ⇒ 网络里的实际容量 = 256×32 / 512×32 / 1024×32 = <b>8192 / 16384 / 32768 SU</b>，一分不多一分不少。
 * 这条数值链的源头是 {@code FieldChargeConfigs.Config#stressSu()}（唯一真源）。</p>
 *
 * <h2>应力怎么真的进网络（为什么不用轴连接）</h2>
 * <p>Create 的动力学网络是"轴连接图"：两个方块要在某个方向上互相 {@code hasShaftTowards} 才能连上。
 * 而手摇曲柄<b>只有背向一个轴面</b>（{@code HandCrankBlock#hasShaftTowards(face == FACING.getOpposite())}），
 * 那个面正是它挂载的那一格，被"支撑它的方块"占着（{@code canSurvive} 要求那一格有碰撞箱）
 * ⇒ "把注入器放在曲柄旁边、用轴连上"在一般情形下<b>物理上做不到</b>；
 * 若强行让注入器用六面轴去连任意邻居，还会在邻居网络方向相反时被 Create 的
 * {@code RotationPropagator#propagateNewSource} 判定为 {@code incompatible} 并
 * {@code destroyBlock}（注入器当场消失）。</p>
 * <p>所以这里的做法是：<b>用 Create 的公开 API 把本实体的容量直接登记进"被赋能动力源所在的动力网络"</b>
 * ——{@link #registerCapacity()} 里取源方块的 {@code getOrCreateNetwork()}，
 * 调 {@code KineticNetwork#updateCapacityFor(this, 每RPM容量)}。效果与"物理连上"完全一样：
 * 同一张 {@code KineticNetwork} 的 {@code sources} 表里多了一份容量，
 * {@code calculateCapacity()} 会把 {@code 256 × |getGeneratedSpeed()|} = 8192 算进去并
 * {@code sync()} 给全网络成员 ⇒ <b>下游机器真的能跑</b>（应力表读数也会真的涨）。</p>
 * <p>对齐关系（谁必须是谁）：{@code getActualCapacityOf(be) = sources.get(be) × |be.getGeneratedSpeed()|}，
 * 而 {@code getGeneratedSpeed()} 只有在本实体<b>被赋能且心跳未过期</b>时才非零
 * ⇒ 心跳一断，网络里那一条的贡献立刻变成 0（不依赖任何"记得去删"的动作）。</p>
 *
 * <h2>⚠ 刻意<b>不持久化</b>任何状态（孤儿自愈）</h2>
 * <p>本类不覆写 {@code write}/{@code read}：{@code energized} / {@code stressSu} / 心跳全在内存里。
 * 于是"服务器崩了 / 存档关了 / 区块卸载再载入"之后，注入器必然是<b>未赋能</b>态，
 * {@link #tick()} 在闲置宽限期到点后<b>自己把自己移除</b>（{@code level.removeBlock(pos, false)}，
 * 无掉落）⇒ 不会在世界里留下一个永远在提供应力的隐藏方块。
 * 装备技能正常运行时每 tick 都会重新赋能（区块刚载入时的 1 tick 空窗远小于宽限期）。</p>
 *
 * @since 1.0.0
 */
public class StressInjectorBlockEntity extends GeneratingKineticBlockEntity {

    /** 被赋能时的生成转速（我们定；32 RPM 与手摇曲柄同量级，视觉/网络都正常）。 */
    public static final int SPEED_RPM = 32;

    /**
     * 心跳时长（tick）：技能侧每 tick 都会续期，这里留 40 tick（2 秒）余量。
     * 断供超过它 ⇒ 自动卸力（容量贡献归零），避免"技能早结束了、容量还挂在网上"。
     */
    public static final int HEARTBEAT_TICKS = 40;

    /** 闲置宽限期（tick）：未赋能这么久就判定为孤儿方块，自己移除。 */
    private static final int IDLE_GRACE_TICKS = 100;

    /** 是否处于被赋能态（内存状态，不持久化）。 */
    private boolean energized;

    /** 当前提供的应力总量（SU，网络口径）。 */
    private int stressSu;

    /** 心跳到期时刻（{@code level.getGameTime()} 口径）。 */
    private long energizedUntil;

    /** 连续未赋能的 tick 数（孤儿判定）。 */
    private int idleTicks;

    /** 被赋能的动力源位置（容量登记进它的网络；unloaded/被拆掉时登记会失败，由技能侧收尾）。 */
    private @Nullable BlockPos sourcePos;

    public StressInjectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------
    // 赋能 / 卸力（只由 FieldChargeRuntime 调；两侧都安全，客户端不会走到）
    // ------------------------------------------------------------------

    /** 此刻是否真的在提供应力（赋能标记 + 心跳未过期）。 */
    public boolean isEnergized() {
        return energized && level != null && level.getGameTime() <= energizedUntil;
    }

    /** 当前应力总量（SU）；未赋能 ⇒ 0。 */
    public int stressSu() {
        return isEnergized() ? stressSu : 0;
    }

    /**
     * 每 RPM 容量（{@code stressSu / SPEED_RPM}）：256 / 512 / 1024。
     *
     * <p>Create 的网络总容量 = 本值 × |{@link #getGeneratedSpeed()}|，所以总应力恒等于
     * {@code FieldChargeConfigs} 里那一档的 SU。</p>
     */
    public float capacityPerRpm() {
        return stressSu / (float) SPEED_RPM;
    }

    /**
     * 赋能（技能侧每 tick 调一次，<b>幂等</b>）：刷新心跳 + 把容量登记进源方块的动力网络。
     *
     * @param source          被赋能的动力源位置（容量登记到它的网络）
     * @param su              该等级要提供的应力总量（SU）
     * @param heartbeatTicks  心跳时长（tick）
     */
    public void energize(@Nullable BlockPos source, int su, int heartbeatTicks) {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean wasEnergized = isEnergized();
        this.sourcePos = source == null ? null : source.immutable();
        this.stressSu = Math.max(0, su);
        this.energized = true;
        this.energizedUntil = level.getGameTime() + Math.max(1, heartbeatTicks);
        this.idleTicks = 0;
        if (!wasEnergized) {
            // 0 → ±32：走 Create 自己的路径（建网络 / 触发容量重算 / sendData 让客户端也知道）
            updateGeneratedRotation();
        }
        registerCapacity();
    }

    /** 卸力（幂等）：先把容量从动力网络里撤掉，再让自己的转速归零。 */
    public void deEnergize() {
        if (!energized) {
            return;
        }
        // ⚠ 顺序承重：先撤容量（此时本实体还在这个位置上，calculateCapacity 认得出它），再清标记。
        registerCapacity(0.0F);
        this.energized = false;
        this.stressSu = 0;
        this.energizedUntil = 0L;
        if (level != null && !level.isClientSide) {
            updateGeneratedRotation();
        }
    }

    /**
     * 把本实体的容量登记进"被赋能动力源所在的动力网络"（幂等）。
     *
     * <p>为什么每次都要登记而不是只在赋能那一刻登记一次：网络本身会在区块卸载/载入、
     * 成员增删时重建（{@code KineticNetwork} 是每个世界的内存对象），而本实体<b>不持久化</b>，
     * 重载后需要重新挂上去。技能侧每 tick 会调 {@link #energize}，成本只是一次 HashMap 写 +
     * 一次容量比较（Create 的 {@code updateCapacity()} 只在数值变化时才 {@code sync()}）。</p>
     */
    private void registerCapacity() {
        registerCapacity(isEnergized() ? capacityPerRpm() : 0.0F);
    }

    private void registerCapacity(float perRpm) {
        if (!(level instanceof ServerLevel server) || sourcePos == null) {
            return;
        }
        if (!server.isLoaded(sourcePos)) {
            return;
        }
        if (!(server.getBlockEntity(sourcePos) instanceof KineticBlockEntity source)) {
            return;
        }
        KineticNetwork network = source.getOrCreateNetwork();
        if (network == null) {
            // 源方块此刻还没有网络（例如曲柄刚被赋能、尚未 attach）⇒ 下一 tick 会再试。
            return;
        }
        network.updateCapacityFor(this, perRpm);
    }

    // ------------------------------------------------------------------
    // Create 侧：转速、容量、tick
    // ------------------------------------------------------------------

    @Override
    public float getGeneratedSpeed() {
        if (!isEnergized()) {
            return 0.0F;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof StressInjectorBlock)) {
            return 0.0F;
        }
        return convertToDirection(SPEED_RPM, state.getValue(StressInjectorBlock.FACING));
    }

    /** 护目镜/网络读的"每 RPM 容量"：未赋能 ⇒ 0（上限恒等于我们自己的表，绝不借 Create 的曲柄值）。 */
    @Override
    public float calculateAddedStressCapacity() {
        float capacity = isEnergized() ? capacityPerRpm() : 0.0F;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        // ① 心跳断了（技能早已结束 / 服务器重启）⇒ 自己卸力，容量贡献立刻归零。
        if (energized && level.getGameTime() > energizedUntil) {
            deEnergize();
        }
        if (getGeneratedSpeed() == 0.0F) {
            // ② 孤儿自愈：未赋能超过宽限期就把自己从世界里移除（无掉落、无物品形态）。
            //    移除发生在方块实体的 tick 里是安全的：Level#tickBlockEntities 用 iterator，
            //    而 LevelChunk#removeBlockEntityTicker 只是把 ticker 重新绑定到空实现，
            //    迭代器结构不变（已核对 1.21.1 源码）。
            if (++idleTicks > IDLE_GRACE_TICKS && !isRemoved()) {
                level.removeBlock(worldPosition, false);
            }
        } else {
            idleTicks = 0;
            // ③ 区块重载后网络是重建的 ⇒ 每 tick 幂等登记一次，应力不会因为重载而丢。
            registerCapacity();
        }
    }
}
