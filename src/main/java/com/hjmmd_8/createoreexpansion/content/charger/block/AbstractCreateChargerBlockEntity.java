package com.hjmmd_8.createoreexpansion.content.charger.block;

import java.awt.Color;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 应力充能器方块实体抽象基类：按转速更新充能态并蓄力，蓄满后向前发射对应等级的能量波。
 *
 * <p>通用行为（模板方法，子类无需改动）：</p>
 * <ul>
 *     <li>充能态解析：0=未接入应力；1=低（≤½ 上限转速）；2=高（½~上限转速）；3=伽马（≥上限转速）；
 *         上限转速读取 Create 配置 {@code maxRotationSpeed}（默认 256 RPM，可调，不硬编码）；</li>
 *     <li>发射间隔：转速 1 → 10 秒，256 → 1 秒，线性插值；</li>
 *     <li>服务端蓄力计数并通过 {@code sendData} 同步进度给客户端（两端一致）；</li>
 *     <li>发射头蓄力收缩 / 发射弹出动画（{@link #getShutterOffset}）；</li>
 *     <li>护目镜蓄力能量条（复用工具能量条，填充色随充能态）。</li>
 * </ul>
 *
 * <p>子类（翡翠充能器/雷鸣充能器）只需覆写：</p>
 * <ul>
 *     <li>{@link #createWave(Level, Vec3, Direction, int)} —— 发射各自的能量波实体；</li>
 *     <li>{@link #getWaveColor(int)} —— 三档充能态对应粒子颜色（子类各自配色）；</li>
 *     <li>{@link #getMachineName()} —— 护目镜标题；</li>
 *     <li>{@link #getGoggleColor(int)} —— 护目镜状态行颜色（默认跟随粒子颜色）。</li>
 * </ul>
 */
public abstract class AbstractCreateChargerBlockEntity extends KineticBlockEntity {

	/** 已蓄力 tick 数 */
	private int chargeTicks;

	/** 发射弹出动画剩余 tick（发射瞬间触发，>0 时发射头做弹簧弹出） */
	private int popTicks;

	/** 弹出动画时长（tick） */
	protected static final int POP_TICKS = 4;

	protected AbstractCreateChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void tick() {
		super.tick();

		// 按转速更新充能态（blockstate MODE），仅在变化时更新
		int mode = getModeForSpeed();
		if (getBlockState().getValue(AbstractCreateChargerBlock.MODE) != mode) {
			level.setBlockAndUpdate(worldPosition, getBlockState().setValue(AbstractCreateChargerBlock.MODE, mode));
		}

		float speed = Math.abs(getSpeed());
		if (speed <= 0) {
			// 仅服务端清零进度：客户端刚加载时 getSpeed() 可能尚未同步（网络数据包未到）为 0，
			// 若也清零会导致进度条/动画与服务端发射时刻错位（大退重开后尤为明显）
			if (!level.isClientSide)
				chargeTicks = 0;
			return;
		}

		// 蓄力：满后发射能量波（服务端累加并通过 sendData 同步给客户端，保证两端进度一致）
		// 客户端不自行累加：进度/发射时刻完全由服务端同步值驱动（大退重开后服务端先 tick 也不失真）
		if (!level.isClientSide) {
			chargeTicks++;
			sendData(); // 同步进度给客户端
			if (chargeTicks >= getIntervalTicks()) {
				chargeTicks = 0;
				popTicks = POP_TICKS; // 发射弹出动画：发射 tick 渲染时从收缩位 -1.5px 起点弹出，与能量波射出同步
				launchWave();
				// 发射 tick 内 popTicks 保持满值（渲染从 t=0 开始），下一 tick 再递减
			} else if (popTicks > 0) {
				popTicks--; // 非发射 tick：弹出动画推进
			}
		} else if (popTicks > 0) {
			popTicks--; // 客户端：弹出动画由 read 触发，tick 内递减推进
		}
	}

	/** 发射间隔（tick）：转速 1 → 10 秒（200 tick），256 → 1 秒（20 tick），线性插值；更快转速保持 20 tick */
	protected int getIntervalTicks() {
		float rpm = Math.abs(getSpeed());
		float interval = Mth.clamp(10f - 9f * (rpm - 1) / 255f, 1f, 10f);
		return Math.max(1, Math.round(interval * 20f));
	}

	/**
	 * 按转速解析充能态：0=无应力，1=低（≤½ 上限转速），2=高（½~上限转速），3=伽马（≥上限转速）。
	 *
	 * <p>档位按 Create 配置的转速上限 {@code maxRotationSpeed}（默认 256 RPM）等比划分，
	 * 上限可调（config 修改/整合包覆盖）时档位自动跟随，不硬编码数值。</p>
	 */
	protected int getModeForSpeed() {
		float speed = Math.abs(getSpeed());
		if (speed <= 0)
			return 0;
		int max = AllConfigs.server().kinetics.maxRotationSpeed.get();
		if (speed <= max / 2f)
			return 1;
		if (speed < max)
			return 2;
		return 3;
	}

	/** 沿 FACING 方向发射当前充能态对应的能量波 */
	private void launchWave() {
		if (level.isClientSide)
			return;
		int mode = getBlockState().getValue(AbstractCreateChargerBlock.MODE);
		if (mode <= 0)
			return;
		Direction facing = getBlockState().getValue(AbstractCreateChargerBlock.FACING);
		// 起点外推 1 格（保证出生点碰撞盒完全离开充能器方块，否则第一 tick 撞到自身消散）
		Vec3 start = Vec3.atCenterOf(worldPosition)
			.add(Vec3.atLowerCornerOf(facing.getNormal())
				.scale(1.0));
		level.addFreshEntity(createWave(level, start, facing, mode));
		// 发射音效：音符盒钟声（清脆响亮、金属机械感，非菜单/UI 声）
		level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/**
	 * 创建能量波实体（子类实现：发射各自的波实体，构造传入本方块实体以读取颜色/等级）。
	 */
	protected abstract AbstractChargerWaveEntity createWave(net.minecraft.world.level.Level level, Vec3 start,
		Direction facing, int mode);

	/** 发射头（shutter）沿 FACING 方向的偏移量（格，正=发射方向），供渲染器做蓄力/弹出动画。
	 * <p>单位均为像素（1 格 = 16 像素）。蓄力期间向传动轴方向（-FACING）线性收缩 1.5 像素；
	 * 发射瞬间触发弹簧弹出（0.2 秒）：从收缩位置迅速冲出到前方 1 像素 → 过冲至 1.5 像素 →
	 * 回落到原位，与能量波射出同步，随后开始下一轮收缩。</p> */
	public float getShutterOffset(float partialTicks) {
		float speed = Math.abs(getSpeed());
		if (speed <= 0)
			return 0;
		float px = 1f / 16f;

		// 发射弹出动画：发射后的短暂时间内弹簧式冲出（与能量波射出同步）
		if (popTicks > 0) {
			float t = Mth.clamp((POP_TICKS - popTicks + partialTicks) / POP_TICKS, 0, 1);
			if (t < 0.35f)
				return px * (-1.5f + (t / 0.35f) * 2.5f); // -1.5px → +1.0px
			if (t < 0.55f)
				return px * (1f + ((t - 0.35f) / 0.2f) * 0.5f); // 过冲至 +1.5px
			return px * (1.5f - ((t - 0.55f) / 0.45f) * 1.5f); // 回落到原位 0
		}

		// 蓄力收缩：线性向里缩 1.5 像素
		float t = Mth.clamp((chargeTicks + partialTicks) / getIntervalTicks(), 0, 1);
		return -1.5f * px * t;
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.putInt("ChargeTicks", chargeTicks);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		int newCharge = compound.getInt("ChargeTicks");
		// 客户端：收到服务端同步值，若从高位归零（发射瞬间）触发弹出动画；客户端不自行累加进度
		if (clientPacket && level != null && level.isClientSide) {
			if (newCharge == 0 && chargeTicks > 0)
				popTicks = POP_TICKS;
		}
		chargeTicks = newCharge;
	}

	/** 当前充能态（1=低/2=高/3=伽马）对应的粒子颜色（RGB int）—— 子类各自配色 */
	protected abstract int getWaveColor(int mode);

	/** 护目镜状态行颜色（默认跟随粒子颜色） */
	protected ChatFormatting getGoggleColor(int mode) {
		return switch (mode) {
			case 2 -> ChatFormatting.GREEN;
			case 3 -> ChatFormatting.AQUA;
			case 1 -> ChatFormatting.YELLOW;
			default -> ChatFormatting.GRAY;
		};
	}

	/** 护目镜标题（如"翡翠应力充能器"） */
	protected abstract Component getMachineName();

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		tooltip.add(getMachineName().copy()
			.withStyle(ChatFormatting.GRAY));
		added = true;

		int mode = getBlockState().getValue(AbstractCreateChargerBlock.MODE);
		// 档位区间按 Create 配置的转速上限（maxRotationSpeed，默认 256）动态计算后作为参数传入，
		// 别人修改上限配置时护目镜显示的 RPM 区间自动跟随（不硬编码数值）
		int max = AllConfigs.server().kinetics.maxRotationSpeed.get();
		Component stateName = switch (mode) {
			case 1 -> Component.translatable("createoreexpansion.goggles.charger_low", max / 2);
			case 2 -> Component.translatable("createoreexpansion.goggles.charger_high", max / 2 + 1, max - 1);
			case 3 -> Component.translatable("createoreexpansion.goggles.charger_gamma", max);
			default -> Component.translatable("createoreexpansion.goggles.charger_idle");
		};
		tooltip.add(stateName.copy()
			.withStyle(getGoggleColor(mode)));

		// 蓄力进度能量条（复用工具能量条 BarTooltipRender，填充色=当前充能态颜色）
		// 发射弹出动画期间（popTicks>0）显示满格，与能量波射出瞬间同步
		if (Math.abs(getSpeed()) > 0) {
			int intervalTicks = getIntervalTicks();
			int fillRgb = getWaveColor(mode);
			Color fill = new Color(fillRgb);
			int displayTicks = popTicks > 0 ? intervalTicks : chargeTicks;
			int filled = (int) ((long) displayTicks * 10 / intervalTicks);
			tooltip.add(BarTooltipRender.energy(filled, 10, 10, fill));
		}
		return added;
	}
}
