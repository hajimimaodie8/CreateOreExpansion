package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * <b>着正电（{@code createoreexpansion:charged_positive}）</b> —— 不可用牛奶解除的负面效果。
 *
 * <p>形状照本模组既有的 {@code TransmutationDisorderEffect}（coe-charge 批 1 口径）：
 * {@code extends MobEffect} + {@code super(类别, 颜色)}。两处刻意的差别，都写在下面：</p>
 * <ul>
 *   <li>多了第三个构造参数（常驻粒子）：{@link ChargeConfigs#PARTICLE_MAIN}
 *       —— 作者要「类似电荷的粒子，不是药水粒子」（需求 §3.8），而 2 参构造器会把常驻粒子
 *       钉死成 {@code entity_effect}（原版药水/滞留粒子）；</li>
 *   <li>颜色 / 粒子都从 {@link ChargeConfigs} 取，<b>本文件零字面量</b>
 *       （需求 §5.1 #5：所有数值只有一个真源）。</li>
 * </ul>
 *
 * <p><b>批 1 的范围</b>：本类只做「注册 + 类别 + 配色 + 常驻粒子 + 扣血节奏判定」。
 * {@link #applyEffectTick(LivingEntity, int)} <b>当前是最小实现（不扣血）</b>，
 * 批 3 才接扣血 —— 见那里的 TODO。极性 {@link #polarity()} 供批 4（四条获得途径）、
 * 批 7（对外 API）、批 8（生物受场）取用。</p>
 *
 * <p>「不可用牛奶解除」<b>不在本类</b>：它走 {@code MobEffectEvent.Remove} 的取消
 * （批 2），而 {@code MobEffectEvent.Expired} <b>必须放行</b>（需求 §3.1 / §3.4 /
 * §5.3 陷阱 1）。</p>
 */
public class ChargedPositiveEffect extends MobEffect {

	public ChargedPositiveEffect() {
		super(MobEffectCategory.HARMFUL, ChargeConfigs.POSITIVE_COLOR, ChargeConfigs.PARTICLE_MAIN);
	}

	/** 本效果对应的电荷极性：正电（需求 §3.1 表：着正电 ⇒ {@code ChargePolarity.POSITIVE}）。 */
	public ChargePolarity polarity() {
		return ChargePolarity.POSITIVE;
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
	 * <p>⚠ 返回值必须是 {@code true}：{@code MobEffectInstance#tick} 把
	 * {@code applyEffectTick} 的 {@code false} 当成「效果结束」并把时长清零
	 * ⇒ 返回 false 会让这个 debuff 应用即消失。空实现也必须返回 true。</p>
	 *
	 * <p>TODO 批 3：照中毒扣血 —— {@code entity.hurt(<伤害来源>, ChargeConfigs.DAMAGE_PER_TICK);}
	 * 并且<b>刻意不照抄</b>原版中毒那道 {@code if (entity.getHealth() > 1.0F)} 保护：
	 * 作者要「扣到底了还是可以扣」（需求 §3.3 / §5.3 陷阱 3），那道保护正是中毒不致死的唯一原因。
	 * 伤害类型按作者 2026-10-03 裁定<b>不新建自定义 {@code DamageType}</b>（需求 §六 #4 被否决）
	 * —— 批 3 从原版 / NeoForge 现成的类型里挑，不在本模组新增注册项。</p>
	 */
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		// TODO 批 3：照中毒扣血（去掉 getHealth() > 1.0F 保护，能扣死）；本批刻意留空。
		return true;
	}
}
