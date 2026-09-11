package com.hjmmd_8.createoreexpansion.util;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import net.minecraft.network.chat.Component;

/**
 * 烈焰燃烧室（Create Blaze Burner）<b>热档</b>工具：变器扫描读数、护目镜与 Jade 展示
 * 共用同一套档位口径，避免三处各写一份「档位 → 文案 / 颜色 / 是否达标」映射而漂移。
 *
 * <p><b>档位来源</b>：Create 的加热档位是<b>方块状态</b>属性
 * （{@code BlazeBurnerBlock.HEAT_LEVEL}，枚举序 NONE &lt; SMOULDERING &lt; FADING &lt;
 * KINDLED &lt; SEETHING），由燃烧室方块实体在燃料变化时写回世界；本模组一律读方块状态
 * （{@link BlazeBurnerBlock#getHeatLevelOf}），与 Create 机器自身读热方式同源。</p>
 *
 * <p><b>是否达标</b>直接复用 Create {@link HeatCondition#testBlazeBurner}：
 * {@code HEATED} = 除 NONE/SMOULDERING 之外（含将熄 FADING、点燃 KINDLED、超级 SEETHING）；
 * {@code SUPERHEATED} = 仅 SEETHING。即一个超级加热的燃烧室同时给出普通 + 超级两档。</p>
 */
public final class HeatLevelNames {

	/** 全部档位（序与 NBT 落盘/网络同步的序号一致）。 */
	private static final BlazeBurnerBlock.HeatLevel[] VALUES = BlazeBurnerBlock.HeatLevel.values();

	private HeatLevelNames() {
	}

	/** 档位总数（NBT 读回的合法序号范围检查用）。 */
	public static int count() {
		return VALUES.length;
	}

	/** 按序号取档位（越界一律回退 {@link BlazeBurnerBlock.HeatLevel#NONE}）。 */
	public static BlazeBurnerBlock.HeatLevel byOrdinal(int ordinal) {
		return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : BlazeBurnerBlock.HeatLevel.NONE;
	}

	/** 档位 id（{@code none} / {@code smouldering} / {@code fading} / {@code kindled} / {@code seething}）。 */
	public static String idOf(BlazeBurnerBlock.HeatLevel level) {
		return level == null ? "none" : level.getSerializedName();
	}

	/** 档位显示名（词条 {@code createoreexpansion.heat_level.<id>}；{@code 无} / {@code 普通加热} / {@code 超级加热}…）。 */
	public static Component displayName(BlazeBurnerBlock.HeatLevel level) {
		return Component.translatable("createoreexpansion.heat_level." + idOf(level));
	}

	/** 该档位是否满足「普通加热」（KINDLED 及以上；将熄 FADING 也算，与 Create HEATED 同口径）。 */
	public static boolean satisfiesHeated(BlazeBurnerBlock.HeatLevel level) {
		return HeatCondition.HEATED.testBlazeBurner(level == null ? BlazeBurnerBlock.HeatLevel.NONE : level);
	}

	/** 该档位是否满足「超级加热」（仅 SEETHING）。 */
	public static boolean satisfiesSuperheated(BlazeBurnerBlock.HeatLevel level) {
		return HeatCondition.SUPERHEATED.testBlazeBurner(level == null ? BlazeBurnerBlock.HeatLevel.NONE : level);
	}

	/**
	 * 档位文字颜色：对齐 Create 热需求配色（普通加热橙 = HEATED 0xE88300、
	 * 超级加热蓝 = SUPERHEATED 0x5C93E8），无热/余烬用灰。
	 */
	public static int colorOf(BlazeBurnerBlock.HeatLevel level) {
		if (level == null || level == BlazeBurnerBlock.HeatLevel.NONE)
			return 0x808080;
		if (level == BlazeBurnerBlock.HeatLevel.SEETHING)
			return 0x5C93E8;
		if (level == BlazeBurnerBlock.HeatLevel.SMOULDERING)
			return 0x808080;
		return 0xE88300;
	}
}
