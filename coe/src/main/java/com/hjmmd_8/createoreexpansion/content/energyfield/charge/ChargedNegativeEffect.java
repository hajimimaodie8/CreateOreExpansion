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
 * {@link ChargePolarity#NEGATIVE}。扣血节奏、构造形状、批 1 的最小实现范围<b>完全相同</b>
 * （理由与 TODO 见 {@link ChargedPositiveEffect}，此处不重复）。</p>
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
	 * 需求 §3.3）。批 1 就接上：批 3 只需要在 {@link #applyEffectTick} 里补伤害那两行。
	 */
	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return ChargeConfigs.shouldApplyDamageThisTick(duration, amplifier);
	}

	/**
	 * 每次触发时的行为 —— <b>批 1 是最小实现：什么都不做</b>。
	 *
	 * <p>⚠ 返回值必须是 {@code true}（{@code false} 会被 {@code MobEffectInstance#tick}
	 * 当成效果结束 ⇒ 应用即消失），理由同 {@link ChargedPositiveEffect}。</p>
	 *
	 * <p>TODO 批 3：照中毒扣血、去掉 {@code getHealth() > 1.0F} 保护（能扣死）、
	 * 不新建自定义 {@code DamageType} —— 详见 {@link ChargedPositiveEffect#applyEffectTick} 的说明。</p>
	 */
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		// TODO 批 3：照中毒扣血（去掉 getHealth() > 1.0F 保护，能扣死）；本批刻意留空。
		return true;
	}
}
