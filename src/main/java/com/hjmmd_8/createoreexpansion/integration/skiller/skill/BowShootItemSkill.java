package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowCurseConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowDisarmConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
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
        C config = CoeSkillSupport.configForLevel(bow, instance, configType);
        if (config == null) {
            return;
        }
        // 冷却中：不执行（consumeResource 里同样跳过，不会白扣能量）
        if (CoeSkillSupport.onCooldown(player, bow)) {
            return;
        }

        int ticks = CoeSkillSupport.cooldownTicks(bow, cooldownSeconds.applyAsInt(config));
        if (ticks > 0) {
            ToolSkillCooldown.startTicks(player, bow, ticks);
        }

        // 标记本次射击携带的技能 id 与有效等级（发射时写进箭，命中时由旧 handler 按等级取配置生效）
        ResourceLocation skillId = CoeSkillSupport.skillIdOf(instance);
        if (skillId == null) {
            return;
        }
        int level = CoeSkillSupport.effectiveLevel(bow, instance);
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
        C config = CoeSkillSupport.configForLevel(bow, instance, configType);
        if (config == null) {
            return;
        }
        if (CoeSkillSupport.onCooldown(player, bow)) {
            return;
        }
        int cost = CoeSkillSupport.cost(bow, energyCost.applyAsInt(config),
                CoeSkillSupport.effectiveLevel(bow, instance));
        CoeSkillSupport.consume(player, bow, consumable, cost);
    }
}
