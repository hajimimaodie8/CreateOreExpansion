package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity.orbitDirectionOf;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity.orbitPlaneAxes;

/**
 * <b>环绕波要素</b>（2026-10-06 行为零变化拆分，从 {@link AbstractChargerWaveEntity} 的
 * {@code applyOrbitElement} / {@code logOrbitDiag} / {@code emitOrbitGeometry} / {@code fmt2}
 * 与其专属节流刻度 <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>被挂上"环绕"要素的那枚波，每一 tick 站在环上的哪一点、
 * 以及在日志与屏幕上怎么证明它在绕</b>——</p>
 * <ol>
 *   <li>{@link #tick}：把自身位置改写成 {@code 锚点位置 + r × (u·cosθ + v·sinθ)}
 *       （环平面垂直于锚点运动方向，θ = 初始相位 + 角速度 × 已存活 tick）；</li>
 *   <li>{@link #logOrbitDiag}：出生一行 + 每 20 tick 一行心跳（走 {@link WaveDiag#trace}，
 *       波相关日志的唯一出口）；</li>
 *   <li>{@link #emitOrbitGeometry}：几何专属层的粒子（环面留痕桩 + 出生整圈标记）——
 *       它<b>不随魔素变</b>，也不进任何风格档案。</li>
 * </ol>
 *
 * <p><b>状态仍住在宿主</b>：{@code orbitAnchorUuid} / {@code orbitRadius} /
 * {@code orbitAngularSpeed} / {@code orbitPhase} / {@code orbitCurrentPhase} /
 * {@code orbitAnchorPos} / {@code orbitRingNormal} 七个字段是
 * {@link AbstractChargerWaveEntity} 的字段（本批只把其中四个由 {@code private} 放宽到包级私有，
 * <b>声明与"无初始化器 ⇒ 默认关闭"的形状一个字未动</b>），本类只读/写它们。
 * 位置的<b>唯一写入点</b>仍是宿主覆写的 {@code setPos}（自走/外力搬运的判定在那里）。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有三类 —— {@code private} → 包级私有、宿主字段被搬动
 * 逼出的 {@code host.} 限定、以及两个<b>公开</b>静态方法 {@code orbitDirectionOf} /
 * {@code orbitPlaneAxes} 改用静态导入（它们仍声明在 {@link AbstractChargerWaveEntity}，
 * 签名与实现一个字未动）。<b>两条闸门（要素未设 ⇒ 第一句返回、锚点不活 ⇒ 返回 false）、
 * 环平面基向量的取法、诊断文案与几何粒子的颗数/散布一个字未动。</b></p>
 */
final class WaveOrbitElement {

	private WaveOrbitElement() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * <b>环绕波心跳日志的节流刻度</b>（每几 tick 一行位置行）：20 tick = 1 秒一行。
	 *
	 * <p>环绕波寿命上限与主波同为 {@code AbstractChargerWaveEntity#MAX_LIFETIME_TICKS} tick
	 * ⇒ 单枚最多 10 行心跳，量级与"发生了多少事"成正比（与 {@link WaveDiag} 的 trace 通道口径一致）；
	 * 取 20 而不是更小，是为了在"证明真的在绕"与"不刷屏"之间取平衡。</p>
	 */
	static final int ORBIT_HEARTBEAT_TICKS = 20;

	/**
	 * <b>环绕波要素</b>（可选、默认关闭，见 {@link AbstractChargerWaveEntity} 的
	 * {@code orbitAnchorUuid}）：把自身位置改写成
	 * <b>{@code 锚点位置 + r × (u·cosθ + v·sinθ)}</b>。
	 *
	 * <p><b>2026-10-02 批 4（作者裁定 D9 = A）</b>：本文档原先逐字写的是"父波"——
	 * 锚点判据已从 {@code instanceof AbstractChargerWaveEntity} 放宽成
	 * {@code anchor != null && anchor.isAlive()}（见 {@link OrbitAnchor}），
	 * 锚点可以是<b>任何实体</b>（回旋镖的环绕技能就是拿镖当锚点）。
	 * 下文的"父波"一律读作"锚点"；对既有调用方（星芒嬗震传的是父波）两者是同一个对象，
	 * 取值与判据<b>逐字不变</b>。</p>
	 *
	 * <h2>坐标基（环平面垂直于锚点运动方向）</h2>
	 * <p>取锚点运动方向 {@code d}（{@link OrbitAnchor#orbitDirection()}；单位向量），
	 * 再取一个与本方向不平行的参考轴
	 * {@code reference}（{@code |d.y| > 0.9} 时用 +X，否则用 +Y——避免叉乘退化），
	 * 然后</p>
	 * <pre>
	 *   u = normalize(reference × d)      // 垂直于 d 的平面基之一
	 *   v = normalize(d × u)              // 与 u、d 都垂直（u × v = d，右手系）
	 * </pre>
	 * <p>于是 {@code u}、{@code v} 张成的平面<b>垂直于运动方向</b>，圆周点落在"以锚点为圆心、
	 * 垂直于飞行方向的环"上：θ = 0 时在 {@code u} 正方向一侧，θ 增大时按 u→v 方向旋转。
	 * 时间基用 {@code tickCount}（θ = 初始相位 + 角速度 × 已存活 tick）⇒ 不额外占字段。</p>
	 *
	 * <h2>默认关闭与两个边界</h2>
	 * <ul>
	 *   <li>要素未设（{@code orbitAnchorUuid == null}）⇒ <b>第一句就返回 true</b>，位置一个字不改
	 *       ——机器波 / 变器波 / 一切既有波的行为与改造前逐字相同；</li>
	 *   <li>客户端 / Ponder 场景（不是 {@link ServerLevel}）⇒ 不动位置（客户端位置由服务端同步，
	 *       客户端 tick 本来就在移动之前 return）；</li>
	 *   <li>锚点取不到或已消散（{@code isAlive() == false}）⇒ 返回 {@code false}，
	 *       调用方<b>立刻 {@code discard()}</b> ⇒ "锚点消散 ⇒ 环绕波一起收尾"只有这一条实现，
	 *       不再叠第二层机制，也不会留下孤立波（对镖同样成立：镖 {@code discard()} 后
	 *       环绕波下一 tick 自己收尾）。</li>
	 * </ul>
	 *
	 * @return {@code false} = 锚点已不存在，调用方必须 discard 自己
	 */
	static boolean applyOrbitElement(AbstractChargerWaveEntity host) {
		if (host.orbitAnchorUuid == null) {
			return true;
		}
		if (!(host.level() instanceof ServerLevel server)) {
			return true;
		}
		Entity anchor = server.getEntity(host.orbitAnchorUuid);
		// ★ 批 4（作者裁定 D9 = A）：锚点判据从"必须是另一枚波"放宽成"是个活着的实体"——
		// 回旋镖的环绕技能要拿**那枚镖**当锚点，而镖是 Projectile、不是波；旧判据会让环绕波
		// 出生即死（第一 tick 就查不到"父波"）。既有调用方（星芒嬗震）传的仍是波 ⇒ 走的还是
		// 下面同一条路径，行为逐字不变。
		if (anchor == null || !anchor.isAlive()) {
			return false;
		}
		// ★ "取运动方向"抽成契约（OrbitAnchor#orbitDirection）：波 = getMovement()（与改造前
		// 逐字同源）、镖 = getDeltaMovement()。非 OrbitAnchor 的实体回落到原版速度向量。
		Vec3 dir = orbitDirectionOf(anchor);
		if (dir.lengthSqr() < 1.0E-9D) {
			dir = host.movement;
		}
		if (dir.lengthSqr() < 1.0E-9D) {
			dir = new Vec3(0.0D, 0.0D, 1.0D);
		}
		dir = dir.normalize();
		Vec3[] axes = orbitPlaneAxes(dir);
		Vec3 u = axes[0];
		Vec3 v = axes[1];
		double theta = host.orbitPhase + host.orbitAngularSpeed * (double) host.tickCount;
		Vec3 anchorPos = anchor.position();
		Vec3 offset = u.scale(host.orbitRadius * Math.cos(theta)).add(v.scale(host.orbitRadius * Math.sin(theta)));
		host.setPos(anchorPos.add(offset));
		// 只记三个原始事实，供粒子/日志用（见三个字段的说明）；位置公式本身仍只有上面这一处。
		host.orbitCurrentPhase = theta;
		host.orbitAnchorPos = anchorPos;
		host.orbitRingNormal = dir;
		logOrbitDiag(host, anchorPos);
		return true;
	}

	/**
	 * 两位小数、<b>与区域设置无关</b>的格式化（日志要机器可比对：某些区域会把小数点写成逗号）。
	 */
	static String fmt2(double value) {
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	/**
	 * <b>环绕波的诊断行（调试可见性；作者 2026-10-02：「现在尝试添加调试行，我来进行测试有没有环绕波生成」）</b>。
	 *
	 * <p>三条时刻线，全部走 {@link WaveDiag#trace}（AGENTS 红线：波相关日志的唯一出口），
	 * <b>不新建第二套日志前缀</b>：</p>
	 * <ol>
	 *   <li><b>出生</b>（{@code tickCount == 1}）：父波 UUID、批次、半径、角速度（弧度/tick 与圈/秒）、
	 *       初始相位 —— 一眼能判"它到底生成了没有、按什么参数绕"；</li>
	 *   <li><b>位置心跳</b>（每 20 tick 一行，节流）：当前 tick、自身坐标、父波坐标、
	 *       <b>距父波距离</b>（恒等于半径 ⇒ 这就是"r≈0.8 真的在绕"的机器可判证据）、批次；</li>
	 *   <li><b>消散</b>：见 {@link AbstractChargerWaveEntity} 的 {@code remove(RemovalReason)}
	 *       （所有移除路径的唯一汇合点）。</li>
	 * </ol>
	 *
	 * <p>节流刻度刻意与"同批次豁免碰撞"那行（{@code tickCount % 20}）取同一拍：
	 * 一次发射的环绕波与并排主波在同一 tick 打日志，读起来能对齐。</p>
	 */
	static void logOrbitDiag(AbstractChargerWaveEntity host, Vec3 anchorPos) {
		if (host.tickCount == 1) {
			WaveDiag.trace(
				"环绕波出生：{} 级波（{}），父波 UUID {}，批次 {}（继承父波），半径 {} 格、角速度 {} 弧度/tick（{} 圈/秒）、起始相位 {}；位置 = 父波位置 + r×(u·cosθ + v·sinθ) 逐 tick 改写",
				host.waveLevel, WaveLevels.glyph(host.waveLevel), host.orbitAnchorUuid, host.getFiringBatch(),
				fmt2(host.orbitRadius), fmt2(host.orbitAngularSpeed),
				fmt2(host.orbitAngularSpeed * (double) ChargeConfigs.TICKS_PER_SECOND / (Math.PI * 2.0D)),
				fmt2(host.orbitPhase));
		} else if (host.tickCount % ORBIT_HEARTBEAT_TICKS == 0) {
			WaveDiag.trace(
				"环绕波心跳：tick {}，自身 {}，父波 {}，距父波 {} 格（恒 = 半径 {} 格），批次 {}",
				host.tickCount, host.position(), anchorPos, fmt2(host.position().distanceTo(anchorPos)),
				fmt2(host.orbitRadius), host.getFiringBatch());
		}
	}

	/**
	 * <b>几何专属层的粒子</b>：只有"这枚波在环绕"这件事能产生的那两簇
	 * （2026-10-03 需求 coe-ess §3.4 的 B 层；只在服务端调用，唯一调用点是
	 * {@link AbstractChargerWaveEntity} 的 {@code tick()} 里 {@code isOrbiting()} 的那条分支）。
	 *
	 * <p><b>本方法刻意只画几何</b>——环绕波的<b>风格粒子</b>（主体尘埃 + 该魔素的点缀，包括
	 * "异"那套 END_ROD / 青焰）已并入风格层，与主波走同一个
	 * {@link ChargerWaveFx#sendTrail} 调用。留在这里的两件东西都依赖环平面：
	 * <b>环面留痕桩</b>（{@link ChargerWaveFx#sendOrbitMarks}）与<b>出生整圈标记</b>
	 * （{@link ChargerWaveFx#burstOrbitSpawn}）。它们<b>不随魔素变</b>，也不进任何风格档案 ——
	 * 主波没有环平面，把桩点塞进"异"档案会在平飞路径上画出一圈没有意义的点。</p>
	 *
	 * <p>注意本方法<b>只负责粒子</b>：调用点不等价于 {@code return}，环绕波照样往下走命中判定、
	 * 方块碰撞、波波碰撞与寿命/收尾分支 —— 观感与机制在这里是分开的两件事。</p>
	 *
	 * <p>三个时刻各来一簇（作者 2026-10-02 要求："出生、命中、父波消散一起收尾"三处都要能感知
	 * 它存在过）：</p>
	 * <ol>
	 *   <li><b>出生</b>（{@code tickCount == 1}，实体刚被放进世界的第一 tick）：在父波位置炸一簇
	 *       {@link ChargerWaveFx#burstOrbitSpawn}，并把整圈轨道一次标出来 —— 玩家发射后立刻能
	 *       看到主波周围多了一个环；</li>
	 *   <li><b>每 tick</b>：每逢 {@link ChargerWaveFx#ORBIT_MARK_INTERVAL_TICKS} 补一圈环面留痕桩
	 *       （{@link ChargerWaveFx#sendOrbitMarks}）；</li>
	 *   <li><b>命中与消散</b>：命中走既有的命中链（{@code hitEffect → ChargerWaveFx.burst →
	 *       discard}，用的是环绕波自己的波级色与其 {@code trailStyle()}）；其余任何移除路径
	 *       （父波没了、寿命与行程上限）在 {@code remove} 里补最后一簇 —— 见那里。</li>
	 * </ol>
	 */
	static void emitOrbitGeometry(AbstractChargerWaveEntity host, ServerLevel server) {
		Vec3 anchorPos = host.orbitAnchorPos;
		if (anchorPos == null) {
			// 本 tick 还没成功改写位置（理论上不会发生：位置改写就在本 tick 移动段里）——
			// 保守退化：这一 tick 不画任何几何粒子（宁可不画，也不画出错位的圈）。
			return;
		}
		// 环平面法向取"位置改写实际用的那个"（见 orbitRingNormal 的说明）：父波方向被能量场掰弯时，
		// 粒子圈必须跟着同一套几何，才不会画出一个与真实轨道不平行的环。
		Vec3 normal = host.orbitRingNormal != null ? host.orbitRingNormal : host.movement;
		Vec3[] axes = orbitPlaneAxes(normal);
		// 出生簇：实体加入世界后的第一 tick（tickCount 在 super.tick() 里 +1，故首 tick 读作 1）
		if (host.tickCount == 1) {
			ChargerWaveFx.burstOrbitSpawn(server, anchorPos, host.renderColor, axes[0], axes[1], host.orbitRadius,
				host.orbitCurrentPhase);
		}
		ChargerWaveFx.sendOrbitMarks(server, host.position(), axes[0], axes[1],
			host.orbitRadius, host.orbitCurrentPhase, host.tickCount % ChargerWaveFx.ORBIT_MARK_INTERVAL_TICKS == 0);
	}
}
