package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergyDraw;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.family.WaveRecipeFamilies;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

/**
 * <b>变体波的一次"命中候选"</b>（2026-09 从 {@code StellarWaveEntity} 抽出成独立类型）。
 *
 * <p>内容：配方数据包 id（批次锁定的稳定标识）+ 配方 + <b>三类资源引用</b>
 * （{@link AuxRef} 辅料 / {@link FluidRef} 流体 / {@link EnergyDraw} 电量，见 {@link WaveResources}）
 * ——三者同构，都是"就近来源优先、波载荷兜底、按来源扣减"。</p>
 *
 * <p><b>辅料引用列表</b>按 ingredient 升序压缩存储：{@code auxes.get(k)} 即
 * {@code recipe.getIngredients().get(k+1)} 所用的那件辅料（主料 = 命中物品）。空列表 = 单输入无辅料。</p>
 *
 * <p><b>族配方归属</b>：{@code familyRecipe} 非空表示本候选来自"步骤族"路径（序列装配）——
 * {@code recipe} 是该装配的<b>下一步成品配方</b>（用于辅料/流体/电量门槛），产物推导与排序口径
 * 仍按族配方（{@link WaveRecipeFamilies}）走。</p>
 *
 * <p><b>形态说明</b>：字段 {@code public final} + 同名访问器（不提供 setter）——原为波实体内的私有
 * 嵌套 record，调用点用字段式访问；抽成独立类型后字段转私有会让 40+ 处调用点编译不过，故保留
 * "不可变数据载体"语义的同时零改动既有调用点。纯数据、不持世界引用，可安全跨 tick 保存在候选里。</p>
 */
public final class Candidate {

	public final ResourceLocation id;
	public final Recipe<?> recipe;
	public final List<AuxRef> auxes;
	public final List<FluidRef> fluids;
	public final List<EnergyDraw> energies;
	public final Recipe<?> familyRecipe;

	public Candidate(ResourceLocation id, Recipe<?> recipe, List<AuxRef> auxes, List<FluidRef> fluids,
		List<EnergyDraw> energies, Recipe<?> familyRecipe) {
		this.id = id;
		this.recipe = recipe;
		this.auxes = auxes;
		this.fluids = fluids;
		this.energies = energies;
		this.familyRecipe = familyRecipe;
	}

	public ResourceLocation id() {
		return id;
	}

	public Recipe<?> recipe() {
		return recipe;
	}

	public List<AuxRef> auxes() {
		return auxes;
	}

	public List<FluidRef> fluids() {
		return fluids;
	}

	public List<EnergyDraw> energies() {
		return energies;
	}

	/** 产物推导应走的"族配方"：步骤族候选返回装配配方，普通候选返回自身。 */
	public Recipe<?> familyTarget() {
		return familyRecipe == null ? recipe : familyRecipe;
	}

	/** 首个辅料引用（"辅料即产物来源"的变形升级 / 锻造融合推导入口；无辅料返回 null）。 */
	public AuxRef firstAux() {
		return auxes.isEmpty() ? null : auxes.get(0);
	}

	/** 合计电量需求（FE；0 = 该配方不耗电）。 */
	public int energyRequired() {
		return WaveResources.totalEnergy(energies);
	}
}
