package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowWaveShiftConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * <b>「发一枚攻击波 + 若干枚环绕伴随波」那一段</b> —— 弓技能批 4（宝石弓「量波置换」，
 * 作者 2026-10-05）把这件事做成一个<b>可复用形状</b>；<b>批 12（2026-10-06）起三把弓共用</b>
 * （宝石 ① / 星界 ② / 雷鸣 ②，见 {@code BowWaveShiftConfigs#appliesTo}）。
 *
 * <p>⚠ <b>批 5 曾经让星界弓「星元波置」原样复用它（只把星界档在
 * {@code BowWaveShiftConfigs#appliesTo} 上改成 {@code true}）；作者同日批 6 撤回了那条口径</b>
 * （"不是发射能量波哈，不是替换哈"），星界弓改走它自己的区域弹幕
 * （{@code BowAstralBarrageLauncher} / {@code BowAstralBarrageConfigs}）。
 * ⚠ <b>批 12 又让星界与雷鸣回来了 —— 但走的是另一条槽</b>：作者的新技能表把「量波置换」
 * 放在它们的<b>槽 1</b>（原缴械风暴），而「星元波置」仍是星界的<b>槽 2</b>（它自己那张表）。
 * ⇒ 同一把弓上两条路并存、各占一槽，由内核按槽位派发、由弓侧的标记闸门分辨
 * （{@code BowExclusiveShotItemSkill#matches}）。本类唯一被批 6 / 批 12 动过的地方是
 * {@link #nextBatch()} 的可见性与批 12 那一行速度修正，发射路径与要素其余部分一个字未动。</p>
 *
 * <h2>形状出处：照抄星芒嬗震的 {@code StarShockWaveLauncher}（不新造第二套）</h2>
 * <p>本类回答的只有一件事：<b>给定世界、施法者与技能等级，往世界里放哪一种波、放在哪、盖哪些要素</b>。
 * 发射路径与既有机器/技能<b>逐条同形</b>（实体类型、构造器、{@code addFreshEntity} 三处与充能器
 * 完全同一条路径）：</p>
 * <table border="1">
 *   <caption>本类 vs {@code StarShockWaveLauncher}</caption>
 *   <tr><th>步骤</th><th>星芒嬗震</th><th>本类</th></tr>
 *   <tr><td>出生点</td><td>眼睛 + 准心 × 1 格</td><td>同一个口径（数值来自
 *       {@code BowWaveShiftConfigs#MUZZLE_FORWARD_OFFSET}）</td></tr>
 *   <tr><td>方向</td><td>玩家准心</td><td>玩家准心（退化视线回落到 +Z，同形兜底）</td></tr>
 *   <tr><td>造实体</td><td>{@code new ChargerWaveEntity(world, origin, look, waveLevel)}</td>
 *       <td><b>同一个构造器</b></td></tr>
 *   <tr><td>波型</td><td>{@code trySetWaveType(WaveTypes.ATTACK)}</td><td>同上</td></tr>
 *   <tr><td>主人</td><td>{@code setOwner(施法者)}</td><td>同上（"不伤发射者"走既有命中链，本类不写判据）</td></tr>
 *   <tr><td>魔素</td><td>主波固定"异"、伴随波从 7 种里各抽</td>
 *       <td><b>主波与每一枚伴随波各自从八种里随机抽</b>（作者："随机魔素"）</td></tr>
 *   <tr><td>重力</td><td>不设（既有波不受重力）</td>
 *       <td><b>同样不设</b> —— 批 10（2026-10-05，作者裁定）起主波<b>走直线</b>：
 *       批 4 曾给主波设过 {@code setGravity(1.35)}，作者批 10 撤回；
 *       波实体上那个要素仍在（默认 0 = 一个字节都不作用），只是<b>全仓没有任何调用点</b></td></tr>
 *   <tr><td>环绕</td><td>{@code setOrbitAnchor(父波 UUID, 半径, 角速度, 相位)}</td>
 *       <td>同上（<b>同一套机制</b>；多枚时相位在圆上均匀切片）</td></tr>
 *   <tr><td>批次</td><td>一次按键一个正号批次</td>
 *       <td>一次发射一个<b>负号段</b>批次（见 {@link #nextBatch()} 的说明）</td></tr>
 * </table>
 *
 * <h2>怎么复用它（现行口径：宝石 / 星界 / 雷鸣三档）</h2>
 * <ol>
 *   <li>在 {@code BowWaveShiftConfigs#appliesTo(BowTier)} 里把那一档弓改成 {@code true}
 *       （穷尽 switch，少一个 case 编译不过）—— 今天<b>三档是 true</b>（宝石 / 星界 / 雷鸣），
 *       翠玉那一档恒 false；</li>
 *   <li>在弓物品的发射闸门（{@code JadeTopazBowItem#shoot} 的 {@code fireWaveShiftInsteadOfArrow}）
 *       照旧生效 —— 它已经是"按档位问 {@code appliesTo}、按技能标记问闸门"的形状，<b>不用改</b>；</li>
 *   <li>等级传<b>该弓的技能等级</b> {@code this.effectiveSkillLevel(weapon)}（批 10 起；物品侧那一行
 *       由批 4 的 {@code this.tier.baseSkillLevel()} 改过来，<b>批 12 起它的基准是
 *       {@code BowTier#skillLevel()}</b> = 宝石 1 / 星界 2 / 雷鸣 2）—— 它含技艺提升 / 记忆回溯的
 *       附魔加成，上限 {@code BowTier#maxSkillLevel()} = 3 ⇒ 三把弓在附魔后都能到 Lv3。
 *       ⚠ 本类<b>不</b>为谁写死等级：写死数字就与"等级由物品侧那一个读数决定"这条唯一口径打架。
 *       等级随后只喂给 {@link BowWaveShiftConfigs#companionWaveCount(int)}（枚数）与
 *       {@link BowWaveShiftConfigs#rollWaveLevel(int, net.minecraft.util.RandomSource)}
 *       （波级分布）——本类<b>一个概率、一个波级数字都不写</b>。</li>
 * </ol>
 * <p>⚠ <b>雷鸣弓「雷鸣神力」不走本类</b>（作者批 5："不是发波，是另一套"）：它是电荷 + 原版闪电，
 * 数值住在 {@code BowThunderMightConfigs}、发射点住在 {@code BowThunderMightLauncher} ——
 * 两者与本类<b>零共用的数值</b>，只有键位 / 闸门 / 耐久那三件事同形。
 * ⚠ <b>批 12 之后同一把星界 / 雷鸣弓上两条路并存</b>：槽 1 走本类、槽 2 走各自的表，
 * 两者仍是"零共用数值、各自一个发射点"，只是不再互斥于"哪把弓"。</p>
 * <p>⚠ <b>星界弓「星元波置」仍不走本类</b>（批 6："不是发射能量波哈，不是替换哈"）：
 * 它改在锚定圆盘上<b>降下弹幕</b>（药水箭 + 随机魔素波），数值住在
 * {@code BowAstralBarrageConfigs}、发射点住在 {@code BowAstralBarrageLauncher}
 * —— 两处唯一与本类有关的是<b>共用号源</b> {@link #nextBatch()}（见它的说明）。</p>
 * <p>⚠ <b>本类不改 {@code WaveAccess}</b>：那两处技能发射点（星芒嬗震、回旋镖环绕）仍按旧口径
 * 直接调实体原生 API，本批的第三处与它们<b>同形</b>（迁移是显式延后的，见
 * {@code check-armor-sets.ps1} 的 {@code wave-api-deferred-consumers}）。</p>
 *
 * <h2>本类<b>不</b>做什么</h2>
 * <ul>
 *   <li><b>不</b>判"该不该发"（那是弓物品侧的档位闸门 + 无箭标记闸门 + 技能标记闸门）；</li>
 *   <li><b>不</b>扣能量、<b>不</b>碰冷却（耗能沿用无箭补给那条路已有的
 *       {@code JadeTopazBowItem.NO_ARROW_COST}，冷却无）；</li>
 *   <li><b>不</b>造实体类型 / 模型 / 贴图 / 渲染器（发的是既有 {@code charger_wave}）。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class BowWaveShiftLauncher {

    private BowWaveShiftLauncher() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * <b>弓这一侧的批次号源</b>（负号段，从 {@link Integer#MIN_VALUE} 起单调递增）。
     *
     * <p>为什么需要它、为什么是这个号段：{@code sameFiringBatch} 的豁免要求"两枚波批次号非 0 且相等"
     * （0 = 不属于任何批次 = 机器波，绝不能拿它当批次，否则全场机器波互相免碰撞）。
     * 而批次号又必须<b>全局唯一</b>—— 否则两次不同的发射撞上同一个号，那两枚波就会"穿过彼此而不湮灭"。
     * 今天仓里已有两个号源，各自占一段、互不相交（这是既有约定，本批照办）：</p>
     * <ul>
     *   <li>星芒嬗震：<b>正号段</b>（{@code BATCH_SEQUENCE++}，从 1 起）；</li>
     *   <li>回旋镖环绕：<b>负号段下沿</b>（{@code ORBIT_BATCH_SEQUENCE--}，从 −1 起递减）；</li>
     *   <li>本类：<b>负号段上沿</b>（从 {@code Integer.MIN_VALUE} 起递增）—— 与上面那个负号源
     *       只有各自发生约 2³¹ 次发射之后才可能相遇（实际不可达），与正号源按符号天然隔离，
     *       0 永不被分配。</li>
     * </ul>
     * <p>写成 {@code synchronized} 是照既有两处的形状（FML 的 mod 构造/事件派发可并行，
     * 而这个计数器是进程内唯一的顺序源）。</p>
     *
     * <p><b>批 6 起它也是星界弓「星元波置」弹幕的批次号源</b>（{@code BowAstralBarrageLauncher}
     * 与本类同包，直接复用这一个计数器）：两处共用一段只意味着号<b>依然唯一</b>——
     * 豁免判据本来就只要求"同一次发射内相等"，跨技能本来就不该相等。</p>
     */
    private static int BATCH_SEQUENCE = Integer.MIN_VALUE;

    /**
     * 分配一个批次号（<b>恒 &lt; 0</b> ⇒ 恒非 0 ⇒ 永远是一个有效批次）。
     *
     * <p>回绕兜底：递增到非负（= 越界进入星芒嬗震的正号段或撞上 0）时绕回
     * {@link Integer#MIN_VALUE}。只有约 2³¹ 次发射之后才可能发生 —— 那时旧号早已不在世界里，
     * 但要"绝不跨进别人的号段"这条性质在代码里成立，而不是靠"跑不到"。</p>
     *
     * <p>包级私有（不是 {@code private}）：同包的 {@code BowAstralBarrageLauncher} 要用<b>同一个</b>
     * 号源，另起一个计数器就得再造一段号段并论证它与已有三段不相交。</p>
     */
    static synchronized int nextBatch() {
        if (BATCH_SEQUENCE >= 0) {
            BATCH_SEQUENCE = Integer.MIN_VALUE;
        }
        return BATCH_SEQUENCE++;
    }

    /**
     * <b>发射一整套「量波置换」</b>：1 枚主波（随机魔素 + 按技能等级掷出的波级）
     * + 该等级决定的伴随环绕波（每枚<b>各自</b>掷自己的波级与魔素）。
     *
     * <p>调用前世界必须是服务端（{@code ServerLevel}）：波的手写位移只在服务端跑，
     * 客户端实例的位置由同步数据驱动。</p>
     *
     * @param world   服务端世界（同时是随机源：{@code world.random}）
     * @param shooter 发射者（同时是波的<b>主人</b>：既有命中链会把他排除在命中之外）
     * @param level   技能等级（1~3，越界由 {@code BowWaveShiftConfigs#level} 夹取；
     *                三把弓（宝石 / 星界 / 雷鸣）传的都是 {@code JadeTopazBowItem#effectiveSkillLevel}，
     *                含附魔提升 —— 它的基准是 {@code BowTier#skillLevel()} = 1 / 2 / 2）
     * @return 本次发射的批次号（&lt; 0），供日志 / 关卡核对
     */
    public static int fire(ServerLevel world, LivingEntity shooter, int level) {
        Vec3 look = shooter.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            // 退化视线（俯仰 ±90° 时原版也会给单位向量，这里只是形状保底）：与星芒嬗震同一处兜底
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        Vec3 origin = shooter.getEyePosition()
            .add(look.scale(BowWaveShiftConfigs.MUZZLE_FORWARD_OFFSET));

        int batch = nextBatch();
        // 主波波级：按技能等级在表里那张分布上<b>掷一次</b>（概率一个都不在本类写；随机源 = 世界）
        int mainLevel = BowWaveShiftConfigs.rollWaveLevel(level, world.random);
        int companionCount = BowWaveShiftConfigs.companionWaveCount(level);

        // 既有能量波实体（与三台应力充能器 / 差波器 / 星芒嬗震同一个类型、同一个构造器、同一个渲染器）
        ChargerWaveEntity main = new ChargerWaveEntity(world, origin, look, mainLevel);
        // 波型 = 攻击态。⚠ 必须在 trySetEssence 之前：魔素是"攻击波专有"，非攻击波上 setEssence
        // 会被静默忽略（返回 false、什么都不写）—— 顺序反了就是"整批波没有魔素"且没有任何报错。
        main.trySetWaveType(WaveTypes.ATTACK);
        // 主人 = 发射者本人（既有命中链据此把他排除：波从自己身上穿过去，不伤自己/不给自己挂效果）
        main.setOwner(shooter);
        WaveTrailStyle mainEssence = BowMetaArrowTrait.randomEssence(world.random);
        main.trySetEssence(mainEssence);
        // ★ 批 12（作者："量波置换的波速要设快一些"）：把主波的速度往上抬一个真源给的修正量。
        //   用的是波实体**既有的**速度修正要素（addSpeedOffset，速度调节器与星界弹幕用的同一个）
        //   ⇒ 波实体零改动；数值住在 BowWaveShiftConfigs.WAVE_SPEED_OFFSET_BLOCKS_PER_SECOND
        //   （依据：它让五档基础速度 2/4/6/7/8 分别落到 6/8/10/11/12，逐档都不触各自的上限夹取）。
        //   ⚠ 只加在主波上：伴随波的位置每 tick 被环绕要素改写成"主波位置 + 环上一点"，
        //   它自己的自走速度不可观测（给它加只会让速度字段与真实位移不一致）。
        main.addSpeedOffset(BowWaveShiftConfigs.WAVE_SPEED_OFFSET_BLOCKS_PER_SECOND);
        // ⛔ 批 10（作者裁定）：这里原来有一句
        //   main.setGravity(BowWaveShiftConfigs.GRAVITY_BLOCKS_PER_SECOND_SQUARED);
        // （批 4 的"空中有实体重力、水中走直线"）—— 作者批 10 撤回了重力那一半，主波自此
        // <b>一直走直线</b>（空中与水中同形）。波实体上的重力要素仍在（默认 0 ⇒
        // applyGravityElement 第一句原样返回同一个 step）；当前唯一调用点是
        // BowAstralBarrageLauncher（星界弹幕，要跟随药水箭的下落剖面），宝石弓这条不设重力。
        // 同一次发射的波共用一个批次号 ⇒ 互相豁免碰撞（否则主波与伴随波出生瞬间就互相湮灭）
        main.setFiringBatch(batch);
        world.addFreshEntity(main);
        // 波相关日志一律走 WaveDiag（全系统唯一出口，前缀/开关只在那里定义）。
        // 这一行让"技能几级、掷出几级波、伴随波几枚、批次多少"在日志里可查 ——
        // 作者只有日志可验收（在场内也能靠这行区分"生成了没有 / 为什么没有"）。
        WaveDiag.trace(
            "量波置换发射：技能 {} 级 → {} 级波（{}，本等级分布掷出；主波走直线），魔素={}，批次 {}，位置 {}，本次伴随波 {} 枚",
            level, mainLevel, WaveLevels.glyph(mainLevel), mainEssence.name(), batch, origin, companionCount);

        fireCompanions(world, main, shooter, level, batch);
        return batch;
    }

    /**
     * 发该等级的<b>伴随波</b>（Lv1 = 0 / Lv2 = 1 / Lv3 = 2）：与主波<b>完全同形</b>的既有波实体，
     * 只多挂一个"环绕波要素"（{@code setOrbitAnchor}）—— <b>它不是新实体类型</b>
     * （仍是 {@code createoreexpansion:charger_wave}，同一个渲染器）。
     *
     * <p>四件必须一起做的事（前三条照抄星芒嬗震那条口径）：</p>
     * <ol>
     *   <li><b>继承主波批次号</b>：出生点与主波重合（相距 0），而半径 0.8 在斜相位（两轴分量 0.566）
     *       也落进对方命中盒（按轴判据 0.6）⇒ 不同批就是"第一圈就互相湮灭"；</li>
     *   <li><b>设环绕要素</b>：主波 UUID + 半径 + 角速度（弧度/tick）+ 该枚的相位
     *       （多枚时在圆上均匀切片）—— 位置公式由那个要素每 tick 改写，本类不碰位置；</li>
     *   <li><b>魔素各自随机</b>：每枚伴随波单独抽一次（与主波同一个八种池子、不去重、不排除相同）；</li>
     *   <li><b>波级各自掷</b>（批 10）：每枚在<b>循环里</b>单独
     *       {@link BowWaveShiftConfigs#rollWaveLevel(int, net.minecraft.util.RandomSource)} 一次
     *       —— 与主波同一张分布、同一个 {@code world.random}，但<b>不是</b>主波那一掷的结果。
     *       作者原文就是"各自 roll"；⇒ 批 4 那条"取伤害不小于主波一半的最低波级"
     *       （{@code companionWaveLevelFor}）<b>就地作废</b>，方法已删除（它现在谁都不是谁的取值点）。</li>
     * </ol>
     *
     * <p>刻意<b>不</b>给伴随波设重力要素：它的位置每 tick 被环绕要素改写成"主波位置 + 环上一点"，
     * 重力对它没有任何可观测效果（批 4 起如此；批 10 之后主波自己也走直线，这条理由只剩"位置被
     * 环绕要素接管"那一半，结论不变）。环平面的法向仍取自主波的 {@code movement}（= 发射方向）。</p>
     */
    private static void fireCompanions(ServerLevel world, ChargerWaveEntity main, LivingEntity shooter,
                                       int level, int batch) {
        int count = BowWaveShiftConfigs.companionWaveCount(level);
        if (count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            // 波级：<b>每枚各自掷一次</b>（作者原文"各自 roll"；同一张分布表、同一个世界随机源）。
            // 批 4 那条"取伤害不小于主波一半的最低波级"（companionWaveLevelFor）就此作废并删除。
            int orbitLevel = BowWaveShiftConfigs.rollWaveLevel(level, world.random);
            ChargerWaveEntity orbit = new ChargerWaveEntity(world, main.position(), main.getMovement(), orbitLevel);
            orbit.trySetWaveType(WaveTypes.ATTACK);
            // 主人 = 发射者本人（与主波同一个；刻意不取主波 —— 两者是两个独立字段，不互相继承）
            orbit.setOwner(shooter);
            WaveTrailStyle essence = BowMetaArrowTrait.randomEssence(world.random);
            orbit.trySetEssence(essence);
            // 环绕波算同一批次：继承主波批次号（出生点与主波重合，斜相位两轴分量 0.566 < 0.6 也相交）
            orbit.setFiringBatch(batch);
            orbit.setOrbitAnchor(main.getUUID(), BowWaveShiftConfigs.ORBIT_RADIUS,
                BowWaveShiftConfigs.ORBIT_ANGULAR_SPEED, BowWaveShiftConfigs.orbitPhaseFor(i, count));
            world.addFreshEntity(orbit);
            WaveDiag.trace(
                "量波置换伴随波：第 {} / 共 {} 枚，{} 级波（{}，本等级分布各自掷出），魔素={}，批次 {}（继承主波），绕 {} 的 r={} 格、{} 圈/秒、相位 {}",
                i + 1, count, orbitLevel, WaveLevels.glyph(orbitLevel), essence.name(), batch,
                main.getId(), fmt2(BowWaveShiftConfigs.ORBIT_RADIUS),
                fmt2(BowWaveShiftConfigs.ORBIT_TURNS_PER_SECOND),
                fmt2(BowWaveShiftConfigs.orbitPhaseFor(i, count)));
        }
    }

    /**
     * 两位小数、<b>与区域设置无关</b>的格式化（日志要机器可比对：某些区域会把小数点写成逗号）。
     *
     * <p>与 {@code StarShockRuntime#fmt2} / {@code WaveOrbitElement#fmt2} 同一个形状 ——
     * 那两处都是包级私有（各自领地的日志格式化），本类在第三个包里，故照形自备一份。</p>
     */
    private static String fmt2(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
