package com.hjmmd_8.createoreexpansion.util;

import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 配方类型显示名工具：把 Create ProcessingRecipe 类型 id（或其 path）映射为
 * 本地化短名（词条 {@code createoreexpansion.recipe_type.<path>}），护目镜面板与
 * Jade 悬浮共用同一张已知类型表。
 *
 * <p>已知类型（有词条）显示译名；未来其它 mod 追加的类型不在表中时回退显示
 * 原始英文 path（不显示词条键本身）。</p>
 */
public final class RecipeTypeNames {

	/** 已知且已本地化的配方类型 path 集合（与 {@code recipe_type.*} 词条一一对应）。 */
	private static final Set<String> LOCALIZED_PATHS = Set.of(
		// Create 原生
		"pressing", "cutting", "milling", "crushing", "splashing", "haunting", "deploying",
		"item_application", "mixing",
		// Create Optical 聚光器
		"focusing",
		// Vintage Improvements
		"coiling", "curving", "polishing", "centrifugation", "vibrating", "leaves_vibrating",
		"pressurizing", "vacuumizing", "hammering", "auto_smithing", "auto_upgrade", "turning",
		"laser_cutting",
		// CC&A 特斯拉线圈放电
		"charging",
		// CC&A 轧制（rolling mill）
		"rolling",
		// 本模组
		"lightning",
		// 本模组：角磨床（三级角磨轮专属的打磨/拆解）
		"grinding", "dismantling");

	private RecipeTypeNames() {
	}

	/** 该类型 path 是否已有本地化词条。 */
	public static boolean isLocalized(String path) {
		return path != null && LOCALIZED_PATHS.contains(path);
	}

	/** 类型显示名组件：已知类型走词条译名，未知类型回退英文 path。 */
	public static Component displayName(ResourceLocation typeId) {
		String path = typeId.getPath();
		if (isLocalized(path))
			return Component.translatable("createoreexpansion.recipe_type." + path);
		return Component.literal(path);
	}
}
