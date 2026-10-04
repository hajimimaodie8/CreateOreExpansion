package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.StarShockConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
 *       {@link #ORBIT_ESSENCE_POOL} 里<b>每枚各自随机抽</b>一种。</li>
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

    /**
     * <b>伴随波（环绕波）的魔素抽签池</b> —— 需求 coe-ess §3.3 第 2 条：伴随波 = 从"剩下 7 种"
     * （水/火/地/风/冰/雷/毒）里<b>每枚各自随机抽一种</b>。<b>不含异</b>（异是主波的固定魔素，
     * 见 {@link #fireMainWave}）。
     *
     * <p><b>名单只有这一份</b>：{@link #fireOrbitWave} 那一次掷骰直接按本数组取。不许在别的
     * 地方再抄一遍这 7 个名字（抄一份就多一个漂移源：改了一处、另一处照旧）；关卡
     * {@code wave-essence-star-shock} 反过来钉着一条等式 —— <b>本池 = {@code WaveTrailStyle}
     * 里除 {@code ARCANE} 以外的全部魔素</b>，所以将来给枚举追加第 9 种魔素时它会变红，
     * 逼一次"新魔素要不要进伴随波池"的决定。</p>
     *
     * <p><b>不去重、不排除连续相同</b>（作者裁定）：顺序照 {@code WaveTrailStyle} 的声明序排，
     * 便于与枚举逐字对照；抽中的结果只由那一次 {@code world.random} 决定，池里没有任何状态。</p>
     */
    private static final WaveTrailStyle[] ORBIT_ESSENCE_POOL = {
        WaveTrailStyle.WATER, WaveTrailStyle.FIRE, WaveTrailStyle.EARTH, WaveTrailStyle.WIND,
        WaveTrailStyle.ICE, WaveTrailStyle.LIGHTNING, WaveTrailStyle.POISON
    };

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
        /**
         * <b>发射者本人</b>（服务端玩家）。
         *
         * <p>为什么要存它：结算时要把"这一发掷骰/命中/生成了多少"作为<b>游戏内提示</b>发给发射者
         * （{@link StarShockDebug#reportOrbit}，只发本人、不广播），而 {@code abandon}（能量见底断停）
         * 与 {@code forget}（离场/死亡）这两条收尾路径<b>拿不到 player 参数</b> —— 它们只有
         * {@code CASTS.remove(...)} 的结果。存在 Cast 里就三条路径一视同仁。</p>
         */
        private final net.minecraft.server.level.ServerPlayer player;
        /** 该等级的配置在**发射那一刻**定死（中途换甲不会让还在飞的波改变编组语义）。 */
        private final StarShockConfigs.Config config;
        /** 该技能的逐技能等级（1~3）。 */
        private final int level;
        /** 已经发出过几枚主波（含点按那第一枚）。 */
        private int fired;
        /** 已经扣掉的能量（点按 + 长按增量）。 */
        private int paid;
        /**
         * 本次发射里环绕波<b>滚了几次骰</b>（每枚主波各自一次，含点按那第一枚 ——
         * 点按 t = 0 ⇒ 概率 0，那次滚骰必然不中，但它仍然算"问过一次"）。
         */
        private int orbitRolls;
        /** 环绕波掷骰<b>命中</b>了几次（概率通过）。
         *  {@code hits < rolls} = "没滚到"；{@code spawns < hits} = "滚到了但没生成"。 */
        private int orbitHits;
        /** 环绕波真的<b>生成了</b>几枚（命中后建实体并加入世界成功）。 */
        private int orbitSpawned;
        /** 本次发射用过的<b>最高</b>环绕概率（日志用：没滚到时也能看出"当时的概率是多少"）。 */
        private double orbitPeakChance;
        /**
         * 最近一次 {@link #hold} 收到的按住 tick 数（0 = 只有点按那一 tick）。
         *
         * <p>为什么在 Cast 里也存一份：{@link #abandon}（能量见底断停）拿不到 {@code heldTicks}
         * 参数，而结算日志必须能写出"实际蓄了多久"（作者只有日志可验收）。</p>
         */
        private int lastHeldTicks;

        private Cast(net.minecraft.server.level.ServerPlayer player, int batch,
            StarShockConfigs.Config config, int level) {
            this.player = player;
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
        Cast cast = new Cast(player, nextBatch(), config, Math.max(1, Math.min(StarShockConfigs.MAX_LEVEL, level)));
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
        Cast cast = CASTS.get(player.getUUID());
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
            fireMainWave(player, cast, heldTicks);
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
        Cast cast = CASTS.remove(player.getUUID());
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
        logSettlement("松手", cast, heldTicks, paid);
    }

    /**
     * <b>本次发射的唯一一行结算日志</b>（作者实机验收的唯一凭据；作者 2026-10-02 裁定第 4 条
     * "把三处日志合并成一行，字段列清楚，别打三行"）。走 {@link WaveDiag}（波系统唯一日志出口）。
     *
     * <p>一行里同时给出（顺序即字段顺序）：</p>
     * <ol>
     *   <li><b>实际蓄力</b>：按住 tick 数 / 秒数 / 该级蓄力上限秒数 / <b>t</b>；</li>
     *   <li><b>共几枚主波</b>（{@code cast.fired} —— 这就是"3 级满蓄力到底出几枚"的凭据）；</li>
     *   <li><b>环绕波滚没滚到</b>：掷骰次数 / 本次最高概率 / 命中次数 / 生成枚数
     *       ⇒ {@code 命中 0} = "没滚到"；{@code 命中 > 生成} = "滚到了但没生成"；
     *       {@code 生成 > 0} 但看不到 = 观感问题（"生成了但立刻消散"另有一行消散日志，见
     *       {@code AbstractChargerWaveEntity#tick} 的环绕要素收尾）；</li>
     *   <li><b>本次总耗能</b>：总额 + 点按部分 + 长按部分（点按 = 400，长按部分 = 总 − 400）。</li>
     * </ol>
     *
     * @param reason    收尾原因（"松手" / "能量见底断停"）
     * @param cast      本次发射状态（调用方已经从 {@link #CASTS} 摘掉）
     * @param heldTicks 收尾时的按住 tick 数
     * @param paid      本次发射实际扣掉的装备能量（点）
     */
    private static void logSettlement(String reason, Cast cast, int heldTicks, int paid) {
        int waveLevel = StarShockConfigs.waveLevelFor(cast.level);
        double t = StarShockConfigs.chargeProgress(heldTicks, cast.config);
        int holdPart = Math.max(0, paid - cast.config.tapCost());
        WaveDiag.trace(
            "星芒嬗震结算（{}）：技能 {} 级 → {} 级波（{}），实际蓄力 {} tick = {} 秒 / 上限 {} 秒（t={}），"
                + "共发 {} 枚主波；环绕波：掷骰 {} 次（本次最高概率 {}）命中 {} 次 → 生成 {} 枚；"
                + "本次总耗能 {} 点（点按 {} + 长按 {}）",
            reason, cast.level, waveLevel, WaveLevels.glyph(waveLevel),
            heldTicks, fmt2(heldTicks / 20.0D), cast.config.chargeSeconds(), fmt2(t),
            cast.fired,
            cast.orbitRolls, fmt2(cast.orbitPeakChance), cast.orbitHits, cast.orbitSpawned,
            paid, cast.config.tapCost(), holdPart);
        // 游戏内调试提示（临时、可整体移除；默认关闭，只有 /orbitdebug on 过的发射者本人会收到）：
        // 让作者<b>不用切窗口看日志</b>就能判"这一发有没有环绕波生成"。走聊天栏、只发本人、不广播；
        // 一个字符都不写日志文件（波日志的唯一出口仍是 WaveDiag）。
        // 移除方式见 StarShockDebug 类尾注释（删本类 + 删下面这一行）。
        if (cast.player instanceof net.minecraft.server.level.ServerPlayer serverCaster) {
            StarShockDebug.reportOrbit(serverCaster, cast.orbitRolls, cast.orbitHits, cast.orbitSpawned);
        }
    }

    /**
     * 两位小数的<b>与区域设置无关</b>格式化（日志里 {@code t} / 秒数 / 概率都要固定形状：
     * 某些区域会把小数点写成逗号，日志就没法机器比对了）。参数类型必须是 {@code double}。
     */
    private static String fmt2(double value) {
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
        Cast cast = CASTS.remove(player.getUUID());
        if (cast != null) {
            // 离场/死亡也是一条收尾路径：同样打一行（不追扣零头），否则日志里会"少一发"
            logSettlement("离场/死亡（不追扣）", cast, cast.lastHeldTicks, cast.paid);
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
        Cast cast = CASTS.remove(player.getUUID());
        if (cast == null) {
            return;
        }
        // 见底断停也要有一行结算日志：否则"能量见底"这条收尾路径在日志里完全不可见
        //（作者只有日志可验收，见 logSettlement）。
        logSettlement("能量见底断停", cast, cast.lastHeldTicks, cast.paid);
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
        // 主人 = <b>施法玩家本人</b>（2026-10-03 需求 coe-boom2 批 1 §3.2）：本批只<b>赋</b>不<b>排</b>
        // —— 命中谓词一个字没改（那是批 2，等作者裁定）。写在 addFreshEntity 之前：波一进世界
        // 就可能命中，"生成瞬间"的保护必须在那之前把字段写好。缺省（不赋）时字段是"无主人"。
        wave.setOwner(player);
        // 魔素（2026-10-03 需求 coe-ess 批 4，§3.3 第 1 条）：星界套的<b>主波固定 = 异</b>。
        // 必须紧跟在 trySetWaveType 之后：trySetEssence 只在"会伤害的波型"上生效（魔素是攻击波
        // 专有，作者裁定），波型还没转成攻击态时设它会返回 false 且什么都不写。
        // 只设这一处 ⇒ 其它一切攻击波（变器攻击波变态、回旋镖…）保持未设 = 继承波型风格 = 火。
        wave.trySetEssence(WaveTrailStyle.ARCANE);
        // 命中附加效果要素（需求 §3.3(d)）：既有命中链一个字不改，只在链尾追加一次 addEffect。
        // 星辉石凝能佩免疫嬗乱走既有的 MobEffectEvent.Applicable 拦截点，本类不写免疫判据。
        wave.setHitEffect(TransmutationEffects.TRANSMUTATION_DISORDER,
            HIT_DISORDER_TICKS, HIT_DISORDER_AMPLIFIER);
        // 同一次发射的多枚波共用一个批次号 ⇒ 互相豁免碰撞（否则并排的第 2/3 枚出生即自爆）
        wave.setFiringBatch(cast.batch);
        world.addFreshEntity(wave);
        cast.fired++;
        // 波相关日志一律走 WaveDiag（全系统唯一出口，前缀/开关只在那里定义）：
        // 这一行让"技能几级、实际打出几级波、本次第几枚 / 共几枚、蓄力进度 t"在日志里可查
        // —— "3 级满蓄力到底出几枚"就靠这一行自证（作者 2026-10-02 裁定第 3 条：
        //   日志必须能证明"本次发射共 N 枚"）。
        WaveDiag.trace("星芒嬗震发射：技能 {} 级 → {} 级波（{}），本次第 {} 枚 / 共 {} 枚（蓄力 t={}），魔素={}，批次 {}，位置 {}",
            cast.level, waveLevel, WaveLevels.glyph(waveLevel), cast.fired,
            StarShockConfigs.mainWaveCount(heldTicks, cast.config),
            fmt2(StarShockConfigs.chargeProgress(heldTicks, cast.config)),
            WaveTrailStyle.ARCANE.name(), cast.batch, origin);

        // 每枚主波各自 0~1 枚环绕波（需求 §3.3(b)）：概率 = t × 该级上限，点按 t = 0 ⇒ 恒 0。
        // 骰子用世界随机（服务端权威），整发波的形状只由这一次掷骰决定。
        // 掷骰 / 命中 / 生成三个计数都记进 cast：结算那一行据此区分三种情形 ——
        // "没滚到"（命中 0）、"滚到了但没生成"（命中 > 生成）、"生成了"（生成 > 0）。
        double orbitChance = StarShockConfigs.orbitChance(heldTicks, cast.config);
        cast.orbitRolls++;
        if (orbitChance > cast.orbitPeakChance) {
            cast.orbitPeakChance = orbitChance;
        }
        if (orbitChance > 0.0D && world.random.nextDouble() < orbitChance) {
            cast.orbitHits++;
            fireOrbitWave(world, wave, cast, heldTicks);
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
    private static void fireOrbitWave(ServerLevel world, ChargerWaveEntity parent, Cast cast, int heldTicks) {
        int orbitLevel = StarShockConfigs.orbitWaveLevelFor(cast.level);
        ChargerWaveEntity orbit = new ChargerWaveEntity(world, parent.position(), parent.getMovement(), orbitLevel);
        orbit.trySetWaveType(WaveTypes.ATTACK);
        // 主人 = <b>施法玩家本人</b>（2026-10-03 需求 coe-boom2 批 1 §3.2；同上，本批只赋不排）。
        // 取 {@code cast.player} 而不是父波：主人永远是"发这条技能的人"，父波只是环绕锚点
        // （父波自己也带着同一个主人，但两者是<b>两个独立字段</b>，不互相继承）。
        orbit.setOwner(cast.player);
        // 魔素（2026-10-03 需求 coe-ess 批 4，§3.3 第 2 条）：伴随波 = 从 {@link #ORBIT_ESSENCE_POOL}
        // 的 7 种里<b>每枚各自抽一种</b>（与"每枚主波各自滚骰"同粒度 ⇒ 一次长按可能同时出现
        // 2~3 种不同魔素的伴随波，那是预期行为；不去重、不排除与上一枚相同）。
        // 随机源用<b>本波已有的</b> world.random（服务端权威；§3.3 明确"不要 new Random()"，
        // 本仓对可复现性有要求）。池与掷骰都只有这一处，见 ORBIT_ESSENCE_POOL 的说明。
        WaveTrailStyle orbitEssence = ORBIT_ESSENCE_POOL[world.random.nextInt(ORBIT_ESSENCE_POOL.length)];
        orbit.trySetEssence(orbitEssence);
        // 命中附加嬗乱与主波同一份（环绕波也是这条技能的波，打中谁都要挂嬗乱）
        orbit.setHitEffect(TransmutationEffects.TRANSMUTATION_DISORDER,
            HIT_DISORDER_TICKS, HIT_DISORDER_AMPLIFIER);
        // 环绕波算同一批次：继承父波批次号（出生点与主波重合，斜相位两轴分量 0.566 < 0.6 也会相交）
        orbit.setFiringBatch(parent.getFiringBatch());
        // 环绕波要素：父波 UUID + 半径 + 角速度（弧度/tick）+ 初始相位
        orbit.setOrbitAnchor(parent.getUUID(), ORBIT_RADIUS, ORBIT_ANGULAR_SPEED, ORBIT_PHASE);
        world.addFreshEntity(orbit);
        cast.orbitSpawned++;
        WaveDiag.trace("星芒嬗震环绕波：技能 {} 级 → {} 级波（{}，主波一半伤害），本次第 {} 枚主波（t={}，概率 {}），魔素={}；批次 {}（继承父波），绕 {} 的 r={} 格、{} 圈/秒",
            cast.level, orbitLevel, WaveLevels.glyph(orbitLevel), cast.fired,
            fmt2(StarShockConfigs.chargeProgress(heldTicks, cast.config)),
            fmt2(StarShockConfigs.orbitChance(heldTicks, cast.config)),
            orbitEssence.name(), parent.getFiringBatch(), parent.getId(), ORBIT_RADIUS,
            ORBIT_TURNS_PER_SECOND);
    }
}
