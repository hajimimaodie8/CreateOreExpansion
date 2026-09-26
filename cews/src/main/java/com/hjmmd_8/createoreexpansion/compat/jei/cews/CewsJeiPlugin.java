package com.hjmmd_8.createoreexpansion.compat.jei.cews;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsJeiCategories;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>CEWS（能量波阵学）的"工作原理"流程卡 JEI 插件</b>（P7a 从
 * {@code compat/jei/CreateOreExpansionJEI} 的 CEWS 那一段拆出）。
 *
 * <p>类别集合 = 拆分前 {@code loadCategories()} 里的 {@code flowCategories} 那一段
 * （{@link CewsJeiCategories#flowCategories()}，当前只有星辉波变器流程卡一张），
 * 用的是与 {@link ChargingJEI} 同一套三段式生命周期回调（registerCategories / registerRecipes /
 * registerRecipeCatalysts），所以"无真实配方的展示卡"的行为逐字不变。</p>
 *
 * <p><b>为什么单独一个插件而不是并进 {@link ChargingJEI}</b>：P7a 只做<b>机械切分</b>
 * （原来的每个调用点一个插件），合并/UID 重整留给 P7b。插件 UID 因此是新取的
 * {@code createoreexpansion:cews_jei}；UID <b>不参与任何持久化</b>
 * （JEI 只把它打在日志标签上），对玩家零可见影响。</p>
 */
@JeiPlugin
@ParametersAreNonnullByDefault
public class CewsJeiPlugin implements IModPlugin {

	private static final ResourceLocation ID = CoeCore.modLoc("cews_jei");

	private final List<StellarWaveTransmuterCategory> flowCategories = new ArrayList<>();

	@Override
	@NotNull
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		flowCategories.clear();
		flowCategories.addAll(CewsJeiCategories.flowCategories());
		registration.addRecipeCategories(flowCategories.toArray(mezz.jei.api.recipe.category.IRecipeCategory[]::new));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		flowCategories.forEach(c -> c.registerRecipes(registration));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		flowCategories.forEach(c -> c.registerCatalysts(registration));
	}
}
