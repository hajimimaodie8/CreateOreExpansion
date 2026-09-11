package com.hjmmd_8.createoreexpansion.content.charger.block;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 蓝宝石应力充能器方块实体：蓝宝石科技线专属充能器。
 *
 * <p><b>与翡翠的区别</b>：</p>
 * <ul>
 *   <li><b>可蓄至 4/5 级</b>：普通模式按转速映射 1~4 级波（转速越高等级越高），
 *       满转速（≥ 上限 RPM）蓄力为 5 级欧米伽波（发射 {@link ChargerWaveEntity}）；</li>
 *   <li><b>发射间隔更快</b>：转速 1 → 6 秒，满转速 → 0.5 秒（翡翠为 10 秒 ~ 1 秒）；</li>
 *   <li><b>储存模式</b>（{@link #isStoringMode()}，默认 false = 普通模式）：持续输入应力时
 *       把应力储存为能量（不逐发发射），蓄满后等待玩家点击 / 红石信号触发一簇（多连发）能量波
 *       输出（触发输入见 Block 交互 / 红石）。</li>
 * </ul>
 *
 * <p><b>模式选择</b>：仿黄铜隧道模式槽——机器水平侧面出现可交互 ValueBox
 * （{@link SapphireChargerModeSlot}，瞄准按住右键 / 滚轮切换），由
 * {@link SapphireChargerMode} 枚举表达（普通 ⇄ 储存），取代旧 Shift+右键交互。</p>
 */
public class SapphireStressChargerBlockEntity extends AbstractCreateChargerBlockEntity {

	/** 模式：false = 普通模式（默认，与翡翠同思路但更快的连续发射）；true = 储存模式 */
	private boolean storingMode;

	/** 模式选择（ValueBox 交互槽，仿黄铜隧道）：值 0/1 ↔ {@link SapphireChargerMode} NORMAL/STORE。 */
	protected ScrollOptionBehaviour<SapphireChargerMode> modeSelection;

	/**
	 * 储存模式<b>已充能层数</b>（0 ~ {@link #getStoreMaxLayers()}，上限由配置
	 * {@code charger.sapphireMaxLayers} 给出，默认 10）：
	 * 每次充能完成（一个发射间隔）层数 +1，<b>满层后不再充能</b>（等玩家释放）。
	 * 每层 = 一次可释放的能量（右键一次放一层 = 一个能量波）。
	 */
	private int storeLayers;

	/** 攒层时的转速等级（释放该层时使用同一等级；取最近一次充能时的档位）。 */
	private int storedLevel = WaveLevels.GAMMA;

	/** 上一 tick 红石信号（上升沿检测：false→true 触发一次释放）。 */
	private boolean prevRedstone;

	public SapphireStressChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 注册模式选择 ValueBox（仿黄铜隧道模式槽，附着水平侧面）。 */
	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		modeSelection = new ScrollOptionBehaviour<>(SapphireChargerMode.class,
			Component.translatable("createoreexpansion.charger.mode"), this, new SapphireChargerModeSlot());
		modeSelection.between(0, SapphireChargerMode.values().length - 1);
		modeSelection.value = storingMode ? SapphireChargerMode.STORE.ordinal() : SapphireChargerMode.NORMAL.ordinal();
		// 玩家通过 ValueBox 调整模式 → 写入 storingMode（服务端由 ValueSettingsPacket 驱动）
		modeSelection.withCallback(this::onModeChanged);
		behaviours.add(modeSelection);
	}

	/** 模式槽回调：0=普通 / 1=储存（退出储存清空已攒层数，与旧 toggleMode 语义一致）。 */
	private void onModeChanged(Integer mode) {
		boolean storing = mode != null && mode == SapphireChargerMode.STORE.ordinal();
		if (storingMode == storing)
			return;
		storingMode = storing;
		if (!storingMode)
			storeLayers = 0;
		notifyUpdate();
	}

	/** 是否储存模式。 */
	@Override
	public boolean isStoringMode() {
		return storingMode;
	}

	/** 切换普通/储存模式（由模式槽/红石驱动；退出储存模式时清空已攒能量）。 */
	public boolean toggleMode() {
		storingMode = !storingMode;
		if (!storingMode)
			storeLayers = 0;
		if (modeSelection != null)
			modeSelection.setValue(storingMode ? SapphireChargerMode.STORE.ordinal() : SapphireChargerMode.NORMAL.ordinal());
		notifyUpdate();
		return storingMode;
	}

	/** 已充能层数（0 ~ {@link #getStoreMaxLayers()}）。 */
	public int getStoreLayers() {
		return storeLayers;
	}

	/**
	 * 层数上限（配置 {@code charger.sapphireMaxLayers}，默认 10）。
	 *
	 * <p><b>可外部自定义</b>：数值取自公共配置 {@code createoreexpansion-common.toml}，
	 * 与星辉石充能器分开配置；改动后（重载/重启）机器容量立即跟随，客户端护目镜读同一份配置，
	 * 显示保持一致。配置尚未加载时用 {@link AllConfig#sapphireMaxStoreLayers} 的初值兜底。</p>
	 */
	@Override
	public int getStoreMaxLayers() {
		return Math.max(1, AllConfig.sapphireMaxStoreLayers);
	}

	/** 是否已满（不能再充）。 */
	public boolean isStoreFull() {
		return storeLayers >= getStoreMaxLayers();
	}

	/** 是否还有可释放的层（右键/红石可触发释放）。 */
	public boolean hasLayerToRelease() {
		return storeLayers > 0;
	}

	/**
	 * 红石信号变化（由 Block.neighborChanged 或本 BE tick 轮询调用）：
	 * 储存模式且收到上升沿时释放一层（同右键逐层释放语义）。
	 */
	public void onRedstoneSignal(boolean signal) {
		if (level != null && !level.isClientSide) {
			boolean rising = signal && !prevRedstone;
			prevRedstone = signal;
			if (rising && storingMode)
				releaseOneLayer();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide)
			onRedstoneSignal(level.hasNeighborSignal(worldPosition));
	}

	/**
	 * 充能完成钩子（间隔期满）：
	 * 普通模式 → 父类逐发发射；储存模式 → <b>层数 +1</b>（每充一次即一层），
	 * 满 10 层后不再充能（多余充能丢弃，等玩家右键释放腾出空间）。
	 */
	@Override
	protected void onChargeComplete() {
		if (storingMode) {
			if (storeLayers < getStoreMaxLayers()) {
				storeLayers++;
				storedLevel = getBlockState().getValue(AbstractCreateChargerBlock.MODE);
				sendData();
			}
			// 已满：丢弃本次充能（无动作，快门保持静止）
		} else {
			super.onChargeComplete();
		}
	}

	/**
	 * 释放一层（点击/红石触发）：层数 -1，打出<b>一个</b>该层等级的能量波。
	 * 每次释放触发一次弹出动画（客户端 read 收到 ReleaseTicks 置满，连续释放平滑衔接）。
	 */
	public void releaseOneLayer() {
		if (level != null && !level.isClientSide) {
			if (storeLayers > 0) {
				storeLayers--;
				int level = Mth.clamp(storedLevel, 1, 5);
				if (storeLayers <= 0)
					storedLevel = WaveLevels.GAMMA;
				// 释放弹出动画（服务端仅作同步源，逐 tick 归零防鬼畜）
				releaseTicks = RELEASE_TICKS;
				sendData();
				launchWave(level);
			}
		}
	}

	/** 兼容旧调用名（Block 交互仍用）：释放一层。 */
	public void fireBurst() {
		releaseOneLayer();
	}

	/** 发射间隔：转速 1 RPM → 6 秒，255+ RPM → 0.5 秒（线性）。 */
	@Override
	protected int getIntervalTicks() {
		float rpm = Math.abs(getSpeed());
		float interval = Mth.clamp(6.0F - 5.5F * (rpm - 1.0F) / 254.0F, 0.5F, 6.0F);
		return Math.max(1, Math.round(interval * 20.0F));
	}

	/**
	 * 转速 → 波等级（普通模式）：上限转速取 Create 配置 maxRotationSpeed（默认 256，可自定义）：
	 * 0 → 未接入（0）；≥ 上限 → 欧米伽（5）；否则 1 ~ 上限-1 RPM 在 1~4 级间均匀分档
	 * （与 {@link #getStateName} 显示的档位区间同源，不会出现提示与实际不符）。
	 */
	@Override
	protected int getModeForSpeed() {
		float speed = Math.abs(getSpeed());
		int max = AllConfigs.server().kinetics.maxRotationSpeed.get();
		if (speed <= 0.0F)
			return 0;
		if (speed >= max)
			return 5;
		int tier = (int) Math.floor((speed - 1.0F) / (max - 1.0F) * 4.0F) + 1;
		return Mth.clamp(tier, 1, 4);
	}

	/** 发射：与翡翠充能器共用同一通用能量波实体（颜色按波等级统一）。 */
	@Override
	protected ChargerWaveEntity createWave(Level level, Vec3 start, Vec3 movementDir, int mode) {
		return new ChargerWaveEntity(level, start, movementDir, mode);
	}

	/** 指示灯/护目镜颜色（ARGB）：1 黄、2 绿、3 蓝、4 紫粉、5 玫红 */
	@Override
	protected int getWaveColor(int mode) {
		return switch (mode) {
			case 1 -> 0xFFFF55; // 低：黄
			case 2 -> 0x55FF55; // 高：绿
			case 3 -> 0x5555FF; // 伽马：蓝
			case 4 -> 0xFF66E0; // 伊普西龙：紫粉
			case 5 -> 0xFF4073; // 欧米伽：玫红（主体红，偏粉紫）
			default -> 0xAAAAAA;
		};
	}

	@Override
	protected Component getMachineName() {
		return Component.translatable("createoreexpansion.goggles.sapphire_charger");
	}

	/** 护目镜文案颜色：4/5 级分别用亮紫/红。 */
	@Override
	protected ChatFormatting getGoggleColor(int mode) {
		return switch (mode) {
			case 4 -> ChatFormatting.LIGHT_PURPLE;
			case 5 -> ChatFormatting.RED;
			default -> super.getGoggleColor(mode);
		};
	}

	/**
	 * 档位文案（含 RPM 区间）：区间边界与 {@link #getModeForSpeed} 同源（按 Create
	 * maxRotationSpeed 上限动态计算，默认 256），修改上限配置后提示自动跟随；
	 * 4=超载充能态、5=终极充能态（波本身仍叫伊普西龙/欧米伽）。
	 */
	@Override
	protected Component getStateName(int mode, int max) {
		return switch (mode) {
			case 1 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_1", 1, tierHi(1, max));
			case 2 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_2", tierLo(2, max), tierHi(2, max));
			case 3 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_3", tierLo(3, max), tierHi(3, max));
			case 4 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_4", tierLo(4, max), max - 1);
			case 5 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_5", max);
			default -> Component.translatable("createoreexpansion.goggles.charger_idle");
		};
	}

	/** 档位 RPM 区间下界（与 {@link #getModeForSpeed} 同一分档公式；mode 1 的下界固定为 1）。 */
	private static int tierLo(int mode, int max) {
		return 1 + (int) Math.floor((max - 1d) * (mode - 1) / 4d) + 1;
	}

	/** 档位 RPM 区间上界 = 下一档下界 - 1（mode 4 的上界 = max - 1）。 */
	private static int tierHi(int mode, int max) {
		return tierLo(mode + 1, max) - 1;
	}

	/** 护目镜 tooltip：附加当前模式（普通/储存）与储存模式下的充能进度（行排版统一缩进）。 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		GoggleUtil.forGoggles(tooltip, Component.translatable(
			storingMode ? "createoreexpansion.goggles.sapphire_charger_storing" : "createoreexpansion.goggles.sapphire_charger_normal")
			.withStyle(storingMode ? ChatFormatting.BLUE : ChatFormatting.GRAY));
		if (storingMode) {
			// 充能层数（x/上限 文字 + 能量条）：每充一次能 +1 层，满层不再充，右键一次放一层
			int maxLayers = getStoreMaxLayers();
			int filled = (int) ((long) storeLayers * 10L / maxLayers);
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.sapphire_charger_store_layers",
				storeLayers, maxLayers)
				.withStyle(ChatFormatting.GRAY));
			GoggleUtil.forGoggles(tooltip,
				(net.minecraft.network.chat.MutableComponent) BarTooltipRender.energy(filled, 10, 10, fillColor()));
			if (isStoreFull())
				GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.sapphire_charger_store_full")
					.withStyle(ChatFormatting.GOLD));
		}
		return true;
	}

	/** 进度条填充色：当前档位波色。 */
	private java.awt.Color fillColor() {
		return new java.awt.Color(getWaveColor(getBlockState().getValue(AbstractCreateChargerBlock.MODE)));
	}

	@Override
	public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.putBoolean("StoringMode", storingMode);
		compound.putInt("StoreLayers", storeLayers);
		compound.putInt("StoredLevel", storedLevel);
	}

	@Override
	protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		storingMode = compound.getBoolean("StoringMode");
		// 兼容旧档迁移：旧版 StoreLayers 直接沿用；旧版 StoreProgress（已删字段）折算——
		// 每满 10 进度折 1 层；更早的 StoredCharge（满 200 视为 1 层）同样兜底。
		// 上限按配置取（改大上限后旧档层数照读，改小则钳到新上限）。
		int maxLayers = getStoreMaxLayers();
		if (compound.contains("StoreLayers")) {
			storeLayers = Mth.clamp(compound.getInt("StoreLayers"), 0, maxLayers);
		} else {
			int progress = compound.getInt("StoreProgress");
			int old = compound.getBoolean("StoringMode") && compound.getInt("StoredCharge") > 0 ? 1 : 0;
			storeLayers = Mth.clamp(progress / 10 + old, 0, maxLayers);
		}
		storedLevel = Mth.clamp(compound.getInt("StoredLevel"), 1, 5);
		// 模式槽值与 storingMode 对齐（读档后 behaviour 的 NBT 已由 super 读取；
		// 以旧版 StoringMode 为准回填，保证新档/旧档/客户端三方一致）
		if (modeSelection != null) {
			int ordinal = storingMode ? SapphireChargerMode.STORE.ordinal() : SapphireChargerMode.NORMAL.ordinal();
			if (modeSelection.value != ordinal)
				modeSelection.value = ordinal;
		}
	}
}
