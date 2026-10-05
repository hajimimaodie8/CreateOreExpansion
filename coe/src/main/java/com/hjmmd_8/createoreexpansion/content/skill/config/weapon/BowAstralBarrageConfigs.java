package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * <b>星界弓专属主动技能「星元波置」的数值真源</b>（作者 2026-10-05 弓技能批 6，
 * <b>整条技能按作者的新定义返工</b>）—— 与 {@link BowCurseConfigs} / {@link BowDisarmConfigs} /
 * {@link BowWaveShiftConfigs} / {@link BowThunderMightConfigs} 同形，是本技能<b>唯一</b>写数字的
 * 地方：等级表（半径 / 滞留秒数 / 波级）、锚点前推量、弹幕条数与节拍、降下高度、初始落速、
 * 嬗乱与滞留的效果参数全部只在这里写一遍；发射处（{@code BowAstralBarrageLauncher}）与物品侧
 * （{@code JadeTopazBowItem}）<b>一个真源数字都不写</b>（关卡 {@code bow6-*} 钉着这条负向）。</p>
 *
 * <h2>作者原话（逐字，批 6 —— 本技能的唯一依据）</h2>
 * <blockquote>
 * 不是发射能量波哈，不是替换哈，就是锚定我方前面 4 格的一块圆形区域，半径等于技能等级加 1，
 * 空中落下无数带有嬗乱效果的药水箭与魔速能量波。魔素随机 8 抽 1。射中之后会造成滞留效果，
 * 时间 4 秒、5 秒、6 秒
 * </blockquote>
 *
 * <h2>逐条落地</h2>
 * <ul>
 *   <li><b>"不是发射能量波、不是替换"</b> ⇒ 批 5 那条（按键那一发换成"一枚带重力的攻击波 +
 *       环绕伴随波"）被<b>撤销</b>：{@link BowWaveShiftConfigs#appliesTo(BowTier)} 里星界那一档
 *       回到 {@code false}（{@code case ASTRAL -> false;}），本技能改由一个<b>自己的闸门</b>
 *       {@link #appliesTo(BowTier)} 接管。两条路都是穷尽 switch、各自独立。</li>
 *   <li><b>"锚定我方前面 4 格"</b> ⇒ 圆心 = 施放者<b>眼睛位置</b> + <b>视线方向</b> ×
 *       {@value #ANCHOR_FORWARD_BLOCKS} 格（形状照抄既有那条唯一口径
 *       {@code StarShockWaveLauncher#fireMainWave} / {@code BowWaveShiftConfigs#MUZZLE_FORWARD_OFFSET}
 *       的"眼睛 + 准心 × N 格"，本表只把 N 换成作者给的 4）。</li>
 *   <li><b>"半径等于技能等级加 1"</b> ⇒ {@link #radiusFor(int)}（1 级 2 格 / 2 级 3 格 / 3 级 4 格）。
 *       等级取 {@link BowTier#baseSkillLevel()}（<b>星界 = 3</b> ⇒ 实机半径 <b>4</b> 格），
 *       与「元矢自生」「量波置换」「雷鸣神力」<b>同一处真源</b>，<b>不是</b>附魔加成的有效等级。</li>
 *   <li><b>"空中落下无数"</b> ⇒ 条数 + 节拍两张数：{@value #ARROW_COUNT} 支药水箭、
 *       {@value #WAVE_COUNT} 枚能量波，每 {@value #SPAWN_INTERVAL_TICKS} tick 降一滴
 *       （"无数"没有确定数字 ⇒ 本表把它定成一个可配置的条数 + 持续节拍，见下"我独创"一节）。</li>
 *   <li><b>"带有嬗乱效果的药水箭"</b> ⇒ 落下的箭是<b>原版药水箭</b>（{@code minecraft:tipped_arrow}
 *       + {@code POTION_CONTENTS}），效果清单里第一项就是本模组既有的
 *       {@code createoreexpansion:transmutation_disorder}（施加方式沿既有那一支：
 *       {@code Arrow#doPostHurtEffects} 对 {@code customEffects} 逐条 {@code addEffect}）。
 *       时长/等级取本表的 {@link #DISORDER_TICKS} / {@link #DISORDER_AMPLIFIER}，
 *       而那两个常量与 {@code WaveEssenceEffects#ARCANE_DISORDER_TICKS}（"同一个效果、
 *       同一个 3 秒"的既有口径）由关卡 {@code bow6-disorder-source} 用一条跨文件等式钉死 ——
 *       为什么是"抄一个等值常量"而不是直接 import，见 {@link #DISORDER_TICKS} 的说明。</li>
 *   <li><b>"魔速能量波"、魔素随机 8 抽 1</b> ⇒ 每枚波各自从<b>同一条八魔素池</b>里抽
 *       （{@code BowMetaArrowTrait#randomEssence}，本表与发射处都<b>不</b>另抄一份名单）；
 *       波实体仍是既有的 {@code ChargerWaveEntity}（同一个类型、同一个渲染器）——
 *       作者 2026-10-02 的硬口径"不要再凭空造出一个新的能量波"照旧。</li>
 *   <li><b>"射中之后会造成滞留效果，时间 4 秒、5 秒、6 秒"</b> ⇒ {@link #immobilizeTicksFor(int)}
 *       （Lv1/2/3 = 4/5/6 秒；秒 × {@link ChargeConfigs#TICKS_PER_SECOND} = <b>80 / 100 / 120 tick</b>，
 *       "一秒 = 多少 tick"取全仓唯一换算因数，本表不写 20）。</li>
 * </ul>
 *
 * <h2>「滞留」为什么是"缓慢 + 跳跃削弱"而不是"定身"</h2>
 * <p>原版没有"定身"这个效果（也没有任何"每 tick 清零位移"的现成机制）。
 * 本批按<b>父会话对作者的落地说明</b>实现：效果本体 = <b>高等级缓慢</b>
 * （{@code minecraft:slowness}，amplifier {@value #STAGNATION_AMPLIFIER} ⇒ 移速乘数被压到 0，
 * 目标在地面上<b>走不动</b>）+ <b>跳跃削弱</b>（{@code minecraft:jump_boost}，
 * amplifier {@code -}{@value #STAGNATION_AMPLIFIER} ⇒ 跳跃强度归 0，目标<b>跳不起来</b>）。
 * 两者都走原版 {@code MobEffectInstance} ⇒ 一切既有"效果免疫/拦截"判据
 * （如星辉石凝能佩那一套 {@code MobEffectEvent.Applicable}）<b>自动生效</b>，本表不另写免疫。</p>
 * <p>⚠ <b>为什么不写成"每 tick 清零水平位移"</b>：那是"完全定身"，父会话明确要求
 * <b>先停下报告</b>、由它向作者确认——本批不做（见交付报告的"没做 / 拿不准"）。</p>
 *
 * <h2>★ 两条成分各自怎么被施加上（唯一的两个施加面，都在既有的地方）</h2>
 * <ul>
 *   <li><b>药水箭</b>：效果清单整个挂在箭的 {@code POTION_CONTENTS} 上（原版行为）——
 *       <b>嬗乱</b> + <b>滞留（缓慢 + 跳跃削弱）</b>，三条一起命中施加。</li>
 *   <li><b>能量波</b>：走既有的<b>命中附加效果要素</b>
 *       （{@code AbstractChargerWaveEntity#setHitEffect(效果, tick, amplifier)}，星芒嬗震用的同一个
 *       要素）—— 该要素<b>只有一个槽位</b>，本批把滞留的<b>主成分（缓慢）</b>放在那里；
 *       "跳跃削弱"半条由同一片区域里落下的药水箭给出（两者落的是同一块圆盘）。
 *       ⚠ 这是本批刻意的取舍：共享波实体<b>一个字节都没动</b>（形状只可能"原样"），
 *       而给要素加第二个槽位会改掉 {@code :cews} 编译依赖的跨模块面。</li>
 * </ul>
 *
 * <h2>⛔ 零注册、零语言键、零新 id、零新实体</h2>
 * <p>与批 4 / 批 5 逐字同形：本技能<b>不是</b>正式技能条目 —— 不加 {@code AllSkills} 条目、
 * 不进内核白名单、不写语言键、不跑 {@code runData}；它挂在本把弓自己那两条既有技能的键位上
 * （{@code CoeSkillRelease#anyHeldItemSkillKeyPressed}，服务端权威读数）。落下的两样东西
 * 都是<b>原版 / 既有</b>实体（{@code minecraft:arrow} 家族与 {@code createoreexpansion:charger_wave}）。</p>
 *
 * <h2>★ 我独创 / 作者没给、本表定死的那些数（调参只改本表）</h2>
 * <ul>
 *   <li>{@value #ARROW_COUNT} / {@value #WAVE_COUNT} / {@value #SPAWN_INTERVAL_TICKS} ——
 *       作者只说"无数"，没给数字；本批把它落成"条数 + 节拍"（条数照半径 4 的圆盘铺满一层，
 *       节拍 4 tick 一滴 ⇒ 整场弹幕可持续约 4 秒）。</li>
 *   <li>{@value #MAX_FALL_HEIGHT} / {@value #CEILING_SEARCH_MIN_HEIGHT} /
 *       {@value #CEILING_CLEARANCE} —— "空中"的高度与"头顶有方块时落在它下面"的留空。</li>
 *   <li>{@value #ARROW_INITIAL_FALL_SPEED} —— 药水箭的初始下落速度（有它才看得出"落下"而不是
 *       "悬停后自由落体"）。</li>
 *   <li>{@value #STAGNATION_AMPLIFIER} —— 滞留的强度（作者只给了时长没给强度；
 *       {@code 6} 让缓慢的移速乘数与跳跃强度都归 0，即"几乎无法移动"）。</li>
 *   <li>波级 —— 作者没给，本表按<b>与姊妹技能同一条规则</b>取"波级随技能等级"（Lv1/2/3 → α/β/γ，
 *       与 {@code BowWaveShiftConfigs#mainWaveLevelFor} 同一条口径，只是本技能自己的表）。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class BowAstralBarrageConfigs {

    private BowAstralBarrageConfigs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 三档（装备/武器技能 3 级封顶，与其余各条同口径；表长 3 就是"上限 3"本身）。 */
    public static final int MAX_LEVEL = 3;

    /**
     * 单级「星元波置」定义。
     *
     * @param radiusBlocks      锚定圆形区域的<b>半径</b>（格）：Lv1 = 2 / Lv2 = 3 / Lv3 = 4
     *                          （作者："半径等于技能等级加 1"）
     * @param immobilizeSeconds <b>滞留</b>时长（<b>秒</b>，作者给的单位）：Lv1 = 4 / Lv2 = 5 / Lv3 = 6
     * @param waveLevel         落下那一波所用的波级（决定伤害 / 颜色 / 波速，见 {@link WaveLevels}）
     */
    public record Level(int radiusBlocks, int immobilizeSeconds, int waveLevel) {
    }

    // ========== 三个等级（作者 2026-10-05 批 6 给死：半径 2/3/4 · 滞留 4/5/6 秒） ==========

    /** Lv1 —— 半径 2 格、滞留 4 秒、波级 α。 */
    public static final Level LEVEL_1 = new Level(2, 4, WaveLevels.LOW);

    /** Lv2 —— 半径 3 格、滞留 5 秒、波级 β。 */
    public static final Level LEVEL_2 = new Level(3, 5, WaveLevels.HIGH);

    /** Lv3 —— 半径 4 格、滞留 6 秒、波级 γ（<b>星界弓走的就是这一档</b>：它的档位起始等级 = 3）。 */
    public static final Level LEVEL_3 = new Level(4, 6, WaveLevels.GAMMA);

    /** 按等级取配置（与其余各条 {@code *Configs} 同名同形；越界先夹到 [1, 3]）。 */
    public static Level level(int level) {
        return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, LEVEL_1, LEVEL_2, LEVEL_3);
    }

    /**
     * <b>本技能对哪一档弓生效</b>（作者 2026-10-05 批 6："星界弓「星元波置」"）。
     *
     * <p>刻意写成<b>穷尽 switch（无 default）</b>：将来给枚举加一档弓，这里会<b>编译不过</b>，
     * 而不是静默地让新弓"什么也不发生"。今天恰好只有星界弓这一档为 {@code true}
     * ⇒ 翠玉 / 宝石 / 雷鸣三把弓的射击路径（含它们各自的技能）<b>一个字节都不变</b>。</p>
     *
     * <p>⚠ 与 {@link BowWaveShiftConfigs#appliesTo(BowTier)}（宝石弓发波）和
     * {@link BowThunderMightConfigs#appliesTo(BowTier)}（雷鸣弓电荷 + 真雷）是<b>三张独立的表</b>：
     * 今天三张表<b>两两不相交</b>（星界只在<b>本表</b>里为 true —— 批 5 那条"星界也走发波表"
     * 已被作者批 6 撤回）。三处都是穷尽 switch ⇒ 加一档新弓时三处都会编译不过。</p>
     */
    public static boolean appliesTo(BowTier tier) {
        if (tier == null) {
            return false;
        }
        return switch (tier) {
            case ASTRAL -> true;
            case JADE_TOPAZ, SAPPHIRE_RUBY, THUNDER -> false;
        };
    }

    /** 该技能等级的<b>锚定区域半径</b>（2 / 3 / 4 格，作者："半径等于技能等级加 1"）。 */
    public static double radiusFor(int level) {
        return level(level).radiusBlocks();
    }

    /**
     * 该技能等级的<b>滞留时长（tick）</b>= 表里的<b>秒数</b> × {@link ChargeConfigs#TICKS_PER_SECOND}
     * （Lv1/2/3 ⇒ 4/5/6 秒 ⇒ <b>80 / 100 / 120 tick</b>）。
     *
     * <p>"一秒 = 多少 tick"取全仓唯一换算因数（{@code ChargeConfigs.TICKS_PER_SECOND} = 20），
     * 本表<b>不写 20</b>：作者给的单位是秒，本方法只做单位换算，改换算因数时全仓一起动。</p>
     */
    public static int immobilizeTicksFor(int level) {
        return level(level).immobilizeSeconds() * ChargeConfigs.TICKS_PER_SECOND;
    }

    /** 该技能等级落下的<b>能量波波级</b>（Lv1/2/3 ⇒ α/β/γ；作者没给，见类注释那一节）。 */
    public static int waveLevelFor(int level) {
        return level(level).waveLevel();
    }

    // ==================================================================================
    // 弹幕的形状（"无数"的落地）—— 圆心 / 条数 / 节拍 / 高度 / 落速
    // ==================================================================================

    /**
     * <b>锚点前推量</b>（格）：圆心 = 施放者眼睛 + 视线 × 本值（作者："锚定我方前面 <b>4</b> 格"）。
     *
     * <p>形状照抄仓里那条唯一口径（{@code StarShockWaveLauncher#fireMainWave} 的
     * "眼睛 + 准心 × 1 格" / {@code BowWaveShiftConfigs#MUZZLE_FORWARD_OFFSET}），
     * 本表只把那个前推量换成作者给的 4。</p>
     */
    public static final double ANCHOR_FORWARD_BLOCKS = 4.0D;

    /**
     * 一次「星元波置」降下的<b>嬗乱药水箭</b>支数（作者只说"无数"⇒ 本批定成一个可配置的条数）。
     *
     * <p>与 {@link #SPAWN_INTERVAL_TICKS} 一起表达"无数"：整场弹幕的持续 tick 数 =
     * 条数 × 节拍（两支成分各自算），所以条数不是"一次性刷一堆"而是"一滴一滴落"。</p>
     */
    public static final int ARROW_COUNT = 20;

    /** 一次降下的<b>随机魔素能量波</b>枚数（与 {@link #ARROW_COUNT} 同一口径）。 */
    public static final int WAVE_COUNT = 10;

    /** 相邻两滴之间的间隔（tick）——"持续落下"的节拍；条数与它共同决定弹幕时长。 */
    public static final int SPAWN_INTERVAL_TICKS = 4;

    /**
     * 弹幕降下的<b>最高高度</b>（锚点上方，格）：正上方 {@value #MAX_FALL_HEIGHT} 格内<b>没有</b>
     * 实体方块时，就从这么高开始落（"空中"）。
     */
    public static final double MAX_FALL_HEIGHT = 16.0D;

    /**
     * <b>天花板搜索的起始高度</b>（锚点上方，格）：低于它的东西不算天花板。
     *
     * <p>它的作用是"贴墙 / 在矮处时别把落点压到脚面上"：从这一高度往上找第一个实体方块，
     * 找到就落在它下面（见 {@link #CEILING_CLEARANCE}）。</p>
     */
    public static final double CEILING_SEARCH_MIN_HEIGHT = 2.0D;

    /** 天花板之下的留空（格）——落点比天花板低这么多，免得一出生就卡在方块里。 */
    public static final double CEILING_CLEARANCE = 1.0D;

    /**
     * 一支药水箭的<b>初始下落速度</b>（格/tick，向下）。
     *
     * <p>为什么不是 0：速度为 0 的箭第一 tick 只被重力推 0.05 格，看起来像"悬停一下再掉"；
     * 给一个初速就是一眼看得出的"落下"。</p>
     */
    public static final double ARROW_INITIAL_FALL_SPEED = 0.5D;

    /** 落下那一波的<b>飞行方向</b>：正下方（"空中落下"）。 */
    public static final Vec3 FALL_DIRECTION = new Vec3(0.0D, -1.0D, 0.0D);

    // ==================================================================================
    // 命中之后的效果参数（嬗乱 / 滞留）
    // ==================================================================================

    /**
     * <b>滞留</b>的效果等级（amplifier，0 = I 级）：{@value #STAGNATION_AMPLIFIER}。
     *
     * <p>{@code 6} ⇒ 缓慢（{@code MOVEMENT_SLOWDOWN}，{@code -15% × (amplifier + 1)} 的移速乘数）
     * 把移速压到 0、跳跃（{@code JUMP}，{@code +0.1 × (amplifier + 1)} 的跳跃强度，
     * 本表用它的<b>相反数</b>）也压到 0 ⇒ "几乎无法移动"。</p>
     */
    public static final int STAGNATION_AMPLIFIER = 6;

    /** 给"跳跃削弱"用的 amplifier = {@code -}{@value #STAGNATION_AMPLIFIER}（同一个数的相反数，不写第二个数）。 */
    public static int jumpWeakenAmplifier() {
        return -STAGNATION_AMPLIFIER;
    }

    /**
     * <b>嬗乱</b>的持续 tick（{@value #DISORDER_TICKS}）—— 与"异"魔素那一支
     * （{@code WaveEssenceEffects#ARCANE_DISORDER_TICKS}）以及星芒嬗震技能侧
     * （{@code StarShockWaveLauncher#HIT_DISORDER_TICKS}）<b>同一个 3 秒</b>。
     *
     * <p>⚠ <b>为什么这里抄了一个 60 而不是 import 那一处</b>：仓里有一条既有关卡
     * （{@code wave-essence-hit-player-only}）钉着"除自己的声明之外，全仓<b>只有两个</b>文件
     * 能提到 {@code WaveEssenceEffects}"（波实体 + 弓命中处理器）—— 那条守卫的意图是
     * "<b>魔素层的施加</b>只能从那两处进去"，而本表只是借一个<b>时长常量</b>，
     * 不该为了它去放宽一条别人批次的守卫。于是代价是第二份 60：
     * 关卡 {@code bow6-disorder-source} 用一条<b>跨文件等式</b>把本常量与
     * {@code WaveEssenceEffects.ARCANE_DISORDER_TICKS} 钉死 —— 改其中一处、另一处立刻红。</p>
     */
    public static final int DISORDER_TICKS = 60;

    /** <b>嬗乱</b>的效果等级（amplifier，{@value #DISORDER_AMPLIFIER} = I 级；口径同上一行那条等式）。 */
    public static final int DISORDER_AMPLIFIER = 0;

    // ==================================================================================
    // 圆盘采样（几何规则，形状照 BowWaveShiftConfigs#orbitPhaseFor：几何口径住在数值真源里）
    // ==================================================================================

    /**
     * <b>圆盘上的一点</b>（水平偏移，Y 恒 0）—— 半径 {@code radiusBlocks} 的圆内<b>面积均匀</b>
     * 采样：半径取 {@code √U × r}（直接取 U × r 会让落点往圆心堆），角度取 {@code 2π × V}。
     *
     * <p>两次取值走<b>同一个</b> {@link RandomSource}（服务端权威、可复现），与仓里其它概率效果
     * 同一口径（不 {@code new Random()}）。</p>
     *
     * <p>刻意做成数值真源里的一个<b>具名规则</b>（而不是让发射处内联两行三角）：发射处因此
     * <b>不写半径、不写角度公式</b>，"圆形区域"这个几何口径只有这一处。</p>
     */
    public static Vec3 discOffset(RandomSource random, double radiusBlocks) {
        double radius = Math.sqrt(random.nextDouble()) * radiusBlocks;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }
}
