package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 工具能量门面：能量存取、技能释放前的统一消耗编排与剩余能量提示。
 *
 * <p>职责边界：</p>
 * <ul>
 *     <li>能量存取与判定（{@link #getEnergy}/{@link #setEnergy}/{@link #canAfford} 等）；</li>
 *     <li>技能释放消耗编排（{@link #tryConsume}，含凝能佩兜底与剩余能量提示）；</li>
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
		Integer mx = stack.getComponents().get(AllDataComponents.MAX_ENERGY);
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
	 * 获取物品的能量值
	 * @param stack 待处理的物品
	 * @return 能量值，物品无能量值时返回 -1
	 */
	public static int getEnergy(ItemStack stack) {
		Integer energy = stack.getComponents().get(AllDataComponents.ENERGY);
		return energy != null ? energy : -1;
	}

	public static void setEnergy(ItemStack stack, int energy) {
		int max = getMaxEnergy(stack);
		if (max <= 0)
			return;
		int value = Math.max(0, Math.min(max, energy));
		stack.set(AllDataComponents.ENERGY, value);
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
	 * 技能释放前统一检查并消耗能量。
	 *
	 * 由各技能在“真正生效前”调用一次（例如破坏方块前、收割前），
	 * 能量不足时发送低能量提示并返回 false，技能应放弃本次释放。
	 *
	 * 注意：无论创造模式与否都会消耗能量（与旧行为一致），
	 * 消耗后立即标记物品栏变更，确保客户端能量条同步刷新。
	 *
	 * @param player 释放技能的玩家（可为 null）
	 * @param stack  手持的工具
	 * @param skill  将要释放的技能（通过 {@link ItemSkill#getCost()} 获取消耗）
	 * @return 是否成功消耗能量（true 表示可以继续执行技能）
	 */
	public static boolean tryConsume(Player player, ItemStack stack, ItemSkill skill) {
		int cost = SkillEnergyCost.compute(stack, skill);
		if (cost == 0) {
			return true;
		}
		// 充能模式下由凝能佩兜底：跳过工具自身能量预检（释放后佩会立刻把工具补满）
		ItemStack medallion = IMedallion.findBoundMedallion(player, stack);
		boolean chargeMode = !medallion.isEmpty()
			&& medallion.getItem() instanceof IMedallion im && im.isChargeMode(medallion);
		if (!chargeMode && !canAfford(stack, cost)) {
			if (player != null) {
				sendLowEnergy(player, stack);
			}
			return false;
		}
		// 已绑定凝能佩：优先消耗佩内储存的应力能量（供应/充能模式由佩对象自身处理）
		if (!medallion.isEmpty() && medallion.getItem() instanceof IMedallion medallionImpl) {
			medallionImpl.consumeToolEnergy(medallion, stack, cost);
			// 充能模式：释放后检查所有绑定工具（仅背包内），没满的补满
			medallionImpl.chargeBoundTools(player, medallion);
		} else {
			setEnergy(stack, getEnergy(stack) - cost);
		}
		if (player != null) {
			// 强制物品栏同步，确保客户端立即看到能量变化
			player.getInventory().setChanged();
			// 同步显示剩余能量：绑定的凝能佩行在上、工具行在下（护目镜判定）
			sendRemainingEnergyWithMedallion(player, stack, medallion);
		}
		return true;
	}

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
		Component toolLine = Component.literal(tool.getHoverName().getString() + "：" + toolEnergy + "/" + toolMax)
			.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(getEnergyColor(tool))));
		if (medallion.isEmpty()) {
			player.displayClientMessage(toolLine, true);
			return;
		}
		String mode = medallion.getItem() instanceof IMedallion im && im.isChargeMode(medallion)
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
	 * 获取工具能量条颜色（RGB）
	 */
	private static int getEnergyColor(ItemStack stack) {
		Integer colorValue = stack.get(AllDataComponents.ENERGY_COLOR);
		return colorValue != null ? (colorValue & 0xFFFFFF) : 0xFFFFFF;
	}
}
