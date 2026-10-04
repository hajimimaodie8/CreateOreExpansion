package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <b>回旋镖破坏方块</b>（2026-10-03 行为零变化拆分，从 {@code AbstractBoomerangEntity} 逐字搬出）。
 *
 * <p>本类是方块破坏的<b>唯一入口</b>：单格的 {@link #mineBlock(AbstractBoomerangEntity, BlockPos)}
 * （临时换主手 → {@code player.gameMode.destroyBlock} → {@code finally} 还原），
 * 以及两种固有十字挖掘的 {@link #mineCross(AbstractBoomerangEntity, BlockPos)}
 * （垂面 4 邻格，每格仍走同一个单格入口）。</p>
 *
 * <p>命中判定在 {@link BoomerangImpact}。全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖。</p>
 *
 * <h2>十一、批 4（2026-10-03 第二轮需求 §3.3）：十字挖掘 —— 星界 / 雷鸣的固有特性</h2>
 * <p>作者裁定：十字挖掘是<b>星界镖 / 雷鸣镖的固有特性</b>（<b>不占技能槽</b> ⇒
 * "回旋镖只有两个技能"仍然成立），翠玉 / 宝石没有；平面 = <b>垂直于飞行方向</b>的平面，
 * 形状 = 中心 1 格 + 该平面内 4 个正交方向各 1 格 = <b>5 格</b>（臂长 1、不随等级变）。</p>
 * <p>落地只有两处：判据 {@link BoomerangTier#crossMine()}（穷尽 {@code switch (this)}、
 * <b>无 {@code default}</b> ⇒ 枚举改名或加档<b>编译就不过</b>；也不是第 12 个构造参数 ——
 * 那 11 项被关卡逐位钉住）+ helper {@link #mineCross(BlockPos)}（垂面法向 = {@code |d|}
 * 最大的世界轴、并列固定序 x→y→z；5 格<b>每一格</b>都走既有唯一破坏入口
 * {@link #mineBlock(BlockPos)} ⇒ 每格各扣 1 耐久（共 −5）、挖不动由它自己跳过且不记账；
 * <b>不吃能量</b>；方块穿透额度仍只扣 1 份；邻格是容器就跳过 —— <b>不挖也不开箱</b>；
 * 零向量退化为单格，<b>不引"上一次有效方向"字段</b>）。
 * 调用点只有 {@link BoomerangImpact#onHitBlock(BlockPos)} 的普通支一处 —— <b>容器支一字未动</b>。</p>
 */
final class BoomerangMining {

	private BoomerangMining() {
	}

	/**
	 * 尝试挖掉 {@code pos} 上的方块。
	 *
	 * <p>流程（顺序不能改）：</p>
	 * <ol>
	 *   <li>门槛：硬度 ≥ 0（基岩之类 -1 直接不动）、硬度 ≤ 该档 maxHardness、不在该档的
	 *       {@code INCORRECT_FOR_*_TOOL} 标签里（= 挖掘等级，与 {@code AllTiers} 同一判据）；</li>
	 *   <li>复刻原版挖掘进度 {@code digSpeed / (hardness * i)}，{@code i = 30/100}
	 *       ——{@code i} 由 {@code player.hasCorrectToolForDrops(state)} 决定，而它读的是
	 *       <b>主手</b>那一格，所以下面必须先把镖塞进手里；</li>
	 *   <li>临时把镖塞进 {@code inventory.selected} + {@code setItemInHand(MAIN_HAND)}，
	 *       {@code player.gameMode.destroyBlock(pos)}；</li>
	 *   <li>{@code finally} 还原那一格（<b>无论如何</b>都要还原，异常也不能把玩家的物品换掉）。</li>
	 * </ol>
	 *
	 * <p><b>代价（2026-10-02 批 2 改口径）</b>：挖掉一个方块<b>不再扣能量</b>，改记一笔
	 * <b>−1 耐久</b>（{@link BoomerangTier#WEAR_PER_HIT}）。
	 * ⚠ 旧的 {@code BoomerangTier#mineCost()}（5/10/15/20 点）已被作者推翻并删除（需求 §3.8 + §3.9），
	 * 本方法里那一行 {@code ToolEnergy.canAfford/consume} 也随之删掉——能量只花在投掷那一处。
	 * 与命中生物一样，这里<b>只记账</b>（{@link #flightWear}），不写回物品。</p>
	 *
	 * @return true = 真的挖掉了（{@code destroyBlock} 答应）；false = 任一门槛没过或没挖动。
	 *         <b>批 3 起有返回值</b>：{@link BoomerangImpact#onHitBlock} 靠它决定"吃不吃穿刺额度、
	 *         穿过去还是掉头"——挖不动的方块不消耗额度，也仍然把镖拦下来（点按段）。
	 */
	static boolean mineBlock(AbstractBoomerangEntity host, BlockPos pos) {
		if (!(host.getOwner() instanceof ServerPlayer player)) {
			return false;
		}
		BlockState state = host.level().getBlockState(pos);
		if (state.isAir()) {
			return false;
		}
		float hardness = state.getDestroySpeed(host.level(), pos);
		if (hardness < 0.0F) {
			return false;
		}
		BoomerangTier tier = host.tier();
		if (hardness > tier.maxHardness()) {
			return false;
		}
		if (state.is(tier.incorrectBlocks())) {
			return false;
		}
		// 原版挖掘进度：一 tick 内进度 ≥ 1 才算挖开（i=30 正确工具 / i=100 用错工具）。
		// 这里用**位置敏感**的 hasCorrectToolForDrops —— 它就是 NeoForge 的 doPlayerHarvestCheck
		// （EventHooks.doPlayerHarvestCheck：先取原版单参判定的值，再过 PlayerEvent.HarvestCheck，
		// 让别的模组有机会否决）。单参那个重载在 NeoForge 里是 @Deprecated。
		int i = player.hasCorrectToolForDrops(state, host.level(), pos) ? 30 : 100;
		if (tier.digSpeed() / (hardness * i) < 1.0F) {
			return false;
		}
		ItemStack stack = host.getItemStack();
		if (stack.isEmpty()) {
			return false; // 没有镖就没有"临时塞进手里"这一步（能量已不再参与挖掘判定）
		}

		Inventory inventory = player.getInventory();
		int hotbar = inventory.selected;
		ItemStack saved = inventory.getItem(hotbar);
		inventory.setItem(hotbar, stack);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		boolean destroyed;
		try {
			// 唯一破坏入口：权限 / 时运 / 掉落归属 / 统计全交给原版（我们只负责"能不能挖"这一关）
			destroyed = player.gameMode.destroyBlock(pos);
		} finally {
			inventory.setItem(hotbar, saved);
			player.setItemInHand(InteractionHand.MAIN_HAND, saved);
		}
		if (destroyed) {
			// 需求 §3.8：挖掉一个方块 ⇒ 额外 −1 耐久。**只记账、不写回**（见 flightWear 的注释）：
			// 这里绝不能调 BoomerangItem#addWear —— 那正是"耐久被提前打到 0"的那条错路。
			host.addFlightWear(BoomerangTier.WEAR_PER_HIT);
		}
		return destroyed;
	}

	/**
	 * ★ <b>十字挖掘</b>（需求 coe-boom2 §3.3；作者裁定 = 星界 / 雷鸣的<b>固有特性、不占技能槽</b>）：
	 * 除命中的那一格之外，再在<b>垂直于飞行方向</b>的平面上多挖 <b>4 个正交邻格</b>
	 * （臂长 1、不随等级变）⇒ 一共 5 格。
	 *
	 * <p><b>平面怎么定（唯一判据处）</b>：垂面的法向取飞行方向 {@code d = getDeltaMovement()}
	 * （与 {@link BoomerangImpact#checkImpact()} 用的是同一个向量、与 {@link AbstractBoomerangEntity#orbitDirection()} 同源）
	 * 里 <b>{@code |d|} 最大的那个世界轴</b>，<b>并列时固定序 x → y → z</b>：</p>
	 * <ul>
	 *   <li>轴 <b>X</b>（沿 X 飞）⇒ 平面 YZ ⇒ 中心 + {@code (0,±1,0)} + {@code (0,0,±1)}
	 *       （水平飞 = 上下 + 前后）；</li>
	 *   <li>轴 <b>Y</b> ⇒ 平面 XZ ⇒ 中心 + {@code (±1,0,0)} + {@code (0,0,±1)}；</li>
	 *   <li>轴 <b>Z</b> ⇒ 平面 XY ⇒ 中心 + {@code (±1,0,0)} + {@code (0,±1,0)}。</li>
	 * </ul>
	 * <p>⚠ <b>"不是永远水平"</b>：这一支跟着 {@code |d|} 走 —— 45° 斜向下飞时 X 与 Y 并列，
	 * 固定序把平面定成 <b>YZ（竖直的）</b>；只有 {@code |d_y|} 真正最大（近乎垂直俯冲）时平面才接近
	 * 水平，而那时它本来就是最接近真垂面的那个离散平面。把平面写死成"水平面"正是这一条的反面。</p>
	 *
	 * <p><b>每一格都走既有唯一破坏入口</b> {@link #mineBlock(BlockPos)}（临时换主手 +
	 * {@code gameMode.destroyBlock} + {@code finally} 还原）：于是"硬度 / 挖掘等级 / 原版进度 /
	 * 权限 / 时运 / 掉落归属"整条判定一个字不改；<b>挖不动它自己返回 false 且不记账</b>
	 * （跳过的那格不扣耐久 —— §3.3 第 3 条），<b>挖掉了它自己记一笔 −1</b> ⇒ 5 格各扣 1、共 −5，
	 * 本方法<b>一笔都不补记</b>（不碰 {@code addFlightWear}）。</p>
	 *
	 * <p><b>不消耗能量</b>（§3.3 第 4 条：挖方块早就改扣耐久了）；
	 * <b>方块穿透额度仍只扣 1 份</b>（额度在 {@code onHitBlock} 那一处按"命中这一格挖没挖掉"记，
	 * 本方法挖了几格都不参与）。</p>
	 *
	 * <p><b>邻格是容器 ⇒ 跳过</b>（不挖、<b>也不开箱</b>）：直接 {@code mineBlock} 会把箱子挖掉，
	 * 而 {@code ChestBlock#onRemove} → {@code Containers.dropContentsOnDestroy} 会把里面
	 * <b>还没取走</b>的东西撒一地（这正是批 7/8 花一整节避开的事）。这里问的是最窄的一句
	 * "这一格装着东西吗"（方块实体 {@code instanceof Container}），<b>不是</b>既有那个
	 * "能不能开箱取物"的判据 —— 十字不做取物，所以不该去问它（问了就等于在邻格也开箱）。</p>
	 *
	 * <p>⚠ <b>零向量兜底</b>（§3.3 的 ⚠）：方向退化时<b>不 normalize、也不引"上一次有效方向"字段</b>
	 * —— 直接只挖命中的这一格（见 {@link AbstractBoomerangEntity#DEGENERATE_DIRECTION_SQR}）。</p>
	 *
	 * <p>⚠ <b>本方法不许自己掉头</b>（{@code setReturning} 全实体恰好 4 处，关卡钉着）；
	 * 返回值只报<b>中心那一格</b>挖没挖掉，供 {@code onHitBlock} 决定"吃不吃额度、穿过去还是掉头"，
	 * 与单格时代逐字同形。</p>
	 *
	 * @return {@code true} = <b>中心那一格</b>真的挖掉了（邻格挖了几格都不改这个答案）
	 */
	static boolean mineCross(AbstractBoomerangEntity host, BlockPos pos) {
		Vec3 motion = host.getDeltaMovement();
		if (motion.lengthSqr() < AbstractBoomerangEntity.DEGENERATE_DIRECTION_SQR) {
			// 零向量兜底：方向退化成一个点 ⇒ 退化为单格挖掘（不引入"上一次有效方向"字段）。
			return mineBlock(host, pos);
		}
		double ax = Math.abs(motion.x);
		double ay = Math.abs(motion.y);
		double az = Math.abs(motion.z);
		// 被垂直的轴 = |d| 最大的那个世界轴；并列时固定序 x -> y -> z（先 x、再 y、最后 z）。
		int axis;
		if (ax >= ay && ax >= az) {
			axis = 0;
		} else if (ay >= az) {
			axis = 1;
		} else {
			axis = 2;
		}
		// 平面内 4 个正交邻格（臂长 1，与等级无关）：三条分支各一张表，别处不许再写第二份。
		BlockPos[] ring = switch (axis) {
			case 0 -> new BlockPos[] { pos.offset(0, 1, 0), pos.offset(0, -1, 0), pos.offset(0, 0, 1), pos.offset(0, 0, -1) };
			case 1 -> new BlockPos[] { pos.offset(1, 0, 0), pos.offset(-1, 0, 0), pos.offset(0, 0, 1), pos.offset(0, 0, -1) };
			default -> new BlockPos[] { pos.offset(1, 0, 0), pos.offset(-1, 0, 0), pos.offset(0, 1, 0), pos.offset(0, -1, 0) };
		};
		// 中心格：命中的就是它。走到这里说明它**不是容器**（容器在 onHitBlock 的前置支里就被截走了，
		// 十字根本不介入）⇒ 直接交给唯一破坏入口。
		boolean destroyed = mineBlock(host, pos);
		for (BlockPos neighbour : ring) {
			// 邻格是容器 ⇒ 跳过：不挖（内容会撒一地）、也不开箱（十字没有取物这一步）。
			if (host.level().getBlockEntity(neighbour) instanceof Container) {
				continue;
			}
			// 挖不动由 mineBlock 自己返回 false 且不记账；挖掉了它自己记一笔 WEAR_PER_HIT。
			mineBlock(host, neighbour);
		}
		return destroyed;
	}
}
