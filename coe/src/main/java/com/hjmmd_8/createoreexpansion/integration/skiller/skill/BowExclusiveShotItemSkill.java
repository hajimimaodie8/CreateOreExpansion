package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.PerSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowAstralBarrageConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowThunderMightConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowWaveShiftConfigs;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillProvider;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.BowShootSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/**
 * <b>三条「专属弓技能」在新内核里的执行槽</b>（2026-10-05 弓技能批 7；形态照
 * {@link BowShootItemSkill}）。
 *
 * <h2>它在修什么</h2>
 * <p>批 4/5/6 把三条专属技能（宝石弓「量波置换」/ 星界弓「星元波置」/ 雷鸣弓「雷鸣神力」）
 * 的触发判据写成<b>「本把弓的<b>任一</b>技能键被按住」</b>
 * （旧 {@code JadeTopazBowItem#waveShiftKeyHeld} →
 * {@code CoeSkillRelease#anyHeldItemSkillKeyPressed}）。那是一个<b>歧义判据</b>：键一 / 键二
 * 也是"本把弓的技能键"，所以按 Shift / R 同样会命中它 ⇒ 这一发被专属技能接管，而内核已经先
 * 按槽位 0/1 放出了继承的 {@code bow_curse} / {@code bow_disarm}（<b>扣了能量、进了冷却、
 * 在弓上写了 {@link com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem#TAG_SKILL}
 * 标记</b>）——那两笔账白付，而它们的效果一个字都没生效（箭根本没造出来）。</p>
 *
 * <h2>批 7 的形状：内核 release 写标记 → 弓的 shoot 读一次并清掉</h2>
 * <ol>
 *   <li><b>写（唯一一处）</b>：本类 {@link #release} 把<b>自己的注册 id</b>写进弓的
 *       {@code CUSTOM_DATA}（键 {@link #TAG_PENDING_SHOT}）。它<b>只会</b>被内核在
 *       "这个技能自己的按键槽位被按下"时调用 —— 槽位来自
 *       {@code CoeSkillProvider#componentOf} 现算出来的绑定表（本把弓的 USE 技能按声明顺序
 *       落在槽位 0/1/2），所以"按哪个键"这件事不再由弓自己猜，而是由内核按真实槽位派发。
 *       ⛔ 别的技能（含继承的 {@code bow_curse} / {@code bow_disarm}）<b>不会</b>写它；</li>
 *   <li><b>读 + 清（唯一一处）</b>：弓的 {@code shoot} 调一次 {@link #consumePendingShot}
 *       —— 读出来交给三道发射闸门，<b>同一次调用里清掉</b>，所以它最多只能影响一发；</li>
 *   <li><b>存在性查询（不读值、不消费）</b>：{@link #hasPendingShot} 只服务被动技能
 *       「元矢自生」的互斥闸门（无箭那一发若已被专属技能接管，就不再掷魔素骰子 —— 否则那道
 *       魔素标记会留在弓上、泄漏到下一发）；它<b>不能</b>变成第二个判定点；</li>
 *   <li><b>只清除（防泄漏回闸）</b>：{@link #clearPendingShot} 由弓的 {@code use()}（拉弓起点）
 *       调用一次，与批 3 的 {@code TAG_META_ESSENCE} 防泄漏闸门同形 —— 一次"没打成"的拉弓
 *       （无箭且能量不足）会在 {@code releaseUsing} 里提前 return，标记必须在下一次拉弓时清掉。</li>
 * </ol>
 *
 * <h2>为什么两个内核方法里只有 release 做事</h2>
 * <ul>
 *   <li>{@link #release}：<b>只写标记</b>。真正的发射（发波 / 降弹幕 / 落雷）在
 *       {@code JadeTopazBowItem#shoot} 里读标记之后交给各自的 launcher —— 理由是"整支箭都不造"
 *       这件事只能在 {@code shoot} 那一层做（在 {@code shootProjectile} 里 discard 会让
 *       {@code addFreshEntity} 每发打一条 WARN，见弓类注释）；</li>
 *   <li>{@link #consumeResource}：<b>批 13 起不再空实现</b> —— 三条技能的代价从"只有那一发箭"
 *       变成「那一发箭（或无箭时的 {@code NO_ARROW_COST}）<b>再加上</b>技能自己那一次释放的
 *       能量与冷却」，见下一节。</li>
 * </ul>
 *
 * <h2>2026-10-06 批 13：三条技能的能量与冷却（作者逐条裁定）</h2>
 * <p>作者原话：</p>
 * <blockquote>
 * 「<b>基础的能量都是一次释放技能，消耗 150 乘以技能等级点</b>」<br>
 * 「<b>只有弓，弓的话，冷却是3秒、4秒、5秒</b>」
 * </blockquote>
 * <p>落法（两个数都<b>不住在本类</b>：本类只按名读那三张 {@code *Configs}，一个数字都不写）：</p>
 * <ul>
 *   <li><b>能量</b> = 各自 {@code *Configs#ENERGY_COST_PER_LEVEL}（一级消耗 150）× <b>有效等级</b>
 *       ⇒ Lv1 / Lv2 / Lv3 = <b>150 / 300 / 450</b>。乘等级走既有算术
 *       {@code CoeSkillSupport.cost(..)} → {@code SkillEnergyCost.compute(..)}；</li>
 *   <li><b>冷却</b> = 各自 {@code *Configs#cooldownSecondsFor(有效等级)}
 *       ⇒ Lv1 / Lv2 / Lv3 = <b>3 / 4 / 5 秒</b>（<b>按等级</b>，三条同表 —— 作者把"3秒/4秒/5秒"
 *       与"150 × 等级"并排说，两处都是同一张等级表；⚠ 按技能各给一个值是另一种读法，
 *       本批<b>不</b>采用，见批 13 报告）；</li>
 *   <li><b>有效等级</b> = {@link CoeSkillSupport#effectiveLevel(ItemStack, ISkillInstance)}
 *       —— 与既有手持物技能<b>同一条口径</b>（内核实例的绑定等级 + 技艺提升/回溯，经该技能自己的
 *       {@code maxLevel} 钳位；三条技能各自只被一把弓携带、上限都是 3）。
 *       ⚠ <b>刻意不用</b> {@code BowTier#thirdSkillLevel()}（那是"发射效果"的等级读数）：
 *       效果半径与能量费<b>不能</b>各读一套等级，但"这一发在哪个槽位上、绑定几级"只有内核实例知道
 *       （量波置换在宝石弓是槽 2 的 ①、在星界/雷鸣弓是槽 1 的 ② —— 同一条技能、不同绑定），
 *       所以这里取实例等级：{@code 150 × 绑定等级} 在三条技能、四把弓上逐一正确；</li>
 *   <li><b>冷却载体</b> = {@link PerSkillCooldown}（<b>按技能记</b>）—— 三条技能都在
 *       {@code BowTier#perSkillCooldown()} = {@code true} 的那三把弓上（翠玉弓没有本族技能），
 *       形态照 {@code BloodPactItemSkill}。⚠ <b>不走迅启减冷却</b>：作者给的是固定 3/4/5 秒
 *       （血契置换那 30 秒同形）；弓上另外两条技能走的是"按技能记 + 迅启折扣"那一支，
 *       这条差异写进批 13 报告；</li>
 *   <li><b>两处同判</b>（既有红线，形状照 {@code BowShootItemSkill}）：
 *       {@link #consumeResource} 里不通过 ⇒ 一个 {@code DelayConsumable} 都不累加 ⇒ <b>不扣能</b>；
 *       {@link #release} 里不通过 ⇒ <b>不写标记、不起冷却</b>。于是"冷却中照扣能量"与
 *       "扣了却不执行"两种形状都写不出来；</li>
 *   <li><b>扣能前置</b>（{@code willDoWork} 等价物）= {@link #willTakeOverShot}：
 *       本把弓那一档确实在本技能自己的 {@code appliesTo} 表上（不在 ⇒ 这一发根本不会被本技能接管，
 *       扣了也是白扣）<b>且</b>冷却就绪。⚠ 能量<b>够不够</b>不在这里判：内核的
 *       {@code DelayConsumable#canConsume} 不过 ⇒ <b>整体放弃</b>（标记不写、冷却不起、能量不扣），
 *       那一发退化为一次普通射击 —— 这是与其余各条技能逐字相同的既有裁决；</li>
 *   <li><b>与箭那一笔账互不替代</b>：有箭那一发的箭仍被 {@code draw(..)} 收走，无箭那一发仍另付
 *       {@code JadeTopazBowItem.NO_ARROW_COST}（那条路一个字未改）。</li>
 * </ul>
 *
 * <p>⚠ 三条技能是<b>各自只被一把弓携带</b>的（不是共用 id），所以它们的等级上限写在自己的
 * {@code AllSkills} 条目上（{@code .maxLevel(3)}，与各自档位行 {@code BowTier#maxSkillLevel()} 同值）。</p>
 *
 * @since 1.0.0
 */
public final class BowExclusiveShotItemSkill implements ItemSkill<BowShootSkillContext> {

    /**
     * <b>「这一发要换成哪个专属技能」的标记</b>（弓 {@code CUSTOM_DATA} 上的一个字符串键，
     * 值是技能注册 id，例如 {@code createoreexpansion:bow_wave_shift}）。
     *
     * <p>⛔ 这个键名<b>只在本文件出现</b>（关卡 §43 用五根扫描钉住）：弓侧只调
     * {@link #consumePendingShot} / {@link #hasPendingShot} / {@link #clearPendingShot}，
     * 既不认识键名、也不认识"值长什么样"。这样"读第二次"这件事在编译期就没有地方可写。</p>
     */
    public static final String TAG_PENDING_SHOT = "jade_topaz_pending_shot";

    /** 宝石弓「量波置换」（键三 G · 起始等级 2） */
    public static final BowExclusiveShotItemSkill WAVE_SHIFT = new BowExclusiveShotItemSkill(
            "bow_wave_shift", BowWaveShiftConfigs::appliesTo, BowWaveShiftConfigs.ENERGY_COST_PER_LEVEL,
            BowWaveShiftConfigs::cooldownSecondsFor);

    /** 星界弓「星元波置」（键三 G · 起始等级 3） */
    public static final BowExclusiveShotItemSkill ASTRAL_BARRAGE = new BowExclusiveShotItemSkill(
            "bow_astral_barrage", BowAstralBarrageConfigs::appliesTo, BowAstralBarrageConfigs.ENERGY_COST_PER_LEVEL,
            BowAstralBarrageConfigs::cooldownSecondsFor);

    /** 雷鸣弓「雷鸣神力」（键三 G · 起始等级 3） */
    public static final BowExclusiveShotItemSkill THUNDER_MIGHT = new BowExclusiveShotItemSkill(
            "bow_thunder_might", BowThunderMightConfigs::appliesTo, BowThunderMightConfigs.ENERGY_COST_PER_LEVEL,
            BowThunderMightConfigs::cooldownSecondsFor);

    /** 本技能在内核注册表（{@code skiller:skill}）里的 id —— 必须与 {@code AllSkills} 那条一字不差。 */
    private final ResourceLocation id;

    /**
     * <b>本技能对哪几档弓生效</b> = 本技能自己那张 {@code *Configs#appliesTo(BowTier)}（方法引用）。
     *
     * <p>它是本类唯一的<b>「这一发会不会真的被本技能接管」</b>判据：不在表上 ⇒ 弓侧那道闸门
     * （{@code JadeTopazBowItem#shoot} 里按 {@code matches(id)} 分流）根本不会走本技能，
     * 于是 {@code consumeResource} 里也就不该扣能 —— 这就是本类 {@code willDoWork} 等价物的第一半。</p>
     */
    private final Predicate<BowTier> appliesTo;

    /** 本技能的<b>一级消耗</b>（作者批 13：150），实际消耗 = 本级消耗 × 有效等级（见类注释）。 */
    private final int energyCostPerLevel;

    /** 本技能<b>该等级的冷却秒数</b>（作者批 13：3 / 4 / 5，按等级），唯一来源 = 各自 {@code *Configs}。 */
    private final IntUnaryOperator cooldownSecondsFor;

    /**
     * <b>三条专属技能在"自己那把弓"上的按键槽位号</b>（键三，默认 G ⇒ 槽位 2）。
     *
     * <p>它与弓侧客户端读数里那行 {@code if (AllKeys.SKILL_RELEASE_3.isPressed()) return 2;}
     * 是<b>同一个事实</b>（槽位 = 该物品技能表里的下标，见 {@code CoeSkillProvider#convert}）：
     * 弓类四把里只有<b>带专属技能的那三把</b>有这个槽位，翠玉之弓只有 0/1。</p>
     */
    private static final int SLOT = 2;

    /**
     * <b>本发是否已被「专属技能」接管</b>（2026-10-05 弓技能批 8 ②）—— 服务端权威、只读。
     *
     * <h2>它在修什么</h2>
     * <p>同时按<b>专属键（键三）</b>与<b>继承键（键一 / 键二）</b>时，内核会<b>两条都放</b>
     * （每个槽位各答各的按键），而这一发最终<b>只有专属那条作数</b>（弓的 {@code shoot}
     * 读标记 ⇒ 整支箭都不造）⇒ 继承那条的能量与冷却<b>白付</b>（效果一个字没生效）。</p>
     *
     * <p>判据只有一条：本把弓的第三个技能槽位（{@link #SLOT}）<b>已绑定技能且此刻被按住</b>
     * （{@link CoeSkillProvider#slotPressed}：与释放路径同一份绑定表 + 同一个键位来源）。
     * 继承那两条在 {@code BowShootItemSkill} 的 {@code consumeResource} 与 {@code release}
     * <b>两处同判</b>（与"冷却类技能两处同判"的既有惯例同形）⇒ 被接管的那一发<b>既不扣能、
     * 也不进冷却</b>，标记也不写（那条箭根本不会造出来）。</p>
     *
     * <p>⚠ 翠玉之弓不受影响：它没有第三个技能槽 ⇒ {@code slotPressed} 恒 false
     * ⇒ 两条继承技能的既有行为一个字节未改。⚠ 装备段（槽位 3/4/5）与工具段<b>并存</b>是作者
     * 既有裁定（一个键同时触发两个<b>段</b>），与本方法处理的"同一把弓上的两条技能"是两件事。</p>
     *
     * @param player 触发者；非服务端（客户端预测）恒 {@code false}
     */
    public static boolean claimsShot(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && CoeSkillProvider.slotPressed(serverPlayer, SLOT);
    }

    /**
     * <b>本技能自己的槽位键号</b>（{@link #SLOT}，键三 ⇒ {@code 2}）—— 2026-10-06 弓技能批 11 第 ③ 条。
     *
     * <p>作者要"星界弓的圆形预选框只在按住那条技能自己的键时才出现"，而"那条技能在哪个键上"
     * 只有 {@link #SLOT} 一个答案：接管判据（{@link #claimsShot}）读它，预选框经
     * {@code BowAstralBarrageConfigs#previewKeySlot()} 也读它 —— 于是"专属键"这件事
     * <b>不可能</b>出现第二个数字。</p>
     *
     * <p>⚠ 它只回答"是哪个槽位"；"此刻按下没有"由两侧各自的既有通道回答
     * （客户端 {@code CoeSkillClient#toolSlotKeyHeld} / 服务端
     * {@code CoeSkillProvider#slotPressed}）。⛔ 预选框<b>不许</b>退回批 7/8 修掉的
     * "任一技能键"那种歧义判据。</p>
     */
    public int ownSlot() {
        return SLOT;
    }

    private BowExclusiveShotItemSkill(String path, Predicate<BowTier> appliesTo, int energyCostPerLevel,
                                     IntUnaryOperator cooldownSecondsFor) {
        this.id = ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, path);
        this.appliesTo = appliesTo;
        this.energyCostPerLevel = energyCostPerLevel;
        this.cooldownSecondsFor = cooldownSecondsFor;
    }

    /**
     * 这次读出来的标记是不是<b>本技能</b>写的。
     *
     * @param pendingShot {@link #consumePendingShot} 读出来的值（空串 = 这一发没有任何专属技能）
     */
    public boolean matches(String pendingShot) {
        return pendingShot != null && id.toString().equals(pendingShot);
    }

    @Override
    public void release(BowShootSkillContext context, ISkillInstance<BowShootSkillContext> instance) {
        Player player = context.getPlayer();
        ItemStack bow = context.bow();
        if (player == null || bow.isEmpty()) {
            return;
        }
        // 批 13：扣能前置的第二半（第一半在 consumeResource 里，两处逐条同判）—— 见 willTakeOverShot。
        if (!willTakeOverShot(player, bow)) {
            return;
        }
        // 冷却（按技能记；秒数取自本技能自己那张表的"该等级"那一行，本类一个数字都不写）。
        // 写在标记之前：与 BowShootItemSkill#release（先 startCooldown、后写标记）同序。
        int level = CoeSkillSupport.effectiveLevel(bow, instance);
        int ticks = CoeSkillSupport.cooldownTicks(bow, cooldownSecondsFor.applyAsInt(level));
        if (ticks > 0) {
            PerSkillCooldown.startTicks(player, id, ticks);
        }
        // 唯一的写点：把"这一发换成我"写进弓（弓的 shoot 读一次并清掉）。
        bow.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, custom -> custom.update(tag ->
                tag.putString(TAG_PENDING_SHOT, id.toString())));
    }

    /**
     * 消耗：<b>批 13 起真的扣</b> —— {@code 一级消耗（150）× 有效等级}，资源 = 工具能量
     * （{@code createoreexpansion:tool_energy}，与弓上另外两条技能同一个池子）。
     *
     * <p>与 {@link #release} <b>两处同判</b>同一个判据（{@link #willTakeOverShot}）：
     * 这里 return ⇒ 本实例一个 {@code DelayConsumable} 都不累加 ⇒ 内核的
     * {@code releaseBundle} 落账里没有它 ⇒ <b>不扣能</b>；只判 release 那一处是不够的。</p>
     *
     * <p>⚠ 能量<b>不够</b>不在这里判：照既有形状交给 {@link CoeSkillSupport#consume}（它补一次
     * 低能量提示后照常累加），由内核的 {@code canConsume} 整体裁决 —— 不过则标记不写、冷却不起、
     * 能量不扣，这一发退化为普通射击。</p>
     */
    @Override
    public void consumeResource(BowShootSkillContext context, Consumable consumable,
                                ISkillInstance<BowShootSkillContext> instance) {
        Player player = context.getPlayer();
        ItemStack bow = context.bow();
        if (player == null || bow.isEmpty()) {
            return;
        }
        if (!willTakeOverShot(player, bow)) {
            return;
        }
        int level = CoeSkillSupport.effectiveLevel(bow, instance);
        int cost = CoeSkillSupport.cost(bow, energyCostPerLevel, level);
        CoeSkillSupport.consume(player, bow, consumable, cost);
    }

    /**
     * <b>这一发真的会被本技能接管、并且付得起"冷却"这一关吗</b> —— 本技能的
     * {@code willDoWork} 等价物（既有口径：{@code consumeResource} 必须先过它才算数）。
     *
     * <p>两半，都是本技能自己的既有事实、不新造判据：</p>
     * <ol>
     *   <li><b>本把弓那一档在 {@link #appliesTo} 表上</b> —— 不在 ⇒ 弓侧那道
     *       {@code matches(id)} 闸门恒不通过，标记写不写都不会有效果 ⇒ 这一发<b>不该扣能</b>；</li>
     *   <li><b>本技能自己的冷却就绪</b>（{@link PerSkillCooldown}，按技能记）—— 创造模式恒就绪
     *       （与 {@code BowShootItemSkill#onCooldown} / {@code CoeSkillSupport#onCooldown}
     *       的创造模式豁免同一条口径）。</li>
     * </ol>
     *
     * <p>⚠ <b>刻意不判"能量够不够"</b>：那不是前置、而是内核的统一裁决点
     * （{@code DelayConsumable#canConsume}），在这里再判一次就是第二个口径。</p>
     */
    private boolean willTakeOverShot(Player player, ItemStack bow) {
        BowTier tier = bow.getItem() instanceof JadeTopazBowItem bowItem ? bowItem.tier() : null;
        if (tier == null || !appliesTo.test(tier)) {
            return false;
        }
        return player.isCreative() || PerSkillCooldown.isReady(player, id);
    }

    /**
     * <b>读 + 清除（唯一消费点）</b>：把这一发被指定的专属技能 id 读出来，并在同一次调用里
     * 从弓上清掉 —— 于是这个标记最多只能影响一发，下一发必须由内核重新写。
     *
     * @return 标记里写的技能 id；没有标记时返回空串（判据与 {@code getSkill(..)} 同形：空串 = 没有）
     */
    public static String consumePendingShot(ItemStack stack) {
        String pending = pendingShotOf(stack);
        if (pending.isEmpty()) {
            return "";
        }
        clearPendingShot(stack);
        return pending;
    }

    /**
     * <b>存在性查询（不读值、不消费）</b>：只给被动技能「元矢自生」的互斥闸门用
     * （这一发已被专属技能接管 ⇒ 被动不再掷魔素骰子）。
     *
     * <p>它与 {@link #consumePendingShot} 分开正是为了<b>不</b>多出一个消费点：这里只问
     * "有没有"，问完标记仍在弓上，等 {@code shoot} 那一次消费。</p>
     */
    public static boolean hasPendingShot(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .contains(TAG_PENDING_SHOT);
    }

    /**
     * <b>只清除、不读</b>：弓在 {@code use()}（拉弓起点）调一次，作为"上一次拉弓没打成"
     * 的防泄漏回闸（与批 3 的 {@code TAG_META_ESSENCE} 那道闸门同形）。
     */
    public static void clearPendingShot(ItemStack stack) {
        stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, custom -> custom.update(tag ->
                tag.remove(TAG_PENDING_SHOT)));
    }

    /**
     * 读标记的<b>唯一一处 {@code getString}</b>（{@link #consumePendingShot} 与
     * {@link #hasPendingShot} 都不直接读值，前者调它一次，后者只问 contains）。
     */
    private static String pendingShotOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getString(TAG_PENDING_SHOT);
    }
}
