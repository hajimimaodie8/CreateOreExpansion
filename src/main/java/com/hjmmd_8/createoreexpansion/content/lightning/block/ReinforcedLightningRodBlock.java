package com.hjmmd_8.createoreexpansion.content.lightning.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 强化避雷针：<b>直接继承原版 {@link LightningRodBlock}</b>，复用原版全部行为——
 * 下雨天吸引闪电（tick）、被自然闪电击中（onLightningStrike）、露天判定、红石信号、
 * 三叉戟引雷交互、铁砧工艺等，原版逻辑完全保留。
 *
 * <p>叠加新增（不改动原版行为）：伽马能量波充能——攒满 {@code MAX_CHARGE} 获得
 * 1 次手动引雷机会，右键释放闪电（带冷却、露天校验）。</p>
 *
 * <p>两条充能路径互不冲突：路径 A = 被自然闪电击中（原版原生，继承自动生效）；
 * 路径 B = 伽马能量波充能（{@link ReinforcedLightningRodBlockEntity#onGammaWaveHit()}）。</p>
 */
public class ReinforcedLightningRodBlock extends LightningRodBlock implements IBE<ReinforcedLightningRodBlockEntity> {

	public ReinforcedLightningRodBlock(Properties properties) {
		super(properties);
	}

	/** 右键：消耗 1 层引雷充能在方块上方生成闪电（充能进度满才可释放，带冷却、露天校验） */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											  Player player, InteractionHand hand, BlockHitResult hitResult) {
		return onBlockEntityUseItemOn(level, pos, be -> be.tryReleaseLightning(player));
	}

	@Override
	public Class<ReinforcedLightningRodBlockEntity> getBlockEntityClass() {
		return ReinforcedLightningRodBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ReinforcedLightningRodBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.REINFORCED_LIGHTNING_ROD.get();
	}

	/** 方块实体 ticker：冷却递减 + 有充能时金色电光粒子 */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : (l, p, s, be) -> {
			if (be instanceof ReinforcedLightningRodBlockEntity rod)
				rod.tick();
		};
	}
}
