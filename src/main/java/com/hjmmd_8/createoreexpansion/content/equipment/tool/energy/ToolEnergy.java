package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
		int cost = skill.getCost();
		if (cost == 0) {
			return true;
		}
		if (!canAfford(stack, cost)) {
			if (player != null) {
				sendLowEnergy(player, stack);
			}
			return false;
		}
		setEnergy(stack, getEnergy(stack) - cost);
		if (player != null) {
			// 强制物品栏同步，确保客户端立即看到能量变化
			player.getInventory().setChanged();
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
		int energy = getEnergy(stack);
		int max = getMaxEnergy(stack);
		int colorRGB = getEnergyColor(stack);
		player.displayClientMessage(
			Component.literal("剩余能量：" + energy + " / " + max)
				.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRGB))), 
			true);
	}

	/**
	 * 获取工具能量条颜色（RGB）
	 */
	private static int getEnergyColor(ItemStack stack) {
		Integer colorValue = stack.get(AllDataComponents.ENERGY_COLOR);
		return colorValue != null ? (colorValue & 0xFFFFFF) : 0xFFFFFF;
	}
}
