package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowCurseConfig;
import com.hjmmd_8.createoreexpansion.content.skill.context.BowShootContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.AbstractSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 翠玉之弓技能 A —— 凋零诅咒（一技能多等级，数值统一在 BowCurseConfigs修改）。
 *
 * <p>射击时释放（BowShootContext）：按有效等级加载配置并消耗能量 + 进入冷却，
 * 在弓上标记本次射击携带的技能 id 与等级；箭命中目标后由
 * {@code JadeTopazBowEventHandler} 读取标记、按等级取配置调用 {@link #applyTo} 生效。</p>
 *
 * <p>效果：目标获得凋零 + 缓慢（按等级概率升级为 II 级），持续按等级随机；
 * 同时生成药水云，半径与缓慢等级按配置。</p>
 */
public class BowCurseSkill extends AbstractSkill implements ConfigSkill<BowShootContext, BowCurseConfig> {

	private int energyCost;
	private int cooldownSeconds;
	private int durationTicks;
	private int durationVariance;
	private float upgradeChance;
	private float cloudRadius;
	private int cloudSlowAmplifier;
	private DataSkill data;

	public BowCurseSkill() {
		super();
	}

	@Override
	public SkillType getType() {
		return SkillType.USE_SKILL;
	}

	@Override
	public int getCost() {
		return energyCost;
	}

	@Override
	public int getCooldownSeconds() {
		return cooldownSeconds;
	}

	@Override
	public void load(BowCurseConfig config, DataSkill data) {
		this.energyCost = config.energyCost;
		this.cooldownSeconds = config.cooldownSeconds;
		this.durationTicks = config.durationTicks;
		this.durationVariance = config.durationVariance;
		this.upgradeChance = config.upgradeChance;
		this.cloudRadius = config.cloudRadius;
		this.cloudSlowAmplifier = config.cloudSlowAmplifier;
		this.data = data;
	}

	@Override
	public Class<BowCurseConfig> getConfigType() {
		return BowCurseConfig.class;
	}

	@Override
	public void release(BowShootContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null)
			return;

		// 真正生效前消耗能量；不足则放弃（提示由 ToolEnergy 统一发送）
		if (!ToolEnergy.tryConsume(player, ctx.bow(), this))
			return;
		ToolSkillCooldown.startTicks(player, ctx.bow(), cooldownSeconds * 20);

		// 标记本次射击携带的技能 id 与有效等级（发射时写入箭，命中时按等级取配置）
		ctx.bow().update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
			net.minecraft.world.item.component.CustomData.EMPTY,
			custom -> custom.update(tag -> {
				tag.putString(JadeTopazBowItem.TAG_SKILL, AllSkills.getId(this).toString());
				tag.putInt(JadeTopazBowItem.TAG_SKILL_LEVEL, effectiveLevel(ctx.bow()));
			}));
	}

	/** 本次射击携带的技能有效等级（含技能提升附魔；无数据时按 1 级） */
	private int effectiveLevel(ItemStack bow) {
		return data != null ? SkillEnergyCost.effectiveLevel(bow, data) : 1;
	}

	/** 箭命中目标后调用：按等级配置附加凋零 + 缓慢 + 药水云 */
	public static void applyTo(Player player, LivingEntity target, BowCurseConfig config) {
		boolean upgraded = target.level().getRandom().nextFloat() < config.upgradeChance;
		int duration = config.durationTicks + target.level().getRandom().nextInt(config.durationVariance + 1);
		int amplifier = upgraded ? 1 : 0;

		target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));

		AreaEffectCloud cloud = new AreaEffectCloud(target.level(), target.getX(), target.getY(), target.getZ());
		cloud.setRadius(config.cloudRadius);
		cloud.setWaitTime(10);
		cloud.setDuration(duration);
		cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, config.cloudSlowAmplifier));
		target.level().addFreshEntity(cloud);
	}
}
