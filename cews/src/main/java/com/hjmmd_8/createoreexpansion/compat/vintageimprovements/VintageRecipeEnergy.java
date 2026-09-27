package com.hjmmd_8.createoreexpansion.compat.vintageimprovements;

import com.negodya1.vintageimprovements.content.kinetics.laser.LaserCuttingRecipe;

import net.minecraft.world.item.crafting.Recipe;

import net.neoforged.fml.ModList;

/**
 * Create: Vintage Improvements（modid {@code vintageimprovements}）配方的<b>电量需求</b>读取
 * （星辉波变器载荷侧联动，区别于 {@link VintageImprovementsMachineIntegration} 的机器注册侧）。
 *
 * <p>Vintage 的<b>激光切割（LASER_CUTTING）</b>配方每条自带 {@code energy} FE 总耗
 * （激光机本体由 CC&amp;A 电网供电）——变体波命中此类配方条目时需扣减携带电量，
 * 电量不足的条目不能执行。其余 Vintage 加工机（卷簧/辊压/磨床/真空室等）为纯应力机器，
 * 配方无电量需求，返回 0。</p>
 *
 * <p><b>完全可选集成</b>：本类只在被调用且 Vintage 已安装时才触碰 Vintage 类；
 * 未安装时第一行即返回（沿用 ModList 隔离范式）。</p>
 */
public final class VintageRecipeEnergy {

	private VintageRecipeEnergy() {
	}

	private static boolean isLoaded() {
		return net.neoforged.fml.ModList.get() != null
			&& net.neoforged.fml.ModList.get().isLoaded("vintageimprovements");
	}

	/** Vintage 配方的电量需求（FE）；未安装 Vintage / 非耗电配方返回 0。 */
	public static int energyRequiredOf(Recipe<?> recipe) {
		if (recipe == null || !isLoaded())
			return 0;
		try {
			if (recipe instanceof LaserCuttingRecipe laser)
				return Math.max(0, laser.getEnergy());
		} catch (Throwable ignored) {
			// 类缺失等异常：按无电量需求处理（配方照常按普通条目尝试）
		}
		return 0;
	}
}
