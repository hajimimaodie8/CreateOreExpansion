package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量波闸方块实体基类：继承 {@link SimpleKineticBlockEntity} 获得 Create 标准的
 * 应力接入/转速计算/网络同步能力，以及<b>齿轮啮合传播</b>逻辑
 * （作为小齿轮可与相邻齿轮啮合反向传动），驱动齿轮随应力旋转。
 *
 * <p><b>能量接收面板状态持久化</b>：顶/底两面 open/close 状态由
 * blockstate 属性（{@code receiver_top}/{@code receiver_bottom}）保存——
 * 方块状态随存档自动持久化、随方块实体网络包同步客户端，
 * 这里额外写入 NBT 作为冗余备份（读取时以 blockstate 为准，不回写覆盖）。</p>
 *
 * <p>子类（能量调级器 / 波速调节器等）只需提供各自 {@code BlockEntityType} 构造器，
 * 面板管理、转速查询（{@link #getSpeed()}）等全部复用本基类。</p>
 */
public abstract class AbstractWaveGateBlockEntity extends SimpleKineticBlockEntity {

	private static final String RECEIVER_TOP_NBT = "ReceiverTop";
	private static final String RECEIVER_BOTTOM_NBT = "ReceiverBottom";

	protected AbstractWaveGateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		// 冗余备份：blockstate 属性随存档保存，这里同步写入 NBT（读取时以 blockstate 为准）
		compound.putBoolean(RECEIVER_TOP_NBT,
			getBlockState().getValue(AbstractWaveGateBlock.RECEIVER_TOP));
		compound.putBoolean(RECEIVER_BOTTOM_NBT,
			getBlockState().getValue(AbstractWaveGateBlock.RECEIVER_BOTTOM));
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		// 面板状态以 blockstate 为权威（随存档/网络包保存），NBT 仅作冗余：
		// 只在 NBT 与 blockstate 不一致时才回写，且用 switchToBlockState（保留动能方块实体，
		// 避免 level.setBlock 重建 BE 打断齿轮网络/触发多余方块更新）。
		if (compound.contains(RECEIVER_TOP_NBT) || compound.contains(RECEIVER_BOTTOM_NBT)) {
			BlockState state = getBlockState();
			boolean top = compound.getBoolean(RECEIVER_TOP_NBT);
			boolean bottom = compound.getBoolean(RECEIVER_BOTTOM_NBT);
			if (state.getValue(AbstractWaveGateBlock.RECEIVER_TOP) != top
				|| state.getValue(AbstractWaveGateBlock.RECEIVER_BOTTOM) != bottom) {
				switchToBlockState(level, worldPosition, state
					.setValue(AbstractWaveGateBlock.RECEIVER_TOP, top)
					.setValue(AbstractWaveGateBlock.RECEIVER_BOTTOM, bottom));
			}
		}
	}
}
