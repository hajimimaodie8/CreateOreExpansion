package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * 动力角磨床配方过滤器槽定位：位于机器前上方（z 前端表面），物品平放显示。
 */
public class GrinderFilterSlot extends ValueBoxTransform {

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		if (state.getValue(PowerAngleGrinderBlock.OPEN)) {
			// 开盖：槽随盖旋转后的位置（随水平朝向旋转）
			return rotateHorizontally(state, VecHelper.voxelSpace(8, 15.86434f, -2.77406f));
		}
		// 关盖：机器前上方，贴近顶部（随水平朝向旋转）
		return rotateHorizontally(state, VecHelper.voxelSpace(8, 15.5f, 3));
	}

	@Override
	public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
		if (state.getValue(PowerAngleGrinderBlock.OPEN)) {
			// 开盖：先绕世界水平轴翻 -22.5°（盖的翻转轴，符号随 facing 正负），再平放（绕世界 x 90°）
			Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			if (facing.getAxis() == Direction.Axis.X) {
				// facing 沿东西：铰链沿南北（世界 z），east 与 north 同向
				float sign = facing == Direction.EAST ? 22.5f : -22.5f;
				ms.mulPose(Axis.ZP.rotationDegrees(sign));
			} else {
				// facing 沿南北：铰链沿东西（世界 x），north 为 -22.5°
				float sign = facing == Direction.NORTH ? 22.5f : -22.5f;
				ms.mulPose(Axis.XP.rotationDegrees(sign));
			}
			ms.mulPose(Axis.XP.rotationDegrees(90)); // 平放
		} else {
			TransformStack.of(ms)
				.rotateXDegrees(90);
		}
	}
}
