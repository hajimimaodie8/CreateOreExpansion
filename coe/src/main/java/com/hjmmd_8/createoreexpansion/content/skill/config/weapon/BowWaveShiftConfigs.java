package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

/**
 * <b>弓被动「量波置换」（宝石弓专属，作者 2026-10-05 弓技能批 4）的数值真源</b> ——
 * 与 {@link BowCurseConfigs} / {@link BowDisarmConfigs} 同形，是这条被动<b>唯一</b>写数字的地方：
 * 等级表（伴随波枚数 / 主波波级）、重力量、环绕几何（半径 / 角速度 / 相位）、炮口前推量
 * 全部只在这里写一遍，发射点（{@code BowWaveShiftLauncher}）与物品侧
 * （{@code JadeTopazBowItem}）<b>一个数字都不写</b>。
 *
 * <h2>作者原话（逐字）</h2>
 * <blockquote>
 * 发射出的弓箭替换为具有同样重力效果，但是在水中能够沿直线飞行的随机魔素攻击能量波，
 * 但是在空中有实体重力，效果与弓箭相似，可能比较不好做，你自己掂量掂量。<br>
 * 与此同时：2 级生成一枚小伴随波，效果也是沿着飞出能量波轨迹的垂直平面环绕，与星界套装的技能相似；
 * 3 级生成两枚。
 * </blockquote>
 *
 * <h2>⛔ 它不定义"波"，只定义"发几枚、多重、怎么绕"</h2>
 * <p>作者 2026-10-02 的硬口径（见 {@code StarShockRuntime} 类注释）是"能量波是由好几个要素定义的，
 * <b>不要再凭空造出一个新的能量波</b>"⇒ 本被动发的就是既有的 {@code ChargerWaveEntity}
 * （实体类型 {@code createoreexpansion:charger_wave}，与三台应力充能器/差波器<b>同一个类型、
 * 同一个渲染器</b>），靠既有要素把它发出去：</p>
 * <ul>
 *   <li><b>波级</b> —— {@link #mainWaveLevelFor(int)}（技能等级 → 波级；定颜色/波速/伤害）；</li>
 *   <li><b>波型</b> —— 攻击态（{@code WaveTypes.ATTACK}，就是"变器攻击波变态"引燃出来的那一个）；</li>
 *   <li><b>魔素</b> —— 每枚<b>各自随机</b>抽一种，池子复用 {@code BowMetaArrowTrait#randomEssence}
 *       （= 那八种魔素的<b>同一条池子</b>，本类不另抄一份名单）；</li>
 *   <li><b>重力</b> —— <b>本批新增的波要素</b>（{@code AbstractChargerWaveEntity#setGravity}），
 *       强度 = {@link #GRAVITY_BLOCKS_PER_SECOND_SQUARED}；</li>
 *   <li><b>环绕</b> —— <b>既有的环绕波要素</b>（{@code setOrbitAnchor}：垂直平面 + 半径 + 角速度
 *       + 相位；与星芒嬗震/回旋镖那条完全同一套机制，不新造第二套）；</li>
 *   <li><b>批次</b> —— 同一次发射的主波与伴随波共用一个批次号（{@code setFiringBatch}），
 *       否则出生瞬间就互相湮灭（见 {@code AbstractChargerWaveEntity#sameFiringBatch}）。</li>
 * </ul>
 *
 * <h2>耗能与冷却：<b>本被动都不新增</b></h2>
 * <p>它挂在既有的"<b>无箭射击</b>"那条路上（{@code JadeTopazBowItem#prepareProjectiles} 的
 * "无箭但能量够 ⇒ 耗 {@code NO_ARROW_COST} 造一支无形魔法箭"分支）⇒ 耗能就是那条路本来就付的
 * {@code JadeTopazBowItem.NO_ARROW_COST}（<b>已有真源，本类不复制那个数</b>），
 * 冷却 <b>无</b>（作者没给；它是一条被动，和「元矢自生」同形：没有技能条目、没有键位、没有冷却）。</p>
 *
 * <h2>重力为什么不是"原版箭那一个 0.05 格/tick²"</h2>
 * <p>原版箭的重力确实是 {@code 0.05 格/tick²}（{@code AbstractArrow#getDefaultGravity}），
 * 换算成格/秒² 是 <b>20</b>。但那是配"满蓄力约 60 格/秒"的初速的量级：把 20 格/秒² 直接搬到本波
 * （β 波 = <b>4 格/秒</b>）身上，波飞 1 秒只前进 4 格却下落 10 格 —— 那不叫"效果与弓箭相似"，
 * 叫"出门就砸脚面"。</p>
 * <p>所以本表取的是<b>同一个"落差 / 水平距离"比</b>，再按本波速度折算：原版箭在 1 秒时的
 * 落差/水平 = ½·20/60 ≈ <b>0.17</b>；本波 4 格/秒要得到同一个比 ⇒
 * {@code g = 2 × 0.17 × 4 ≈ }<b>{@value #GRAVITY_BLOCKS_PER_SECOND_SQUARED}</b> 格/秒²。
 * 观感：飞 2 秒时前进 8 格、落下约 2.7 格 —— 一条看得见的抛物线，与"要抬枪口才打得远"的弓箭手感同形。</p>
 * <p>⚠ <b>这个数是本项目自定的第一版手感值</b>（作者只给了"与弓箭相似"这句话，没给数）：
 * 要拉平/拉陡，只改这一行（本类是全仓唯一取值点，关卡 {@code bow4-gravity-source} 读它）。</p>
 *
 * @since 1.0.0
 */
public final class BowWaveShiftConfigs {

    private BowWaveShiftConfigs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 三档（装备/武器技能 3 级封顶，与其余各条同口径；表长 3 就是"上限 3"本身）。 */
    public static final int MAX_LEVEL = 3;

    /**
     * 单级「量波置换」定义。
     *
     * @param companionWaves 额外生成的<b>伴随波</b>枚数（Lv1 = 0 / Lv2 = 1 / Lv3 = 2，作者给死）
     * @param waveLevel      主波波级（本波实际打出的伤害/速度/颜色由它决定，见 {@link WaveLevels}）
     */
    public record Level(int companionWaves, int waveLevel) {
    }

    // ========== 三个等级（作者 2026-10-05 给死：1 级只发那枚波 / 2 级 +1 / 3 级 +2） ==========

    /** Lv1 —— 只发那枚波（0 枚伴随波），主波 α。 */
    public static final Level LEVEL_1 = new Level(0, 1);

    /** Lv2 —— 额外 1 枚伴随波（<b>宝石弓走的就是这一档</b>），主波 β。 */
    public static final Level LEVEL_2 = new Level(1, 2);

    /** Lv3 —— 额外 2 枚伴随波，主波 γ。 */
    public static final Level LEVEL_3 = new Level(2, 3);

    /** 按等级取配置（与其余各条 {@code *Configs} 同名同形；越界先夹到 [1, 3]）。 */
    public static Level level(int level) {
        return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, LEVEL_1, LEVEL_2, LEVEL_3);
    }

    /**
     * <b>本被动对哪一档弓生效</b>（作者 2026-10-05："宝石弓专属技能"）。
     *
     * <p>刻意写成<b>穷尽 switch（无 default）</b>：将来给枚举加一档弓，这里会<b>编译不过</b>，
     * 而不是静默地让新弓"什么也不发生"。今天恰好只有宝石弓这一档为 {@code true}
     * ⇒ 翠玉 / 星界 / 雷鸣三把弓的射击（含它们各自继承的两条技能）<b>一个字节都不变</b>。</p>
     *
     * <p><b>后续批次怎么接进来</b>（作者已排的两批，本批不实现）：星界弓「星元波置」= "等同量波置换
     * <b>2 级</b>" ⇒ 只需把下面那一行注释换成 {@code case ASTRAL -> true;}，并在物品侧把发射等级
     * 传成 {@code LEVEL_2} 对应值（{@code 2}）；雷鸣弓「雷鸣神力」是另一套（电荷 + 真劈雷），
     * 与本被动无共用的数值，但它若要复用"发波 + 环绕波"那段，同样只需要一个 {@code true}。</p>
     */
    public static boolean appliesTo(BowTier tier) {
        if (tier == null) {
            return false;
        }
        return switch (tier) {
            case SAPPHIRE_RUBY -> true;
            // 星界弓「星元波置」= 等同量波置换 2 级（作者 2026-10-05 排的下一批）：
            // case ASTRAL -> true;
            case JADE_TOPAZ, ASTRAL, THUNDER -> false;
        };
    }

    /** 该技能等级的<b>伴随波枚数</b>（0 / 1 / 2，作者给死）。 */
    public static int companionWaveCount(int level) {
        return level(level).companionWaves();
    }

    /** 该技能等级的<b>主波波级</b>（1/2/3 ⇒ α/β/γ）。 */
    public static int mainWaveLevelFor(int level) {
        return level(level).waveLevel();
    }

    /**
     * <b>伴随波的波级</b> —— 口径照抄星芒嬗震的 {@code StarShockConfigs#orbitWaveLevelFor}：
     * <b>取伤害不小于"主波伤害一半"的最低波级</b>（"取不到一半就宁高不宁低"）。
     *
     * <p>实算（{@link WaveLevels#damage} = 4 / 6 / 8 / 10 / 12，主波 α/β/γ = 4 / 6 / 8，
     * 一半 = 2 / 3 / 4）：三个等级都落在 <b>α（伤害 4）</b> —— 这正是作者要的
     * "一枚<b>小</b>伴随波"（伤害最小、颜色最淡的那一档）。</p>
     *
     * <p>刻意<b>写规则而不是写死 {@code WaveLevels.LOW}</b>：将来主波波级表变大（或者作者把
     * 伴随波改成"主波同款"）时，这里跟着算，而不是留下一个与主波脱节的常数。
     * ⚠ 伴随波的<b>波速</b>对观感没有意义（它的位置每 tick 被环绕要素改写），
     * 有意义的只有伤害与颜色。</p>
     */
    public static int companionWaveLevelFor(int level) {
        float wanted = WaveLevels.damage(mainWaveLevelFor(level)) / 2.0F;
        int chosen = WaveLevels.LOW;
        for (int lv = WaveLevels.LOW; lv <= WaveLevels.MAX_LEVEL; lv++) {
            chosen = lv;
            if (WaveLevels.damage(lv) >= wanted) {
                break;
            }
        }
        return chosen;
    }

    /**
     * <b>重力要素的强度</b>（格/秒²）—— 唯一取值点，口径与来由见类注释那一节。
     *
     * <p>它<b>不是</b>本类的字段而是常量：波实体只提供"要素"（{@code setGravity(强度)}），
     * 强度由发射点传进去；要按等级分档（作者没要求）就把本方法改成查等级表。</p>
     */
    public static final double GRAVITY_BLOCKS_PER_SECOND_SQUARED = 1.35D;

    /**
     * <b>伴随波的环绕半径</b>（格）：照抄星芒嬗震那条口径的 <b>0.80</b>。
     *
     * <p>⚠ 这个半径是"伴随波必须继承主波批次号"的原因（两件事都会爆）：伴随波出生点就是主波位置
     * （相距 0），飞起来后波盒 0.2 + 各自外扩 0.4 ⇒ 相交判据是"按轴距离 &lt; 0.6"，
     * 而 0.8 格半径在斜相位（两轴分量 0.566）也落进对方盒里 ⇒ 不同批就是"第一圈就互相湮灭"。</p>
     */
    public static final double ORBIT_RADIUS = 0.80D;

    /** <b>伴随波角速度</b>：照抄星芒嬗震 —— <b>1 圈/秒</b>。 */
    public static final double ORBIT_TURNS_PER_SECOND = 1.0D;

    /**
     * <b>环绕角速度</b>（弧度/tick）= {@code 2π × 圈/秒 ÷ 20}；1 圈/秒 时 ≈ 0.3142。
     *
     * <p>单位是<b>弧度/tick</b>（位置公式直接进 {@code Math.cos/sin}）。
     * "一秒 = 多少 tick"取 {@link ChargeConfigs#TICKS_PER_SECOND} 这个全仓唯一换算因数，
     * 不在本类写 20。</p>
     */
    public static final double ORBIT_ANGULAR_SPEED =
        2.0D * Math.PI * ORBIT_TURNS_PER_SECOND / ChargeConfigs.TICKS_PER_SECOND;

    /**
     * <b>伴随波的基准相位</b>（弧度）：照抄星芒嬗震 —— <b>0</b>（出生在环平面基向量 u 的正方向一侧）。
     *
     * <p>需要多枚伴随波时（Lv3 = 2 枚）由 {@link #orbitPhaseFor(int, int)} 在圆上<b>均匀切片</b>，
     * 不把它们叠在同一个点上。</p>
     */
    public static final double ORBIT_PHASE = 0.0D;

    /**
     * 第 {@code index} 枚（0 起）伴随波在 {@code count} 枚里的<b>初始相位</b>（弧度）：
     * 一圈均匀切 {@code count} 份，基准相位就是 {@link #ORBIT_PHASE}。
     *
     * <p>口径来自回旋镖那套环绕（"one uniform slice per orbiter"）：1 枚时相位恒 = 基准相位
     * （= 星芒嬗震的单枚形状，逐字相同）；2 枚时相差 π（环的两侧各一枚，一眼看得出是两枚）。</p>
     */
    public static double orbitPhaseFor(int index, int count) {
        if (count <= 1) {
            return ORBIT_PHASE;
        }
        return ORBIT_PHASE + 2.0D * Math.PI * (double) index / (double) count;
    }

    /**
     * <b>炮口前推量</b>（格）：出生点 = 施法者眼睛 + 视线 × 本值。
     *
     * <p>照抄星芒嬗震那条"眼睛 + 准心 × 1 格"（免得刚出生就撞上自己脚下的方块 / 自己的身体）。</p>
     */
    public static final double MUZZLE_FORWARD_OFFSET = 1.0D;
}
