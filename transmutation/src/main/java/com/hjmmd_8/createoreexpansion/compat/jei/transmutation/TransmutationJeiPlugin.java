package com.hjmmd_8.createoreexpansion.compat.jei.transmutation;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationJeiCategories;
import com.hjmmd_8.createoreexpansion.compat.jei.category.base.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>TRANS（机械嬗变化）的 {@code @JeiPlugin}</b>（P7a 从 {@code compat/jei/CreateOreExpansionJEI}
 * 的 TRANS 那一段拆出）。
 *
 * <p>类别集合 = 拆分前 {@code loadCategories()} 里的 TRANS 那一段
 * （{@link TransmutationJeiCategories#categories()}，当前只有 {@code fan_transmuting} 一张），
 * 用的是与原来同一套三段式生命周期回调，所以类别内容逐字不变。</p>
 *
 * <p><b>为什么必须有它</b>：JEI 的插件发现走 {@code ModList.get().getAllScanData()}（全局扫描，
 * 与插件住在哪个 mod 文件无关，只要求那个文件被加载）。拆分前三个插件类都住在根 jar 里，
 * 于是"只装 transmutation.jar"时 TRANS 的嬗变类别也没人装配。每层一份之后，
 * 该模块自己的类别才随它一起到场。</p>
 *
 * <p>插件 UID 新取 {@code createoreexpansion:transmutation_jei}；JEI 的 UID
 * <b>不参与任何持久化</b>（只用于日志标签），对玩家零可见影响。
 * 侧栏顺序与插件无关（由 {@code recipe-category-sort-order.ini} 对配方类型 UID 排序决定）。</p>
 */
@JeiPlugin
@ParametersAreNonnullByDefault
public class TransmutationJeiPlugin implements IModPlugin {

	private static final ResourceLocation ID = CoeCore.modLoc("transmutation_jei");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	@Override
	@NotNull
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		allCategories.clear();
		allCategories.addAll(TransmutationJeiCategories.categories());
		registration.addRecipeCategories(allCategories.toArray(IRecipeCategory[]::new));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		allCategories.forEach(c -> c.registerRecipes(registration));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		allCategories.forEach(c -> c.registerCatalysts(registration));
	}
}
