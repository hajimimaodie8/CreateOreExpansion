package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
import com.hjmmd_8.createoreexpansion.common.energy.MedallionLink;
import com.hjmmd_8.createoreexpansion.common.energy.ToolDataComponents;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import java.awt.Color;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 工具能量门面：能量存取、扣减编排与剩余能量提示。
 *
 * <p><b>P3p</b>：本类已搬进共享库（core）。旧技能框架（{@code ItemSkill}/{@code DataSkill}）
 * 专用的「按技能实例算消耗并扣能」那一支搬去了层里的
 * {@code skill.SkillEnergySpend}（P3r 后旧技能框架住 skill 包），
 * 这里只留与技能类型无关的部分；凝能佩联动改走 {@link MedallionLink} 注入契约
 * （层里的 {@code IMedallion} 不能被库 import）。</p>
 *
 * <p>职责边界：</p>
 * <ul>
 *     <li>能量存取与判定（{@link #getEnergy}/{@link #setEnergy}/{@link #canAfford} 等）；</li>
 *     <li>扣能编排（{@link #consume}，含凝能佩兜底与剩余能量提示）；</li>
 *     <li>能量提示（{@link #sendLowEnergy} 等，护目镜限定）。</li>
 * </ul>
 *
 * <p>消耗数值计算见 {@link SkillEnergyCost}，附魔读取见 {@link ToolEnchantments}。</p>
 */
public final class ToolEnergy {
	/** 能量不足时在快捷栏上方显示的提示文案（全模组统一） */
	public static final String LOW_ENERGY_MESSAGE = "由于能量不足，无法释放技能！";

	private ToolEnergy() {}

	/**
	 * 获取物品的最大能量值
	 * @param stack 待处理的物品
	 * @return 最大能量值，物品无最大能量值时返回 -1
	 */
	public static int getMaxEnergy(ItemStack stack) {
		Integer mx = stack.getComponents().get(ToolDataComponents.MAX_ENERGY);
		if (mx != null)
			return mx;
		return -1;
	}

	/**
	 * 判断物品是否具有能量值
	 * @param stack 待处理的物品
	 * @return 是否具有能量值
	 */
	public static boolean hasEnergy(ItemStack stack) {
		int max = getMaxEnergy(stack);
		return max != -1;
	}

	/**
	 * <b>该物品能不能充能</b>——"有充能条才算数"这条口径的<b>唯一判据</b>（用户 2026-09-15 明确要求
	 * "普通物品没有充能条，自然就不能进行充能"，并要求把"普通物品 / 雷鸣系列武器工具"区分清楚）。
	 *
	 * <p>实现上等同于 {@link #hasEnergy}（判据 = 物品带 {@code MAX_ENERGY} 组件），单独开一个名字是为了
	 * <b>在调用点上把意图写清楚</b>：凡是"雷击补能 / 充能器充能 / 佩给工具补能"这类路径，都必须先过这一关。
	 * 于是：</p>
	 * <ul>
	 *   <li><b>雷鸣系列工具/武器</b>（带充能条）→ true，可充能；</li>
	 *   <li><b>雷鸣系列材料</b>（锭/碎块/板/杆/线）与<b>方块物品</b>（雷鸣块）→ <b>false</b>：
	 *       它们属于雷鸣系列（有岩浆免疫等系列特性），但<b>没有充能条</b>，不参与充能；</li>
	 *   <li><b>普通物品</b>（石头、别的模组的工具…）→ false。</li>
	 * </ul>
	 *
	 * <p><b>以后新增"带充能条"的物品时</b>：先确认它到底属于哪一类——是雷鸣/星辉系列的武器工具
	 * （应当能充能、并进系列标签拿系列特性），还是普通物品（不应当能充能）。{@link #setEnergy} 本身
	 * 也有 {@code max <= 0 → 直接返回} 的兜底，所以"给普通物品写能量"这件事在物理上写不进去。</p>
	 */
	public static boolean canCharge(ItemStack stack) {
		return hasEnergy(stack);
	}

	/**
	 * 获取物品的能量值
	 * @param stack 待处理的物品
	 * @return 能量值，物品无能量值时返回 -1
	 */
	public static int getEnergy(ItemStack stack) {
		Integer energy = stack.getComponents().get(ToolDataComponents.ENERGY);
		return energy != null ? energy : -1;
	}

	public static void setEnergy(ItemStack stack, int energy) {
		int max = getMaxEnergy(stack);
		if (max <= 0)
			return;
		int value = Math.max(0, Math.min(max, energy));
		stack.set(ToolDataComponents.ENERGY, value);
	}

	/**
	 * 判断当前能量是否足够释放一次技能。
	 *
	 * 判定标准：能量值大于等于所需消耗（能量为 0 或不足时返回 false）。
	 * 是能量预检查与技能释放前的统一判定入口。
	 *
	 * @param stack 待检查的物品
	 * @param cost  所需能量消耗（&lt;= 0 视为无需能量，恒可释放）
	 * @return 是否足够
	 */
	public static boolean canAfford(ItemStack stack, int cost) {
		if (cost <= 0) return true;
		return hasEnergy(stack) && getEnergy(stack) >= cost;
	}

	/**
	 * <b>当前可用能量总量</b> = 主手工具自身的 FE + 玩家身上绑定凝能佩内的 FE（无佩时即工具自身）。
	 *
	 * <p><b>仅供界面/信息显示</b>（新内核 {@code SkillResource#getAmount}）：它<b>不</b>参与任何
	 * 判定——判定口径见 {@link #canAfford(Player, ItemStack, int)}（沿用旧的工具级门槛）。</p>
	 *
	 * @param player 玩家（可为 null，为 null 时只算工具自身）
	 * @param stack  主手工具
	 * @return 可用总量（各段负值按 0 计）
	 */
	public static int getAvailable(Player player, ItemStack stack) {
		ItemStack medallion = MedallionLink.get().findBound(player, stack);
		return storedEnergy(stack) + storedEnergy(medallion);
	}

	/**
	 * <b>可用能量是否够一次消耗</b>——与技能类型无关的统一判定。
	 *
	 * <p><b>门槛口径刻意与旧 {@code tryConsume} 逐字一致（不许"顺手优化"）：</b></p>
	 * <ul>
	 *     <li>充能模式（绑定凝能佩且佩处于充能）→ <b>恒 true</b>：佩会在本次扣费后立刻把工具补满，
	 *         因此不设工具自身能量门槛；</li>
	 *     <li>其余情况 → <b>只看工具自身 FE</b>（{@link #canAfford(ItemStack, int)}）。
	 *         注意：<b>不是</b>"工具 + 佩"求和——旧实现就是工具级门槛，改成求和会让
	 *         "工具没能量但佩里有"这一窄场景从"拒绝"变成"由佩代付"，属于玩家可感知的
	 *         玩法改动，未经用户拍板不得扩大。</li>
	 * </ul>
	 * <p>（{@link #getAvailable(Player, ItemStack)} 报的是"工具 + 佩"的总量，那是给界面看的
	 * 信息量，<b>不</b>参与判定。）</p>
	 *
	 * @param player 释放技能的玩家（可为 null）
	 * @param stack  主手工具
	 * @param amount 本次消耗（&lt;= 0 视为无需能量，恒可支付）
	 * @return 是否够支付
	 */
	public static boolean canAfford(Player player, ItemStack stack, int amount) {
		if (amount <= 0) return true;
		return isChargeMode(MedallionLink.get().findBound(player, stack)) || canAfford(stack, amount);
	}

	/**
	 * <b>扣除能量</b>（工具 + 凝能佩兜底），成功返回 true。
	 *
	 * <p>与技能类型无关的统一扣减入口：有绑定凝能佩时由佩对象自己处理供应/充能两种模式
	 * （{@link MedallionLink#consumeToolEnergy} + 充能模式的 {@link MedallionLink#chargeBoundTools}），
	 * 否则直接扣工具自身 FE。<b>创造模式照旧扣能</b>（与旧 {@code tryConsume} 一致，
	 * 创造豁免由技能释放链路自己决定，本方法不做判断）。</p>
	 *
	 * <p>门槛沿用 {@link #canAfford(Player, ItemStack, int)}（= 旧 {@code tryConsume} 的预检查），
	 * 不足时返回 false 且不产生任何扣减。</p>
	 *
	 * @param player 释放技能的玩家（可为 null）
	 * @param stack  主手工具
	 * @param amount 本次消耗（&lt;= 0 视为无需能量，直接成功）
	 * @return 是否成功扣除（能量不足返回 false，且不产生任何扣减）
	 */
	public static boolean consume(Player player, ItemStack stack, int amount) {
		if (amount <= 0) return true;
		if (!canAfford(player, stack, amount)) {
			return false;
		}
		MedallionLink link = MedallionLink.get();
		ItemStack medallion = link.findBound(player, stack);
		if (!medallion.isEmpty() && link.isMedallion(medallion)) {
			link.consumeToolEnergy(medallion, stack, amount);
			// 充能模式：扣费后检查所有绑定工具（仅背包内），没满的补满
			link.chargeBoundTools(player, medallion);
		} else {
			setEnergy(stack, getEnergy(stack) - amount);
		}
		return true;
	}

	/** 物品自身 FE（无充能条按 0 计，负值截断为 0）。 */
	private static int storedEnergy(ItemStack stack) {
		if (stack == null || stack.isEmpty() || !hasEnergy(stack)) return 0;
		return Math.max(0, getEnergy(stack));
	}

	/** 该凝能佩是否处于充能模式（空佩恒 false）。 */
	private static boolean isChargeMode(ItemStack medallion) {
		if (medallion == null || medallion.isEmpty())
			return false;
		MedallionLink link = MedallionLink.get();
		return link.isMedallion(medallion) && link.isChargeMode(medallion);
	}

	/**
	 * 技能释放前统一检查并消耗能量。
	 *
	 * <p><b>P3p 已搬走</b>：本方法原先在这里，因为它按旧技能框架的 {@code ItemSkill} 算消耗
	 * （要读 {@code SKILLS} 组件与技能注册表），而库不能依赖那些层内类型。
	 * 现在它是 {@code foundation.item.skill.SkillEnergySpend#tryConsume(Player, ItemStack, ItemSkill)}，
	 * 方法体逐字未改；本类通过公开的
	 * {@link #canAfford(Player, ItemStack, int)} / {@link #consume(Player, ItemStack, int)} /
	 * {@link #sendLowEnergy(Player, ItemStack)} / {@link #sendRemainingEnergyWithMedallion}
	 * 继续为它提供全部能力。</p>
	 */

	public static void sendLowEnergy(Player player, ItemStack stack) {
		int colorRGB = getEnergyColor(stack);
		player.displayClientMessage(
			Component.literal(LOW_ENERGY_MESSAGE)
				.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRGB))), 
			true);
	}

	public static void sendRemainingEnergy(Player player, ItemStack stack) {
		// 只有头部佩戴工程师护目镜时才能查看释放后的能量消耗（剩余能量）
		if (!GogglesItem.isWearingGoggles(player))
			return;
		int energy = getEnergy(stack);
		int max = getMaxEnergy(stack);
		int colorRGB = getEnergyColor(stack);
		player.displayClientMessage(
			Component.literal("剩余能量：" + energy + " / " + max)
				.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRGB))), 
			true);
	}

	/**
	 * 释放技能后显示剩余能量（物品栏上方 actionbar，受工程师护目镜限制）。
	 * 有佩：单行格式 `[模式]佩名：x/y；工具名：x/y`（佩段用佩色、工具段用工具色）；
	 * 无佩：仅工具行。
	 */
	public static void sendRemainingEnergyWithMedallion(Player player, ItemStack tool, ItemStack medallion) {
		if (!GogglesItem.isWearingGoggles(player))
			return;
		int toolEnergy = getEnergy(tool);
		int toolMax = getMaxEnergy(tool);
		Component toolLine = toolLineComponent(tool, toolEnergy, toolMax);
		if (medallion.isEmpty()) {
			player.displayClientMessage(toolLine, true);
			return;
		}
		MedallionLink link = MedallionLink.get();
		String mode = link.isMedallion(medallion) && link.isChargeMode(medallion)
			? "充能模式" : "供应模式";
		Component msg = Component.literal("[" + mode + "]")
			.withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
			.append(Component.literal(medallion.getHoverName().getString() + "：")
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(getEnergyColor(medallion)))))
			.append(Component.literal(getEnergy(medallion) + "/" + getMaxEnergy(medallion))
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(getEnergyColor(medallion)))))
			.append(Component.literal("；").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
			.append(toolLine);
		player.displayClientMessage(msg, true);
	}

	/**
	 * 工具行的剩余能量文案。翠玉之弓（传说武器）做黄→绿渐变，其余工具保持单色（工具能量色）。
	 * 仅对弓生效：其它工具显示不受影响。
	 */
	private static Component toolLineComponent(ItemStack tool, int energy, int max) {
		String text = tool.getHoverName().getString() + "：" + energy + "/" + max;
		if (tool.getItem() instanceof EnergyGradientTool) {
			// 黄（起点）→ 绿（终点）逐字符渐变
			return gradientText(text, new Color(0xFFFF55), new Color(0x55FF55));
		}
		return Component.literal(text)
			.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getEnergyColor(tool))));
	}

	/** 将文本逐字符做从 startColor 到 endColor 的线性渐变 */
	private static Component gradientText(String text, Color startColor, Color endColor) {
		MutableComponent result = Component.empty();
		int len = text.length();
		if (len == 0)
			return result;
		for (int i = 0; i < len; i++) {
			float t = (float) i / (len - 1);
			int rgb = lerpColor(startColor, endColor, t).getRGB() & 0xFFFFFF;
			result.append(Component.literal(String.valueOf(text.charAt(i)))
				.withStyle(Style.EMPTY.withColor(rgb)));
		}
		return result;
	}

	private static Color lerpColor(Color a, Color b, float t) {
		int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
		int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
		int bl = (int) (a.getBlue() + (b.getBlue()  - a.getBlue())  * t);
		return new Color(r, g, bl);
	}

	/**
	 * 获取工具能量条颜色（RGB）
	 */
	private static int getEnergyColor(ItemStack stack) {
		Integer colorValue = stack.get(ToolDataComponents.ENERGY_COLOR);
		return colorValue != null ? (colorValue & 0xFFFFFF) : 0xFFFFFF;
	}
}
