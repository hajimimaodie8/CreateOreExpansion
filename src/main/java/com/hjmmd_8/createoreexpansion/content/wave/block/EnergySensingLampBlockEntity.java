package com.hjmmd_8.createoreexpansion.content.wave.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量感应灯计时方块实体：仅记录"亮态熄灭时刻"并逐 tick 检查，到点将 blockstate
 * 的 {@code lamp_state} 置回 0（熄灭）。<b>不参与渲染</b>——外观完全由 blockstate
 * 变体模型切换贴图驱动，本 BE 仅承担服务端计时。
 *
 * <p>每次被能量波命中时 {@link #refreshLit(int)} 重置熄灭时刻
 * （按波级 1/2/3 → 持续 20/40/60 tick，即 1/2/3 秒）；熄灭时刻持久化进 NBT，
 * 存档重进后计时不丢。</p>
 */
public class EnergySensingLampBlockEntity extends BlockEntity {

	private static final String EXPIRE_AT_NBT = "ExpireAt";

	/** 亮态熄灭时刻（世界游戏时间 {@code level.getGameTime()}）；0 = 未点亮 */
	private long expireAt;

	public EnergySensingLampBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 每次被波命中：刷新亮态熄灭时刻（波级 1/2/3 → 20/40/60 tick）。 */
	public void refreshLit(int waveLevel) {
		int ticks = switch (waveLevel) {
			case 1 -> 20; // 低：1 秒
			case 2 -> 40; // 高：2 秒
			case 3 -> 60; // 伽马：3 秒
			default -> 0;
		};
		if (level != null)
			expireAt = level.getGameTime() + ticks;
		setChanged();
	}

	/** 服务端逐 tick：亮态到点 → 置回熄灭。 */
	public void tick() {
		if (level == null || level.isClientSide)
			return;
		if (expireAt == 0)
			return; // 未点亮
		if (level.getGameTime() < expireAt)
			return; // 未到熄灭时刻
		BlockState state = getBlockState();
		if (state.getValue(EnergySensingLampBlock.LAMP_STATE) > 0) {
			level.setBlock(worldPosition, state.setValue(EnergySensingLampBlock.LAMP_STATE, 0),
				Block.UPDATE_ALL);
		}
		expireAt = 0;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putLong(EXPIRE_AT_NBT, expireAt);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		expireAt = tag.getLong(EXPIRE_AT_NBT);
	}
}
