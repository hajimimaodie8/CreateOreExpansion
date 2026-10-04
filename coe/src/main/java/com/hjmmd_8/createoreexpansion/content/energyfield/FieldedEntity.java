package com.hjmmd_8.createoreexpansion.content.energyfield;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;

import net.minecraft.world.phys.Vec3;

/**
 * "可被能量场作用的带电实体"统一接口。
 *
 * <p>任何需要受加速场/偏转场影响的实体（能量波、粒子状实体、未来生物/弹射物）
 * 实现本接口，由场应用器在每 tick 读取位置/速度/电荷并调用
 * {@link EnergyField#apply(Vec3, ChargePolarity, double)} 修正运动。</p>
 *
 * <p>实现约定：{@link #fieldVelocity()} 返回实体<b>当前运动速度（格/秒）</b>；
 * {@link #setFieldVelocity(Vec3)} 写入修正后的速度。波实体把"场速"与内部 tick 位移挂钩。</p>
 *
 * <p><b>★ 受场强度缩放（coe-charge 批 8）：契约只做加法</b>。新增的
 * {@link #fieldStrengthScale()} 是一个<b>默认方法</b>，默认值是<b>中性</b>的
 * {@link ChargeConfigs#NEUTRAL_FIELD_STRENGTH_SCALE}（= 1.0）—— 也就是本接口此前隐含的
 * 行为（场应用器过去在调用点硬写 1.0）。于是：
 * <ul>
 *   <li><b>既有实现者行为一字不变</b>：今天唯一的实现者是波实体
 *       {@code AbstractChargerWaveEntity}，它不覆写这个方法 ⇒ 走的仍是 1.0，
 *       与批 8 之前 {@code EnergyFields#applyFields} 传的字面量 1.0 完全等价
 *       （"加一个默认值"是纯新增，没有任何既有语义被改）；</li>
 *   <li><b>只有需要缩放的实现者才覆写</b>：受场的<b>生物</b>由
 *       {@code EntityFieldBridge} 的窄句柄覆写成
 *       {@link ChargeConfigs#LIVING_FIELD_STRENGTH_SCALE}（= 0.05，需求 §六 #9「比波小」）。</li>
 * </ul>
 * 因此这个默认值<b>不能</b>取 0：那会让所有既有实现者当场失去场作用（静默的玩法改动）。</p>
 */
public interface FieldedEntity {

	/** 实体所在 Level 的维度 key 字符串（"minecraft:overworld" 等），用于匹配场注册表。 */
	String fieldLevelKey();

	/** 实体当前位置（世界坐标）。 */
	Vec3 fieldPosition();

	/** 实体当前运动速度（格/秒，方向=运动方向；可为 0）。 */
	Vec3 fieldVelocity();

	/** 写入修正后的运动速度（格/秒）。 */
	void setFieldVelocity(Vec3 velocity);

	/** 当前电荷极性；返回 null = 不带电（场对其无效）。 */
	ChargePolarity getChargePolarity();

	/**
	 * 本实体受能量场作用时的<b>强度缩放</b>（{@code 1.0} = 原强度 = 中性；见类注释）。
	 *
	 * <p><b>默认方法、默认中性</b>（coe-charge 批 8 新增）：默认值 = 数值真源里的
	 * {@link ChargeConfigs#NEUTRAL_FIELD_STRENGTH_SCALE}（= <b>1.0</b>）。取 1.0 的依据是
	 * <b>代码现状</b>：批 8 之前唯一调用点 {@code EnergyFields#applyFields} 在它的两处
	 * {@code field.apply(..)} 上写的就是字面量 {@code 1.0} —— 也就是"本接口的实现者一律满强度"。
	 * 默认方法返回同一个值 ⇒ 所有既有实现者行为不变（关卡里有断言钉住"是默认方法 + 中性默认值"，
	 * 还有一条负向钉住它<b>不得</b>退化成抽象方法、默认值<b>不得</b>是 0）。</p>
	 *
	 * <p>本方法只影响"场对本实体的力度"，<b>不影响</b>场的存在与否、方向与极性
	 * （那三件事仍由 {@link #getChargePolarity()} 与 {@link EnergyField} 决定）：
	 * 返回 0 仍会走完整条场链，只是这一 tick 的增量被缩为 0。</p>
	 *
	 * @return 强度缩放（0 = 完全不受力，1 = 原强度）；单位无量纲
	 */
	default double fieldStrengthScale() {
		return ChargeConfigs.NEUTRAL_FIELD_STRENGTH_SCALE;
	}
}
