package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * <b>着负电（{@code createoreexpansion:charged_negative}）</b> —— 不可用牛奶解除的负面效果。
 *
 * <p>与 {@link ChargedPositiveEffect} <b>逐条对称</b>，只有两处取值不同：
 * 配色 {@link ChargeConfigs#NEGATIVE_COLOR}（蓝）与极性
 * {@link ChargePolarity#NEGATIVE}。构造形状、扣血节奏、
 * {@link #applyEffectTick(LivingEntity, int)} 的扣血实现与<b>全部</b>口径（伤害类型
 * {@code damageSources().magic()}、恒返回 {@code true}、照原版不清 {@code invulnerableTime}、
 * {@code getHealth() > 1.0F} 那道门被刻意删除）<b>完全相同</b> —— 理由见
 * {@link ChargedPositiveEffect} 的逐段说明，此处不重复。</p>
 */
public class ChargedNegativeEffect extends MobEffect {

	public ChargedNegativeEffect() {
		super(MobEffectCategory.HARMFUL, ChargeConfigs.NEGATIVE_COLOR, ChargeConfigs.PARTICLE_MAIN);
	}

	/** 本效果对应的电荷极性：负电（需求 §3.1 表：着负电 ⇒ {@code ChargePolarity.NEGATIVE}）。 */
	public ChargePolarity polarity() {
		return ChargePolarity.NEGATIVE;
	}

	/**
	 * 扣血节奏判定（照原版中毒 {@code PoisonMobEffect#shouldApplyEffectTickThisTick}，
	 * 需求 §3.3）：{@code int i = 25 >> amplifier; return i > 0 ? duration % i == 0 : true;}
	 * —— 实现与常量都在 {@link ChargeConfigs#shouldApplyDamageThisTick}（本文件零字面量）。
	 * ⚠ {@code duration % i} 的陷阱（被每 tick 刷新时节奏会失真）见
	 * {@link ChargedPositiveEffect#shouldApplyEffectTickThisTick}。</p>
	 */
	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return ChargeConfigs.shouldApplyDamageThisTick(duration, amplifier);
	}

	/**
	 * 每次触发时的行为 —— <b>批 3：与 {@link ChargedPositiveEffect} 逐字同形</b>
	 * （照原版中毒扣血、删掉 {@code getHealth() > 1.0F} 那道「不致死」保护 ⇒ 能扣死）。
	 * 伤害类型同取 {@link net.minecraft.world.entity.Entity#damageSources()}{@code .magic()}
	 * （不新增自定义 {@code DamageType}，与既有嬗乱效果同口径），
	 * {@code invulnerableTime} 照原版不清 —— 三条理由与观感后果都写在
	 * {@link ChargedPositiveEffect#applyEffectTick} 的 javadoc 里，此处不重复。
	 *
	 * <p>⚠ 返回值必须<b>恒为 {@code true}</b>（{@code false} 会被 {@code MobEffectInstance#tick}
	 * 当成效果结束 ⇒ 应用即消失）。</p>
	 */
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		// 照 PoisonMobEffect#applyEffectTick，只删除 if (entity.getHealth() > 1.0F) 那道门；
		// 伤害类型与既有嬗乱效果同口径，不新增自定义 DamageType。
		entity.hurt(entity.damageSources().magic(), ChargeConfigs.DAMAGE_PER_TICK);
		return true;
	}
}
