package com.hjmmd_8.createoreexpansion.content.charger.block;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.hjmmd_8.createoreexpansion.util.SpeedBands;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 *       满转速（≥ 上限 RPM）蓄力为 5 级 ω 波（发射 {@link ChargerWaveEntity}）；
 *       五档的转速区间来自 {@link #createSpeedTierBands(int)} 这一张表（默认上限 256 时
 *       α 1~64 / β 65~128 / γ 129~192 / ε 193~255 / ω ≥256），<b>判定与护目镜文案同表</b>；</li>
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

	/**
	 * "最后一次有效档位"（0/越界 = 无记录）：<b>只在停转时</b>作为释放档位的回落值
	 * （口径只有一处，见 {@link AbstractCreateChargerBlockEntity#resolveReleaseLevel}）。
	 * 有应力时释放一律取当前转速档，故它不参与"跟着转速变档"的路径。
	 */
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
	 *
	 * <p>攒层时顺手把当前档位记为"最后一次有效档位"：它<b>只影响停转后的释放</b>
	 * （有应力时释放一律取当前档位，见
	 * {@link AbstractCreateChargerBlockEntity#resolveReleaseLevel}）。</p>
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
				// 档位跟随当前转速（口径见 AbstractCreateChargerBlockEntity#resolveReleaseLevel）：
				// 调好档位后立刻释放用的就是新档位；停转时回落到最后一次有效档位，囤的层数照样放得出来
				int level = resolveReleaseLevel(storedLevel);
				storedLevel = level;
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
	 * <b>本机的转速档位表</b>（判定与显示的唯一来源）：五档 α/β/γ/ε/ω，档值 = 波等级（1~5），
	 * 下限由 {@link SpeedBands#equalSteps(float, float, int, int)} 现算并<b>向上取整为整数</b>——
	 * {@code equalSteps(1, max, 5, 1)} → {@code 1 / 1+⌈(max−1)/4⌉ / 1+⌈(max−1)/2⌉ / 1+⌈3(max−1)/4⌉ / max}
	 * （默认 max=256 时 = {@code 1 / 65 / 129 / 193 / 256}）。</p>
	 *
	 * <p><b>为什么是"向上取整"而不是四舍五入</b>：改造前的判定是
	 * {@code floor((speed−1)/(max−1)×4)+1}（分界是实数 {@code 1+(max−1)k/4}），
	 * 整数转速下等价于分界<b>向上取整</b>；取整方向一改，整数转速的档位就会跟着变，
	 * 而本档位决定 blockstate {@code MODE}（玩法侧），重构不允许。</p>
	 *
	 * <p><b>为什么不再各自算一遍</b>：改前本类有两套边界——{@code getModeForSpeed()} 一个 floor 公式、
	 * 显示用的 {@code tierLo/tierHi} 又一个；两者在 {@code (max−1)×k/4} 恰为整数时会差一格
	 * （{@code max=257} 的每个 k、{@code max=255} 的 k=2 都是），出现"提示 66 起、实际 65 就进第二档"
	 * 这类错位。现在判定读 {@code bands.valueAt(speed)}、显示读同一张 {@code bands} 的下限/上界，
	 * 结构上不可能再分家（表按 max 缓存，配置改了重建，见 {@link #speedTierBands(int)}）。</p>
	 */
	@Override
	protected SpeedBands createSpeedTierBands(int max) {
		return SpeedBands.equalSteps(1, max, WaveLevels.MAX_LEVEL - WaveLevels.LOW + 1, WaveLevels.LOW);
	}

	/** 发射：与翡翠充能器共用同一通用能量波实体（颜色按波等级统一）。 */
	@Override
	protected ChargerWaveEntity createWave(Level level, Vec3 start, Vec3 movementDir, int mode) {
		return new ChargerWaveEntity(level, start, movementDir, mode);
	}

	// 指示灯/护目镜颜色（ARGB）不再本类实现：与星辉石同用基类
	// AbstractCreateChargerBlockEntity#getWaveColor(int) 的标准 5 档表
	// （1=α 黄、2=β 绿、3=γ 蓝、4=ε 紫粉、5=ω 玫红，越界回落未接入灰）。

	@Override
	protected Component getMachineName() {
		return Component.translatable("createoreexpansion.goggles.sapphire_charger");
	}

	// 护目镜的单行文案颜色（getGoggleColor）不再本类覆写：本机有转速档位表，护目镜走"一档一行"，
	// 当前档用它的光芒色（getWaveColor）、其余暗灰；基类那份单行配色只服务"没有转速档位的机型"。

	/**
	 * 档位文案（含 RPM 区间）：区间边界<b>直读 {@link #speedTierBands(int)} 这一张表</b>
	 * （与 blockstate {@code MODE} 的判定同源，改 maxRotationSpeed 上限后判定与提示一起跟随）；
	 * 档位名一律"希腊字母 + 充能态"——α/β/γ/ε/ω 充能态（符号见
	 * {@link com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels#glyph(int)}）。
	 *
	 * <p>区间是<b>闭区间</b>：前四档写"下限~（下一档下限 − 1）"、末档写"≥下限"，
	 * 下界全是整数（表在构造时就量化过），所以文案里不再出现小数。</p>
	 */
	@Override
	protected Component getStateName(int mode, int max) {
		SpeedBands bands = speedTierBands(max);
		if (bands == null || mode < 1 || mode > bands.count())
			return Component.translatable("createoreexpansion.goggles.charger_idle");
		int index = mode - 1;
		String lo = SpeedBands.formatRpm(bands.lowerRpm(index));
		return switch (mode) {
			case 1 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_1", lo,
				SpeedBands.formatRpm(bands.upperInclusiveRpm(index)));
			case 2 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_2", lo,
				SpeedBands.formatRpm(bands.upperInclusiveRpm(index)));
			case 3 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_3", lo,
				SpeedBands.formatRpm(bands.upperInclusiveRpm(index)));
			case 4 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_4", lo,
				SpeedBands.formatRpm(bands.upperInclusiveRpm(index)));
			case 5 -> Component.translatable("createoreexpansion.goggles.sapphire_charger_5", lo);
			default -> Component.translatable("createoreexpansion.goggles.charger_idle");
		};
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
