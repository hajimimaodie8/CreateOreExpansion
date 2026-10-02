package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.skill.config.StarShockConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * <b>星芒嬗震（星界套 · 槽位 3）的发射运行时</b> —— 用户 2026-10-02 星界轮需求 §3.3。
 *
 * <h2>⛔ 它发的是<b>既有</b>能量波，不是新实体（用户 2026-10-02 硬口径）</h2>
 * <p>作者原话：「能量波是由好几个要素定义的…<b>你不要再凭空造出一个新的能量波哈，
 * 不要造出一个攻击波哈</b>」⇒ 本类一律 new <b>既有的</b> {@link ChargerWaveEntity}
 * （实体类型 {@code createoreexpansion:charger_wave}，与三台应力充能器/差波器<b>同一个类型、
 * 同一个渲染器</b>），靠设置波情五要素把它发出去：</p>
 * <ol>
 *   <li><b>波级</b> —— 构造参数 {@code waveLevel} = {@link StarShockConfigs#waveLevelFor(int)}
 *       （技能 1/2/3 级 ⇒ <b>γ / ε / ω</b>；定颜色、波速与伤害，见 {@link WaveLevels}）；</li>
 *   <li><b>波型</b> —— {@code trySetWaveType(WaveTypes.ATTACK)}（攻击态，与"变器攻击波变态"同一个波型）；</li>
 *   <li><b>波速</b> —— 由波级查 {@code WaveLevels#baseSpeed} 得来（6/7/8 格/秒），本类不额外写 speedOffset；</li>
 *   <li><b>波载荷</b> —— <b>不设置</b>（普通能量波不带载荷；只有星辉波变器的 {@code stellar_wave} 带）；</li>
 *   <li><b>剩余寿命</b> —— 不设置（沿用 {@code AbstractChargerWaveEntity} 的 200 tick 上限）。</li>
 * </ol>
 *
 * <h2>发射路径与既有机器<b>逐条同形</b>（不是我另起一条）</h2>
 * <table border="1">
 *   <caption>机器（应力充能器）vs 本类</caption>
 *   <tr><th>步骤</th><th>机器</th><th>本类</th></tr>
 *   <tr><td>取发射点</td><td>{@code AbstractCreateChargerBlockEntity#launchWave(int)}：方块中心 +
 *       FACING 法线 × 1 格（外推 1 格，免得出生就撞自己）</td>
 *       <td>玩家眼睛位置 + 准心 × 1 格（再叠一个分叉偏移）—— 同一个"外推 1 格"口径</td></tr>
 *   <tr><td>方向</td><td>FACING 法线（经 Sable 结构位姿换算成世界向量）</td>
 *       <td>玩家准心向量（玩家永远在主世界，无需结构换算）</td></tr>
 *   <tr><td>造实体</td><td>{@code createWave(...)} ⇒
 *       {@code new ChargerWaveEntity(level, start, movementDir, mode)}</td>
 *       <td>{@code new ChargerWaveEntity(world, origin, look, waveLevel)} —— <b>同一个构造器</b></td></tr>
 *   <tr><td>设波型</td><td>不设（机器波是普通态，靠变器穿波才变态）</td>
 *       <td>{@code trySetWaveType(WaveTypes.ATTACK)}（技能波生来就是攻击态）</td></tr>
 *   <tr><td>放到世界</td><td>{@code targetLevel.addFreshEntity(createWave(...))}</td>
 *       <td>{@code world.addFreshEntity(wave)} —— 同一个调用</td></tr>
 *   <tr><td>音效</td><td>音符盒钟声（机器才有）</td><td>无（技能不额外发声）</td></tr>
 * </table>
 * <p>也就是说：<b>实体类型、构造器、addFreshEntity 三处与机器完全同一条路径</b>；本类只多做了
 * "设波型 = 攻击态"和"盖批次号"两件机器不做的事，而那两件都落在既有字段上、不新增实体。</p>
 *
 * <p>2026-10-02 的上一版曾经为这条技能自造了一个 {@code StarShockWaveEntity}
 * （新实体类型 {@code star_shock_wave}）来承载"自定义伤害 / 环绕波 / 命中嬗乱"，
 * 但那个类型<b>没有渲染器</b> ⇒ 客户端把它渲染进视野即 NPE 崩溃
 * （{@code crash-reports/crash-2026-10-02_14.23.33-client.txt}）。
 * 该实体类型与子类已整体删除，本类改走上面的既有路径。</p>
 *
 * <h2>★ 上一轮"被堵住的两条"的承载方式（作者 2026-10-02 裁定，本轮落地）</h2>
 * <p>作者口径是「能量波是由好几个要素定义的，它是一种出于定义的一种特殊的实体」⇒
 * <b>给既有波增加"要素"符合口径，另造实体才违反</b>。于是那两件事做成
 * {@code AbstractChargerWaveEntity} 上的<b>通用、可选、默认关闭</b>的要素（不新增实体类型、
 * 不新增贴图/模型/渲染器；不设这两个要素时机器波/变器波行为逐字不变）：</p>
 * <ol>
 *   <li><b>环绕波</b>（需求 §3.3(b)(g)）：<b>环绕波要素</b>
 *       {@code setOrbitAnchor(父波 UUID, 半径, 角速度, 相位)} ⇒ 波每 tick 把位置改写成
 *       {@code 父波位置 + r×(u·cosθ + v·sinθ)}（u/v = 垂直于父波运动方向的平面基）；
 *       父波消散 ⇒ 自己 {@code discard()}（不留孤立波）。数值：
 *       {@value #ORBIT_RADIUS} 格半径、{@value #ORBIT_TURNS_PER_SECOND} 圈/秒
 *       （每 tick 2π/20 弧度）、初始相位 0；<b>每枚主波各自</b>按
 *       {@code StarShockConfigs.orbitChance(蓄力进度)} 滚一次（点按 t = 0 ⇒ 概率 0），
 *       环绕波<b>继承父波批次号</b>（半径 0.8 < 两盒判定距离 ⇒ 不同批就出生即自爆），
 *       波级由 {@code StarShockConfigs.orbitWaveLevelFor} 定（主波一半伤害的最近波级 = α/β/β）。</li>
 *   <li><b>命中附加效果</b>（需求 §3.3(d)）：<b>命中附加效果要素</b>
 *       {@code setHitEffect(效果, 持续 tick, amplifier)} —— 既有命中链
 *       （攻击态 ⇒ {@code hurt(WaveLevels.damage(波级))} ⇒ 给穿戴护甲玩家充能 ⇒ 绽放消散）
 *       <b>一个字不改</b>，只在链尾追加一次 {@code addEffect}。本技能设的是
 *       {@code createoreexpansion:transmutation_disorder}，{@value #HIT_DISORDER_TICKS} tick、
 *       amplifier {@value #HIT_DISORDER_AMPLIFIER}；目标穿戴星辉石凝能佩时，
 *       <b>既有</b>拦截点 {@code MedallionEffectHandler#onEffectApplicable}
 *       （{@code MobEffectEvent.Applicable}）自动豁免 —— 本类不写任何免疫判据。</li>
 * </ol>
 * <p>主波伤害不受这两条影响：它由<b>波级</b>决定（{@code WaveLevels#damage}），映射见
 * {@link StarShockConfigs#waveLevelFor(int)}（技能 1/2/3 级 ⇒ 8/10/12 点）。</p>
 *
 * <h2>一次完整的释放</h2>
 * <ol>
 *   <li><b>按下那一 tick = 点按</b>：立刻发出<b>第 1 枚主波</b>（作者原话"点按也是可以的"），
 *       并扣掉点按能量 {@code tapCost}（三档都 400）。这一次按键同时开一个<b>发射批次</b>
 *       （{@link #nextBatch()}），本批次里的全部波共用一个批次号 ⇒ 互相豁免碰撞
 *       （否则同向并排的第 2/3 枚出生瞬间就被第 1 枚湮灭）。</li>
 *   <li><b>长按期间</b>：按蓄力曲线补发主波 —— {@code t} 达到 1/3、2/3、1.0 时依次放出
 *       第 2、第 3 枚（Lv1 上限 1 枚 ⇒ 永不分叉）。</li>
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
 *   <li>一次按键 = 一个批次（主波 + 并排分叉波共用）；</li>
 *   <li>服务端权威 + 随波实体同步（写进波的 {@code SynchedEntityData}，不走 NBT）；</li>
 *   <li>豁免范围只有"同批次"：批次号 0（机器波、别人的波）照旧互相湮灭 —— 长期口径不变。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class StarShockRuntime {

    /**
     * 发射批次计数器（服务端权威，进程内自增）。从 1 开始，{@code 0} 保留给"没有批次"
     * （机器波、老存档）—— 见 {@code AbstractChargerWaveEntity#sameFiringBatch} 里"双方都必须非 0"。
     *
     * <p>用 {@code int} 而不是 UUID：批次号只在"同一次发射内部比较"，不需要全局唯一。</p>
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

    /**
     * <b>环绕波几何：半径</b>（格）—— 需求 §3.3(b) 给的是 <b>0.80 格</b>。
     *
     * <p>⚠ 这个半径就是"环绕波必须继承父波批次号"的原因（两件事都会爆）：</p>
     * <ul>
     *   <li>环绕波的<b>出生点</b>就是主波的位置（相距 0 ⇒ 两盒必然相交）；</li>
     *   <li>飞起来以后，波盒 0.2、各自外扩 0.4 ⇒ 两枚波的盒相交判据是"<b>按轴</b>距离 &lt; 0.6"，
     *       而 0.8 格半径在斜相位（θ ≈ 45°）的两个分量是 0.566 &lt; 0.6 ⇒ 每圈约有 29°
     *       （4 段各 7.2°，合计约 8% 的圆周）两盒相交。</li>
     * </ul>
     * <p>⇒ 不继承批次号就是"第一圈就与主波互相湮灭"（同批豁免见 {@code sameFiringBatch}）。</p>
     */
    private static final double ORBIT_RADIUS = 0.80D;

    /** <b>环绕角速度</b>：需求 §3.3(b) 给的是 <b>1 圈/秒</b>。 */
    private static final double ORBIT_TURNS_PER_SECOND = 1.0D;

    /**
     * <b>环绕角速度（弧度/tick）</b> = 2π × 圈/秒 ÷ 20；1 圈/秒 时 = <b>2π/20 ≈ 0.3142</b>。
     *
     * <p>单位是<b>弧度/tick</b>（不是度/tick）：位置公式里直接进 {@code Math.cos/sin}，
     * 少一次单位换算、少一个"度还是弧度"的歧义点。</p>
     */
    private static final double ORBIT_ANGULAR_SPEED = 2.0D * Math.PI * ORBIT_TURNS_PER_SECOND / 20.0D;

    /** <b>环绕初始相位</b>（弧度）：需求 §3.3(b) 给的是 <b>0</b>（出生在基向量 u 正方向一侧）。 */
    private static final double ORBIT_PHASE = 0.0D;

    /** 命中附加嬗乱的持续 tick 数（需求 §3.3(d)：60 tick = 3 秒）。 */
    private static final int HIT_DISORDER_TICKS = 60;

    /** 命中附加嬗乱的效果等级（需求 §3.3(d)：amplifier 0 = I 级）。 */
    private static final int HIT_DISORDER_AMPLIFIER = 0;

    private StarShockRuntime() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 一次发射的状态。 */
    private static final class Cast {
        /** 本次发射的批次号（> 0）。 */
        private final int batch;
        /** 该等级的配置在**发射那一刻**定死（中途换甲不会让还在飞的波改变编组语义）。 */
        private final StarShockConfigs.Config config;
        /** 该技能的逐技能等级（1~3）。 */
        private final int level;
        /** 已经发出过几枚主波（含点按那第一枚）。 */
        private int fired;
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
        // 点按那一 tick：t = 0 ⇒ 1 枚主波（需求 §3.3(b)"点按（t ≈ 0）⇒ 1 枚"）。
        // ⚠ 这里刻意把蓄力 tick 数写成字面量 0：环绕概率 = t × 上限 ⇒ 点按概率<b>恒为 0</b>
        // （需求 §3.3(b) 的"点按不生成环绕波"就靠这一个入参，不另写分支）。
        fireMainWave(player, cast, 0);
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
        //    每枚主波各自按"当下的蓄力进度"滚一次环绕波（所以 t 要传下去）。
        int target = StarShockConfigs.mainWaveCount(heldTicks, cast.config);
        while (cast.fired < target) {
            fireMainWave(player, cast, heldTicks);
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
     * 发一枚既有能量波（主波 / 并排分叉波）。
     *
     * <p>分叉几何（需求 §3.3(b) 作者裁定第 16 条"同向并排 + 垂直方向小偏移"）：
     * 第 1 枚沿准心；第 2 枚往准心的右侧偏 {@value #FORK_SIDE_OFFSET} 格、上偏 {@value #FORK_UP_OFFSET} 格；
     * 第 3 枚往左侧偏、下偏。方向一律是准心方向（并排而非散开）⇒ 三枚平行飞、看起来是一把扇形。</p>
     *
     * <p>⚠ 偏移量刻意小于碰撞盒外扩半径（0.4）⇒ 三枚一定在彼此的命中盒里，靠<b>同批次豁免</b>
     * 才不自爆（见 {@code AbstractChargerWaveEntity#sameFiringBatch}）。</p>
     *
     * <p>本方法另外给这枚波盖上两个<b>可选要素</b>：命中附加嬗乱（所有主波都有）与
     * ——如果这次掷骰中了——环绕波（见 {@link #fireOrbitWave}）。</p>
     *
     * @param heldTicks 该枚主波发出时的蓄力 tick 数（点按 = 0）：环绕波概率 = t × 该级上限，
     *                  该级上限见 {@code StarShockConfigs.orbitChanceCap}。每枚主波<b>各自</b>滚一次。
     */
    private static void fireMainWave(ServerPlayer player, Cast cast, int heldTicks) {
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

        // 既有能量波实体（与充能器/差波器同一个类型、同一个渲染器、同一个构造器）
        int waveLevel = StarShockConfigs.waveLevelFor(cast.level);
        ChargerWaveEntity wave = new ChargerWaveEntity(world, origin, look, waveLevel);
        // 波型 = 攻击态（与"变器攻击波变态"引燃出来的波型是同一个）
        wave.trySetWaveType(WaveTypes.ATTACK);
        // 命中附加效果要素（需求 §3.3(d)）：既有命中链一个字不改，只在链尾追加一次 addEffect。
        // 星辉石凝能佩免疫嬗乱走既有的 MobEffectEvent.Applicable 拦截点，本类不写免疫判据。
        wave.setHitEffect(TransmutationEffects.TRANSMUTATION_DISORDER,
            HIT_DISORDER_TICKS, HIT_DISORDER_AMPLIFIER);
        // 同一次发射的多枚波共用一个批次号 ⇒ 互相豁免碰撞（否则并排的第 2/3 枚出生即自爆）
        wave.setFiringBatch(cast.batch);
        world.addFreshEntity(wave);
        cast.fired++;
        // 波相关日志一律走 WaveDiag（全系统唯一出口，前缀/开关只在那里定义）：
        // 这一行让"技能几级、实际打出几级波、属于哪个批次"在日志里可查。
        WaveDiag.trace("星芒嬗震发射：技能 {} 级 → {} 级波（{}），批次 {}，位置 {}",
            cast.level, waveLevel, WaveLevels.glyph(waveLevel), cast.batch, origin);

        // 每枚主波各自 0~1 枚环绕波（需求 §3.3(b)）：概率 = t × 该级上限，点按 t = 0 ⇒ 恒 0。
        // 骰子用世界随机（服务端权威），整发波的形状只由这一次掷骰决定。
        double orbitChance = StarShockConfigs.orbitChance(heldTicks, cast.config);
        if (orbitChance > 0.0D && world.random.nextDouble() < orbitChance) {
            fireOrbitWave(world, wave, cast);
        }
    }

    /**
     * 发一枚<b>环绕波</b>：波实体类型 / 构造器 / 波型与主波<b>完全同形</b>，
     * 只多挂一个"环绕波要素"（{@code setOrbitAnchor}）——它<b>不是</b>新实体类型
     * （实体仍是 {@code createoreexpansion:charger_wave}，同一个渲染器）。
     *
     * <p>三件必须一起做的事：</p>
     * <ol>
     *   <li><b>继承父波批次号</b>（{@code setFiringBatch(parent.getFiringBatch())}）：
     *       环绕波出生点与主波重合（相距 0），且半径 {@value #ORBIT_RADIUS} 格在斜相位
     *       （两轴分量 0.566）也落进对方命中盒（按轴判据 0.6）⇒ 不同批就是"第一圈就互相湮灭"
     *       （需求 §3.3(b)"环绕波算同一批次"）；</li>
     *   <li><b>设环绕要素</b>：父波 UUID + 半径 {@value #ORBIT_RADIUS} 格 +
     *       角速度 {@value #ORBIT_ANGULAR_SPEED} 弧度/tick（1 圈/秒）+ 初始相位
     *       {@value #ORBIT_PHASE}；位置由那个要素每 tick 改写，本方法不写位置公式；</li>
     *   <li><b>波级</b> = {@code StarShockConfigs.orbitWaveLevelFor}（"主波一半伤害"的最近波级）
     *       ⇒ 伤害由既有 {@code WaveLevels.damage(波级)} 决定，不新开伤害通道。</li>
     * </ol>
     * <p>父波消散后环绕波自己收尾（"取不到父波 ⇒ discard"在
     * {@code AbstractChargerWaveEntity#applyOrbitElement} 里，本类不再叠第二层机制）。</p>
     */
    private static void fireOrbitWave(ServerLevel world, ChargerWaveEntity parent, Cast cast) {
        int orbitLevel = StarShockConfigs.orbitWaveLevelFor(cast.level);
        ChargerWaveEntity orbit = new ChargerWaveEntity(world, parent.position(), parent.getMovement(), orbitLevel);
        orbit.trySetWaveType(WaveTypes.ATTACK);
        // 命中附加嬗乱与主波同一份（环绕波也是这条技能的波，打中谁都要挂嬗乱）
        orbit.setHitEffect(TransmutationEffects.TRANSMUTATION_DISORDER,
            HIT_DISORDER_TICKS, HIT_DISORDER_AMPLIFIER);
        // 环绕波算同一批次：继承父波批次号（出生点与主波重合，斜相位两轴分量 0.566 < 0.6 也会相交）
        orbit.setFiringBatch(parent.getFiringBatch());
        // 环绕波要素：父波 UUID + 半径 + 角速度（弧度/tick）+ 初始相位
        orbit.setOrbitAnchor(parent.getUUID(), ORBIT_RADIUS, ORBIT_ANGULAR_SPEED, ORBIT_PHASE);
        world.addFreshEntity(orbit);
        WaveDiag.trace("星芒嬗震环绕波：技能 {} 级 → {} 级波（{}，主波一半伤害），批次 {}（继承父波），绕 {} 的 r={} 格、{} 圈/秒",
            cast.level, orbitLevel, WaveLevels.glyph(orbitLevel), parent.getFiringBatch(),
            parent.getId(), ORBIT_RADIUS, ORBIT_TURNS_PER_SECOND);
    }
}
