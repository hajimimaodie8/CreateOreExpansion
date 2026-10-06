package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.integration.skiller.skill.BowExclusiveShotItemSkill;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 *       等级取 {@link BowTier#thirdSkillLevel()}（<b>批 12 起</b>；作者新表把「星元波置」定为
 *       <b>①</b> ⇒ 实机半径 <b>2</b> 格、滞留 <b>4 秒</b> = 80 tick。批 6~11 取的是
 *       {@code BowTier#baseSkillLevel()} = 3 ⇒ 半径 4 / 120 tick；两者都按同一张等级表算，
 *       改的只是"星界那条技能读几级"，见 {@link #level(int)}），
 *       与「量波置换」读的 {@link BowTier#skillLevel()} <b>同一张档位表、不同的列</b>，
 *       两列都是注册期固定量，<b>不是</b>附魔加成的有效等级。</li>
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
 *       半径仍 = {@link #radiusFor(int)}（"技能等级 + 1"，批 12 起等级读
 *       {@link BowTier#thirdSkillLevel()} = 1 ⇒ 2 格）。<b>颜色不在本表</b>——
 *       走弓自己那套既有描边色（{@code BowTier#skillOutlineColor()}：星界 =
 *       {@code SkillOutlineColors.STELLARSTONE_PINK}），渲染器里一个 RGB 字面量都没有；
 *       ⚠ <b>批 12 把边界外观改成"闪烁的能量波样式粒子"</b>（作者原话见下面批 12 一节）——
 *       粒子类型与颜色口径取<b>波自己那一套</b>，做法与性能量级写在
 *       {@code BowAstralBarragePreviewRenderer} 里，本表<b>不</b>新增任何粒子参数；</li>
 *   <li><b>移动</b>：{@link #previewForwardBlocks(int)} —— 起点 {@link #ANCHOR_FORWARD_BLOCKS}(4)、
 *       每 tick {@value #PREVIEW_FORWARD_BLOCKS_PER_TICK} 格、上限
 *       {@link #PREVIEW_MAX_FORWARD_BLOCKS}（批 9 = 5 ⇒ <b>批 12 作者改 15</b>）；</li>
 *   <li><b>释放</b>：松手按"那一刻"的圆心落（客户端与服务端<b>同一个</b>
 *       {@link #previewCenter}）；落下的波用既有的
 *       {@code AbstractChargerWaveEntity#addSpeedOffset(double)} 把速度设成
 *       {@link #fallSpeedBlocksPerSecond()}（= 箭的下落速度，见 {@link #waveSpeedOffsetFor(int)}）
 *       —— <b>箭一个字节没改</b>；</li>
 *   <li><b>存活</b>：{@link #barrageScheduleTicks()}（= 条数 × 节拍 = 80 tick）—— 客户端预选框据此
 *       在释放期间一直存在，数完才消失。</li>
 * </ul>
 *
 * <h2>2026-10-06 弓技能批 12（作者原话，逐字；本表只承担其中两项）</h2>
 * <blockquote>
 * 1. 能跑的距离：上限 5 → <b>15</b>（推进速度不动，仍是每 tick 0.05 格）。<br>
 * 2. 边界外观：从"一圈线"改成<b>闪烁的能量波样式粒子</b>（复用能量波那套粒子）。
 * </blockquote>
 * <p>逐条落地：</p>
 * <ul>
 *   <li><b>①"上限 15"</b>：只改 {@link #PREVIEW_MAX_FORWARD_BLOCKS} 这一个常量（5.0D → 15.0D），
 *       {@link #previewForwardBlocks(int)} 那条"起点 + 速度 × tick 夹上限"的算式<b>一个字未改</b>
 *       （推进速度仍 {@value #PREVIEW_FORWARD_BLOCKS_PER_TICK} 格/tick）。⇒ 拉弓到 220 tick
 *       （11 秒）时圆心才推满 15 格；原版弓的使用时长是 72000 tick，玩家想推满就能推满。</li>
 *   <li><b>②"闪烁的能量波粒子"</b>：<b>不在本表</b> —— 它是纯表现，住在
 *       {@code BowAstralBarragePreviewRenderer}（那一侧刻意只放表现参数：段数 / 透明度系数 /
 *       粒子条数 / 闪烁相位，与"数值真源只放玩法数字"的既有分工一致）。本表只提供它要读的
 *       两件事：半径（{@link #radiusFor(int)}）与圆心（{@link #previewCenter}）。</li>
 * </ul>
 *
 * <h2>2026-10-06 弓技能批 11（作者原话，逐字）</h2>
 * <blockquote>
 * 第1条不必要完全严格，但是误差尽量都在1秒之内，而且射下乱箭或能量波的时候，关于世界的外轴，有一个小范围的、
 * 不超过10°左右的倾斜角，来塑造出相应的感觉。第二条需要限制，第三条最好贴地，但不贴地也是可以接受。
 * 你可以加入一个检测机制，通过玩家的纵坐标与附近地面的距离（高度差）来实现以下效果：<br>
 * 1. 圈的固定位置逻辑：(a) 当玩家飞在空中，攻击天空中的凋零等目标时，圈就没必要固定在地面上。
 * (b) 当玩家在起伏较小的平坡上，攻击僵尸或骷髅等目标时，圈最好固定在地上，而不是必须设定在玩家的平面。<br>
 * 2. 高度差显示优化：如果有高度差，圈的显示最好在衔接的地方加入一些预选框，否则高度突然落差可能会显得有些突兀。
 * </blockquote>
 * <p>逐条落地（<b>数值与几何全部只在本表</b>，发射处与渲染器一个数字都不写）：</p>
 * <ul>
 *   <li><b>①"误差尽量都在 1 秒之内"</b>：落下的波与药水箭<b>共用同一条下落剖面</b> ——
 *       同一个初始落速（{@link #ARROW_INITIAL_FALL_SPEED}）、同一个下落加速度
 *       （{@link #FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED} 由箭那一侧的
 *       {@link #ARROW_FALL_ACCELERATION_BLOCKS_PER_TICK_SQUARED} 乘两次一秒换算得出）。
 *       两侧到达 tick 的差由 {@link #arrivalDeltaSeconds(double)} 现算（纯数学、无 MC 类型），
 *       上限 {@link #ARRIVAL_SYNC_TOLERANCE_SECONDS}。⚠ <b>箭本身一个字节没改</b>
 *       （原版重力是全局行为）；改的是<b>弹幕这一批波</b>：它们第一次拿到波实体既有的重力要素
 *       （{@code setGravity}）—— 批 10 撤回的是<b>宝石弓那一发</b>的标记，与本技能无关；</li>
 *   <li><b>②"不超过 10° 左右的倾斜角"</b>：{@link #TILT_MAX_DEGREES}（上限只住这里）。
 *       ★ <b>作者 2026-10-06 补充裁定（逐字）</b>："对了，小角度散射的话，整个场可以稍微错乱。
 *       每个都是随机生成的，都是散射，发射角度也可以是不一样的。这样的话，才更有那种氛围"
 *       ⇒ <b>每一枚各自独立随机</b>：在<b>生成那一枚的那一刻</b>掷一次方位角
 *       {@link #tiltAzimuthRadians(RandomSource)} 与一个 {@code [0, 10°]} 的倾角
 *       {@link #tiltAngleRadians(RandomSource)}，落向 {@link #fallDirection(double, double)}
 *       —— 随机源是<b>服务端权威</b>的 {@code world.random}，方向当场写进实体、其后不再重算。
 *       速率大小不变：箭的速度向量走 {@link #arrowInitialVelocity(Vec3)}（速率恒 =
 *       {@link #ARROW_INITIAL_FALL_SPEED}），波仍走既有的速度要素 —— 改的只有<b>方向</b>；
 *       而斜插必然横向漂出，{@link #tiltSpawnShift(double, double, double)} 逐枚把出生点往回让，
 *       让落点仍落在预选框那块圆盘上（漂移模型与 ① 是同一条下落剖面）；</li>
 *   <li><b>③"第二条需要限制"</b>：预选框只在<b>本技能自己的槽位键</b>被按住时出现 ——
 *       槽位由 {@link #previewKeySlot()}（= {@code BowExclusiveShotItemSkill#ownSlot()}，
 *       与"同一发的接管判据"同一个常量）给出，两侧各自的既有键位通道读数
 *       （客户端 {@code CoeSkillClient#toolSlotKeyHeld} / 服务端 {@code CoeSkillProvider#slotPressed}）。
 *       ⛔ <b>不是</b>"按住任意技能键"（批 7/8 刚把那种歧义修掉）；</li>
 *   <li><b>④"最好贴地 / 加一个检测机制"</b>：{@link #nearbyGroundY(net.minecraft.world.level.Level, Entity)}
 *       打一条
 *       向下的方块射线（形状照 {@code BowAstralBarrageLauncher#rainOriginY} 那一条），
 *       高度差 = 玩家纵坐标 − 附近地面；{@link #GROUND_STICK_MAX_HEIGHT_DIFF} 以内 =
 *       "起伏较小的平坡" ⇒ 圈贴地；否则 = "飞在空中" ⇒ 圈留在准心那个平面。
 *       圆心的 Y 由 {@link #landingCentre(Vec3, double, boolean)} 一处决定；
 *       有高度差时 {@link #transitionRingYs(double, double)} 在圈下方按固定间隔给出过渡环；</li>
 *   <li><b>⑤"圈与落点同源"</b>：{@link #previewCenter} 仍是<b>唯一</b>的圆心算法（批 9），
 *       {@link #landingCentre} 只是它的 Y 收口 —— <b>客户端画圈与服务端弹幕都走这两个方法</b>，
 *       谁也不自己算第二份。</li>
 * </ul>
 *
 * <h2>② 为什么是"每一枚各自随机散射"（作者 2026-10-06 补充裁定，本批的口径）</h2>
 * <p>作者原话（逐字，覆盖了此前"整场统一一个倾斜"那个读法）：</p>
 * <blockquote>
 * 对了，小角度散射的话，整个场可以稍微错乱。每个都是随机生成的，都是散射，发射角度也可以是不一样的。
 * 这样的话，才更有那种氛围。
 * </blockquote>
 * <p>落地口径（三条一起看）：</p>
 * <ol>
 *   <li><b>逐枚各自掷</b>：每一支药水箭、每一枚波在<b>自己出生的那一刻</b>掷一对方位角 + 倾角
 *       （都在 {@code [0, }<b>上限</b>{@code ]} 内），互不影响 ⇒ 整场看起来"稍微错乱"；</li>
 *   <li><b>随机源 = {@code world.random}</b>：服务端权威、可复现，不新造号源；
 *       ⛔ <b>方向在出生那一刻就定死并写进实体</b>（箭 = {@code setDeltaMovement}，
 *       波 = 构造参数 {@code movement}），此后<b>没有任何地方重算它</b> —— 每 tick 重算就是方向抖动；</li>
 *   <li><b>圈 = 落点仍守得住</b>：漂移补偿也是<b>逐枚</b>的（每枚按自己的方位角/倾角把出生点
 *       往上游挪）⇒ 散射之后落点仍落在预选框那块圆盘里，只是整片落点比"整场统一"更蓬松一点。</li>
 * </ol>
 * <p>⚠ <b>与 ① 的"≤1 秒"不冲突</b>：判据是<b>整体</b>（不是逐枚对齐）—— 两支成分的竖直剖面
 * 同源（竖直初速同为 {@code 箭速 × cos θ}、每 tick 增量同为 0.05 格/tick²），只差箭那一侧的
 * 0.99 阻力 ⇒ 无论每枚掷到哪个角度，到达差都在同一个量级（{@link #arrivalDeltaBoundSeconds(double)}
 * 取的就是最坏的那一档）。散射改的是落点与路径长度，不改这条剖面。</p>
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
 * （服务端读 {@code CoeSkillProvider#slotPressed} / {@code CoeSkillProvider#pressedSlot}，
 * 客户端读 {@code CoeSkillClient#toolSlotKeyHeld}；那个旧的
 * {@code CoeSkillRelease#anyHeldItemSkillKeyPressed} 批 8 已删）。落下的两样东西
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
 *       与批 4 的 {@code BowWaveShiftConfigs} 同一条口径，只是本技能自己的表）。
 *       ⚠ 姊妹技能那张表在<b>批 10</b> 改成了"按技能等级掷一次分布"（并扩到 ε/ω），
 *       本表<b>不受影响</b>：星界弹幕仍按等级取一个固定波级（作者批 10 只点了「量波置换」）。</li>
 *   <li><b>批 9 新增的四个"作者没给"</b>：{@link #PREVIEW_MAX_FORWARD_BLOCKS}（作者批 9 给了"最多 5 格"
 *       这个上限、<b>批 12 改成 15</b>，但他没给"从几格开始推"⇒ 起点<b>沿用批 6 的 4</b>，
 *       松手越早越接近旧手感）、
 *       {@value #PREVIEW_FORWARD_BLOCKS_PER_TICK}（"一点一点"的速度）、
 *       {@link #barrageScheduleTicks()}（"技能放完"的判据 = 排程总长 80 tick）、
 *       {@link #fallSpeedBlocksPerSecond()}（作者说"波速 = 箭速"但没说箭速是多少 ⇒
 *       箭速仍取批 6 的 {@value #ARROW_INITIAL_FALL_SPEED} 格/tick，波被设成同一个速度）。</li>
 *   <li><b>批 11 新增的四个"作者没给"</b>：{@value #GROUND_PROBE_DEPTH}（"附近地面"探多深 ——
 *       作者只说"附近"，没给深度）、{@value #GROUND_STICK_MAX_HEIGHT_DIFF}（"起伏较小的平坡"
 *       与"飞在空中"的分界高度差 —— 2 格 = 跳跃/台阶以内算贴地）、
 *       {@value #TRANSITION_RING_COUNT} / {@value #TRANSITION_RING_SPACING}（作者只说
 *       "加入一些预选框"、"一两圈"，没给条数与间隔）、{@value #GROUND_PROBE_LIFT}（射线起点
 *       抬半个格子，纯技术兜底，不是玩法数值）。</li>
 *   <li><b>批 11 的倾角上限不是"我定的"</b>：{@value #TILT_MAX_DEGREES}° 就是作者原话里的
 *       "不超过 10° 左右"；本表只负责把它<b>只写一处</b>。实际角度<b>每枚各自</b>在
 *       {@code [0, 上限]} 上掷（作者 2026-10-06 补充裁定："每个都是随机生成的，都是散射"——
 *       所以是散射，不是"整场一个方向"）。</li>
 *   <li><b>批 11 的散射"补偿"是本表定的</b>：作者只说"散射、角度可以不一样"，没说落点要不要
 *       跟着挪。本表按"圈 = 落点"那条既有红线，逐枚把出生点往上游让掉斜插的横向漂移
 *       （{@link #tiltSpawnShift(double, double, double)}）⇒ 散射只让落点更蓬松，
 *       不会让整片落点漂出预选框。⛔ 若作者要的是"落点也随机漂出去"，那是另一条口径，
 *       改的是发射处那一行（把让掉的量去掉即可），不是本表的公式。</li>
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
     * <p>⚠ <b>这个名字会遮蔽 {@code net.minecraft.world.level.Level}</b>（同一个简单名）：
     * 本类里凡是要提 MC 的 {@code Level}（批 11 的地面射线），一律写全限定名。</p>
     *
     * @param radiusBlocks      锚定圆形区域的<b>半径</b>（格）：Lv1 = 2 / Lv2 = 3 / Lv3 = 4
     *                          （作者："半径等于技能等级加 1"）
     * @param immobilizeSeconds <b>滞留</b>时长（<b>秒</b>，作者给的单位）：Lv1 = 4 / Lv2 = 5 / Lv3 = 6
     * @param waveLevel         落下那一波所用的波级（决定伤害 / 颜色 / 波速，见 {@link WaveLevels}）
     */
    public record Level(int radiusBlocks, int immobilizeSeconds, int waveLevel) {
    }

    /**
     * <b>一级消耗</b>（作者 2026-10-06 批 13 给死）：「基础的能量都是一次释放技能，消耗
     * <b>150 乘以技能等级</b>点」⇒ 本条技能每释放一次扣 {@code 150 × 有效等级} 点工具能量
     * （Lv1 = 150 / Lv2 = 300 / Lv3 = 450）。
     *
     * <p>这里写的是<b>一级消耗</b>（一个数），"乘等级"由既有算术
     * {@code CoeSkillSupport.cost(..) ← SkillEnergyCost.compute(..)} 完成；唯一调用点 =
     * {@code BowExclusiveShotItemSkill#consumeResource}（按名读本常量，发射点与物品侧一个数字都不写）。</p>
     */
    public static final int ENERGY_COST_PER_LEVEL = 150;

    // ========== 三个等级（作者 2026-10-05 批 6 给死：半径 2/3/4 · 滞留 4/5/6 秒） ==========
    // ⚠ 批 13 加的那一列（冷却 3/4/5 秒）**不进下面三行**：半径 / 滞留 / 波级三个字段被关卡
    // bow6-radius 逐字钉住 ⇒ 为加一列而改那处钉法，会把"作者既有三行的数值"从"逐字不变"
    // 降级成"改过一遍"。冷却单独走下面那个按等级的读数（同一张表、同一个夹取）。

    /** Lv1 —— 半径 2 格、滞留 4 秒、波级 α。 */
    public static final Level LEVEL_1 = new Level(2, 4, WaveLevels.LOW);

    /** Lv2 —— 半径 3 格、滞留 5 秒、波级 β。 */
    public static final Level LEVEL_2 = new Level(3, 5, WaveLevels.HIGH);

    /** Lv3 —— 半径 4 格、滞留 6 秒、波级 γ（批 12 起星界弓读 <b>Lv1</b>：作者新表把「星元波置」定为 ①）。 */
    public static final Level LEVEL_3 = new Level(4, 6, WaveLevels.GAMMA);

    /** 按等级取配置（与其余各条 {@code *Configs} 同名同形；越界先夹到 [1, 3]）。 */
    public static Level level(int level) {
        return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, LEVEL_1, LEVEL_2, LEVEL_3);
    }

    /**
     * <b>本技能对哪一档弓生效</b>（作者 2026-10-05 批 6："星界弓「星元波置」"）。
     *
     * <p>刻意写成<b>穷尽 switch（无 default）</b>：将来给枚举加一档弓，这里会<b>编译不过</b>，
     * 而不是静默地让新弓"什么也不发生"。今天只有星界弓这一档为 {@code true}
     * ⇒ 翠玉 / 宝石 / 雷鸣三把弓的这条闸门恒不通过。</p>
     *
     * <p>⚠ 与 {@link BowWaveShiftConfigs#appliesTo(BowTier)}（量波置换：宝石 / 星界 / 雷鸣）
     * 和 {@link BowThunderMightConfigs#appliesTo(BowTier)}（雷鸣神力：只有雷鸣）是<b>三张独立的表</b>：
     * 批 12 之后<b>星界在两处都为 true</b> —— 但那是<b>两条不同的技能槽</b>（槽 1 = 量波置换、
     * 槽 2 = 本表这个星元波置），弓侧三道闸门的第二道判据是"这一发的标记是不是本技能写的"
     * （{@code BowExclusiveShotItemSkill#matches}）⇒ 同一次射击最多只被其中一条接管，两张表
     * 不会互相顶替。三处都是穷尽 switch ⇒ 加一档新弓时三处都会编译不过。</p>
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

    /**
     * 该技能等级的<b>冷却秒数</b>（3 / 4 / 5；作者 2026-10-06 批 13 给死）。
     *
     * <p>三条弓专属技能（量波置换 / 星元波置 / 雷鸣神力）<b>同一张表</b>；越界先夹到 [1, 3]
     * —— 走 {@link SkillLevelTables#pick3Clamped(int, int, Object, Object, Object)} 这个既有形状
     * （表长 = 上限本身，与 {@link #level(int)} 同一个夹取），本类<b>不</b>手写
     * {@code Math.max/min}、也不另立第二张等级表。</p>
     *
     * <p>宿主 = 按技能记的 {@code PerSkillCooldown}（星界弓的
     * {@code BowTier#perSkillCooldown()} = true），判定位置 = {@code BowExclusiveShotItemSkill} 的
     * {@code consumeResource} 与 {@code release} <b>两处同判</b>（既有红线：只判一处会出现
     * "冷却中照扣能量"或"扣了却不执行"）。</p>
     */
    public static int cooldownSecondsFor(int level) {
        return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, 3, 4, 5);
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
     * <p>作者 2026-10-05："预选框会一点一点往远离视角的方向移动，边缘最多移动至距离玩家准心 5 格
     * 的位置"；<b>批 12 把上限改成 15</b>（速度不动）⇒ 松手越晚圆心越远。取"起点仍是 4"是刻意的：
     * 松手越早越接近批 6 的既有手感（0 tick 时圆心与批 6 逐字同点）。</p>
     */
    public static final double ANCHOR_FORWARD_BLOCKS = 4.0D;

    // ==================================================================================
    // 批 9：技能预选框（拉弓时的圆形范围预览）—— 推进 / 上限 / 存活
    // ==================================================================================

    /**
     * <b>拉弓期间预选框圆心能推到的最远前推量</b>（格，{@value #PREVIEW_MAX_FORWARD_BLOCKS}）——
     * 作者原话（批 9）"边缘最多移动至距离玩家准心 <b>5</b> 格的位置"，
     * <b>批 12（2026-10-06）作者改口为 15</b>（原话："能跑的距离，翻 4 倍"；父会话追问后明确
     * = <b>5 变 15</b>，且"推进速度不要动"）。
     *
     * <p>它同时是<b>实际落点</b>的上限：松手那一刻预选框在哪，弹幕的圆盘圆心就在哪
     * （客户端与服务端<b>同一个规则方法</b>算出来，见 {@link #previewCenter}）——
     * 所以"客户端看到的圈"和"服务端落的点"不可能分家。</p>
     *
     * <p>⚠ <b>推到 15 格要多久</b>：起点 4 格 + 速度 {@value #PREVIEW_FORWARD_BLOCKS_PER_TICK}
     * 格/tick ⇒ 需要 {@code (15 - 4) / 0.05 = 220} tick = <b>11 秒</b>的持续拉弓。
     * 这不是缺陷而是两个口径的合成结果：作者既要"最远 15"又要"速度别动"，
     * 而原版弓的使用时长是 72000 tick（玩家想拉多久就拉多久）⇒ 想推满就推满。</p>
     */
    public static final double PREVIEW_MAX_FORWARD_BLOCKS = 15.0D;

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
     * <p>"一点点往外推"与"最远 {@value #PREVIEW_MAX_FORWARD_BLOCKS} 格"这两个作者口径
     * <b>只有这一处实现</b>：客户端渲染器与服务端
     * 发射器都只调 {@link #previewCenter}，谁也不自己写这两个数（关卡 {@code bow9-preview-cap} /
     * {@code bow12-preview-cap} / {@code bow9-one-centre} 各钉一块）。</p>
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

    // ==================================================================================
    // 批 11①：下落剖面（箭与波共用同一条）—— "误差尽量都在 1 秒之内"
    // ==================================================================================

    /**
     * <b>原版箭每 tick 的下落加速度</b>（格/tick²，{@value #ARROW_FALL_ACCELERATION_BLOCKS_PER_TICK_SQUARED}）。
     *
     * <p>它就是原版 {@code AbstractArrow#tick} 里那一句 {@code deltaMovement.add(0.0, -0.05F, 0.0)}
     * ——本表<b>只读它</b>（箭一个字节都不改：那是全局行为），用途是把弹幕这一批<b>波</b>的
     * 下落加速度换算成同一个物理量（见 {@link #FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED}）。</p>
     */
    public static final double ARROW_FALL_ACCELERATION_BLOCKS_PER_TICK_SQUARED = 0.05D;

    /**
     * <b>原版箭每 tick 的速度保留率</b>（{@value #ARROW_FALL_DRAG_PER_TICK} = 原版
     * {@code AbstractArrow#tick} 的 {@code 0.99F}）。
     *
     * <p>它只服务 {@link #arrivalTicksForArrow(double)} 这条<b>静态测算</b>：
     * 箭有空气阻力、波没有（波实体只累加下落速度），所以两支成分的下落剖面严格说不是同一条
     * 抛物线 —— 差多少、够不够 1 秒，本表用这条剖面现算（见 {@link #arrivalDeltaSeconds(double)}）。</p>
     */
    public static final double ARROW_FALL_DRAG_PER_TICK = 0.99D;

    /**
     * <b>落下的波要用的下落加速度</b>（格/秒²）= {@link #ARROW_FALL_ACCELERATION_BLOCKS_PER_TICK_SQUARED}
     * × 一秒的 tick 数 × 一秒的 tick 数（{@value #FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED}）。
     *
     * <p>"一秒 = 多少 tick"取全仓唯一换算因数 {@link ChargeConfigs#TICKS_PER_SECOND}，
     * 本表不写 20。为什么是乘两次：波实体的重力要素是<b>两步</b>换算
     * （{@code gravityFallSpeed += accel × perTickFactor()} 再 {@code step += gravityFallSpeed × perTickFactor()}），
     * 所以"每 tick 的位移增量"= {@code accel / 400}；要让它与箭的 0.05 格/tick² 逐值相同，
     * 加速度就必须是箭那一侧的 <b>400 倍</b> —— 这条等式是 ① 的全部内容，
     * 两支成分因此<b>不可能各自漂移</b>（改箭那一侧的常量，波这边自动跟着走）。</p>
     */
    public static final double FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED =
        ARROW_FALL_ACCELERATION_BLOCKS_PER_TICK_SQUARED
            * ChargeConfigs.TICKS_PER_SECOND * ChargeConfigs.TICKS_PER_SECOND;

    /**
     * <b>作者给的误差上限</b>（秒，{@value #ARRIVAL_SYNC_TOLERANCE_SECONDS}）：
     * 作者原话"误差尽量都在 1 秒之内"。
     *
     * <p>它只服务 {@link #arrivalDeltaSeconds(double)} 的自检 —— 谁把下落剖面改坏
     * （例如把波那边的加速度接回某一个固定值），这个式子立刻超过它。</p>
     */
    public static final double ARRIVAL_SYNC_TOLERANCE_SECONDS = 1.0D;

    /** 下落剖面的模拟上限（tick）：够任何一次弹幕（最高 16 格 + 天花板场景）数完。 */
    private static final int FALL_PROFILE_MAX_TICKS = 400;

    /**
     * <b>斜着落下时，竖直方向上的初始速度</b>（格/tick）= {@link #ARROW_INITIAL_FALL_SPEED} ×
     * {@code cos(倾角)}。
     *
     * <p>为什么要它：批 11 的落向是斜的，而**两支成分的速率大小是同一个**
     * （箭 = 落向 × 箭速，波 = 沿落向的"波速 = 箭速"）⇒ 它们的<b>竖直</b>分量同为
     * {@code 箭速 × cos θ}。两支成分的竖直剖面因此逐值同源，散射（每枚各自一个 θ）也不会
     * 把两者拉开 —— 逐枚的竖直剖面只差一个共同因子。</p>
     */
    public static double verticalFallSpeed(double tiltRadians) {
        return ARROW_INITIAL_FALL_SPEED * Math.cos(tiltRadians);
    }

    /**
     * <b>一支箭从 {@code fallBlocks} 格高处落到地面要多少 tick</b>（静态测算，纯数学）。
     *
     * <p>剖面逐字照原版 {@code AbstractArrow}：初速是<b>竖直分量</b>
     * {@link #verticalFallSpeed(double)}，每 tick 先按当前速度前进，再把速度
     * {@code (v + 0.05) × 0.99}（重力在前、阻力在后 —— 与 ① 的换算口径一致）。</p>
     *
     * @param tiltRadians 该枚自己的倾角（散射时每枚各不相同 ⇒ 逐枚算）
     */
    public static int arrivalTicksForArrow(double fallBlocks, double tiltRadians) {
        double moved = 0.0D;
        double speed = verticalFallSpeed(tiltRadians);
        for (int tick = 1; tick <= FALL_PROFILE_MAX_TICKS; tick++) {
            moved += speed;
            if (moved >= fallBlocks) {
                return tick;
            }
            speed = (speed + ARROW_FALL_ACCELERATION_BLOCKS_PER_TICK_SQUARED) * ARROW_FALL_DRAG_PER_TICK;
        }
        return FALL_PROFILE_MAX_TICKS;
    }

    /**
     * <b>一枚波从 {@code fallBlocks} 格高处落到地面要多少 tick</b>（静态测算，纯数学）。
     *
     * <p>剖面逐字照波实体的重力要素：竖直初速是与箭<b>同一个</b>
     * {@link #verticalFallSpeed(double)}（批 9 的"波速 = 箭速" + 批 11 的斜落向），此后每 tick
     * 的位移增量恒为
     * {@code FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED × perTickFactor() × perTickFactor()}
     * （= 0.05 格/tick²，与箭那一侧逐值相同），<b>无阻力</b>（波实体没有拖拽）。</p>
     *
     * @param tiltRadians 该枚自己的倾角（散射时每枚各不相同 ⇒ 逐枚算）
     */
    public static int arrivalTicksForWave(double fallBlocks, double tiltRadians) {
        double moved = 0.0D;
        double speed = verticalFallSpeed(tiltRadians);
        double step = FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED
            * ChargeConfigs.perTickFactor() * ChargeConfigs.perTickFactor();
        for (int tick = 1; tick <= FALL_PROFILE_MAX_TICKS; tick++) {
            moved += speed;
            if (moved >= fallBlocks) {
                return tick;
            }
            speed += step;
        }
        return FALL_PROFILE_MAX_TICKS;
    }

    /**
     * <b>两支成分落到同一高度的时间差</b>（秒）—— ① 的自检式，作者口径是"尽量都在 1 秒之内"。
     *
     * <p>⚠ <b>判据是"整体"而不是"逐枚对齐"</b>（作者 2026-10-06 补充裁定：散射是每枚各自随机
     * 方向的）：两支成分的竖直剖面<b>只差箭那一侧的 0.99 阻力</b>，而它们的竖直初速同源
     * （{@link #verticalFallSpeed}）、每 tick 增量同源（{@link #FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED}）
     * ⇒ <b>无论每枚掷到 0~上限之间哪个角度，这个差都是同一个量级</b>；本方法按该枚自己的倾角
     * 现算，整场要报的那个数由 {@link #arrivalDeltaBoundSeconds(double)} 给（倾角区间两端取大）。</p>
     */
    public static double arrivalDeltaSeconds(double fallBlocks, double tiltRadians) {
        int arrowTicks = arrivalTicksForArrow(fallBlocks, tiltRadians);
        int waveTicks = arrivalTicksForWave(fallBlocks, tiltRadians);
        return Math.abs(arrowTicks - waveTicks) / (double) ChargeConfigs.TICKS_PER_SECOND;
    }

    /**
     * <b>本次弹幕里任何一枚的到达差上界</b>（秒）：在倾角区间 {@code [0, }上限{@code ]} 的<b>两端</b>
     * 各算一次、取较大者。
     *
     * <p>为什么取两端就够：散射让每枚的倾角不同，而两支成分的竖直初速是<b>同一个</b>
     * {@code 箭速 × cos θ}、每 tick 增量也是同一个 0.05 格/tick²，两支剖面之间只差箭那一侧的
     * 0.99 阻力 ⇒ 差随倾角的变化很小（16 格落差处三个角度逐值相同 = 1 tick；
     * 64 格处 0.25 秒 / 0.20 秒），两端取大就是这个区间上的保守值。</p>
     *
     * <p>它同时是发射处日志里报的那一个数（作者只有日志可验收）—— 报上界而不是报"某一枚"，
     * 因为每枚的角度不同，报单枚没有意义。</p>
     */
    public static double arrivalDeltaBoundSeconds(double fallBlocks) {
        double upright = arrivalDeltaSeconds(fallBlocks, 0.0D);
        double tilted = arrivalDeltaSeconds(fallBlocks, Math.toRadians(TILT_MAX_DEGREES));
        return Math.max(upright, tilted);
    }

    // ==================================================================================
    // 批 11②：世界的竖直轴那一个小倾斜角（"斜插"的手感；上限只住这里）
    // ==================================================================================

    /**
     * <b>下落方向相对世界竖直轴的最大倾角</b>（度，{@value #TILT_MAX_DEGREES}）——
     * 作者原话"有一个小范围的、<b>不超过 10° 左右</b>的倾斜角"。
     *
     * <p>⚠ 它是<b>上限</b>，实际角度<b>每一枚</b>在 {@code [0, 本值]} 上各自掷一次
     * （见 {@link #tiltAngleRadians(RandomSource)}；作者 2026-10-06 补充裁定："每个都是随机
     * 生成的，都是散射，发射角度也可以是不一样的"）。小角度散射让整场看起来"稍微错乱、
     * 都在斜插"，而不是笔直砸下来 —— 这就是作者要的"相应的感觉"。</p>
     */
    public static final double TILT_MAX_DEGREES = 10.0D;

    /**
     * <b>某一枚的落向方位角</b>（弧度，{@code [0, 2π)}）——"这一枚往哪一边斜"。
     *
     * <p>随机源由调用方逐枚传入（发射点传服务端权威的 {@code world.random}），本表不持有号源。</p>
     */
    public static double tiltAzimuthRadians(RandomSource random) {
        return random.nextDouble() * Math.PI * 2.0D;
    }

    /**
     * <b>倾角上限的度数读数</b>（{@value #TILT_MAX_DEGREES}°）—— 只服务日志/显示。
     *
     * <p>为什么要有它：发射处要把"每枚各自掷一个 0~上限的落向"打进日志（作者只有日志可验收），
     * 而那条日志<b>不该</b>直接去提几何常量（调用点按名读表才是本仓的口径；关卡钉着
     * "调用点不许出现 TILT_MAX_DEGREES"）。几何本身仍只走 {@link #tiltAngleRadians(RandomSource)}，
     * 本方法<b>不做任何换算、也不参与取角</b>。</p>
     */
    public static double tiltMaxDegrees() {
        return TILT_MAX_DEGREES;
    }

    /**
     * <b>某一枚的落向倾角</b>（弧度，{@code [0, }{@link #TILT_MAX_DEGREES}{@code °]}）——
     * "斜多少"。上限只在 {@link #TILT_MAX_DEGREES} 那一处写。
     *
     * <p>⚠ <b>每一枚各自掷一次</b>（作者 2026-10-06 补充裁定："每个都是随机生成的，都是散射，
     * 发射角度也可以是不一样的，这样的话才更有那种氛围"）⇒ 调用点是"生成某一枚的那一刻"，
     * 不是"一次施放掷一次"。</p>
     */
    public static double tiltAngleRadians(RandomSource random) {
        return Math.toRadians(random.nextDouble() * TILT_MAX_DEGREES);
    }

    /**
     * <b>斜插的落向单位向量</b>：世界竖直轴（正下方）绕 {@code azimuthRadians} 那一侧偏
     * {@code tiltRadians}。
     *
     * <p>倾角为 0（或负数）时返回批 6 那条既有的正下方常量 {@link #FALL_DIRECTION}
     * ——退化情形与批 6 逐字同向，不需要第二个"正下方"写法。</p>
     *
     * <p>⚠ 这是<b>一枚</b>的落向（散射：每枚各自一对方位角 + 倾角）。它只被求值一次 ——
     * 在那一枚<b>出生的那一刻</b>，随即写进实体（箭 = {@code setDeltaMovement}，
     * 波 = 构造参数 {@code movement}）⇒ 之后没有任何地方重算它，方向因此不会逐 tick 抖。</p>
     */
    public static Vec3 fallDirection(double azimuthRadians, double tiltRadians) {
        if (tiltRadians <= 0.0D) {
            return FALL_DIRECTION;
        }
        double lateral = Math.sin(tiltRadians);
        return new Vec3(lateral * Math.cos(azimuthRadians), -Math.cos(tiltRadians),
            lateral * Math.sin(azimuthRadians));
    }

    /**
     * <b>落下的箭的初始速度向量</b> = 落向 × {@link #ARROW_INITIAL_FALL_SPEED}。
     *
     * <p>速率大小<b>恒等于</b>批 6/9 那条常量（作者批 11："不许改变速度大小"）——
     * 倾斜只改方向。放在本表是因为"箭速"这个数只许写一处，而发射处现在需要的是<b>向量</b>
     * 而不是"向下 0.5"。</p>
     */
    public static Vec3 arrowInitialVelocity(Vec3 fallDirection) {
        return fallDirection.scale(ARROW_INITIAL_FALL_SPEED);
    }

    /**
     * <b>斜插落下的水平漂移</b>（格）：从 {@code fallBlocks} 格高处按 {@code tiltRadians} 斜着落下，
     * 落点会比出生点沿方位角漂出多远。
     *
     * <p>漂移用<b>波那条剖面</b>算（{@link #arrivalTicksForWave(double, double)}，该枚自己的倾角）：
     * 两支成分在同一高度上的水平漂移几乎逐值相同（箭的横向分量按 0.99 衰减、但箭落得早；
     * 波不衰减、但落得晚，16 格实机上一个 1.44 格、一个 1.47 格），用其中一条就够，
     * 不必再养第二条漂移模型。</p>
     */
    public static Vec3 tiltDrift(double azimuthRadians, double tiltRadians, double fallBlocks) {
        double lateralSpeed = ARROW_INITIAL_FALL_SPEED * Math.sin(tiltRadians);
        double drift = lateralSpeed * arrivalTicksForWave(fallBlocks, tiltRadians);
        return new Vec3(Math.cos(azimuthRadians) * drift, 0.0D, Math.sin(azimuthRadians) * drift);
    }

    /**
     * <b>出生点要往回让多少</b>（格）= {@link #tiltDrift} 的反向量。
     *
     * <p>为什么要让：斜插会让落点整体沿倾斜方向漂出去（10° 时十几格落差就是 1.5 格上下），
     * 而<b>预选框画的是落点那块圆盘</b> ⇒ 出生点必须先往上游挪同样的距离，
     * 落点才仍落在圈里。不让就等于把"圈 = 落点"偷偷改成"圈 = 出生点"。</p>
     *
     * <p>⚠ 散射之后它是<b>逐枚</b>的（每枚自己的方位角/倾角）—— 调用点就是那一枚的出生代码。</p>
     */
    public static Vec3 tiltSpawnShift(double azimuthRadians, double tiltRadians, double fallBlocks) {
        return tiltDrift(azimuthRadians, tiltRadians, fallBlocks).scale(-1.0D);
    }

    // ==================================================================================
    // 批 11④：贴地判据（"玩家纵坐标 − 附近地面"）+ 圆心的 Y 收口
    // ==================================================================================

    /** 向下的地面射线起点相对玩家脚底的抬升（格）：免得脚正好踩在方块顶面时射线从面内出发。 */
    public static final double GROUND_PROBE_LIFT = 0.5D;

    /**
     * <b>地面射线的最大深度</b>（格，{@value #GROUND_PROBE_DEPTH}）——
     * "附近地面"的定义：脚下方这个距离以内找不到地面，就算<b>飞在空中</b>。
     */
    public static final double GROUND_PROBE_DEPTH = 8.0D;

    /**
     * <b>算作"在平坡上"的最大高度差</b>（格，{@value #GROUND_STICK_MAX_HEIGHT_DIFF}）。
     *
     * <p>作者 2026-10-06 的判据是"玩家的纵坐标与附近地面的距离（高度差）"：
     * 差 ≤ 本值 ⇒ (b) "玩家在起伏较小的平坡上" ⇒ <b>圈贴地</b>；
     * 差 &gt; 本值（或者探不到地面）⇒ (a) "玩家飞在空中" ⇒ <b>圈留在准心那个平面</b>。</p>
     *
     * <p>为什么是 2 格：原版一次跳跃约 1.25 格、一格台阶 1 格 ⇒ 站在地上、站在台阶上、
     * 刚起跳都算"贴着地面"；而飞起来（任意高度）与站在两格以上的柱子上都算"在空中"。</p>
     */
    public static final double GROUND_STICK_MAX_HEIGHT_DIFF = 2.0D;

    /**
     * <b>一条向下的方块射线</b>（形状照 {@code BowAstralBarrageLauncher#rainOriginY} 那一条：
     * {@code Block.COLLIDER} 实体方块、{@code Fluid.NONE} 不算地面）。
     *
     * <p><b>探不到地面时返回 {@code from.y - depth}</b>（= "地面在这么深的地方"）：
     * 调用方不必处理 null，而高度差判据天然把它判成"在空中"（差 = depth &gt; 阈值）。
     * 射线从 {@link #GROUND_PROBE_LIFT} 之上出发，脚正踩在方块顶面时也能打到那一格。</p>
     *
     * <p>⚠ 这是<b>唯一</b>的地面查询形状：客户端渲染器与服务端发射器各自的 {@code Level}
     * 都调它（同一个方法、同一个判据），"贴不贴地"不可能两边各判一套。</p>
     */
    public static double groundSurfaceY(net.minecraft.world.level.Level level, Vec3 from, double depth, Entity probe) {
        Vec3 start = from.add(0.0D, GROUND_PROBE_LIFT, 0.0D);
        Vec3 end = start.add(0.0D, -depth, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, probe));
        return hit.getType() == HitResult.Type.MISS ? end.y : hit.getLocation().y;
    }

    /**
     * <b>玩家脚下方"附近地面"的高度</b>（格）—— 作者要的那个"检测机制"的唯一实现。
     *
     * <p>从玩家纵坐标往下探 {@link #GROUND_PROBE_DEPTH} 格；探不到就返回
     * {@code 玩家纵坐标 − 深度}（判据据此判"在空中"）。</p>
     */
    public static double nearbyGroundY(net.minecraft.world.level.Level level, Entity player) {
        return groundSurfaceY(level, player.position(), GROUND_PROBE_DEPTH, player);
    }

    /**
     * <b>玩家是不是"站在起伏较小的平坡上"</b>（作者 1(b)）—— 高度差 ≤
     * {@link #GROUND_STICK_MAX_HEIGHT_DIFF}。
     *
     * <p>差值 = <b>玩家纵坐标</b> − 附近地面（作者原话就是这么定义的）；
     * 为负（站在方块里）也算贴地。</p>
     */
    public static boolean sticksToGround(double playerFeetY, double nearbyGroundY) {
        return playerFeetY - nearbyGroundY <= GROUND_STICK_MAX_HEIGHT_DIFF;
    }

    /**
     * <b>圈 / 弹幕圆盘圆心的 Y 收口（全仓唯一一处）</b>：贴地时把批 9 那条圆心算法给出的
     * 准心平面点<b>换成附近地面</b>，不贴地时原样返回。
     *
     * <p>XZ <b>不动</b>（水平位置仍由 {@link #previewCenter} 唯一决定），所以"圈在哪"这件事
     * 仍然只有一个算法；本方法只接管作者 1(a)/(b) 那一条<b>Y 的口径</b>。</p>
     *
     * <p>⚠ 客户端画圈与服务端弹幕<b>都走这一个方法</b>：否则"圈贴在地上、箭落在半空"
     * 或反过来，都会是那种圈≠落点的老毛病。</p>
     *
     * @param planeCentre     {@link #previewCenter} 给出的准心平面圆心
     * @param nearbyGroundY   {@link #nearbyGroundY} 给出的附近地面高度
     * @param stickToGround   {@link #sticksToGround} 的判定结果
     */
    public static Vec3 landingCentre(Vec3 planeCentre, double nearbyGroundY, boolean stickToGround) {
        if (!stickToGround) {
            return planeCentre;
        }
        return new Vec3(planeCentre.x, nearbyGroundY, planeCentre.z);
    }

    // ==================================================================================
    // 批 11④-2：高度差处的过渡环（"圈的显示最好在衔接的地方加入一些预选框"）
    // ==================================================================================

    /** 过渡环的条数（作者没给数 ⇒ 本表定死"一两圈"里的两圈）。 */
    public static final int TRANSITION_RING_COUNT = 2;

    /** 过渡环之间的固定间隔（格，沿正下方）。 */
    public static final double TRANSITION_RING_SPACING = 1.5D;

    /**
     * <b>主圈下方那几道过渡环的 Y</b>（世界坐标）—— 有高度差时把落差"接"起来。
     *
     * <p>画法：从圆心正下方按 {@link #TRANSITION_RING_SPACING} 等距排
     * {@link #TRANSITION_RING_COUNT} 道环（与主圈同半径、同色，只是每一道更淡），
     * <b>落进地面以下就停</b>（不会把环画进地里）。</p>
     *
     * <p>只在<b>不贴地</b>时才有内容：贴地时圈本来就在地面上，没有落差不需衔接
     * （作者原话是"如果有高度差"）。</p>
     */
    public static double[] transitionRingYs(double centreY, double nearbyGroundY) {
        double[] ys = new double[TRANSITION_RING_COUNT];
        int count = 0;
        for (int i = 1; i <= TRANSITION_RING_COUNT; i++) {
            double y = centreY - i * TRANSITION_RING_SPACING;
            if (y <= nearbyGroundY) {
                break;
            }
            ys[count++] = y;
        }
        double[] trimmed = new double[count];
        System.arraycopy(ys, 0, trimmed, 0, count);
        return trimmed;
    }

    // ==================================================================================
    // 批 11③：预选框的"专属键"（作者："第二条需要限制"）
    // ==================================================================================

    /**
     * <b>预选框听的那一个槽位键</b> —— 就是本技能自己的槽位
     * （{@code BowExclusiveShotItemSkill#ownSlot()}，键三 ⇒ 2）。
     *
     * <p>它不是第二个常量：与"同一发的接管判据"读<b>同一个</b> {@code SLOT}，
     * 所以"星界弓这条技能在哪个键上"永远只有一处答案。</p>
     *
     * <p><b>两侧都取得到的判据</b>：本方法只回答"是哪个槽位"；"此刻按下没有"由两侧各自的
     * 既有通道回答 —— 客户端 {@code CoeSkillClient#toolSlotKeyHeld(槽位)}（既有键位表
     * {@code SLOT_KEYS} + {@code AllKeys.isPressed()}），服务端
     * {@code CoeSkillProvider#slotPressed(玩家, 槽位)}（{@code PlayerPressedKeys}）。
     * 纯客户端渲染走客户端那一条，<b>不</b>把服务端权威那套硬套上去；两条通道读的是同一个槽位号。</p>
     *
     * <p>⛔ 批 7/8 刚修掉"任一技能键"那种歧义（按 Shift / R 也会命中）：预选框
     * <b>不许</b>退回那个判据。</p>
     */
    public static int previewKeySlot() {
        return BowExclusiveShotItemSkill.ASTRAL_BARRAGE.ownSlot();
    }
}
