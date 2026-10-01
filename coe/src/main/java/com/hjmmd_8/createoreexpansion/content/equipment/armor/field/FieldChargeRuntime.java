package com.hjmmd_8.createoreexpansion.content.equipment.armor.field;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillFx;
import com.hjmmd_8.createoreexpansion.content.skill.config.FieldChargeConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>临域充力（宝石套 · 槽位 2）的运行时</b> —— 用户 2026-10-01 规格 §8 第 3 层。
 *
 * <h2>一次完整的发动（规格 §2.1 逐条）</h2>
 * <ol>
 *   <li><b>判定</b>：以玩家为中心、<b>边长 = 2×radius+1 的立方体</b>内至少存在一个"已登记的动力源方块"
 *       （{@link StressSourceRegistry}；手摇曲柄是第一个实现）。一个都没有 ⇒ {@code NO_SOURCE}，
 *       调用方只提示、<b>不扣能、不进冷却</b>（规格 §2.1 第 1 条 + 第 3 层清单）。</li>
 *   <li><b>随机抽一个</b>曲柄（洗牌后逐个试，试不通就换下一个），先看它<b>有没有空位放注入器</b>
 *       （纯判定），再 {@code drive} 它真转起来。</li>
 *   <li><b>放注入器</b>：{@link StressSourceRegistry} 给出的候选位里挑第一个可替换的格子，
 *       放上 {@code stress_injector} 并给它赋能（容量按等级 8192/16384/32768 SU，
 *       登记进曲柄所在的动力网络）。</li>
 *   <li>全都放不下 ⇒ {@code NO_SPACE}，同样<b>不扣能、不进冷却</b>（用户："放不下就换位置/换曲柄，
 *       全都放不下按发动失败处理"）。</li>
 * </ol>
 *
 * <h2>按住期间（每 tick）</h2>
 * <ul>
 *   <li>{@code keepAlive} 续期曲柄（Create 的手摇曲柄 {@code inUse} 每 tick 自减 1，不续期就自己停）；</li>
 *   <li>注入器 {@code energize}（刷新心跳 + 幂等把容量登记进网络）；</li>
 *   <li><b>按"点/秒"累计扣能</b>：{@code due = floor(heldTicks × energyPerSecond / 20)}，
 *       只把<b>增量</b>交给 {@link ArmorEnergy#consume}（四件平摊、全有或全无）——
 *       这与其它装备技能"松手一次性按比例扣"不同，是规格 §2.3"长按期间持续消耗能量"的字面实现；</li>
 *   <li>粒子：曲柄周围<b>蓝→红渐变圈状环绕</b>粒子 + 玩家身上<b>蓝→红拖尾</b>（规格 §2.1 第 3 条 + §0.3）。</li>
 * </ul>
 *
 * <h2>退出路径（<b>每一条都要收尾</b>，用户最强调的一条）</h2>
 * <p>松手（{@code release} 分支）· 到时限（{@link HoldResult#TIME_UP}）· 能量见底
 * （{@link HoldResult#ENERGY_OUT}）· 登出/死亡（{@code ArmorSkillRuntime#forget}）
 * · 换维度或曲柄/注入器消失（{@link HoldResult#INVALID}）· <b>会话心跳超时</b>
 * （{@link #watchdog}，兜住任何我没枚举到的路径）⇒ 全部走同一个 {@link #finish}
 * （移除注入器 → 曲柄静止 → 忘掉会话）。</p>
 *
 * <h2>为什么还要一个 watchdog</h2>
 * <p>{@code ArmorSkillRuntime} 的长按状态是"按槽位"的，而技能 id 是按<b>当前生效的那一套</b>
 * 解析出来的。玩家在长按期间脱甲/换套，槽位就解析不出 {@code field_charge} 了 ——
 * 那条 hold 会被别的东西接管，{@code finish} 就没人调。所以
 * {@code ArmorSkillHandler} 每个服务端 tick 还会给每个玩家调一次 {@link #watchdog}：
 * 会话超过 {@link #WATCHDOG_TICKS} tick 没被续期 ⇒ 强制收尾。这是"漏一处就会留下永远在转的曲柄"
 * 的兜底。</p>
 *
 * @since 1.0.0
 */
public final class FieldChargeRuntime {

    /** {@link #start} 的结果：{@code OK} 之外的两个都按"发动失败"处理（提示 + 不扣能 + 不进冷却）。 */
    public enum StartResult {
        /** 发动成功（注入器已放下、曲柄已在转）。 */
        OK,
        /** 立方体内一个已登记的动力源方块都没有（规格 §2.1 第 1 条）。 */
        NO_SOURCE,
        /** 有动力源，但所有候选曲柄的相邻格都放不下注入器。 */
        NO_SPACE
    }

    /** {@link #hold} 的结果：非 {@code ACTIVE} 表示这一 tick 已经收尾。 */
    public enum HoldResult {
        /** 仍在供能。 */
        ACTIVE,
        /** 到时长上限（规格 §2.2 durationSeconds）⇒ 已收尾。 */
        TIME_UP,
        /** 能量扣不起了 ⇒ 已收尾（调用方按"见底即清空"再清一次剩余能量）。 */
        ENERGY_OUT,
        /** 换维度 / 曲柄没了 / 注入器没了 ⇒ 已收尾。 */
        INVALID
    }

    /** 会话心跳宽限（tick）：这么久没被 {@link #hold} 续期 ⇒ 强制收尾（漏路径兜底）。 */
    private static final int WATCHDOG_TICKS = 5;

    /** 玩家 → 当前这一次临域充力的会话（键的写法与 {@code ArmorSkillRuntime#HOLD_TICKS} 同款）。 */
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private FieldChargeRuntime() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 一次供能的全部状态（**内存态**：服务器重启/换维度都会重新开始，绝不会留下幽灵会话）。 */
    private static final class Session {
        private final ResourceKey<Level> dimension;
        private final StressSourceKind kind;
        private final BlockPos source;
        private final BlockPos injector;
        /** 等级在**发动那一刻**定死：中途换甲不会让已放的注入器容量漂移。 */
        private final FieldChargeConfigs.Config config;
        /** 已经扣掉的能量（与 {@code FieldChargeConfigs.costAfter} 逐值对齐）。 */
        private int paid;
        /** 上一次被续期的 tick（watchdog 用）。 */
        private long lastRefresh;

        private Session(ResourceKey<Level> dimension, StressSourceKind kind, BlockPos source,
                        BlockPos injector, FieldChargeConfigs.Config config, long now) {
            this.dimension = dimension;
            this.kind = kind;
            this.source = source;
            this.injector = injector;
            this.config = config;
            this.lastRefresh = now;
        }
    }

    // ------------------------------------------------------------------
    // 发动
    // ------------------------------------------------------------------

    /**
     * 发动（{@code ArmorSkillRuntime} 在"这一槽位刚开始长按"时调一次）。
     *
     * @param level 该技能的逐技能等级（1~3，见 {@code ArmorSkillRuntime#effectiveLevel}）
     */
    public static StartResult start(ServerPlayer player, int level) {
        if (player == null) {
            return StartResult.NO_SOURCE;
        }
        // 残留会话（理论上不该有）先收尾，绝不允许"两次发动共用一个注入器坐标"。
        finish(player);
        ServerLevel world = player.serverLevel();
        FieldChargeConfigs.Config config = FieldChargeConfigs.config(level);
        List<BlockPos> candidates =
            StressSourceRegistry.findSources(world, player.blockPosition(), config.radius());
        if (candidates.isEmpty()) {
            return StartResult.NO_SOURCE;
        }
        // "随机抽取一个"（规格 §2.1 第 2 条）：洗牌后用玩家自己的随机源做种子。
        Collections.shuffle(candidates, new Random(player.getRandom().nextLong()));
        for (BlockPos source : candidates) {
            BlockState sourceState = world.getBlockState(source);
            StressSourceKind kind = StressSourceRegistry.kindOf(sourceState);
            if (kind == null || !kind.matches(sourceState)) {
                continue;
            }
            // ① 先纯判定"这一格放得下注入器吗"（无副作用）——放不下直接换下一个曲柄，
            //    避免出现"驱动了一个又立刻停下"的抖动。
            BlockPos socket = pickSocket(world, kind, source);
            if (socket == null) {
                continue;
            }
            // ② 驱动它**真转**（驱动不了就换下一个；此刻还没有副作用需要回滚）。
            if (!kind.drive(world, source)) {
                continue;
            }
            // ③ 放注入器 + 赋能（容量登记进曲柄的网络）。失败 ⇒ 回滚驱动，换下一个。
            if (!placeInjector(world, socket, source, config)) {
                kind.halt(world, source);
                continue;
            }
            SESSIONS.put(player.getUUID(),
                new Session(world.dimension(), kind, source.immutable(), socket.immutable(), config,
                    world.getGameTime()));
            CoeCore.LOGGER.info("[临域充力] 发动：等级={} 应力={}SU 时长={}s 曲柄={} 注入器={}",
                level, config.stressSu(), config.durationSeconds(), source, socket);
            return StartResult.OK;
        }
        return StartResult.NO_SPACE;
    }

    /** 在候选位里挑第一个"可替换"的格子（世界已加载 + 不是别的注入器 + {@code canBeReplaced()}）。 */
    private static @Nullable BlockPos pickSocket(ServerLevel world, StressSourceKind kind, BlockPos source) {
        List<BlockPos> sockets = kind.injectorSockets(world, source);
        if (sockets == null || sockets.isEmpty()) {
            return null;
        }
        java.util.ArrayList<BlockPos> shuffled = new java.util.ArrayList<>(sockets.size());
        for (BlockPos pos : sockets) {
            if (pos != null) {
                shuffled.add(pos.immutable());
            }
        }
        Collections.shuffle(shuffled, new Random(world.getRandom().nextLong()));
        for (BlockPos pos : shuffled) {
            if (canPlaceInjector(world, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** 那一格能不能放注入器（纯判定）。 */
    private static boolean canPlaceInjector(ServerLevel world, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return false;
        }
        if (!world.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        if (state.is(CoeBlocks.STRESS_INJECTOR.get())) {
            // 已经是注入器：可能是别的会话占着，也可能是上一次没收干净的孤儿（它会自己移除）
            return false;
        }
        return state.canBeReplaced();
    }

    /**
     * 放注入器并赋能。
     *
     * @return {@code false} = 放不下（已回滚，没留下任何痕迹）
     */
    private static boolean placeInjector(ServerLevel world, BlockPos socket, BlockPos source,
                                         FieldChargeConfigs.Config config) {
        BlockState state = CoeBlocks.STRESS_INJECTOR.get()
            .defaultBlockState()
            .setValue(StressInjectorBlock.FACING, facingTowards(socket, source));
        if (!world.setBlock(socket, state, Block.UPDATE_ALL)) {
            return false;
        }
        if (!(world.getBlockEntity(socket) instanceof StressInjectorBlockEntity injector)) {
            // 方块实体没建起来（理论上不会）：立刻把方块撤掉，不留半成品
            world.removeBlock(socket, false);
            return false;
        }
        injector.energize(source, config.stressSu(), StressInjectorBlockEntity.HEARTBEAT_TICKS);
        return true;
    }

    /** 从 {@code from} 指向 {@code to} 的方向（只可能差一格，所以按轴从大到小取）。 */
    private static Direction facingTowards(BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int dz = Integer.signum(to.getZ() - from.getZ());
        if (dx != 0) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (dy != 0) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        if (dz != 0) {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return Direction.NORTH;
    }

    // ------------------------------------------------------------------
    // 按住期间
    // ------------------------------------------------------------------

    /**
     * 每 tick 续期（{@code ArmorSkillRuntime} 在"这一槽位仍被按住"时调）。
     *
     * @param heldTicks 已按住的服务端 tick 数
     */
    public static HoldResult hold(ServerPlayer player, int heldTicks) {
        if (player == null) {
            return HoldResult.INVALID;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return HoldResult.INVALID;
        }
        session.lastRefresh = player.level().getGameTime();
        ServerLevel world = player.serverLevel();
        // ① 换维度 ⇒ 收尾（规格要求"换维度"也在退出路径里）
        if (!world.dimension().equals(session.dimension)) {
            finish(player);
            return HoldResult.INVALID;
        }
        // ② 曲柄必须还是那个动力源；注入器必须还在（被玩家挖掉 / 被 Create 的传播判定销毁 ⇒ 收尾）
        BlockState sourceState = world.getBlockState(session.source);
        if (!session.kind.matches(sourceState)) {
            finish(player);
            return HoldResult.INVALID;
        }
        if (!(world.getBlockEntity(session.injector) instanceof StressInjectorBlockEntity injector)) {
            finish(player);
            return HoldResult.INVALID;
        }
        // ③ 续期：曲柄继续转 + 注入器心跳/容量登记
        session.kind.keepAlive(world, session.source);
        injector.energize(session.source, session.config.stressSu(),
            StressInjectorBlockEntity.HEARTBEAT_TICKS);
        // ④ 按"点/秒"累计扣能（只扣增量；扣不动 ⇒ 见底）
        int due = FieldChargeConfigs.costAfter(heldTicks, session.config);
        if (due > session.paid) {
            int delta = due - session.paid;
            if (!ArmorEnergy.consume(player, delta)) {
                finish(player);
                return HoldResult.ENERGY_OUT;
            }
            session.paid = due;
        }
        // ⑤ 到时长上限 ⇒ 收尾（规格 §7 Q7：停止供能 + 移除注入器 + 进冷却）
        if (heldTicks >= session.config.durationTicks()) {
            finish(player);
            return HoldResult.TIME_UP;
        }
        // ⑥ 粒子（每 2 tick 一次；形态见 ArmorSkillFx，颜色一律取自宝石套 GEM_STOPS）
        if (player.tickCount % 2 == 0) {
            ArmorSkillFx.fieldChargeRing(world, session.source, player.tickCount * 0.25D);
            ArmorSkillFx.gemTrail(player);
        }
        return HoldResult.ACTIVE;
    }

    // ------------------------------------------------------------------
    // 松手结算
    // ------------------------------------------------------------------

    /**
     * <b>松手结算</b>（{@code ArmorSkillRuntime#release} 的临域充力分支）：补上最后不足一步的零头，
     * 然后走 {@link #finish} 收尾。用户 2026-10-01："中途松开即终止"供能。
     *
     * <p>为什么还要"补零头"：能量是<b>每 tick 扣增量</b>的（{@link #hold}），
     * 正常路径下松手那一刻的 {@code costAfter(heldTicks)} 已经扣过，这里是<b>恒等保全</b>——
     * 万一某个 tick 的 hold 没跑到（例如会话刚被判定失效），也保证"用多少扣多少"与累计式一致。</p>
     */
    public static void release(ServerPlayer player, int heldTicks) {
        if (player == null) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) {
            int due = FieldChargeConfigs.costAfter(heldTicks, session.config);
            if (due > session.paid) {
                ArmorEnergy.consume(player, due - session.paid);
                session.paid = due;
            }
        }
        finish(player);
    }

    // ------------------------------------------------------------------
    // 收尾
    // ------------------------------------------------------------------

    /**
     * <b>唯一的收尾口</b>（幂等）：移除注入器 → 曲柄立刻静止 → 忘掉会话。
     *
     * <p>冷却与"见底清空"由调用方（{@code ArmorSkillRuntime}）按各自口径处理，
     * 因为"登出/死亡"不该起冷却、而"松手/到限/见底"要起。</p>
     */
    public static void finish(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        teardown(player.getServer(), session);
        CoeCore.LOGGER.info("[临域充力] 收尾：曲柄={} 注入器={} 已扣能量={}",
            session.source, session.injector, session.paid);
    }

    /** 玩家离场/死亡：与 {@code ArmorSkillRuntime#forget} 同一条路径（不收冷却，冷却本来就在持久数据里）。 */
    public static void forget(Player player) {
        if (player == null) {
            return;
        }
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        teardown(player.getServer(), session);
    }

    /**
     * <b>服务器关闭时把所有会话收干净</b>（由 {@code ArmorSkillHandler} 挂 {@code ServerStoppingEvent}）。
     *
     * <p>为什么值得单独一条：{@code ServerStoppingEvent} 在存档写盘<b>之前</b>触发，
     * 所以在这里移除注入器、让曲柄归零，存档里就<b>不会留下任何孤儿方块</b>
     * （否则要靠注器实体的"闲置自愈"在下次载入后才清掉）。
     * 只有硬崩溃（kill -9 / 断电）才会走到那条自愈路径。</p>
     */
    public static void finishAll(@Nullable net.minecraft.server.MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        java.util.List<UUID> players = new java.util.ArrayList<>(SESSIONS.keySet());
        for (UUID id : players) {
            Session session = SESSIONS.remove(id);
            if (session != null) {
                teardown(server, session);
            }
        }
        CoeCore.LOGGER.info("[临域充力] 服务器关闭：已收尾 {} 个会话", players.size());
    }

    /**
     * 会话心跳兜底：超过 {@link #WATCHDOG_TICKS} tick 没被 {@link #hold} 续期就强制收尾。
     *
     * <p>由 {@code ArmorSkillHandler} 每个服务端 tick 给每个在线玩家调一次。</p>
     */
    public static void watchdog(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (player.level().getGameTime() - session.lastRefresh <= WATCHDOG_TICKS) {
            return;
        }
        CoeCore.LOGGER.warn("[临域充力] 会话心跳超时（{} tick 未续期）⇒ 强制收尾，"
            + "曲柄={} 注入器={}", WATCHDOG_TICKS, session.source, session.injector);
        finish(player);
    }

    /**
     * 把世界侧的东西收干净：<b>先移除注入器</b>（顺带把容量从网络里撤掉），<b>再让曲柄静止</b>。
     *
     * <p>顺序承重：注入器先撤容量再消失，曲柄再失速 ⇒ 不会出现"曲柄还在转、容量却没了"的一瞬间，
     * 也不会留下一个没有任何宿主却仍在提供应力的方块。</p>
     */
    private static void teardown(@Nullable net.minecraft.server.MinecraftServer server, Session session) {
        if (server == null) {
            // 服务器已经在关了：方块会随存档保存，下次载入时注入器（不持久化状态）会自己移除，
            // 曲柄的 inUse 也会在 10 tick 内自减到 0 ⇒ 不会永远在转。
            return;
        }
        ServerLevel world = server.getLevel(session.dimension);
        if (world == null) {
            return;
        }
        removeInjector(world, session.injector);
        session.kind.halt(world, session.source);
    }

    /** 移除注入器（无掉落；只移除"那一格确实是注入器"的情况）。 */
    private static void removeInjector(ServerLevel world, BlockPos pos) {
        if (!world.isLoaded(pos)) {
            return;
        }
        if (!world.getBlockState(pos).is(CoeBlocks.STRESS_INJECTOR.get())) {
            return;
        }
        if (world.getBlockEntity(pos) instanceof StressInjectorBlockEntity injector) {
            injector.deEnergize();
        }
        // removeBlock（不是 destroyBlock）⇒ 不产生任何掉落物。
        world.removeBlock(pos, false);
    }

    /** 诊断用：当前有会话的玩家数（只给日志/调试看）。 */
    public static int activeSessions() {
        return SESSIONS.size();
    }
}
