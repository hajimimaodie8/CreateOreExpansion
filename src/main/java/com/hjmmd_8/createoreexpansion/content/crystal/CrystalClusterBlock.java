package com.hjmmd_8.createoreexpansion.content.crystal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * 可生长水晶芽/簇（小芽 → 中芽 → 大芽 → 簇 四阶段）。
 *
 * <p>继承原版 {@link AmethystClusterBlock}，保留 FACING/WATERLOGGED 属性、碰撞箱、
 * 支撑面破坏掉落等全部原版行为，使 AE2 晶体催生器（对原版芽簇的 {@code instanceof}
 * 兼容加速）可直接加速本模组水晶。</p>
 *
 * <p>原版 {@code randomTick} 按 {@code this == 原版方块} 判断生长阶段，子类不适用，
 * 故完全重写：每次随机刻按配置的「每阶段秒数」换算的推进概率，推进到下一阶段方块
 * （保持朝向与含水状态）。</p>
 */
public class CrystalClusterBlock extends AmethystClusterBlock {

	/** 下一阶段方块（null = 已是最终簇，不再生长） */
	private final Supplier<? extends Block> nextStage;
	/** 芽每阶段平均生长时间（秒，来自 {@link CrystalGrowthConfigs}） */
	private final float stageGrowSeconds;

	public CrystalClusterBlock(int height, int xzOffset, Properties properties,
			Supplier<? extends Block> nextStage, float stageGrowSeconds) {
		super(height, xzOffset, properties);
		this.nextStage = nextStage;
		this.stageGrowSeconds = stageGrowSeconds;
	}

	/** 是否为最终水晶簇（可采集，不再生长） */
	public boolean isCluster() {
		return nextStage == null;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (nextStage == null)
			return; // 簇不再生长
		// 随机刻平均约 68.27 秒一次；按配置的每阶段秒数换算推进概率（默认 340 秒 ≈ 1/5，与原版节奏一致）
		float avgSeconds = Math.max(1.0F, stageGrowSeconds);
		float chance = (float) Math.min(1.0, 68.27 / avgSeconds);
		if (random.nextFloat() < chance) {
			level.setBlock(pos, nextStage.get().defaultBlockState()
				.setValue(FACING, state.getValue(FACING))
				.setValue(WATERLOGGED, state.getValue(WATERLOGGED)), 2);
		}
	}
}
