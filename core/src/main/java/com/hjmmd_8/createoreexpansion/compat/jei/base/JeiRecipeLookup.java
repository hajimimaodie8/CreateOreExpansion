package com.hjmmd_8.createoreexpansion.compat.jei.base;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * <b>JEI 侧的配方查询助手</b>（P3s：{@code CreateOreExpansionJEI#consumeTypedRecipes}
 * 的唯一新家，方法体逐字未改）。
 *
 * <p><b>为什么要单独一个类</b>：{@link CreateRecipeCategory.Builder#addTypedRecipes}
 * 需要"按配方类型把客户端已知配方喂给消费者"。原先它调的是聚合入口
 * {@code CreateOreExpansionJEI} 上的静态方法，于是<b>通用基类反向依赖 JEI 插件本体，
 * 而插件本体又 import 三个层</b>——那条链让 COE / CEWS / TRANS 的每一层都无法脱离根工程。
 * 把这段纯 Minecraft 逻辑下移到 core 之后，基类对根工程零引用。</p>
 *
 * <p><b>两处同名同签名</b>：根侧的 {@code CreateOreExpansionJEI.consumeTypedRecipes}
 * 保留为一行转发（它是对外兼容面），本类是实现处。JEI 插件 UID、类别收集顺序、
 * 侧栏顺序都不受影响。</p>
 */
public final class JeiRecipeLookup {

	private JeiRecipeLookup() {
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
