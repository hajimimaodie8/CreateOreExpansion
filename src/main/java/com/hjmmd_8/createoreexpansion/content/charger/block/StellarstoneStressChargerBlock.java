package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 星辉石应力充能器：六向应力机器（可水平/竖直放置），星辉石科技线专属。
 *
 * <p>继承 {@link AbstractCreateChargerBlock} 复用全部通用行为（放置朝向、MODE、传动轴接入），
 * 仅绑定星辉石充能器方块实体。整体逻辑为蓝宝石充能器的复制变体
 * （参考 {@link SapphireStressChargerBlock}），差别在两侧各一个交互槽：</p>
 * <ul>
 *   <li><b>模式槽</b>（FACING 逆时针侧 getCounterClockWise()，即规格 rotateYCCW；竖直时东侧）：普通 ⇄ 储存（沿用 {@link SapphireChargerMode}）；</li>
 *   <li><b>手动发射波等级槽</b>（FACING 顺时针侧 getClockWise()，即规格 rotateY；竖直时西侧）：固定 1~5 级，
 *       发射等级与转速无关（见 {@link StellarstoneStressChargerBlockEntity#getModeForSpeed()}）。</li>
 * </ul>
 */
public class StellarstoneStressChargerBlock extends AbstractCreateChargerBlock
	implements IBE<StellarstoneStressChargerBlockEntity> {

	public StellarstoneStressChargerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<StellarstoneStressChargerBlockEntity> getBlockEntityClass() {
		return StellarstoneStressChargerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StellarstoneStressChargerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.STELLARSTONE_STRESS_CHARGER.get();
	}

	/**
	 * 右键交互（与蓝宝石充能器一致）：
	 * <ul>
	 *   <li><b>模式切换 / 等级调整</b>：由两侧 ValueBox 交互完成（瞄准侧面按住右键 / 滚轮）；</li>
	 *   <li><b>普通右键</b>：储存模式释放一层（每层一个能量波，可连点逐层放完）；普通模式无操作。</li>
	 * </ul>
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
		BlockHitResult hitResult) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof StellarstoneStressChargerBlockEntity be) {
			if (be.isStoringMode() && be.hasLayerToRelease()) {
				be.releaseOneLayer();
			} else {
				return InteractionResult.PASS; // 无操作放行（不打断其它交互）
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** 红石信号变化（上升沿）触发储存模式簇射。 */
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
		boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (level.isClientSide)
			return;
		if (level.getBlockEntity(pos) instanceof StellarstoneStressChargerBlockEntity be) {
			be.onRedstoneSignal(level.hasNeighborSignal(pos));
		}
	}
}
