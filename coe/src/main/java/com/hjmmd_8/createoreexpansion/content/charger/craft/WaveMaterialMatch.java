package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * <b>变体波的材料匹配（含机器上下文兜底）</b>（2026-10-06 行为零变化拆分，从
 * {@link WaveCandidateEvaluator} 的 {@code matches} / {@code genericIngredientsMatch} /
 * {@code matchesQuietly} 三件<b>逐字搬出</b>——它们只回答"这一条配方与这件主料（＋已确认的辅料）
 * 到底匹不匹配"，与检索面、与辅料/流体/电量门槛都无关）。
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、参数类型由内层类名
 * {@code Context} 改写为 {@link WaveCandidateEvaluator.Context}（同一个值类型，只是它的宿主是
 * {@link WaveCandidateEvaluator}）。<b>判定顺序（BasinRecipe 手工判定 → 单槽 → 双槽 →
 * 通用兜底）、输入类型、临时 ItemStackHandler 的装配方式与三处 "异常按不匹配" 的兜底
 * 一个字未动</b>；辅料齐备性仍由调用方 {@link WaveCandidateEvaluator} 先行确认。</p>
 *
 * <p>本类<b>不 import 任何可选模组</b>（CC&amp;A / Vintage / Jade / JEI / Optical / Sable）——
 * 机器上下文族的兜底判据是"标准两种输入类型都失败"这一形状，不是某个 mod 的类型 id。</p>
 */
final class WaveMaterialMatch {

	private WaveMaterialMatch() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 判定配方是否命中当前物品（兼容 Create 6.0.10 各配方输入类型）。
	 *
	 * <p>Create 6.0.x 起不同配方类要求的 {@link RecipeInput} 子类不一致：
	 * <ul>
	 *   <li>{@code SingleRecipeInput} 型——压机 Pressing / 鼓风机 Splashing / Haunting
	 *       （{@code StandardProcessingRecipe<SingleRecipeInput>}）；</li>
	 *   <li>{@code RecipeWrapper} 型——锯 Cutting / 物品应用 ItemApplication、
	 *       Vintage 卷绕 Coiling / 车床 Turning（可带第二槽 = 辅料）；</li>
	 *   <li>{@code RecipeInput} 型——粉碎轮 Crushing / 石磨 Milling、杵锤 Hammering。</li>
	 * </ul>
	 * 若按 {@code Recipe<RecipeInput>} 统一强转调用,`SingleRecipeInput` 型配方的桥接
	 * {@code matches(RecipeInput)} 内部会 {@code checkcast SingleRecipeInput}——传
	 * {@code RecipeWrapper} 即抛 ClassCastException,导致压片等永久匹配失败（波"穿过"物品）。
	 * 故先以单槽 {@code SingleRecipeInput} 试,失败（类型不符或确实不匹配）再回退
	 * 双槽 {@code RecipeWrapper}（槽0=主料、槽1=可选辅料）。</p>
	 *
	 * <p><b>机器上下文配方兜底（③a，2026-09 通用化）</b>：Create 6 中另有一族"机器上下文"
	 * ProcessingRecipe（非 BasinRecipe 子类）的 {@code matches(RecipeInput)} <b>恒返回 false</b>
	 * ——真实匹配走绑定机器实体（如 Vintage 杵锤 {@code HammeringRecipe.matches} 字节码即
	 * {@code iconst_0; ireturn}，真实判定在静态 {@code HammeringRecipe.match(HelveBlockEntity,…)}，
	 * 依赖机器实体上的砧座/锤击数上下文）。变体波没有机器实体上下文，故在<b>标准两种输入类型
	 * 判定均失败之后</b>再做一次手工材料判定（主料命中 ingredient[0]、其余 ingredient 逐条对应
	 * 已确认的辅料——辅料可来自波载荷或命中容器其它槽），否则这类配方永远无法远程执行。</p>
	 *
	 * <p><b>为何不再按"类型 id 白名单"限定</b>：此前仅放行 {@code vintageimprovements:hammering}，
	 * 于是"任何 mod 的 matches 恒 false 机器上下文配方"都要逐个加白名单才能用。现在改为
	 * <b>通用兜底</b>——只要配方是 ProcessingRecipe 且两种标准判定都失败，就按材料手工判定，
	 * 未来任何 mod 的同类配方自动可用（无需登记）。</p>
	 *
	 * <p><b>为何该兜底安全（不会误命中普通配方）</b>：判定顺序决定了它是"最后手段"——
	 * <ol>
	 *   <li>BasinRecipe 已在上方单独手工判定；</li>
	 *   <li>普通单输入配方（压机/喷洗/闹鬼）走 {@code SingleRecipeInput} 一定成功，
	 *       根本不会走到兜底；</li>
	 *   <li>普通双/三输入配方（卷绕/车削/物品应用/变形升级）走 {@code RecipeWrapper} 成功，
	 *       也不会走到兜底；</li>
	 *   <li>只有"标准 matches 恒 false"的机器上下文族才会落到此处，而此时仍要求
	 *       {@code ingredients[0].test(input)} 成立、且其余 ingredient 逐条有对应辅料
	 *       （可来自波载荷或命中容器其它槽；辅料在 {@link WaveCandidateEvaluator} 中已先行确认）。
	 *       也就是说，材料不符的普通配方在 {@code ingredients[0].test} 这一步就会失败，
	 *       绝不会被兜底误放行；</li>
	 *   <li>兜底不放宽任何其它门槛：流体输入数/辅料齐备/流体量/电量/机器环境（加热、压弯头、
	 *       鼓风机媒介）仍由 {@link WaveCandidateEvaluator} 与 {@link WaveEnvironmentChecks#satisfied}
	 *       先行把关。</li>
	 * </ol>
	 */
	static boolean matches(WaveCandidateEvaluator.Context ctx, Recipe<?> recipe, ItemStack input, List<AuxRef> auxes,
		IItemHandler handler) {
		// BasinRecipe 系（工作盆/真空室等机器上下文配方，如 Vintage 加压/抽真空）：
		// Create 6 的 BasinRecipe.matches() 恒返回 false（真实匹配走机器静态 match，
		// 绑定 BasinBlockEntity 的过滤器/加热状态）。变体波是"远程能量执行器"，没有
		// 机器实体上下文——这里按配方自身材料需求手工判定（忽略机器过滤器与热需求，
		// 波自带加工能量），否则真空室配方永远无法远程执行。
		if (recipe instanceof com.simibubi.create.content.processing.basin.BasinRecipe basin) {
			return genericIngredientsMatch(ctx, basin, input, auxes, handler);
		}
		// 先单槽：主流单输入配方（压机/喷洗/闹鬼…）的输入类型
		net.minecraft.world.item.crafting.SingleRecipeInput single =
			new net.minecraft.world.item.crafting.SingleRecipeInput(input);
		if (matchesQuietly(ctx, recipe, single))
			return true;
		// 回退多槽：RecipeWrapper 型配方（含 2~3 输入需辅料槽者；AutoSmithing/AutoUpgrade 的
		// matches(RecipeWrapper) 只校验槽 0，辅料齐备性由 evalCandidate 的 findAux 先行确认）；
		// 单槽型配方传此会 CCE → 静默 false。槽数 = 1 主料 + 辅料数（至少 2，兼容旧双槽形状）；
		// 槽 1..n 填<b>按来源解析出的辅料真实物品副本</b>（载荷 / 命中容器槽皆可），
		// 用<b>临时</b> ItemStackHandler 装配——绝不对命中容器做 setStackInSlot（避免改动物品）。
		net.neoforged.neoforge.items.ItemStackHandler handlerProbe =
			new net.neoforged.neoforge.items.ItemStackHandler(Math.max(2, 1 + auxes.size()));
		handlerProbe.setStackInSlot(0, input);
		for (int k = 0; k < auxes.size(); k++) {
			ItemStack aux = ctx.aux.resolveAux(auxes.get(k), handler);
			if (!aux.isEmpty())
				handlerProbe.setStackInSlot(k + 1, aux.copy());
		}
		net.neoforged.neoforge.items.wrapper.RecipeWrapper wrapper =
			new net.neoforged.neoforge.items.wrapper.RecipeWrapper(handlerProbe);
		if (matchesQuietly(ctx, recipe, wrapper))
			return true;
		// 两种标准输入类型都判定失败 → 通用兜底：机器上下文配方族（matches 恒 false）按材料手工判定
		if (!(recipe instanceof ProcessingRecipe<?, ?>))
			return false; // 本引擎只执行 ProcessingRecipe 族；非该族一律不兜底
		return genericIngredientsMatch(ctx, recipe, input, auxes, handler);
	}

	/**
	 * 手工材料判定（机器上下文兜底 + BasinRecipe 共用）：主料 = 命中物品须命中
	 * {@code ingredients[0]}；其余 ingredient 逐条对应 {@link WaveCandidateEvaluator} 已确认的辅料引用
	 * （{@code auxes.get(k)} ↔ {@code ingredients.get(k+1)}，按 ingredient 升序压缩存储），
	 * 并按来源解析出真实物品<b>再验一次</b>（防"确认后被消耗/槽位变化"）；
	 * 流体/电量/环境门槛由调用方先行检查。
	 *
	 * @param handler 命中容器（解析 CONTAINER 来源辅料用；掉落物路径传 null）
	 */
	private static boolean genericIngredientsMatch(WaveCandidateEvaluator.Context ctx, Recipe<?> recipe,
		ItemStack input, List<AuxRef> auxes, IItemHandler handler) {
		var ingredients = recipe.getIngredients();
		if (ingredients.isEmpty())
			return false;
		if (!ingredients.get(0)
			.test(input))
			return false;
		for (int i = 1; i < ingredients.size(); i++) {
			int slot = i - 1;
			if (auxes == null || slot >= auxes.size())
				return false; // 辅料数不足：该 ingredient 无料可对应
			ItemStack stack = ctx.aux.resolveAux(auxes.get(slot), handler);
			if (stack.isEmpty() || !ingredients.get(i)
				.test(stack))
				return false;
		}
		return true;
	}

	/** 静默调配方 matches：输入类型不符（CCE）或桥接异常按"不匹配"处理，不向调用方抛。 */
	@SuppressWarnings("unchecked")
	private static boolean matchesQuietly(WaveCandidateEvaluator.Context ctx, Recipe<?> recipe,
		net.minecraft.world.item.crafting.RecipeInput input) {
		try {
			return ((Recipe<net.minecraft.world.item.crafting.RecipeInput>) recipe).matches(input, ctx.level);
		} catch (ClassCastException ignored) {
			return false; // 输入类型与配方要求不符（如给 SingleRecipeInput 型配方传了双槽包装）
		} catch (Throwable ignored) {
			return false; // 个别配方桥接异常：当作不匹配
		}
	}
}
