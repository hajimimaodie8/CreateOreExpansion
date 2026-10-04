package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * <b>回旋镖的环绕技能</b>（2026-10-03 行为零变化拆分，从 {@code AbstractBoomerangEntity} 逐字搬出）。
 *
 * <p>投掷时挂上 L 枚环绕波：魔素抽签池 {@link #ORBIT_ESSENCE_POOL}、负数批次号
 * {@link #nextOrbitBatch()}、唯一生成处 {@link #spawnOrbitWaves(AbstractBoomerangEntity, ItemStack, Vec3)}。
 * 镖自己作为环绕锚点的两个契约实现（{@code orbitDirection} / {@code orbitMineBlock}）仍留在
 * {@link AbstractBoomerangEntity} —— 它们是 {@code @Override}，必须由实体类声明。</p>
 *
 * <h2>七、批 4（2026-10-02）：环绕技能（需求 §3.6 / §3.7）</h2>
 * <p>投掷时挂上 <b>L 枚环绕波</b>（L = 有效技能等级，1..5），锚点就是<b>这枚镖</b>：
 * 镖实现 {@link OrbitAnchor}（契约由作者裁定 D9 = A 放宽："锚点不必是波"），
 * 于是既有的环绕波要素（半径 / 角速度 / 相位 / 垂面几何 / "锚点没了就收尾"）
 * <b>一个字都不用改</b>就服务了回旋镖。要点：</p>
 * <ul>
 *   <li><b>数量 = L</b>（{@code BoomerangSkillConfigs#orbitCount}）；
 *       <b>半径 1.5</b>、<b>角速度 1 圈/秒</b>、基相位 0、第 i 枚相位 {@code 2π·i/L}
 *       （均匀铺满一圈，否则 L 枚会重合成一枚）；</li>
 *   <li><b>平面 = 镖运动方向的垂面</b>（{@link AbstractBoomerangEntity#orbitDirection()} = {@code getDeltaMovement()}，
 *       既有几何负责把它变成两个基向量）；</li>
 *   <li><b>同批豁免</b>：L 枚共用一个<b>负数</b>批次号（见 {@link #nextOrbitBatch()}）——
 *       镖不是波、没有批次号，所以这里自造一个来源；负数是为了与星芒嬗震的<b>正数</b>序列
 *       永不相等（否则两组批次可能撞号 ⇒ 两枚本该湮灭的波互相豁免）。机器波仍是批次 0
 *       （= "不属于任何批次"）⇒ 长期口径不变；</li>
 *   <li><b>伤害 2×L</b>：走 {@code ChargerWaveEntity} 的"自定义伤害"要素（波形仍是既有实体、
 *       仍是 ATTACK 波型），<b>撞生物 ⇒ 该枚消失</b>（既有波的命中语义，不需要新代码）；</li>
 *   <li><b>撞方块 ⇒ 挖掉 + 该枚消失</b>：{@link AbstractBoomerangEntity#orbitMineBlock(BlockPos)} 直接复用
 *       {@link BoomerangMining#mineBlock}（同一条 {@code maxHardness} / {@code miningLevel} / 原版进度判定，
 *       同样 −1 耐久）；掉落物与经验由原版生成在世界里，靠镖既有的回程吸附
 *       （{@link BoomerangPickup#pickUpItems()}）带走、{@link BoomerangTails#finishFlight(boolean)} 交给玩家（零新机制）；</li>
 *   <li><b>收尾</b>：镖 {@code discard()} 后环绕波下一 tick 自己收尾（既有
 *       "锚点没了即 discard"语义）；</li>
 *   <li><b>消耗 15×L</b>：与模式消耗、穿刺 20×L 相加在 {@code BoomerangItem#throwCost} 同一处。</li>
 * </ul>
 */
final class BoomerangOrbitWaves {

	private BoomerangOrbitWaves() {
	}

	/**
	 * <b>环绕波的魔素抽签池</b>（2026-10-03 需求 coe-boom2 批 3 §3.1）：每枚环绕波<b>各自</b>
	 * 从这 <b>8 种全部</b>魔素里随机抽一种（水/火/地/风/冰/雷/毒/异）。
	 *
	 * <p><b>为什么是 8 种（含"异"）</b>：星芒嬗震那边排除 {@code ARCANE}，是因为它的<b>主波已经
	 * 固定占了"异"</b>（见 {@code StarShockRuntime#ORBIT_ESSENCE_POOL} 那个 7 值池）。
	 * 回旋镖这一侧<b>没有"主波占掉一个魔素"这回事</b>（镖本身不是波、不设魔素）⇒
	 * 没有理由排除任何一种，8 种全在池里。</p>
	 *
	 * <p><b>名单只有这一份</b>：{@link #spawnOrbitWaves} 那一次掷骰直接按本数组取，顺序照
	 * {@code WaveTrailStyle} 的<b>声明序</b>排，便于与枚举逐字对照。关卡
	 * {@code boomerang-orbit-essence-pool} 正向钉"这 8 个就是枚举的全部 8 个魔素、顺序一致"，
	 * 反向钉"本文件里每个魔素名只出现一次"；另一条
	 * {@code boomerang-orbit-essence-invariant} 把本池与星芒嬗震的 7 值池对照：
	 * <b>并集 = 8、交集 = 7</b>（两个池各自只有一份）。</p>
	 *
	 * <p><b>不去重、不排除连续相同</b>（与星芒嬗震同口径）：同一次投掷的 L 枚可以互不相同、
	 * 也可以连着抽到同一种；结果只由那一次掷骰决定，池里没有任何状态。</p>
	 */
	private static final WaveTrailStyle[] ORBIT_ESSENCE_POOL = {
		WaveTrailStyle.WATER, WaveTrailStyle.FIRE, WaveTrailStyle.EARTH, WaveTrailStyle.WIND,
		WaveTrailStyle.ICE, WaveTrailStyle.LIGHTNING, WaveTrailStyle.POISON, WaveTrailStyle.ARCANE
	};

	/**
	 * <b>环绕波批次号计数器</b>（服务端权威、进程内自减 ⇒ 分配出来的批次号<b>恒为负数</b>）。
	 *
	 * <p><b>为什么自造一个来源</b>：豁免判据 {@code sameFiringBatch} 比较的是"两枚波是不是
	 * 同一次发射"，而<b>镖不是波、根本没有批次号</b>（唯一既有的分配器
	 * {@code StarShockRuntime#nextBatch()} 是星界套的私有实现，本项目口径也不让回旋镖依赖它）。
	 * 所以这里造一个只用给"这一次投掷的 L 枚环绕波"的号，让它们<b>彼此不湮灭</b>。</p>
	 *
	 * <p><b>为什么是负数（与"批次 0"和星芒嬗震都不撞号）</b>：{@code sameFiringBatch} 要求
	 * 双方都非 0 且相等，而<b>机器波恒为 0</b>（"不属于任何批次"）⇒ 负数天然不等于 0；
	 * 星芒嬗震的序列是<b>正数</b>（{@code BATCH_SEQUENCE++}，从 1 起）⇒
	 * 两个来源的值域不相交，永远不可能出现"一次星界技能发射与一次回旋镖投掷撞号"，
	 * 也就不会出现"两枚本应互相湮灭的波互相豁免"（长期口径：任意两波相交即爆炸湮灭）。</p>
	 *
	 * <p>位宽 32 位、只在同一次投掷内部比较 ⇒ 与既有批次号同一种"进程内短标识"口径。</p>
	 */
	private static int ORBIT_BATCH_SEQUENCE = 0;

	/**
	 * 分配一个<b>环绕波批次号</b>（{@link #ORBIT_BATCH_SEQUENCE}；<b>恒 &lt; 0</b> ⇒ 恒非 0）。
	 *
	 * <p>回绕兜底：{@code int} 减到 {@code Integer.MIN_VALUE} 再减会变成正数
	 * （= 可能与星芒嬗震的序列撞号），所以到 0 就绕回 {@code -1}。正常游戏里不可能发生，
	 * 但"绕回正数"这条路径正是必须堵掉的（它与"批次号 0"是同一类静默失效）。</p>
	 */
	static synchronized int nextOrbitBatch() {
		ORBIT_BATCH_SEQUENCE--;
		if (ORBIT_BATCH_SEQUENCE >= 0) {
			ORBIT_BATCH_SEQUENCE = -1;
		}
		return ORBIT_BATCH_SEQUENCE;
	}

	/**
	 * ★ <b>投掷时挂上环绕技能：生成 L 枚环绕波</b>（需求 §3.6；批 4 的唯一生成处）。
	 *
	 * <p>由 {@code BoomerangItem#releaseUsing} 在镖<b>已入世界之后</b>调用一次
	 * （点按与长按<b>都</b>调 —— 需求 §六 推断值 #9：技能不需要开关、两种模式都生效）。</p>
	 *
	 * <p>每一枚都是<b>既有波实体</b>（{@code ChargerWaveEntity}，与充能器/星芒嬗震同一种），
	 * 只设三个要素：</p>
	 * <ol>
	 *   <li><b>波型 = ATTACK</b>（{@code trySetWaveType}）：既有命中链里"造成伤害"那一支的
	 *       门槛就是 {@code getWaveType().dealsDamage()}，不设它环绕波就只是个观光粒子；</li>
	 *   <li><b>自定义伤害 = 2 × 等级</b>（{@code ChargerWaveEntity#setCustomDamage}）——
	 *       正是作者裁定 D9(A) 的"伤害可覆写"，<b>不</b>另写一套波级表；</li>
	 *   <li><b>环绕要素</b>（{@code setOrbitAnchor}）：锚点 = 这枚镖的 UUID、半径 1.5、
	 *       角速度 1 圈/秒、相位 {@code 基相位 + 2π·i/L}（均匀铺满一圈）。</li>
	 * </ol>
	 * <p>外加<b>同一个批次号</b>（同一次投掷的 L 枚彼此不湮灭；见 {@link #nextOrbitBatch()}）。</p>
	 *
	 * <p>⚠ <b>不新增实体类型 / 贴图 / 模型 / 渲染器</b>：用的是既有 {@code charger_wave}，
	 * 视觉仍走既有的环绕波粒子（关卡守着这一条）。</p>
	 *
	 * @param stack     投掷的那一把镖（读取有效技能等级：基准等级取本档 + 技艺提升/回溯，钳 1..5）
	 * @param flightDir 出手方向（点按 = 镖刚拿到的速度向量；长按 = 投掷那一刻的准心方向，
	 *                  因为花瓣段第一 tick 还没有位移）——它同时是环绕波的出生朝向，
	 *                  而每 tick 的环平面法向仍从锚点<b>实时</b>取（镖转弯时环跟着转）
	 */
	static void spawnOrbitWaves(AbstractBoomerangEntity host, ItemStack stack, Vec3 flightDir) {
		// ★ 第一道门：**这次投掷必须携带环绕技能**（投掷那一刻按住了键二；作者 2026-10-02
		// 第二次裁定）。不按技能键 ⇒ 一枚都不生成。判据是投掷时写在实体上的标记，
		// **不是**这里现读按键（"投掷那一刻读一次"是唯一口径，见 setCarriedSkills）。
		if (!host.isOrbitSkillCarried()) {
			return;
		}
		if (!(host.level() instanceof ServerLevel server)) {
			return; // 实体由服务端生成（两端各造一枚就成双份）
		}
		int level = BoomerangItem.effectiveSkillLevel(stack, host.tier().baseSkillLevel());
		int count = BoomerangSkillConfigs.orbitCount(level);
		float damage = BoomerangSkillConfigs.orbitDamage(level);
		int batch = nextOrbitBatch();
		Vec3 dir = flightDir == null ? Vec3.ZERO : flightDir;
		for (int i = 0; i < count; i++) {
			// 既有波实体、既有构造（与充能器/星芒嬗震同一个）：
			// 出生点 = 镖此刻的位置（第一 tick 就会被环绕要素改写到环上）。
			ChargerWaveEntity orbit = new ChargerWaveEntity(host.level(), host.position(), dir,
				BoomerangSkillConfigs.ORBIT_WAVE_LEVEL);
			orbit.setFiringBatch(batch); // 同一次投掷的 L 枚共用一个（负数）批次号
			orbit.trySetWaveType(WaveTypes.ATTACK); // 要素 4：伤害那一支的门槛
			// 要素 5（2026-10-03 需求 coe-boom2 批 3 §3.1）：魔素 = 8 种里<b>每枚各自随机</b>抽一种
			// （含"异"——回旋镖这边没有"主波占掉一个魔素"这回事）。
			// ⚠ <b>必须紧跟在 trySetWaveType(ATTACK) 之后</b>：trySetEssence 对"不造成伤害的波型"
			// 直接 return false —— <b>静默空操作</b>（不报错、不打日志），顺序反了整批都没有魔素、
			// 观感退回波型默认（火）。随机源用该波/世界的既有 RandomSource
			// （{@code level().random}，服务端权威；禁自造随机源：新 Random / Math.random / RandomSource.create）。
			orbit.trySetEssence(ORBIT_ESSENCE_POOL[host.level().random.nextInt(ORBIT_ESSENCE_POOL.length)]);
			// 主人 = <b>投掷玩家本人</b>（2026-10-03 需求 coe-boom2 批 1 §3.2）：本批只<b>赋</b>不<b>排</b>
			// —— 环绕波会绕着镖飞、离玩家很近（半径 1.5 格），"不伤发射者"要等批 2 改命中谓词。
			// 取 {@code Projectile#getOwner()}（投掷者），<b>不是镖的 UUID</b>：镖 UUID 已经用作
			// 环绕锚点（下面 setOrbitAnchor 的 getUUID()），两者语义不同、不能混用。
			orbit.setOwner(host.getOwner());
			orbit.setCustomDamage(damage); // 2 × 等级（既有实体的可选自定义伤害）
			orbit.setOrbitAnchor(host.getUUID(), BoomerangSkillConfigs.ORBIT_RADIUS,
				BoomerangSkillConfigs.ORBIT_ANGULAR_SPEED,
				BoomerangSkillConfigs.ORBIT_PHASE + Math.PI * 2.0D * (double) i / (double) count);
			server.addFreshEntity(orbit);
		}
	}
}
