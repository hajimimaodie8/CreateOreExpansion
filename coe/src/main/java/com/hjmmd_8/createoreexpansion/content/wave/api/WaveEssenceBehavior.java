package com.hjmmd_8.createoreexpansion.content.wave.api;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * <b>魔素的可选"命中时效果"（2026-10-24 波系统重构批 4，D5 的保守解法）</b>。
 *
 * <p>作者对 D5（"新增魔素的命中行为归谁"）的指示是<b>选一个比较保守的方式</b>，本批的口径是：
 * <b>行为挂在注册项上、可空、默认退化为既有行为</b>——</p>
 * <ul>
 *   <li>注册时<b>不</b>给 behavior（内置 11 个全部如此）⇒ 命中<b>逐字</b>走既有的
 *       {@code WaveEssenceEffects} 八支表（水/火/冰/风/地/毒/异/雷的效果一个字节都没动）；</li>
 *   <li>注册时给了 behavior ⇒ 由它接管这一次命中（内置表不再执行），因为"这个魔素做什么"
 *       是注册方自己的知识，不该再要求它去改本模组的 {@code switch}。</li>
 * </ul>
 *
 * <p><b>调用时机</b>：只在<b>服务端</b>命中链里调用（波命中玩家 / 波命中生物两处），
 * 已经过了"主人的波不打主人"那道排除，并且{@code level} 就是命中发生的服务端世界
 * （实现方不必再写 {@code instanceof ServerLevel}）。客户端不会调用本接口。</p>
 *
 * <p><b>本批的边界</b>：只接"波命中"这一条链路。箭（弓技能的元矢）那一路仍只走内置八支表，
 * 第三方行为的箭侧接线不在本批范围（报告里单列）。</p>
 */
public interface WaveEssenceBehavior {

	/** 本魔素的波打中<b>玩家</b>（服务端；{@code player} 可能是主人以外任何人）。 */
	default void onPlayerHit(AbstractChargerWaveEntity wave, Player player, ServerLevel level) {
	}

	/** 本魔素的波打中<b>生物（非玩家）</b>（服务端）。 */
	default void onCreatureHit(AbstractChargerWaveEntity wave, LivingEntity target, ServerLevel level) {
	}
}
