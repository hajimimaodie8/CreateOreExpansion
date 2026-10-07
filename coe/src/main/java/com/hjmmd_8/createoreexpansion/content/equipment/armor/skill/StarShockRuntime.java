package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.StarShockConfigs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergy;

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
 *   <li><b>魔素</b>（2026-10-03 需求 coe-ess 批 4 追加的第六项，纯观感）—— 主波
 *       {@code trySetEssence(WaveTrailStyle.ARCANE)}（固定 = 异）；伴随波从
 *       {@code StarShockWaveLauncher#ORBIT_ESSENCE_POOL} 里<b>每枚各自随机抽</b>一种。</li>
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
 *       {@code StarShockWaveLauncher#ORBIT_RADIUS} 格半径、{@code StarShockWaveLauncher#ORBIT_TURNS_PER_SECOND} 圈/秒
 *       （每 tick 2π/20 弧度）、初始相位 0；<b>每枚主波各自</b>按
 *       {@code StarShockConfigs.orbitChance(蓄力进度)} 滚一次（点按 t = 0 ⇒ 概率 0），
 *       环绕波<b>继承父波批次号</b>（半径 0.8 < 两盒判定距离 ⇒ 不同批就出生即自爆），
 *       波级由 {@code StarShockConfigs.orbitWaveLevelFor} 定（主波一半伤害的最近波级 = α/β/β）。</li>
 *   <li><b>命中附加效果</b>（需求 §3.3(d)）：<b>命中附加效果要素</b>
 *       {@code setHitEffect(效果, 持续 tick, amplifier)} —— 既有命中链
 *       （攻击态 ⇒ {@code hurt(WaveLevels.damage(波级))} ⇒ 给穿戴护甲玩家充能 ⇒ 绽放消散）
 *       <b>一个字不改</b>，只在链尾追加一次 {@code addEffect}。本技能设的是
 *       {@code createoreexpansion:transmutation_disorder}，{@code StarShockWaveLauncher#HIT_DISORDER_TICKS} tick、
 *       amplifier {@code StarShockWaveLauncher#HIT_DISORDER_AMPLIFIER}；目标穿戴星辉石凝能佩时，
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
 *   之后每 tick  ：应付 = holdCostAfter（= floor(min(按住 tick, 蓄力上限 tick) × 100 / 20)），
 *                  只扣"应付 − 已扣"的增量；已扣里含那 400 ⇒ 长按费先被 400 抵扣
 *   满蓄力（t ≥ 1）：**停止扣能** —— 应付不再增长，也没有"零头追扣"
 *                  （作者 2026-10-02 裁定第 2 条；此时玩家**可以继续按着**，只是不再多花钱，
 *                   本类**不做**"到点自动释放"：何时松手由玩家决定）
 *   总计 = max(400, tapCost + floor(min(按住 tick, 蓄力上限 tick) × 100 / 20))
 *        = 400 + max(0, 长按费 − 400)   // 三档按满蓄力分别为 300/200/100 ⇒ 都落在 400
 * </pre>
 * <p>需求 §3.3(e) 给的是"点按一次 400"与"长按期间 100 点/秒"两条；本实现把 400 当"发动费"
 * 并让它<b>抵扣</b>长按费。⚠ <b>与需求表那句"一次满蓄力共 500 / 600 / 700 点"不一致</b>：
 * 因为 400 抵扣，按满蓄力<b>只花 400</b>（长按费 300/200/100 全被 400 吃掉）。这是<b>既有行为</b>，
 * 本轮不动它（作者没要求改口径）；要改成需求表的 500/600/700，把 {@link #press} 里
 * {@code cast.paid = config.tapCost();} 的初值改成 {@code 0} 即可（一处）。</p>
 * <p><b>为什么"超过蓄力上限还按着"以前会多扣</b>：旧写法把计费 tick 直接取 {@code heldTicks}，
 * 于是 t 早已钳到 1.0、枚数与环绕概率都不再变，能量却继续按秒扣（作者 2026-10-02 实测：
 * Lv3 蓄力上限 1 秒、按住 8.5 秒 ⇒ 实扣 ~830 点而不是 400）。现在计费 tick 与 t 用同一个上限
 * （{@code StarShockConfigs#holdCostAfter} 钳 {@code chargeTicks}，扣能分支再包在
 * {@code StarShockConfigs#charging} 里）。</p>
 *
 * <h2>批次号（用户裁定第 10/11/12 条）</h2>
 * <ul>
 *   <li>一次按键 = 一个批次（主波 + 并排分叉波共用）；</li>
 *   <li>服务端权威 + 随波实体同步（写进波的 {@code SynchedEntityData}，不走 NBT）；</li>
 *   <li>豁免范围只有"同批次"：批次号 0（机器波、别人的波）照旧互相湮灭 —— 长期口径不变。</li>
 * </ul>
 *
 * <h2>本类的形状（2026-10-05 行为零变化拆分）</h2>
 * <p>本类保留<b>公开出口与运行态</b>（press / hold / release / forget / abandon / cooldownSeconds /
 * holdCost / activeCasts、批次计数器、发射状态登记表、日志格式化那一处真源），按职责域拆出的同包类只做搬运：</p>
 * <ul>
 *   <li>{@link StarShockCast} —— 一次发射的状态（值类型；登记表仍由本类拥有）；</li>
 *   <li>{@link StarShockWaveLauncher} —— 把主波 / 并排分叉波 / 环绕波发出去（几何、魔素池、命中附加要素）；</li>
 *   <li>{@link StarShockReport} —— 那一行结算日志（松手 / 见底 / 离场三条路径共用）。</li>
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
    private static final Map<UUID, StarShockCast> CASTS = new HashMap<>();

    private StarShockRuntime() {
        throw new AssertionError("This class should not be instantiated");
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
        // 2026-10-06 批 15：这里<b>不再</b>钳位。上面 config(level) 内部已经用同一个上限
        // （StarShockConfigs.MAX_LEVEL）钳过一次，而本方法唯一的调用点
        // （ArmorSkillHoldLoop 传 ArmorSkillRuntime.effectiveLevel(player, set, skill)）
        // 本身就恒落在 [1, EQUIPMENT_SKILL_MAX_LEVEL] ⇒ 第二道钳位是纯多余的一份算术
        // （批 15 之前它在这里又抄了一遍 Math.max(1, Math.min(..))）。
        // 去掉之后，"技能等级夹取"全仓只剩 core 的 SkillEnergyCost#clamp 一处实现。
        StarShockCast cast = new StarShockCast(player, nextBatch(), config, level);
        cast.paid = config.tapCost();
        CASTS.put(player.getUUID(), cast);
        // 点按那一 tick：t = 0 ⇒ 1 枚主波（需求 §3.3(b)"点按（t ≈ 0）⇒ 1 枚"）。
        // ⚠ 这里刻意把蓄力 tick 数写成字面量 0：环绕概率 = t × 上限 ⇒ 点按概率<b>恒为 0</b>
        // （需求 §3.3(b) 的"点按不生成环绕波"就靠这一个入参，不另写分支）。
        StarShockWaveLauncher.fireMainWave(player, cast, 0);
        return true;
    }

    // ------------------------------------------------------------------
    // 长按期间
    // ------------------------------------------------------------------

    /**
     * <b>按住期间每 tick 一次</b>：补发主波（按蓄力曲线）+ 按 tick 折算扣能。
     *
     * <p><b>扣能只在 {@code t < 1} 时发生</b>（作者 2026-10-02 裁定第 2 条）：达到该级蓄力上限后
     * 应付费用不再增长，也没有"零头追扣" —— 玩家<b>可以继续按着</b>（本类刻意不做"到点自动释放"，
     * 何时松手由玩家决定），只是不再多花钱。</p>
     *
     * @param player    释放者
     * @param heldTicks 已按住的服务端 tick 数
     * @return {@code false} = 能量见底、本次释放已被收尾（调用方据此进冷却）
     */
    public static boolean hold(ServerPlayer player, int heldTicks) {
        if (player == null) {
            return false;
        }
        StarShockCast cast = CASTS.get(player.getUUID());
        if (cast == null) {
            return false;
        }
        cast.lastHeldTicks = Math.max(cast.lastHeldTicks, heldTicks);
        // ① 按 tick 折算扣能（边按边扣，与临域充力同一条纪律：只扣增量）。
        //    ⚠ 整支包在 charging(...) =（t < 1）里：满蓄力后**停止扣能**。这是作者 2026-10-02
        //    裁定第 2 条的"源码形状"——计费 tick 的钳位在 StarShockConfigs#holdCostAfter 里
        //    （那处保证金额不涨），这里保证"满蓄力后连算都不算"。
        if (StarShockConfigs.charging(heldTicks, cast.config)) {
            int due = StarShockConfigs.holdCostAfter(heldTicks, cast.config);
            if (due > cast.paid) {
                int delta = due - cast.paid;
                if (!ArmorEnergy.consume(player, delta)) {
                    CASTS.remove(player.getUUID());
                    return false;
                }
                cast.paid = due;
            }
        }
        // ② 蓄力曲线：该有几枚主波，就补发到几枚（点按那第一枚已经发过）。
        //    每枚主波各自按"当下的蓄力进度"滚一次环绕波（所以 t 要传下去）。
        //    枚数上限来自配置表（StarShockConfigs#mainWaveCount 内部读 config.maxMainWaves()）。
        int target = StarShockConfigs.mainWaveCount(heldTicks, cast.config);
        while (cast.fired < target) {
            StarShockWaveLauncher.fireMainWave(player, cast, heldTicks);
        }
        return true;
    }

    /**
     * <b>松手结算</b>：把剩余不足一步的零头扣掉（仍只在 {@code t < 1} 内）、写一行结算日志，
     * 然后忘掉本次发射。
     *
     * <p>"零头"指 {@code holdCostAfter} 已经算出来但还没扣掉的那部分（正常路径下 {@link #hold}
     * 每 tick 已经扣过；这里只是恒等保全，与 {@code FieldChargeRuntime#release} 同一条纪律）。
     * 满蓄力之后既有 {@code holdCostAfter} 自带的 tick 钳位、又有 {@code charging} 这道闸
     * ⇒ <b>不会再出现"满蓄力之后的零头追扣"</b>。</p>
     */
    public static void release(ServerPlayer player, int heldTicks) {
        if (player == null) {
            return;
        }
        StarShockCast cast = CASTS.remove(player.getUUID());
        if (cast == null) {
            return;
        }
        int paid = cast.paid;
        // 零头也只算到蓄力上限（t < 1 之外一分不追）—— 与 hold 同一处判据
        if (StarShockConfigs.charging(heldTicks, cast.config)) {
            int due = StarShockConfigs.holdCostAfter(heldTicks, cast.config);
            if (due > cast.paid) {
                int delta = due - cast.paid;
                ArmorEnergy.consume(player, delta);
                paid = due;
            }
        }
        StarShockReport.logSettlement("松手", cast, heldTicks, paid);
    }

    /**
     * 两位小数的<b>与区域设置无关</b>格式化（日志里 {@code t} / 秒数 / 概率都要固定形状：
     * 某些区域会把小数点写成逗号，日志就没法机器比对了）。参数类型必须是 {@code double}。
     *
     * <p>2026-10-05 行为零变化拆分后由宿主保留（实现一个字未改、只放宽到包级私有）：
     * {@link StarShockWaveLauncher} 的发射行与 {@link StarShockReport} 的结算行都要用它，
     * 日志形状因此仍然只有这一处真源。</p>
     */
    static String fmt2(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
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
        if (player == null) {
            return;
        }
        StarShockCast cast = CASTS.remove(player.getUUID());
        if (cast != null) {
            // 离场/死亡也是一条收尾路径：同样打一行（不追扣零头），否则日志里会"少一发"
            StarShockReport.logSettlement("离场/死亡（不追扣）", cast, cast.lastHeldTicks, cast.paid);
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
        if (player == null) {
            return;
        }
        StarShockCast cast = CASTS.remove(player.getUUID());
        if (cast == null) {
            return;
        }
        // 见底断停也要有一行结算日志：否则"能量见底"这条收尾路径在日志里完全不可见
        //（作者只有日志可验收，见 logSettlement）。
        StarShockReport.logSettlement("能量见底断停", cast, cast.lastHeldTicks, cast.paid);
    }

    /** 当前处于"发射中"的玩家数（诊断/日志用）。 */
    public static int activeCasts() {
        return CASTS.size();
    }
}
