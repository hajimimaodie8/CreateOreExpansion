package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.SkillCooldowns;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.content.skill.context.LivingHurtContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class HurtLivingEntityHandler {
	/** 防重入：技能造成的后续伤害（如夺取的吸血）不得再次触发技能，否则同一 tick 内会递归释放直至能量耗尽 */
	private static boolean releasing = false;

	@SubscribeEvent
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide)
			return;
		if (!(event.getSource().getDirectEntity() instanceof Player player))
			return;
		if (releasing)
			return;

		releasing = true;
		try {
			// 新内核（Skiller）路径：已经迁移过去的受击技能由它执行。
			// 放在旧路径的按键判定之前 —— 旧 AllKeys 是纯客户端对象，专用服务器上恒为"未按下"，
			// 若放在后面会被那句 early-return 挡掉；新路径读的是服务端权威按键状态。
			// 两条路径不会重复：迁移闸门让旧路径跳过已注册进新内核的技能。
			if (player instanceof ServerPlayer serverPlayer) {
				CoeSkillRelease.release(serverPlayer, CoeSkillTypes.HIT,
						SkillContextEnvironment.withEvent(serverPlayer, serverPlayer.level(), event));
			}

			boolean key1 = AllKeys.SKILL_RELEASE.isPressed();
			boolean key2 = AllKeys.SKILL_RELEASE_2.isPressed();
			boolean key3 = AllKeys.SKILL_RELEASE_3.isPressed();
			if (!key1 && !key2 && !key3) return;

			ItemStack sword = player.getMainHandItem();
			SkillItemStack skillStack = SkillItemStack.of(sword);
			if (!skillStack.hasSkill(SkillType.HIT_SKILL))
				return;

			SkillsComponent holder = skillStack.getSkillsHolder();
			List<DataSkill> hitSkills = holder.getDataSkills(SkillType.HIT_SKILL);
			if (hitSkills.isEmpty()) return;

			// 键一 → 槽位 0；键二 → 槽位 1；键三 → 槽位 2（可同时按，各自释放互不影响）
			if (key1) triggerSlot(skillStack, holder, hitSkills, 0, event, sword, player);
			if (key2) triggerSlot(skillStack, holder, hitSkills, 1, event, sword, player);
			if (key3) triggerSlot(skillStack, holder, hitSkills, 2, event, sword, player);
		} finally {
			releasing = false;
		}
	}

	private static void triggerSlot(SkillItemStack skillStack, SkillsComponent holder,
									List<DataSkill> hitSkills, int slot,
									LivingIncomingDamageEvent event, ItemStack sword, Player player) {
		if (slot >= hitSkills.size()) return;
		DataSkill data = hitSkills.get(slot);

		// 冷却：优先技能自身冷却（剑双技能各自独立）；未定义时退回注册表
		int cooldownTicks = data.skill.getCooldownSeconds() > 0
				? data.skill.getCooldownSeconds() * 20
				: SkillCooldowns.getTicks(sword);
		if (!ToolSkillCooldown.isReady(player, sword))
			return;

		LivingHurtContext context = new LivingHurtContext(event);
		// 释放成功（能量预检查通过）后进入冷却；剩余能量显示统一由 ToolEnergy.tryConsume 处理
		if (holder.releaseSkillAt(skillStack, SkillType.HIT_SKILL, slot, context)) {
			ToolSkillCooldown.startTicks(player, sword, cooldownTicks);
		}
	}
}
