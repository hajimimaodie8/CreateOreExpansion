package com.hjmmd_8.createoreexpansion.compat.jei.animation;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlock;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * JEI 分类动画：蓝宝石应力充能器（4/5 级超载/终极充能配方专用显示）。
 *
 * <p>仅替换机身方块状态与 shutter/轴 partial 为蓝宝石机型，动画节奏复用
 * {@link AnimatedJadeCharger}（蓄力收缩 → 弹出发射）。</p>
 */
public class AnimatedSapphireCharger extends AnimatedJadeCharger {

	@Override
	protected BlockState machineState() {
		return AllBlocks.SAPPHIRE_STRESS_CHARGER.getDefaultState()
			.setValue(SapphireStressChargerBlock.FACING, Direction.DOWN)
			.setValue(SapphireStressChargerBlock.MODE, mode);
	}

	@Override
	protected PartialModel axisModel() {
		return AllPartialModels.SAPPHIRE_CHARGER_AXIS;
	}

	@Override
	protected PartialModel shutterModel() {
		return AllPartialModels.SAPPHIRE_CHARGER_SHUTTER;
	}
}
