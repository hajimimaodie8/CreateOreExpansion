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
 *   <li><b>圆心</b> = 施放者<b>眼睛位置</b> + <b>视线方向</b> ×
 *       {@code BowAstralBarrageConfigs.ANCHOR_FORWARD_BLOCKS}（作者的"我方前面 4 格"；
 *       形状照抄既有那条唯一口径 —— {@code StarShockWaveLauncher} 的"眼睛 + 准心 × N 格"，
 *       退化视线回落到 +Z 也照抄它）；</li>
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
 *       {@code setHitEffect} / {@code setFiringBatch}）。</li>
 * </ul>
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
     * @return 本次弹幕的批次号（&lt; 0，供日志 / 关卡核对）
     */
    public static int fire(ServerLevel world, LivingEntity shooter, int level) {
        Vec3 look = shooter.getLookAngle();
        if (look.lengthSqr() < 1.0E-6D) {
            // 退化视线（俯仰 ±90° 时原版也会给单位向量，这里只是形状保底）：与星芒嬗震 / 批 4 同一处兜底
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        look = look.normalize();
        Vec3 anchor = shooter.getEyePosition()
            .add(look.scale(BowAstralBarrageConfigs.ANCHOR_FORWARD_BLOCKS));

        double radius = BowAstralBarrageConfigs.radiusFor(level);
        double dropY = rainOriginY(world, shooter, anchor);
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
                () -> dropPotionArrow(world, shooter, anchor, radius, dropY, immobilizeTicks)));
        }
        for (int i = 0; i < waves; i++) {
            int delay = i * interval;
            server.tell(new TickTask(startTick + delay,
                () -> dropEssenceWave(world, shooter, anchor, radius, dropY, waveLevel, immobilizeTicks, batch)));
        }

        // 波相关日志一律走 WaveDiag（全系统唯一出口，前缀/开关只在那里定义）。
        // 这一行让"锚点在哪、圆盘多大、降下多少、排程多久、滞留多长"在日志里可查 ——
        // 作者只有日志可验收（在场内也能靠它区分"生成了没有 / 为什么没有"）。
        WaveDiag.trace(
            "星元波置：技能 {} 级 → 锚点 {}（眼睛 + 视线前推 {} 格），半径 {} 格的圆盘，落点高度 {}；排程降下 {} 支嬗乱药水箭（{} tick 时长）+ {} 枚随机魔素 {} 级波（命中附加滞留缓慢，{} tick），每 {} tick 一滴，滞留 {} tick，批次 {}",
            level, anchor, fmt2(BowAstralBarrageConfigs.ANCHOR_FORWARD_BLOCKS), fmt2(radius), fmt2(dropY),
            arrows, BowAstralBarrageConfigs.DISORDER_TICKS, waves, waveLevel, immobilizeTicks,
            interval, immobilizeTicks, batch);
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
     *       初速向下（{@code ARROW_INITIAL_FALL_SPEED}）—— 剩下的一切交给原版箭的物理
     *       （重力、拖拽、命中、插在方块上），本类不碰。</li>
     * </ol>
     */
    private static void dropPotionArrow(ServerLevel world, LivingEntity shooter, Vec3 anchor, double radius,
                                        double dropY, int immobilizeTicks) {
        Vec3 offset = BowAstralBarrageConfigs.discOffset(world.random, radius);
        ItemStack stack = new ItemStack(Items.TIPPED_ARROW);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(),
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
        arrow.setPos(anchor.x + offset.x, dropY, anchor.z + offset.z);
        arrow.setDeltaMovement(new Vec3(0.0D, -BowAstralBarrageConfigs.ARROW_INITIAL_FALL_SPEED, 0.0D));
        arrow.getPersistentData().putBoolean(TAG_BARRAGE, true);
        world.addFreshEntity(arrow);
    }

    /**
     * <b>降下一枚"带随机魔素的能量波"</b>：既有 {@link ChargerWaveEntity}，方向正下方。
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
     */
    private static void dropEssenceWave(ServerLevel world, LivingEntity shooter, Vec3 anchor, double radius,
                                        double dropY, int waveLevel, int immobilizeTicks, int batch) {
        Vec3 offset = BowAstralBarrageConfigs.discOffset(world.random, radius);
        Vec3 pos = new Vec3(anchor.x + offset.x, dropY, anchor.z + offset.z);
        ChargerWaveEntity wave = new ChargerWaveEntity(world, pos, BowAstralBarrageConfigs.FALL_DIRECTION, waveLevel);
        wave.trySetWaveType(WaveTypes.ATTACK);
        wave.setOwner(shooter);
        WaveTrailStyle essence = BowMetaArrowTrait.randomEssence(world.random);
        wave.trySetEssence(essence);
        // 命中附加"滞留"的主成分：高等级缓慢（唯一槽位；跳跃削弱半条由同一片区域里落下的药水箭给出）
        wave.setHitEffect(MobEffects.MOVEMENT_SLOWDOWN, immobilizeTicks,
            BowAstralBarrageConfigs.STAGNATION_AMPLIFIER);
        wave.setFiringBatch(batch);
        world.addFreshEntity(wave);
        WaveDiag.trace("星元波置落波：{} 级波（{}），魔素={}，落点 {}（半径 {} 格的圆盘），滞留缓慢 {} tick，批次 {}",
            waveLevel, WaveLevels.glyph(waveLevel), essence.name(), pos, fmt2(radius), immobilizeTicks, batch);
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
