package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowAstralBarrageConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * <b>「在锚定的圆形区域上降下弹幕」那一段</b> —— 弓技能批 6（星界弓「星元波置」，
 * 作者 2026-10-05，<b>按作者的新定义整条返工</b>）。
 *
 * <h2>作者原话（逐字，本类存在的唯一理由）</h2>
 * <blockquote>
 * 不是发射能量波哈，不是替换哈，就是锚定我方前面 4 格的一块圆形区域，半径等于技能等级加 1，
 * 空中落下无数带有嬗乱效果的药水箭与魔速能量波。魔素随机 8 抽 1。射中之后会造成滞留效果，
 * 时间 4 秒、5 秒、6 秒
 * </blockquote>
 *
 * <h2>它<b>不</b>做什么（与批 5 那条的分水岭）</h2>
 * <ul>
 *   <li><b>不发射"那枚波"</b>：批 5 的星界弓走的是 {@link BowWaveShiftLauncher}
 *       （按键那一发换成"一枚带重力的攻击波 + 0/1/2 枚环绕伴随波"）—— <b>作者撤回了这条口径</b>，
 *       本类与它零共用（只共用号源，见下）。</li>
 *   <li><b>不替换箭</b>：本技能<b>整支箭都不造</b>（与批 4 / 批 5 同一条"覆写 {@code shoot}"的路，
 *       见 {@code JadeTopazBowItem#shoot} 的说明），落下的东西是<b>本类新生成的实体</b>。</li>
 *   <li><b>不</b>造实体类型 / 模型 / 贴图 / 渲染器 / 注册项 / 语言键：落下的两样都是
 *       <b>原版 / 既有</b>实体 —— {@code minecraft:arrow}（药水箭）与既有
 *       {@code createoreexpansion:charger_wave}（能量波，与三台应力充能器<b>同一个类型、
 *       同一个渲染器</b>）。</li>
 * </ul>
 *
 * <h2>落点怎么算（三个数全在 {@link BowAstralBarrageConfigs}，本类一个都不写）</h2>
 * <ol>
 *   <li><b>圆心</b> = {@code BowAstralBarrageConfigs#previewCenter}(施放者眼睛, 视线, 松手时的拉弓
 *       tick 数) —— <b>批 9 起这是全仓唯一一处圆心算法</b>（形状仍是既有那条唯一口径
 *       {@code StarShockWaveLauncher} 的"眼睛 + 准心 × N 格"，退化视线回落到 +Z 的兜底也一起
 *       搬进了真源）。它同时是<b>客户端预选框</b>画的那个圆心 ⇒ 客户端与服务端不可能有两套算法
 *       （关卡 {@code bow9-one-centre}：两个调用点都不许自己 {@code scale(} 视线）；</li>
 *   <li><b>半径</b> = {@code BowAstralBarrageConfigs.radiusFor(level)}（作者："半径等于技能等级加 1"；
 *       实机星界弓的档位起始等级 = 3 ⇒ <b>4</b> 格）；</li>
 *   <li><b>降下高度</b> = {@code rainOriginY(..)}：从锚点正上方
 *       {@code CEILING_SEARCH_MIN_HEIGHT} 处往上一路探到 {@code MAX_FALL_HEIGHT}，
 *       探到实体方块就落在它下面 {@code CEILING_CLEARANCE} 格处（"空中"的最保守落地：
 *       露天就从 16 格高开始落，屋里/洞里就落在天花板下面，而不是一头扎进石头里）。</li>
 * </ol>
 * <p>每一滴的水平偏移走 {@code BowAstralBarrageConfigs.discOffset(..)}（圆内<b>面积均匀</b>采样）
 * —— 本类<b>不写半径、不写角度公式</b>，"圆形区域"这个几何口径只有数值真源那一处。</p>
 *
 * <h2>★ "无数" = 条数 + 节拍（持续掉落）</h2>
 * <p>作者只说"无数"，没有确定数字 ⇒ 本批把它落成"<b>可配置的条数</b> +
 * <b>持续 tick 的节拍</b>"：{@code ARROW_COUNT} 支药水箭与 {@code WAVE_COUNT} 枚能量波，
 * 每 {@code SPAWN_INTERVAL_TICKS} tick 各降一滴，用原版既有的
 * {@code MinecraftServer#tell(new TickTask(tick, ..))} 排程（<b>不是</b>自建计时器、
 * <b>不是</b>新实体、<b>不是</b>每 tick 的世界事件）。排程里的每一个闭包只做一件事：
 * 在圆盘上随机取一点，放一朵落下的箭 / 一枚落下的波。</p>
 *
 * <h2>★ 两条成分（各自的施加面）</h2>
 * <ul>
 *   <li><b>药水箭</b> = {@code minecraft:tipped_arrow} + {@code POTION_CONTENTS}：
 *       效果清单 = <b>嬗乱</b>（本模组既有 {@code createoreexpansion:transmutation_disorder}，
 *       时长 / 等级取自 {@code BowAstralBarrageConfigs.DISORDER_TICKS} /
 *       {@code #DISORDER_AMPLIFIER} —— 那两个常量与"异"魔素那一支的既有 3 秒由关卡
 *       {@code bow6-disorder-source} 用一条跨文件等式钉死）
 *       + <b>滞留</b>（缓慢 + 跳跃削弱，时长取 {@code immobilizeTicksFor(level)}）。
 *       施加者是<b>原版 {@code Arrow#doPostHurtEffects}</b>：它对 {@code customEffects} 逐条
 *       {@code addEffect} ⇒ 一切既有"效果免疫/拦截"判据（{@code MobEffectEvent.Applicable}）
 *       自动生效，本类不写免疫；</li>
 *   <li><b>能量波</b> = 既有 {@link ChargerWaveEntity}：波型 = 攻击态（与"变器攻击波变态"
 *       引燃出来的是同一个）、主人 = 施放者、<b>魔素各自随机</b>（
 *       {@code BowMetaArrowTrait#randomEssence}，同一条八魔素池 —— 作者"魔素随机 8 抽 1"）、
 *       命中附加效果要素 = <b>滞留的主成分（缓慢）</b>（既有
 *       {@code setHitEffect(效果, tick, amplifier)}，星芒嬗震用的同一个要素）。</li>
 * </ul>
 *
 * <h2>★ 不许伤害施放者（两条成分各走各的既有约定）</h2>
 * <ul>
 *   <li><b>波</b>：{@code setOwner(shooter)} ⇒ 既有命中链的 {@code isOwner(e)} 把主人排除在命中
 *       列表之外（与星芒嬗震 / 批 4 完全同一条），本类不写第二套判据；</li>
 *   <li><b>药水箭</b>：原版箭在 {@code leftOwner} 之后<b>可以</b>命中主人（原版没有"排除发射者"
 *       这条），而本技能的圆盘圆心在前方 4 格、半径可达 4 格 ⇒ <b>施放者自己就站在圆盘边上</b>。
 *       所以本类给每一支箭打上"这是弹幕箭"的标记（{@link #isBarrageArrow(Arrow)}），
 *       由 {@code JadeTopazBowEventHandler#onProjectileImpact} 取消"打中主人"的那一次命中
 *       （原版 {@code ProjectileImpactEvent} 取消后箭照飞，见那里的说明）。</li>
 *   <li><b>顺带</b>：{@code setOwner(玩家)} 会让原版把箭的 {@code pickup} 从
 *       {@code DISALLOWED} 翻成 {@code ALLOWED}（{@code AbstractArrow#setOwner}）——
 *       落地后可捡的"免费嬗乱药水箭"显然不是作者要的，本类显式改回 {@code DISALLOWED}。</li>
 * </ul>
 *
 * <h2>本类<b>不</b>做什么（续）</h2>
 * <ul>
 *   <li><b>不</b>判"该不该放"（物品侧的两个闸门：{@code BowAstralBarrageConfigs#appliesTo} +
 *       技能键，见 {@code JadeTopazBowItem#fireAstralBarrageInsteadOfArrow}）；</li>
 *   <li><b>不</b>扣能量、<b>不</b>碰冷却、<b>不</b>扣耐久（耐久与原版同一笔账在物品侧扣）；</li>
 *   <li><b>不</b>碰共享波实体的形状（批 6 对 {@code AbstractChargerWaveEntity} <b>零改动</b>：
 *       本类只用它<b>已经</b>有的 {@code setOwner} / {@code trySetWaveType} / {@code trySetEssence} /
 *       {@code setHitEffect} / {@code setFiringBatch}）。⚠ <b>批 9 也照这条办</b>："波速 = 箭速"
 *       用的是它<b>已经</b>有的速度修正要素 {@code addSpeedOffset(double)}（波速调节器用的同一个），
 *       共享波实体仍然<b>一个字节都没动</b>（关卡 {@code bow6-public-shape} 照旧守着这条）。</li>
 * </ul>
 *
 * <h2>2026-10-05 弓技能批 9（作者原话，逐字）</h2>
 * <blockquote>
 * 1. 把箭的数量改少一点，能量波的数量翻一倍。<br>
 * 2. 下优化：当他拉弓时，应该会看到一个圆形技能范围的预选框。……移动机制：预选框会一点一点往远离
 * 视角的方向移动，边缘最多移动至距离玩家准心 5 格的位置。……释放机制：确定位置后松手，开始释放技能。
 * 释放的过程中，箭和能量波应该同步落下，两者落下速度应该一致，能量波的速度和箭的速度就是一样的……
 * 波速是它里面的一个参数。技能释放完之后，该预选框才会消失。
 * </blockquote>
 * <ul>
 *   <li><b>数量</b>：{@code ARROW_COUNT 20 → 10}、{@code WAVE_COUNT 10 → 20} —— 本类的两个循环
 *       仍是按名读真源的那两行，<b>一个数字都不写</b>；</li>
 *   <li><b>圆心</b>：{@link #fire} 多收一个 {@code drawnTicks}（松手那一刻的拉弓时长），
 *       圆心交给真源的 {@code previewCenter} ⇒ 与客户端预选框<b>同源</b>；</li>
 *   <li><b>波速 = 箭速</b>：落下的每一枚波都按名调
 *       {@code wave.addSpeedOffset(BowAstralBarrageConfigs.waveSpeedOffsetFor(waveLevel))}
 *       —— 目标速度由 {@code ARROW_INITIAL_FALL_SPEED}（箭的落速，<b>箭本身没改</b>）经仓里唯一的
 *       格/秒换算得出；</li>
 *   <li><b>本类不画圈</b>：预选框是<b>客户端</b>的事（{@code BowAstralBarragePreviewRenderer}），
 *       它只问真源要圆心与半径。</li>
 * </ul>
 *
 * <h2>2026-10-06 弓技能批 11（作者原话，逐字）</h2>
 * <blockquote>
 * 第1条不必要完全严格，但是误差尽量都在1秒之内，而且射下乱箭或能量波的时候，关于世界的外轴，
 * 有一个小范围的、不超过10°左右的倾斜角，来塑造出相应的感觉。第二条需要限制，第三条最好贴地，
 * 但不贴地也是可以接受。你可以加入一个检测机制，通过玩家的纵坐标与附近地面的距离（高度差）来实现：
 * (a) 当玩家飞在空中……圈就没必要固定在地面上。(b) 当玩家在起伏较小的平坡上……圈最好固定在地上。
 * 2. 高度差显示优化：如果有高度差，圈的显示最好在衔接的地方加入一些预选框。
 * </blockquote>
 * <p>本类落的四处（数值与几何全部按名取自 {@link BowAstralBarrageConfigs}）：</p>
 * <ol>
 *   <li><b>①到达同步</b>：落下的每一枚波装上波实体<b>既有</b>的重力要素
 *       （{@code setGravity(真源那条由箭的加速度换算出来的值)}）—— 此前波是匀速、箭有原版重力，
 *       长落差会把两者拉开；现在两支成分的下落剖面同源，到达差由真源现算并打进日志。
 *       ⛔ 箭本身一个字节没改（原版重力是全局行为）；
 *       ⛔ 宝石弓那一发是另一个技能（批 10 已按作者裁定撤回重力），本批不碰它；</li>
 *   <li><b>②散射</b>（作者 2026-10-06 补充裁定："小角度散射的话，整个场可以稍微错乱。
 *       每个都是随机生成的，都是散射，发射角度也可以是不一样的"）：<b>每一枚</b>药水箭 /
 *       每一枚波在<b>出生的那一刻</b>各自掷一对方位角 + 一个 ≤ 真源上限的倾角
 *       （服务端权威的 {@code world.random}），当场写进实体 —— 箭写 {@code setDeltaMovement}、
 *       波交给构造器写 {@code movement}，其后<b>没有任何地方重算</b>（否则方向会逐 tick 抖）。
 *       箭的初速走真源的"落向 × 箭速"规则（<b>速率不变</b>）；斜插的横向漂移由真源按同一条
 *       下落剖面算出来，<b>逐枚</b>把出生点往上游让掉 ⇒ 落点仍在圈里；</li>
 *   <li><b>④贴地判据</b>：{@code nearbyGroundY}(向下射线) + {@code sticksToGround}(高度差阈值) +
 *       {@code landingCentre}(圆心 Y 收口) —— 三步都在真源，客户端渲染器调的是<b>同样这三个</b>；</li>
 *   <li><b>③专属键</b>：本类<b>不</b>读键（那端在渲染器；服务端这一发的闸门仍是内核写下的
 *       专属标记，见 {@code JadeTopazBowItem#fireAstralBarrageInsteadOfArrow}）。</li>
 * </ol>
 *
 * @since 1.0.0
 */
public final class BowAstralBarrageLauncher {

    private BowAstralBarrageLauncher() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * <b>"这一支箭是星界弓弹幕落下的"标记键</b>（写在箭的 {@code persistentData} 上，
     * 形状与 {@code JadeTopazBowItem} 的 {@code TAG_SKILL} / {@code TAG_SOURCE_BOW} 同源：
     * 生成时写、命中时读）。
     *
     * <p>刻意<b>不</b>挂到 {@code JadeTopazBowItem} 上：那个类有一道"公开面一个成员都不许新增"
     * 的跨批关卡（批 5 的 {@code bow5-public-shape} 钉着它与共享波实体的公开/受保护签名集），
     * 而本标记<b>只有本类写、只有本类读</b>（命中处理器不认键名，只调
     * {@link #isBarrageArrow(Arrow)}）⇒ 放在这里既不动那道面，也没有第二个认这个键的文件。</p>
     *
     * <p>⚠ 它与"来源标记"（{@code TAG_SOURCE_BOW}）是<b>两件事</b>：弹幕箭<b>不是</b>弓射出来的，
     * 所以它<b>不带</b>来源标记 ⇒ 既不会滚那套基础概率效果，也不会带"元矢自生"的魔素标记。</p>
     */
    private static final String TAG_BARRAGE = "coe_astral_barrage";

    /**
     * <b>放一次「星元波置」</b>：在锚定圆盘上排程降下"嬗乱药水箭 + 随机魔素能量波"。
     *
     * <p>调用前世界必须是服务端（{@code ServerLevel}）：落点几何、实体生成、掉落排程都只在服务端
     * 有权威结果。本方法<b>立刻返回</b>（弹幕由排程一滴一滴落下，不是一次刷完）。</p>
     *
     * @param world   服务端世界
     * @param shooter 施放者（取<b>眼睛位置 + 视线</b>当锚点；同时是落下的箭与波的主人 =
     *                两者都据此把他排除在命中之外）
     * @param level   技能等级（1~3，越界夹取；星界弓传的是 {@code BowTier#baseSkillLevel()} = 3）
     * @param drawnTicks <b>松手那一刻已经拉了多少 tick</b>（批 9）：预选框圆心与弹幕圆盘圆心
     *                都由它经 {@code BowAstralBarrageConfigs#previewCenter} 算出 ——
     *                客户端画圈用的是<b>同一个方法、同一个输入口径</b>（{@code #drawnTicks}），
     *                所以"圈在哪"与"落在哪"不可能分家。调用点传的是 {@code releaseUsing} 里
     *                已经算好的 {@code pullTime}（经弓上的私有暂存键搬过来的），
     *                <b>本类不自己猜</b>。
     * @return 本次弹幕的批次号（&lt; 0，供日志 / 关卡核对）
     */
    public static int fire(ServerLevel world, LivingEntity shooter, int level, int drawnTicks) {
        // 批 9：圆心不再在本类现算 —— 它只住数值真源那一处（眼睛 + 视线 × 推进量），
        // 于是"客户端画的圈"与"服务端落的点"共用同一个算法（退化视线的兜底也在那里）。
        Vec3 anchor = BowAstralBarrageConfigs.previewCenter(
            shooter.getEyePosition(), shooter.getLookAngle(), drawnTicks);

        // ★ 批 11④（作者 2026-10-06："你可以加入一个检测机制，通过玩家的纵坐标与附近地面的距离"）：
        //   "附近地面"由真源那一条向下的方块射线给出；高度差在阈值以内 = 起伏较小的平坡 ⇒ 圈贴地，
        //   否则 = 飞在空中 ⇒ 圈留在准心那个平面。⚠ 圈与弹幕圆心是**同一个** landingCentre
        //   （客户端画圈走同一个方法）⇒ 不会出现"圈贴在地上、箭落在半空"。
        double nearbyGroundY = BowAstralBarrageConfigs.nearbyGroundY(world, shooter);
        boolean stickToGround = BowAstralBarrageConfigs.sticksToGround(shooter.getY(), nearbyGroundY);
        Vec3 discCentre = BowAstralBarrageConfigs.landingCentre(anchor, nearbyGroundY, stickToGround);

        double radius = BowAstralBarrageConfigs.radiusFor(level);
        double dropY = rainOriginY(world, shooter, discCentre);
        // ★ 批 11② 修订（作者 2026-10-06 补充裁定："对了，小角度散射的话，整个场可以稍微错乱。
        //   每个都是随机生成的，都是散射，发射角度也可以是不一样的。这样的话才更有那种氛围"）：
        //   **每一枚各自**在出生的那一刻掷自己的落向 —— 本类**不在这里掷**（一次施放一个方向
        //   就是"整场统一"，正是这条裁定推翻的读法），只把"从出生点落到圈那个平面"的落差
        //   （= 斜插漂移要补偿的那段行程）交给两个落点方法，由它们各自掷、各自写进实体。
        //   为什么落差取"出生点 → 圈那个平面"而不是 → 地面：圈就是这块技能区域，两支成分要在
        //   圈上对齐（贴地时圈本来就在地面上，两者是同一个高度；飞在空中时圈在准心平面，
        //   "到达"就是穿过那个平面的时刻）。让掉的量与 ① 用的是同一条下落剖面。
        double fallToRing = dropY - discCentre.y;
        int batch = BowWaveShiftLauncher.nextBatch();
        int waveLevel = BowAstralBarrageConfigs.waveLevelFor(level);
        int immobilizeTicks = BowAstralBarrageConfigs.immobilizeTicksFor(level);

        MinecraftServer server = world.getServer();
        int startTick = server.getTickCount();
        int interval = BowAstralBarrageConfigs.SPAWN_INTERVAL_TICKS;
        int arrows = BowAstralBarrageConfigs.ARROW_COUNT;
        int waves = BowAstralBarrageConfigs.WAVE_COUNT;
        for (int i = 0; i < arrows; i++) {
            int delay = i * interval;
            server.tell(new TickTask(startTick + delay,
                () -> dropPotionArrow(world, shooter, discCentre, radius, dropY, immobilizeTicks,
                    fallToRing)));
        }
        for (int i = 0; i < waves; i++) {
            int delay = i * interval;
            server.tell(new TickTask(startTick + delay,
                () -> dropEssenceWave(world, shooter, discCentre, radius, dropY, waveLevel, immobilizeTicks,
                    batch, fallToRing)));
        }

        // 波相关日志一律走 WaveDiag（全系统唯一出口，前缀/开关只在那里定义）。
        // 这一行让"锚点在哪、圆盘多大、降下多少、排程多久、滞留多长"在日志里可查 ——
        // 作者只有日志可验收（在场内也能靠它区分"生成了没有 / 为什么没有"）。
        WaveDiag.trace(
            "星元波置：技能 {} 级 → 锚点 {}（眼睛 + 视线前推 {} 格，松手时已拉弓 {} tick），半径 {} 格的圆盘，落点高度 {}；排程降下 {} 支嬗乱药水箭（{} tick 时长）+ {} 枚随机魔素 {} 级波（下落速度 = 箭速 {} 格/秒，命中附加滞留缓慢，{} tick），每 {} tick 一滴，滞留 {} tick，预选框释放后再存活 {} tick，批次 {}",
            level, discCentre, fmt2(BowAstralBarrageConfigs.previewForwardBlocks(drawnTicks)), drawnTicks,
            fmt2(radius), fmt2(dropY),
            arrows, BowAstralBarrageConfigs.DISORDER_TICKS, waves, waveLevel,
            fmt2(BowAstralBarrageConfigs.fallSpeedBlocksPerSecond()), immobilizeTicks,
            interval, immobilizeTicks, BowAstralBarrageConfigs.barrageScheduleTicks(), batch);
        // ★ 批 11：落地感那三条（贴不贴地 / 散射多少 / 两支成分的到达差）也各留一行 ——
        //   作者只有日志可验收，而这三件事全都"不报错、不打日志也能静默坏掉"。
        //   "到达差"取的是本次弹幕的**上界**（真源在倾角区间两端取较大者）：散射让每枚的倾角不同，
        //   而两支成分的竖直剖面只差一个共同的 cos 因子 ⇒ 逐枚的差同量级，报区间上界即可。
        WaveDiag.trace(
            "星元波置落地感：附近地面 {}（玩家纵坐标 {}），{}；散射 = 每枚各自掷一个 0~{}° 的落向（服务端 world.random，出生即定死）；出生点逐枚往上游让（落差 {} 格）；箭与波到达差（区间上界）{} 秒（作者上限 {} 秒）",
            fmt2(nearbyGroundY), fmt2(shooter.getY()),
            stickToGround ? "圈贴地（平坡）" : "圈留在准心平面（在空中）",
            fmt2(BowAstralBarrageConfigs.tiltMaxDegrees()), fmt2(fallToRing),
            fmt2(BowAstralBarrageConfigs.arrivalDeltaBoundSeconds(fallToRing)),
            fmt2(BowAstralBarrageConfigs.ARRIVAL_SYNC_TOLERANCE_SECONDS));
        return batch;
    }

    /**
     * <b>弹幕的降下高度</b>（锚点正上方多少格）：从 {@code CEILING_SEARCH_MIN_HEIGHT} 处往上探到
     * {@code MAX_FALL_HEIGHT}，探到实体方块就落在它下面 {@code CEILING_CLEARANCE} 格处。
     *
     * <p>为什么要探一次（而不是直接写"锚点上方 16 格"）：作者要的是"<b>空中</b>落下"——
     * 玩家在地洞里直接按技能时，固定 16 格意味着箭与波<b>出生在石头里</b>（箭当场插进方块、
     * 波当场爆掉），整条技能看起来"什么都没发生"且没有任何报错。探一次天花板之后，
     * 洞里就落在洞顶之下，露天仍是 16 格高。</p>
     *
     * <p>射线形状照仓里既有的方块射线（{@code BowThunderMightLauncher#pickImpact} 那一处
     * {@code world.clip(new ClipContext(.., Block.COLLIDER, Fluid.NONE, shooter))}）；
     * 流体不算天花板（水里照样能从上面落下来）。三个数全在数值真源里。</p>
     */
    private static double rainOriginY(ServerLevel world, LivingEntity shooter, Vec3 anchor) {
        double lowest = anchor.y + BowAstralBarrageConfigs.CEILING_SEARCH_MIN_HEIGHT;
        Vec3 to = anchor.add(0.0D, BowAstralBarrageConfigs.MAX_FALL_HEIGHT, 0.0D);
        BlockHitResult ceiling = world.clip(new ClipContext(
            new Vec3(anchor.x, lowest, anchor.z), to,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (ceiling.getType() == HitResult.Type.MISS) {
            return to.y;
        }
        // 天花板就在头顶时也不把落点压到脚面上（夹到搜索起点 = "最低也从这个高度落"）
        return Math.max(ceiling.getLocation().y - BowAstralBarrageConfigs.CEILING_CLEARANCE, lowest);
    }

    /**
     * <b>降下一支"带嬗乱效果的药水箭"</b>：原版药水箭实体，效果清单 =
     * 嬗乱（既有时长/等级）+ 滞留（缓慢 + 跳跃削弱，时长按等级）。
     *
     * <p>三件必须一起做的事：</p>
     * <ol>
     *   <li><b>效果挂 {@code POTION_CONTENTS}</b>（原版药水箭就是这么带效果的）⇒ 命中时由
     *       {@code Arrow#doPostHurtEffects} 逐条施加，本类不写任何 {@code addEffect}；</li>
     *   <li><b>主人 = 施放者</b>（伤害归属照旧记在他头上；"不伤主人"由命中处理器那一支取消，
     *       见类注释）；</li>
     *   <li><b>打标记 + 落点 / 初速</b>：位置取圆盘上的一点、高度取弹幕的降下高度，
     *       初速沿<b>这一支箭自己</b>的落向、速率恒为 {@code ARROW_INITIAL_FALL_SPEED}（批 11②：
     *       散射，每枚一个方向）—— 剩下的一切交给原版箭的物理
     *       （重力、拖拽、命中、插在方块上），本类不碰。</li>
     * </ol>
     */
    private static void dropPotionArrow(ServerLevel world, LivingEntity shooter, Vec3 anchor, double radius,
                                        double dropY, int immobilizeTicks, double fallToRing) {
        Vec3 offset = BowAstralBarrageConfigs.discOffset(world.random, radius);
        // ★ 批 11② 修订：**这一支箭自己**的落向 —— 在出生的这一刻掷一次（服务端权威的
        //   world.random），倾角上限只住真源；斜插的横向漂移当场补偿掉（逐枚各自的方向 ⇒
        //   逐枚各自的补偿）。方向一写进实体就再没有第二次求值 ⇒ 不会逐 tick 抖。
        double tiltAzimuth = BowAstralBarrageConfigs.tiltAzimuthRadians(world.random);
        double tiltAngle = BowAstralBarrageConfigs.tiltAngleRadians(world.random);
        Vec3 fallDirection = BowAstralBarrageConfigs.fallDirection(tiltAzimuth, tiltAngle);
        Vec3 spawnShift = BowAstralBarrageConfigs.tiltSpawnShift(tiltAzimuth, tiltAngle, fallToRing);
        ItemStack stack = new ItemStack(Items.TIPPED_ARROW);
        // 粒子/药水色<b>显式钉成嬗乱自己的颜色</b>（{@code MobEffect#getColor()}，不写第二个色值）：
        // 原版那条"按效果算色"的公式拿 amplifier + 1 当权重（PotionContents#getColorOptional），
        // 而本清单里的"跳跃削弱"是一个<b>负</b> amplifier ⇒ 权重为负，算出来的色会偏到橙色去。
        // 作者要的是"带嬗乱效果的药水箭"，落在世界里的拖尾粒子就该是嬗乱那支颜色的。
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(),
            Optional.of(TransmutationEffects.TRANSMUTATION_DISORDER.value().getColor()),
            List.of(
                new MobEffectInstance(TransmutationEffects.TRANSMUTATION_DISORDER,
                    BowAstralBarrageConfigs.DISORDER_TICKS, BowAstralBarrageConfigs.DISORDER_AMPLIFIER),
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, immobilizeTicks,
                    BowAstralBarrageConfigs.STAGNATION_AMPLIFIER),
                new MobEffectInstance(MobEffects.JUMP, immobilizeTicks,
                    BowAstralBarrageConfigs.jumpWeakenAmplifier()))));
        Arrow arrow = new Arrow(world, shooter, stack, null);
        // 原版 setOwner(玩家) 会把 pickup 翻成 ALLOWED ⇒ 显式改回"捡不起来"：
        // 一次弹幕落下几十支药水箭，能捡就是把"技能"变成"免费刷嬗乱药水箭"。
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        // ★ 批 11②：出生点整体往上游让 tiltSpawnShift（斜插的漂移补偿）—— 落点因此仍落在
        //   预选框那块圆盘上。初速方向改走真源那条"落向 × 箭速"的规则：**速率一字未变**
        //   （仍恒等于 ARROW_INITIAL_FALL_SPEED），改的只有方向（作者："不许改变速度大小"）。
        arrow.setPos(anchor.x + offset.x + spawnShift.x, dropY, anchor.z + offset.z + spawnShift.z);
        arrow.setDeltaMovement(BowAstralBarrageConfigs.arrowInitialVelocity(fallDirection));
        arrow.getPersistentData().putBoolean(TAG_BARRAGE, true);
        world.addFreshEntity(arrow);
    }

    /**
     * <b>降下一枚"带随机魔素的能量波"</b>：既有 {@link ChargerWaveEntity}，
     * 方向 = <b>这一枚自己的落向</b>（批 11② 散射：每枚各自一个 ≤ 真源上限的小倾角）。
     *
     * <p>四件与批 4 / 星芒嬗震<b>逐条同形</b>的事（顺序也不能换）：</p>
     * <ol>
     *   <li><b>先成攻击波再设魔素</b>：{@code trySetEssence} 只在"会伤害的波型"上生效，
     *       顺序反了就是"整批波没有魔素"且没有任何报错；</li>
     *   <li><b>魔素各自随机</b>：每枚单独抽一次（同一条八魔素池，不去重、不排除相同）；</li>
     *   <li><b>主人 = 施放者</b>：既有命中链据此把他排除（"不伤发射者"的既有约定）；</li>
     *   <li><b>共用一个批次号</b>：本次弹幕的全部波（以及本技能同一次发射的其它波）互相豁免碰撞
     *       —— 它们出生在同一片空域里，不豁免就是"出生瞬间互相湮灭"。</li>
     * </ol>
     *
     * <p><b>批 9 加的第五件</b>（与上面四件同层、也走既有面）：<b>波速 = 箭速</b> ——
     * {@code wave.addSpeedOffset(真源给的差值)}，用的是波实体<b>已经</b>有的速度修正要素
     * （"波速是它里面的一个参数"）。目标速度与差值都在数值真源里算，本方法只按名调用。</p>
     */
    private static void dropEssenceWave(ServerLevel world, LivingEntity shooter, Vec3 anchor, double radius,
                                        double dropY, int waveLevel, int immobilizeTicks, int batch,
                                        double fallToRing) {
        Vec3 offset = BowAstralBarrageConfigs.discOffset(world.random, radius);
        // ★ 批 11② 修订：**这一枚波自己**的落向（与同一 tick 落下的那支箭各掷各的）——
        //   服务端权威的 world.random；方向交给构造器当场写进 movement，之后没有任何地方重算。
        double tiltAzimuth = BowAstralBarrageConfigs.tiltAzimuthRadians(world.random);
        double tiltAngle = BowAstralBarrageConfigs.tiltAngleRadians(world.random);
        Vec3 fallDirection = BowAstralBarrageConfigs.fallDirection(tiltAzimuth, tiltAngle);
        Vec3 spawnShift = BowAstralBarrageConfigs.tiltSpawnShift(tiltAzimuth, tiltAngle, fallToRing);
        Vec3 pos = new Vec3(anchor.x + offset.x + spawnShift.x, dropY, anchor.z + offset.z + spawnShift.z);
        ChargerWaveEntity wave = new ChargerWaveEntity(world, pos, fallDirection, waveLevel);
        wave.trySetWaveType(WaveTypes.ATTACK);
        wave.setOwner(shooter);
        // ★ 批 9：波速 = 箭速（作者："两者落下速度应该一致，能量波的速度和箭的速度就是一样的……
        //   波速是它里面的一个参数"）。用既有的"速度修正量"要素把落下的波设成与药水箭同速
        //   （目标速度与其差值全在数值真源里算，本类不写速度数字、更不改箭）：
        //   波实体只提供 addSpeedOffset（叠加语义），所以真源给的是"目标 − 该波级基础速度"。
        wave.addSpeedOffset(BowAstralBarrageConfigs.waveSpeedOffsetFor(waveLevel));
        // ★ 批 11①（作者 2026-10-06："误差尽量都在 1 秒之内"）：波此前是**匀速**下落、药水箭有
        //   原版重力会加速 ⇒ 长落差会把两者拉开。这里把波实体**既有**的重力要素装上，加速度
        //   由箭那一侧的常量换算得出（真源里那一条等式）⇒ 两支成分的下落剖面逐值同源，
        //   16 格落差的到达差不到 0.1 秒（上限 1 秒）。⚠ 装的是**弹幕这一批**波；
        //   宝石弓那一发（BowWaveShiftLauncher）批 10 已按作者裁定撤回重力，本批不碰它。
        wave.setGravity(BowAstralBarrageConfigs.FALL_ACCELERATION_BLOCKS_PER_SECOND_SQUARED);
        WaveTrailStyle essence = BowMetaArrowTrait.randomEssence(world.random);
        wave.trySetEssence(essence);
        // 命中附加"滞留"的主成分：高等级缓慢（唯一槽位；跳跃削弱半条由同一片区域里落下的药水箭给出）
        wave.setHitEffect(MobEffects.MOVEMENT_SLOWDOWN, immobilizeTicks,
            BowAstralBarrageConfigs.STAGNATION_AMPLIFIER);
        wave.setFiringBatch(batch);
        world.addFreshEntity(wave);
        WaveDiag.trace("星元波置落波：{} 级波（{}），魔素={}，落点 {}（半径 {} 格的圆盘），下落速度 {} 格/秒（= 箭速），滞留缓慢 {} tick，批次 {}",
            waveLevel, WaveLevels.glyph(waveLevel), essence.name(), pos, fmt2(radius),
            fmt2(wave.getWaveSpeed()), immobilizeTicks, batch);
    }

    /**
     * <b>这一支箭是不是本技能弹幕落下的</b>（唯一判据，形状照 {@code JadeTopazBowItem#isFromOurBow}：
     * 一个字符串键的"写了就是"）。
     *
     * <p>唯一消费者是 {@code JadeTopazBowEventHandler#onProjectileImpact}：它据此把"弹幕箭打中
     * 施放者本人"那一次命中取消掉（原版箭一旦 {@code leftOwner} 就可以命中主人，而本技能的圆盘
     * 圆心在施放者前方 4 格、半径可达 4 格 ⇒ 他自己就站在圆盘边上）。</p>
     */
    public static boolean isBarrageArrow(Arrow arrow) {
        return arrow.getPersistentData().getBoolean(TAG_BARRAGE);
    }

    /**
     * 两位小数、<b>与区域设置无关</b>的格式化（日志要机器可比对：某些区域会把小数点写成逗号）。
     *
     * <p>与 {@code StarShockRuntime#fmt2} / {@code BowWaveShiftLauncher#fmt2} 同一个形状 ——
     * 那两处都是包级私有（各自领地的日志格式化），本类照形自备一份。</p>
     */
    private static String fmt2(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
