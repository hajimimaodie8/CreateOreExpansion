package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsJeiCategories;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeJeiCategories;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationJeiCategories;
import com.hjmmd_8.createoreexpansion.compat.jei.cews.StellarWaveTransmuterCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.coe.base.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import com.hjmmd_8.createoreexpansion.common.CoeCore;

/**
 * <b>JEI 的协调入口</b>（P3c：四个"多层枢纽文件"按层拆分后保留的同名入口 + <b>唯一的</b>
 * {@code @JeiPlugin}）。
 *
 * <p><b>拆分前</b>：本类一个类里同时写着 TRANS 的嬗变类别、COE 的雷击/方块雷击类别、
 * CEWS 的星辉波变器流程卡——三层的东西混在一处。</p>
 *
 * <p><b>拆分后</b>：三层各自收集自己那一份类别与催化剂
 * （{@link TransmutationJeiCategories#categories()} /
 * {@link CoeJeiCategories#categories()} /
 * {@link CewsJeiCategories#flowCategories()}），本类只负责按<b>拆分前的逐条顺序</b>装配并转发给 JEI。
 * 插件 UID 仍是 {@code createoreexpansion:core_jei}（<b>没有</b>改动，JEI 用 UID 做唯一键）。</p>
 *
 * <p><b>本轮的关键取舍：为什么不拆成三个 {@code @JeiPlugin}</b></p>
 * <ul>
 *   <li><b>风险</b>：JEI 的插件身份就是 UID。多出两个 {@code @JeiPlugin} 就多出两个 UID、两份
 *       插件实例与两轮 {@code registerCategories/registerRecipes/registerRecipeCatalysts}
 *       回调；而本轮的硬指标是"产物与行为零变化"，多出的插件会改变 JEI 看到的东西
 *       （尤其是分类在侧栏里的顺序与归属）——收益只是"包位置更整齐"。</li>
 *   <li><b>已有先例</b>：本模组早就按层分过 JEI 插件——{@code compat/jei/GrindingJEI}
 *       （COE 角磨/拆解）与 {@code compat/jei/ChargingJEI}（CEWS 充能）各自就是独立插件、
 *       各自的 UID。也就是说"按层拆 JEI 插件"这条路已经由那两个文件占着，
 *       而 {@code CreateOreExpansionJEI} 里剩下的是"必须排在同一个插件里"的那部分。</li>
 *   <li><b>所以</b>：本轮只做"各层收集自己那一份"的分层，{@code @JeiPlugin} 本体留一个。
 *       将来真要拆成三个插件，把 {@link #loadCategories()} 里的三行换成三个插件类即可，
 *       三层的收集代码无需再动。</li>
 * </ul>
 *
 * <p><b>{@code @EventBusSubscriber} 归属</b>：本类没有 {@code @EventBusSubscriber}
 * （JEI 插件由 JEI 自己的发现机制加载，不走 NeoForge 事件总线）；本轮也没有新增任何
 * {@code @EventBusSubscriber}。附近的 {@code @EventBusSubscriber} 是
 * {@code data/CreateOreExpansionDatagen}（{@code modid = createoreexpansion}，COE 主层，
 * 一字未动）。</p>
 */
@JeiPlugin
@ParametersAreNonnullByDefault
public class CreateOreExpansionJEI implements IModPlugin {

	private static final ResourceLocation ID =
		ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "core_jei");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	/** 无真实配方的"工作原理"说明分类（目前：星辉波变器流程卡，归 CEWS）。 */
	private final List<StellarWaveTransmuterCategory> flowCategories = new ArrayList<>();

	/**
	 * 按层收集类别。调用顺序 = 拆分前 {@code loadCategories()} 里的逐条顺序，
	 * 也就是 JEI 侧栏里的分类顺序：<b>TRANS（嬗变风扇）→ COE（雷击 / 方块雷击）→ CEWS（星辉波变器）</b>。
	 *
	 * <p>注意这个顺序<b>不是</b>"COE → CEWS → TRANS"的分组顺序——拆分前本文件的类别就是这个交错顺序
	 * （嬗变排在最前、雷击居中、波变器流程卡在末）。本轮以"与拆分前逐字相同"为准。</p>
	 */
	private void loadCategories() {
		allCategories.clear();
		flowCategories.clear();

		allCategories.addAll(TransmutationJeiCategories.categories());
		allCategories.addAll(CoeJeiCategories.categories());
		flowCategories.addAll(CewsJeiCategories.flowCategories());
	}

	@Override
	@NotNull
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		loadCategories();
		registration.addRecipeCategories(allCategories.toArray(IRecipeCategory[]::new));
		registration.addRecipeCategories(flowCategories.toArray(IRecipeCategory[]::new));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		allCategories.forEach(c -> c.registerRecipes(registration));
		flowCategories.forEach(c -> c.registerRecipes(registration));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		allCategories.forEach(c -> c.registerCatalysts(registration));
		flowCategories.forEach(c -> c.registerCatalysts(registration));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends Recipe<?>> void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer,
		RecipeType<?> type) {
		List<? extends RecipeHolder<?>> map = Minecraft.getInstance()
			.getConnection()
			.getRecipeManager()
			.getAllRecipesFor((RecipeType) type);
		if (!map.isEmpty())
			map.forEach(consumer);
	}
}
