package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 星辉石能量调级器方块实体：面板/啮合全部继承 {@link AbstractWaveGateBlockEntity}，
 * 仅覆写机型参数：32 RPM 起调制、最大可把波提升至 5 级（欧米伽，终极充能态），
 * 且<b>单次提升级数由本机转速决定</b>（32~128 RPM → +1；129~256 RPM → +2）。
 *
 * <p>提升级数通过 {@link #getBoostStepForSpeed()} 提供给波实体应用端
 * （{@code WaveMachineActions} + {@code AbstractChargerWaveEntity} 的延迟升级），
 * 最终等级在波侧封顶于 {@link WaveLevels#MAX_LEVEL}（5），不会超出。</p>
 */
public class StellarstoneWaveRegulatorBlockEntity extends AbstractWaveGateBlockEntity {

	public StellarstoneWaveRegulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 星辉石调级器：32 RPM 起调制（蓝宝石为 64、翡翠为 FAST 100）。 */
	@Override
	public float getModulationSpeedThreshold() {
		return 32f;
	}

	/** 转速是否达标（≥32 RPM）：覆写 Create 的 FAST 门槛判定，供护目镜/渲染使用。 */
	@Override
	public boolean isSpeedRequirementFulfilled() {
		return Math.abs(getSpeed()) >= getModulationSpeedThreshold();
	}

	/**
	 * 星辉石调级器：单次提升级数随本机转速（调制门槛 32 RPM 之上）——
	 * <ul>
	 *   <li>32 ~ 128 RPM（含 128）→ 每次穿过提升 <b>1</b> 级；</li>
	 *   <li>129 ~ 256 RPM（含以上）→ 每次穿过提升 <b>2</b> 级。</li>
	 * </ul>
	 * 翡翠/蓝宝石调级器不覆写本方法（默认恒 1）。
	 */
	@Override
	public int getBoostStepForSpeed() {
		return Math.abs(getSpeed()) >= 129f ? 2 : 1;
	}

	/** 星辉石调级器：最大可升等级 5（欧米伽）——提升封顶到 5（全局上限）。 */
	@Override
	public int getMaxBoostLevel() {
		return WaveLevels.MAX_LEVEL;
	}

	/** 星辉石调级器：可承载/输出 1~5 级波（不受翡翠 3 级限制）。 */
	@Override
	public int getMaxSupportedWaveLevel() {
		return WaveLevels.MAX_LEVEL;
	}

	/** 护目镜标题（基类主干输出）：星辉石能量调级器。 */
	@Override
	protected Component getGoggleTitle() {
		return Component.translatable("createoreexpansion.goggles.stellarstone_wave_regulator");
	}

	/**
	 * 当前转速对应的单次提升级数（夹到 1~2，供护目镜直接显示；转速不达标时无意义）。
	 * 仅在 {@link #isSpeedRequirementFulfilled()} 为 true 时调用。
	 */
	public int getDisplayBoostStep() {
		return Mth.clamp(getBoostStepForSpeed(), 1, 2);
	}

	/** 护目镜：星辉石调级器标题（基类主干）+ 按本机转速显示单次提升级数（+1 / +2）。 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		// 未接入应力/转速不足（< 32 RPM）：无调制（需求提示已由基类给出）
		if (!isSpeedRequirementFulfilled())
			return added;

		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellarstone_wave_boost",
			getDisplayBoostStep())
			.withStyle(ChatFormatting.AQUA));
		return added;
	}
}
