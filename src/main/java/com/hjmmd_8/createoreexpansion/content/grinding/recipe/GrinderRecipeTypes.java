package com.hjmmd_8.createoreexpansion.content.grinding.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

/**
 * 角磨床可执行的配方类型注册表（按角磨轮等级挂接，开闭原则：对扩展开放、对修改封闭）。
 *
 * <p>内置挂接：1 级轮 = {@code GRINDING}（角磨配方，全部等级可执行）；
 * 2 级轮 += {@code CRUSHING}/{@code MILLING}（Create 粉碎轮/石磨配方）；
 * 3 级轮 += {@code DISMANTLING}（拆磨配方）。</p>
 *
 * <p>第三方拓展：调用 {@link #register(int, IRecipeTypeInfo)} 即可让指定等级及以上的
 * 角磨轮执行自己的配方类型，无需改动本模组任何代码；低等级挂接的类型对高等级同样生效。
 * 角磨床内部只遍历本注册表，不感知具体配方类型。</p>
 */
public final class GrinderRecipeTypes {

	private static final Map<Integer, List<Supplier<IRecipeTypeInfo>>> BY_TIER = new LinkedHashMap<>();

	static {
		register(1, AllRecipeTypes.GRINDING);
		register(2, com.simibubi.create.AllRecipeTypes.CRUSHING);
		register(2, com.simibubi.create.AllRecipeTypes.MILLING);
		register(3, AllRecipeTypes.DISMANTLING);
	}

	private GrinderRecipeTypes() {
	}

	/** 挂接配方类型到指定等级（该等级及以上的角磨轮可执行） */
	public static void register(int tierLevel, IRecipeTypeInfo typeInfo) {
		BY_TIER.computeIfAbsent(tierLevel, k -> new ArrayList<>())
			.add(() -> typeInfo);
	}

	/** 获取某等级角磨轮可执行的全部配方类型（含更低等级挂接的） */
	public static List<IRecipeTypeInfo> getFor(int tierLevel) {
		List<IRecipeTypeInfo> result = new ArrayList<>();
		for (int i = 1; i <= tierLevel; i++) {
			List<Supplier<IRecipeTypeInfo>> list = BY_TIER.get(i);
			if (list != null)
				list.forEach(s -> result.add(s.get()));
		}
		return result;
	}
}
