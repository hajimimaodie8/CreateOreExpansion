package com.hjmmd_8.createoreexpansion.compat.vintageimprovements;

import com.negodya1.vintageimprovements.content.kinetics.grinder.PolishingRecipe;
import com.negodya1.vintageimprovements.infrastructure.config.VintageConfig;

import net.minecraft.world.item.crafting.Recipe;

/**
 * Create: Vintage Improvements（modid {@code vintageimprovements}）配方的<b>转速档</b>读取
 * （星辉波变器载荷侧联动，与 {@link VintageRecipeEnergy} 同属"配方字段读取"一族，
 * 区别于 {@link VintageImprovementsMachineIntegration} 的机器注册侧）。
 *
 * <p><b>为什么有这一档</b>：Vintage 的<b>砂带打磨机（belt grinder）</b>配方每条自带
 * {@code speed_limits}（1/2/3 = 低/中/高档）。它的实现（去混淆字节码核对
 * {@code GrinderBlockEntity#getCurrentSpeedMode} 与配方搜索逻辑）是：</p>
 * <ol>
 *   <li>机器按<b>自身转速</b>算当前档位：{@code 停转 → 0；|RPM| ≤ lowSpeedValue（默认 16）→ 1；
 *       ≤ mediumSpeedValue（默认 64）→ 2；更高 → 3}（阈值取自 Vintage 自己的
 *       {@code VCRecipes.lowSpeedValue / mediumSpeedValue} 配置，整合包可改）；</li>
 *   <li>配方按 {@code speed_limits == 当前档位} 分成优选桶与兜底桶：<b>优先执行档位匹配的配方，
 *       一条都不匹配时才退化执行不匹配的</b>——所以它是<b>优先级，不是硬门槛</b>。</li>
 * </ol>
 *
 * <p>本模组的对应口径（用户 2026-09 拍板）：<b>波执行配方时的"加工转速"= 变器自身转速</b>
 * （波携带 {@code carriedRpm}），排序时优先选中与波转速档匹配的配方，与 Vintage 自身的
 * "优选 → 兜底"次序一致。</p>
 *
 * <p><b>完全可选集成</b>：本类只在被调用且 Vintage 已安装时才触碰 Vintage 类；
 * 未安装时第一行即返回（沿用 ModList 隔离范式）；配置读取失败退回文档口径的 16 / 64 默认值。</p>
 */
public final class VintageRecipeSpeed {

	/** 配置读不到时的兜底阈值（Vintage ponder 文档口径：低 ≤16、中 16~64、高 >64）。 */
	private static final int FALLBACK_LOW_RPM = 16;
	private static final int FALLBACK_MEDIUM_RPM = 64;

	private VintageRecipeSpeed() {
	}

	private static boolean isLoaded() {
		return net.neoforged.fml.ModList.get() != null
			&& net.neoforged.fml.ModList.get()
				.isLoaded("vintageimprovements");
	}

	/**
	 * 配方要求的转速档：{@code 1} 低 / {@code 2} 中 / {@code 3} 高；{@code 0} = 无要求
	 * （非 Vintage 抛光配方、未安装 Vintage、或字段缺省）。
	 */
	public static int requiredSpeedMode(Recipe<?> recipe) {
		if (recipe == null || !isLoaded())
			return 0;
		try {
			if (recipe instanceof PolishingRecipe polishing)
				return Math.max(0, polishing.getSpeedLimits());
		} catch (Throwable ignored) {
			// 类缺失/字段异常：按"无要求"处理（排序退化为原逻辑，不影响能否执行）
		}
		return 0;
	}

	/**
	 * 按给定 RPM 判定 Vintage 转速档（与 {@code GrinderBlockEntity#getCurrentSpeedMode()} 同口径同配置）：
	 * {@code 0} = 停转；{@code 1} = |RPM| ≤ low；{@code 2} = ≤ medium；{@code 3} = 更高。
	 */
	public static int speedModeOf(float rpm) {
		if (!isLoaded() || rpm == 0f)
			return 0;
		float abs = Math.abs(rpm);
		try {
			var recipes = VintageConfig.server().recipes;
			if (abs <= recipes.lowSpeedValue.get())
				return 1;
			if (abs <= recipes.mediumSpeedValue.get())
				return 2;
			return 3;
		} catch (Throwable ignored) {
			// 配置不可读（未加载/时序）：退回文档默认阈值
			if (abs <= FALLBACK_LOW_RPM)
				return 1;
			return abs <= FALLBACK_MEDIUM_RPM ? 2 : 3;
		}
	}
}
