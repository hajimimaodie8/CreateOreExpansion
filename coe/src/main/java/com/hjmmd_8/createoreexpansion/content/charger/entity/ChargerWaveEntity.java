package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.AllEntityTypes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波（唯一通用波实体）：翡翠/蓝宝石等所有应力充能器与差波器发射的都是同一种波，
 * 颜色按波等级统一（1~5：α=黄、β=绿、γ=蓝、ε=紫粉、ω=玫红，拖尾金），
 * 与机型无关。全部飞行/命中/加工逻辑在 {@link AbstractChargerWaveEntity}。
 *
 * <p><b>本类不再有"带实体类型的通用构造"</b>（2026-10-02 删除）：那一版构造是给自造的
 * {@code StarShockWaveEntity} 子类用的，而那个子类（连同它的实体类型 {@code star_shock_wave}）
 * 因为没注册渲染器导致客户端 NPE，已按作者口径整体删除 —— 技能发波改用<b>本类</b>、
 * 靠设置波的五要素（波速 / 波级 / 波载荷 / 波型 / 剩余寿命）实现，
 * 见 {@code content/equipment/armor/StarShockRuntime}。</p>
 */
public class ChargerWaveEntity extends AbstractChargerWaveEntity {

	public ChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	public ChargerWaveEntity(Level level, Vec3 pos, Vec3 movementDir, int waveLevel) {
		super(AllEntityTypes.CHARGER_WAVE.get(), level, pos, movementDir, waveLevel);
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
