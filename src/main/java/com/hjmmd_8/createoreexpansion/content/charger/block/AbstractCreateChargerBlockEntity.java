package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import java.awt.Color;
import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
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
import net.minecraft.world.level.Level;
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
 *     <li>{reateWave(Level, Vec3, Direction, int)} —— 发射各自的能量波实体；</li>
 *     <li>{@link #getWaveColor(int)} —— 三档充能态对应粒子颜色（子类各自配色）；</li>
 *     <li>{@link #getMachineName()} —— 护目镜标题；</li>
 *     <li>{@link #getGoggleColor(int)} —— 护目镜状态行颜色（默认跟随粒子颜色）。</li>
 * </ul>
 */
public abstract class AbstractCreateChargerBlockEntity extends KineticBlockEntity {

	/** 已蓄力 tick 数（protected：蓝宝石子类的储存/簇射模式需自行推进与清零） */
	protected int chargeTicks;

	/** 发射弹出动画剩余 tick（发射瞬间触发，>0 时发射头做弹簧弹出） */
	protected int popTicks;

	/** 弹出动画时长（tick） */
	protected static final int POP_TICKS = 4;

	/**
	 * 释放弹出动画剩余 tick（仅储存模式用）：右键释放一层时触发一次"弹出→收回"，
	 * 连续释放时由子类在释放瞬间重新置满以平滑衔接。
	 */
	protected int releaseTicks;

	/** 是否储存模式（快门动画模式切换：蓄能静止 + 释放弹出）。翡翠默认否；蓝宝石储存模式覆写为 true。 */
	public boolean isStoringMode() {
		return false;
	}

	/**
	 * 应力消耗随发射能量波等级（蓄力档）变化：消耗 = 一级基准消耗 × 档位。
	 * 一级基准 = 注册/配置的 IMPACT（默认 4.0）：翡翠 1~3 → 4/8/12，蓝宝石 1~5 → 4/8/12/16/20。
	 * 0（未接入应力）→ 0；护目镜/网络按此实时计耗（档位变化见 tick 内的网络推送）。
	 */
	@Override
	public float calculateStressApplied() {
		int mode = getBlockState().getValue(AbstractCreateChargerBlock.MODE);
		if (mode <= 0) {
			this.lastStressApplied = 0f;
			return 0f;
		}
		float base = (float) com.simibubi.create.api.stress.BlockStressValues.getImpact(getStressConfigKey());
		float impact = base * mode;
		this.lastStressApplied = impact;
		return impact;
	}

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
			// 应力消耗随充能档位变化（impact = 4 × 档位）：已接入网络时推送新消耗并让网络重算，
			// 否则护目镜/网络仍按旧档位计耗
			if (!level.isClientSide && hasNetwork())
				getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
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
			// 储存模式：快门不随每轮蓄力弹动（攒层由 onChargeComplete 处理），
			// 仅服务端维护 chargeTicks 进度；releaseTicks 由释放（fireBurst）置满后逐 tick 归零，
			// 客户端只在"收到满值"那一包触发一次动画（避免每包重置导致鬼畜）。
			chargeTicks++;
			sendData(); // 同步进度给客户端
			if (releaseTicks > 0)
				releaseTicks--; // 释放弹标记逐 tick 归零（服务端仅作同步源）
			if (chargeTicks >= getIntervalTicks()) {
				chargeTicks = 0;
				onChargeComplete(); // 满蓄处理（默认发射；蓝宝石储存模式可覆写为攒入簇射储备）
			} else if (popTicks > 0) {
				popTicks--; // 非发射 tick：弹出动画推进
			}
		} else if (popTicks > 0) {
			popTicks--; // 客户端：弹出动画由 read 触发，tick 内递减推进
		} else if (releaseTicks > 0) {
			releaseTicks--; // 储存模式释放弹出动画推进
		}
	}

	/**
	 * 满蓄处理钩子：默认触发弹出动画并发射能量波。
	 * <p>子类可覆写以改变"蓄满后"的行为（如蓝宝石储存模式：攒入簇射储备、满后停止）。</p>
	 */
	protected void onChargeComplete() {
		popTicks = POP_TICKS; // 发射弹出动画：发射 tick 渲染时从收缩位 -1.5px 起点弹出，与能量波射出同步
		launchWave();
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

	/** 沿 FACING 方向发射当前充能态（blockstate MODE）对应的能量波。 */
	protected void launchWave() {
		int mode = getBlockState().getValue(AbstractCreateChargerBlock.MODE);
		if (mode <= 0)
			return;
		launchWave(mode);
	}

	/**
	 * 沿 FACING 方向发射指定等级的能量波（含 Sable 物理结构坐标换算）。
	 *
	 * @param mode 能量波等级（1=低/2=高/3=伽马/4=伊普西龙/5=欧米伽）；供蓝宝石
	 *             储存模式的簇射 / 红石触发发射指定等级波复用
	 */
	protected void launchWave(int mode) {
		if (level.isClientSide)
			return;
		Direction facing = getBlockState().getValue(AbstractCreateChargerBlock.FACING);
		// 起点外推 1 格（保证出生点碰撞盒完全离开充能器方块，否则第一 tick 撞到自身消散）
		Vec3 start = Vec3.atCenterOf(worldPosition)
			.add(Vec3.atLowerCornerOf(facing.getNormal())
				.scale(1.0));
		Level targetLevel = level;
		// 飞行方向保留任意角度（结构旋转后随旋转，不再 Direction.getNearest 量化成轴向）
		Vec3 worldDir = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 soundPos = Vec3.atCenterOf(worldPosition);

		// Sable 物理结构适配：充能器在结构（sub-level）上时，BE 的 worldPosition/FACING 是
		// 结构本地坐标，而能量波实体必须出生在真实世界——经 sub-level 位姿矩阵换算到世界坐标后
		// 把波加到主世界（否则波会出生在虚拟子世界，主世界看不到 = "不发射"）。
		SubLevelBridge bridge = SableBridges.get();
		if (bridge != null) {
			SubLevelBridge.Hit hit = bridge.ofBlockEntity(this);
			if (hit != null) {
				start = bridge.toWorld(hit, start);
				// 本地方向 → 世界方向（Pose 四元数旋转，保留任意角度）
				worldDir = bridge.toWorldDir(hit, Vec3.atLowerCornerOf(facing.getNormal()));
				targetLevel = bridge.worldLevel(hit);
				soundPos = bridge.toWorld(hit, Vec3.atCenterOf(worldPosition));
			}
		}

		targetLevel.addFreshEntity(createWave(targetLevel, start, worldDir, mode));
		// 发射音效：音符盒钟声（清脆响亮、金属机械感，非菜单/UI 声）
		targetLevel.playSound(null, soundPos.x, soundPos.y, soundPos.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/**
	 * 创建能量波实体（子类实现：发射各自的波实体，构造传入本方块实体以读取颜色/等级）。
	 *
	 * @param movementDir 世界空间飞行方向（任意角度向量，无需单位化）
	 */
	protected abstract AbstractChargerWaveEntity createWave(net.minecraft.world.level.Level level, Vec3 start,
		Vec3 movementDir, int mode);

	/** 发射头（shutter）沿 FACING 方向的偏移量（格，正=发射方向），供渲染器做蓄力/弹出动画。
	 * <p>单位均为像素（1 格 = 16 像素）。</p>
	 * <ul>
	 *   <li><b>普通模式</b>：蓄力期间向传动轴方向（-FACING）线性收缩 1.5 像素；
	 *       发射瞬间触发弹簧弹出（0.2 秒）：从收缩位置迅速冲出到前方 1 像素 → 过冲至 1.5 像素 →
	 *       回落到原位，与能量波射出同步，随后开始下一轮收缩；</li>
	 *   <li><b>储存模式</b>（{@link #isStoringMode()}）：快门<b>不随每轮蓄力播放动画</b>——
	 *       静止在收缩位 -1.5px（蓄能待机）；仅在右键释放（{@code releaseTicks > 0}）时
	 *       做一次"弹出→收回"动画（{@link #RELEASE_TICKS}），连续释放会重置计时保持平滑。</li>
	 * </ul> */
	public float getShutterOffset(float partialTicks) {
		float speed = Math.abs(getSpeed());
		if (speed <= 0)
			return 0;
		float px = 1f / 16f;

		// 储存模式：静止收缩位 + 释放弹出动画
		if (isStoringMode()) {
			if (releaseTicks > 0) {
				float t = Mth.clamp((RELEASE_TICKS - releaseTicks + partialTicks) / RELEASE_TICKS, 0, 1);
				if (t < 0.4f)
					return px * (-1.5f + (t / 0.4f) * 2.5f); // -1.5px → +1.0px 弹出
				if (t < 0.6f)
					return px * (1f + ((t - 0.4f) / 0.2f) * 0.5f); // 过冲至 +1.5px
				return px * (1.5f - ((t - 0.6f) / 0.4f) * 3.0f); // 回收到静止收缩位 -1.5px
			}
			// 蓄能静止：始终停在收缩位（不发每轮动画）
			return -1.5f * px;
		}

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

	/** 储存模式释放弹出动画时长（tick）。 */
	protected static final int RELEASE_TICKS = 5;

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.putInt("ChargeTicks", chargeTicks);
		compound.putInt("ReleaseTicks", releaseTicks);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		int newCharge = compound.getInt("ChargeTicks");
		int newRelease = compound.getInt("ReleaseTicks");
		// 客户端：普通模式收到 ChargeTicks 高位归零（发射瞬间）触发弹出动画；
		// 储存模式仅在收到"满值释放标记"(=RELEASE_TICKS，服务端 fireBurst 那一拍)时触发
		// 释放弹出动画——服务端随后逐 tick 递减，避免每包重置导致快门鬼畜。
		if (clientPacket && level != null && level.isClientSide) {
			if (isStoringMode()) {
				if (newRelease >= RELEASE_TICKS)
					releaseTicks = RELEASE_TICKS;
			} else if (newCharge == 0 && chargeTicks > 0) {
				popTicks = POP_TICKS;
			}
		}
		chargeTicks = newCharge;
		// 服务端读档恢复；客户端只认满值触发（动画进度由 tick 递减维护，不直接回填）
		if (!clientPacket)
			releaseTicks = newRelease;
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

	/**
	 * 护目镜状态行文字（按当前充能态）：翡翠 0~3；蓝宝石子类覆写支持 4=伊普西龙/5=欧米伽。
	 *
	 * @param mode 当前充能态（blockstate MODE）
	 * @param max  Create 配置的转速上限（默认 256），用于显示档位 RPM 区间
	 */
	protected Component getStateName(int mode, int max) {
		return switch (mode) {
			case 1 -> Component.translatable("createoreexpansion.goggles.charger_low", max / 2);
			case 2 -> Component.translatable("createoreexpansion.goggles.charger_high", max / 2 + 1, max - 1);
			case 3 -> Component.translatable("createoreexpansion.goggles.charger_gamma", max);
			default -> Component.translatable("createoreexpansion.goggles.charger_idle");
		};
	}

	/** 护目镜标题（如"翡翠应力充能器"） */
	protected abstract Component getMachineName();

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		// 行排版学习 Create Ore Excavation：面板正文统一缩进（GoggleUtil）；能量条保持本模组原样
		GoggleUtil.forGoggles(tooltip, getMachineName().copy()
			.withStyle(ChatFormatting.GRAY));
		added = true;

		int mode = getBlockState().getValue(AbstractCreateChargerBlock.MODE);
		// 档位区间按 Create 配置的转速上限（maxRotationSpeed，默认 256）动态计算后作为参数传入，
		// 别人修改上限配置时护目镜显示的 RPM 区间自动跟随（不硬编码数值）
		int max = AllConfigs.server().kinetics.maxRotationSpeed.get();
		GoggleUtil.forGoggles(tooltip, getStateName(mode, max).copy()
			.withStyle(getGoggleColor(mode)));

		// 蓄力进度能量条（复用工具能量条 BarTooltipRender，填充色=当前充能态颜色）
		// 发射弹出动画期间（popTicks>0）显示满格，与能量波射出瞬间同步。
		// 推进逻辑保持本模组实现；行位置与其它面板正文对齐（补 Create 4 空格缩进）
		if (Math.abs(getSpeed()) > 0) {
			int intervalTicks = getIntervalTicks();
			int fillRgb = getWaveColor(mode);
			Color fill = new Color(fillRgb);
			int displayTicks = popTicks > 0 ? intervalTicks : chargeTicks;
			int filled = (int) ((long) displayTicks * 10 / intervalTicks);
			GoggleUtil.forGoggles(tooltip,
				(net.minecraft.network.chat.MutableComponent) BarTooltipRender.energy(filled, 10, 10, fill));
		}
		return added;
	}
}
