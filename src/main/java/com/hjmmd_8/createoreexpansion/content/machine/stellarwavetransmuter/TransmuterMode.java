package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * <b>星辉波变器的双处理模式</b>（枚举带行为 = 策略模式）。
 *
 * <p>存在意义：变器对波只有两件事——"把波加工成全能波"和"把波点燃成攻击波"。这两件事
 * 各自的完整行为（命中怎么处置、周边有没有场、场该多久扫一次）都收在本枚举的常量体里，
 * 调用方只按<b>当前模式</b>取值分派：{@code WaveHitResolver} 只做
 * {@code switch (mode.onWaveHit(...))} 三选一，变器方块实体按模式自报的
 * {@link #fieldIntervalTicks()}（0 = 本模式没有场、一次都不跑）调 {@link #applyField}。
 * <b>调用方不判断"是不是某种模式"</b>，
 * 以后再加模式（例如"吸收波"）只需在这里加一个常量，不改任何调用点。</p>
 *
 * <p><b>两态语义（用户定义）</b>：</p>
 * <ul>
 *   <li>{@link #PROCESSING} <b>加工波变态</b>（默认）：波口开着才让波穿过，穿波即把原波转成
 *       全能波（携带扫描属性与载荷，见 {@link StellarWaveTransmuterPass#tryConvert}）；
 *       入口未开或撞到不可穿的机壳面 → 按撞墙处理；入口开而对面出口关 → 原路遣返；</li>
 *   <li>{@link #ATTACK} <b>攻击波变态</b>：变器对波<b>完全透明</b>——不做穿波转换、不遣返、
 *       <b>面开关也不拦波</b>（波照常飞过去）；同时变器自身成为<b>攻击场</b>：读取半径内
 *       <b>确实穿过场</b>的普通波被点燃成攻击波（{@link WaveTypes#ATTACK}；查询框按"两次扫描
 *       之间波最多能走的距离"外扩，穿过与否另按线段判定，见 {@link #applyField}）。
 *       攻击波是纯攻击、不参与加工（加工路径已由波基类按波型闸门挡住，本模式不提供任何加工逻辑）。</li>
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

	/** 攻击波变态：对波透明 + 自身是攻击场（点燃穿过场的普通波）。 */
	ATTACK("attack", ChatFormatting.RED) {
		@Override
		public WaveOutcome onWaveHit(AbstractChargerWaveEntity wave, BlockPos pos) {
			// 透明 = 什么都不做，而且**不做 setPos 位移**：防二次判定的推出方块外手法是为
			// "波被机器拦下"设计的（遣返/转向必须立刻离开方块盒，否则下一 tick 再判一次）；
			// 本模式下变器根本不拦波，波按自己的速度照常飞出去，再判几次也没有副作用。
			return WaveOutcome.TRANSPARENT;
		}

		/**
		 * <b>本模式的场节拍</b>：攻击场按"波不可能整段穿过场盒"的最短间隔单独跑
		 * （公式与推导见 {@link #shortestSafeFieldInterval()}），与每
		 * {@link StellarWaveTransmuterBlockEntity#scanIntervalTicks()} tick 一次的重扫描解耦。
		 */
		@Override
		public int fieldIntervalTicks() {
			return shortestSafeFieldInterval();
		}

		/**
		 * 攻击场：把"本轮确实穿过场盒"的普通波点燃成攻击波（{@link WaveTypes#ATTACK}）。
		 *
		 * <p><b>本轮只做两件事</b>：一次实体查询（只找波实体）+ 逐个候选的"最近一 tick 位移线段
		 * 是否与场盒相交"判定（见 {@link #crossedField}）。<b>不做任何方块遍历</b>，也不给波增加
		 * 任何状态（位置全读原版 {@code Entity} 已有的 {@code xo/yo/zo}）——这是"场提速到 2 tick
		 * 仍然廉价"的全部根据。</p>
		 *
		 * <p><b>为什么重扫描仍是 8 tick、场却要更短的节拍</b>：重扫描那一套（热源 / 机器 /
		 * 设备计数 / 载荷估算）是<b>读数</b>，8 tick（≈160ms）玩家已看不出差别，也没有正确性要求；
		 * 场是<b>穿过判定</b>，它的正确性上限由"两次场扫描之间波能走多远"决定：判定只覆盖
		 * "最近一 tick 的位移线段"，两次场扫描之间还有 {@code 节拍 − 1} tick <b>完全没有被任何线段
		 * 覆盖</b>，波只要在这段空档里整段穿过场盒，就一次都不会被点燃。按旧实现（场挂在 8 tick
		 * 的重扫描上）空档有 7 tick 以上（Create 的 lazyTick 计数让实际周期略长于声明值），
		 * 而半径 1 的场盒棱长只有 3 格（最高波速 12 格/秒
		 * = 0.6 格/tick，正面穿场只要 5 tick &lt; 7）——这就是"被波速调节器加速过的波在半径 1 的
		 * 变器上可能整段穿过而不被点燃"的根因。现在场按 {@link #fieldIntervalTicks()}
		 * （攻击波变态 = 2 tick，空档 1 tick）单独跑，重扫描仍每
		 * {@link StellarWaveTransmuterBlockEntity#scanIntervalTicks()} tick 一次，两者互不牵连：
		 * 漏波窗口被关掉，而热源/机器/计数那一整套并没有被一起提速。</p>
		 *
		 * <p><b>为什么查询框要外扩</b>：判定依据是"波现在在哪、上一 tick 在哪"，一次查询必须把
		 * "最近一 tick 的位移线段可能碰到场盒"的波都捞进来，所以查询框 = 场盒外扩一个行程余量
		 * （见 {@link #queryBox} → {@link #travelMarginPerScan()}）。该余量按<b>较长的重扫描间隔</b>
		 * 算，对更短的场节拍是<b>充分安全的超集</b>：多捞进来的波还要过 {@link #crossedField}，
		 * 判定口径不会被放宽，只是候选多几个。</p>
		 *
		 * <p><b>为什么外扩之后还要线段判定</b>：外扩后的查询框覆盖的是"以场盒为中心、来回各一个
		 * 行程"的大盒，从旁边飞过（压根没进场）的波同样会被查到。所以对每个候选再做一次"上一 tick
		 * 位置 → 当前位置"与<b>场盒本体</b>的相交判定（见 {@link #crossedField}）：外扩只放宽
		 * "看得到谁"，不放宽"算不算穿过"。</p>
		 *
		 * <p><b>剩余窗口（如实说明）</b>：本方案把"正面穿场整段落在观测空档"这一类情形按公式
		 * 排除掉（{@link #shortestSafeFieldInterval()} 保证 {@code 节拍 − 1} 恒小于整段穿场耗时，
		 * 且节拍被夹到 ≥1 后极端情况下干脆没有空档），但抽样判定的本质仍留下一类几何巧合：波<b>只擦到
		 * 场盒的棱/角</b>（从相邻两个面进出、盒内弦长远短于棱长），且它落在盒内的采样点恰好全是
		 * <b>未被覆盖</b>的那些 tick——此时不会被点燃。节拍 2 tick 下这要求"盒内停留不足 1 tick
		 * 的行程（≈0.6 格）"，即弹道恰好擦过盒棱附近。要彻底闭合它得给每只波记路径位置或按波自身
		 * 方向外推（弯折路径会误判），与"不给波加状态、不加每 tick 方块遍历"的要求冲突，故不采用。</p>
		 */
		@Override
		public void applyField(Level level, BlockPos pos, int radius) {
			AABB field = fieldBox(pos, radius);
			for (AbstractChargerWaveEntity wave : level.getEntitiesOfClass(AbstractChargerWaveEntity.class,
				queryBox(field)))
				// 只调用一次"点燃"：是不是普通波由 trySetWaveType 自己判定（一生只能变一次），
				// 这里不重复判断波型——写两遍就是两处口径，迟早不一致
				if (crossedField(wave, field))
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
	 * <b>本模式的"场"作用</b>：变器按 {@link #fieldIntervalTicks()} 的节拍调用一次
	 * （0 节拍 = 没有场 = 调用方一次都不调）。
	 *
	 * <p>默认空实现——加工波变态没有场（它只在波<b>命中</b>时才有效果）；
	 * 攻击波变态覆写为攻击场。</p>
	 *
	 * @param radius 本机当前读取半径（扫描已算好的那一个，见
	 *               {@code StellarWaveTransmuterBlockEntity#resolveRadius()}）
	 */
	public void applyField(Level level, BlockPos pos, int radius) {
	}

	/**
	 * <b>本模式"场"的扫描节拍（tick）</b>：变器按此间隔调用一次 {@link #applyField}；
	 * <b>0 = 本模式没有场</b>（调用方不跑，连半径都不用读）。
	 *
	 * <p>默认返回 0——加工波变态没有场：它只在波<b>命中</b>变器的那一刻才有效果，没有"在区域内
	 * 持续作用"这回事，也就不需要任何节拍。攻击波变态覆写为
	 * {@link #shortestSafeFieldInterval()}。</p>
	 *
	 * <p><b>为什么把节拍做成模式的属性</b>：节拍是"这个模式要不要场、场多久扫一次"的一部分，
	 * 与"命中怎么处置"同级，都收在常量体里。调用方（{@code StellarWaveTransmuterBlockEntity}）
	 * 只按 {@code mode.fieldIntervalTicks()} 自己的计数器跑，<b>不写
	 * {@code if (mode == ATTACK)}</b>；以后再加有场的模式（例如"吸收波"），只需在该常量体里覆写
	 * 本方法，方块实体一行都不用改。</p>
	 *
	 * <p><b>它必须与 {@link StellarWaveTransmuterBlockEntity#scanIntervalTicks()} 分开</b>：那个
	 * 是"读数（热源/机器/设备计数/载荷）的刷新间隔"，8 tick ≈ 160ms 足够；本方法是"场（穿过判定）
	 * 的观测间隔"，它的正确性由"波能不能在两次观测之间整段穿过场盒"决定，必须更短。
	 * 一个数值服务两件事必然顾此失彼，故这里另开一个只属于模式的节拍。</p>
	 *
	 * @return 场扫描间隔（tick）；0 表示本模式没有场
	 */
	public int fieldIntervalTicks() {
		return 0;
	}

	// ================= 攻击场的几何（各项口径只有一处实现，改动只发生在这里） =================

	/**
	 * 本机可能取到的<b>场半径下界</b>（格）。
	 *
	 * <p>变器的读取半径只有三种取值：不在能量场里 = 1，弱场 = 2，强场 = 3
	 * （见 {@code StellarWaveTransmuterBlockEntity#resolveRadius()}）。场节拍要成为
	 * <b>与半径无关</b>的模式属性，就必须按最坏情形算；而"场盒越小 → 波整段穿过越快"，
	 * 所以下界 1 是最坏情形：按棱长 3 格的场盒算出的节拍，对半径 2/3（棱长 5/7 格）同样安全
	 * （按公式它们本可放宽到 4/5 tick，这里主动取更严的那个）。</p>
	 */
	private static final int MIN_FIELD_RADIUS = 1;

	/**
	 * 场节拍的<b>安全系数</b>：节拍 ≤ 整段穿场耗时 ÷ 2。
	 *
	 * <p>取 2 的理由：判定只覆盖"最近一 tick 的位移线段"，两次场扫描之间有 {@code 节拍 − 1} tick
	 * 的空档；只要 {@code 节拍 − 1} 小于"整段穿场耗时"（= 场盒内最短弦长 ÷ 每 tick 位移），
	 * 波就不可能整段穿过而不被任何一次覆盖到。除以 2 让该不等式恒成立且留一倍余量
	 * （现表下：穿场 5 tick、节拍 2 tick、空档 1 tick）。</p>
	 */
	private static final double FIELD_INTERVAL_SAFETY = 2.0d;

	/**
	 * 行程余量之外再留下的缓冲（格）。
	 *
	 * <p>取 1 格的理由：行程余量公式只刻画"等级速度上限之内"的位移，实际还有三处会让同一 tick
	 * 走得更远、或让观测偏一点——</p>
	 * <ul>
	 *   <li><b>带电波在能量场里被加速</b>：{@code EnergyField#apply} 每 tick 给速度加上
	 *       {@code 场强 / 20} 格每秒（场强 = 4 × 档位 ≤ 16 → 单 tick 至多 +0.8 格/秒），而该 tick
	 *       的位移直接用修正后的向量走，8 tick 至多多出 0.32 格/场；</li>
	 *   <li>浮点与同步误差；</li>
	 *   <li>服务端卡顿造成的 tick 抖动。</li>
	 * </ul>
	 * <p>1 格 ≈ 上限速度（12 格/秒）下 1.7 tick 的行程，足以覆盖上述量级（约 3 个同时生效的
	 * 最高档加速场叠加），又远小于半径本身的量级（半径 1 的场盒边长 3 格）。</p>
	 */
	private static final double TRAVEL_SLACK = 1.0d;

	/**
	 * <b>攻击场本体盒</b>：以变器方块为中心、边长 {@code 2×radius+1} 的立方体。
	 *
	 * <p>口径与扫描/取料同一个立方体（{@code AABB(pos).inflate(radius)} 覆盖的方块格与
	 * {@code util.RadiusScan} 完全一致）——本方法只是给"场盒"一个名字，保证全类只有一处定义：
	 * 判定口径只在这里改，别处都引用它。</p>
	 */
	private static AABB fieldBox(BlockPos pos, int radius) {
		return new AABB(pos).inflate(radius);
	}

	/**
	 * 本轮扫描的<b>实体查询框</b>＝场盒外扩一个行程余量（{@link #travelMarginPerScan()} 格）。
	 *
	 * <p>外扩只放宽"查询得到谁"：盒边长变成 {@code 2×(radius+行程余量)+1}，于是"两次扫描之间
	 * 从场里穿过、此刻已经飞出盒外"的波也还在候选里；它<b>不</b>放宽"算不算穿过"——那由
	 * {@link #crossedField} 按场盒本体判定。</p>
	 */
	private static AABB queryBox(AABB field) {
		return field.inflate(travelMarginPerScan());
	}

	/**
	 * <b>行程余量</b>（格）＝ 两次扫描之间波最多能走的距离
	 * ＝ 最大波速 × 扫描间隔 / 20 + {@link #TRAVEL_SLACK}。
	 *
	 * <p>两个因子都取各自唯一的定义处，本式<b>不含任何魔法数字</b>：最大波速来自波速表
	 * （{@link #maxWaveSpeed()} → {@link WaveLevels#maxSpeed(int)}），扫描间隔来自变器方块实体
	 * （{@link StellarWaveTransmuterBlockEntity#scanIntervalTicks()}）。波速表或扫描间隔以后调整了，
	 * 外扩量自动跟随——不会再出现"只在速度/间隔调整之后才暴露"的漏波。</p>
	 *
	 * <p><b>为什么按重扫描间隔（而不是更短的场节拍）算</b>：场现在按 {@link #fieldIntervalTicks()}
	 * 单独跑，判定其实只需"再多扩一 tick 的位移 + 缓冲"就够；但余量取两者中更大的那个
	 * （重扫描间隔 → 现表 12 × 8 / 20 + 1 = 5.8 格）只会多捞几个候选，而"算不算穿过"由
	 * {@link #crossedField} 收口，不会把"路过"判成"穿过"——所以这是<b>充分安全的超集</b>，
	 * 不必随场节拍来回改。</p>
	 */
	private static double travelMarginPerScan() {
		return maxWaveSpeed() * StellarWaveTransmuterBlockEntity.scanIntervalTicks() / 20.0d + TRAVEL_SLACK;
	}

	/**
	 * <b>最坏情形下的场节拍（tick）</b>——本类唯一一处"节拍公式"实现。
	 *
	 * <p>公式（每个因子都取自它自己的唯一定义处，本式<b>不含魔法数字</b>）：</p>
	 * <pre>节拍 = max(1, floor(场盒内最短弦长 ÷ (最大波速 ÷ 20) ÷ 安全系数))</pre>
	 * <ul>
	 *   <li><b>场盒内最短弦长</b>：取"正面垂直穿过"的最坏情形——弦长 = 场盒棱长 =
	 *       {@code 2 × 半径 + 1}（棱长口径与 {@link #fieldBox} 同一处：{@code AABB(pos).inflate(radius)}），
	 *       半径按本机下界 {@link #MIN_FIELD_RADIUS}；</li>
	 *   <li><b>最大波速 ÷ 20</b>：波每 tick 最多走的格数（原版一秒 20 tick），速度来自波速表
	 *       {@link #maxWaveSpeed()}；</li>
	 *   <li><b>安全系数</b>：{@link #FIELD_INTERVAL_SAFETY}（理由见其 javadoc）；</li>
	 *   <li><b>max(1, …)</b>：每 tick 至多一次（本方法只做一次实体查询，绝不会演化成每 tick
	 *       方块遍历）。</li>
	 * </ul>
	 *
	 * <p><b>代入现表数值</b>：最大波速 12 格/秒 → 0.6 格/tick；半径 1 → 棱长 3 格 → 穿场
	 * 3 ÷ 0.6 = 5 tick → 5 ÷ 2 = 2（向下取整）→ <b>2 tick</b>。（半径 2/3 的棱长 5/7 格对应
	 * 8.33/11.67 → 4/5 tick，本方法主动取半径下界，对它们只会更安全。）</p>
	 *
	 * <p>由此得到的保证：{@code 节拍 − 1} 恒小于整段穿场耗时（若波速极高导致算出的节拍被夹到 1，
	 * 则两次观测之间干脆没有空档，覆盖是逐 tick 完整的），所以"正面穿场整段落在观测空档"
	 * 不可能发生。</p>
	 */
	private static int shortestSafeFieldInterval() {
		double shortestChord = 2.0d * MIN_FIELD_RADIUS + 1.0d;
		double travelPerTick = maxWaveSpeed() / 20.0d;
		double crossingTicks = shortestChord / travelPerTick;
		return Math.max(1, (int) Math.floor(crossingTicks / FIELD_INTERVAL_SAFETY));
	}

	/**
	 * <b>全等级谱系的最大飞行速度</b>（格/秒）：在 {@link WaveLevels#LOW}~{@link WaveLevels#MAX_LEVEL}
	 * 上逐档取 {@link WaveLevels#maxSpeed(int)} 的较大者（现表为 1~3 级 10、4/5 级 12 → 12）。
	 *
	 * <p>不直接写 {@code 12}：那等于把速度表复制一份到本类，加等级或改上限时必然失配；也不假定
	 * "最高等级一定最快"（那是对表内部实现的隐式假设）——逐档取最大值才是稳的取法。</p>
	 */
	private static double maxWaveSpeed() {
		double max = 0;
		for (int level = WaveLevels.LOW; level <= WaveLevels.MAX_LEVEL; level++)
			max = Math.max(max, WaveLevels.maxSpeed(level));
		return max;
	}

	/**
	 * 波是否<b>真的穿过</b>了场盒（而不是只从旁边飞过去）。
	 *
	 * <p><b>线段取"上一 tick 位置 → 当前位置"</b>：原版 {@code Entity} 的 {@code xo/yo/zo} 就是
	 * 本 tick 开始前的坐标——{@code ServerLevel#tickNonPassenger} 在 tick 实体之前先调
	 * {@code setOldPosAndRot()} 写入它，而波自己的 {@code tick()} 又是在 {@code super.tick()} 之后
	 * 才 {@code setPos} 走出本 tick 的位移，所以这两点之间的线段恰好是"最近一 tick 的位移段"，
	 * 不必为波增加任何逐 tick 记录（波不持有状态，正是本设计能"只在周期扫描里做事"的关键）。</p>
	 *
	 * <p><b>为什么用 {@link AABB#clip(Vec3, Vec3)}</b>：它是原版的线段裁剪工具（返回入面交点，
	 * 无交点返回 {@code Optional.empty()}），语义正好是"这条线段与盒有没有相交"；而
	 * {@link AABB#intersects(Vec3, Vec3)} 只是"线段包围盒与场盒的粗略重叠"，斜着擦过场、
	 * 甚至只在拐角附近路过的波都会被判成穿过——那正是本判定要排除的误判，故不能用。</p>
	 *
	 * <p><b>{@code contains} 那一支不可省</b>：{@code clip} 内部的 {@code clipPoint} 只接受
	 * {@code 0 < t < 1} 的入面交点，起点已经在盒内时返回 {@code empty}（原版
	 * {@code BlockGetter#clip} 因此另配 isInside 分支）。少了这一支，"上一 tick 就已在场盒内"的波
	 * 会全部漏点燃——那恰恰是半径小、波速慢时最常见的情形。</p>
	 */
	private static boolean crossedField(AbstractChargerWaveEntity wave, AABB field) {
		Vec3 previous = new Vec3(wave.xo, wave.yo, wave.zo);
		return field.contains(previous) || field.clip(previous, wave.position())
			.isPresent();
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
