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
	 * 需求 §3.3）：{@code int i = 25 >> amplifier; return i > 0 ? duration % i == 0 : true;}
	 * —— 实现与常量都在 {@link ChargeConfigs#shouldApplyDamageThisTick}（本文件零字面量）。
	 *
	 * <p>⚠ <b>{@code duration % i} 的陷阱</b>：{@code duration} 是「效果还剩多少 tick」。
	 * 若将来出现<b>每 tick 刷新</b>这个效果的来源（把时长反复重置回满值），
	 * {@code duration} 会被钉在同一个数上，{@code duration % i} 要么恒真要么恒假 ——
	 * 扣血会变成「每 tick 都扣」或「永远不扣」，而且没有任何报错。
	 * 本批<b>没有</b>这样的刷新源（四条获得途径的落地在批 4 / 批 5），故照抄原版即可；
	 * 将来加「持续刷新的场」时必须回来重新评估这一段。</p>
	 */
	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return ChargeConfigs.shouldApplyDamageThisTick(duration, amplifier);
	}

	/**
	 * 每次触发时的行为 —— <b>批 3：照原版中毒扣血，只删掉那道「不致死」保护</b>（需求 §3.3 / §5.3 陷阱 3）。
	 *
	 * <p>原版中毒 {@code PoisonMobEffect#applyEffectTick} 的全部内容就是
	 * {@code if (entity.getHealth() > 1.0F) entity.hurt(<伤害>, 1.0F);} 加一个 {@code return true;}。
	 * 本实现<b>唯一的差别</b>是把那道 {@code getHealth() > 1.0F} 的门整条删掉：
	 * 它正是原版中毒不会打死人的唯一原因，而作者要「扣到底了还是可以扣」（能扣死）。
	 * 「是否扣血」的节奏由 {@link #shouldApplyEffectTickThisTick} 决定，
	 * 「扣多少」由 {@link ChargeConfigs#DAMAGE_PER_TICK} 决定 —— 本文件依旧零字面量。</p>
	 *
	 * <p>⚠ 返回值必须<b>恒为 {@code true}</b>：{@code MobEffectInstance#tick} 把
	 * {@code applyEffectTick} 的 {@code false} 当成「效果结束」并把时长清零
	 * ⇒ 返回 false 会让这个 debuff 应用即消失（扣血与消失同时发生，症状是「挂上就没」）。</p>
	 *
	 * <p><b>伤害类型</b>：{@link ChargeConfigs#CHARGE_DAMAGE_TYPE}
	 * （{@code createoreexpansion:charge}）—— <b>作者 2026-10-03 裁定（批 6 执行期间改判）</b>：
	 * 按开工需求 §六 #4 的原文新建自定义伤害类型（便于区分与免疫）。批 3 当时按作者前一版
	 * 裁定走的 {@code damageSources().magic()}（与既有嬗乱效果同口径）<b>已被覆盖</b>；
	 * 现在两个效果与中和爆炸共用<b>同一个</b>键，数据包文件见那个常量的 javadoc。
	 * ⚠ 旧那条「电荷文件不得出现自定义伤害类型」的关卡负向断言已<b>反向改口径</b>
	 * （现在是「必须恰好有这一个」，见 {@code charge-one-custom-damage-type}）。</p>
	 *
	 * <p><b>无敌帧（刻意照原版，不清 {@code invulnerableTime}）</b>：{@code LivingEntity#hurt}
	 * 会占掉受击冷却（{@code invulnerableTime = 20}）。原版中毒同样不清，照抄即同观感：
	 * 1.21 的伤害只有在 {@code invulnerableTime > 10} <b>且</b>本次伤害 ≤ 上次伤害时才被吞掉
	 * —— 扣血间隔 Lv1..Lv4 = 25/12/6/3 tick，到下一次扣血时冷却早已 < 10，故每一下都实打实生效；
	 * 只有 Lv5（间隔 1 tick）会出现「冷却期内等额伤害被吞、实际约每 2 tick 掉 1 点」，
	 * 这正是原版中毒 Lv5 的既有行为。另一面：被扣血的那一瞬间如果恰好挨了别的伤害，
	 * 双方会按 {@code hurt} 的同帧规则竞争（magic 不带 {@code BYPASSES_COOLDOWN}），
	 * 观感上就是「电荷把那一下吃掉了」—— 原版中毒一模一样，本批刻意不修。</p>
	 */
	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		// 照 PoisonMobEffect#applyEffectTick，只删除 if (entity.getHealth() > 1.0F) 那道门：
		// 中毒靠它不致死，作者要能扣死。伤害类型 = ChargeConfigs.CHARGE_DAMAGE_TYPE
		// （作者 2026-10-03 改判：按需求 §六 #4 原文新建自定义类型；两个效果与中和爆炸共用它）；
		// invulnerableTime 照原版不清（理由见 javadoc）。
		entity.hurt(entity.damageSources().source(ChargeConfigs.CHARGE_DAMAGE_TYPE),
			ChargeConfigs.DAMAGE_PER_TICK);
		return true;
	}
}
