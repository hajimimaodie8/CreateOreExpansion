package com.hjmmd_8.createoreexpansion.content.charger.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * <b>环绕波的锚点契约</b>（2026-10-02 批 4；作者裁定 D9 = A："把锚点契约放宽 + 伤害可覆写"）。
 *
 * <h2>一、为什么要有这个接口</h2>
 * <p>环绕波要素（{@code anchorWaveUuid / orbitRadius / orbitAngularSpeed / orbitPhase} +
 * {@code AbstractChargerWaveEntity#applyOrbitElement()}）原先<b>写死</b>"锚点必须是另一枚波"：</p>
 * <pre>
 *   if (!(anchor instanceof AbstractChargerWaveEntity parent) || !parent.isAlive()) { return false; }
 *   Vec3 dir = parent.getMovement();
 * </pre>
 * <p>而回旋镖的环绕技能（开工需求 §3.6）要求<b>锚点 = 那枚回旋镖</b>——镖是 {@code Projectile}，
 * 不是波。写死的判据会让环绕波<b>出生即死</b>（第一 tick 就查不到"父波" ⇒ discard）。</p>
 *
 * <p>所以本轮把两件事各抽成<b>一处</b>：</p>
 * <ol>
 *   <li><b>锚点判据</b>放宽成 {@code instanceof Entity anchor}（只要求"是个活着的实体"）
 *       —— 于是"镖当锚点"与"波当锚点"走的是同一条代码路径；</li>
 *   <li><b>"取运动方向"抽成契约</b>（本接口）—— 环平面的法向就是锚点的运动方向：
 *       <ul>
 *         <li><b>波</b>：{@link AbstractChargerWaveEntity#orbitDirection()} =
 *             {@code getMovement()}（<b>逐字</b>沿用改造前那一行，见该实现）；</li>
 *         <li><b>镖</b>：{@code AbstractBoomerangEntity#orbitDirection()} =
 *             {@code getDeltaMovement()}（镖的手写位移就写在这个原版速度字段上）。</li>
 *       </ul></li>
 * </ol>
 *
 * <h2>二、既有波行为逐字不变</h2>
 * <p>本接口的两个方法对既有波都是<b>零改变</b>：</p>
 * <ul>
 *   <li>{@link #orbitDirection()}：波的实现体只有一行 {@code return getMovement();} ——
 *       与改造前 {@code applyOrbitElement()} 里的取值<b>同一个字段、同一个方法</b>；</li>
 *   <li>{@link #orbitMineBlock(BlockPos)}：<b>默认实现返回 {@code false}</b> ——
 *       波锚点（星芒嬗震的环绕波就是"环着一枚波"）不挖方块，行为与改造前完全一致；
 *       只有回旋镖覆写它（环绕波撞方块 ⇒ 用镖那一档的挖掘判定挖掉）。</li>
 * </ul>
 *
 * <p>⚠ 本接口<b>只描述"环绕波能向锚点要什么"</b>，不新增任何实体类型 / 贴图 / 模型 / 渲染器
 * （关卡 {@code no-invented-wave-entity} 与 {@code boomerang-orbit-skill} 守着这一条）。</p>
 *
 * @since 1.0.0
 */
public interface OrbitAnchor {

	/**
	 * <b>本 tick 的运动方向</b>（单位向量；环绕波的环平面法向就是它）。
	 *
	 * <p>返回零向量是允许的：{@code applyOrbitElement()} 有"锚点方向退化 ⇒ 回落到环绕波自己的
	 * 方向 ⇒ 再回落到 +Z"两级兜底（与改造前逐字相同）。</p>
	 *
	 * <p><b>波实现 {@code getMovement()}、镖实现 {@code getDeltaMovement()}——各自只有一处。</b></p>
	 */
	Vec3 orbitDirection();

	/**
	 * <b>环绕波撞到普通方块时的回调</b>（可选；默认"不挖"，即既有波的行为）。
	 *
	 * <p>环绕波撞上方块后<b>无论如何都会消散</b>（{@code WaveHitResolver} 的"撞墙"分支），
	 * 本回调只是给它一个"消散之前把这块挖掉"的机会：</p>
	 * <ul>
	 *   <li><b>默认 {@code false} 且不做事</b> ⇒ 星芒嬗震的环绕波、任何"环着一枚波"的环绕波
	 *       行为与改造前逐字相同；</li>
	 *   <li><b>回旋镖覆写</b>：走镖自己那一档的挖掘判定
	 *       （{@code maxHardness} / {@code miningLevel} / 原版挖掘进度），挖掉了就记 −1 耐久
	 *       —— 掉落物与经验是原版在世界里生成的，靠镖既有的回程吸附带走（需求 §3.6 第 4 条，
	 *       零新机制）。</li>
	 * </ul>
	 *
	 * @param pos 环绕波撞到的那一格（普通方块；带物品槽的方块与机器方块不走这条回调）
	 * @return 是否真的挖掉了（默认 {@code false}）—— 调用方（波实体）不据此改变自己的消散语义
	 */
	default boolean orbitMineBlock(BlockPos pos) {
		return false;
	}
}
