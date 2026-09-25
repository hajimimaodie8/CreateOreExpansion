package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.hjmmd_8.createoreexpansion.util.SpeedBands;
import java.awt.Color;
import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
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
 *     <li>充能态解析：0=未接入应力；1=α（≤½ 上限转速）；2=β（½~上限转速）；3=γ（≥上限转速）；
 *         上限转速读取 Create 配置 {@code maxRotationSpeed}（默认 256 RPM，可调，不硬编码）；</li>
 *     <li>发射间隔：转速 1 → 10 秒，256 → 1 秒，线性插值；</li>
 *     <li>服务端蓄力计数并通过 {@code sendData} 同步进度给客户端（两端一致）；</li>
 *     <li>发射头蓄力收缩 / 发射弹出动画（{@link #getShutterOffset}）；</li>
 *     <li>护目镜蓄力能量条（复用工具能量条，填充色随充能态）。</li>
 * </ul>
 *
 * <p><b>档位的唯一来源 = 一张 {@link SpeedBands} 表</b>（2026-09 整理）：判定（{@link #getModeForSpeed()}）
 * 与显示（{@link #getStateName(int, int)} 的 RPM 区间、护目镜的逐档列举）都读
 * {@link #speedTierBands(int)}，<b>谁都不许另算一份</b>。基线的三档 α/β/γ 下限量化为整数
 * （{@code equalSteps(1, max, 3, 1)} → {@code 1 / max/2+1 / max}），区间文案写成闭区间
 * （非末档 {@code 下限~（下一档下限 − 1）}、末档 {@code ≥下限}）。</p>
 *
 * <p><b>护目镜的档位行</b>（2026-09 用户规格，与变器攻击态同一套方案）：<b>一档一行列出全部档位</b>，
 * 排在机器名之后；当前所处档用它的<b>光芒色</b>（{@link #getWaveColor(int)}，即指示灯颜色）高亮、
 * 其余档 {@link ChatFormatting#DARK_GRAY}；未接入应力（{@code MODE == 0}）时先给一行
 * {@code charger_idle}「未接入应力」，N 行档位照常列出但全部暗灰（与变器"未达门槛也列全档"一致）。</p>
 *
 * <p>子类（翡翠充能器/蓝宝石充能器/星辉石充能器）只需覆写：</p>
 * <ul>
 *     <li>{@link #createSpeedTierBands(int)} —— 本机的转速档位表（基线给翡翠的三档；<b>档位与转速
 *         无关的手动等级机型返回 {@code null}</b>，护目镜逐档列举随即退化成现状的单行）；</li>
 *     <li>{@link #createWave(Level, Vec3, Vec3, int)} —— 发射各自的能量波实体；</li>
 *     <li>{@link #getWaveColor(int)} —— 档位指示色（默认查 {@link WaveLevels#indicatorColor(int)}
 *         标准 5 档表，翡翠子类覆写为只认 α~γ；也是当前档档位行的高亮色）；</li>
 *     <li>{@link #getMachineName()} —— 护目镜标题；</li>
 *     <li>{@link #getGoggleColor(int)} —— 单行档位文案的颜色（只服务"没有转速档位"的机型，
 *         有档位表的机型逐档上色，用不到它）。</li>
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
	 * 储存模式最高充能层数（每层 = 一次可释放的能量波）。
	 *
	 * <p>基类给一个保守默认值；<b>具体机型覆写为读配置</b>——蓝宝石
	 * {@code charger.sapphireMaxLayers}（默认 10）、星辉石 {@code charger.stellarstoneMaxLayers}
	 * （默认 20），见 {@code common/AllConfig}。外部（护目镜/其它模组/整合包代码）也可以直接调用
	 * 本方法取到当前生效的上限。</p>
	 */
	public int getStoreMaxLayers() {
		return 10;
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
	 * 按转速解析充能态：0=无应力，1=α（≤½ 上限转速），2=β（½~上限转速），3=γ（≥上限转速）。
	 *
	 * <p><b>实现已不是内联三元，而是读分档表</b>（2026-09 整理）：判定与显示共用
	 * {@link #speedTierBands(int)} 这一个对象（{@link SpeedBands#valueAt(float)}），
	 * 本方法体内<b>一个阈值都没有</b>——改档数/边界只动 {@link #createSpeedTierBands(int)}。
	 * 档位按 Create 配置的转速上限 {@code maxRotationSpeed}（默认 256 RPM，可调）现算，
	 * 上限改掉时判定与文案一起跟随。</p>
	 *
	 * <p><b>本方法决定 blockstate {@code MODE}（玩法侧）</b>：重构只允许改"非整数转速"的归档
	 * （整数下限量化所致），<b>整数转速下的档位逐个不变</b>（{@code build/patch} 里的逐点对照程序为证）。</p>
	 */
	protected int getModeForSpeed() {
		float speed = Math.abs(getSpeed());
		if (speed <= 0)
			return 0;
		SpeedBands bands = speedTierBands(AllConfigs.server().kinetics.maxRotationSpeed.get());
		// 没有转速档位的机型（手动等级，如星辉石充能器）必须自行覆写本方法；这里返回 0 是"无档位"的兜底
		return bands == null ? 0 : bands.valueAt(speed);
	}

	// ================= 转速档位表（判定与显示的唯一来源，按 max 缓存） =================

	/** 档位表缓存对应的转速上限（{@link #speedTierBands(int)} 的重建判据）。 */
	private int cachedBandMax = Integer.MIN_VALUE;

	/** 按 {@link #cachedBandMax} 缓存下来的档位表（null = 本机档位与转速无关）。 */
	private SpeedBands cachedSpeedBands;

	/**
	 * <b>本机的转速档位表</b>（判定与显示的唯一来源）——按给定的转速上限现算一张表。
	 *
	 * <p>默认 = 基线的三档 α/β/γ：下限 {@code 1 / max/2+1 / max}（<b>整数</b>），档值 1/2/3
	 * （= 波等级，也就是 blockstate {@code MODE}）。用
	 * {@link SpeedBands#equalSteps(float, float, int, int)}（下限<b>向上取整</b>）而不是四舍五入，
	 * 是为了让整数转速下的归档与改造前逐点一致——{@code speed <= max/2 → 1}、{@code speed < max → 2}、
	 * 否则 3 这一套整数语义，等价于"分界 1 / max/2+1 / max"，而四舍五入会在某些 max 上挪动一格。</p>
	 *
	 * <p><b>返回 null 的机型</b>（档位与转速无关，例如手动等级槽的星辉石充能器）：护目镜的
	 * "逐档列举"这条路会退化成现状的<b>单行</b>（{@link #getStateName(int, int)} + {@link #getGoggleColor(int)}），
	 * 行为与文案一个字都不变。</p>
	 *
	 * <p>Create 的 {@code maxRotationSpeed} 配置最小值是 64（见 {@code CKinetics}），
	 * 所以 {@code 1 / max/2+1 / max} 恒为严格升序，不会触发 {@link SpeedBands} 的构造校验。</p>
	 */
	protected SpeedBands createSpeedTierBands(int max) {
		return SpeedBands.equalSteps(1, max, 3, WaveLevels.LOW);
	}

	/**
	 * 取本机的档位表（<b>按 {@code max} 缓存</b>，配置改了自动重建）。
	 *
	 * <p>判定（{@link #getModeForSpeed()}）、区间文案（{@link #getStateName(int, int)}）与护目镜的
	 * 逐档列举都走这里，读的是<b>同一个对象</b>：文案写"β 充能态（129~255 RPM）"时判定就不可能
	 * 把 128 算进第二档——那两件事读的是同一个 {@code lowerRpm}。</p>
	 *
	 * @param max Create 配置的转速上限（RPM）
	 * @return 本机的档位表；{@code null} = 本机档位与转速无关（见 {@link #createSpeedTierBands(int)}）
	 */
	protected final SpeedBands speedTierBands(int max) {
		if (max != cachedBandMax) {
			cachedSpeedBands = createSpeedTierBands(max);
			cachedBandMax = max;
		}
		return cachedSpeedBands;
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
	 * @param mode 能量波等级（1=α/2=β/3=γ/4=ε/5=ω）；供蓝宝石
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

	/** 未接入应力 / 越界档位的指示灯灰（ARGB）：标准 5 档表之外，本线唯一的固定配色。 */
	protected static final int IDLE_INDICATOR_COLOR = 0xAAAAAA;

	/**
	 * 档位指示色（ARGB）：查<b>标准 5 档表</b> {@link WaveLevels#indicatorColor(int)}
	 * （1=α 黄、2=β 绿、3=γ 蓝、4=ε 紫粉、5=ω 玫红），越界（0 = 未接入应力、坏数据）回落
	 * {@link #IDLE_INDICATOR_COLOR}。
	 *
	 * <p>颜色值本身只有 {@link WaveLevels#indicatorColor(int)} 一处实现；本方法只补"未接入/越界"
	 * 的回落色，供指示灯、护目镜能量条填充色与各子类共用。</p>
	 */
	protected static int indicatorColorForMode(int mode) {
		return WaveLevels.isValid(mode) ? WaveLevels.indicatorColor(mode) : IDLE_INDICATOR_COLOR;
	}

	/**
	 * 储存模式<b>释放档位的唯一解析处</b>：<b>以"当前档位"为准</b>，只有在没有应力
	 * （{@code MODE = 0}，本机停转）时才回落到 {@code rememberedLevel}（最后一次有效档位）。
	 *
	 * <p><b>为什么必须跟随当前档位</b>：普通模式的发射走 {@link #launchWave()}，它每发都读当前
	 * {@code MODE}；储存模式却曾经在"充能完成"时才抄一份档位，于是<b>同一台机器的两种模式对
	 * "波级从哪来"给了两个答案</b>——玩家把档位（蓝宝石=转速档、星辉石=手动等级槽）调好之后立刻
	 * 释放，打出来的还是调之前那一发，等下一次充能攒满才"跟上"（用户 2026-09-14 实测反馈：
	 * "第一次发射还是原来的波形，第二次才是改进后的"）。层数只是个计数，任何一层都没有"自己的等级"
	 * 可查（护目镜只显示 {@code 充能层数：x/y}），所以"每层记住充能当时的等级"在界面上既不可见、
	 * 也无法预期，跟随当前档位才与普通模式一致。</p>
	 *
	 * <p><b>为什么还要保留"最后一次有效档位"</b>：储存模式的用途是"先攒着、以后随时放"，
	 * 而档位是由<b>应力</b>推出来的（{@code MODE = 0} 表示未接入应力/停转）。若释放时死抠当前档位，
	 * 停转（含大退重开后没接轴）的机器就一发都放不出来——囤的层数全成摆设。故停转时回落到
	 * 记下来的最后一次有效档位；两者都无效（新机器、坏数据）才用 {@link WaveLevels#GAMMA} 兜底。</p>
	 *
	 * @param rememberedLevel 调用方保存的"最后一次有效档位"（可能为 0 / 越界 = 无记录）
	 * @return 本次释放应当使用的波等级（恒为 {@link WaveLevels#LOW}~{@link WaveLevels#MAX_LEVEL}）
	 */
	protected int resolveReleaseLevel(int rememberedLevel) {
		int current = getBlockState().getValue(AbstractCreateChargerBlock.MODE);
		if (WaveLevels.isValid(current))
			return current; // 有应力：以当前档位为准（与普通模式的发射口径一致）
		return WaveLevels.isValid(rememberedLevel) ? rememberedLevel : WaveLevels.GAMMA;
	}

	/**
	 * 当前充能档位（1=α/2=β/3=γ/4=ε/5=ω）对应的指示灯 / 护目镜颜色（ARGB）。
	 *
	 * <p>默认即 {@link #indicatorColorForMode(int)}：标准 5 档表 + 未接入灰。翡翠充能器只到 γ 档，
	 * 故其子类覆写为"仅 α~γ 有效"，把 ε/ω 也归入未接入灰。</p>
	 */
	protected int getWaveColor(int mode) {
		return indicatorColorForMode(mode);
	}

	/**
	 * <b>单行</b>档位文案的颜色（默认跟随粒子颜色）。
	 *
	 * <p><b>只有"没有转速档位"的机型用得到它</b>（{@link #createSpeedTierBands(int)} 返回 null，
	 * 护目镜退化回单行——见 {@link #addToGoggleTooltip}）；有档位表的机型逐档上色，
	 * 当前档用它的<b>光芒色</b>（{@link #getWaveColor(int)}）、其余暗灰，不经过本方法。</p>
	 */
	protected ChatFormatting getGoggleColor(int mode) {
		return switch (mode) {
			case 2 -> ChatFormatting.GREEN;
			case 3 -> ChatFormatting.AQUA;
			case 1 -> ChatFormatting.YELLOW;
			default -> ChatFormatting.GRAY;
		};
	}

	/**
	 * 护目镜档位行文字（按充能档位）：翡翠 α/β/γ；蓝宝石子类覆写支持 ε/ω。
	 *
	 * <p>RPM 区间<b>直读 {@link #speedTierBands(int)}</b>（与 {@link #getModeForSpeed()} 同一张表）：
	 * 非末档写"下限~（下一档下限 − 1）"的<b>闭区间</b>（{@link SpeedBands#upperInclusiveRpm(int)}），
	 * 末档写"≥下限"。下限已量化成整数，所以文案里不会再出现小数位。</p>
	 *
	 * @param mode 当前充能档位（blockstate MODE）
	 * @param max  Create 配置的转速上限（默认 256），用于现算档位 RPM 区间
	 */
	protected Component getStateName(int mode, int max) {
		SpeedBands bands = speedTierBands(max);
		if (bands == null || mode < 1 || mode > bands.count())
			return Component.translatable("createoreexpansion.goggles.charger_idle");
		int index = mode - 1;
		if (index == 0)
			// 首档：词条里已写死下界 1（= 本表首档下限）
			return Component.translatable("createoreexpansion.goggles.charger_low",
				SpeedBands.formatRpm(bands.upperInclusiveRpm(index)));
		if (index == bands.count() - 1)
			// 末档：≥下限（没有这样的上界，故不写"~ 上界"）
			return Component.translatable("createoreexpansion.goggles.charger_gamma",
				SpeedBands.formatRpm(bands.lowerRpm(index)));
		return Component.translatable("createoreexpansion.goggles.charger_high",
			SpeedBands.formatRpm(bands.lowerRpm(index)), SpeedBands.formatRpm(bands.upperInclusiveRpm(index)));
	}

	/** 护目镜标题（如"翡翠应力充能器"） */
	protected abstract Component getMachineName();

	/**
	 * <b>档位行</b>：当前所处档用它的<b>光芒色</b>（{@link #getWaveColor(int)} 的指示灯 ARGB 去掉
	 * alpha 后的 RGB）高亮，其余档 {@link ChatFormatting#DARK_GRAY}（用户 2026-09 规格：
	 * 充能器与变器攻击态同一套"一档一行、当前档高亮"方案，<b>只有高亮色不同</b>——
	 * 变器用白，充能器用该档自己的光芒色）。颜色是"我在哪档"的唯一标记，各行文案本身同构。
	 */
	private static MutableComponent tierLine(Component text, boolean current, int waveColor) {
		MutableComponent line = text.copy();
		return current
			? line.withStyle(style -> style.withColor(TextColor.fromRgb(waveColor & 0xFFFFFF)))
			: line.withStyle(ChatFormatting.DARK_GRAY);
	}

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
		// 判定与显示同源（见 speedTierBands）；null = 本机档位与转速无关 → 保持现状的单行
		SpeedBands bands = speedTierBands(max);
		if (bands == null) {
			GoggleUtil.forGoggles(tooltip, getStateName(mode, max).copy()
				.withStyle(getGoggleColor(mode)));
		} else {
			// 未接入应力：保留既有"未接入应力"行（颜色沿用旧的单行口径），档位行照常列全
			if (mode <= 0)
				GoggleUtil.forGoggles(tooltip, getStateName(0, max).copy()
					.withStyle(getGoggleColor(0)));
			// 一档一行列全部档位（排在机器名之后）：档值与档号都由分档表给出（档值 = 波等级 = MODE），
			// 当前档高亮成该档的光芒色、其余暗灰。循环上界 = 档数，所以改档数/改上限时行数自动跟随。
			for (int i = 0; i < bands.count(); i++) {
				int level = bands.valueOfIndex(i);
				GoggleUtil.forGoggles(tooltip,
					tierLine(getStateName(level, max), level == mode, getWaveColor(level)));
			}
		}

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
