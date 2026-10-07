package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.StarShockConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * <b>星芒嬗震"把波发出去"的那一段</b>（2026-10-05 行为零变化拆分，从 {@code StarShockRuntime} 的
 * {@code fireMainWave} / {@code fireOrbitWave} 与它们专用的几何 / 魔素 / 命中附加常量
 * <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>给定一次发射的状态与蓄力进度，往世界里放哪一种波、放在哪、
 * 盖哪些要素</b>：</p>
 * <ol>
 *   <li><b>主波（含并排分叉）</b>：出生点 = 眼睛 + 准心 × 1 格 + 分叉偏移（第 1 枚沿准心、第 2/3 枚
 *       左右上下各偏一点），实体一律是<b>既有的</b> {@link ChargerWaveEntity}（与三台应力充能器
 *       同一个类型、同一个构造器、同一个渲染器），盖上波型 = 攻击态、主人 = 施法者、魔素 = 异、
 *       命中附加嬗乱、同批次号；</li>
 *   <li><b>环绕波</b>：与主波同形，只多挂一个"环绕波要素"（{@code setOrbitAnchor}），
 *       <b>继承父波批次号</b>（出生点与主波重合，不同批就是第一圈互相湮灭），魔素从
 *       {@link #ORBIT_ESSENCE_POOL} 里每枚各自随机抽；</li>
 *   <li>每枚主波各自按"当下的蓄力进度"滚一次环绕概率，掷骰 / 命中 / 生成三个计数写回
 *       {@link StarShockCast}（结算那一行据此区分"没滚到 / 滚到了但没生成 / 生成了"）。</li>
 * </ol>
 *
 * <p>搬运口径：方法体逐字相同，差异只有三类 —— {@code private} → 包级私有、{@code Cast} →
 * {@link StarShockCast}（同一个值类型，只是升为顶层包级私有类）、{@code fmt2(...)} 改写成
 * {@code StarShockRuntime.fmt2(...)}（日志格式化仍只有宿主那一处）。<b>数值、判断顺序、日志文案与
 * 批次 / 主人 / 要素的盖上时机一个字未动</b>；"是谁在什么时候调它"仍由
 * {@link StarShockRuntime#press} 与 {@link StarShockRuntime#hold} 决定（本类不碰能量、不碰状态机）。</p>
 */
final class StarShockWaveLauncher {

    private StarShockWaveLauncher() {
        throw new AssertionError("This class should not be instantiated");
    }

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
     * <b>环绕角速度（弧度/tick）</b> = 2π × 圈/秒 ÷ {@link ChargeConfigs#TICKS_PER_SECOND}；
     * 1 圈/秒 时 = <b>2π/20 ≈ 0.3142</b>。
     *
     * <p>单位是<b>弧度/tick</b>（不是度/tick）：位置公式里直接进 {@code Math.cos/sin}，
     * 少一次单位换算、少一个"度还是弧度"的歧义点。</p>
     */
    private static final double ORBIT_ANGULAR_SPEED =
        2.0D * Math.PI * ORBIT_TURNS_PER_SECOND / (double) ChargeConfigs.TICKS_PER_SECOND;

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
    static void fireMainWave(ServerPlayer player, StarShockCast cast, int heldTicks) {
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
            StarShockRuntime.fmt2(StarShockConfigs.chargeProgress(heldTicks, cast.config)),
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
    private static void fireOrbitWave(ServerLevel world, ChargerWaveEntity parent, StarShockCast cast,
                                      int heldTicks) {
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
            StarShockRuntime.fmt2(StarShockConfigs.chargeProgress(heldTicks, cast.config)),
            StarShockRuntime.fmt2(StarShockConfigs.orbitChance(heldTicks, cast.config)),
            orbitEssence.name(), parent.getFiringBatch(), parent.getId(), ORBIT_RADIUS,
            ORBIT_TURNS_PER_SECOND);
    }
}
