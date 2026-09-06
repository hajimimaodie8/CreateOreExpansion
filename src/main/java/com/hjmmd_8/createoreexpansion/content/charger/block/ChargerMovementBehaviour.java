package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 翡翠应力充能器的 Create 动态结构（contraption）行为 —— 充能器装进
 * 动力轴承 / 矿车装配站等结构后<b>随结构自行工作</b>（像钻头/动力锯一样）。
 *
 * <p><b>固定行为（简化版）</b>：每 3 秒发射一个<b>中能量波</b>（高充能档），
 * 发射方向 = 方块 FACING 经 {@code context.rotation}（contraption 旋转）换算到世界，
 * 波出生在 {@code context.world}（主世界）。不做 RPM 分档/blockstate 更新
 * （contraption 方块模型不随 blockstate 重烘焙，转速读取也不稳定，保持最简单可靠的行为）。</p>
 *
 * <p><b>机制</b>：contraption 不 tick 方块实体（BE 数据序列化进
 * {@code MovementContext.blockEntityData}），因此通过 {@link MovementBehaviour#tick}
 * 实现蓄力 + 发射：蓄力计数存于 {@code MovementContext.data}（随 contraption 持久化）。</p>
 */
public class ChargerMovementBehaviour implements MovementBehaviour {

	/** 蓄力间隔（tick）：3 秒一发。 */
	private static final int CHARGE_INTERVAL = 60;

	/** 固定充能态：2 = 高充能（中能量波）。 */
	private static final int MOUNTED_MODE = 2;

	@Override
	public boolean isActive(MovementContext context) {
		// 只有蓄力/发射需要活动；被控制装置禁用时停止
		return !context.disabled;
	}

	@Override
	public void tick(MovementContext context) {
		// 服务端权威：发射只在服务端执行（客户端仅接收实体同步）
		if (context.world.isClientSide)
			return;

		// 蓄力（持久化于 context.data，随 contraption 存档）
		int charge = context.data.getInt("ChargeTicks") + 1;
		if (charge < CHARGE_INTERVAL) {
			context.data.putInt("ChargeTicks", charge);
			return;
		}
		context.data.putInt("ChargeTicks", 0);

		// 发射：方向 = 方块 FACING 经 contraption 旋转 → 世界方向
		Direction facing = context.state.getValue(AbstractCreateChargerBlock.FACING);
		Vec3 worldDir = context.rotation.apply(Vec3.atLowerCornerOf(facing.getNormal()));
		// 出生点沿方向外推 1 格（避免第一 tick 撞自身）
		Vec3 start = context.position.add(worldDir.scale(1.0));

		context.world.addFreshEntity(new ChargerWaveEntity(context.world, start, worldDir, MOUNTED_MODE));
		context.world.playSound(null, start.x, start.y, start.z, SoundEvents.NOTE_BLOCK_BELL.value(),
			SoundSource.BLOCKS, 1.0F, 1.0F);
	}
}
