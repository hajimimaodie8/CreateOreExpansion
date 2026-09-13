package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxSource;
import com.hjmmd_8.createoreexpansion.content.charger.craft.family.WaveRecipeFamilies;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * <b>变体波"一次加工的产物怎么算"</b>（2026-09 从 {@code StellarWaveEntity} 结构性抽出）。
 *
 * <p>本类按<b>策略</b>组织四条互斥的产物推导路径，{@link #compute} 依
 * {@link #STRATEGIES} 的列表顺序<b>分派并短路</b>：</p>
 * <ol>
 *   <li><b>族</b>（{@link FamilyStrategy}，{@link WaveRecipeFamilies} 登记的非
 *       {@code ProcessingRecipe} 族：拆解 / 序列装配）——产物推导交给族自己；</li>
 *   <li><b>变形升级</b>（{@link UpgradeStrategy}，③b {@code vintageimprovements:auto_upgrade}）
 *       —— 辅料钻石工具原位升级为下界合金版（{@link #upgradeResultOf}）；</li>
 *   <li><b>锻造融合</b>（{@link SmithingStrategy}，③c {@code vintageimprovements:auto_smithing}）
 *       —— 借原版锻造配方产出纹饰盔甲（{@link #smithingBridgeResult}）；</li>
 *   <li><b>默认</b>（{@link RecipeApplierStrategy}）—— RecipeApplier 滚结果
 *       （杵锤 hammering 等 results 非空者即走此路），恒兜底。</li>
 * </ol>
 *
 * <p><b>调用方契约不变</b>：{@link #compute} 返回 {@code null} = 无可执行产物
 * （调用方须放弃且不消耗任何东西）；返回<b>空表</b>在族路径里表示"只产流体也算成功"。</p>
 *
 * <p><b>状态经 {@link Context} 注入</b>：实体侧把 {@code level()} / {@code auxResolver()} /
 * {@code this::craftDebug} 打包成一个 {@link Context} 传入——本类自身不持世界引用、无静态状态。
 * 所有调试日志的文本与参数顺序与原实现一字不差（仅出口由实体方法改为
 * {@link DebugLog#log}）。</p>
 *
 * <p><b>配方类型判定</b>：{@link #isUpgradeTransformationRecipe}／{@link #isAutoSmithingRecipe}
 * 两条静态判定（以及它们依赖的 {@link #typeKeyOf}／{@link #typeKeyString}）随产物推导一并落在这里，
 * 实体侧改为委托调用——这样 craft 包不反向依赖实体类。判定口径原样照搬：<b>配方类型 id 字符串</b>
 * （Vintage 是可选 mod，content 包不得 import 其类），不 import 任何可选模组。</p>
 */
public final class WaveCraftResults {

	private WaveCraftResults() {
	}

	/** 诊断日志出口（实体把 {@code craftDebug(String, Object...)} 用方法引用传进来）。 */
	@FunctionalInterface
	public interface DebugLog {
		void log(String msg, Object... args);
	}

	/**
	 * 产物推导所需的<b>实体侧状态</b>（每次推导现场构造一个；不跨载荷变更缓存复用）。
	 *
	 * @param level 世界（族产物推导 / RecipeApplier / 原版锻造配方检索用，即实体的 {@code level()}）
	 * @param aux   辅料解析器（即实体每次现场构造的 {@code auxResolver()}）
	 * @param debug 调试日志出口（实体的 {@code this::craftDebug}）
	 */
	public static final class Context {
		public final Level level;
		public final WaveAuxResolver aux;
		public final DebugLog debug;

		public Context(Level level, WaveAuxResolver aux, DebugLog debug) {
			this.level = level;
			this.aux = aux;
			this.debug = debug;
		}
	}

	/**
	 * 一条产物推导路径。<b>认领语义</b>：{@link #claims} 为 true 时该候选归本路径，
	 * {@link #compute} 的返回值即终局（{@code null} = 放弃，<b>不再</b>交给后续路径）。
	 */
	public interface Strategy {

		/** 本路径是否认领该候选。 */
		boolean claims(Candidate candidate);

		/** 推导产物；{@code null} = 无可执行产物（调用方须放弃且不消耗任何东西）。 */
		List<ItemStack> compute(Context ctx, Candidate candidate, ItemStack single, IItemHandler handler,
			BlockPos around);
	}

	/** 产物推导路径表：<b>顺序即优先级</b>（族 → 变形升级 → 锻造融合 → 默认 RecipeApplier）。 */
	private static final List<Strategy> STRATEGIES = List.of(new FamilyStrategy(), new UpgradeStrategy(),
		new SmithingStrategy(), new RecipeApplierStrategy());

	// ================= 分派入口 =================

	/**
	 * 计算一次加工的产物列表；返回 {@code null} = 无可执行产物（调用方须放弃且不消耗任何东西）。
	 * <ul>
	 *   <li><b>族</b>（{@link WaveRecipeFamilies} 登记的非 ProcessingRecipe 族）→ 由族自己推导；</li>
	 *   <li><b>变形升级</b>（③b，{@code vintageimprovements:auto_upgrade}）→ {@link #upgradeResultOf}
	 *       （辅料钻石工具原位升级为下界合金版）；</li>
	 *   <li><b>锻造融合</b>（③c，{@code vintageimprovements:auto_smithing}）→ {@link #smithingBridgeResult}
	 *       （借原版锻造配方产出纹饰盔甲）；</li>
	 *   <li>其余普通类 → RecipeApplier 滚结果（杵锤 hammering 等 results 非空者即走此路）。</li>
	 * </ul>
	 * @param handler 命中容器（解析 CONTAINER 来源辅料的真实物品用；掉落物路径传 null）
	 * @param around  命中点（透传给非 ProcessingRecipe 族，见 {@link WaveRecipeFamilies}）
	 */
	public static List<ItemStack> compute(Context ctx, Candidate candidate, ItemStack single, IItemHandler handler,
		BlockPos around) {
		for (Strategy strategy : STRATEGIES)
			if (strategy.claims(candidate))
				return strategy.compute(ctx, candidate, single, handler, around);
		// 当前四条路径里"默认 RecipeApplier"恒认领，故实际不会走到这里
		return null;
	}

	// ================= 路径 1｜族（非 ProcessingRecipe 族：拆解 / 序列装配） =================

	/**
	 * <b>非 ProcessingRecipe 族</b>（{@link WaveRecipeFamilies} 登记：拆解 / 序列装配）：
	 * 产物推导交给族自己（拆解 = floor(材料数 × 剩余耐久比)；序列装配 = 推进进度或收尾抽产物）；
	 * 空表/null = 推不出产物 → 跳过该候选。若族只产流体（无物品产物）但声明了流体产物，仍算成功。
	 */
	private static final class FamilyStrategy implements Strategy {

		@Override
		public boolean claims(Candidate candidate) {
			return WaveRecipeFamilies.familyOf(candidate.familyTarget()) != null;
		}

		@Override
		public List<ItemStack> compute(Context ctx, Candidate candidate, ItemStack single, IItemHandler handler,
			BlockPos around) {
			Recipe<?> familyRecipe = candidate.familyTarget();
			WaveRecipeFamilies.Family family = WaveRecipeFamilies.familyOf(familyRecipe);
			List<ItemStack> familyResults;
			try {
				familyResults = family.results(ctx.level, around, candidate.id, familyRecipe, single);
			} catch (Throwable ignored) {
				ctx.debug.log("无产物：族配方 {} 推导异常", candidate.id);
				return null;
			}
			if (familyResults != null && !familyResults.isEmpty())
				return familyResults;
			if (!familyFluidResults(ctx.level, candidate, single, around).isEmpty())
				return new ArrayList<>(); // 只产流体也算成功（物品产物为空表）
			ctx.debug.log("无产物：族配方 {} [{}] 推不出产物（输入 {}）", candidate.id, typeKeyString(familyRecipe),
				single.getItem());
			return null;
		}
	}

	/**
	 * 非 ProcessingRecipe 族的流体产物（无族认领 / 族实现异常时返回空表）。
	 *
	 * @param level  世界（族需要世界上下文时传入；即实体的 {@code level()}）
	 * @param around 命中点（透传给族，供需要世界上下文的族使用）
	 */
	public static List<FluidStack> familyFluidResults(Level level, Candidate candidate, ItemStack input,
		BlockPos around) {
		Recipe<?> owner = candidate.familyTarget();
		WaveRecipeFamilies.Family family = WaveRecipeFamilies.familyOf(owner);
		if (family == null)
			return List.of();
		try {
			List<FluidStack> fluids = family.fluidResults(level, around, candidate.id, owner, input);
			return fluids == null ? List.of() : fluids;
		} catch (Throwable ignored) {
			return List.of();
		}
	}

	// ================= 路径 2｜变形升级（③b：Vintage auto_upgrade） =================

	/** 认领"变形升级"配方并按辅料推导产物（详见 {@link #upgradeResultOf}）。 */
	private static final class UpgradeStrategy implements Strategy {

		@Override
		public boolean claims(Candidate candidate) {
			return isUpgradeTransformationRecipe(candidate.recipe);
		}

		@Override
		public List<ItemStack> compute(Context ctx, Candidate candidate, ItemStack single, IItemHandler handler,
			BlockPos around) {
			AuxRef ref = candidate.firstAux();
			ItemStack auxStack = ctx.aux.resolveAux(ref, handler);
			if (auxStack.isEmpty())
				return null;
			ItemStack upgraded = upgradeResultOf(auxStack);
			if (upgraded.isEmpty()) {
				ctx.debug.log("无产物：变形升级辅料 {} 无法映射为下界合金版", auxStack.getItem());
				return null;
			}
			List<ItemStack> one = new ArrayList<>(1);
			one.add(upgraded);
			return one;
		}
	}

	/**
	 * 是否"变形升级"配方：results 为空、产物 = 辅料原位升级（钻石工具 → 下界合金版）。
	 * Vintage 杵锤的 {@code auto_upgrade} 配方即此形态（RecipeApplier 拿不到物品结果）。
	 * 用配方类型 id 判定（Vintage 可选 mod，content 不得 import 其类）。
	 */
	public static boolean isUpgradeTransformationRecipe(Recipe<?> recipe) {
		if (!(recipe instanceof ProcessingRecipe<?, ?>))
			return false;
		ResourceLocation typeKey = typeKeyOf(recipe);
		return typeKey != null && "vintageimprovements".equals(typeKey.getNamespace())
			&& "auto_upgrade".equals(typeKey.getPath());
	}

	/**
	 * 由辅料（钻石工具）推导升级产物：把物品注册 id 路径中的 {@code diamond_} 替换为
	 * {@code netherite_}（helmet/chestplate/leggings/boots/sword/pickaxe/axe/shovel/hoe 全覆盖），
	 * 保留附魔等数据组件；映射不到有效物品返回 EMPTY（该候选不可执行）。
	 */
	@SuppressWarnings("deprecation") // BuiltInRegistries.ITEM.get(ResourceLocation)：NeoForge 旧注册表 API（仅有取用方式）
	public static ItemStack upgradeResultOf(ItemStack auxTool) {
		if (auxTool == null || auxTool.isEmpty())
			return ItemStack.EMPTY;
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(auxTool.getItem());
		if (id == null || !"minecraft".equals(id.getNamespace()))
			return ItemStack.EMPTY;
		String path = id.getPath();
		if (!path.startsWith("diamond_"))
			return ItemStack.EMPTY;
		ResourceLocation targetId =
			ResourceLocation.fromNamespaceAndPath("minecraft", "netherite_" + path.substring("diamond_".length()));
		Item target = BuiltInRegistries.ITEM.get(targetId); // 默认注册表：缺失项返回默认值 AIR
		if (target == null || target == Items.AIR)
			return ItemStack.EMPTY;
		// 等价"锻造升级"：换物品但保留附魔/自定义名等组件
		return new ItemStack(target.builtInRegistryHolder(), 1, auxTool.getComponentsPatch());
	}

	// ================= 路径 3｜锻造融合（③c：Vintage auto_smithing → 借原版锻造配方产出） =================

	/** 认领"锻造融合"配方并借原版锻造配方推导产物（详见 {@link #smithingBridgeResult}）。 */
	private static final class SmithingStrategy implements Strategy {

		/**
		 * 角色排列表：{@code {templateStacksIdx, baseStacksIdx, additionStacksIdx}}——
		 * {@code stacks} 内的 3 件物品在 {@link SmithingRecipeInput} 三个角色上的<b>全部 6 种分配</b>。
		 */
		private static final int[][] SMITHING_ROLE_ORDERS = {
			{ 0, 1, 2 }, { 0, 2, 1 }, { 1, 0, 2 }, { 1, 2, 0 }, { 2, 0, 1 }, { 2, 1, 0 }
		};

		@Override
		public boolean claims(Candidate candidate) {
			return isAutoSmithingRecipe(candidate.recipe);
		}

		@Override
		public List<ItemStack> compute(Context ctx, Candidate candidate, ItemStack single, IItemHandler handler,
			BlockPos around) {
			return smithingBridgeResult(ctx, candidate, single, handler);
		}
	}

	/**
	 * 是否"锻造融合"配方（{@code vintageimprovements:auto_smithing}）：3 输入
	 * （锻造模板 → 可锻造盔甲 → 锻造材料），<b>results 为空</b>——真实产物来自原版锻造配方。
	 * 用配方类型 id 判定（Vintage 可选 mod，content 不得 import 其类）。
	 */
	public static boolean isAutoSmithingRecipe(Recipe<?> recipe) {
		if (!(recipe instanceof ProcessingRecipe<?, ?>))
			return false;
		ResourceLocation typeKey = typeKeyOf(recipe);
		return typeKey != null && "vintageimprovements".equals(typeKey.getNamespace())
			&& "auto_smithing".equals(typeKey.getPath());
	}

	/**
	 * <b>锻造融合（auto_smithing）真实产物推导</b>：该配方 JSON 的 {@code results} 为空，
	 * Vintage 的真实产物逻辑在机器实体里——{@code HelveBlockEntity} 持有 {@code lastSmithingRecipe}
	 * （原版 {@code net.minecraft.world.item.crafting.SmithingRecipe}），并构造原版
	 * {@link SmithingRecipeInput} 调 {@code assemble} 产出（就是"给盔甲加纹饰"）。变体波没有
	 * 机器实体，这里等价复用<b>原版锻造配方</b>本身：遍历 {@link RecipeType#SMITHING} 全部配方
	 * （含 {@code smithing_transform} 与 {@code smithing_trim} 两类），找到能匹配的就 assemble 出产物。
	 *
	 * <p><b>三件物品的角色映射依据（实证，非猜测）</b>：</p>
	 * <ol>
	 *   <li>{@code javap} 确认构造器签名为
	 *       {@code SmithingRecipeInput(ItemStack template, ItemStack base, ItemStack addition)}
	 *       （record 访问器 {@code template()/base()/addition()} 同序），即"模板、底材、附加材料"；</li>
	 *   <li>{@code auto_smithing.json} 的 ingredients 顺序为
	 *       {@code #minecraft:trim_templates} → {@code #minecraft:trimmable_armor} →
	 *       {@code #minecraft:trim_materials}，与上述 template/base/addition 顺序<b>一一对应</b>；
	 *       原版 {@code *_smithing_trim.json} 的字段顺序（template/base/addition）也完全一致；</li>
	 *   <li>Vintage 自己的 {@code HelveBlockEntity.processSmith()} 字节码显示：它<b>不按槽位顺序</b>，
	 *       而是用配方的 {@code isTemplateIngredient / isBaseIngredient / isAdditionIngredient}
	 *       逐件归类（归到 bufInv 的 0/1/2 号槽），再
	 *       {@code new SmithingRecipeInput(buf0, buf1, buf2)} + {@code assemble(input, level.registryAccess())}
	 *       + {@code matches(input, level())} 复核——即角色由<b>配方判定</b>决定，与来源槽位无关。</li>
	 * </ol>
	 * <p>因此本方法采用"<b>排列穷举 + matches 复核</b>"：把 {@code stacks = [主料, 辅料1, 辅料2]}
	 * 的 6 种角色分配逐个代入原版 {@code SmithingRecipe.matches(...)}（该方法即
	 * {@code template.test(t) && base.test(b) && addition.test(a)}），只要有一种成立即为正确映射，
	 * 比硬编码下标顺序更稳（等价于 Vintage 的角色判定，且以原版 matches 为准绳，不会张冠李戴）。</p>
	 *
	 * <p>找不到任何匹配配方（或 API 异常）→ 返回 {@code null}：调用方按"无产物可执行"放弃，
	 * <b>不消耗任何物品</b>，绝不硬造产物、不抛异常。</p>
	 *
	 * @param single   主料（命中物品/槽内物品，count 已置 1）
	 * @param candidate 已确认的候选（{@code auxes} 指向载荷或命中容器中的其余 2 件）
	 * @param handler  命中容器（解析 CONTAINER 来源辅料用；掉落物路径传 null）
	 */
	private static List<ItemStack> smithingBridgeResult(Context ctx, Candidate candidate, ItemStack single,
		IItemHandler handler) {
		List<ItemStack> stacks = new ArrayList<>(1 + candidate.auxes.size());
		stacks.add(single);
		for (AuxRef ref : candidate.auxes) {
			ItemStack aux = ctx.aux.resolveAux(ref, handler);
			if (aux.isEmpty())
				return null;
			ItemStack one = aux.copy();
			one.setCount(1);
			stacks.add(one);
		}
		if (stacks.size() < 3) {
			ctx.debug.log("无产物：锻造融合需要 3 件（模板/盔甲/材料），实得 {} 件", stacks.size());
			return null;
		}
		try {
			for (RecipeHolder<SmithingRecipe> holder : ctx.level.getRecipeManager()
				.getAllRecipesFor(RecipeType.SMITHING)) {
				SmithingRecipe smithing = holder.value();
				if (smithing == null)
					continue;
				for (int[] roles : SmithingStrategy.SMITHING_ROLE_ORDERS) {
					if (roles[0] >= stacks.size() || roles[1] >= stacks.size() || roles[2] >= stacks.size())
						continue;
					SmithingRecipeInput input = new SmithingRecipeInput(stacks.get(roles[0]), stacks.get(roles[1]),
						stacks.get(roles[2]));
					if (!smithing.matches(input, ctx.level))
						continue; // 该角色分配不成立（原版 matches 即三角色 ingredient 全测）
					ItemStack out = smithing.assemble(input, ctx.level.registryAccess());
					if (out.isEmpty())
						continue;
					ctx.debug.log("锻造融合产物：原版配方 {} 命中（角色分配 {}/{}/{}）→ {} ×{}", holder.id(), roles[0],
						roles[1], roles[2], out.getItem(), out.getCount());
					List<ItemStack> one = new ArrayList<>(1);
					one.add(out);
					return one;
				}
			}
		} catch (Throwable ignored) {
			// 配方管理器不可用 / 个别原版锻造配方 assemble 异常：按"无可执行产物"放弃（不消耗、不抛）
			return null;
		}
		ctx.debug.log("无产物：原版 SMITHING 配方中找不到能匹配当前 3 件物品者（模板/盔甲/材料组合无效）");
		return null;
	}

	// ================= 路径 4｜默认（RecipeApplier 滚结果；恒兜底） =================

	/** 兜底路径：除上述三条之外的普通配方一律走 RecipeApplier 滚结果。 */
	private static final class RecipeApplierStrategy implements Strategy {

		@Override
		public boolean claims(Candidate candidate) {
			return true;
		}

		@Override
		public List<ItemStack> compute(Context ctx, Candidate candidate, ItemStack single, IItemHandler handler,
			BlockPos around) {
			List<ItemStack> results = RecipeApplier.applyRecipeOn(ctx.level, single, candidate.recipe, false);
			if (results.isEmpty() && !hasFluidOutput(candidate.recipe) && candidate.fluids.isEmpty()) {
				ctx.debug.log("无产物：{} [{}] 的 RecipeApplier 结果为空且无流体产物", candidate.id,
					typeKeyString(candidate.recipe));
				return null;
			}
			return results;
		}
	}

	/** 配方是否有流体产物（纯流体产物配方如 硫磺→SO₂：物品结果为空但可执行）。 */
	public static boolean hasFluidOutput(Recipe<?> recipe) {
		return recipe instanceof ProcessingRecipe<?, ?> pr && !pr.getFluidResults()
			.isEmpty();
	}

	// ================= 产物回位提示（⑥） =================

	/**
	 * 产物回位目标槽（⑥）：对"辅料即产物来源"的推导类配方——{@code auto_upgrade}（钻石工具 → 下界合金版）
	 * 与 {@code auto_smithing}（盔甲 + 纹饰 → 带纹饰盔甲）——产物在语义上就是<b>辅料被加工成的新物</b>，
	 * 故优先放回<b>首个辅料所在的容器槽</b>（{@code auxes.get(0)} 恰好是配方 JSON 里承担"被改造物"角色的
	 * ingredient[1]：auto_upgrade = 工具、auto_smithing = 可锻造盔甲）。
	 * 该辅料来自波载荷（通道 A）或没有容器上下文（掉落物路径）时，回退主料槽 {@code mainSlot}。
	 * 普通配方（产物 = 全新物品）一律回主料槽，行为与改动前一致。
	 *
	 * @param mainSlot 主料槽（回退目标）
	 * @return 产物优先插入的槽号
	 */
	public static int productSlotHint(Candidate candidate, int mainSlot) {
		if (!isUpgradeTransformationRecipe(candidate.recipe) && !isAutoSmithingRecipe(candidate.recipe))
			return mainSlot;
		AuxRef first = candidate.firstAux();
		if (first != null && first.source() == AuxSource.CONTAINER && first.index() >= 0)
			return first.index();
		return mainSlot;
	}

	// ================= 诊断描述 =================

	/** 产物列表的简短描述（轨迹日志用）。 */
	public static String describeResults(List<ItemStack> results) {
		if (results == null || results.isEmpty())
			return "（无物品产物）";
		StringBuilder sb = new StringBuilder();
		for (ItemStack s : results) {
			if (s.isEmpty())
				continue;
			if (sb.length() > 0)
				sb.append(" + ");
			sb.append(s.getItem()).append('×').append(s.getCount());
		}
		return sb.length() == 0 ? "（无物品产物）" : sb.toString();
	}

	/** 容器内容摘要（轨迹日志用；最多列 6 个非空槽）。 */
	public static String describeHandler(IItemHandler handler) {
		if (handler == null)
			return "（无容器）";
		StringBuilder sb = new StringBuilder();
		int shown = 0;
		for (int i = 0; i < handler.getSlots() && shown < 6; i++) {
			ItemStack s = handler.getStackInSlot(i);
			if (s.isEmpty())
				continue;
			if (sb.length() > 0)
				sb.append(", ");
			sb.append('[').append(i).append(']').append(s.getItem()).append('×').append(s.getCount());
			shown++;
		}
		return sb.length() == 0 ? "（空）" : sb.toString();
	}

	// ================= 配方类型判定（随静态判定一并落在此处，供实体委托调用） =================

	/** 配方类型 id（BuiltIn 注册表查 key；查不到返回 null）。 */
	public static ResourceLocation typeKeyOf(Recipe<?> recipe) {
		try {
			return BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 配方类型 id 字符串（诊断日志用；取不到返回 {@code "?"}）。 */
	public static String typeKeyString(Recipe<?> recipe) {
		ResourceLocation key = typeKeyOf(recipe);
		return key == null ? "?" : key.toString();
	}
}
