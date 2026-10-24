package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;


import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/**
 * <b>波的全部 NBT 属性的唯一声明处</b>（2026-10-24 波系统重构批 3）。
 *
 * <p>31 行 = 改造前手写在三个实体类里的 31 个键，<b>每个键名在整棵源码树里只出现这一次</b>：
 * 基类 10 总是 + 13 条件（{@code AbstractChargerWaveEntity}）、{@code charger_wave} 1 个
 * （{@code charger_wave} 的自定义伤害）、{@code stellar_wave} 7 个（6 总是 + 1 条件）。
 * 加一个属性 = 在这里加一行；{@link WaveStateNbt} 自动读写，不再有第二处清单。</p>
 *
 * <p><b>兼容口径逐键写在声明里</b>（键名 / 类型 / 缺省值 / 写不写 / 读不读 / 属于哪种波）：</p>
 * <ul>
 *   <li><b>键名、类型逐字不变</b>：{@link WaveAttrKey.Codec} 选的就是旧代码用的
 *       {@code getInt/getFloat/getDouble/getString/getList/hasUUID/getLong}；</li>
 *   <li><b>"写不写"逐字不变</b>：{@link WaveAttrKey.Persist#ALWAYS} = 旧代码无条件 {@code put}；
 *       {@link WaveAttrKey.Persist#WHEN_SET} = 旧代码 {@code if (x != null) { put }}；
 *       成组键用 {@link WaveAttrKey#group()} 表达"组长设了才写整组"（环绕四件套 / 命中效果三件套 /
 *       出生点三件套）；</li>
 *   <li><b>读侧语义逐字不变</b>：{@link WaveAttrKey.Read#ALWAYS} = 旧代码直接 {@code getX}
 *       （缺键读作 0 / 空表）；{@link WaveAttrKey.Read#WHEN_PRESENT} = 旧代码
 *       {@code if (tag.contains(..))}（缺键保持当前值）；</li>
 *   <li><b>唯一的"读侧夹取"</b>是旧代码本来就夹取的那四个键 —— {@code BoostStep}
 *       （{@code clamp(1..maxLevel)}）、{@code WaveCarriedRpm}（{@code abs}）、
 *       {@code WaveCarriedRadius}（{@code max(1, ·)}）、{@code WaveChain}（{@code max(0, ·)}）；
 *       <b>{@code WaveLevel} 不夹取</b>（方案 R-3：夹取会改玩法，越界等级仍按各表自己的回落口径走）。</li>
 * </ul>
 */
public final class WaveAttrs {

	private WaveAttrs() {
		throw new AssertionError("This class should not be instantiated");
	}

	// ==================================================================================
	// 编解码器里"不是单纯 getX/putX"的那几个：口径全部逐字来自旧实现
	// ==================================================================================

	/** 波型：存 id 字符串；空串 / 未知 id ⇒ {@link WaveTypes#NORMAL}（旧 {@code byIdString}）。 */
	private static final WaveAttrKey.Codec<WaveType> WAVE_TYPE_CODEC = new WaveAttrKey.Codec<>() {
		@Override
		public WaveType read(CompoundTag tag, String key) {
			return WaveTypes.byIdString(tag.getString(key));
		}

		@Override
		public void write(CompoundTag tag, String key, WaveType value) {
			tag.putString(key, value.id()
				.toString());
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 魔素：存<b>枚举名</b>（老存档 11 个字面量原样可读）；认不出的名字 ⇒ {@code null}（= 未设）。 */
	private static final WaveAttrKey.Codec<WaveTrailStyle> ESSENCE_CODEC = new WaveAttrKey.Codec<>() {
		@Override
		public WaveTrailStyle read(CompoundTag tag, String key) {
			// 2026-10-24：查找实现从实体类搬到这里（行为逐字相同：空串/未知名 ⇒ null，不抛异常）。
			return essenceByName(tag.getString(key));
		}

		@Override
		public void write(CompoundTag tag, String key, WaveTrailStyle value) {
			tag.putString(key, value.name());
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 电荷：存 int 编码（0 无 / 1 正 / 2 负；其它值一律"无"）。 */
	private static final WaveAttrKey.Codec<com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity> CHARGE_CODEC =
		new WaveAttrKey.Codec<>() {
			@Override
			public com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity read(CompoundTag tag, String key) {
				int code = tag.getInt(key);
				return code == 1
					? com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE
					: code == 2
						? com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.NEGATIVE
						: null;
			}

			@Override
			public void write(CompoundTag tag, String key,
				com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity value) {
				tag.putInt(key, value == null ? 0
					: value == com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE ? 1 : 2);
			}

			@Override
			public boolean present(CompoundTag tag, String key) {
				return tag.contains(key);
			}
		};

	/**
	 * 命中附加效果：存<b>注册名</b>；取不到 Holder / 名字解析失败 ⇒ {@code null}（旧代码
	 * {@code tryParse → getHolder(...).orElse(null)}）。写侧另由
	 * {@link #writableHitEffect(WaveState)} 守一道"注册名取得到"（旧代码
	 * {@code if (hitEffectId != null)}）。
	 */
	private static final WaveAttrKey.Codec<Holder<MobEffect>> HIT_EFFECT_CODEC = new WaveAttrKey.Codec<>() {
		@Override
		public Holder<MobEffect> read(CompoundTag tag, String key) {
			ResourceLocation id = ResourceLocation.tryParse(tag.getString(key));
			return id == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(id)
				.orElse(null);
		}

		@Override
		public void write(CompoundTag tag, String key, Holder<MobEffect> value) {
			ResourceLocation id = value == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(value.value());
			if (id != null)
				tag.putString(key, id.toString());
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 加热档：存<b>序号</b>；越界回 {@code NONE}（旧 {@code HeatLevelNames.byOrdinal}）。 */
	private static final WaveAttrKey.Codec<BlazeBurnerBlock.HeatLevel> HEAT = new WaveAttrKey.Codec<>() {
		@Override
		public BlazeBurnerBlock.HeatLevel read(CompoundTag tag, String key) {
			return HeatLevelNames.byOrdinal(tag.getInt(key));
		}

		@Override
		public void write(CompoundTag tag, String key, BlazeBurnerBlock.HeatLevel value) {
			tag.putInt(key, value.ordinal());
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/**
	 * 配方类型快照：存 id 列表；读侧按注册表反查，<b>认不出的 id 跳过</b>（旧代码
	 * {@code recipeTypeById(..) != null} 才 add）；写侧跳过空条目（旧代码 {@code type != null &&
	 * type.getId() != null}）。
	 */
	private static final WaveAttrKey.Codec<List<IRecipeTypeInfo>> RECIPE_TYPES = new WaveAttrKey.Codec<>() {
		@Override
		public List<IRecipeTypeInfo> read(CompoundTag tag, String key) {
			List<ResourceLocation> ids = WaveAttrKey.RESOURCE_LOCATIONS.read(tag, key);
			List<IRecipeTypeInfo> types = new ArrayList<>(ids.size());
			for (ResourceLocation id : ids) {
				IRecipeTypeInfo type = WaveMachineIntegrationPoints.recipeTypeById(id);
				if (type != null)
					types.add(type);
			}
			return List.copyOf(types);
		}

		@Override
		public void write(CompoundTag tag, String key, List<IRecipeTypeInfo> value) {
			List<ResourceLocation> ids = new ArrayList<>(value.size());
			for (IRecipeTypeInfo type : value)
				if (type != null && type.getId() != null)
					ids.add(type.getId());
			WaveAttrKey.RESOURCE_LOCATIONS.write(tag, key, ids);
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** 波源位置：存 {@code BlockPos.asLong()}；缺键 ⇒ {@code null}（旧 {@code contains ? of(long) : null}）。 */
	private static final WaveAttrKey.Codec<BlockPos> WAVE_ORIGIN_CODEC = new WaveAttrKey.Codec<>() {
		@Override
		public BlockPos read(CompoundTag tag, String key) {
			return BlockPos.of(tag.getLong(key));
		}

		@Override
		public void write(CompoundTag tag, String key, BlockPos value) {
			tag.putLong(key, value.asLong());
		}

		@Override
		public boolean present(CompoundTag tag, String key) {
			return tag.contains(key);
		}
	};

	/** {@code BoostStep} 读侧夹取（旧 {@code Mth.clamp(getInt, 1, WaveLevels.maxLevel())}）。 */
	private static final WaveAttrKey.Codec<Integer> BOOST_STEP_CODEC = new WaveAttrKey.Codec<>() {
		@Override
		public Integer read(CompoundTag tag, String key) {
			return net.minecraft.util.Mth.clamp(tag.getInt(key), 1, WaveLevels.maxLevel());
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

	/** {@code WaveCarriedRpm} 读侧取绝对值（旧 {@code Math.abs(getFloat)}）。 */
	private static final WaveAttrKey.Codec<Float> ABS_FLOAT = new WaveAttrKey.Codec<>() {
		@Override
		public Float read(CompoundTag tag, String key) {
			return Math.abs(tag.getFloat(key));
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

	/** {@code WaveCarriedRadius} 读侧 {@code max(1, ·)}（旧代码同形）。 */
	private static final WaveAttrKey.Codec<Integer> AT_LEAST_ONE = new WaveAttrKey.Codec<>() {
		@Override
		public Integer read(CompoundTag tag, String key) {
			return Math.max(1, tag.getInt(key));
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

	/** {@code WaveChain} 读侧 {@code max(0, ·)}（旧代码同形）。 */
	private static final WaveAttrKey.Codec<Integer> AT_LEAST_ZERO = new WaveAttrKey.Codec<>() {
		@Override
		public Integer read(CompoundTag tag, String key) {
			return Math.max(0, tag.getInt(key));
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

	// ==================================================================================
	// 31 行属性表 —— 每个键名在整棵源码树里只出现这一次（关卡 wave-nbt-keys 守着）
	// 列义：键名 / 类型 / 缺省值 / 写入 / 读出 / 属于哪种波 / 成组组长
	// ==================================================================================

	// ---- 基类（AbstractChargerWaveEntity）：总是写 10 个 ----
	/** Int；缺键 0；总是写；总是读。<b>读侧不夹取</b>（越界等级按各表自己的回落口径）。 */
	public static final WaveAttrKey<Integer> LEVEL = always("WaveLevel", WaveAttrKey.INT, 0);
	/** Double；缺键 0；总是写；总是读。方向向量的 X（不是 Motion）。 */
	public static final WaveAttrKey<Double> MOVE_X = always("MoveX", WaveAttrKey.DOUBLE, 0.0D);
	/** Double；缺键 0；总是写；总是读。 */
	public static final WaveAttrKey<Double> MOVE_Y = always("MoveY", WaveAttrKey.DOUBLE, 0.0D);
	/** Double；缺键 0；总是写；总是读。 */
	public static final WaveAttrKey<Double> MOVE_Z = always("MoveZ", WaveAttrKey.DOUBLE, 0.0D);
	/** Float；缺键 0；总是写；总是读。调级器延迟升级剩余距离。 */
	public static final WaveAttrKey<Float> BOOST_REMAINING = always("BoostRemaining", WaveAttrKey.FLOAT, 0.0F);
	/** Int；缺键 1（旧字段初值 1、读侧 clamp(1..maxLevel)）；总是写；总是读。 */
	public static final WaveAttrKey<Integer> BOOST_STEP = always("BoostStep", BOOST_STEP_CODEC, 1);
	/** Double；缺键 0；总是写；总是读。速度修正（格/秒）。 */
	public static final WaveAttrKey<Double> SPEED_OFFSET = always("SpeedOffset", WaveAttrKey.DOUBLE, 0.0D);
	/** String（波型 id）；缺键 = 保持当前值；总是写。 */
	public static final WaveAttrKey<WaveType> WAVE_TYPE =
		alwaysWhenPresent("WaveType", WAVE_TYPE_CODEC, WaveTypes.NORMAL);
	/** Int；缺键 0；总是写；总是读。同批豁免碰撞的批次号。 */
	public static final WaveAttrKey<Integer> FIRING_BATCH = always("FiringBatch", WaveAttrKey.INT, 0);
	/** Int 编码（0 无 / 1 正 / 2 负）；总是写；总是读。 */
	public static final WaveAttrKey<com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity> CHARGE =
		always("Charge", CHARGE_CODEC, null);

	// ---- 基类：条件写 13 个（没设过就一个字节都不写） ----
	/** String（枚举名）；设过才写；标签里有才读。 */
	public static final WaveAttrKey<WaveTrailStyle> ESSENCE = optional("Essence", ESSENCE_CODEC, null);
	/** UUID；设过才写；标签里有才读。 */
	public static final WaveAttrKey<UUID> OWNER = optional("Owner", WaveAttrKey.UUID, null);
	/** UUID；<b>环绕组的组长</b>（设了它才写/读另外三个键）。 */
	public static final WaveAttrKey<UUID> ORBIT_ANCHOR = optional("OrbitAnchor", WaveAttrKey.UUID, null);
	/** Double；随 {@link #ORBIT_ANCHOR} 成组。 */
	public static final WaveAttrKey<Double> ORBIT_RADIUS =
		grouped("OrbitRadius", WaveAttrKey.DOUBLE, 0.0D, ORBIT_ANCHOR);
	/** Double；随 {@link #ORBIT_ANCHOR} 成组。 */
	public static final WaveAttrKey<Double> ORBIT_ANGULAR_SPEED =
		grouped("OrbitAngularSpeed", WaveAttrKey.DOUBLE, 0.0D, ORBIT_ANCHOR);
	/** Double；随 {@link #ORBIT_ANCHOR} 成组。 */
	public static final WaveAttrKey<Double> ORBIT_PHASE = grouped("OrbitPhase", WaveAttrKey.DOUBLE, 0.0D, ORBIT_ANCHOR);
	/** 命中药水效果；设了且注册名取得到才写；标签里有才读。 */
	public static final WaveAttrKey<Holder<MobEffect>> HIT_EFFECT =
		optional("HitEffect", HIT_EFFECT_CODEC, null, WaveAttrs::writableHitEffect);
	/** Int；随 {@link #HIT_EFFECT} 成组。 */
	public static final WaveAttrKey<Integer> HIT_EFFECT_DURATION =
		grouped("HitEffectDuration", WaveAttrKey.INT, 0, HIT_EFFECT);
	/** Int；随 {@link #HIT_EFFECT} 成组。 */
	public static final WaveAttrKey<Integer> HIT_EFFECT_AMPLIFIER =
		grouped("HitEffectAmplifier", WaveAttrKey.INT, 0, HIT_EFFECT);
	/** Double；{@code > 0} 才写；总是读（旧代码无条件 getDouble）。 */
	public static final WaveAttrKey<Double> GRAVITY =
		optionalReadAlways("GravityAcceleration", WaveAttrKey.DOUBLE, 0.0D, WaveAttrs::positiveGravity);
	/** Double；<b>出生点组的组长</b>（设了它才写/读三个键）。 */
	public static final WaveAttrKey<Double> SPAWN_X = optional("SpawnX", WaveAttrKey.DOUBLE, 0.0D);
	/** Double；随 {@link #SPAWN_X} 成组。 */
	public static final WaveAttrKey<Double> SPAWN_Y = grouped("SpawnY", WaveAttrKey.DOUBLE, 0.0D, SPAWN_X);
	/** Double；随 {@link #SPAWN_X} 成组。 */
	public static final WaveAttrKey<Double> SPAWN_Z = grouped("SpawnZ", WaveAttrKey.DOUBLE, 0.0D, SPAWN_X);

	// ---- charger_wave：1 个条件键 ----
	/** Float；{@code > 0} 才写；标签里有才读（缺键 = -1 = 未设）。 */
	public static final WaveAttrKey<Float> CUSTOM_DAMAGE =
		optionalScoped("CustomDamage", WaveAttrKey.FLOAT, -1.0F, WaveAttrKey.Scope.CHARGER, WaveAttrs::positiveDamage);

	// ---- stellar_wave：6 总是 + 1 条件 ----
	/** {@code List<String>}；总是写（空表也写）；总是读。 */
	public static final WaveAttrKey<List<ResourceLocation>> ATTRIBUTES =
		scopedAlways("WaveAttributes", WaveAttrKey.RESOURCE_LOCATIONS, List.of(), WaveAttrKey.Scope.STELLAR);
	/** Int（加热档序号）；总是写；总是读（越界回 NONE）。 */
	public static final WaveAttrKey<BlazeBurnerBlock.HeatLevel> CARRIED_HEAT =
		scopedAlways("WaveCarriedHeat", HEAT, BlazeBurnerBlock.HeatLevel.NONE, WaveAttrKey.Scope.STELLAR);
	/** Float；总是写；总是读（读侧取绝对值）。 */
	public static final WaveAttrKey<Float> CARRIED_RPM =
		scopedAlways("WaveCarriedRpm", ABS_FLOAT, 0.0F, WaveAttrKey.Scope.STELLAR);
	/** Int；总是写；总是读（读侧 {@code max(1, ·)}）。 */
	public static final WaveAttrKey<Integer> CARRIED_RADIUS =
		scopedAlways("WaveCarriedRadius", AT_LEAST_ONE, 1, WaveAttrKey.Scope.STELLAR);
	/** Int；总是写；总是读（读侧 {@code max(0, ·)}）。 */
	public static final WaveAttrKey<Integer> CHAIN =
		scopedAlways("WaveChain", AT_LEAST_ZERO, 0, WaveAttrKey.Scope.STELLAR);
	/** {@code List<String>}；总是写；总是读（认不出的 id 跳过）。 */
	public static final WaveAttrKey<List<IRecipeTypeInfo>> RECIPE_TYPE_IDS =
		scopedAlways("WaveRecipeTypeIds", RECIPE_TYPES, List.of(), WaveAttrKey.Scope.STELLAR);
	/** Long（{@code BlockPos.asLong}）；设过才写；标签里有才读。 */
	public static final WaveAttrKey<BlockPos> WAVE_ORIGIN =
		optionalScoped("WaveOrigin", WAVE_ORIGIN_CODEC, null, WaveAttrKey.Scope.STELLAR);

	/** 全表（声明顺序）。 */
	private static final List<WaveAttrKey<?>> ALL = List.of(
		LEVEL, MOVE_X, MOVE_Y, MOVE_Z, BOOST_REMAINING, BOOST_STEP, SPEED_OFFSET, WAVE_TYPE, FIRING_BATCH, CHARGE,
		ESSENCE, OWNER, ORBIT_ANCHOR, ORBIT_RADIUS, ORBIT_ANGULAR_SPEED, ORBIT_PHASE,
		HIT_EFFECT, HIT_EFFECT_DURATION, HIT_EFFECT_AMPLIFIER, GRAVITY,
		SPAWN_X, SPAWN_Y, SPAWN_Z,
		CUSTOM_DAMAGE,
		ATTRIBUTES, CARRIED_HEAT, CARRIED_RPM, CARRIED_RADIUS, CHAIN, RECIPE_TYPE_IDS, WAVE_ORIGIN);

	/** 键名索引（{@link WaveStateNbt} 判断"表外键"用；Java 的字符串判断走这一份）。 */
	private static final Set<String> NAMES;

	static {
		Set<String> names = new HashSet<>(ALL.size() * 2);
		for (WaveAttrKey<?> key : ALL)
			names.add(key.nbtKey());
		NAMES = Set.copyOf(names);
	}

	/** 全表（只读；顺序 = 声明顺序）。 */
	public static List<WaveAttrKey<?>> all() {
		return ALL;
	}

	/** 这个 NBT 键名是否在本表里（是 ⇒ 由表读写；不是 ⇒ 进未知键袋）。 */
	public static boolean isDeclared(String nbtKey) {
		return NAMES.contains(nbtKey);
	}

	// ==================================================================================
	// 声明辅助：把"写不写 / 读不读 / 属于哪种波"收敛成几个具名形状，表里每行只有一个调用
	// ==================================================================================

	private static <T> WaveAttrKey<T> always(String id, WaveAttrKey.Codec<T> codec, T defaultValue) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.ALWAYS, WaveAttrKey.Read.ALWAYS,
			WaveAttrKey.Scope.BASE, null, null);
	}

	private static <T> WaveAttrKey<T> alwaysWhenPresent(String id, WaveAttrKey.Codec<T> codec, T defaultValue) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.ALWAYS, WaveAttrKey.Read.WHEN_PRESENT,
			WaveAttrKey.Scope.BASE, null, null);
	}

	private static <T> WaveAttrKey<T> optional(String id, WaveAttrKey.Codec<T> codec, T defaultValue) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.WHEN_SET, WaveAttrKey.Read.WHEN_PRESENT,
			WaveAttrKey.Scope.BASE, null, null);
	}

	private static <T> WaveAttrKey<T> optional(String id, WaveAttrKey.Codec<T> codec, T defaultValue,
		Predicate<WaveState> writeGate) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.WHEN_SET, WaveAttrKey.Read.WHEN_PRESENT,
			WaveAttrKey.Scope.BASE, null, writeGate);
	}

	private static <T> WaveAttrKey<T> optionalReadAlways(String id, WaveAttrKey.Codec<T> codec, T defaultValue,
		Predicate<WaveState> writeGate) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.WHEN_SET, WaveAttrKey.Read.ALWAYS,
			WaveAttrKey.Scope.BASE, null, writeGate);
	}

	private static <T> WaveAttrKey<T> grouped(String id, WaveAttrKey.Codec<T> codec, T defaultValue,
		WaveAttrKey<?> leader) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.WHEN_SET, WaveAttrKey.Read.WHEN_PRESENT,
			leader.scope(), leader, null);
	}

	private static <T> WaveAttrKey<T> optionalScoped(String id, WaveAttrKey.Codec<T> codec, T defaultValue,
		WaveAttrKey.Scope scope) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.WHEN_SET, WaveAttrKey.Read.WHEN_PRESENT,
			scope, null, null);
	}

	private static <T> WaveAttrKey<T> optionalScoped(String id, WaveAttrKey.Codec<T> codec, T defaultValue,
		WaveAttrKey.Scope scope, Predicate<WaveState> writeGate) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.WHEN_SET, WaveAttrKey.Read.WHEN_PRESENT,
			scope, null, writeGate);
	}

	private static <T> WaveAttrKey<T> scopedAlways(String id, WaveAttrKey.Codec<T> codec, T defaultValue,
		WaveAttrKey.Scope scope) {
		return new WaveAttrKey<>(id, codec, defaultValue, WaveAttrKey.Persist.ALWAYS, WaveAttrKey.Read.ALWAYS, scope,
			null, null);
	}

	// ---- 两个写侧守卫（旧代码里写键之前的 if 条件，逐字搬过来） ----

	/**
	 * 魔素的<b>容错名字查找</b>（旧 {@code AbstractChargerWaveEntity#essenceByName} 逐字搬来）：
	 * 空串 / 未知名 ⇒ {@code null}（不抛异常；这条路径跑在读档里，{@code valueOf} 一抛就是崩）。
	 */
	static WaveTrailStyle essenceByName(String name) {
		if (name == null || name.isEmpty())
			return null;
		for (WaveTrailStyle style : WaveTrailStyle.values()) {
			if (style.name()
				.equals(name))
				return style;
		}
		return null;
	}

	/** 旧代码：{@code if (hitEffect != null) { key = registry.getKey(...); if (key != null) 写三个 }}。 */
	private static boolean writableHitEffect(WaveState state) {
		Holder<MobEffect> effect = state.get(HIT_EFFECT);
		return effect != null && BuiltInRegistries.MOB_EFFECT.getKey(effect.value()) != null;
	}

	/** 旧代码：{@code if (gravityAcceleration > 0.0D)}。 */
	private static boolean positiveGravity(WaveState state) {
		return state.get(GRAVITY) > 0.0D;
	}

	/** 旧代码：{@code if (customDamage > 0.0F)}。 */
	private static boolean positiveDamage(WaveState state) {
		return state.get(CUSTOM_DAMAGE) > 0.0F;
	}
}
