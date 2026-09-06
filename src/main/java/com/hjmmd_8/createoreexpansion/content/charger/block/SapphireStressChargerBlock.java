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
 * 蓝宝石应力充能器：六向应力机器（可水平/竖直放置），蓝宝石科技线专属。
 *
 * <p>继承 {@link AbstractCreateChargerBlock} 复用全部通用行为（放置朝向、MODE、传动轴接入），
 * 仅绑定蓝宝石充能器方块实体。</p>
 *
 * <p><b>与翡翠的区别</b>：可蓄力至 4 级（伊普西龙）/5 级（欧米伽）并发射蓝宝石波
 * （等级映射与双模式逻辑见 {@link SapphireStressChargerBlockEntity}）。</p>
 *
 * <p><b>交互</b>：右键切换模式（普通 ⇄ 储存）；储存模式蓄满后右键触发簇射。
 * <b>红石</b>：上升沿触发储存模式簇射。</p>
 */
public class SapphireStressChargerBlock extends AbstractCreateChargerBlock implements IBE<SapphireStressChargerBlockEntity> {

	public SapphireStressChargerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<SapphireStressChargerBlockEntity> getBlockEntityClass() {
		return SapphireStressChargerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SapphireStressChargerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SAPPHIRE_STRESS_CHARGER.get();
	}

	/**
	 * 右键交互：
	 * <ul>
	 *   <li><b>模式切换</b>：由机器水平侧面的 ValueBox 交互完成（瞄准侧面按住右键 / 滚轮，
	 *       仿黄铜隧道模式槽，见 {@link SapphireChargerModeSlot}）——不再使用 Shift+右键；</li>
	 *   <li><b>普通右键</b>：储存模式释放一层（每层一个能量波，可连点逐层放完）；普通模式无操作。</li>
	 * </ul>
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
		BlockHitResult hitResult) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof SapphireStressChargerBlockEntity be) {
			if (be.isStoringMode() && be.hasLayerToRelease()) {
				be.releaseOneLayer();
			} else {
				return InteractionResult.PASS; // 无操作放行（不打断其它交互）
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** 红石信号变化（上升沿）触发储存模式簇射。
	 * <p>检测到方块接收到红石信号即转发给 BE；BE 内做上升沿判断（false→true 才触发）。</p> */
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
		boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (level.isClientSide)
			return;
		if (level.getBlockEntity(pos) instanceof SapphireStressChargerBlockEntity be) {
			be.onRedstoneSignal(level.hasNeighborSignal(pos));
		}
	}
}
