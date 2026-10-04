package com.hjmmd_8.createoreexpansion.content.grinding.recipe;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeTypes;
import com.hjmmd_8.createoreexpansion.compat.jei.subcategory.GrindingAssemblySubCategory;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * 角磨配方：动力角磨床加工（物品在角磨轮上缓慢移动并处理）。
 * 配方 JSON 形态：{@code createoreexpansion:grinding/xxx.json}。
 *
 * <p>实现 {@link IAssemblyRecipe}：角磨步骤可加入序列加工配方
 * （Create 6.0 序列步骤 = 任意实现 IAssemblyRecipe 的 ProcessingRecipe）。</p>
 */
public class GrindingRecipe extends StandardProcessingRecipe<RecipeWrapper> implements IAssemblyRecipe {

	public GrindingRecipe(ProcessingRecipeParams params) {
		super(CoeRecipeTypes.GRINDING, params);
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		if (inv.isEmpty())
			return false;
		return ingredients.get(0)
			.test(inv.getItem(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	/**
	 * <b>本类型的配方 JSON 里带 {@code processing_time}（9 条角磨配方一律 200）</b>——本方法声明
	 * "允许声明时长"，是那 9 条能被加载的前提。
	 *
	 * <p><b>为什么必须有这个覆写</b>：Create 的 {@code ProcessingRecipe#validate()} 里有一条
	 * {@code if (processingDuration > 0 && !canSpecifyDuration()) errors.add("Recipe specified a
	 * duration. Durations have no impact on this type of recipe.")}，而
	 * {@code ProcessingRecipe#codec(...)} 把这个 {@code validate()} 包成了
	 * {@code MapCodec#validate(...)} ⇒ <b>编解码两个方向都会跑它</b>。默认实现返回
	 * {@code false}，于是任何 {@code processing_time > 0} 的角磨配方：
	 * <ul>
	 *   <li><b>读</b>（游戏加载 {@code data/…/recipe/grinding/*.json}）→ codec 报错 ⇒
	 *       {@code RecipeManager} 丢掉这条配方（只在日志里留一行）；</li>
	 *   <li><b>写</b>（datagen 用 {@code StandardProcessingRecipe.Builder#duration}）→
	 *       {@code DataProvider.saveStable} 的 {@code encodeStart(...).getOrThrow()} 直接抛
	 *       {@code IllegalStateException}，{@code runData} 整轮失败。</li>
	 * </ul>
	 *
	 * <p><b>这不是"顺手加的功能"，是补一个漏掉的声明</b>：这 9 条配方从写下来那天起就带着
	 * {@code processingTime}/{@code processing_time} 键（见 {@code 470aa174}），作者显然<b>意图</b>
	 * 让它们有时长；Create 自己的同类配方（{@code CuttingRecipe}——锯切，形态与本族一模一样：
	 * 一条配方 + 一个 tick 数）也是覆写成 {@code true} 的。所以缺的是本类的声明，不是那 9 个 JSON 的键。</p>
	 *
	 * <p><b>实测证据（配方迁移 批 1，隔离沙箱里跑的 runData）</b>：
	 * ① 编码侧——{@code GrindingRecipewith id createoreexpansion:grinding/diamond_grinding_wheel
	 * failed validation: Recipe specified a duration…} ⇒ {@code runData} BUILD FAILED；
	 * ② 解码侧——沙箱里临时探针把<手写>的 {@code grinding/jade_small_shard.json} 喂给
	 * {@code CoeRecipeTypes.GRINDING.getSerializer().codec().codec().parse(JsonOps.INSTANCE, json)}
	 * 打印 {@code ERROR: Recipe specified a duration…}。⇒ 在 {@code 470aa174} 之后、本类补上这个
	 * 声明之前，这 9 条角磨配方<b>在游戏里根本不存在</b>（会被 RecipeManager 丢弃）。</p>
	 *
	 * <p><b>它对玩法没有别的影响</b>：本模组全仓没有任何地方读
	 * {@code ProcessingRecipe#getProcessingDuration()}（{@code grep -r ProcessingDuration} 只命中
	 * 注释），Create 侧读它的四处（锯/搅拌机/石磨/粉碎轮）都不可能拿到 {@code GrindingRecipe}
	 * ——角磨床是自家的 {@code PowerAngleGrinderBlockEntity}。所以这个覆写只是让"配方能被加载"，
	 * 时长本身仍然是惰性数据（与 {@code 470aa174} 之前"键写错 ⇒ 解析成 0"的净效果一致：
	 * 配方在、时长不被读）。</p>
	 */
	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	// ========== 序列加工（IAssemblyRecipe） ==========

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	@OnlyIn(Dist.CLIENT)
	public Component getDescriptionForAssembly() {
		return Component.translatable("createoreexpansion.recipe.assembly.grinding");
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(CoeMachines.POWER_ANGLE_GRINDER.get());
	}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> GrindingAssemblySubCategory::new;
	}
}
