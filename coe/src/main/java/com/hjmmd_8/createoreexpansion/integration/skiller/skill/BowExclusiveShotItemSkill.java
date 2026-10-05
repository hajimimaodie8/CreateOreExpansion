package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.BowShootSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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
 *   <li>{@link #consumeResource}：<b>空实现</b>（契约要求，不是没写完）。这三条技能的代价是
 *       <b>那一发箭本身</b>（有箭时 {@code draw(..)} 已经把箭从物品栏收走，无箭时那条既有路
 *       已经付过 {@code JadeTopazBowItem.NO_ARROW_COST} 点能量），冷却也<b>无</b>（作者没给）
 *       —— 这与批 4/5/6 已落地的口径逐字相同，本批<b>不</b>给它们新增耗能或冷却（硬边界：
 *       三套行为的表现一个字不许改）。</li>
 * </ul>
 *
 * <p>⚠ 三条技能是<b>各自只被一把弓携带</b>的（不是共用 id），所以它们的等级上限写在自己的
 * {@code AllSkills} 条目上（{@code .maxLevel(3)}，与各自档位行
 * {@code BowTier#maxSkillLevel()} 同值）；发射用的等级仍是<b>档位起始等级</b>
 * （{@code BowTier#baseSkillLevel()} = 2/3/3），与批 4/5/6 逐字相同。</p>
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
    public static final BowExclusiveShotItemSkill WAVE_SHIFT = new BowExclusiveShotItemSkill("bow_wave_shift");

    /** 星界弓「星元波置」（键三 G · 起始等级 3） */
    public static final BowExclusiveShotItemSkill ASTRAL_BARRAGE =
            new BowExclusiveShotItemSkill("bow_astral_barrage");

    /** 雷鸣弓「雷鸣神力」（键三 G · 起始等级 3） */
    public static final BowExclusiveShotItemSkill THUNDER_MIGHT =
            new BowExclusiveShotItemSkill("bow_thunder_might");

    /** 本技能在内核注册表（{@code skiller:skill}）里的 id —— 必须与 {@code AllSkills} 那条一字不差。 */
    private final ResourceLocation id;

    private BowExclusiveShotItemSkill(String path) {
        this.id = ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, path);
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
        // 唯一的写点：把"这一发换成我"写进弓（弓的 shoot 读一次并清掉）。
        bow.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, custom -> custom.update(tag ->
                tag.putString(TAG_PENDING_SHOT, id.toString())));
    }

    @Override
    public void consumeResource(BowShootSkillContext context, Consumable consumable,
                                ISkillInstance<BowShootSkillContext> instance) {
        // 空实现：这三条技能的代价是那一发箭本身（无箭时付 NO_ARROW_COST），冷却无 —— 见类注释。
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
