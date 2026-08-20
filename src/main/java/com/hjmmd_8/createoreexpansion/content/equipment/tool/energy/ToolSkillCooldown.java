package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 工具技能冷却（per-stack 独立，互不影响）。
 *
 * <p>冷却截止时刻记录在物品自身的 {@link AllDataComponents#SKILL_COOLDOWN_UNTIL} 组件上，
 * 因此同种物品的不同 stack 各自独立冷却（A 剑释放不影响 B 剑）。
 * 同时调用原版 {@code addCooldown} 显示快捷栏冷却图标——图标按物品类型一起显示只是视觉，
 * 实际冷却判定仍为每把工具独立。</p>
 */
public final class ToolSkillCooldown {

	private ToolSkillCooldown() {
	}

	/** 该 stack 是否已过冷却（无冷却记录或已到期） */
	public static boolean isReady(Player player, ItemStack stack) {
		if (stack.isEmpty()) return false;
		Long until = stack.get(AllDataComponents.SKILL_COOLDOWN_UNTIL);
		return until == null || until <= player.level().getGameTime();
	}

	/** 以秒为单位开启冷却（内部应用迅启附魔减冷却） */
	public static void start(Player player, ItemStack stack, int seconds) {
		startTicks(player, stack, seconds * 20);
	}

	/** 以 tick 为单位开启冷却（内部应用迅启附魔减冷却） */
	public static void startTicks(Player player, ItemStack stack, int ticks) {
		if (stack.isEmpty()) return;
		int reduced = ToolEnchantments.reduceCooldown(stack, ticks);
		stack.set(AllDataComponents.SKILL_COOLDOWN_UNTIL, player.level().getGameTime() + reduced);
		player.getInventory().setChanged();
		// 快捷栏冷却图标（按物品类型显示遮罩；实际冷却判定仍为每把工具独立）
		player.getCooldowns().addCooldown(stack.getItem(), reduced);
	}

}
