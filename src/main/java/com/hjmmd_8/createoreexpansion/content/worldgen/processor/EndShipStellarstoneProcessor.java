package com.hjmmd_8.createoreexpansion.content.worldgen.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllStructureProcessors;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * 末影船彩蛋：50% 概率将<b>龙首后方</b>的紫珀台阶替换为星辉石块。
 *
 * <p>原版末影船船头挂着 {@code dragon_wall_head}（墙挂龙首，朝向船外前方），
 * 其<b>后方</b>（船身方向）紧邻一格是紫珀台阶（purpur_stairs）。</p>
 *
 * <p>实现要点：{@code finalizeProcessing} 里方块位置已是<b>旋转后</b>的世界坐标，
 * 而方块状态的 FACING 仍是<b>模板原值</b>——必须先用 {@code settings.getRotation()}
 * 旋转朝向，再取反方向定位后方台阶，否则方向错位（星辉石会生成到龙首旁边而非后方）。</p>
 *
 * <p>隐藏建筑彩蛋：不做稳定产出，仅探索惊喜（与末影船鞘翅房宝箱保底互补，不放大产出）。</p>
 */
public class EndShipStellarstoneProcessor extends StructureProcessor {

	public static final MapCodec<EndShipStellarstoneProcessor> CODEC =
		MapCodec.unit(() -> new EndShipStellarstoneProcessor());

	@Override
	protected StructureProcessorType<?> getType() {
		return AllStructureProcessors.endShipStellarstone();
	}

	@Override
	public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos seedPos,
		BlockPos originalPos, List<StructureBlockInfo> originalBlocks, List<StructureBlockInfo> processedBlocks,
		StructurePlaceSettings settings) {
		// 建立 位置 → 列表下标 映射，便于查相邻
		Set<BlockPos> positions = new HashSet<>();
		for (StructureBlockInfo info : processedBlocks)
			positions.add(info.pos());

		BlockPos target = null;
		for (StructureBlockInfo info : processedBlocks) {
			BlockState state = info.state();
			if (!state.is(Blocks.DRAGON_WALL_HEAD))
				continue;
			// 龙首 FACING（无 FACING 属性则跳过）
			if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
				continue;
			// 模板朝向 → 按结构旋转换算成世界朝向（位置已是旋转后的世界坐标）
			Direction templateFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
			Direction worldFacing = settings.getRotation().rotate(templateFacing);
			// 后方 = 世界朝向反方向（船身方向，紫珀台阶所在格）
			BlockPos behind = info.pos().relative(worldFacing.getOpposite());
			if (positions.contains(behind)) {
				target = behind;
				break;
			}
		}
		if (target == null)
			return processedBlocks;

		// 50% 概率出彩蛋
		if (settings.getRandom(target).nextFloat() >= 0.5f)
			return processedBlocks;

		// 替换为星辉石块（保留原 nbt，台阶方块通常无 nbt）
		for (int i = 0; i < processedBlocks.size(); i++) {
			StructureBlockInfo info = processedBlocks.get(i);
			if (!info.pos().equals(target))
				continue;
			processedBlocks.set(i, new StructureBlockInfo(info.pos(),
				AllBlocks.STELLARSTONE_BLOCK.getDefaultState(), info.nbt()));
			break;
		}
		return processedBlocks;
	}
}
