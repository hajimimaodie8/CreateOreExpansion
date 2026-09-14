package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftResults.DebugLog;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 变体波的<b>加工编排</b>：把"命中 → 找可加工项 → 逐条试执行 → 消耗 → 产物落点"这条链跑完。
 *
 * <p>分工上它是"指挥"，不自己干具体活：候选检索交 {@link WaveCandidateEvaluator}、产物推导交
 * {@link WaveCraftResults}、资源扣减交 {@link WaveCraftConsumption}、产物落点交
 * {@link WaveOutputPlacer}、环境判定/读数交 {@link WaveEnvironmentChecks}。本类只负责
 * <b>顺序、循环、批锁与链数</b>这三件事。</p>
 *
 * <p><b>两条入口</b>：</p>
 * <ul>
 *   <li>{@link #handleInventoryBlock}：命中带物品槽的方块（置物台/工作盆…）——一次碰撞处理<b>一整批</b>
 *       （链允许时循环清空可加工项），批内用批锁固定配方，链尽才释放载荷并消散。普通波退化路径
 *       （没有携带任何加工能力）由 {@link Host#handleInventoryBlockAsPlainWave} 交回基类实现；</li>
 *   <li>{@link #tryCraft}：命中掉落物——逐 tick 一件（掉落物会动，不像方块槽可以一次扫完）。</li>
 * </ul>
 *
 * <p><b>状态全部走 {@link Host}</b>：链数、批锁 LRU、引雷额度、补料、释放/消散、载荷三件套都在实体里，
 * 本类不持任何字段（只有 Host 引用），因此"先结算后消耗"的既有顺序不会被搬动打乱。</p>
 */
public final class WaveCraftExecutor {

	/** 编排所需的实体侧状态与动作（实现方即波实体；不含任何"可写公共字段"）。 */
	public interface Host {

		/** 世界（能力查询 / 掉落物生成用）。 */
		Level level();

		/** 波自身所在方块位置（产出/缓存中心用）。 */
		BlockPos wavePos();

		/** 波自身的精确位置（掉落物生成用；与 {@link #wavePos()} 不同，是实体坐标而非方块中心）。 */
		Vec3 waveVec();

		/** 候选检索上下文（见 {@link WaveCandidateEvaluator.Context}）。 */
		WaveCandidateEvaluator.Context candidateContext();

		/** 产物推导上下文（见 {@link WaveCraftResults.Context}）。 */
		WaveCraftResults.Context craftResultsContext();

		/** 资源扣减器（见 {@link WaveCraftConsumption}）。 */
		WaveCraftConsumption consumption();

		/** 剩余链数（每成功加工一次 −1，0 即释放载荷并消散）。 */
		int chainLeft();

		/** 覆写剩余链数。 */
		void setChainLeft(int chainLeft);

		/** 从候选集里挑一条（批锁 → 输入种类 LRU → 转速档 → 专用度）。 */
		Candidate pickCandidate(List<Candidate> candidates, ItemStack input, Candidate preferred);

		/** 候选排序（批锁优先，其余按策略类排序）。 */
		List<Candidate> orderCandidates(List<Candidate> candidates, ItemStack input, Candidate preferred);

		/** 记录"该输入种类最近成功的配方"（批锁 LRU）。 */
		void rememberCraftLock(ItemStack input, Candidate chosen);

		/** 引雷（携带避雷针释放机会时，在命中点落雷）；返回是否真引了。 */
		boolean summonLightningAt(BlockPos pos);

		/** 本次命中是否已引雷（引雷那道理负责自己的加工类型，本波不再重复执行）。 */
		boolean strikeOwnsTypes();

		/** 覆写"本次命中已引雷"。 */
		void setStrikeOwnsTypes(boolean owns);

		/** 链用尽：释放剩余载荷并消散。 */
		void finishAndDiscard();

		/** 命中后就地补料（范围 = 变器读取半径；默认配置关闭）。 */
		void refillPayloadAround(BlockPos center);

		/** 记录"最近加工过的方块"（余料处置以它为圆心找最近可存容器）。 */
		void setLastProcessedBlock(BlockPos pos);

		/** 是否携带了任何加工能力（属性集与配方类型集都空 = 退化波，按普通波行为处理）。 */
		boolean hasCraftingAbility();

		/** 携带的加工机属性数量（轨迹读数用）。 */
		int carriedMachineCount();

		/** 携带的应执行配方类型数量（轨迹读数用）。 */
		int carriedRecipeTypeCount();

		/** 普通波的"命中物品槽方块"行为（基类实现；变体波无加工能力时退化到它）。 */
		boolean handleInventoryBlockAsPlainWave(IItemHandler handler, BlockPos pos);

		/** 波源位置（环境判定第二中心；轨迹读数用；可为 null）。 */
		BlockPos waveOrigin();

		/** 变器携带的热档（轨迹读数用）。 */
		BlazeBurnerBlock.HeatLevel getCarriedHeat();

		/** 加工诊断日志出口。 */
		DebugLog debug();

		/** 轨迹日志出口。 */
		DebugLog trace();
	}

	private final Host host;

	public WaveCraftExecutor(Host host) {
		this.host = host;
	}

	/**
	 * 命中带物品槽方块（置物台/工作台/工作盆…）时的加工编排。
	 *
	 * <p>变体波：用自己携带的加工机配方类型逐槽链式远程加工，成功则本 tick 结束不消散（链用尽才绽放）；
	 * 无可加工项则返回 {@code false}，由调用方按"撞墙消散"处理。</p>
	 *
	 * @return true = 已处理（波不在此消散，继续飞行/穿出）；false = 槽内无一可加工
	 */
	public boolean handleInventoryBlock(IItemHandler handler, BlockPos pos) {
		if (host.level()
			.isClientSide)
			return true;
		if (!host.hasCraftingAbility())
			return host.handleInventoryBlockAsPlainWave(handler, pos); // 无加工能力：维持普通波行为
		if (host.chainLeft() <= 0) {
			host.finishAndDiscard();
			return true;
		}
		StellarWaveMachineIntegrations.ensureRegistered();

		// 记录"最近加工过的方块"：余料处置（配置 wave.payloadRelease=NEAREST_CONTAINER）以它为圆心，
		// 在变器读取半径内找最近的可存容器（见 releasePayload）
		host.setLastProcessedBlock(pos.immutable());

		// 引雷：携带强化避雷针释放机会时，命中哪个方块就在那里落雷
		// （闪电加工由本模组"闪电落地统一加工"接管该落点 1 格内的掉落物/置物台/工作盆）
		host.setStrikeOwnsTypes(false); // 每次命中复位：本命中是否引雷，决定要不要摘掉雷击类类型
		host.summonLightningAt(pos);

		// 命中后先"就地补料"（范围 = 变器读取半径）：让波能在加工现场补到辅料/流体/电量
		host.refillPayloadAround(pos);

		// 命中点流体能力（可空：置物台/无流体能力的方块返回 null → 流体只能来自载荷）
		IFluidHandler containerFluid = host.level()
			.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);

		// 循环加工：链允许时尽量把该方块槽内可加工的物品处理完（一次碰撞处理一"批"），
		// 链尽才消散；与掉落物"逐 tick 一件"不同——方块槽静止，波一次扫过应清空可加工项。
		// batchLock：<b>单次方块命中处理生命周期内的候选锁</b>（不跨 tick）——同一批同种物品
		// 反复进循环时优先复用首次成功的配方，杜绝一锅混出两种产物（①修复）。
		boolean any = false;
		Candidate batchLock = null;
		while (host.chainLeft() > 0) {
			// 找槽顺带取回候选表：执行方直接复用，避免对同一槽再全库评估一遍（性能，见 SlotPlan）
			SlotPlan plan = findCraftableSlot(handler, pos, containerFluid);
			if (plan.slot() < 0) {
				// 没有配方候选：再试一次"把载荷流体灌进容器里的可装流体物品"（2026-09-14 新增）。
				// 这一步是**货载动作**（波自己带的水倒进空桶），不是机器加工，所以不需要携带任何配方类型，
				// 与"余料流体注进附近储罐"同一类行为；Create 真机的喷口给桶注液同理——它也不是配方，
				// 而是 GenericItemFilling 对物品自身流体能力的特判（见 WaveItemFluidFilling 的说明）。
				if (WaveItemFluidFilling.fillOne(host, handler, pos)) {
					any = true;
					continue; // 灌了一个，继续看容器里还有没有能装的物品 / 载荷里还有没有流体
				}
				break;
			}
			Candidate done = craftFromSlot(handler, plan.slot(), pos, batchLock, containerFluid, plan.candidates());
			if (done == null)
				break; // 该槽无可执行（如环境不满足/无产物/并发变化）：不再原地空转，结束本批处理
			batchLock = done; // 记住本批本次使用的配方，同批后续同种物品优先复用
			any = true;
		}
		if (any) {
			if (host.chainLeft() <= 0)
				host.finishAndDiscard(); // 链尽：释放载荷并消散
			return true; // 已处理（波不在此消散，继续飞行/穿出）
		}
		host.trace()
			.log("命中 {} 无任何可加工候选 → 按撞墙消散；容器内容 {}（属性 {} 个 / 配方类型 {} 个）", pos,
				WaveCraftResults.describeHandler(handler), host.carriedMachineCount(),
				host.carriedRecipeTypeCount());
		return false; // 槽内无一可加工 → 调用方按撞墙消散
	}

	/** 单次加工尝试（掉落物路径）：全库检索候选，选 1 条执行；返回是否成功加工。 */
	public boolean tryCraft(ItemEntity item) {
		ItemStack input = item.getItem();
		if (input.isEmpty())
			return false;
		StellarWaveMachineIntegrations.ensureRegistered();

		// 强化避雷针释放机会：本次命中最多引一道雷（2026-09 审计修复）。
		// 旧实现逐件掉落物都调一次：一次 sweep 命中多件时会把引雷额度全烧光、在多处各落一道雷，
		// 且逐件复位 strikeOwnsTypes 会让"同一件物品只被雷/波加工一遍"的护栏对后续掉落物失效。
		// 这里用 strikeOwnsTypes 兼作"本次命中已引雷"，不再逐件复位（方块路径每次命中仍会复位）。
		if (!host.strikeOwnsTypes())
			host.summonLightningAt(item.blockPosition());

		// 环境判定以命中点为中心（掉落物自身位置），供配方机器环境前置条件使用。
		// 掉落物路径没有容器上下文 → 物品容器/流体容器都传 null、主料槽 = -1
		// （辅料与流体只可能来自波载荷）。
		List<Candidate> candidates = WaveCandidateEvaluator.collect(host.candidateContext(), input,
			item.blockPosition(), null, -1, null);
		if (candidates.isEmpty())
			return false;

		Candidate chosen = host.pickCandidate(candidates, input, null);
		if (!applyCraft(item, chosen))
			return false;
		host.rememberCraftLock(input, chosen); // 同种物品批次锁定：下次同输入直接复用该配方
		return true;
	}

	/**
	 * <b>槽 + 该槽的候选表</b>：{@link #findCraftableSlot} 的返回值。
	 *
	 * <p>2026-09-14（性能，审计第 6 条第②半）：旧实现 findCraftableSlot 只回槽号、把候选表丢掉，
	 * 紧接着 craftFromSlot 对<b>同一槽</b>再全库评估一遍 → 每加工一件跑两遍评估。
	 * 现在把候选表一并带回给执行方复用。**安全性**：两次调用同属一次命中处理、同一 tick，
	 * 中间不取料也不改容器（执行路径是在评估之后才 `extractItem`），故候选表不会过期。</p>
	 */
	private record SlotPlan(int slot, java.util.List<Candidate> candidates) {

		/** 没有可加工槽（槽号 -1、候选表为空）。 */
		static final SlotPlan NONE = new SlotPlan(-1, java.util.List.of());
	}

	/**
	 * 找第一个存在可加工候选（含环境前置）的槽（不真取），并把<b>该槽的候选表</b>一并返回给调用方复用。
	 * 候选评估带上本容器（物品槽 + 流体槽）与主料槽：辅料优先取<b>本容器其它槽</b>、载荷兜底；
	 * 流体优先取<b>本容器流体槽</b>、载荷兜底。
	 */
	private SlotPlan findCraftableSlot(IItemHandler handler, BlockPos pos, IFluidHandler containerFluid) {
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemStack probe = stack.copy();
			probe.setCount(1);
			List<Candidate> candidates = WaveCandidateEvaluator.collect(host.candidateContext(), probe, pos, handler, slot,
				containerFluid);
			if (!candidates.isEmpty())
				return new SlotPlan(slot, candidates);
		}
		return SlotPlan.NONE;
	}

	/**
	 * 从槽取 1 个主料执行一次配方（产物优先放回原槽；"辅料即产物来源"类放回辅料槽）。
	 *
	 * @param preferred 本批方块处理已锁定的候选（可为 null = 尚无锁）；当前槽物品仍匹配它时直接复用，
	 *                  否则重新按输入种类锁定（参见 {@link Host#pickCandidate}）
	 * @param containerFluid 命中点流体能力（可空）
	 * @param reused 由 {@link #findCraftableSlot} 预先算好的候选表（同 tick、未取料 → 可直接复用，
	 *               省掉第二次全库评估）；传 null/空表则本方法自己再评估一次
	 * @return 成功返回所用候选（调用方用于批锁）；无可执行（含环境/产物问题）返回 null
	 */
	private Candidate craftFromSlot(IItemHandler handler, int slot, BlockPos pos, Candidate preferred,
		IFluidHandler containerFluid, List<Candidate> reused) {
		ItemStack stack = handler.getStackInSlot(slot);
		if (stack.isEmpty())
			return null;
		ItemStack probe = stack.copy();
		probe.setCount(1);
		List<Candidate> candidates = reused != null && !reused.isEmpty() ? reused
			: WaveCandidateEvaluator.collect(host.candidateContext(), probe, pos, handler, slot, containerFluid);
		if (candidates.isEmpty())
			return null;

		// 逐条候选试执行（按 批锁 → 输入种类 LRU → 专用度 排序，见 orderCandidates）：
		// 材料匹配但"推不出产物"的候选（某些 mod 的 results 为空配方、辅料在派生期间被并发取走、
		// 或升级映射不到目标物品）只跳过该条，不再像以前那样直接中断整批——
		// "命中一条空产物候选 → 整锅罢工"是"放了料却什么都没发生"的来源之一。
		List<Candidate> ordered = host.orderCandidates(candidates, probe, preferred);
		for (Candidate chosen : ordered) {
			List<ItemStack> results = WaveCraftResults.compute(host.craftResultsContext(), chosen, probe.copy(),
				handler, pos);
			if (results == null) {
				host.trace()
					.log("跳过候选 {} [{}]：材料匹配但推不出产物（输入 {}）", chosen.id,
						WaveCraftResults.typeKeyString(chosen.recipe), probe.getItem());
				continue;
			}

			// 真取 1 个主料；取空（并发变化）则跳过此槽
			ItemStack input = handler.extractItem(slot, 1, false);
			if (input.isEmpty())
				return null;

			host.consumption()
				.consumeForCraft(chosen, handler, containerFluid);
			host.rememberCraftLock(probe, chosen); // 批锁（同输入种类后续复用）
			// 产物回位（⑥）：产物优先放回<b>被消耗辅料所在的槽</b>（变形升级/锻造融合的产物在语义上
			// 就是"辅料被加工成的新物"——如下界合金工具/带纹饰盔甲），主料槽作次选，都放不下才掉落在
			// 方块上方。这样"钻石工具/盔甲那一个盆槽"里直接出现产物，而不是堆到主料槽。
			int target = WaveCraftResults.productSlotHint(chosen, slot);
			if (target < 0 || target >= handler.getSlots())
				target = slot; // 防御：槽号失效则回主料槽
			if (target != slot)
				host.debug()
					.log("产物回位：优先槽 {}（辅料槽），主料槽 {}；辅料 {}", target, slot,
						WaveAuxResolver.describeAuxes(chosen.auxes));
			for (ItemStack result : results) {
				if (result.isEmpty())
					continue;
				ItemStack leftover = WaveOutputPlacer.insertBack(handler, target, result);
				if (!leftover.isEmpty() && target != slot)
					leftover = WaveOutputPlacer.insertBack(handler, slot, leftover); // 次选：主料槽
				if (!leftover.isEmpty())
					WaveOutputPlacer.dropAtBlock(host.level(), pos, leftover);
			}
			// 流体产物：优先注入命中方块自身储罐（真空室等），放不下/无处可存再就近
			if (chosen.recipe instanceof ProcessingRecipe<?, ?> pr && !pr.getFluidResults()
				.isEmpty()) {
				for (FluidStack fr : pr.getFluidResults())
					if (!fr.isEmpty()) {
						FluidStack rest = WaveOutputPlacer.fillBlock(host.level(), pos, fr.copy());
						if (!rest.isEmpty())
							WaveOutputPlacer.fillNearby(host.level(), host.wavePos(), rest); // 尽力而为，剩余浪费
					}
			} else {
				// 非 ProcessingRecipe 族（拆解等）的流体产物：同样"命中方块储罐优先 → 就近"
				for (FluidStack fr : WaveCraftResults.familyFluidResults(host.level(), chosen, probe, pos))
					if (!fr.isEmpty()) {
						FluidStack rest = WaveOutputPlacer.fillBlock(host.level(), pos, fr.copy());
						if (!rest.isEmpty())
							WaveOutputPlacer.fillNearby(host.level(), host.wavePos(), rest);
					}
			}
			host.trace()
				.log("加工 {} → 选中 {} [{}]（候选 {} 条，辅料 {}）产出 {}{}", input.getItem(), chosen.id,
					WaveCraftResults.typeKeyString(chosen.recipe), ordered.size(),
					WaveAuxResolver.describeAuxes(chosen.auxes),
					WaveCraftResults.describeResults(results),
					WaveEnvironmentChecks.describeHeat(host.level(), pos, chosen.recipe, host.waveOrigin(),
						host.getCarriedHeat()));
			host.setChainLeft(host.chainLeft() - 1);
			return chosen;
		}
		host.trace()
			.log("槽 {} 的 {} 匹配到 {} 条候选但全部推不出产物 → 本槽放弃", slot, probe.getItem(), ordered.size());
		return null;
	}

	/**
	 * 执行一次加工（掉落物路径）：主料 −1；命中第二/第三输入时逐个消耗对应辅料（手持物类配方不消耗；
	 * 产物推导类辅料被"变形/锻入"产物）；流体输入按需消耗；产物生成在命中点（流体产物就近注罐，
	 * 注不进浪费）；链 −1。
	 */
	private boolean applyCraft(ItemEntity item, Candidate candidate) {
		ItemStack input = item.getItem();
		ItemStack single = input.copy();
		single.setCount(1);

		// 产物计算：产物推导类（③b auto_upgrade 变形升级 / ③c auto_smithing 锻造融合：results 为空）
		// 产物需从当前候选主料+辅料推导；普通类走 RecipeApplier 滚结果。
		// 掉落物路径没有容器上下文，故 handler 传 null（辅料只可能来自波载荷）。
		List<ItemStack> results = WaveCraftResults.compute(host.craftResultsContext(), candidate, single, null,
			item.blockPosition());
		if (results == null)
			return false; // 无可执行产物（不消耗任何东西）

		input.shrink(1);
		if (input.isEmpty())
			item.discard();
		// 消耗资源（辅料逐个扣 / 流体 / 电量）——与方块槽路径共用同一实现
		// （此路径无容器上下文：辅料与流体都只可能来自载荷）
		host.consumption()
			.consumeForCraft(candidate, null, null);

		Vec3 pos = item.isRemoved() ? host.waveVec() : item.position();
		for (ItemStack result : results) {
			if (result.isEmpty())
				continue;
			ItemEntity out = new ItemEntity(host.level(), pos.x, pos.y, pos.z, result);
			out.setDeltaMovement(Vec3.ZERO);
			host.level()
				.addFreshEntity(out);
		}
		// 流体产物就近注入储罐（无处可存则浪费，不回波）
		if (candidate.recipe instanceof ProcessingRecipe<?, ?> pr && !pr.getFluidResults()
			.isEmpty()) {
			for (FluidStack fr : pr.getFluidResults())
				if (!fr.isEmpty())
					WaveOutputPlacer.fillNearestTank(host.level(), host.wavePos(), fr.copy());
		} else {
			// 非 ProcessingRecipe 族（拆解等）的流体产物：掉落物路径无容器上下文，只能就近注入
			for (FluidStack fr : WaveCraftResults.familyFluidResults(host.level(), candidate, single,
				item.blockPosition()))
				if (!fr.isEmpty())
					WaveOutputPlacer.fillNearestTank(host.level(), host.wavePos(), fr.copy());
		}
		host.setChainLeft(host.chainLeft() - 1);
		return true;
	}
}
