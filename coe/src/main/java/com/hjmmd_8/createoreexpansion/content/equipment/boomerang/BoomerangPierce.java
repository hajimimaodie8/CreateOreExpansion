package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

/**
 * <b>穿刺技能：穿透额度的惰性初始化 + 命中之后"要不要掉头"的唯一判定</b>
 * （2026-10-04 行为零变化拆分，从 {@link BoomerangImpact} 逐字搬出 —— 那个类当时自述"管三件事"
 * （射线命中判定 / 命中之后的两条处理 / 掉头与额度），第三件就是本类）。
 *
 * <p>射线与两条命中处理仍在 {@link BoomerangImpact}；额度本身是<b>实体状态</b>
 * （{@code AbstractBoomerangEntity#pierceMobsLeft} / {@code pierceBlocksLeft}，字段仍 private，
 * 只经实体那四个一行包级访问器读写）。全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖。</p>
 *
 * <h2>六、批 3（2026-10-02）：穿刺技能（需求 §3.5 / §3.6 的"镖本身"那一半）</h2>
 * <p>四把镖都带 {@code createoreexpansion:pierce}（基准等级 1/2/3/3、钳到 5），
 * 不需要开关、点按与长按都生效（2026-10-02 第二次裁定改成"按住技能键才携带"，见
 * {@link AbstractBoomerangEntity} 的技能携带标记那一节）。额度<b>每次投掷各一份</b>：实体是每次投掷
 * 新建的，账记在实体的两个额度字段上，由 {@link #ensurePierceQuota(AbstractBoomerangEntity)} 从镖的栈
 * 现读等级算一次（{@link BoomerangSkillConfigs} 的 {@code 3L} / {@code 5L}）。三条规则：</p>
 * <ol>
 *   <li><b>额度用完即掉头</b>（需求 §3.5 的原话，= 回到"碰到就回"的行为）：生物额度用完的那一次
 *       命中、方块额度用完的那一次挖掘，都会 {@code setReturning(true)}；</li>
 *   <li><b>"飞完一瓣"优先</b>（需求 §3.2 + 批 3 裁定）：花瓣段只吃额度、<b>绝不</b>提前掉头
 *       ——两个 {@code isPetalFlight()} 分支就是这条优先级的落点，关卡 §29n-2 钉着它；</li>
 *   <li><b>穿透破坏照样扣耐久</b>（需求 §六 推断值 #4）：命中生物 / 挖掉方块各
 *       −{@link BoomerangTier#WEAR_PER_HIT}，走的仍是批 2 的"只累计、回程一次结算"。</li>
 * </ol>
 * <p>⚠ <b>撞上挖不动的方块不掉额度但会掉头</b>（点按段）：{@link BoomerangMining#mineBlock(AbstractBoomerangEntity, BlockPos)}
 * 的三关（硬度 / 挖掘等级 / 原版进度）没过 ⇒ 不消耗额度、也不穿墙（批 1/2 的"撞墙即回"照旧）。
 * 花瓣段则穿过（曲线是固定路径，与批 2 的花瓣段行为一致）。</p>
 */
final class BoomerangPierce {

	private BoomerangPierce() {
	}

	/**
	 * <b>初始化本次投掷的穿透额度</b>（只在第一次需要时算一次）。
	 *
	 * <p>等级走 {@link BoomerangItem#effectiveSkillLevel(ItemStack, int)}（与物品侧<b>同一个</b>
	 * 读取点）——基准等级取本档 {@link BoomerangTier#baseSkillLevel()}，叠技艺提升 / 技艺回溯后
	 * 钳到 5。额度本身来自 {@link BoomerangSkillConfigs}（{@code 3L} 与 {@code 5L}）。</p>
	 *
	 * <p>为什么<b>惰性</b>初始化而不是在投掷时写：实体在被投掷的那一刻已经把镖的栈同步进来了
	 * （{@code setItemStack} 在 {@code addFreshEntity} 之前），但"这一趟到底有没有用到穿透"
	 * 只有第一次命中才知道；惰性初始化让"没打过任何东西的一趟"完全不碰这套账，
	 * 也让跨区块重载（{@code -1} 与已用的剩余额度都进 NBT）与之一致。</p>
	 */
	static void ensurePierceQuota(AbstractBoomerangEntity host) {
		if (host.pierceMobsLeft() >= 0) {
			return;
		}
		// 额度只有一个来源：**穿刺技能**（投掷那一刻按住键一，作者 2026-10-02 第二次裁定）。
		// 「长按自带 1 / 1」那个暂定值已被第三次裁定废除（花瓣段不吃任何额度，见
		// BoomerangSkillConfigs 里那一段留档）⇒ 没携带技能时额度恒 0，判定自然退化成
		// "碰到即回"（点按段）。
		if (host.isPierceSkillCarried()) {
			int level = BoomerangItem.effectiveSkillLevel(host.getItemStack(), host.tier().baseSkillLevel());
			host.setPierceMobsLeft(BoomerangSkillConfigs.pierceMobQuota(level));
			host.setPierceBlocksLeft(BoomerangSkillConfigs.pierceBlockQuota(level));
		} else {
			host.setPierceMobsLeft(0);
			host.setPierceBlocksLeft(0);
		}
	}

	/**
	 * <b>命中之后"要不要掉头"的唯一判定</b>（生物与方块两条命中路径共用这一处）。
	 *
	 * <p>三条判据，顺序固定 —— <b>"飞完一瓣"永远排第一</b>（需求 §3.2 + 批 3 裁定：
	 * 花瓣段不许因为命中而提前返回，额度用完也不行）：</p>
	 * <ol>
	 *   <li>{@code isPetalFlight()} ⇒ 恒 <b>不掉头</b>（花瓣段唯一允许的掉头是"走完 s=1"）；</li>
	 *   <li>这次命中<b>本来就不允许穿过</b>（{@code mayPierceThrough = false}：撞上挖不动的方块）
	 *       ⇒ 照旧掉头（批 1/2 的"撞墙即回"）；</li>
	 *   <li>额度还没用完（{@code quotaLeft > 0}）⇒ 穿过去继续飞；<b>用完 ⇒ 掉头</b>
	 *       （需求 §3.5："额度用完即掉头（= 回到点按那种碰到就回的行为）"）。</li>
	 * </ol>
	 *
	 * @param quotaLeft        该类额度在本次命中<b>扣减之后</b>的余量
	 * @param mayPierceThrough {@code false} = 这次命中不允许穿过（挖不动的方块）
	 * @return {@code true} = 已转入回程（调用方不得再前进）
	 */
	static boolean turnAroundIfNotPiercing(AbstractBoomerangEntity host, int quotaLeft, boolean mayPierceThrough) {
		if (host.isPetalFlight()) {
			// ★ 花瓣段（作者 2026-10-02 第三次裁定）：**近程无限制** —— 沿花瓣轨迹上所有方块都破坏、
			// 所有生物都伤害，**不吃任何额度**（"长按自带 1/1"那个暂定值当场作废）。
			// 只有**飞远了**（超出本档"能力范围"）才回到上限规则：那时遇到**超出本档能力**的方块
			// （挖不动 ⇒ mayPierceThrough == false）就「既不破坏也不伤害，直接返回」。
			// 近程遇到挖不动的方块 ⇒ **穿过去继续飞完这一瓣**（"无限制"那一句的最小读法；
			// 需求没写死这一种情形，见报告 §⑥）。
			if (mayPierceThrough || BoomerangFlight.withinCapabilityRange(host)) {
				return false;
			}
		} else {
			// 点按段（逐字沿用批 3 口径）：撞不动（墙）或额度用完 ⇒ 掉头；否则穿过去继续飞。
			boolean quotaSpent = quotaLeft <= 0;
			if (mayPierceThrough && !quotaSpent) {
				return false;
			}
		}
		// ⚠ 全实体只剩这一个 setReturning(true) 在这条规则里（另外三处分别在距离判据、
		// 去程时间上限、飞完一瓣）——关卡 §29n-4 把"恰好 4 处"钉死，别再复制一份。
		host.setReturning(true);
		return true;
	}
}
