package com.hjmmd_8.createoreexpansion.content.charger.block;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 星辉石应力充能器方块实体：蓝宝石充能器的复制变体，发射等级改为<b>手动指定</b>。
 *
 * <p><b>与蓝宝石的区别</b>（其它充能/储存/释放逻辑沿用蓝宝石实现）：</p>
 * <ul>
 *   <li><b>发射等级手动固定</b>：{@link #getModeForSpeed()} 不再按转速分档，
 *       直接返回等级槽的数值（1~5，与转速无关；接入应力即按该等级蓄力/发射）；</li>
 *   <li><b>双槽交互</b>：机器左右两侧各一个 ValueBox——
 *       {@link StellarstoneChargerModeSlot}（模式，普通 ⇄ 储存，沿用 {@link SapphireChargerMode}）
 *       与 {@link StellarstoneChargerLevelSlot}（手动发射波等级 1~5 整数滚动），
 *       分别贴在不同侧面（详见各槽 {@code isSideActive} 注释）；</li>
 *   <li><b>储存模式释放层</b>：每层均按手动等级发射（攒层时记录当时的手动等级）。</li>
 * </ul>
 */
public class StellarstoneStressChargerBlockEntity extends AbstractCreateChargerBlockEntity {

	/** 模式：false = 普通模式（默认）；true = 储存模式（攒层 → 右键/红石逐层释放） */
	private boolean storingMode;

	/** 模式槽（ValueBox，值 0/1 ↔ {@link SapphireChargerMode} NORMAL/STORE），挂在模式槽位。 */
	protected ScrollOptionBehaviour<SapphireChargerMode> modeSelection;

	/** 手动发射波等级槽（ValueBox，整数 1~5），挂在等级槽位（与模式槽相对的另一侧）。 */
	protected StellarstoneChargerLevelScrollBehaviour levelSelection;

	/** 储存模式已充能层数（0 ~ {@link #getStoreMaxLayers()}，上限由配置给出，默认 20），每充一次 +1。 */
	private int storeLayers;

	/** 攒层时的发射等级（释放该层时使用；取最近一次充能时的手动等级）。 */
	private int storedLevel = WaveLevels.GAMMA;

	/** 上一 tick 红石信号（上升沿检测：false→true 触发一次释放）。 */
	private boolean prevRedstone;

	public StellarstoneStressChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		// —— 槽 1：模式（普通/储存，沿用 SapphireChargerMode；蓝宝石同款逻辑） ——
		modeSelection = new ScrollOptionBehaviour<>(SapphireChargerMode.class,
			Component.translatable("createoreexpansion.charger.mode"), this, new StellarstoneChargerModeSlot());
		modeSelection.between(0, SapphireChargerMode.values().length - 1);
		modeSelection.value = storingMode ? SapphireChargerMode.STORE.ordinal() : SapphireChargerMode.NORMAL.ordinal();
		modeSelection.withCallback(this::onModeChanged);
		behaviours.add(modeSelection);

		// —— 槽 2：手动发射波等级（1~5 整数滚动；独立 BehaviourType/NBT 键，可与模式槽并存） ——
		levelSelection = new StellarstoneChargerLevelScrollBehaviour(
			Component.translatable("createoreexpansion.charger.manual_level"), this, new StellarstoneChargerLevelSlot());
		levelSelection.value = WaveLevels.GAMMA; // 默认 3（伽马）
		behaviours.add(levelSelection);
	}

	/** 模式槽回调：0=普通 / 1=储存（退出储存清空已攒层数，与蓝宝石语义一致）。 */
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

	/** 当前手动发射波等级（1~5；槽未就绪时回退伽马 3）。 */
	public int getManualLevel() {
		if (levelSelection == null)
			return WaveLevels.GAMMA;
		return Mth.clamp(levelSelection.getValue(), WaveLevels.LOW, WaveLevels.MAX_LEVEL);
	}

	/** 已充能层数（0 ~ {@link #getStoreMaxLayers()}）。 */
	public int getStoreLayers() {
		return storeLayers;
	}

	/**
	 * 层数上限（配置 {@code charger.stellarstoneMaxLayers}，<b>默认 20</b>）。
	 *
	 * <p><b>可外部自定义</b>：数值取自公共配置 {@code createoreexpansion-common.toml}，
	 * 与蓝宝石充能器分开配置；改动后机器容量立即跟随，护目镜 {@code 充能层数：x/上限} 与能量条
	 * 都按该值显示。配置尚未加载时用 {@link AllConfig#stellarstoneMaxStoreLayers} 的初值兜底。</p>
	 */
	@Override
	public int getStoreMaxLayers() {
		return Math.max(1, AllConfig.stellarstoneMaxStoreLayers);
	}

	/** 是否已满（不能再充）。 */
	public boolean isStoreFull() {
		return storeLayers >= getStoreMaxLayers();
	}

	/** 是否还有可释放的层（右键/红石可触发释放）。 */
	public boolean hasLayerToRelease() {
		return storeLayers > 0;
	}

	/** 红石信号变化（由 Block.neighborChanged 或本 BE tick 轮询调用）：储存模式且收到上升沿时释放一层。 */
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
	 * 普通模式 → 父类逐发发射（按手动等级）；储存模式 → 层数 +1，并记录当时的手动等级。
	 */
	@Override
	protected void onChargeComplete() {
		if (storingMode) {
			if (storeLayers < getStoreMaxLayers()) {
				storeLayers++;
				storedLevel = getManualLevel();
				sendData();
			}
			// 已满：丢弃本次充能（无动作，快门保持静止）
		} else {
			super.onChargeComplete();
		}
	}

	/**
	 * 释放一层（点击/红石触发）：层数 -1，打出<b>一个</b>该层等级（手动等级）的能量波。
	 */
	public void releaseOneLayer() {
		if (level != null && !level.isClientSide) {
			if (storeLayers > 0) {
				storeLayers--;
				int level = Mth.clamp(storedLevel, 1, 5);
				if (storeLayers <= 0)
					storedLevel = WaveLevels.GAMMA;
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

	/** 发射间隔：转速 1 RPM → 6 秒，255+ RPM → 0.5 秒（线性，同蓝宝石）。 */
	@Override
	protected int getIntervalTicks() {
		float rpm = Math.abs(getSpeed());
		float interval = Mth.clamp(6.0F - 5.5F * (rpm - 1.0F) / 254.0F, 0.5F, 6.0F);
		return Math.max(1, Math.round(interval * 20.0F));
	}

	/**
	 * 转速 → 波等级：<b>直接返回手动发射波等级槽值</b>（1~5 固定，与转速无关）。
	 * 未接入应力（转速 ≤ 0）→ 0（父类据此显示待机、应力计耗为 0）。
	 */
	@Override
	protected int getModeForSpeed() {
		if (Math.abs(getSpeed()) <= 0.0F)
			return 0;
		return getManualLevel();
	}

	/** 发射：与翡翠/蓝宝石充能器共用同一通用能量波实体（颜色按波等级统一）。 */
	@Override
	protected ChargerWaveEntity createWave(Level level, Vec3 start, Vec3 movementDir, int mode) {
		return new ChargerWaveEntity(level, start, movementDir, mode);
	}

	/** 指示灯/护目镜颜色（ARGB）：1 黄、2 绿、3 蓝、4 紫粉、5 玫红（同蓝宝石配色）。 */
	@Override
	protected int getWaveColor(int mode) {
		return switch (mode) {
			case 1 -> 0xFFFF55; // 低：黄
			case 2 -> 0x55FF55; // 高：绿
			case 3 -> 0x5555FF; // 伽马：蓝
			case 4 -> 0xFF66E0; // 伊普西龙：紫粉
			case 5 -> 0xFF4073; // 欧米伽：玫红
			default -> 0xAAAAAA;
		};
	}

	@Override
	protected Component getMachineName() {
		return Component.translatable("createoreexpansion.goggles.stellarstone_charger");
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
	 * 档位文案：星辉石充能器等级固定为手动槽数值，不显示 RPM 区间——
	 * 直接提示当前手动发射波等级（0 = 未接入应力，复用公共 idle 文案）。
	 */
	@Override
	protected Component getStateName(int mode, int max) {
		if (mode <= 0)
			return Component.translatable("createoreexpansion.goggles.charger_idle");
		return Component.translatable("createoreexpansion.goggles.stellarstone_charger_manual_level", mode);
	}

	/** 护目镜 tooltip：附加当前模式（普通/储存）与储存模式下的充能进度（排版同蓝宝石）。 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		GoggleUtil.forGoggles(tooltip, Component.translatable(
			storingMode ? "createoreexpansion.goggles.stellarstone_charger_storing" : "createoreexpansion.goggles.stellarstone_charger_normal")
			.withStyle(storingMode ? ChatFormatting.BLUE : ChatFormatting.GRAY));
		if (storingMode) {
			int maxLayers = getStoreMaxLayers();
			int filled = (int) ((long) storeLayers * 10L / maxLayers);
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellarstone_charger_store_layers",
				storeLayers, maxLayers)
				.withStyle(ChatFormatting.GRAY));
			GoggleUtil.forGoggles(tooltip,
				(net.minecraft.network.chat.MutableComponent) BarTooltipRender.energy(filled, 10, 10, fillColor()));
			if (isStoreFull())
				GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellarstone_charger_store_full")
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
		// 上限按配置取（默认 20）：改大上限后旧档层数照读，改小则钳到新上限
		storeLayers = Mth.clamp(compound.getInt("StoreLayers"), 0, getStoreMaxLayers());
		storedLevel = Mth.clamp(compound.getInt("StoredLevel"), 1, 5);
		// 模式槽值与 storingMode 对齐（读档后 behaviour 的 NBT 已由 super 读取；
		// 以 StoringMode 为准回填，保证新档/旧档/客户端三方一致）
		if (modeSelection != null) {
			int ordinal = storingMode ? SapphireChargerMode.STORE.ordinal() : SapphireChargerMode.NORMAL.ordinal();
			if (modeSelection.value != ordinal)
				modeSelection.value = ordinal;
		}
	}

	// ===================================================================================
	//  双槽 ValueBox 变换（几何/贴面参数沿用 SapphireChargerModeSlot 已调准数值）
	// ===================================================================================

	/**
	 * 模式槽位（ValueBoxTransform.Sided）：<b>渲染几何完全复用蓝宝石
	 * {@link SapphireChargerModeSlot}（已调准）</b>，仅把可交互面收窄到一台机器的一个侧面：
	 * <ul>
	 *   <li>水平放置：FACING 的 {@code getCounterClockWise()} 侧（FACING 逆时针 90° 的水平侧面，即规格 rotateYCCW）；</li>
	 *   <li>竖直放置（FACING 轴 Y）：固定东（EAST）侧。</li>
	 * </ul>
	 */
	public static class StellarstoneChargerModeSlot extends SapphireChargerModeSlot {

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			if (!direction.getAxis()
				.isHorizontal())
				return false;
			Direction facing = state.getValue(BlockStateProperties.FACING);
			if (facing.getAxis() == Axis.Y)
				// 竖直：模式槽只保留东侧
				return direction == Direction.EAST;
			// 水平：模式槽只保留 FACING 逆时针一侧（getCounterClockWise = 规格中的 rotateYCCW）
			return direction == facing.getCounterClockWise();
		}
	}

	/**
	 * 手动发射波等级槽位（ValueBoxTransform.Sided）：与模式槽共用蓝宝石
	 * {@link SapphireChargerModeSlot} 的渲染几何，只出现在<b>相对的另一侧</b>——
	 * <ul>
	 *   <li>水平放置：FACING 的 {@code getClockWise()} 侧（FACING 顺时针 90° 的水平侧面，即规格 rotateY）；</li>
	 *   <li>竖直放置（FACING 轴 Y）：固定西（WEST）侧。</li>
	 * </ul>
	 */
	public static class StellarstoneChargerLevelSlot extends StellarstoneChargerModeSlot {

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			if (!direction.getAxis()
				.isHorizontal())
				return false;
			Direction facing = state.getValue(BlockStateProperties.FACING);
			if (facing.getAxis() == Axis.Y)
				// 竖直：等级槽只保留西侧（与模式槽东侧相对）
				return direction == Direction.WEST;
			// 水平：等级槽只保留 FACING 顺时针一侧（getClockWise = 规格中的 rotateY）
			return direction == facing.getClockWise();
		}
	}

	/**
	 * 手动发射波等级滚动行为：整数 1~5（普通 Create ScrollValueBehaviour）。
	 *
	 * <p>需要独立的 {@link BehaviourType}（否则与模式槽 ScrollOptionBehaviour 同为
	 * {@code ScrollValueBehaviour.TYPE}，会在 BE behaviour 表中互相覆盖）与独立 NBT 键
	 * （"ManualLevelScroll"，避免与模式槽的 "ScrollValue" 冲突）；{@code netId()=1}
	 * 用于服务端 ValueSettingsPacket 区分两个滚动槽（模式槽默认 0）。</p>
	 *
	 * <p><b>面板定制（2026-09 修复）</b>：Create 的 {@code ScrollValueBehaviour.createBoard}
	 * 固定给"0..max 列 + 左侧硬编码英文 Value 行标"的面板——对我们的 1~5 级槽就是
	 * <b>多出一个永远选不中的 0 列</b>，且左侧行标是未翻译的 {@code Component.literal("Value")}
	 * （Create 自己的写法，任何 ScrollValueBehaviour 都这样）。这里覆写三处对齐语义：</p>
	 * <ul>
	 *   <li>{@link #createBoard}：板宽 = 4（列 0..4 → 等级 1..5），行标走词条
	 *       {@code createoreexpansion.charger.level_row}，数值显示 = 列 + 1；</li>
	 *   <li>{@link #getValueSettings}：把内部等级换算成"列"（value − 1）回给面板，光标初始位置才对；</li>
	 *   <li>{@link #setValueSettings}：把面板选中的列换回等级（+1）再写入，并沿用 Create 的反馈音。</li>
	 * </ul>
	 */
	public static class StellarstoneChargerLevelScrollBehaviour extends ScrollValueBehaviour {

		public static final BehaviourType<StellarstoneChargerLevelScrollBehaviour> TYPE = new BehaviourType<>();

		public StellarstoneChargerLevelScrollBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
			super(label, be, slot);
			between(WaveLevels.LOW, WaveLevels.MAX_LEVEL);
		}

		@Override
		public BehaviourType<?> getType() {
			return TYPE;
		}

		@Override
		public int netId() {
			return 1;
		}

		/** 面板：列 0..(MAX−LOW)。左行标用本模组词条，数值显示为"等级"(= 列 + LOW)。 */
		@Override
		public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
			return new ValueSettingsBoard(label, WaveLevels.MAX_LEVEL - WaveLevels.LOW, 10,
				com.google.common.collect.ImmutableList.of(
					Component.translatable("createoreexpansion.charger.level_row")),
				new ValueSettingsFormatter(settings -> Component.literal(
					String.valueOf(settings.value() + WaveLevels.LOW))));
		}

		/** 内部等级(1~5) → 面板列(0~4)：光标初始落点与当前等级一致。 */
		@Override
		public ValueSettings getValueSettings() {
			return new ValueSettings(0, Mth.clamp(value, WaveLevels.LOW, WaveLevels.MAX_LEVEL) - WaveLevels.LOW);
		}

		/** 面板列(0~4) → 内部等级(1~5)：收口时再钳一次，杜绝越界。 */
		@Override
		public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
			int target = Mth.clamp(valueSetting.value() + WaveLevels.LOW, WaveLevels.LOW, WaveLevels.MAX_LEVEL);
			if (target == value)
				return;
			setValue(target);
			playFeedbackSound(this);
		}

		@Override
		public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
			nbt.putInt("ManualLevelScroll", value);
		}

		@Override
		public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
			if (nbt.contains("ManualLevelScroll"))
				value = Mth.clamp(nbt.getInt("ManualLevelScroll"), WaveLevels.LOW, WaveLevels.MAX_LEVEL);
		}
	}
}
