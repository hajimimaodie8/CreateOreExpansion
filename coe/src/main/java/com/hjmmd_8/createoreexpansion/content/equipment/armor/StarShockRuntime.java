package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.charger.entity.StarShockWaveEntity;
import com.hjmmd_8.createoreexpansion.content.skill.config.StarShockConfigs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * <b>星芒嬗震（星界套 · 槽位 3）的发射运行时</b> —— 用户 2026-10-02 星界轮需求 §3.3。
 *
 * <h2>一次完整的释放</h2>
 * <ol>
 *   <li><b>按下那一 tick = 点按</b>：立刻发出<b>第 1 枚主波</b>（作者原话"点按也是可以的"），
 *       并扣掉点按能量 {@code tapCost}（三档都 400）。这一次按键同时开一个<b>发射批次</b>
 *       （{@link #nextBatch()}），本批次里的全部波共用一个批次号 ⇒ 互相豁免碰撞。</li>
 *   <li><b>长按期间</b>：按蓄力曲线补发主波 —— {@code t} 达到 1/3、2/3、1.0 时依次放出
 *       第 2、第 3 枚（Lv1 上限 1 枚 ⇒ 永不分叉）。每枚主波各自掷一次环绕概率
 *       {@code t × 该级上限}，中了就给它配一枚<b>环绕波</b>。</li>
 *   <li><b>松手</b>：只结算能量与冷却 —— 与蓄能疾骋/绝境守护同一条口径，
 *       波是"按下就发出去了"的，松手不补发也不召回。</li>
 * </ol>
 *
 * <h2>耗能（我定的口径，写在这里以便一句话改）</h2>
 * <pre>
 *   按下那一 tick：扣 tapCost（400）
 *   之后每 tick  ：按 holdCostPerSecond 折算（100 点/秒 ⇒ 每 20 tick 100 点），边按边扣
 *   总计 = 400 + 100 × ceil(按住秒数)
 * </pre>
 * <p>需求 §3.3(e) 给的是"点按一次 400"与"长按期间 100 点/秒"两条，没有说"长按后那 400 还要不要"。
 * 本实现取<b>都要</b>（400 是"发动费"，100/秒是"蓄力费"）：Lv3 按满 1 秒 = 500、Lv1 按满 3 秒 = 700，
 * 与需求里那句"一次满蓄力共 500 / 600 / 700 点"<b>逐值相同</b>（Lv1 400+300、Lv2 400+200、Lv3 400+100）；
 * 也解释了 10 点/秒那一栏为什么三档一样。作者若要"400 抵扣第一秒"，只改这一处公式。</p>
 *
 * <h2>批次号（用户裁定第 10/11/12 条）</h2>
 * <ul>
 *   <li>一次按键 = 一个批次（主波 + 分叉波 + 它们的环绕波共用）；</li>
 *   <li>环绕波<b>算同批</b>（批次号随释放下传）—— 否则第 2 枚起就会自爆；</li>
 *   <li>服务端权威 + 随波实体同步（写进波的 {@code SynchedEntityData}，不走 NBT）。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class StarShockRuntime {

    /**
     * 发射批次计数器（服务端权威，进程内自增）。从 1 开始，{@code 0} 保留给"没有批次"
     * （机器波、老存档）—— 见 {@code AbstractChargerWaveEntity#sameFiringBatch} 里"双方都必须非 0"。
     *
     * <p>用 {@code int} 而不是 UUID：批次号只在"同一次发射内部比较"，不需要全局唯一；</p>
     */
    private static int BATCH_SEQUENCE = 0;

    /** 服务端每次技能发射的状态（点按 + 长按共用一条）。 */
    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    /** 分叉几何：并排横向偏移（格）。±0.35 是"同向并排、能看出是两枚"的量级。 */
    private static final double FORK_SIDE_OFFSET = 0.35D;

    /**
     * 分叉几何：垂直方向的小偏移（格）。第 2、3 枚各取一个方向 ⇒
     * "同向并排 + 垂直方向小偏移"就是需求 §3.3(b) 要的扇形观感。
     */
    private static final double FORK_UP_OFFSET = 0.22D;

    private StarShockRuntime() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 一次发射的状态。 */
    private static final class Cast {
        /** 本次发射的批次号（> 0）。 */
        private final int batch;
        /** 该等级的配置在**发射那一刻**定死（中途换甲不会让还在飞的波改变编组语义）。 */
        private final StarShockConfigs.Config config;
        /** 该技能等级（= 波等级，决定颜色/波速/爆炸等级）。 */
        private final int level;
        /** 已经发出过几枚主波（含点按那第一枚）。 */
        private int fired;
        /** 最近发出的那一枚主波的 UUID（环绕波用它认父波；波实体把 UUID 的高低 32 位 ×2 同步出去）。 */
        private java.util.UUID lastParentId;
        /** 已经扣掉的能量（点按 + 长按增量）。 */
        private int paid;

        private Cast(int batch, StarShockConfigs.Config config, int level) {
            this.batch = batch;
            this.config = config;
            this.level = level;
        }
    }

    /** 分配一个新的批次号（> 0）。 */
    private static synchronized int nextBatch() {
        BATCH_SEQUENCE++;
        if (BATCH_SEQUENCE <= 0) {
            // 溢出兜底：绝不可能在正常游戏里发生，但 int 回绕会让批次号撞上 0（= "无批次"），
            // 那等于"这一批突然可以被任何波湮灭"。绕回 1 即可。
            BATCH_SEQUENCE = 1;
        }
        return BATCH_SEQUENCE;
    }

    // ------------------------------------------------------------------
    // 按下（点按那一 tick）
    // ------------------------------------------------------------------

    /**
     * <b>按下技能键那一 tick</b>：发第 1 枚主波 + 扣点按能量 + 开批次。
     *
     * @param player 释放者
     * @param level  该技能的逐技能等级（1~3）
     * @return 是否真的发出去了（能量不足 ⇒ false，调用方据此给出可见提示）
     */
    public static boolean press(ServerPlayer player, int level) {
        if (player == null) {
            return false;
        }
        StarShockConfigs.Config config = StarShockConfigs.config(level);
        // 点按能量先行（"点按一次 400"是硬口径）：扣不动就当发动失败，一枚波都不发。
        if (!ArmorEnergy.consume(player, config.tapCost())) {
            return false;
        }
        Cast cast = new Cast(nextBatch(), config, Math.max(1, Math.min(StarShockConfigs.MAX_LEVEL, level)));
        cast.paid = config.tapCost();
        CASTS.put(player.getUUID(), cast);
        // 点按那一 tick：t = 0 ⇒ 环绕概率 0（需求 §3.3(b)"点按（t ≈ 0）⇒ 环绕概率 ≈ 0"）
        fireMainWave(player, cast, 0.0D);
        return true;
    }

    // ------------------------------------------------------------------
    // 长按期间
    // ------------------------------------------------------------------

    /**
     * <b>按住期间每 tick 一次</b>：补发主波（按蓄力曲线）+ 按 tick 折算扣能。
     *
     * @param player    释放者
     * @param heldTicks 已按住的服务端 tick 数
     * @return {@code false} = 能量见底、本次释放已被收尾（调用方据此进冷却）
     */
    public static boolean hold(ServerPlayer player, int heldTicks) {
        if (player == null) {
            return false;
        }
        Cast cast = CASTS.get(player.getUUID());
        if (cast == null) {
            return false;
        }
        // ① 按 tick 折算扣能（边按边扣，与临域充力同一条纪律：只扣增量）
        int due = StarShockConfigs.holdCostAfter(heldTicks, cast.config);
        if (due > cast.paid) {
            int delta = due - cast.paid;
            if (!ArmorEnergy.consume(player, delta)) {
                CASTS.remove(player.getUUID());
                return false;
            }
            cast.paid = due;
        }
        // ② 蓄力曲线：该有几枚主波，就补发到几枚（点按那第一枚已经发过）。
        //    补发时按**当前**的 t 掷环绕概率（点按时 t=0 ⇒ 0；越接近蓄满越接近该级上限）。
        double chance = StarShockConfigs.orbitChance(heldTicks, cast.config);
        int target = StarShockConfigs.mainWaveCount(heldTicks, cast.config);
        while (cast.fired < target) {
            fireMainWave(player, cast, chance);
        }
        return true;
    }

    /**
     * <b>松手结算</b>：把剩余不足一步的零头扣掉，然后忘掉本次发射。
     *
     * <p>"零头"指 {@code holdCostAfter} 已经算出来但还没扣掉的那部分（正常路径下 {@link #hold}
     * 每 tick 已经扣过；这里只是恒等保全，与 {@code FieldChargeRuntime#release} 同一条纪律）。</p>
     */
    public static void release(ServerPlayer player, int heldTicks) {
        if (player == null) {
            return;
        }
        Cast cast = CASTS.remove(player.getUUID());
        if (cast == null) {
            return;
        }
        int due = StarShockConfigs.holdCostAfter(heldTicks, cast.config);
        if (due > cast.paid) {
            ArmorEnergy.consume(player, due - cast.paid);
        }
    }

    /** 该技能该起的冷却秒数（点按与长按共用，需求 §3.3(a)）。 */
    public static int cooldownSeconds(int level) {
        return StarShockConfigs.config(level).cooldownSeconds();
    }

    /** 该技能该扣的长按能量（预览/见底判定用；口径与 {@link #hold} 同一处）。 */
    public static int holdCost(int level, int heldTicks) {
        return StarShockConfigs.holdCostAfter(heldTicks, StarShockConfigs.config(level));
    }

    /** 玩家离场/换套/死亡：忘掉发射状态（波照自己的寿命飞完，不追回）。 */
    public static void forget(Player player) {
        if (player != null) {
            CASTS.remove(player.getUUID());
        }
    }

    /**
     * <b>见底即断停</b>：丢掉发射状态、<b>不再追扣零头</b>（已经扣掉的部分不退）。
     *
     * <p>与 {@link #release} 的区别只有一条：release 会补上"不足一步的零头"，
     * 而见底路径下玩家已经付不起了，再扣一次没有意义（其它装备技能的"见底即把剩余能量清空"
     * 由 {@code ArmorSkillRuntime} 统一处理，本方法不碰能量池）。</p>
     */
    public static void abandon(Player player) {
        if (player != null) {
            CASTS.remove(player.getUUID());
        }
    }

    /** 当前处于"发射中"的玩家数（诊断/日志用）。 */
    public static int activeCasts() {
        return CASTS.size();
    }

    // ------------------------------------------------------------------
    // 发射
    // ------------------------------------------------------------------

    /**
     * 发一枚主波（并给这一枚掷一次环绕概率）。
     *
     * <p>分叉几何（需求 §3.3(b) 作者裁定第 16 条"同向并排 + 垂直方向小偏移"）：
     * 第 1 枚沿准心；第 2 枚往准心的右侧偏 {@value #FORK_SIDE_OFFSET} 格、上偏 {@value #FORK_UP_OFFSET} 格；
     * 第 3 枚往左侧偏、下偏。方向一律是准心方向（并排而非散开）⇒ 三枚平行飞、看起来是一把扇形。</p>
     *
     * @param orbitChance 这一枚主波掷环绕概率时用的概率（点按 = 0；长按补发 = t × 该级上限）
     */
    private static void fireMainWave(ServerPlayer player, Cast cast, double orbitChance) {
        ServerLevel world = player.serverLevel();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        Vec3 up = side.cross(look).normalize();

        int index = cast.fired;
        double sideSign = index % 2 == 0 ? 1.0D : -1.0D;
        double upSign = index == 0 ? 0.0D : (index % 2 == 0 ? -1.0D : 1.0D);
        double sideAmount = index == 0 ? 0.0D : FORK_SIDE_OFFSET * ((index + 1) / 2);
        Vec3 offset = side.scale(sideSign * sideAmount).add(up.scale(upSign * FORK_UP_OFFSET));
        // 出生点在眼睛高度、沿准心前推一格，避免刚出生就撞到自己脚下的方块
        Vec3 origin = player.getEyePosition().add(look.scale(1.0D)).add(offset);

        StarShockWaveEntity wave =
            new StarShockWaveEntity(world, origin, look, cast.level);
        wave.setFiringBatch(cast.batch);
        world.addFreshEntity(wave);
        cast.fired++;
        cast.lastParentId = wave.getUUID();

        // 每枚主波各自 0~1 枚环绕波（需求 §3.3(b)：触发概率随长按时长上升，该级上限 50/75/85%）
        if (orbitChance > 0.0D && world.getRandom().nextDouble() < orbitChance) {
            spawnOrbiter(world, cast, origin, look);
        }
    }

    /**
     * 给刚发出的那枚主波配一枚环绕波。
     *
     * <p>初始相位<b>固定为 0</b>（我定的，可一句话改）：0 相位 = 从 {@code u} 方向起步，
     * {@code u} 由"垂直于准心的退化安全基"决定 ⇒ 朝同一方向发射时第一枚环绕波总在同一侧，
     * 可复现、便于实测对照。若要"每枚随机起相位"，改这一行的 {@code 0.0D} 即可。</p>
     *
     * <p>出生位置按圆周点算好（而不是出生在波心再下一 tick 跳出去）：否则第一帧会有一跳，
     * 而玩家看到的就是"环绕波闪一下才到位"。</p>
     */
    private static void spawnOrbiter(ServerLevel world, Cast cast, Vec3 origin, Vec3 look) {
        double startAngle = 0.0D;
        Vec3 spawnPos = StarShockWaveEntity.orbitPos(origin, look,
            StarShockWaveEntity.ORBIT_RADIUS, startAngle);
        StarShockWaveEntity orbiter = new StarShockWaveEntity(world, spawnPos, look, cast.level,
            cast.lastParentId, startAngle);
        orbiter.setFiringBatch(cast.batch);
        world.addFreshEntity(orbiter);
        // 让主波知道"我到寿/被湮灭时要带走谁"这个信息由主波自己在 remove 里按 UUID 找，
        // 所以这里不需要回写主波（见 StarShockWaveEntity#onRemoved）。
    }
}
