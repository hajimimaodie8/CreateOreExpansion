package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveAttrKey;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveAttrs;

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
 *
 * <h2>2026-10-02 批 4：<b>自定义伤害</b>要素（作者裁定 D9 = A；需求 §3.6 第 3 条）</h2>
 * <p>环绕技能（回旋镖）的单枚伤害是 {@code 2 × 技能等级}，而既有波的伤害<b>只由波级决定</b>
 * （{@code WaveLevels.damage(波级)} = 4/6/8/10/12，见基类的 {@code getDamage()}）。
 * 需求 §3.6 明确"环绕波不另写一套"（复用既有实体）⇒ 作者裁定<b>把伤害做成可覆写</b>：
 * 本类多一个<b>可选、默认关闭</b>的"自定义伤害"要素。</p>
 * <ul>
 *   <li>字段默认 {@code -1} = <b>没设</b> ⇒ {@link #getDamage()} 走 {@code super.getDamage()}
 *       = 既有波级表 ⇒ <b>一切既有波（充能器波 / 变器波 / 星芒嬗震的波与环绕波）
 *       伤害逐字不变</b>；</li>
 *   <li>设了正值 ⇒ 该值就是本波的命中伤害（回旋镖的环绕波设成 {@code 2 × 等级}）；</li>
 *   <li>波级仍然决定观感（颜色 / 粒子 / 拖尾风格），<b>不</b>再决定伤害。</li>
 * </ul>
 *
 * <p><b>2026-10-24 批 3（波属性收进 {@code WaveState}）</b>：本类不再声明字段、不再写 NBT ——
 * 自定义伤害是 {@code WaveAttrs.CUSTOM_DAMAGE} 的一行，键名 / Float 类型 / "&gt; 0 才写" /
 * "缺键读作 -1" 全部由表声明（作用域 {@link WaveAttrKey.Scope#CHARGER}）。读档/存档走基类的
 * {@code WaveStateNbt} 表遍历，本类一个字都不用写。</p>
 */
public class ChargerWaveEntity extends AbstractChargerWaveEntity {

	/** 本实体种类追加的 NBT 键范围（{@code CustomDamage} 一行）。 */
	@Override
	protected WaveAttrKey.Scope waveScope() {
		return WaveAttrKey.Scope.CHARGER;
	}

	public ChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	public ChargerWaveEntity(Level level, Vec3 pos, Vec3 movementDir, int waveLevel) {
		super(AllEntityTypes.CHARGER_WAVE.get(), level, pos, movementDir, waveLevel);
	}

	/**
	 * 设置<b>自定义伤害要素</b>（可选；不调用 = 命中伤害仍由波级表决定）。
	 *
	 * <p>只接受正值：{@code <= 0} 一律视为"没设"（回落波级表），
	 * 于是"忘了设"与"明确设成 0"不会变成"打不疼"这种静默差异。</p>
	 */
	public void setCustomDamage(float customDamage) {
		waveState().set(WaveAttrs.CUSTOM_DAMAGE, customDamage);
	}

	/** 自定义伤害（{@code <= 0} = 要素未设，伤害走波级表）。 */
	public float getCustomDamage() {
		return waveState().get(WaveAttrs.CUSTOM_DAMAGE);
	}

	/**
	 * <b>命中伤害</b>：设了自定义要素就用它，否则<b>逐字</b>沿用既有波级表
	 * （{@link AbstractChargerWaveEntity} 里那一行 {@code WaveLevels.damage(waveLevel)}）。
	 *
	 * <p>覆写点只有本类一处 ⇒ "默认路径不变"与"可覆写"同时成立，不需要在基类里多一个字段。</p>
	 */
	@Override
	protected float getDamage() {
		float customDamage = getCustomDamage();
		return customDamage > 0.0F ? customDamage : super.getDamage();
	}

	/**
	 * 差器均摊分发：创建同类型的降级子波（出口方向为任意向量——斜口出口沿 45° 对角）。
	 * 普通能量波不带载荷，故 index/total（载荷均摊份额）在这里用不上。
	 *
	 * <p><b>2026-10-24 批 5（作者裁定 D5）</b>：本类现在只回答"造哪种空壳"——属性继承一律由基类
	 * {@link AbstractChargerWaveEntity#createChildWave} 的模板完成（整份 {@code WaveState} 拷贝，
	 * 见 {@code inheritForChild}）。因此<b>自定义伤害要素现在会继承给子波</b>（旧注释写的是"刻意不继承"）：
	 * 子波等级 = 父波 - 降级量，伤害取父波的自定义值。</p>
	 *
	 * <p>⚠ 对唯一会设自定义伤害的场景（回旋镖环绕波，值 = {@code 2 × 等级}）两者恰好相等：
	 * 子波等级 {@code L-1} 的等级表伤害 = {@code 2(L-1)+2 = 2L} = 父波的自定义值 ⇒ 绝对值逐值不变。
	 * 其余场景按"完整拷贝"口径走（这是 D5 的字面含义，已列入行为变化清单）。</p>
	 */
	@Override
	protected AbstractChargerWaveEntity newChildWave(Vec3 pos, Vec3 dir, int level) {
		return new ChargerWaveEntity(level(), pos, dir, level);
	}
}
