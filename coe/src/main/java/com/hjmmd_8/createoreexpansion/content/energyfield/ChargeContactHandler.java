package com.hjmmd_8.createoreexpansion.content.energyfield;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * <b>电荷中和爆炸的触发点②：两个带电极性相反的生物<b>接触</b>（碰撞箱相交）</b>
 * （coe-charge 批 6；需求 §3.5 的「触发」+ §5.1 #6 的「两实体接触检测」）。
 *
 * <h2>为什么这个类必须存在（不只是一个"补充触发点"）</h2>
 * <p>需求 §3.5 把中和的触发写成<b>几何事实</b>：「一个着正电的实体与一个着负电的实体
 * <b>接触</b>（hitbox 相交）」，还专门注明「一个实体自身不可能同时带两种 ⇒
 * <b>必然是"两个实体相遇"</b>」。而<b>接触不产生任何事件</b>：另外几条获得途径都是
 * 「某件事发生 ⇒ 调 {@link ChargeApi#apply}」，两个带电生物贴在一起却什么都没发生。
 * ⇒ 只在 {@code apply} 的异极分支里落地中和的话，作者要的那一幕（正电生物撞上负电生物
 * ⇒ 爆炸）<b>永远不会发生</b>，而且没有任何报错、没有日志、静态关卡全绿 ——
 * 正是本仓反复踩的「静默少一条」形状。</p>
 *
 * <h2>为什么是本类（而不是把检查塞进别的处理器）</h2>
 * <ul>
 *   <li><b>电荷自己的订阅者</b>：形状照批 5 的两个途径处理器
 *       （{@code ThunderiteHitChargeHandler} / {@code TeslaCoilLivingCharger}）——
 *       一个 {@code @EventBusSubscriber(modid = CoeCore.MOD_ID)} + 一个 {@code @SubscribeEvent}
 *       静态方法，住在 {@code :coe}（第一层，永远在场）；</li>
 *   <li><b>不塞进 {@code ChargeEffectRemovalHandler}</b>：那个类被关卡 §36 钉死
 *       「{@code @SubscribeEvent} 恰好一个、只订 {@code MobEffectEvent.Remove}」，
 *       多订一个事件会让那道「挡牛奶」的守卫失去可判性；</li>
 *   <li><b>不塞进 {@code ChargeApi}</b>：门面是对外承诺的五条静态方法，
 *       让它变成事件订阅者会引入 {@code net.neoforged} 的 import，而 §38 的
 *       {@code charge-api-no-optional-import} 只允许「本仓 + {@code net.minecraft.} +
 *       {@code java.}」三根白名单 —— 那条白名单是<b>对外承诺</b>的一部分，不放宽。</li>
 * </ul>
 *
 * <h2>本类只做三件事</h2>
 * <ol>
 *   <li><b>逐实体 tick</b>（{@link EntityTickEvent.Post}）：每个生物每 tick 一次；</li>
 *   <li><b>两道最便宜的过滤</b>：不是活着的生物 / 客户端 ⇒ 直接返回（服务端权威）；</li>
 *   <li><b>交给门面</b> {@link ChargeApi#checkContact}：那里才问「这个实体带不带电」、
 *       「在不在中和冷却里」、以及「碰撞箱里有没有相反极性的一方」。
 *       ⚠ 判据一律在门面里：本类不带任何数值、不碰任何效果，免得「谁是带电的」出现第二处口径。</li>
 * </ol>
 *
 * <h2>性能</h2>
 * <p>每次调用只有两条 {@code hasEffect} 查询（{@code HashMap} 命中）；只有<b>真的带电</b>
 * 的实体才会走到 {@code getEntitiesOfClass} 的碰撞箱扫描，而带电实体需要雷击 / 通电线圈 /
 * 带电波命中才会出现 ⇒ 常态下本类的开销可以忽略。冷却记账（双方各一笔）保证
 * 「同一对贴身」不会每 tick 重复爆炸。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class ChargeContactHandler {

	private ChargeContactHandler() {
	}

	/**
	 * 每 tick 的入口：活着的、服务端的生物 ⇒ 问一次门面「这一刻它要不要中和」。
	 *
	 * @param event 实体 tick 事件（{@code Post}：与既有 {@code TeslaCoilLivingCharger} 同一档位）
	 */
	@SubscribeEvent
	public static void onEntityTick(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof LivingEntity living) || !living.isAlive()) {
			return;
		}
		if (living.level().isClientSide) {
			return; // 服务端权威（伤害 / 移除效果 / 粒子都在服务端结算）
		}
		ChargeApi.checkContact(living);
	}
}
