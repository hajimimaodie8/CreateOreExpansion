package com.hjmmd_8.createoreexpansion.content.worldgen.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllStructureProcessors;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * 堡垒宝藏室彩蛋：50% 概率在<b>金块堆内部</b>镶嵌 1 块蓝宝石块。
 *
 * <p>金块堆是露天金字塔（y=3 底层 → y=5 锥尖）。要<b>埋藏在内部、不露天暴露</b>，
 * 必须只替换<b>上方有金块遮挡</b>的金块：实现里对每个金块统计「上方是否金块 + 四面
 * 金块邻居数」，选出内埋最深的一块（上遮挡 + 邻面最多者优先），50% 概率替换为蓝宝石块。
 * 替换后蓝宝石块被上层与四周金块包裹，玩家必须挖开金块堆才能发现。</p>
 *
 * <p>单堡垒最多 1 处（只替换一块）；挖掘该方块会激怒猪灵
 * （蓝宝石块已加入 {@code minecraft:guarded_by_piglins} tag）。</p>
 */
public class BastionTreasureSapphireProcessor extends StructureProcessor {

	public static final MapCodec<BastionTreasureSapphireProcessor> CODEC =
		MapCodec.unit(() -> new BastionTreasureSapphireProcessor());

	private static final Direction[] HORIZONTALS = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

	@Override
	protected StructureProcessorType<?> getType() {
		return AllStructureProcessors.bastionTreasureSapphire();
	}

	@Override
	public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos seedPos,
		BlockPos originalPos, List<StructureBlockInfo> originalBlocks, List<StructureBlockInfo> processedBlocks,
		StructurePlaceSettings settings) {
		// 位置 → 方块状态 映射
		Set<BlockPos> positions = new HashSet<>();
		for (StructureBlockInfo info : processedBlocks)
			positions.add(info.pos());

		// 选出「上方有金块遮挡」且内埋最深的金块：
		// 内埋分 = 上方金块(权重高) + 四面金块邻居数
		BlockPos target = null;
		int bestScore = -1;
		for (StructureBlockInfo info : processedBlocks) {
			if (!info.state().is(Blocks.GOLD_BLOCK))
				continue;
			BlockPos pos = info.pos();
			// 上方必须是金块（否则替换后露天可见）
			BlockPos above = pos.above();
			if (!positions.contains(above))
				continue;
			// 统计四面金块邻居
			int sideCount = 0;
			for (Direction dir : HORIZONTALS) {
				if (positions.contains(pos.relative(dir)))
					sideCount++;
			}
			// 内埋分：上遮挡是硬条件，邻面数越高越靠内部
			int score = 4 + sideCount;
			if (score > bestScore) {
				bestScore = score;
				target = pos;
			}
		}
		if (target == null)
			return processedBlocks;

		// 50% 概率出彩蛋
		RandomSource random = settings.getRandom(target);
		if (random.nextFloat() >= 0.5f)
			return processedBlocks;

		// 替换为蓝宝石块（保留原 nbt）
		for (int i = 0; i < processedBlocks.size(); i++) {
			StructureBlockInfo info = processedBlocks.get(i);
			if (!info.pos().equals(target))
				continue;
			BlockState sapphire = AllBlocks.SAPPHIRE_BLOCK.getDefaultState();
			processedBlocks.set(i, new StructureBlockInfo(info.pos(), sapphire, info.nbt()));
			break;
		}
		return processedBlocks;
	}
}
