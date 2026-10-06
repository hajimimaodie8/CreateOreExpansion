package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

import net.minecraft.util.RandomSource;

/**
 * <b>弓主动技能「量波置换」（宝石弓，作者 2026-10-05 弓技能批 4）的数值真源</b> ——
 * （⚠ 批 5 曾把星界弓「星元波置」挂到本表上；作者批 6 撤回了那条口径：星界弓改成
 * "在锚定区域降下弹幕"，数值改住 {@link BowAstralBarrageConfigs}，本表重新<b>只服务宝石弓</b>。）
 * 与 {@link BowCurseConfigs} / {@link BowDisarmConfigs} 同形，是这条技能<b>唯一</b>写数字的地方：
 * 等级表（伴随波枚数）、<b>波级分布表</b>（技能等级 → α/β/γ/ε/ω 的累积权重区间）、
 * 环绕几何（半径 / 角速度 / 相位）、炮口前推量
 * 全部只在这里写一遍，发射点（{@code BowWaveShiftLauncher}）与物品侧
 * （{@code JadeTopazBowItem}）<b>一个数字都不写</b>。
 *
 * <h2>作者原话（逐字，批 4）</h2>
 * <blockquote>
 * 发射出的弓箭替换为具有同样重力效果，但是在水中能够沿直线飞行的随机魔素攻击能量波，
 * 但是在空中有实体重力，效果与弓箭相似，可能比较不好做，你自己掂量掂量。<br>
 * 与此同时：2 级生成一枚小伴随波，效果也是沿着飞出能量波轨迹的垂直平面环绕，与星界套装的技能相似；
 * 3 级生成两枚。
 * </blockquote>
 * <p>⚠ <b>上面引文里"空中有实体重力"那一半已被批 10（2026-10-05，作者裁定）撤回：</b>
 * 主波<b>不再</b>设重力要素，改成<b>一直走直线</b>（水里与空中同形）。
 * 伴随波枚数那半（2 级一枚 / 3 级两枚）<b>不变</b>；批 10 同时把波级从"按等级查一个值"
 * 改成"按技能等级掷一次分布"（见下两节）。</p>
 *
 * <h2>⛔ 它不定义"波"，只定义"发几枚、多重、怎么绕"</h2>
 * <p>作者 2026-10-02 的硬口径（见 {@code StarShockRuntime} 类注释）是"能量波是由好几个要素定义的，
 * <b>不要再凭空造出一个新的能量波</b>"⇒ 本技能发的就是既有的 {@code ChargerWaveEntity}
 * （实体类型 {@code createoreexpansion:charger_wave}，与三台应力充能器/差波器<b>同一个类型、
 * 同一个渲染器</b>），靠既有要素把它发出去：</p>
 * <ul>
 *   <li><b>波级</b> —— {@link #rollWaveLevel(int, RandomSource)}（技能等级 → 一张累积权重分布表
 *       → 掷一次；定颜色/波速/伤害。<b>主波与每一枚伴随波各自独立掷</b>）；</li>
 *   <li><b>波型</b> —— 攻击态（{@code WaveTypes.ATTACK}，就是"变器攻击波变态"引燃出来的那一个）；</li>
 *   <li><b>魔素</b> —— 每枚<b>各自随机</b>抽一种，池子复用 {@code BowMetaArrowTrait#randomEssence}
 *       （= 那八种魔素的<b>同一条池子</b>，本类不另抄一份名单）；</li>
 *   <li><b>重力</b> —— ⛔ <b>批 10 起不用了</b>：波实体上的那个要素仍在
 *       （{@code AbstractChargerWaveEntity#setGravity}，默认 0 = 不作用），但本表与发射点
 *       <b>都不再</b>设它 ⇒ 主波走直线（见下面「重力要素：批 4 加、批 10 撤」那一节）；</li>
 *   <li><b>环绕</b> —— <b>既有的环绕波要素</b>（{@code setOrbitAnchor}：垂直平面 + 半径 + 角速度
 *       + 相位；与星芒嬗震/回旋镖那条完全同一套机制，不新造第二套）；</li>
 *   <li><b>批次</b> —— 同一次发射的主波与伴随波共用一个批次号（{@code setFiringBatch}），
 *       否则出生瞬间就互相湮灭（见 {@code AbstractChargerWaveEntity#sameFiringBatch}）。</li>
 * </ul>
 *
 * <h2>耗能与冷却：<b>本技能都不新增</b></h2>
 * <p>它挂在既有的"<b>无箭射击</b>"那条路上（{@code JadeTopazBowItem#prepareProjectiles} 的
 * "无箭但能量够 ⇒ 耗 {@code NO_ARROW_COST} 造一支无形魔法箭"分支）⇒ 耗能就是那条路本来就付的
 * {@code JadeTopazBowItem.NO_ARROW_COST}（<b>已有真源，本类不复制那个数</b>），
 * 冷却 <b>无</b>（作者没给；它是一条主动技能，只是没有正式技能条目，与「元矢自生」那条被动同形地"无冷却"：没有内核条目、没有独立键位）。</p>
 *
 * <h2>波级：为什么是"按技能等级掷一次分布"而不是"按等级查一个波级"</h2>
 * <p>批 4 的写法是"技能等级 → 一个波级"（1/2/3 ⇒ α/β/γ）。作者批 10（2026-10-05）把它改成
 * <b>按技能等级给一张分布、每次发射掷一次</b>，并且分布扩到 ε/ω（此前 4/5 级只由蓝宝石
 * 256 RPM 充能器产出）：</p>
 * <table border="1">
 *   <caption>等级 → 波级分布（作者给死；主波与每一枚伴随波<b>各自</b>掷一次）</caption>
 *   <tr><th>技能等级</th><th>α(1)</th><th>β(2)</th><th>γ(3)</th><th>ε(4)</th><th>ω(5)</th></tr>
 *   <tr><td>Lv1</td><td>100%</td><td>—</td><td>—</td><td>—</td><td>—</td></tr>
 *   <tr><td>Lv2</td><td>—</td><td>50%</td><td>50%</td><td>—</td><td>—</td></tr>
 *   <tr><td>Lv3</td><td>—</td><td>—</td><td>50%</td><td>25%</td><td>25%</td></tr>
 * </table>
 * <p>⚠ 平衡（作者已过目）：Lv3 掷出 ω 时是 <b>12 伤害</b>（批 4 那档 β 是 6 ⇒ <b>翻倍</b>），
 * 而 ε/ω 此前只由蓝宝石 256 RPM 充能器产出 —— 这就是"技能等级越高越能打出高波级"的既定意图。</p>
 * <p>落法：分布写成<b>整数累积权重区间</b>（{@link WaveLevelOdds}），掷一个
 * {@code [0, 100)} 的整数落进哪段就是哪级 ⇒ 无浮点、无舍入误差，概率一个都不在调用点写。
 * 等级越界（{@code <1} 或 {@code >3}）由 {@link #level(int)} 先夹到 [1, 3]。</p>
 *
 * <h2>重力要素：批 4 加、批 10 撤（要素本身留在波实体上）</h2>
 * <p>批 4 给主波设过重力（{@code setGravity(1.35)}，"与弓箭相似"）；<b>批 10 撤回了它</b>：
 * 发射点不再设 ⇒ 主波与伴随波一样<b>一直走直线</b>（水中与空中同形）。
 * 因此本表原来那个 {@code GRAVITY_BLOCKS_PER_SECOND_SQUARED = 1.35D} 常量<b>已删除</b>
 * （它当时的换算口径也一并作废）。</p>
 * <p>波实体侧那个要素<b>刻意保留</b>（{@code setGravity} / {@code hasGravity} /
 * {@code applyGravityElement} / NBT 的 {@code GravityAcceleration}）：它的默认值 0
 * ⇒ 要素未设时 {@code applyGravityElement} 第一句原样返回同一个 {@code step}，
 * 对机器波/变器波/差波器子波/星芒嬗震/回旋镖环绕波的飞行<b>逐字不变</b>；
 * 而且它是 {@code :cews} 也编译得到的公开形状，而"共享波基类的 public/protected 形状只许增长"
 * 是既有的判定（关卡 {@code bow4-public-shape} 逐条枚举它的成员，删掉那两个访问器会直接红）。
 * 今天<b>全仓没有任何 {@code .setGravity(} 调用点</b>；将来若要把抛物线加回来，
 * 改的是发射点一行，不必再动实体与存档形状。</p>
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
     * <b>一个等级的波级分布</b>：候选波级 + <b>累积权重</b>（整数百分比，最后一项恒 = 100）。
     *
     * <p>两个数组<b>等长且同序</b>：第 {@code i} 个候选波级的区间是
     * {@code [cumulative[i-1], cumulative[i])}（{@code cumulative[-1] = 0}）。
     * 掷法 = 在 {@code [0, 最后一项)} 上均匀取一个整数，落进哪个区间就是哪个波级 ——
     * <b>整数区间、无浮点、无舍入误差</b>，而"最后一项 = 100"就是权重总和本身
     * （关卡两头都核：区间严格递增，且最后一项 = 100）。</p>
     *
     * <p>⚠ 本记录<b>不持有随机源</b>：随机源由调用方每枚波各传一次（发射点传 {@code world.random}），
     * 于是本类不可能造出第二个号源，也不会碰 {@code nextBatch()} 那个批次号计数器。</p>
     *
     * @param waveLevels       候选波级（{@link WaveLevels#LOW α} ~ {@link WaveLevels#OMEGA ω}）
     * @param cumulativeWeight 累积权重（严格递增，最后一项 = 100）
     */
    public record WaveLevelOdds(int[] waveLevels, int[] cumulativeWeight) {

        /** 在 {@code [0, 100)} 上均匀取一个整数，落进哪个累积区间就返回哪个波级。 */
        public int roll(RandomSource random) {
            int pick = random.nextInt(cumulativeWeight[cumulativeWeight.length - 1]);
            for (int i = 0; i < cumulativeWeight.length; i++) {
                if (pick < cumulativeWeight[i]) {
                    return waveLevels[i];
                }
            }
            return waveLevels[waveLevels.length - 1];
        }
    }

    /**
     * 单级「量波置换」定义。
     *
     * @param companionWaves 额外生成的<b>伴随波</b>枚数（Lv1 = 0 / Lv2 = 1 / Lv3 = 2，作者给死）
     * @param waveLevelOdds  本等级的<b>波级分布</b>（主波与每一枚伴随波各自掷一次，见
     *                       {@link #rollWaveLevel(int, RandomSource)}）
     */
    public record Level(int companionWaves, WaveLevelOdds waveLevelOdds) {
    }

    // ========== 三个等级（作者 2026-10-05 批 10 给死；伴随波枚数沿用批 4，波级改成分分布） ======

    /** Lv1 —— 只发那枚波（0 枚伴随波），波级 <b>100% α</b>。 */
    public static final Level LEVEL_1 = new Level(0, new WaveLevelOdds(new int[] { 1 }, new int[] { 100 }));

    /** Lv2 —— 额外 1 枚伴随波（<b>宝石弓的基准档</b>），波级 <b>50% β / 50% γ</b>。 */
    public static final Level LEVEL_2 = new Level(1, new WaveLevelOdds(new int[] { 2, 3 }, new int[] { 50, 100 }));

    /** Lv3 —— 额外 2 枚伴随波（宝石弓附魔提升后能到这一档），波级 <b>50% γ / 25% ε / 25% ω</b>。 */
    public static final Level LEVEL_3 = new Level(2, new WaveLevelOdds(new int[] { 3, 4, 5 }, new int[] { 50, 75, 100 }));

    /**
     * 按等级取配置（与其余各条 {@code *Configs} 同名同形）。
     *
     * <p><b>越界一律夹到 [1, 3]</b>：{@code <1}（含 0 / 负数）⇒ 第 1 档，{@code >3} ⇒ 第 3 档。
     * 夹取走 {@link SkillLevelTables#pick3Clamped} 这个既有形状（表长 = 上限本身），
     * 本类不手写 {@code Math.max/min} —— 掷波级也走这里，所以"越界夹取"只有一处。</p>
     */
    public static Level level(int level) {
        return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, LEVEL_1, LEVEL_2, LEVEL_3);
    }

    /**
     * <b>本技能对哪一档弓生效</b>（作者 2026-10-05 批 4："宝石弓专属技能"）。
     *
     * <p>刻意写成<b>穷尽 switch（无 default）</b>：将来给枚举加一档弓，这里会<b>编译不过</b>，
     * 而不是静默地让新弓"什么也不发生"。今天<b>只有一档</b>为 {@code true}：</p>
     * <ul>
     *   <li><b>宝石弓</b>（批 4，2026-10-05「量波置换」）—— 与批 4 那一行<b>逐字相同</b>，
     *       本批一个字节都没动它。</li>
     * </ul>
     *
     * <p>⚠ <b>星界弓在批 5 曾经是第二档 {@code true}，作者 2026-10-05 批 6 把它撤了</b>
     * （作者原话："<b>不是发射能量波哈，不是替换哈</b>，就是锚定我方前面 4 格的一块圆形区域…"）
     * ⇒ 星界弓那一档回到 {@code false}：它按键那一发<b>不再走"发射一枚波 + 若干枚环绕伴随波"</b>
     * 这条路，改走它自己的 {@code BowAstralBarrageConfigs} / {@code BowAstralBarrageLauncher}
     * （区域弹幕）。这不是"放宽"：批 5 的关卡断言按<b>同一条口径</b>同步改成钉 {@code false}
     * （见 {@code check-armor-sets.ps1} 的 {@code bow5-astral-tier} 与 {@code bow6-* }）。</p>
     *
     * <p>另外两档（翠玉 / 雷鸣）在这里恒为 {@code false}：翠玉之弓那两条技能一个字不许动；
     * 雷鸣弓走的是<b>另一张表</b>（{@code BowThunderMightConfigs#appliesTo} —— 电荷 + 真劈雷，
     * 与本表零共用的数值，而且它<b>一枚波都不发</b>）。三张表都是穷尽 switch ⇒
     * 加一档新弓时<b>三处都会编译不过</b>，不会被任何一方静默吞掉。</p>
     */
    public static boolean appliesTo(BowTier tier) {
        if (tier == null) {
            return false;
        }
        return switch (tier) {
            case SAPPHIRE_RUBY -> true;
            case ASTRAL -> false;
            case JADE_TOPAZ, THUNDER -> false;
        };
    }

    /** 该技能等级的<b>伴随波枚数</b>（0 / 1 / 2，作者给死）。 */
    public static int companionWaveCount(int level) {
        return level(level).companionWaves();
    }

    /**
     * <b>按技能等级掷一次波级</b>（作者 2026-10-05 批 10）。
     *
     * <p>三件一起看：</p>
     * <ol>
     *   <li><b>分布住在表里</b>：本方法只问 {@link #level(int)} 取本等级的 {@link WaveLevelOdds}
     *       再掷一次 —— 概率一个都不在调用点写，越界夹取也在同一条路上；</li>
     *   <li><b>主波与每一枚伴随波各自掷</b>：<b>每枚波各调一次本方法</b>（主波一次、伴随波循环里
     *       每枚一次），与魔素池（{@code BowMetaArrowTrait#randomEssence}）完全同形 ——
     *       同一个方法、按波调用、每枚独立。⇒ 批 4 那条"伴随波取伤害不小于主波一半的最低波级"的
     *       规则（{@code companionWaveLevelFor}）已被这条口径<b>作废并删除</b>：它现在既不是
     *       伴随波的取值点，也不再是任何人的取值点（同一份分布才是伴随波的来源）；</li>
     *   <li><b>随机源是 {@code world.random}</b>：调用方（发射点）逐枚传进来的服务端世界随机源，
     *       本类不持有号源、也不碰批次号计数器（那是 {@code BowWaveShiftLauncher#nextBatch()} 的事）。</li>
     * </ol>
     *
     * @param level  技能等级（越界先夹到 [1, 3]，见 {@link #level(int)}）
     * @param random 服务端世界随机源（发射点传 {@code world.random}）
     * @return 本次掷出的波级（1~5；本表三行分别落在 {1} / {2,3} / {3,4,5}）
     */
    public static int rollWaveLevel(int level, RandomSource random) {
        return level(level).waveLevelOdds().roll(random);
    }

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
