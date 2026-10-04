package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.PerSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowCurseConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowDisarmConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.BowShootSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.ToIntFunction;

/**
 * 弓射击技能（新内核版）——{@code bow_curse}（凋零诅咒）与 {@code bow_disarm}（缴械风暴）共用。
 *
 * <h2>两段式怎么迁的</h2>
 * <p>弓技是两个阶段：<b>松手射击</b>（耗能 + 冷却 + 在弓上写"本次携带哪个技能/等级"的标记）
 * 与 <b>箭命中</b>（读标记、按等级取配置、执行效果）。这里<b>只迁第一段</b>：
 * 第二段仍然由旧的 {@code JadeTopazBowEventHandler} + 旧技能类的 {@code applyTo} 完成——
 * 因为标记里写的 id 依旧是 {@code createoreexpansion:bow_curse} / {@code bow_disarm}，
 * 旧注册表（{@code AllSkills}）里那两个条目我们一直没删，所以命中那一段<b>一行都不用动</b>。
 * 将来要把第二段也迁过来时，只需把 {@code applyTo} 的实现搬进新技能并改 handler。</p>
 *
 * <h2>与旧实现（BowCurseSkill / BowDisarmSkill 的 {@code release}）的差异</h2>
 * <ul>
 *   <li>去掉技能键判定（新路由只对按下的槽位调用）。</li>
 *   <li>耗能移到 {@link #consumeResource}；冷却判定两处同判（冷却中不耗能也不执行）。</li>
 *   <li>写标记与"进入冷却"仍留在 {@link #release}，顺序与旧实现一致
 *       （旧：tryConsume → startTicks → 写标记）。</li>
 * </ul>
 *
 * <h2>2026-10-03 弓技能批 1：三把弓继承这同一对技能之后，本类多担两件事</h2>
 * <ol>
 *   <li><b>等级的唯一读取点</b> = {@link #effectiveLevel} → 弓侧的
 *       {@code JadeTopazBowItem#effectiveSkillLevel(ItemStack)}（基准/上限都取档位表：
 *       起始 1/2/3/3、上限 5/3/3/3）。写进标记的等级、取配置的等级、算能量费的等级
 *       <b>是同一个值</b>；命中段只读箭上写好的那个等级
 *       —— 两段式（松手写 / 命中读）必须同改，改一边就是静默失效。</li>
 *   <li><b>冷却载体按弓分家</b> = {@link #perSkillCooldown(ItemStack)} → 档位表
 *       {@code BowTier#perSkillCooldown()}：翠玉之弓仍走<b>按物品记</b>
 *       （{@link ToolSkillCooldown} + {@link CoeSkillSupport#onCooldown}，与它升级前逐字相同）；
 *       三把继承弓走<b>按技能记</b>（{@link PerSkillCooldown}，同一把弓上两条技能互不连坐）。
 *       秒数两个分支同源（{@link CoeSkillSupport#cooldownTicks} ← 各等级的
 *       {@code BowCurseConfig#cooldownSeconds} / {@code BowDisarmConfig#cooldownSeconds}）。
 *       允许两条并存是作者明确裁定（第 6 条），边界就是那一个 switch。</li>
 * </ol>
 *
 * @param <C> 该弓技能的配置类型（两个技能各一份分级数值表）
 * @since 1.0.0
 */
public class BowShootItemSkill<C extends AutoSkillConfig> implements ItemSkill<BowShootSkillContext> {

    /** 凋零诅咒（槽位 0） */
    public static final BowShootItemSkill<BowCurseConfig> CURSE =
            new BowShootItemSkill<>(BowCurseConfig.class, c -> c.energyCost, c -> c.cooldownSeconds);

    /** 缴械风暴（槽位 1） */
    public static final BowShootItemSkill<BowDisarmConfig> DISARM =
            new BowShootItemSkill<>(BowDisarmConfig.class, c -> c.energyCost, c -> c.cooldownSeconds);

    private final Class<C> configType;
    private final ToIntFunction<C> energyCost;
    private final ToIntFunction<C> cooldownSeconds;

    private BowShootItemSkill(Class<C> configType, ToIntFunction<C> energyCost, ToIntFunction<C> cooldownSeconds) {
        this.configType = configType;
        this.energyCost = energyCost;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public void release(BowShootSkillContext context, ISkillInstance<BowShootSkillContext> instance) {
        Player player = context.getPlayer();
        ItemStack bow = context.bow();
        if (player == null || bow.isEmpty()) {
            return;
        }
        // 本技能实例的注册 id：冷却的"按技能记"分支与弓/箭上的技能标记都用它。
        ResourceLocation skillId = CoeSkillSupport.skillIdOf(instance);
        int level = effectiveLevel(bow, instance);
        C config = CoeSkillSupport.configForLevel(skillId, level, configType);
        if (config == null) {
            return;
        }
        // 冷却中：不执行（consumeResource 里同样跳过，不会白扣能量）
        if (onCooldown(player, bow, skillId)) {
            return;
        }

        int ticks = CoeSkillSupport.cooldownTicks(bow, cooldownSeconds.applyAsInt(config));
        if (ticks > 0) {
            startCooldown(player, bow, skillId, ticks);
        }

        // 标记本次射击携带的技能 id 与有效等级（发射时写进箭，命中时由旧 handler 按等级取配置生效）
        if (skillId == null) {
            return;
        }
        bow.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, custom -> custom.update(tag -> {
            tag.putString(JadeTopazBowItem.TAG_SKILL, skillId.toString());
            tag.putInt(JadeTopazBowItem.TAG_SKILL_LEVEL, level);
        }));
    }

    @Override
    public void consumeResource(BowShootSkillContext context, Consumable consumable,
                                ISkillInstance<BowShootSkillContext> instance) {
        Player player = context.getPlayer();
        ItemStack bow = context.bow();
        if (player == null || bow.isEmpty()) {
            return;
        }
        ResourceLocation skillId = CoeSkillSupport.skillIdOf(instance);
        int level = effectiveLevel(bow, instance);
        C config = CoeSkillSupport.configForLevel(skillId, level, configType);
        if (config == null) {
            return;
        }
        if (onCooldown(player, bow, skillId)) {
            return;
        }
        int cost = CoeSkillSupport.cost(bow, energyCost.applyAsInt(config), level);
        CoeSkillSupport.consume(player, bow, consumable, cost);
    }

    /**
     * 本次释放的<b>有效技能等级 —— 唯一读取点</b>（2026-10-03 弓技能批 1）。
     *
     * <p>弓侧：{@code JadeTopazBowItem#effectiveSkillLevel(ItemStack)}（基准与上限都取档位表
     * {@code BowTier#baseSkillLevel()} / {@code maxSkillLevel()}，形态照
     * {@code BoomerangItem#effectiveSkillLevel}）。标记用的等级、取配置用的等级、算能量费用
     * 的等级<b>必须是同一个值</b>（三处各算一遍就是"取配置用一档、扣费用另一档"的静默偏差）。</p>
     *
     * <p>非弓物品（本技能只被弓携带，这条是防御性回落）仍走内核口径
     * {@link CoeSkillSupport#effectiveLevel}：实例等级经注册表 {@code maxLevel} 钳位。</p>
     */
    private static int effectiveLevel(ItemStack bow, ISkillInstance<?> instance) {
        return bow.getItem() instanceof JadeTopazBowItem bowItem
                ? bowItem.effectiveSkillLevel(bow)
                : CoeSkillSupport.effectiveLevel(bow, instance);
    }

    /**
     * <b>本把弓的技能冷却走哪个载体</b>（作者 2026-10-03 弓技能批 1 第 6 条，边界在档位表
     * {@code BowTier#perSkillCooldown()}）：三把继承弓 = <b>按技能记</b>
     * （{@link PerSkillCooldown}，同一把弓上的凋零诅咒与缴械风暴各记各的，互不连坐）；
     * 翠玉之弓 = <b>按物品记</b>（{@link ToolSkillCooldown} —— 它的既有行为，红线不许动）。
     */
    private static boolean perSkillCooldown(ItemStack bow) {
        return bow.getItem() instanceof JadeTopazBowItem bowItem && bowItem.tier().perSkillCooldown();
    }

    /**
     * 是否正在冷却中（创造模式恒 false，与 {@link CoeSkillSupport#onCooldown} 同语义）。
     *
     * <p>判定与时长是分开的两件事（与旧实现一致）：这里只判"就绪没有"，
     * 按技能记用技能键、按物品记用 stack 上的组件（两种载体互不读取）。</p>
     */
    private static boolean onCooldown(Player player, ItemStack bow, ResourceLocation skillId) {
        if (player.isCreative()) {
            return false;
        }
        return perSkillCooldown(bow)
                ? !PerSkillCooldown.isReady(player, skillId)
                : CoeSkillSupport.onCooldown(player, bow);
    }

    /**
     * 进入冷却：两条载体各写自己那一格（写与显示都在各自载体的同一个入口里）。
     *
     * <p>⚠ 迅启附魔的减冷却（{@code ToolEnchantments#reduceCooldown}）在"按物品记"那条路上是
     * {@link ToolSkillCooldown#startTicks} 内部做的；{@link PerSkillCooldown#startTicks} 是
     * coe-pact 批 2 的纯新增载体、**不做**这一步（血契置换是固定的 30 秒）。四把弓都在
     * {@code createoreexpansion:skill_boostable} 标签里（经 {@code #skill_tools}）⇒ 迅启对弓可用，
     * 所以"按技能记"这一支在这里补上<b>同一条折扣</b>，否则三把继承弓会比翠玉之弓少一档缩减
     * （同两条技能、同一张数值表，只有起始等级不同 —— 写在报告里）。</p>
     */
    private static void startCooldown(Player player, ItemStack bow, ResourceLocation skillId, int ticks) {
        if (perSkillCooldown(bow)) {
            PerSkillCooldown.startTicks(player, skillId, ToolEnchantments.reduceCooldown(bow, ticks));
        } else {
            // 按物品记：减冷却在它内部自己应用（不要在这里再减一次）
            ToolSkillCooldown.startTicks(player, bow, ticks);
        }
    }
}
