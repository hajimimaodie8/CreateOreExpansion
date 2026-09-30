package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

/**
 * 右键类技能（USE 族：耕作等）的触发点。
 *
 * <p><b>⚠ 不要因为「没有代码引用」就删掉本类</b>（2026-09-30 真实教训）：
 * 本类只靠 {@link EventBusSubscriber} 被事件总线调用，静态探针看不到调用者；
 * 我在换核第 2 阶段把它当死代码删除，导致<b>锄头耕作等右键技能不再触发</b>，
 * 而所有静态关卡照旧绿灯。判定入口类存活要看「@EventBusSubscriber 清单」。</p>
 *
 * <p>职责（换核后只剩一条路径）：右键物品 / 对方块使用物品时，把事件交给
 * Skiller 新内核的 USE 族技能执行（耗能 / 冷却 / 效果都在技能实现里）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public class UseItemHandler {

	@SubscribeEvent
	public static void onUseOnBlock(UseItemOnBlockEvent event) {
		if (event.getPlayer() == null)
			return;
		// UseItemOnBlockEvent 会按交互阶段触发多次（ITEM_BEFORE_BLOCK/BLOCK/ITEM_AFTER_BLOCK），
		// 技能只应在第一次（物品交互前）释放一次，否则一次右键会重复扣费。
		if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK)
			return;
		release(event.getPlayer(), event.getItemStack(), event);
	}

	@SubscribeEvent
	public static void onUse(PlayerInteractEvent.RightClickItem event) {
		release(event.getEntity(), event.getItemStack(), event);
	}

	private static void release(Player player, ItemStack stack, net.neoforged.bus.api.Event triggerEvent) {
		if (player.level().isClientSide())
			return;
		// 弓类武器技能由弓自身在松手射击（releaseUsing）时释放：右键拉弓瞬间不触发，
		// 否则会在蓄力开始时就消耗能量/进入冷却（USE 触发时机不匹配）。
		if (stack.getItem() instanceof JadeTopazBowItem)
			return;
		SkillItemStack skillStack = SkillItemStack.of(stack);
		SkillsComponent holder = skillStack.getSkillsHolder();
		// 非技能物品（无 SKILLS 组件）直接忽略，避免 NPE 干扰原版交互（如放置方块）
		if (holder == null || holder.getDataSkills(SkillType.USE_SKILL).isEmpty())
			return;

		// 新内核（Skiller）路径：右键技能由它执行。
		// 按键槽位由内核按服务端权威按键状态自己挑（旧 AllKeys 是纯客户端对象，
		// 专用服务器上恒为"未按下"，不能用来判定）。
		if (player instanceof ServerPlayer serverPlayer) {
			CoeSkillRelease.release(serverPlayer, CoeSkillTypes.USE,
					SkillContextEnvironment.withEvent(serverPlayer, serverPlayer.level(), triggerEvent));
		}
	}
}
