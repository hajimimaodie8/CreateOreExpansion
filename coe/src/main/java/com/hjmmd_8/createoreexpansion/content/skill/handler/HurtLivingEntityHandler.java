package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 受击技能（HIT 族：剥取 / 夺取）的触发点。
 *
 * <p><b>⚠ 不要因为「没有代码引用」就删掉本类</b>（2026-09-30 真实教训）：
 * 本类只靠 {@link EventBusSubscriber} 被事件总线调用，静态探针（按类名找引用）
 * 永远看不到调用者 —— 我曾在换核第 2 阶段把它当死代码删除，导致
 * <b>剥取 / 夺取两个技能在游戏里彻底不触发</b>，而 compileJava / runData /
 * 分层 / 包重叠 / 92 条自足性<b>全部照旧绿灯</b>。这类"入口类"必须靠
 * 「@EventBusSubscriber 清单」而不是「引用计数」来判定存活。</p>
 *
 * <p>职责（换核后只剩一条路径）：玩家直接造成伤害时，把该次伤害事件交给
 * Skiller 新内核的 HIT 族技能执行（耗能 / 冷却 / 效果都在技能实现里）。</p>
 *
 * <p>防重入：技能造成的后续伤害（如夺取的吸血）不得再次触发技能，
 * 否则同一 tick 内会递归释放直至能量耗尽。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public class HurtLivingEntityHandler {

	/** 防重入：技能造成的后续伤害不得再次触发技能。 */
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
			// 新内核（Skiller）路径：受击技能由它执行。
			// 按键槽位由内核按服务端权威按键状态自己挑（旧 AllKeys 是纯客户端对象，
			// 专用服务器上恒为"未按下"，不能用来判定）。
			if (player instanceof ServerPlayer serverPlayer) {
				CoeSkillRelease.release(serverPlayer, CoeSkillTypes.HIT,
						SkillContextEnvironment.withEvent(serverPlayer, serverPlayer.level(), event));
			}
		} finally {
			releasing = false;
		}
	}
}
