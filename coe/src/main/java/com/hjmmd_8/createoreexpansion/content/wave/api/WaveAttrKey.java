package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>一个波属性的完整声明</b>（2026-10-24 波系统重构批 3，方案 §3.2）：NBT 键名、读写方式、
 * 缺省值、<b>写不写</b>的规则、<b>读不读</b>的规则、属于哪种波实体、是否成组，全部在这一行里。
 *
 * <p><b>为什么要有它</b>：改造前同一份"波有哪些属性"在实体字段、同步数据、117 行手写 NBT、
 * 各个 switch 里各写一遍；加一个属性要同时改六处，漏一处就是静默失效。现在它是
 * {@link WaveAttrs} 表里的一行，{@link WaveStateNbt} 遍历表读写、<b>没有手写键名、没有 switch</b>。</p>
 *
 * <p><b>兼容六条（方案 §3.3 R-1…R-6，本类逐条落实）</b>：</p>
 * <ol>
 *   <li><b>键名逐字保留</b>：{@link #nbtKey()} 就是旧代码里的字面量，一个字母都没改；</li>
 *   <li><b>"写不写"由表声明</b>：{@link Persist#ALWAYS} / {@link Persist#WHEN_SET}，
 *       成组键再叠一个"组长"（{@link #group()}）；</li>
 *   <li><b>读侧语义不改</b>：{@link #read(CompoundTag)} 只按存下来的值还原，<b>不夹取</b>
 *       （唯一的例外是旧代码本身读侧就夹取/取绝对值的那几个键，写在各自的 {@link Codec} 里）；</li>
 *   <li><b>枚举名即 id</b> / 波型 id：解析失败一律回落到旧口径的"未设 / normal"，不抛异常；</li>
 *   <li><b>未知键</b>：本表之外的键由 {@link WaveState#unknown()} 原样保留（见 {@link WaveStateNbt}）；</li>
 *   <li><b>同步不扩 accessor</b>：本类不碰 {@code SynchedEntityData}，那 7 个视图见 {@link WaveStateSync}。</li>
 * </ol>
 *
 * @param <T> 该属性在 {@link WaveState} 里的值类型（引用型缺省值可以是 {@code null}）
 */
public final class WaveAttrKey<T> {

	/** 写规则：总是写 / 只在真的设过时写。 */
	public enum Persist {
		/** 任何一发波都有这个键（旧代码里"总是写"的 10 + 子类 6 个）。 */
		ALWAYS,
		/** 只在真的设过时写键（旧代码里"条件写"的 13 + 子类 1 个）。 */
		WHEN_SET
	}

	/** 读规则：总是读 / 只在标签里存在时读。 */
	public enum Read {
		/** 缺键按 {@link #defaultValue()} 读（旧代码直接 {@code getInt/getDouble/...}）。 */
		ALWAYS,
		/** 缺键保持当前值（旧代码 {@code if (tag.contains(..))} 的形状）。 */
		WHEN_PRESENT
	}

	/** 属性属于哪种波实体：基类两个实体共用的 / 只有 {@code charger_wave} / 只有 {@code stellar_wave}。 */
	public enum Scope {
		/** 两个波实体都读写（基类那 23 个键）。 */
		BASE,
		/** 只有 {@code charger_wave} 读写。 */
		CHARGER,
		/** 只有 {@code stellar_wave} 读写。 */
		STELLAR;

		/** 本次读写（实体种类为 {@code actual}）是否覆盖该键所属的层。 */
		public boolean covers(Scope keyScope) {
			return keyScope == BASE || keyScope == this;
		}
	}

	/**
	 * <b>类型化读写</b>：怎么把标签里的一个键读成 {@code T}、怎么写回去、怎么判断它"在不在"。
	 *
	 * <p>刻意做成独立接口而不是在 {@link WaveStateNbt} 里按 {@link Scope}/{@link Persist} 分支：
	 * 每个类型的容错口径（UUID 的 {@code hasUUID}、列表的跳过非法项、魔素的枚举名解析…）
	 * 都只写在它自己的编解码器里，加一个类型只加一个常量。</p>
	 */
	public interface Codec<T> {

		/** 从标签读出该键的值（缺键时由实现决定回落口径；<b>不夹取</b>）。 */
		T read(CompoundTag tag, String key);

		/** 把值写进标签（只在调用方判定"该写"之后调用）。 */
		void write(CompoundTag tag, String key, T value);

		/** 该键在标签里是否存在（读侧的成组守卫与"条件写"的判定都用它）。 */
		boolean present(CompoundTag tag, String key);
	}

	/** 整数键（Int）。 */
	public static final Codec<Integer> INT = new Codec<>() {
		@Override
		public Integer read(CompoundTag tag, String key) {
			return tag.getInt(key);
		}

		@Override
		public void write(CompoundTag tag, String key, Integer value) {
			tag.putInt(key, value);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 单精度键（Float）。 */
	public static final Codec<Float> FLOAT = new Codec<>() {
		@Override
		public Float read(CompoundTag tag, String key) {
			return tag.getFloat(key);
		}

		@Override
		public void write(CompoundTag tag, String key, Float value) {
			tag.putFloat(key, value);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 双精度键（Double）。 */
	public static final Codec<Double> DOUBLE = new Codec<>() {
		@Override
		public Double read(CompoundTag tag, String key) {
			return tag.getDouble(key);
		}

		@Override
		public void write(CompoundTag tag, String key, Double value) {
			tag.putDouble(key, value);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 字符串键（String）。 */
	public static final Codec<String> STRING = new Codec<>() {
		@Override
		public String read(CompoundTag tag, String key) {
			return tag.getString(key);
		}

		@Override
		public void write(CompoundTag tag, String key, String value) {
			tag.putString(key, value);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 长整数键（Long）。 */
	public static final Codec<Long> LONG = new Codec<>() {
		@Override
		public Long read(CompoundTag tag, String key) {
			return tag.getLong(key);
		}

		@Override
		public void write(CompoundTag tag, String key, Long value) {
			tag.putLong(key, value);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/**
	 * UUID 键（IntArray(4)）：读侧必须走 {@link CompoundTag#hasUUID}（长度不对 / 不是 int 数组
	 * 一律当"没写"），这正是旧代码 {@code hasUUID("Owner")} / {@code hasUUID("OrbitAnchor")} 的口径；
	 * 裸解析会抛异常，而这条路径跑在读档里。
	 */
	public static final Codec<UUID> UUID = new Codec<>() {
		@Override
		public UUID read(CompoundTag tag, String key) {
			return tag.getUUID(key);
		}

		@Override
		public void write(CompoundTag tag, String key, UUID value) {
			tag.putUUID(key, value);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.hasUUID(key);
		}
	};

	/**
	 * {@code List<String>} 形态的资源 id 列表：非法 id 跳过（旧代码
	 * {@code ResourceLocation.tryParse} 后判 null 再 add 的口径）；读出来的列表不可变。
	 */
	public static final Codec<List<ResourceLocation>> RESOURCE_LOCATIONS = new Codec<>() {
		@Override
		public List<ResourceLocation> read(CompoundTag tag, String key) {
			ListTag list = tag.getList(key, Tag.TAG_STRING);
			List<ResourceLocation> ids = new ArrayList<>(list.size());
			for (int i = 0; i < list.size(); i++) {
				ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
				if (id != null)
					ids.add(id);
			}
			return List.copyOf(ids);
		}

		@Override
		public void write(CompoundTag tag, String key, List<ResourceLocation> value) {
			ListTag list = new ListTag();
			for (ResourceLocation id : value)
				if (id != null)
					list.add(StringTag.valueOf(id.toString()));
			tag.put(key, list);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	private final String nbtKey;
	private final Codec<T> codec;
	private final T defaultValue;
	private final Persist persist;
	private final Read read;
	private final Scope scope;
	private final WaveAttrKey<?> group;
	private final Predicate<WaveState> writeGate;

	WaveAttrKey(String nbtKey, Codec<T> codec, T defaultValue, Persist persist, Read read, Scope scope,
		WaveAttrKey<?> group, Predicate<WaveState> writeGate) {
		this.nbtKey = nbtKey;
		this.codec = codec;
		this.defaultValue = defaultValue;
		this.persist = persist;
		this.read = read;
		this.scope = scope;
		this.group = group;
		this.writeGate = writeGate;
	}

	/** NBT 键名（= 旧代码里的字面量，逐字保留）。 */
	public String nbtKey() {
		return nbtKey;
	}

	/** 缺省值（读不到 / 没设过时 {@link WaveState#get} 返回它）。 */
	public T defaultValue() {
		return defaultValue;
	}

	/** 该键属于哪种波实体。 */
	public Scope scope() {
		return scope;
	}

	/** 成组键的"组长"（{@code null} = 独立键）：组长"设了"才读写这一组。 */
	public WaveAttrKey<?> group() {
		return group;
	}

	/** 本次实体种类是否读写该键。 */
	public boolean inScope(Scope owner) {
		return owner.covers(scope);
	}

	/** 读侧：这个键在当前标签里该不该读。 */
	public boolean readable(CompoundTag tag) {
		if (group != null)
			return group.readable(tag);
		return read == Read.ALWAYS || codec.present(tag, nbtKey);
	}

	/**
	 * 写侧：这个键现在该不该写。
	 *
	 * <p>{@link Persist#WHEN_SET} 的独立键必须"真的被设过"（{@link WaveState#isSet}）；
	 * 成组键只看组长（组长该写就写整组，与旧代码 {@code if (spawnPos != null) { 写三个 } } 同形）；
	 * 组长还可以再声明一个 {@link #writeGate}（"能取到注册名"这类可写性判定）。</p>
	 */
	public boolean writable(WaveState state) {
		if (group != null) {
			if (!group.writable(state))
				return false;
		} else if (persist == Persist.WHEN_SET && !state.isSet(this)) {
			return false;
		}
		return writeGate == null || writeGate.test(state);
	}

	/** 读一个键进状态（由 {@link WaveStateNbt} 遍历时调用；泛型在此处收敛）。 */
	void readInto(WaveState state, CompoundTag tag) {
		state.put(this, codec.read(tag, nbtKey));
	}

	/** 把状态里的值写进标签（由 {@link WaveStateNbt} 遍历时调用）。 */
	void writeInto(WaveState state, CompoundTag tag) {
		codec.write(tag, nbtKey, state.get(this));
	}

	@Override
	public String toString() {
		return "WaveAttrKey[" + nbtKey + "]";
	}
}
