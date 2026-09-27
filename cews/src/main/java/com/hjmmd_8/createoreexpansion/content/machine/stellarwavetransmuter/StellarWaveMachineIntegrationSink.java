package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import java.util.List;
import java.util.Set;

import com.hjmmd_8.createoreexpansion.compat.createaddition.TeslaCoilWaveCharger;
import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveMachineIntegrationPoints;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/**
 * <b>加工机联动（第二层 / CEWS）在第一层登记表里的实现</b>（W6-a）。
 *
 * <p>W6-a 之前，波引擎（第一层）直接调
 * {@link StellarWaveMachineIntegrations} / {@link StellarWaveMachineRegistry} /
 * {@link TeslaCoilWaveCharger} 的静态方法 —— 机械搬运后那是 {@code L1 → L2} 禁止边
 * （见 {@code build/patch/w6-restructure-PLAN.md} §2.2 的 E6–E12）。本类把那些调用
 * <b>原样</b>收到第二层里，并通过 {@link WaveMachineIntegrationPoints#install} 登记进
 * 第一层的表（方向 L2 → L1，合法）。</p>
 *
 * <p><b>行为零变化</b>：每个方法的方法体就是原先第一层调用点的那个表达式，
 * 被委托的两个门面（{@code StellarWaveMachineIntegrations} / {@code StellarWaveMachineRegistry}）
 * 一字未改；第一层调用点的 try/catch 也全部保留。</p>
 *
 * <p>登记点是 {@code CewsMod} 的构造器（第二层自己的 {@code @Mod} 入口），
 * 比任何一次波飞行都早，且 {@link WaveMachineIntegrationPoints#install} 幂等。</p>
 */
public final class StellarWaveMachineIntegrationSink implements WaveMachineIntegrationPoints.Sink {

	/** 唯一实例（无状态转发，登记一次即可）。 */
	public static final StellarWaveMachineIntegrationSink INSTANCE = new StellarWaveMachineIntegrationSink();

	private StellarWaveMachineIntegrationSink() {
	}

	@Override
	public void ensureRegistered() {
		StellarWaveMachineIntegrations.ensureRegistered();
	}

	@Override
	public int consumeTeslaCoil(Level level, BlockPos pos, int max) {
		return StellarWaveMachineIntegrations.consumeTeslaCoil(level, pos, max);
	}

	@Override
	public int maxStrikeChargingEnergyFe(Level level) {
		return StellarWaveMachineIntegrations.maxStrikeChargingEnergyFe(level);
	}

	@Override
	public int teslaCoilEnergy(Level level, BlockPos pos) {
		return StellarWaveMachineIntegrations.teslaCoilEnergy(level, pos);
	}

	@Override
	public int recipeEnergyRequired(Recipe<?> recipe) {
		return StellarWaveMachineIntegrations.recipeEnergyRequired(recipe);
	}

	@Override
	public int recipeSpeedMode(Recipe<?> recipe) {
		return StellarWaveMachineIntegrations.recipeSpeedMode(recipe);
	}

	@Override
	public int speedModeFor(float rpm) {
		return StellarWaveMachineIntegrations.speedModeFor(rpm);
	}

	@Override
	public List<IRecipeTypeInfo> energyExtraRecipeTypes(boolean payloadHasEnergy) {
		return StellarWaveMachineIntegrations.energyExtraRecipeTypes(payloadHasEnergy);
	}

	@Override
	public Set<ResourceLocation> strikeHandledTypeIds() {
		return StellarWaveMachineIntegrations.strikeHandledTypeIds();
	}

	@Override
	public List<IRecipeTypeInfo> typesFor(ResourceLocation machineId) {
		return StellarWaveMachineRegistry.typesFor(machineId);
	}

	@Override
	public List<IRecipeTypeInfo> allRecipeTypes() {
		return StellarWaveMachineRegistry.allRecipeTypes();
	}

	@Override
	public boolean isMachinery(Level level, BlockPos pos) {
		return StellarWaveMachineRegistry.isMachinery(level, pos);
	}

	@Override
	public void chargeNearbyCoil(AbstractChargerWaveEntity wave) {
		TeslaCoilWaveCharger.chargeNearbyCoil(wave);
	}
}
