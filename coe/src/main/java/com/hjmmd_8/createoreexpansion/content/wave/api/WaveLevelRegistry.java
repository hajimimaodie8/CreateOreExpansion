package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * <b>波等级注册表</b>：等级号 → {@link WaveLevelSpec} 的唯一清单，也是扩展模组的接入点。
 *
 * <p><b>为什么要有它</b>：抽取前"一个波级"分散在 6 张表（符号数组、速度 switch、速度上限阈值、
 * 伤害 switch、指示灯色 switch、实体里的 {@code isOmega() >= 5}）+ 1 张住在
 * {@code ChargingRecipe} 的外来表（充能量）。加第 6 级要改 8 处代码。本类把这 7 个数值
 * 收成"一条记录"，等级号由注册参数给出，{@link WaveLevels} 降级为纯门面
 * （既有 87 处调用一个字不用改）。</p>
 *
 * <p><b>注册一行</b>：</p>
 * <pre>{@code
 * WaveLevelRegistry.register(6, WaveLevelSpec.builder("ζ")
 *     .baseSpeed(9.0d).maxSpeed(14.0d).damage(14.0f)
 *     .indicatorColor(0xFFB020).build());
 * }</pre>
 *
 * <h2>注册时机（唯一承诺）</h2>
 * <p>在扩展模组自己的 {@code @Mod} 构造器里注册（即 {@code RegisterEvent} 之前、任何波被生成 /
 * 读取之前），与 {@link WaveTypes#register} 的约定一致。<b>这是一个运行期活表</b>：读侧每次都查
 * 当前快照，所以本表<b>没有</b>冻结门（本仓在 {@code :coe} 侧没有"所有 mod 构造完毕"的钩子，
 * 硬造一个只会多一处时序真源）。在世界已加载之后注册，既有波的下一次查表也会看到新值——
 * 这种用法<b>不被支持</b>，行为由调用方自己承担。</p>
 *
 * <h2>异常行为（都要么幂等、要么当场报错，绝不静默）</h2>
 * <ul>
 *   <li><b>重复注册</b>：同等级 + 同内容（{@link WaveLevelSpec#equals}）= <b>幂等无操作</b>，
 *       返回先注册的那一份（多个入口重复注册不会互相打架）；同等级 + <b>不同</b>内容 =
 *       {@link IllegalArgumentException}——等级号是全局协议（{@code JADE_MAX} / {@code SAPPHIRE_MAX} /
 *       波形 NBT 都按数字读），两个模组抢同一个号只能由人裁决，静默让后到的赢会让前者的波
 *       用别人的数值飞。</li>
 *   <li><b>越界</b>：{@code level < }{@link WaveLevels#LOW}（0 / 负数——它们的语义是"未接入应力"
 *       或"老存档坏数据"，不是等级）→ {@link IllegalArgumentException}。上界<b>刻意不设</b>
 *       （作者 D3：允许任意注册）；注意 {@code MAX_LEVEL} 会跟着涨，而本模组自己的
 *       {@code ValueSettingsBoard}（星辉石充能器的手动等级槽）按 {@code MAX_LEVEL − LOW} 出行数，
 *       注册很大的等级会让那个板子很长——这是注册方的选择，不是本表的限制。</li>
 *   <li><b>非法数值</b>：{@code null} spec、空符号、非正 / 非有限速度、{@code maxSpeed < baseSpeed}、
 *       负伤害、负充能点数 → {@link IllegalArgumentException}（校验在 {@link WaveLevelSpec} 构造器里）。</li>
 * </ul>
 *
 * <p><b>线程模型</b>：注册是 {@code synchronized} 的、整体替换一个只读快照；读侧走
 * {@code volatile} 快照、无锁。注册期望在模组构造期（单线程派发）完成，上述措辞只是为了
 * "偶发晚注册也不至于读到半个表"。</p>
 */
public final class WaveLevelRegistry {

	private static final Object LOCK = new Object();

	/** 已注册等级的只读快照（升序；每次注册后整体替换，读侧无锁）。 */
	private static volatile NavigableMap<Integer, WaveLevelSpec> byLevel = Collections.emptyNavigableMap();

	/*
	 * 内置 5 级（1 α / 2 β / 3 γ / 4 ε / 5 ω）——数值与抽取前逐值相同（本批的硬要求：不改玩法）。
	 *
	 * 行格式是机器读的：tools/check-armor-sets.ps1 的 star-wave-level-map 与 wave-level-table
	 * 两个断言按 `register(WaveLevels.<符号>, new WaveLevelSpec("<符号>", <速度>d, <上限>d, <伤害>f,
	 * 0x<ARGB>, <金色拖尾>, <充能量>))` 逐行解析。改这里的任何数值 = 改玩法，必须同批重瞄那两道关。
	 */
	static {
		// 等级 1：α —— 2 格/秒，上限 10，伤害 4，黄，充能 100
		register(WaveLevels.LOW, new WaveLevelSpec("α", 2.0d, 10.0d, 4.0f, 0xFFFF55, false, 100));
		// 等级 2：β —— 4 格/秒，上限 10，伤害 6，绿，充能 500
		register(WaveLevels.HIGH, new WaveLevelSpec("β", 4.0d, 10.0d, 6.0f, 0x55FF55, false, 500));
		// 等级 3：γ —— 6 格/秒，上限 10，伤害 8，蓝，充能 1000
		register(WaveLevels.GAMMA, new WaveLevelSpec("γ", 6.0d, 10.0d, 8.0f, 0x5555FF, false, 1000));
		// 等级 4：ε —— 7 格/秒，上限 12，伤害 10，紫粉，充能 1500（蓝宝石线专属）
		register(WaveLevels.EPSILON, new WaveLevelSpec("ε", 7.0d, 12.0d, 10.0f, 0xFF66E0, false, 1500));
		// 等级 5：ω —— 8 格/秒，上限 12，伤害 12，玫红，金色拖尾 + 充能 2000（仅蓝宝石 256 RPM）
		register(WaveLevels.OMEGA, new WaveLevelSpec("ω", 8.0d, 12.0d, 12.0f, 0xFF4073, true, 2000));
	}

	private WaveLevelRegistry() {
	}

	/**
	 * 注册一个波等级（扩展模组接入点；调用形态见类注释）。
	 *
	 * @param level 等级号（≥ {@link WaveLevels#LOW}；内置占 1~5）
	 * @param spec  该等级的完整数值声明，见 {@link WaveLevelSpec}
	 * @return 生效的声明：同等级 + 同内容重复注册时返回先注册的那一份
	 * @throws IllegalArgumentException spec 为 {@code null}、等级 &lt; 1、或同等级已注册了不同内容
	 */
	public static WaveLevelSpec register(int level, WaveLevelSpec spec) {
		if (spec == null)
			throw new IllegalArgumentException("波等级 " + level + " 的 WaveLevelSpec 不能为 null");
		if (level < WaveLevels.LOW)
			throw new IllegalArgumentException("波等级必须 >= " + WaveLevels.LOW + "，收到 " + level
				+ "（0 / 负数是『未接入应力』或『老存档坏数据』的语义，不是可注册的等级）");
		synchronized (LOCK) {
			WaveLevelSpec existing = byLevel.get(level);
			if (existing != null) {
				if (existing.equals(spec))
					return existing;
				throw new IllegalArgumentException("波等级 " + level + " 已被注册为 " + existing
					+ "，拒绝用 " + spec + " 覆盖——等级号是全局协议（机器封顶 / 波形 NBT 都按数字读），"
					+ "同名不同值必须由整合包作者裁决");
			}
			TreeMap<Integer, WaveLevelSpec> next = new TreeMap<>(byLevel);
			next.put(level, spec);
			byLevel = Collections.unmodifiableNavigableMap(next);
			WaveLevels.refreshMaxLevel(next.lastKey());
			return spec;
		}
	}

	/** 该等级已注册的声明；未注册返回 {@code null}（不回落、不抛异常——回落口径在 {@link WaveLevels}）。 */
	public static WaveLevelSpec spec(int level) {
		return byLevel.get(level);
	}

	/** 该等级是否已注册（{@link WaveLevels#isValid(int)} 的唯一判据）。 */
	public static boolean isRegistered(int level) {
		return byLevel.containsKey(level);
	}

	/** 不高于 {@code level} 的最高已注册声明；没有更低的返回 {@code null}（越界阈值型取值的回落用）。 */
	public static WaveLevelSpec highestAtMost(int level) {
		java.util.Map.Entry<Integer, WaveLevelSpec> entry = byLevel.floorEntry(level);
		return entry == null ? null : entry.getValue();
	}

	/** 全部已注册等级（升序只读视图）。 */
	public static Collection<Integer> levels() {
		return byLevel.keySet();
	}

	/** 全部已注册声明（按等级升序的只读视图）。 */
	public static Collection<WaveLevelSpec> all() {
		return byLevel.values();
	}

	/** 当前最高已注册等级（内置时 = {@link WaveLevels#OMEGA}）。 */
	public static int maxLevel() {
		return byLevel.isEmpty() ? WaveLevels.OMEGA : byLevel.lastKey();
	}
}
