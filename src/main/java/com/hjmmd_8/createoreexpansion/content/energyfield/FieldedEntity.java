package com.hjmmd_8.createoreexpansion.content.energyfield;

import net.minecraft.world.phys.Vec3;

/**
 * "可被能量场作用的带电实体"统一接口。
 *
 * <p>任何需要受加速场/偏转场影响的实体（能量波、粒子状实体、未来生物/弹射物）
 * 实现本接口，由场应用器在每 tick 读取位置/速度/电荷并调用
 * {@link EnergyField#apply(Vec3, ChargePolarity, double)} 修正运动。</p>
 *
 * <p>实现约定：{@link #getFieldVelocity()} 返回实体<b>当前运动速度（格/秒）</b>；
 * {@link #setFieldVelocity(Vec3)} 写入修正后的速度。波实体把"场速"与内部 tick 位移挂钩。</p>
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
}
