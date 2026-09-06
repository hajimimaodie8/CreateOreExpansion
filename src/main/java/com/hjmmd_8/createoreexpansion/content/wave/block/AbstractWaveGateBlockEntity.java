package com.hjmmd_8.createoreexpansion.content.wave.block;

import java.util.List;

import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

	// ========== 机型参数（子类覆写区分翡翠/蓝宝石等变体） ==========

	/**
	 * 最低调制转速（RPM）：达到后才对能量波做等级/速度调制，否则仅通道。
	 * 翡翠 = Create FAST（默认 100）；蓝宝石 = 64。
	 */
	public float getModulationSpeedThreshold() {
		return IRotate.SpeedLevel.FAST.getSpeedValue();
	}

	/** 调级器可提升到的最大波等级（1~5）：翡翠 3（伽马），蓝宝石 5（欧米伽）。 */
	public int getMaxBoostLevel() {
		return 3;
	}

	/**
	 * 该机型<b>可承载/允许输出</b>的最高波等级（1~5）：
	 * 翡翠线 = 3（伽马），蓝宝石线 = 5（欧米伽）。
	 * <p>翡翠调级器/波速调节器遇到 4/5 级波（蓝宝石专属）时，只有把波降级到本上限内的
	 * 操作才有效；维持/升级/原样穿过 4/5 级波一律操作无效（波湮灭）。蓝宝石机型不受限。</p>
	 */
	public int getMaxSupportedWaveLevel() {
		return 3;
	}

	/**
	 * 波速调节器分档数（翡翠 4 档：速度修正 0.5/1/1.5/2；蓝宝石 6 档：0.5~3）。
	 * 仅波速调节器子类使用；调级器返回 0 无意义。
	 */
	public int getSpeedTierCount() {
		return 4;
	}

	/** 波速调节器单档修正量（格/秒）。 */
	public float getSpeedTierStep() {
		return 0.5f;
	}

	/** 波速调节器分档转速区间下界（RPM），低于此不调制。 */
	public float getSpeedTierBase() {
		return IRotate.SpeedLevel.FAST.getSpeedValue();
	}

	/** 波速调节器分档转速区间上界（RPM）。 */
	public float getSpeedTierMax() {
		return 256f;
	}

	// ========== 护目镜悬浮信息（行排版学 Create Ore Excavation 钻机） ==========

	/** 护目镜标题行（子类各自机型名，文案走 lang 键）。 */
	protected abstract Component getGoggleTitle();

	/**
	 * 波闸统一护目镜主干（子类调用 super 后追加各自调制信息行）：
	 * KBE 应力行 → 机型标题 → 转速需求/调制状态行（未达标金色提示，仿钻机 speedRequirement）。
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		GoggleUtil.forGoggles(tooltip, getGoggleTitle().copy()
			.withStyle(ChatFormatting.GRAY));
		added = true;

		float threshold = getModulationSpeedThreshold();
		float speed = Math.abs(getSpeed());
		if (speed < threshold) {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.gate_need_speed", (int) threshold)
				.withStyle(ChatFormatting.GOLD));
		} else {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.gate_speed_ok", (int) speed)
				.withStyle(ChatFormatting.AQUA));
		}
		return added;
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
