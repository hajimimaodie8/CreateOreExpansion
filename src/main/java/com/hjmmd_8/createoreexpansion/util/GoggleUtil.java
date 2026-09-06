package com.hjmmd_8.createoreexpansion.util;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/**
 * 护目镜 / 准星悬浮 tooltip 行排版工具 —— 写法学习自 Create Ore Excavation 的
 * {@code TooltipUtil}：Create 的悬浮面板（GoggleOverlayRenderer）中每一行都需
 * 以"缩进 + 内容"加入，缩进按当前字体实际空格宽度换算（默认 4 空格 = 一级），
 * 与 Create 官方 {@code LangBuilder.forGoggles} 的输出排版保持一致。
 *
 * <p>用法：{@code GoggleUtil.forGoggles(tooltip, line)} 一级行、
 * {@code forGoggles(tooltip, 1, line)} 二级行（特殊行如能量条不加缩进，保持原样）。</p>
 */
public final class GoggleUtil {

	private GoggleUtil() {
	}

	/** 一级行（4 空格缩进，同 Create 面板正文）。 */
	public static void forGoggles(List<? super MutableComponent> tooltip, MutableComponent line) {
		forGoggles(tooltip, 0, line);
	}

	/** 追加一行；indents 为额外空格数（0 = 一级 4 空格，1 = 二级 5 空格…）。 */
	public static void forGoggles(List<? super MutableComponent> tooltip, int indents, MutableComponent line) {
		tooltip.add(Component.literal(" ".repeat(spaces(Minecraft.getInstance().font, 4 + indents))).append(line));
	}

	/** 空格宽度换算：字体空格非 4px 时按比例取整，保证各级对齐（仿 TooltipUtil）。 */
	private static int spaces(Font font, int defaultSpaces) {
		int spaceWidth = font.width(" ");
		return spaceWidth == 4 ? defaultSpaces : Mth.ceil(4.0F * defaultSpaces / spaceWidth);
	}
}
