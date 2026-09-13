package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * <b>星辉波变器的双处理模式</b>（枚举带行为 = 策略模式）。
 *
 * <p>存在意义：变器对波只有两件事——"把波加工成全能波"和"把波点燃成攻击波"。这两件事
 * 各自的完整行为（命中怎么处置、周边有没有场）都收在本枚举的常量体里，调用方只按<b>当前模式</b>
 * 取值分派：{@code WaveHitResolver} 只做 {@code switch (mode.onWaveHit(...))} 三选一，
 * 变器方块实体只在周期扫描里调一次 {@link #applyField}。<b>调用方不判断"是不是某种模式"</b>，
 * 以后再加模式（例如"吸收波"）只需在这里加一个常量，不改任何调用点。</p>
 *
 * <p><b>两态语义（用户定义）</b>：</p>
 * <ul>
 *   <li>{@link #PROCESSING} <b>加工波变态</b>（默认）：波口开着才让波穿过，穿波即把原波转成
 *       全能波（携带扫描属性与载荷，见 {@link StellarWaveTransmuterPass#tryConvert}）；
 *       入口未开或撞到不可穿的机壳面 → 按撞墙处理；入口开而对面出口关 → 原路遣返；</li>
 *   <li>{@link #ATTACK} <b>攻击波变态</b>：变器对波<b>完全透明</b>——不做穿波转换、不遣返、
 *       <b>面开关也不拦波</b>（波照常飞过去）；同时变器自身成为<b>攻击场</b>：读取半径内
 *       仍是普通波的波被点燃成攻击波（{@link WaveTypes#ATTACK}）。攻击波是纯攻击、不参与加工
 *       （加工路径已由波基类按波型闸门挡住，本模式不提供任何加工逻辑）。</li>
 * </ul>
 *
 * <p><b>与波口开关的区别</b>：模式 = "变器怎么处理波"，存在方块实体里，扳手右键切换
 * （NBT + 同步包，见 {@link StellarWaveTransmuterBlockEntity#cycleMode()}）；
 * 波口开关 = "哪一面让波进出"，存在 blockstate 里，空手右键切换
 * （见 {@link StellarWaveTransmuterBlock}）。两者互不干涉。</p>
 *
 * <p><b>中立性</b>：只用原版 / Create 基础类型，不 import 任何可选联动模组。</p>
 */
public enum TransmuterMode {

	/** 加工波变态（默认）：穿波转换，把路过变器的波变成携带加工能力的全能波。 */
	PROCESSING("processing", ChatFormatting.AQUA) {
		@Override
		public WaveOutcome onWaveHit(AbstractChargerWaveEntity wave, BlockPos pos) {
			// 穿波判定本身仍是一处实现（StellarWaveTransmuterPass，含开口三态与载荷抽取），
			// 这里只把它的三态结果翻译成本枚举的处置口径
			return switch (StellarWaveTransmuterPass.tryConvert(wave, pos)) {
				case CONVERTED, BOUNCED -> WaveOutcome.CONSUMED;
				case HIT_WALL -> WaveOutcome.BLOCKED;
			};
		}
	},

	/** 攻击波变态：对波透明 + 自身是攻击场（点燃读取半径内仍是普通波的波）。 */
	ATTACK("attack", ChatFormatting.RED) {
		@Override
		public WaveOutcome onWaveHit(AbstractChargerWaveEntity wave, BlockPos pos) {
			// 透明 = 什么都不做，而且**不做 setPos 位移**：防二次判定的推出方块外手法是为
			// "波被机器拦下"设计的（遣返/转向必须立刻离开方块盒，否则下一 tick 再判一次）；
			// 本模式下变器根本不拦波，波按自己的速度照常飞出去，再判几次也没有副作用。
			return WaveOutcome.TRANSPARENT;
		}

		@Override
		public void applyField(Level level, BlockPos pos, int radius) {
			// 攻击场 = 以变器为中心、边长为 2×半径+1 的立方体（与扫描/取料同一立方体口径，
			// 见 util.RadiusScan：AABB(pos).inflate(radius) 覆盖的方块格与它完全一致）
			AABB area = new AABB(pos).inflate(radius);
			for (AbstractChargerWaveEntity wave : level.getEntitiesOfClass(AbstractChargerWaveEntity.class, area))
				// 只调用一次"点燃"：是不是普通波由 trySetWaveType 自己判定（一生只能变一次），
				// 这里不重复判断波型——写两遍就是两处口径，迟早不一致
				wave.trySetWaveType(WaveTypes.ATTACK);
		}
	};

	/** 稳定的模式标识：NBT 落盘与翻译键后缀共用，等价于存档格式的一部分（改它要同时处理老存档）。 */
	private final String key;

	/** 护目镜面板上的行颜色（攻击态用红，一眼区分两态）。 */
	private final ChatFormatting color;

	TransmuterMode(String key, ChatFormatting color) {
		this.key = key;
		this.color = color;
	}

	/** 模式标识（{@code processing} / {@code attack}）。 */
	public String key() {
		return key;
	}

	/** 护目镜行翻译键：{@code createoreexpansion.stellar_wave_transmuter.mode.<key>}。 */
	public String translationKey() {
		return "createoreexpansion.stellar_wave_transmuter.mode." + key;
	}

	/** 护目镜行颜色。 */
	public ChatFormatting displayColor() {
		return color;
	}

	/** 下一个模式（扳手右键循环切换）。 */
	public TransmuterMode next() {
		TransmuterMode[] all = values();
		return all[(ordinal() + 1) % all.length];
	}

	/**
	 * <b>本模式对"命中变器的波"的处置</b>：波命中解析器（{@code WaveHitResolver}）的唯一分派点。
	 *
	 * @param wave 命中的波（服务端实例）
	 * @param pos  变器方块位置
	 * @return 调用方该执行的处置（见 {@link WaveOutcome}）
	 */
	public abstract WaveOutcome onWaveHit(AbstractChargerWaveEntity wave, BlockPos pos);

	/**
	 * <b>本模式的"场"作用</b>：变器的周期扫描（每 8 tick）调用一次。
	 *
	 * <p>默认空实现——加工波变态没有场（它只在波<b>命中</b>时才有效果）；
	 * 攻击波变态覆写为攻击场。</p>
	 *
	 * @param radius 本机当前读取半径（扫描已算好的那一个，见
	 *               {@code StellarWaveTransmuterBlockEntity#resolveRadius()}）
	 */
	public void applyField(Level level, BlockPos pos, int radius) {
	}

	/** 按标识取模式；空值或未知（老存档没有该键）→ {@link #PROCESSING}。 */
	public static TransmuterMode byKey(String key) {
		if (key != null)
			for (TransmuterMode mode : values())
				if (mode.key.equals(key))
					return mode;
		return PROCESSING;
	}

	/**
	 * 读取某位置变器当前的处理模式（波命中解析等"只有坐标"的地方从这里取）。
	 *
	 * <p>读不到方块实体（区块未加载、破块瞬间等）时按 {@link #PROCESSING}：与引入双模式之前的
	 * 历史行为一致——那种情况下穿波转换本身也会因取不到实体而按撞墙处理。</p>
	 */
	public static TransmuterMode at(Level level, BlockPos pos) {
		return level != null && level.getBlockEntity(pos) instanceof StellarWaveTransmuterBlockEntity be
			? be.getMode()
			: PROCESSING;
	}

	/**
	 * <b>变器对一次方块命中的处置</b>（由 {@link TransmuterMode#onWaveHit} 给出，调用方照此执行）。
	 */
	public enum WaveOutcome {
		/** 这一击已处理完（穿波转换 / 原路遣返）：调用方结束本 tick 的方块遍历。 */
		CONSUMED,
		/** 波口未开或撞到不可穿的机壳面：调用方按"撞墙"处理（绽放 + 消散）。 */
		BLOCKED,
		/** 本模式下变器对波透明（攻击波变态）：当作这里没有方块，波继续飞。 */
		TRANSPARENT;
	}
}
