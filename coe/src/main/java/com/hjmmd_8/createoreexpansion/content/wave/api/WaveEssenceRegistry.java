package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;

/**
 * <b>魔素注册表</b>：本模组全部魔素的唯一清单，也是扩展模组的接入点
 * （2026-10-24 波系统重构批 4，作者 D4"魔素必须允许第三方新增"）。
 *
 * <p><b>为什么要有它</b>：改造前魔素是 {@link WaveTrailStyle} 枚举，颜色档案与命中表都以枚举为键
 * （{@code EnumMap} / {@code switch}）⇒ 枚举封死 = 第三方<b>一个都加不了</b>；而作者的原话是
 * "第三方可能有相关的元素属性，可以往上面添加无数种元素……我们这边得与时俱进"。
 * 本类把"魔素"从枚举值变成<b>一条可注册的记录</b>：id + 颜色 + 可选拖尾档案 + 可选命中行为。</p>
 *
 * <h2>一行注册（第三方）</h2>
 * <pre>{@code
 * WaveEssenceRegistry.register(WaveEssence.of("yourmod:thunder")
 *     .color(0x9B5CFF)
 *     .trailCount(8).trailScale(0.55f)
 *     .behavior(new MyThunderHit())
 *     .build());
 * }</pre>
 *
 * <h2>内置 11 个：id 逐字等于枚举名（老 NBT 兼容的硬要求）</h2>
 * <p>下面静态块里那 11 行 {@code builtin("ID", WaveTrailStyle.NAME)} 是<b>唯一</b>的内置登记处：
 * 每行第一个参数是 id 字面量，第二个是它挂着的枚举值，两者必须逐字相同
 * （{@link #register(WaveEssence)} 会当场校验）。
 * 于是存档里的 {@code Essence:"FIRE"}、同步数据里的 {@code "FIRE"}、以及
 * {@link WaveTrailStyle#byName(String)} 的解析结果，改造前后<b>逐字节相同</b>。</p>
 *
 * <h2>解析规则（方案 §3.5 R-4，{@link #byIdString(String)}）</h2>
 * <ul>
 *   <li>空串 / {@code null} ⇒ {@code null}（= 没有魔素）；</li>
 *   <li><b>含冒号</b> ⇒ 按 {@code namespace:path} 查本表（第三方的路）；</li>
 *   <li><b>不含冒号</b> ⇒ 先按内置枚举名解析（老存档的路），再查本表；</li>
 *   <li>认不出 ⇒ {@code null}（与改造前"未知名 → null"逐字一致，<b>不抛异常</b>——
 *       这条路径跑在读档 / 实体 tick 里，一抛就是崩）。</li>
 * </ul>
 *
 * <h2>注册时机与冲突</h2>
 * <p>在扩展模组自己的 {@code @Mod} 构造器里注册（{@code RegisterEvent} 之前、任何波被生成 /
 * 读取之前），与 {@link WaveTypes#register} / {@link WaveLevelRegistry#register} 的约定一致。
 * <b>同 id 重复注册</b>⇒ 返回先注册的那一份（与 {@link WaveTypes#register} 同口径：多个入口重复
 * 注册互不打架；本表不做"同名不同值"裁决，因为魔素 id 天然带命名空间，冲突方自己能看出来）。</p>
 *
 * <p><b>线程模型</b>：注册 {@code synchronized}、整体替换只读快照；读侧走 {@code volatile}
 * 快照、无锁（实体 tick 每帧都要读颜色）。</p>
 */
public final class WaveEssenceRegistry {

	private static final Object LOCK = new Object();

	/** 已注册魔素的只读快照（保持登记顺序；每次注册后整体替换，读侧无锁）。 */
	private static volatile Map<String, WaveEssence> byId = Collections.emptyMap();

	/*
	 * 内置 11 行 —— 机器读的格式：tools/check-armor-sets.ps1 的 wave-essence-registry 断言按
	 * `builtin("<ID>", WaveTrailStyle.<NAME>);` 逐行解析，并比较两侧逐字相同。
	 * 颜色一律走 ChargerWaveFx 的调色板（枚举 → 变换），所以观感与批 3 逐字节相同。
	 * 前三个是"波型风格"（NORMAL / MECHANICAL / DAMAGE），后八个才是真魔素；11 个都登记，
	 * 因为 NBT / 同步 / API 都能把任意一个枚举名传进来（改造前就是这样，不能收窄）。
	 *
	 * ⚠ 这里的 id 字面量<b>不许</b>改成别的写法：老存档 Essence 键存的就是枚举名。
	 */
	static {
		// —— 波型风格（前三个，声明序同枚举：只增不减）——
		builtin("NORMAL", WaveTrailStyle.NORMAL);
		builtin("MECHANICAL", WaveTrailStyle.MECHANICAL);
		builtin("DAMAGE", WaveTrailStyle.DAMAGE);
		// —— 八种魔素 ——
		builtin("WATER", WaveTrailStyle.WATER);
		builtin("FIRE", WaveTrailStyle.FIRE);
		builtin("EARTH", WaveTrailStyle.EARTH);
		builtin("WIND", WaveTrailStyle.WIND);
		builtin("ICE", WaveTrailStyle.ICE);
		builtin("LIGHTNING", WaveTrailStyle.LIGHTNING);
		builtin("POISON", WaveTrailStyle.POISON);
		builtin("ARCANE", WaveTrailStyle.ARCANE);
	}

	private WaveEssenceRegistry() {
	}

	/** 登记一个内置魔素：id 字面量 + 它挂着的枚举值；颜色 = 该枚举值在调色板里的变换。 */
	private static void builtin(String id, WaveTrailStyle style) {
		WaveEssenceRegistry.register(WaveEssence.builtin(id, style,
			base -> ChargerWaveFx.styleColor(style, base)));
	}

	/**
	 * 注册一个魔素（扩展模组接入点）。
	 *
	 * @return 生效的魔素：id 已被占用时返回先注册的那一个
	 * @throws IllegalArgumentException id 为空、内置魔素的 id ≠ 枚举名、或第三方 id 不含冒号
	 */
	public static WaveEssence register(WaveEssence essence) {
		if (essence == null)
			throw new IllegalArgumentException("WaveEssenceRegistry 不接受 null 魔素");
		String id = essence.id();
		if (id == null || id.isEmpty())
			throw new IllegalArgumentException("魔素的 id 不能为空");
		WaveTrailStyle builtin = essence.trailStyle();
		if (builtin != null) {
			if (!id.equals(builtin.name())) {
				throw new IllegalArgumentException("内置魔素的 id 必须逐字等于枚举名（收到 id=" + id + "，枚举="
					+ builtin.name() + "）—— 老存档的 Essence 键存的就是枚举名，改一个字母就是存档不兼容");
			}
		} else if (id.indexOf(':') < 0) {
			throw new IllegalArgumentException("第三方魔素的 id 必须写成 namespace:path（收到 " + id
				+ "）—— 不含冒号的名字一律按内置枚举名解析（老存档口径），否则它永远读不回来");
		}
		synchronized (LOCK) {
			WaveEssence existing = byId.get(id);
			if (existing != null)
				return existing;
			Map<String, WaveEssence> next = new LinkedHashMap<>(byId);
			next.put(id, essence);
			byId = Collections.unmodifiableMap(next);
			return essence;
		}
	}

	/** 按 id 精确查（不做容错）；未知 ⇒ {@code null}。 */
	public static WaveEssence get(String id) {
		return id == null ? null : byId.get(id);
	}

	/** 内置枚举值 → 它的注册项；未登记（理论不可能）⇒ {@code null}。 */
	public static WaveEssence builtinFor(WaveTrailStyle style) {
		return style == null ? null : byId.get(style.name());
	}

	/**
	 * <b>存档 / 同步字符串的唯一容错解析口</b>（NBT 的 {@code Essence} 键、同步的 STRING 都走它）：
	 * 规则见类注释；认不出 ⇒ {@code null}（不抛异常）。
	 */
	public static WaveEssence byIdString(String raw) {
		if (raw == null || raw.isEmpty())
			return null;
		if (raw.indexOf(':') < 0) {
			// 老存档的路：枚举名 → 内置登记项。认不出 ⇒ null（= 没有魔素）。
			WaveTrailStyle style = WaveTrailStyle.byName(raw);
			return style == null ? null : builtinFor(style);
		}
		// 第三方的路：namespace:path。
		return byId.get(raw);
	}

	/** 全部已注册魔素（登记顺序的只读视图）。 */
	public static Collection<WaveEssence> all() {
		return byId.values();
	}

	/** 已注册数量（诊断 / 关卡用）。 */
	public static int size() {
		return byId.size();
	}
}
