package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.AllEntityTypes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波（唯一通用波实体）：翡翠/蓝宝石等所有应力充能器与差波器发射的都是同一种波，
 * 颜色按波等级统一（1~5：α=黄、β=绿、γ=蓝、ε=紫粉、ω=玫红，拖尾金），
 * 与机型无关。全部飞行/命中/加工逻辑在 {@link AbstractChargerWaveEntity}。
 */
public class ChargerWaveEntity extends AbstractChargerWaveEntity {

	public ChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	public ChargerWaveEntity(Level level, Vec3 pos, Vec3 movementDir, int waveLevel) {
		super(AllEntityTypes.CHARGER_WAVE.get(), level, pos, movementDir, waveLevel);
	}

	/**
	 * <b>带实体类型的通用构造</b>（2026-10-02 星界轮）：子类（{@code StarShockWaveEntity}）
	 * 需要沿用父类那套"出生点 + 方向 + 等级"的初始化，但必须把自己注册的
	 * {@code EntityType} 传下去（实体类型决定存档/同步的身份，不能借父类的）。
	 *
	 * @param type        本实体自己的注册类型（子类传 {@code AllEntityTypes.X.get()}）
	 * @param level       出生世界
	 * @param pos         出生位置
	 * @param movementDir 飞行方向
	 * @param waveLevel   波等级（1=α … 5=ω）
	 */
	protected ChargerWaveEntity(EntityType<?> type, Level level, Vec3 pos, Vec3 movementDir, int waveLevel) {
		super(type, level, pos, movementDir, waveLevel);
	}

	/**
	 * 差器均摊分发：创建同类型的降级子波（出口方向为任意向量——斜口出口沿 45° 对角）。
	 * 普通能量波不带载荷，故 index/total（载荷均摊份额）在这里用不上。
	 */
	@Override
	public AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level, int index, int total) {
		return new ChargerWaveEntity(level(), pos, dir, level);
	}
}
