package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeJeiCategories;
import com.hjmmd_8.createoreexpansion.compat.jei.category.base.CreateRecipeCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.base.JeiRecipeLookup;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import com.hjmmd_8.createoreexpansion.common.CoeCore;

/**
 * <b>COE（矿物拓展）的 {@code @JeiPlugin}</b>——原来那个"三层合装"的
 * {@code compat/jei/CreateOreExpansionJEI} 的 COE 部分（P7a 拆出）。
 *
 * <p><b>为什么必须拆</b>：原类同时装配 TRANS 的 {@code fan_transmuting}、COE 的
 * {@code lightning}/{@code lightning_block}、CEWS 的星辉波变器流程卡。它<b>没有</b>任何一个模块
 * 可以整体容纳：{@code :coe} 只编译期依赖 core（看不到 CEWS/TRANS），{@code :cews} 看不到 TRANS，
 * 而 {@code TRANS -> CEWS} 是禁止方向。加上 JPMS 的"一个包只能属于一个 mod 文件"
 * （{@code compat.jei} 这个包本身不能同时出现在三个模块里），切分是<b>编译期就成立</b>的唯一出路。</p>
 *
 * <p>本类只做<b>机械切分</b>：类别集合 = 拆分前 {@code loadCategories()} 里的 COE 那一段
 * （{@link CoeJeiCategories#categories()}），插件 UID 沿用原来的
 * {@code createoreexpansion:core_jei}（JEI 的 UID <b>不参与任何持久化</b>，只用于日志标签）。
 * TRANS / CEWS 各自的插件见 {@code :transmutation} 与 {@code :cews} 的 {@code compat/jei/**}。</p>
 *
 * <p><b>为什么侧栏顺序不需要"保持"</b>（更正拆分前这里写错的机制描述）：JEI 侧栏分类顺序由
 * {@code config/jei/recipe-category-sort-order.ini}（{@code RecipeCategorySortingConfig}）
 * 对<b>配方类型 UID</b> 排序决定 —— 老玩家由已有的 ini 钉住，新装玩家按配方类型 UID 的字典序；
 * 与"哪个插件、按什么顺序收集类别"无关（{@code RecipeManagerInternal} 构造时会统一排序）。
 * 唯一会改顺序的操作是改类别 UID 或整个删掉某个类别，而本轮两者都没有做。</p>
 *
 * <p><b>{@code @EventBusSubscriber} 归属</b>：本类没有 {@code @EventBusSubscriber}
 * （JEI 插件由 JEI 自己的发现机制加载 —— {@code ForgePluginFinder} 扫
 * {@code ModList.get().getAllScanData()}，全局、与插件住在哪个 mod 文件无关，
 * 只要求"那个文件被加载了"）。</p>
 */
@JeiPlugin
@ParametersAreNonnullByDefault
public class CreateOreExpansionJEI implements IModPlugin {

	private static final ResourceLocation ID =
		ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "core_jei");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	/** 按层收集类别（P7a 起只剩 COE 这一段）。 */
	private void loadCategories() {
		allCategories.clear();
		allCategories.addAll(CoeJeiCategories.categories());
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
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		allCategories.forEach(c -> c.registerRecipes(registration));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		allCategories.forEach(c -> c.registerCatalysts(registration));
	}

	/**
	 * 按配方类型把客户端已知配方喂给消费者（对外兼容面）。
	 *
	 * <p><b>P3s</b>：实现已下移到 core 的 {@link JeiRecipeLookup}（同名同签名，方法体逐字未改），
	 * 这里保留一行转发，是为了不动任何既有调用点；下移的理由见那个类。</p>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends Recipe<?>> void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer,
		RecipeType<?> type) {
		JeiRecipeLookup.<T>consumeTypedRecipes(consumer, type);
	}
}
