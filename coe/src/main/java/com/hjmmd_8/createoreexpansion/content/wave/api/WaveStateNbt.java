package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * <b>波属性 NBT 读写的唯一实现 —— 表驱动、零手写键名、零 switch</b>
 * （2026-10-24 波系统重构批 3，方案 §3.2）。
 *
 * <p>改造前 {@code AbstractChargerWaveEntity#readAdditionalSaveData} /
 * {@code #addAdditionalSaveData} 共 117 行、再加两个子类各一段，全是手写键名与逐字段赋值；
 * 加一个属性要改四处，漏一处就是静默丢档。现在这两个方法只是<b>遍历 {@link WaveAttrs} 表</b>：
 * 读侧按 {@link WaveAttrKey#readable} 判定"读不读"、写侧按 {@link WaveAttrKey#writable}
 * 判定"写不写"，值怎么进出标签由每个键自己的 {@link WaveAttrKey.Codec} 决定。</p>
 *
 * <p><b>兼容的六条规则里由本类落实的三条</b>：</p>
 * <ul>
 *   <li><b>R-2 "写不写"由表声明</b>：旧代码的 10 总是 / 13 条件 / 子类 6 总是 + 1 条件，
 *       现在全部是表里的 {@code Persist} + 组长关系，<b>判定结果逐键相同</b>；</li>
 *   <li><b>R-3 读侧不夹取</b>：本类不添加任何额外的 clamp；旧代码本来就夹取/取绝对值的四个键，
 *       夹取写在它们的编解码器里（见 {@link WaveAttrs}）。<b>{@code WaveLevel} 越界时仍原样读入</b>，
 *       由各查表点自己回落 —— 夹取会改玩法（老存档坏数据今天显示为 α 黄，夹取后变成 1 级）。</li>
 *   <li><b>R-5 未知键保留袋</b>：表里没有的键 → {@link WaveState#unknown()}；写回时<b>只补不覆盖</b>
 *       （{@code if (!tag.contains(key))}）。改造前"读一遍再写一遍"会把第三方写在波 NBT 上的属性
 *       抹掉，本批修掉。</li>
 * </ul>
 *
 * <p><b>为什么未知键"只补不覆盖"</b>：{@code Entity.saveWithoutId} 先写 Pos / Motion / … 再调
 * {@code addAdditionalSaveData}（原版 {@code Entity.java:1799}），所以本类被调用时标签里已经有
 * 父类那些键。未知键袋为了不误伤父类字段（尤其 {@code Pos}：覆盖它会让"存一次档 = 回到旧位置"）
 * 采用"标签里已有就跳过"的规则；同时父类键名也在 {@link #PARENT_KEYS} 里被排除，避免
 * {@code NeoForgeData} 这类条件键被旧快照"复活"。</p>
 */
public final class WaveStateNbt {

	/**
	 * 父类 {@code Entity.saveWithoutId} 自己写的键（原版 {@code Entity.java:1717-1797}）。
	 *
	 * <p>它们不属于"波属性"，也<b>不</b>进未知键袋：本类不声明、不覆盖、不复活它们。
	 * 这不是第二份波属性清单 —— 波属性清单只有 {@link WaveAttrs} 一份；这里是"父类占用的名字"。</p>
	 */
	private static final Set<String> PARENT_KEYS = Set.of(
		"id", "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "Invulnerable",
		"PortalCooldown", "UUID", "CustomName", "CustomNameVisible", "Silent", "NoGravity", "Glowing",
		"TicksFrozen", "HasVisualFire", "Tags", "Passengers", "neoforge:attachments", "NeoForgeData");

	private WaveStateNbt() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 读：遍历表中（属于本实体种类）的每个键，按各自的读规则还原进 {@code state}。
	 *
	 * @param state 目标状态（本方法只写它；表外键收进 {@link WaveState#unknown()}）
	 * @param tag   实体标签（{@code Entity.load} 传进来的整份标签）
	 * @param scope 本实体种类（{@code BASE} / {@code CHARGER} / {@code STELLAR}）
	 */
	public static void read(WaveState state, CompoundTag tag, WaveAttrKey.Scope scope) {
		state.unknown().clear();
		for (WaveAttrKey<?> key : WaveAttrs.all()) {
			if (!key.inScope(scope))
				continue;
			if (!key.readable(tag))
				continue;
			key.readInto(state, tag);
		}
		for (String name : tag.getAllKeys()) {
			if (WaveAttrs.isDeclared(name) || PARENT_KEYS.contains(name))
				continue;
			Tag value = tag.get(name);
			if (value != null)
				state.unknown()
					.put(name, value.copy());
		}
	}

	/**
	 * 写：遍历表中（属于本实体种类）的每个键，按各自的写规则放进 {@code tag}；最后把未知键
	 * 原样补回（<b>只补不覆盖</b>，见类注释）。
	 *
	 * @param state 源状态
	 * @param tag   实体标签（{@code Entity.saveWithoutId} 已写入父类字段）
	 * @param scope 本实体种类
	 */
	public static void write(WaveState state, CompoundTag tag, WaveAttrKey.Scope scope) {
		for (WaveAttrKey<?> key : WaveAttrs.all()) {
			if (!key.inScope(scope))
				continue;
			if (!key.writable(state))
				continue;
			key.writeInto(state, tag);
		}
		for (Map.Entry<String, Tag> entry : state.unknown()
			.entrySet()) {
			if (!tag.contains(entry.getKey()))
				tag.put(entry.getKey(), entry.getValue()
					.copy());
		}
	}

	/** 某种波实体"应当有"的键数（诊断 / 关卡用；不是实际写了几个）。 */
	public static int declaredCount(WaveAttrKey.Scope scope) {
		int count = 0;
		for (WaveAttrKey<?> key : WaveAttrs.all())
			if (key.inScope(scope))
				count++;
		return count;
	}
}
