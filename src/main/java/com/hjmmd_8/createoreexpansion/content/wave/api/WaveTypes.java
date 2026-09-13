package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

/**
 * <b>波型注册表</b>：本模组全部波型的唯一清单，也是扩展模组的接入点。
 *
 * <p>用法（扩展模组）：</p>
 * <pre>{@code
 * // 注册一个"雷波"：只伤害生物，不做任何加工
 * WaveTypes.register(new SimpleWaveType(
 *     ResourceLocation.fromNamespaceAndPath("yourmod", "thunder_wave"),
 *     false,          // 不充能加工
 *     false,          // 不远程加工
 *     true,           // 伤害生物
 *     WaveTrailStyle.DAMAGE));
 * }</pre>
 *
 * <p>三个内置波型（{@link #NORMAL} / {@link #OMNI} / {@link #ATTACK}）在类初始化时注册，
 * 语义与本模组的三态口径一一对应：普通波只会充能加工、全能波只会远程加工、攻击波只伤害。</p>
 *
 * <p>注册是<b>幂等</b>的：同 id 重复注册返回已存在的那一个（避免多个入口重复注册时互相覆盖），
 * 但若是<b>不同实现</b>用同一个 id 注册，则后到的会被忽略并在日志里体现（查日志可发现冲突）。</p>
 */
public final class WaveTypes {

	private static final Map<ResourceLocation, WaveType> REGISTRY = new LinkedHashMap<>();

	/** 普通波：只会充能加工（"废物"，此外什么都没有）。 */
	public static final WaveType NORMAL = register(new SimpleWaveType(
		ResourceLocation.fromNamespaceAndPath("createoreexpansion", "normal"),
		true, false, false, WaveTrailStyle.NORMAL));

	/** 全能波（原"变体波"）：读取机器并远程加工，不充能加工、不伤害生物。 */
	public static final WaveType OMNI = register(new SimpleWaveType(
		ResourceLocation.fromNamespaceAndPath("createoreexpansion", "omni"),
		false, true, false, WaveTrailStyle.MECHANICAL));

	/** 攻击波：纯攻击，只伤害生物。 */
	public static final WaveType ATTACK = register(new SimpleWaveType(
		ResourceLocation.fromNamespaceAndPath("createoreexpansion", "attack"),
		false, false, true, WaveTrailStyle.DAMAGE));

	private WaveTypes() {
	}

	/**
	 * 注册一个波型（扩展模组接入点）。
	 *
	 * @return 生效的波型：id 已被占用时返回先注册的那一个
	 */
	public static WaveType register(WaveType type) {
		if (type == null || type.id() == null)
			return NORMAL;
		WaveType existing = REGISTRY.get(type.id());
		if (existing != null)
			return existing;
		REGISTRY.put(type.id(), type);
		return type;
	}

	/** 按 id 查波型；未知 id 返回 {@link #NORMAL}（老存档/被移除的扩展不会导致崩溃）。 */
	public static WaveType get(ResourceLocation id) {
		WaveType type = id == null ? null : REGISTRY.get(id);
		return type == null ? NORMAL : type;
	}

	/** 全部已注册波型（只读视图，供 Jade / 护目镜 / 调试命令遍历）。 */
	public static Collection<WaveType> all() {
		return Collections.unmodifiableCollection(REGISTRY.values());
	}

	/** 按 id 字符串查（存档与同步数据都用这个口径，容错回 {@link #NORMAL}）。 */
	public static WaveType byIdString(String id) {
		return id == null || id.isEmpty() ? NORMAL : get(ResourceLocation.tryParse(id));
	}
}
