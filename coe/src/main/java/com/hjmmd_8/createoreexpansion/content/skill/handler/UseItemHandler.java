package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.BoomerangItem;
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
 *
 * <p>⚠ <b>两类物品在这里被豁免</b>（两处 {@code instanceof} 都在 {@link #release} 里，
 * 各自写明理由）：弓（{@code JadeTopazBowItem}，它在松手射击时自己释放）与
 * <b>回旋镖</b>（{@link BoomerangItem}，2026-10-02 批 3 裁定 D11：它的穿刺效果在镖的命中判定里，
 * 右键那一刻再释放一次＝投一次扣两次能量）。</p>
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
		// ⚠ 回旋镖必须同样豁免（2026-10-02 批 3，裁定 D11）——**为什么必须**：
		// 四把镖绑的是 createoreexpansion:pierce，类型是 USE_SKILL（由 AllSkills 登记，
		// 因为它是"投掷时生效"的主动技能）。而本方法就挂在
		// PlayerInteractEvent.RightClickItem / UseItemOnBlockEvent 上 ⇒ 不豁免的话，
		// **右键投掷的同一瞬间**会再走一次内核释放：`ToolEnergy` 被第二次扣款
		// （投掷费已在 BoomerangItem#releaseUsing 里按"模式消耗 + 20L"合计扣过一次），
		// 也就是"投一次扣两次能量"。穿刺的效果本身不靠这里 —— 它在**镖飞出去以后**的
		// 命中判定里（AbstractBoomerangEntity#onHitEntity/onHitBlock），与右键那一刻无关。
		// 判据按**类型**（BoomerangItem），不按物品 id：四把镖共用一个类，按 id 硬编码
		// 会在加法宝时静默漏掉那一条（coe-ench 那轮的同类坑）。
		// 与弓的区别：弓的技能真的需要"释放"这个动作（松手时自己调），镖连释放都不需要。
		if (stack.getItem() instanceof BoomerangItem)
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
