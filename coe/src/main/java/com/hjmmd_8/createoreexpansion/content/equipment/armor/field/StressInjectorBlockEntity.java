package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>应力注入器方块实体</b>（{@code GeneratingKineticBlockEntity} 子类）—— 用户 2026-10-01 规格 §4.2 的
 * "方案 A"：被赋能时<b>真的给应力</b>，停手立刻静止。用户 2026-10-02 实测反馈后<b>重做</b>了"怎么进网络"
 * 与"护目镜怎么读"两件事，见下。
 *
 * <h2>容量：我们自己的表，不借 Create 给手摇曲柄的容量</h2>
 * <p>Create 的应力口径是"<b>每 RPM 容量 × |生成转速|</b>"：</p>
 * <ul>
 *   <li>{@code KineticNetwork#getActualCapacityOf(KineticBlockEntity)}：
 *       {@code return sources.get(be) * getStressMultiplierForSpeed(be.getGeneratedSpeed());}</li>
 *   <li>{@code KineticNetwork#getStressMultiplierForSpeed(float)}：{@code return Math.abs(speed);}</li>
 * </ul>
 * <p>所以我们固定转速 {@link #SPEED_RPM} = 32 RPM，并按等级取
 * {@code 每RPM容量 = stressSu / 32} = <b>256 / 512 / 1024</b>
 * ⇒ 网络里的实际容量 = 256×32 / 512×32 / 1024×32 = <b>8192 / 16384 / 32768 SU</b>，一分不多一分不少。
 * 这条数值链的源头是 {@code FieldChargeConfigs.Config#stressSu()}（唯一真源）。</p>
 *
 * <h2>应力怎么真的进网络（2026-10-02 重做；旧做法为什么会被判"没生效"）</h2>
 * <p><b>① 为什么不能用轴连</b>：Create 的轴连接判据是
 * {@code RotationPropagator#getRotationSpeedModifier} 里的
 * {@code definitionFrom.hasShaftTowards(...) && definitionTo.hasShaftTowards(...)}，
 * 而手摇曲柄只有<b>一个</b>轴面（{@code HandCrankBlock#hasShaftTowards} =
 * {@code face == FACING.getOpposite()}），那一面正是它挂载的那一格 ——
 * {@code HandCrankBlock#canSurvive} 要求那一格有碰撞箱 ⇒ <b>永远被支撑方块占着</b>，
 * 注入器放不进去（注入器本身是 {@code noCollission()}，曲柄也不可能架在它上面）。
 * 强行用六面轴去连任意邻居还会被 {@code RotationPropagator#propagateNewSource} 的
 * {@code incompatible} 分支 {@code world.destroyBlock(pos, true)} 当场销毁。
 * ⇒ <b>方案 1（物理接轴）在本方块上不成立</b>，这是 Create 的几何约束，不是实现偷懒。</p>
 *
 * <p><b>② 现在走的路：Create 自己的"并入某张网络"入口</b> ——
 * {@code KineticBlockEntity#setNetwork(Long)}：</p>
 * <pre>
 * setNetwork(networkIn):
 *     if (network != null) getOrCreateNetwork().remove(this);
 *     network = networkIn;
 *     KineticNetwork network = getOrCreateNetwork();
 *     network.initialized = true;
 *     network.add(this);                      // &lt;&lt;&lt; 关键
 * KineticNetwork#add(be):
 *     if (members.containsKey(be)) return;
 *     if (be.isSource()) sources.put(be, be.calculateAddedStressCapacity());
 *     members.put(be, be.calculateStressApplied());
 *     updateFromNetwork(be);
 *     be.networkDirty = true;
 * </pre>
 * <p>即：注入器<b>成为被赋能方块那张 {@code KineticNetwork} 的正式成员</b>（{@code sources} 与
 * {@code members} 两张表都进），而不是只往 {@code sources} 里塞一个数字。之后：</p>
 * <ul>
 *   <li>{@code KineticNetwork#calculateCapacity()} 把
 *       {@code getActualCapacityOf(注入器)} = 256 × 32 = <b>8192 SU</b> 算进网络总量
 *       （它只要求 {@code be.getLevel().getBlockEntity(be.getBlockPos()) == be}，本实体就在自己那一格上）；</li>
 *   <li>{@code KineticNetwork#updateCapacity()} → {@code sync()} →
 *       {@code updateFromNetwork(currentCapacity, currentStress, size)}
 *       会把<b>网络总量</b>写进每一个成员（曲柄、传动轴、机器、<b>应力表</b>）的 {@code capacity} 字段
 *       ⇒ 应力表（{@code StressGaugeBlockEntity}）的护目镜读数与表针当场跟着涨；</li>
 *   <li>{@code be.networkDirty = true} ⇒ 下一 tick 由 {@code KineticBlockEntity#tick} 走
 *       {@code getOrCreateNetwork().updateNetwork()} 重算，与 Create 自己的方块走的是同一条路径。</li>
 * </ul>
 *
 * <p><b>③ 旧做法（只调 {@code KineticNetwork#updateCapacityFor}）为什么不够</b>：它只写
 * {@code sources} 一张表，注入器<b>不是</b> {@code members} 的成员 ⇒ {@code sync()} 不会给它本人
 * 回写 {@code capacity}/{@code stress}，本实体的 Network NBT（护目镜要读的那一段）永远是空的；
 * 而且它<b>没有自己的网络字段</b>，客户端读到的 {@code capacity} 恒为 0。
 * 现在改成 {@code setNetwork} → {@code add}，这两件事一起解决：注入器既是<b>源</b>（贡献 8192），
 * 也是<b>成员</b>（拿到网络总量并可同步给客户端）。</p>
 *
 * <h2>护目镜怎么读出来（用户 2026-10-02 的验收标准）</h2>
 * <p>Create 的铁律：<b>动力部件（曲柄/轴/齿轮）的护目镜只显示它自己的容量</b>，网络总量只有
 * 应力表会显示 —— 证据是 Create 自己的护目镜提示文案
 * {@code item.create.goggles.tooltip.behaviour1}："<i>Kinetic components</i> show added
 * <i>Stress Impact</i> or <i>Capacity</i>. <i>Stressometers</i> show statistics of their
 * <i>attached kinetic network</i>."，代码侧就是
 * {@code GeneratingKineticBlockEntity#addToGoggleTooltip} 用
 * {@code calculateAddedStressCapacity() * getTheoreticalSpeed()} 算出来的那两行。</p>
 * <p>所以本类做两件事，让玩家<b>对着曲柄就能读</b>（这是用户原本的期待）：</p>
 * <ol>
 *   <li>覆写 {@link #addToGoggleTooltip}：不借 Create 给曲柄的 8 SU/RPM，直接打我们自己的
 *       {@code 8192/16384/32768 SU}，再补一段应力表口径的<b>网络总容量</b>
 *       （用的全是 Create 已有翻译键：{@code gui.goggles.generator_stats} /
 *       {@code tooltip.capacityProvided} / {@code generic.unit.stress} /
 *       {@code gui.goggles.at_current_speed} / {@code gui.stressometer.*}），零新增语言键；</li>
 *   <li>{@code HandCrankGoggleProxyMixin} 让<b>手摇曲柄</b>在"旁边挂着正在供能的注入器"时
 *       把护目镜信息转到这里来（Create 的 {@code IProxyHoveringInformation} 机制，
 *       {@code GoggleOverlayRenderer#proxiedOverlayPosition}）⇒ 玩家瞄准曲柄，
 *       读到的就是注入器这几行。注入器本身是<b>空形状 + 不渲染</b>（看不到也瞄不到），
 *       所以这条显示路径是唯一的、也是玩家唯一需要的入口。</li>
 * </ol>
 * <p>客户端要拿到读数，靠的是 {@code KineticBlockEntity#write} 写进 NBT 的
 * {@code Network{Capacity, Stress}}（本类在 {@link #updateFromNetwork} 里补一次
 * {@code sendData()}，与 {@code StressGaugeBlockEntity#updateFromNetwork} 同款手法）+
 * 本类自己写的 {@code ClientInjectSu}（等级对应的 SU；**只在客户端包上写**）。</p>
 *
 * <h2>⚠ 刻意<b>不持久化</b>任何状态（孤儿自愈）</h2>
 * <p>本类不覆写 {@code write} 的动力字段（只在客户端包上补一个显示值），
 * 并且在 {@code read} 的<b>存档</b>分支里把动力信息清干净：{@code energized} / {@code stressSu} /
 * 心跳全在内存里，{@code network} / {@code speed} 也不落盘。于是"服务器崩了 / 存档关了 /
 * 区块卸载再载入"之后，注入器必然是<b>未赋能</b>态，{@link #tick()} 在闲置宽限期到点后
 * <b>自己把自己移除</b>（{@code level.removeBlock(pos, false)}，无掉落）
 * ⇒ 不会在世界里留下一个永远在提供应力的隐藏方块，也不会留下一个"指向旧网络的幽灵容量"。
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

    /** 客户端显示用的 SU 键（**只在客户端包上写**，不落盘 —— 本类刻意不持久化任何状态）。 */
    private static final String NBT_CLIENT_SU = "ClientInjectSu";

    /** 是否处于被赋能态（内存状态，不持久化）。 */
    private boolean energized;

    /** 当前提供的应力总量（SU，网络口径）。 */
    private int stressSu;

    /** 心跳到期时刻（{@code level.getGameTime()} 口径）。 */
    private long energizedUntil;

    /** 连续未赋能的 tick 数（孤儿判定）。 */
    private int idleTicks;

    /** 被赋能的动力源位置（并入它所在的那张网络；unloaded/被拆掉时登记会失败，由技能侧收尾）。 */
    private @Nullable BlockPos sourcePos;

    /** 上一次真正登记进网络的"每 RPM 容量"（{@code NaN} = 还没登记过/需要重登）。 */
    private float registeredPerRpm = Float.NaN;

    /** 客户端显示副本（服务端恒 0；来自 {@link #NBT_CLIENT_SU}）。 */
    private int clientStressSu;

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
     * <b>护目镜显示用</b>：这台注入器此刻是不是在供能（只看同步过来的状态，客户端也能问）。
     *
     * <p>客户端没有 {@code energized}（本类刻意不同步授权状态），但 {@code speed} 与
     * {@link #NBT_CLIENT_SU} 都是同步过来的 ⇒ 用它俩判定即可。曲柄那边的代理 Mixin 也用它，
     * 决定"要不要把护目镜信息转到这里"（没在供能就回落到曲柄自己的读数）。</p>
     */
    public boolean isProvidingDisplay() {
        return getTheoreticalSpeed() != 0.0F || clientStressSu > 0;
    }

    /**
     * 赋能（技能侧每 tick 调一次，<b>幂等</b>）：刷新心跳 + 并入源方块所在的那张动力网络。
     *
     * @param source          被赋能的动力源位置（容量并入它的网络）
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
        // 0 → ±32：直接把速度字段写成我们的转速，**不走 updateGeneratedRotation()** ——
        // 那个方法会给本实体自己新开一张网络（applyNewSpeed 的 prevSpeed == 0 分支），
        // 而我们要的是"be a member of 曲柄那张网络"，所以网络字段只由 joinSourceNetwork() 设。
        setSpeed(getGeneratedSpeed());
        joinSourceNetwork();
        if (!wasEnergized) {
            // 首次赋能：把转速 / 显示值发给客户端（护目镜读数要用）。
            sendData();
        }
    }

    /** 卸力（幂等）：先把容量从动力网络里撤掉，再让自己的转速归零（并同步给客户端）。 */
    public void deEnergize() {
        if (!energized) {
            return;
        }
        KineticNetwork network = currentNetwork();
        this.energized = false;
        this.stressSu = 0;
        this.energizedUntil = 0L;
        this.registeredPerRpm = Float.NaN;
        this.clientStressSu = 0;
        if (level != null && !level.isClientSide) {
            if (network != null && network.members.containsKey(this)) {
                // 立刻重算：我们的贡献变成 0（getActualCapacityOf 用 getGeneratedSpeed()）。
                network.updateCapacityFor(this, 0.0F);
            }
            setSpeed(0.0F);
            // 客户端拿到 speed = 0 + 空显示值 ⇒ isProvidingDisplay() 变 false，曲柄回落到自己的读数。
            sendData();
        }
    }

    // ------------------------------------------------------------------
    // 并入源方块的动力网络（唯一入口；逐 tick 幂等）
    // ------------------------------------------------------------------

    /**
     * <b>把本实体并入"被赋能动力源所在的动力网络"</b>（Create 自己的入口，逐 tick 幂等）。
     *
     * <p>三条依据（全部来自本机留档的 Create 6.0.10 源码）：</p>
     * <ol>
     *   <li>{@code KineticBlockEntity#setNetwork(Long)}：设字段 + {@code network.add(this)}；</li>
     *   <li>{@code KineticNetwork#add(KineticBlockEntity)}：{@code sources} / {@code members} 两张表
     *       都进（{@code be.isSource()} 为真时写 {@code calculateAddedStressCapacity()}）；</li>
     *   <li>{@code KineticNetwork#updateCapacityFor(KineticBlockEntity, float)}：数值变了才重算 + 同步。</li>
     * </ol>
     *
     * <p>为什么要逐 tick 而不是只登记一次：{@code KineticNetwork} 是每个世界的内存对象
     * （{@code TorquePropagator.networks}），网络会在成员增删、区块卸载/载入时被整体替换或重建 ——
     * 连 {@code setNetwork} 都有"id 相同就早退"的短路，所以这里必须同时检查
     * "网络对象还在不在"（{@code members} 里有没有我），不能只看 id。</p>
     */
    private void joinSourceNetwork() {
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
        boolean joined = false;
        if (this.network == null || !this.network.equals(network.id)) {
            // 首次（或换了一张网络）：走 Create 自己的 setNetwork → network.add(this)。
            setNetwork(network.id);
            joined = true;
        } else if (!network.members.containsKey(this)) {
            // id 相同但网络对象被换过（setNetwork 会因 id 相同而早退）⇒ 直接重新 add。
            network.add(this);
            joined = true;
        }
        float perRpm = capacityPerRpm();
        if (joined || Float.isNaN(registeredPerRpm) || !Mth.equal(registeredPerRpm, perRpm)) {
            network.updateCapacityFor(this, perRpm);
            registeredPerRpm = perRpm;
        }
    }

    /** 本实体此刻挂在哪张网络上（没有则 null）。 */
    private @Nullable KineticNetwork currentNetwork() {
        if (level == null || level.isClientSide || !hasNetwork()) {
            return null;
        }
        return getOrCreateNetwork();
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

    /** 网络/护目镜读的"每 RPM 容量"：未赋能 ⇒ 0（上限恒等于我们自己的表，绝不借 Create 的曲柄值）。 */
    @Override
    public float calculateAddedStressCapacity() {
        float capacity = isEnergized() ? capacityPerRpm() : 0.0F;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    /**
     * 网络把"总量/总应力"回写给我们时，顺手 {@code sendData()} 一次（与
     * {@code StressGaugeBlockEntity#updateFromNetwork} 同款手法）—— 否则客户端手里的
     * {@code Network{Capacity}} 永远是空的，护目镜就读不到网络总量。
     */
    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        sendData();
    }

    /**
     * <b>护目镜读数</b>（本类唯一显示出口；玩家瞄准曲柄时由
     * {@code HandCrankGoggleProxyMixin} 把信息转到这里）。
     *
     * <p>只用 Create 已有的翻译键，零新增语言键：</p>
     * <pre>
     * Generator Stats:                 create:gui.goggles.generator_stats
     * Kinetic Stress Capacity:         create:tooltip.capacityProvided
     * 8192 su at current speed         create:generic.unit.stress + create:gui.goggles.at_current_speed
     * Network Stress                   create:gui.stressometer.title
     * Remaining Capacity               create:gui.stressometer.capacity
     * 8448 su                          create:generic.unit.stress
     * </pre>
     * <p>第一段是<b>我们自己的表</b>（{@code FieldChargeConfigs} 的 8192/16384/32768，客户端从
     * {@link #NBT_CLIENT_SU} 读），第二段是<b>整张网络的总容量</b>（Create 的应力表口径，
     * 客户端从同步过来的 {@code capacity}/{@code stress} 读）。两段一起打，玩家才能同时看到
     * "这台注入器给了多少"和"这条线现在总共多大"。</p>
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        int su = displayStressSu();
        if (su <= 0) {
            // 没在供能 ⇒ 不占用护目镜（曲柄那边会回落到 Create 自己的读数）。
            return false;
        }
        CreateLang.translate("gui.goggles.generator_stats")
            .forGoggles(tooltip);
        CreateLang.translate("tooltip.capacityProvided")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip);
        CreateLang.number(su)
            .translate("generic.unit.stress")
            .style(ChatFormatting.AQUA)
            .space()
            .add(CreateLang.translate("gui.goggles.at_current_speed")
                .style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 1);

        if (capacity > 0) {
            CreateLang.translate("gui.stressometer.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
            CreateLang.translate("gui.stressometer.capacity")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
            float remaining = capacity - stress;
            var value = CreateLang.number(remaining)
                .add(CreateLang.translate("generic.unit.stress"))
                .style(ChatFormatting.AQUA);
            if (!Mth.equal(remaining, capacity)) {
                value.text(ChatFormatting.GRAY, " / ")
                    .add(CreateLang.number(capacity)
                        .add(CreateLang.translate("generic.unit.stress"))
                        .style(ChatFormatting.DARK_GRAY));
            }
            value.forGoggles(tooltip, 1);
        }
        return true;
    }

    /** 显示用的 SU：服务端用真实值，客户端用同步过来的副本。 */
    private int displayStressSu() {
        if (level != null && level.isClientSide) {
            return clientStressSu;
        }
        return stressSu();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        if (clientPacket && stressSu > 0) {
            // 只在客户端包上写：护目镜要读它，但**不落盘**（本类刻意不持久化任何状态）。
            compound.putInt(NBT_CLIENT_SU, stressSu);
        }
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (clientPacket) {
            clientStressSu = compound.getInt(NBT_CLIENT_SU);
            return;
        }
        // 存档分支：把动力信息清干净（本类刻意不持久化）——
        // 否则载入后我们会带着一个"指向旧网络的幽灵容量"（initialize() 会把它当已加载成员登记），
        // 那张网络若被曲柄重新用起来，读数就会凭空多出 8192。
        clearKineticInformation();
        clientStressSu = 0;
        registeredPerRpm = Float.NaN;
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
            return;
        }
        idleTicks = 0;
        // ③ 区块重载 / 网络被重建后重新并入（幂等），应力不会因为重载而丢。
        joinSourceNetwork();
    }
}
