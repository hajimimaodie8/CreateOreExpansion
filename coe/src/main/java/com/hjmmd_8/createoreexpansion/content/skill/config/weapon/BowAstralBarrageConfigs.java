package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
 *       的"眼睛 + 准心 × N 格"，本表只把 N 换成作者给的 4）。⚠ <b>批 9 起这个 4 是"推进起点"</b>
 *       而不是固定的圆心：见下面批 9 那一节与 {@link #previewCenter}。</li>
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
 * <h2>2026-10-05 弓技能批 9（作者原话，逐字）</h2>
 * <blockquote>
 * 1. 把箭的数量改少一点，能量波的数量翻一倍。<br>
 * 2. 下优化：当他拉弓时，应该会看到一个圆形技能范围的预选框。<br>
 * &nbsp;&nbsp;1. 预选框外观：边界是一圈类似于能量波的粉色线条。<br>
 * &nbsp;&nbsp;2. 移动机制：预选框会一点一点往远离视角的方向移动，边缘最多移动至距离玩家准心 5 格的位置。<br>
 * &nbsp;&nbsp;3. 释放机制：确定位置后松手，开始释放技能。释放的过程中，箭和能量波应该同步落下，两者落下速度应该一致，
 * 能量波的速度和箭的速度就是一样的，你这个意思就这么调整。波速是它里面的一个参数。技能释放完之后，该预选框才会消失。
 * </blockquote>
 * <p>逐条落地（<b>数值全部只在</b>本表）：</p>
 * <ul>
 *   <li><b>数量</b>：{@link #ARROW_COUNT} {@code 20 → 10}、{@link #WAVE_COUNT} {@code 10 → 20}；</li>
 *   <li><b>预选框</b>：圆心 = {@link #previewCenter}(眼睛, 视线, {@link #drawnTicks})，
 *       半径仍 = {@link #radiusFor(int)}（"技能等级 + 1"）。<b>颜色不在本表</b>——
 *       走弓自己那套既有描边色（{@code BowTier#skillOutlineColor()}：星界 =
 *       {@code SkillOutlineColors.STELLARSTONE_PINK}），渲染器里一个 RGB 字面量都没有；</li>
 *   <li><b>移动</b>：{@link #previewForwardBlocks(int)} —— 起点 {@link #ANCHOR_FORWARD_BLOCKS}(4)、
 *       每 tick {@value #PREVIEW_FORWARD_BLOCKS_PER_TICK} 格、上限
 *       {@link #PREVIEW_MAX_FORWARD_BLOCKS}(5)；</li>
 *   <li><b>释放</b>：松手按"那一刻"的圆心落（客户端与服务端<b>同一个</b>
 *       {@link #previewCenter}）；落下的波用既有的
 *       {@code AbstractChargerWaveEntity#addSpeedOffset(double)} 把速度设成
 *       {@link #fallSpeedBlocksPerSecond()}（= 箭的下落速度，见 {@link #waveSpeedOffsetFor(int)}）
 *       —— <b>箭一个字节没改</b>；</li>
 *   <li><b>存活</b>：{@link #barrageScheduleTicks()}（= 条数 × 节拍 = 80 tick）—— 客户端预选框据此
 *       在释放期间一直存在，数完才消失。</li>
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
 *   <li><b>批 9 新增的四个"作者没给"</b>：{@value #PREVIEW_MAX_FORWARD_BLOCKS}（作者只给了"最多 5 格"
 *       这个上限，没给"从几格开始推"⇒ 起点<b>沿用批 6 的 4</b>，松手越早越接近旧手感）、
 *       {@value #PREVIEW_FORWARD_BLOCKS_PER_TICK}（"一点一点"的速度）、
 *       {@link #barrageScheduleTicks()}（"技能放完"的判据 = 排程总长 80 tick）、
 *       {@link #fallSpeedBlocksPerSecond()}（作者说"波速 = 箭速"但没说箭速是多少 ⇒
 *       箭速仍取批 6 的 {@value #ARROW_INITIAL_FALL_SPEED} 格/tick，波被设成同一个速度）。</li>
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
     * <b>锚点前推量（= 拉弓起点的前推量）</b>（格）：圆心 = 施放者眼睛 + 视线 × 本值。
     *
     * <p>作者批 6 原话"锚定我方前面 <b>4</b> 格"；形状照抄仓里那条唯一口径
     * （{@code StarShockWaveLauncher#fireMainWave} 的 "眼睛 + 准心 × 1 格" /
     * {@code BowWaveShiftConfigs#MUZZLE_FORWARD_OFFSET}），本表只把那个前推量换成作者给的 4。</p>
     *
     * <p><b>⚠ 批 9 起它不再是一个固定圆心，而是"推进的起点"</b>（作者 2026-10-05：
     * "预选框会一点一点往远离视角的方向移动，边缘最多移动至距离玩家准心 5 格的位置"）：
     * 拉弓期间圆心沿视线方向从本值一点一点往外推，上限 {@link #PREVIEW_MAX_FORWARD_BLOCKS}，
     * 每 tick 推进 {@link #PREVIEW_FORWARD_BLOCKS_PER_TICK}。取"起点仍是 4"是刻意的：
     * 松手越早越接近批 6 的既有手感（0 tick 时圆心与批 6 逐字同点），
     * 作者要的那点移动量（4 → 5）也正好落在"一点一点"上。</p>
     */
    public static final double ANCHOR_FORWARD_BLOCKS = 4.0D;

    // ==================================================================================
    // 批 9：技能预选框（拉弓时的圆形范围预览）—— 推进 / 上限 / 存活
    // ==================================================================================

    /**
     * <b>拉弓期间预选框圆心能推到的最远前推量</b>（格，{@value #PREVIEW_MAX_FORWARD_BLOCKS}）——
     * 作者原话"边缘最多移动至距离玩家准心 <b>5</b> 格的位置"。
     *
     * <p>它同时是<b>实际落点</b>的上限：松手那一刻预选框在哪，弹幕的圆盘圆心就在哪
     * （客户端与服务端<b>同一个规则方法</b>算出来，见 {@link #previewCenter}）——
     * 所以"客户端看到的圈"和"服务端落的点"不可能分家。</p>
     */
    public static final double PREVIEW_MAX_FORWARD_BLOCKS = 5.0D;

    /**
     * <b>预选框圆心的推进速度</b>（格/tick，{@value #PREVIEW_FORWARD_BLOCKS_PER_TICK}）=
     * 一格 / 一秒：拉满弓（{@code JadeTopazBowItem#MAX_PULL_TIME} = 25 tick）之前就推到上限，
     * 肉眼看得见"一点一点往外挪"。
     *
     * <p>作者没给数（只说"一点一点"）⇒ 本表定死；调参只改这一处（关卡 {@code bow9-preview-cap}
     * 钉着"上限与速度都只住真源、两个调用点一个数字都不写"）。</p>
     */
    public static final double PREVIEW_FORWARD_BLOCKS_PER_TICK = 0.05D;

    /**
     * <b>拉弓已经多少 tick</b> —— "蓄力时长"的唯一算式，<b>两端共用这一条</b>。
     *
     * <p>形状与 {@code JadeTopazBowItem#releaseUsing} 里既有的
     * {@code getUseDuration(stack, entity) - timeLeft} 逐字同形（客户端把它写成
     * {@code stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()}，
     * 与 {@code JadeTopazBowModelRegistration} 里拉弓进度那一段同一个减式）。</p>
     *
     * <p>刻意收进数值真源：预选框的推进量是 {@code drawnTicks} 的函数，两端必须用<b>同一条</b>
     * 蓄力口径喂它，否则"客户端画的圈"与"服务端落的点"会各自漂移。</p>
     *
     * @param stack    正在被拉的那把弓
     * @param entity   拉弓者
     * @param timeLeft 剩余使用 tick（服务端传 {@code releaseUsing} 的形参 timeLeft，
     *                 客户端传 {@code getUseItemRemainingTicks()}）
     */
    public static int drawnTicks(ItemStack stack, LivingEntity entity, int timeLeft) {
        return stack.getUseDuration(entity) - timeLeft;
    }

    /**
     * 拉弓 {@code drawnTicks} tick 之后预选框圆心的<b>前推量</b>（格）：
     * {@link #ANCHOR_FORWARD_BLOCKS} + 推进速度 × tick，<b>夹在</b>
     * {@link #PREVIEW_MAX_FORWARD_BLOCKS} 以内。
     *
     * <p>"一点点往外推"与"最远 5 格"这两个作者口径<b>只有这一处实现</b>：客户端渲染器与服务端
     * 发射器都只调 {@link #previewCenter}，谁也不自己写这两个数（关卡 {@code bow9-preview-cap} /
     * {@code bow9-one-centre} 各钉一半）。</p>
     */
    public static double previewForwardBlocks(int drawnTicks) {
        double travelled = ANCHOR_FORWARD_BLOCKS + Math.max(0, drawnTicks) * PREVIEW_FORWARD_BLOCKS_PER_TICK;
        return Math.min(travelled, PREVIEW_MAX_FORWARD_BLOCKS);
    }

    /**
     * <b>预选框 / 弹幕圆盘圆心 —— 全仓唯一一处算法</b>：眼睛 + 视线 ×
     * {@link #previewForwardBlocks}(drawnTicks)。
     *
     * <p>退化视线（俯仰 ±90° 之类）回落到 +Z 的兜底也在这里——原先它写在
     * {@code BowAstralBarrageLauncher#fire} 里，批 9 收进真源，于是"圆心怎么算"这件事
     * <b>在调用点上彻底没有第二个版本可写</b>（关卡 {@code bow9-one-centre} 的负向断言：
     * 两个调用点都不许出现 {@code scale(} / {@code getEyePosition().add(}）。</p>
     *
     * @param eye        眼睛位置（{@code LivingEntity#getEyePosition()}）
     * @param look       视线单位向量（{@code LivingEntity#getLookAngle()}）
     * @param drawnTicks 拉弓已持续的 tick 数（见 {@link #drawnTicks}）
     */
    public static Vec3 previewCenter(Vec3 eye, Vec3 look, int drawnTicks) {
        Vec3 direction = look;
        if (direction == null || direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return eye.add(direction.normalize().scale(previewForwardBlocks(drawnTicks)));
    }

    /**
     * <b>一次弹幕的排程总 tick 数</b> = 两支成分里条数多的那一边 × {@link #SPAWN_INTERVAL_TICKS}
     * （γ 档实机 = {@code max(}{@value #ARROW_COUNT}{@code , }{@value #WAVE_COUNT}{@code ) × }
     * {@value #SPAWN_INTERVAL_TICKS}{@code } = 80 tick = 4 秒）。
     *
     * <p>它是客户端预选框的<b>存活时长</b>：作者要"技能释放完之后，该预选框才会消失"，
     * 而客户端没有"弹幕放完了"的包可收 ⇒ 只能按排程时长自己数。排程时长与发射处的
     * {@code i * SPAWN_INTERVAL_TICKS} 同源（同两个常量），所以"最后一滴落下"之前圈不会先没。</p>
     */
    public static int barrageScheduleTicks() {
        return Math.max(ARROW_COUNT, WAVE_COUNT) * SPAWN_INTERVAL_TICKS;
    }

    /**
     * <b>落下的能量波必须飞多快</b>（格/秒）= 药水箭的落速，也就是
     * {@link #ARROW_INITIAL_FALL_SPEED}（<b>格/tick</b>）÷ {@link ChargeConfigs#perTickFactor()}
     * （仓里唯一的"格/秒 ↔ 格/tick"换算因数，本表不写 20）。
     *
     * <p>作者 2026-10-05："释放的过程中，箭和能量波应该同步落下，<b>两者落下速度应该一致，
     * 能量波的速度和箭的速度就是一样的</b>……波速是它里面的一个参数。"</p>
     */
    public static double fallSpeedBlocksPerSecond() {
        return ARROW_INITIAL_FALL_SPEED / ChargeConfigs.perTickFactor();
    }

    /**
     * <b>让一枚落下的波与药水箭同速所需的"速度修正量"</b>（格/秒，叠加语义）=
     * {@link #fallSpeedBlocksPerSecond()} − {@link WaveLevels#baseSpeed(int)}（该波级的等级基础速度）。
     *
     * <p>为什么是"差值"而不是"直接设速度"：波实体只提供既有的
     * {@code AbstractChargerWaveEntity#addSpeedOffset(double)}（速度调节器用的同一个要素、
     * <b>本批对波实体零改动</b>），而它的语义是<b>在等级基础速度上叠加</b> ⇒ 想让最终速度等于
     * 箭速，就必须把基础速度减掉。γ 档实机：基础 6 + 修正 4 = <b>10 格/秒</b> = 0.5 格/tick，
     * 与箭的初速逐值相同（且 10 = {@code WaveLevels.maxSpeed(γ)}，不触夹取）。</p>
     *
     * <p>两边同源于 {@link #ARROW_INITIAL_FALL_SPEED} 一个常量 ⇒ <b>只改箭速</b>时波速自动跟着走，
     * 不存在"只改了一边"的写法（关卡 {@code bow9-wave-speed} 钉着这条）。</p>
     */
    public static double waveSpeedOffsetFor(int waveLevel) {
        return fallSpeedBlocksPerSecond() - WaveLevels.baseSpeed(waveLevel);
    }

    /**
     * 一次「星元波置」降下的<b>嬗乱药水箭</b>支数（作者只说"无数"⇒ 批 6 定成一个可配置的条数）。
     *
     * <p>与 {@link #SPAWN_INTERVAL_TICKS} 一起表达"无数"：整场弹幕的持续 tick 数 =
     * 条数 × 节拍（两支成分各自算），所以条数不是"一次性刷一堆"而是"一滴一滴落"。</p>
     *
     * <p><b>⚠ 批 9（作者 2026-10-05）：箭"改少一点" ⇒ 20 → </b>{@value #ARROW_COUNT}<b>，
     * 能量波"翻一倍" ⇒ 10 → </b>{@link #WAVE_COUNT}。作者原话："把箭的数量改少一点，能量波的数量
     * 翻一倍。"两个数<b>只住本表这一处</b>：发射处（{@code BowAstralBarrageLauncher}）与物品侧
     * 一个真源数字都不写（关卡 {@code bow9-counts} 钉着这条负向）。</p>
     */
    public static final int ARROW_COUNT = 10;

    /**
     * 一次降下的<b>随机魔素能量波</b>枚数（与 {@link #ARROW_COUNT} 同一口径）。
     *
     * <p>⚠ 批 9：{@code 10 → }{@value #WAVE_COUNT}（作者："能量波的数量翻一倍"）。</p>
     */
    public static final int WAVE_COUNT = 20;

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
