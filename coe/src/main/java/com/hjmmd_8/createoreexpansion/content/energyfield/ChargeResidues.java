package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * ★ <b>电荷残留（coe-charge 批 7；需求 §3.5 #6~#9 与 §3.8）</b>—— 中和爆炸在爆炸中心留下的
 * 一块「会传染的电荷区域」。
 *
 * <h2>它是什么、住在哪</h2>
 * <p>一条残留 = <b>一块方形区域</b>（中和爆炸那个立方体，见 {@link #regionOf}）+ <b>到期时刻</b>
 * + <b>一个极性</b> + <b>一份已染电实体的账本</b>（{@link Residue#charged()}）。它是
 * <b>纯数据</b>，没有实体、没有方块、没有注册项：<b>零新注册</b>是作者裁定的硬条件，
 * 而需求 §六 #11 给的两条路（不可见残留实体 / 复用既有区域存储）里本批<b>选了后者</b>
 * —— 多注册一个实体类型要动 {@code Registries} 与发布形态（三个模块 jar 的自足性），
 * 换来的只是「残留能被 {@code /kill}」这种没人要的能力。</p>
 *
 * <h2>内存表 + SavedData（老存档缺它 = 无残留）</h2>
 * <p>残留表<b>就住在维度存档对象里</b>（{@link ChargeResidueData}，逐维度一份，名字 =
 * {@link ChargeConfigs#RESIDUE_DATA_NAME}）：它平时是内存里的一个 {@code List}
 * （维度加载期间常驻，tick 直接读它，没有第二份拷贝可以跟它走散），
 * 随维度一起 {@code save}/{@code load}。⇒ 退出重进后残留与<b>它那份账本</b>都还在
 * （关卡与实机验收都钉这一条）。</p>
 * <p><b>为什么不做成「静态表 + 每维度落盘」</b>（{@code EnergyFields} 那种形状）：
 * 那样就必须在<b>维度加载</b>事件里补一次 {@code restore}，忘掉那一句的后果是
 * 「残留表在重进世界后是空的」—— 一个没有任何报错、静态关卡也全绿的静默少一条
 * （本仓反复踩的形状）。住在 {@code SavedData} 里则<b>不可能忘</b>：读表和落盘是同一个对象。</p>
 * <p><b>老存档的缺文件行为</b>：见 {@link ChargeConfigs#RESIDUE_DATA_NAME} 的 javadoc
 * —— 维度数据存储对不存在的文件直接调构造器（空表），{@code load} 只在文件存在时被调用，
 * 所以老存档里的残留数恒为 0，不需要任何迁移。</p>
 *
 * <h2>生命期与表现（数值全部按名取自 {@link ChargeConfigs}）</h2>
 * <ol>
 *   <li><b>寿命</b> = {@link ChargeConfigs#residueLifetimeTicks(int)}（{@code round((5+ln(L+1))×20)}）；
 *       到点由本类的 tick 删掉（不是「时间一到就自己消失」的实体，所以删除点只有一个）；</li>
 *   <li><b>粒子</b>：每 {@link ChargeConfigs#RESIDUE_PARTICLE_INTERVAL_TICKS} tick 发一次，
 *       颗数 = {@link ChargeConfigs#residueParticleCount(int)}（两色各一份），
 *       散布 = 残留区域半边长、速度沿用爆炸那一个常量。⚠ <b>不发 {@code FLASH}</b>：
 *       那是「一次中和恰好一颗」的爆炸专用表现（需求 §3.8）；</li>
 *   <li><b>触发</b>：区域内、<b>活着的</b>生物 —— 已带电的跳过（与既有「已带电则跳过」同口径）、
 *       本残留已经给过电的跳过（{@link Residue#charged()} 账本，见下）、免疫（
 *       {@code canBeAffected} 为 false）由 {@link ChargeApi#applyResidue} 的返回值如实回报
 *       ⇒ <b>只有真的染上了电才记账</b>。</li>
 * </ol>
 *
 * <h2>★ 防无限连锁：三道闸（需求 §3.5「必须处理的一个设计洞」）</h2>
 * <ol>
 *   <li><b>同一实体对同一块残留只染一次</b>：账本 = 每条残留自己的 {@code Set<UUID>}，
 *       而这份账本<b>随 {@link ChargeResidueData} 一起入档</b>（「别只放内存」是作者点名的
 *       条件：放内存的话，重进世界会让同一条残留把同一个实体再染一次）；</li>
 *   <li><b>已带电的实体不再给</b>：区域里站着但身上还带电的生物直接跳过 ⇒ 残留不会把
 *       「刚被别的来源染过」的实体翻面（那会当场触发中和）；</li>
 *   <li><b>★ 残留不再生残留</b>：因残留而染上的那笔电带标记，
 *       它参与中和时<b>不再留下新残留</b>（实现见 {@link ChargeApi#applyResidue} 与
 *       {@code ChargeApi#leaveResidue} 的 {@code fromResidue} 分支）。
 *       前两道闸只能保证「一块残留给同一个实体一次」，链条真正的出口是这一道：
 *       没有它，残留 → 染电 → 中和 → 新残留 → …… 会一直滚下去。</li>
 * </ol>
 * <p>第四条（需求 §六 #7 的「残留间爆炸最小间隔」）<b>不需要新机制</b>：残留引发的中和走的
 * 仍是 {@code ChargeApi} 那一处 {@code detonate(..)}，而它对双方记账
 * （{@link ChargeConfigs#NEUTRALIZE_COOLDOWN_TICKS}）⇒ 同一对实体在窗口内不会再爆。</p>
 *
 * <h2>残留的极性（★ 需求留白处，本批定的口径）</h2>
 * <p>需求 §3.5 #8 只写「生物接触残留 ⇒ <b>随机</b>染上正电或负电」。本批把它落成
 * <b>每条残留自己的极性、在生成那一刻随机抽一次</b>（{@code level.getRandom()}
 * —— 维度自己的 {@code RandomSource}，同种子可复现），随后对每个接触者给的都是这一极。
 * 依据：① 残留是一「块」区域，整块是同一极才读得出「这里是正电残留区」；
 * ② 若改成「每个生物各抽一次」，同一条残留就能同时给出正负两极，区域里两只刚被染电的
 * 生物会立刻互相中和 —— 表现上更像随机爆炸而不是残留。
 * ⚠ 这是留白处的选择，<b>不是需求原文</b>；要改成「逐个生物抽」时改的是
 * {@link #infect} 一处（把 {@code residue.polarity()} 换成
 * {@code target.getRandom().nextBoolean()}），其余不动。</p>
 *
 * <p><b>不碰的东西</b>（需求 §四）：{@code EnergyField} / {@code EnergyFields} /
 * {@code FieldedEntity}（批 8 的带电生物受场）、两个效果类、注册器。<b>不 import 任何可选模组类</b>。</p>
 */
public final class ChargeResidues {

	private ChargeResidues() {
	}

	/**
	 * <b>一条残留</b>（纯数据）。
	 *
	 * @param x              爆炸中心（区域中心）
	 * @param y              同上
	 * @param z              同上
	 * @param explosionLevel 中和爆炸等级 {@code L}（寿命、给的等级、区域大小、粒子颗数都由它派生）
	 * @param polarity       这条残留的极性（生成时抽一次，之后不再变）
	 * @param expiresAt      到期时刻（{@code level.getGameTime()} 绝对值，写法与中和冷却键同口径：
	 *                       缺失/新对象读出 {@code 0} ⇒ 立即过期，不会误判成「还有效」）
	 * @param charged        <b>已经被这条残留染过电的实体</b>账本（{@link UUID}；
	 *                       这个 {@code Set} 是可变的 <b>且随存档持久化</b>——见
	 *                       {@link ChargeResidueData#save}）
	 */
	public record Residue(double x, double y, double z, int explosionLevel, ChargePolarity polarity,
			long expiresAt, Set<UUID> charged) {
	}

	/**
	 * ★ <b>中和爆炸留下一条残留</b> —— 由 {@code ChargeApi} 的中和核心调用，全仓<b>唯一</b>的
	 * 残留生成点（关卡有负向断言：别处不许再建残留）。
	 *
	 * @param level         中和发生的维度（残留按维度记账；只有 {@code ServerLevel} 有存档）
	 * @param x             爆炸中心 x
	 * @param y             爆炸中心 y
	 * @param z             爆炸中心 z
	 * @param explosionLevel 中和爆炸等级 {@code L}（{@code min(两极等级)}）
	 */
	public static void spawn(ServerLevel level, double x, double y, double z, int explosionLevel) {
		ChargePolarity polarity = level.getRandom().nextBoolean()
			? ChargePolarity.POSITIVE
			: ChargePolarity.NEGATIVE;
		long expiresAt = level.getGameTime() + ChargeConfigs.residueLifetimeTicks(explosionLevel);
		Residue residue = new Residue(x, y, z, explosionLevel, polarity, expiresAt, new HashSet<>());
		ChargeResidueData data = ChargeResidueData.get(level);
		data.residues().add(residue);
		data.setDirty();
		CoeCore.LOGGER.info("[电荷残留] 中和点 {} 留下{}残留：爆炸等级 Lv{}、存活 {} tick、区域半边长 {} 格",
			residue, label(polarity), explosionLevel, ChargeConfigs.residueLifetimeTicks(explosionLevel),
			ChargeConfigs.neutralizeChebyshevRadius(explosionLevel));
	}

	/**
	 * <b>每服务端 tick 一次</b>（由 {@code ChargeResidueHandler} 对每个维度调用）——
	 * 三件事，顺序即语义：先筛掉到期的（它们不再表现、也不再给电），再对留下的发粒子、查接触。
	 *
	 * <p><b>为什么没有残留时几乎零开销</b>：{@link ChargeResidueData#find} 是一次维度数据存储的
	 * 查表，<b>空存档读出来就是 {@code null}</b>（不会凭空造一张表、也不会往老存档里写空标签）。</p>
	 */
	public static void tick(ServerLevel level) {
		ChargeResidueData data = ChargeResidueData.find(level);
		if (data == null) {
			return; // 这个维度从来没有过残留（老存档常态）：一次查表就结束
		}
		List<Residue> residues = data.residues();
		if (residues.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		boolean dirty = residues.removeIf(residue -> residue.expiresAt() <= now);
		for (Residue residue : residues) {
			emit(level, residue, now);
			dirty |= infect(level, residue);
		}
		if (dirty) {
			data.setDirty();
		}
	}

	/**
	 * <b>残留区域的判据（唯一一处）</b>：以残留中心为心、Chebyshev 半径
	 * {@link ChargeConfigs#neutralizeChebyshevRadius(int)} 的立方体 —— 与中和爆炸<b>同一个区域</b>
	 * （需求 §3.5 #6 原话「在<b>该区域内</b>留下残留」）。
	 */
	private static AABB regionOf(Residue residue) {
		double radius = ChargeConfigs.neutralizeChebyshevRadius(residue.explosionLevel());
		return new AABB(residue.x() - radius, residue.y() - radius, residue.z() - radius,
			residue.x() + radius, residue.y() + radius, residue.z() + radius);
	}

	/**
	 * <b>稀疏粒子</b>：每 {@link ChargeConfigs#RESIDUE_PARTICLE_INTERVAL_TICKS} tick 一次，
	 * 两色各 {@link ChargeConfigs#residueParticleCount(int)} 颗，散布 = 残留区域半边长。
	 *
	 * <p>⚠ <b>不发 {@code FLASH}</b>：那个粒子是「一次中和恰好一颗」的爆炸专用表现
	 * （{@link ChargeConfigs#NEUTRALIZE_FLASH_COUNT}），残留每 10 tick 发一次，跟着刷就是
	 * 把「⚠ 单次，别每 tick 刷」那条口径反过来做。</p>
	 */
	private static void emit(ServerLevel level, Residue residue, long now) {
		if (now % ChargeConfigs.RESIDUE_PARTICLE_INTERVAL_TICKS != 0) {
			return;
		}
		int count = ChargeConfigs.residueParticleCount(residue.explosionLevel());
		double spread = ChargeConfigs.neutralizeChebyshevRadius(residue.explosionLevel());
		double speed = ChargeConfigs.NEUTRALIZE_PARTICLE_SPEED;
		level.sendParticles(ChargeConfigs.PARTICLE_NEUTRALIZE_POSITIVE,
			residue.x(), residue.y(), residue.z(), count, spread, spread, spread, speed);
		level.sendParticles(ChargeConfigs.PARTICLE_NEUTRALIZE_NEGATIVE,
			residue.x(), residue.y(), residue.z(), count, spread, spread, spread, speed);
	}

	/**
	 * <b>接触传染</b>：区域里每个活着的生物，满足「不带电 + 本残留没给过它」就给一次电。
	 *
	 * <p>三道判据的来源见类注释「防无限连锁」；这里只强调两点写法：</p>
	 * <ul>
	 *   <li><b>先问 {@link ChargeApi#hasCharge}</b>，不是先问账本 —— 顺序反了也不会错，
	 *       但先问带电是「与既有口径同一句话」（特斯拉线圈那条途径就是这么跳过的）；</li>
	 *   <li><b>只有 {@link ChargeApi#applyResidue} 返回 true 才记账</b>：被免疫
	 *       （{@code canBeAffected} 为 false）拦下时什么都没加上，记进账本会让
	 *       「残留没给过它电」这件事变成谎话（免疫是终身的，所以下个 tick 再问一次也不亏）。</li>
	 * </ul>
	 *
	 * @return 本次是否改动了账本（需要落盘）
	 */
	private static boolean infect(ServerLevel level, Residue residue) {
		AABB region = regionOf(residue);
		int inflictedLevel = ChargeConfigs.residueInflictedLevel(residue.explosionLevel());
		boolean recorded = false;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, region, LivingEntity::isAlive)) {
			if (ChargeApi.hasCharge(target)) {
				continue; // 已带电的实体不再给（需求 §3.5 的防连锁：别当场把它翻面）
			}
			if (residue.charged().contains(target.getUUID())) {
				continue; // 同一实体对同一块残留只染一次（需求 §六 #7）
			}
			if (!ChargeApi.applyResidue(target, residue.polarity(), inflictedLevel)) {
				continue; // 免疫：什么都没加上 ⇒ 不记账（下个 tick 再说）
			}
			residue.charged().add(target.getUUID());
			recorded = true;
			CoeCore.LOGGER.info("[电荷残留] {} 接触残留，被染上{}（等级 Lv{}、持续 {} tick）",
				target.getName().getString(), label(residue.polarity()),
				inflictedLevel, ChargeConfigs.durationTicks(inflictedLevel));
		}
		return recorded;
	}

	/** 极性 → 日志用中文标签（只服务本类的两行日志，不参与任何判定）。 */
	private static String label(ChargePolarity polarity) {
		return polarity == ChargePolarity.POSITIVE ? "正电" : "负电";
	}
}
