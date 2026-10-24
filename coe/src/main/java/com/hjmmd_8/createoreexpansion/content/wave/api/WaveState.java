package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.Tag;

/**
 * <b>一枚能量波的全部属性值</b>（2026-10-24 波系统重构批 3，方案 §3.2"单一真源"）。
 *
 * <p>它是属性的<b>唯一持有者</b>：实体只持有位置 / 寿命计时 / tick 阶段 / 命中链，<b>不再声明
 * 属性字段</b>；NBT 不再有自己的代码，只是本对象经 {@link WaveStateNbt} 的序列化形态。</p>
 *
 * <p><b>三件事刻意不做</b>（方案 §3.4 的边界）：</p>
 * <ul>
 *   <li><b>不算术</b>——速度/伤害/颜色仍由 {@link WaveSpeed} / {@link WaveLevels} 给；</li>
 *   <li><b>不查表、不知道 {@code Level}</b>——本类不 import 任何世界类型，可脱离游戏跑（探针用）；</li>
 *   <li><b>不决定缺省值的语义</b>——缺省值写在 {@link WaveAttrs} 的键声明里。</li>
 * </ul>
 *
 * <p><b>未知键保留袋</b>（方案 §3.3 R-5）：{@link #unknown()} 存的是"表里没有的 NBT 键"，
 * 由 {@link WaveStateNbt#read} 原样收进来、{@link WaveStateNbt#write} 原样带出去。改造前
 * "读一遍再写一遍"会把第三方写在波上的属性抹掉——那个缺陷在本批修掉。</p>
 */
public final class WaveState {

	/** 属性值：键是 {@link WaveAttrs} 里的静态常量（不重写 equals ⇒ 身份语义）。 */
	private final Map<WaveAttrKey<?>, Object> values = new LinkedHashMap<>();

	/** 表外键（第三方属性）：原样保留，写出时按"不覆盖已有键"的规则带回。 */
	private final Map<String, Tag> unknown = new LinkedHashMap<>();

	/** 读一个属性；没设过 ⇒ 该键声明的缺省值（引用型可能是 {@code null}）。 */
	@SuppressWarnings("unchecked")
	public <T> T get(WaveAttrKey<T> key) {
		Object value = values.get(key);
		if (value == null)
			return key.defaultValue();
		return (T) value;
	}

	/**
	 * 写一个属性；{@code null} = <b>清除</b>（回到缺省值）。
	 *
	 * <p>刻意不在写入时判断"是不是等于缺省值"：写不写由 {@link WaveAttrKey#writable} 在读档/存档时
	 * 决定，这里只负责把<"设过 / 没设过">记下来——{@code SpawnX = 0} 这种"值恰好等于缺省值、
	 * 但必须写键"的情形靠的正是这个存在位（旧代码由 {@code spawnPos != null} 表达）。</p>
	 */
	public <T> void set(WaveAttrKey<T> key, T value) {
		if (value == null) {
			values.remove(key);
		} else {
			values.put(key, value);
		}
	}

	/** 这个属性是否"真的被设过"（{@link #get} 会回落缺省值，这里问的是存在位）。 */
	public boolean isSet(WaveAttrKey<?> key) {
		return values.containsKey(key);
	}

	/** 清除一个属性（回到缺省值）。 */
	public void clear(WaveAttrKey<?> key) {
		values.remove(key);
	}

	/** 读侧写入口（{@link WaveAttrKey#readInto} 用；与 {@link #set} 同一语义）。 */
	<T> void put(WaveAttrKey<T> key, T value) {
		set(key, value);
	}

	/** 表外键的<b>可写视图</b>（{@link WaveStateNbt} 读进来 / 写出去；测试也用它塞未知键）。 */
	public Map<String, Tag> unknown() {
		return unknown;
	}

	/** 深拷贝（未知键的 {@link Tag} 也复制，避免两个状态共享可变标签）。 */
	public WaveState copy() {
		WaveState copy = new WaveState();
		copy.values.putAll(values);
		for (Map.Entry<String, Tag> entry : unknown.entrySet())
			copy.unknown.put(entry.getKey(), entry.getValue().copy());
		return copy;
	}

	/** 已设属性的个数（诊断 / 关卡用；不是 31 个键的总数）。 */
	public int size() {
		return values.size();
	}

	@Override
	public String toString() {
		return "WaveState" + values.keySet();
	}
}
