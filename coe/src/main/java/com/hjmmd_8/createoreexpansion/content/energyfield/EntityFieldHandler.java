package com.hjmmd_8.createoreexpansion.content.energyfield;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * <b>「生物受能量场作用」的驱动点：每服务端 tick 把已加载维度里的生物接进那条链</b>
 * （coe-charge 批 8；需求 §3.7 的"逐 tick 生效"）。
 *
 * <h2>为什么需要它（而不是把受力挂在别处）</h2>
 * <p>受力<b>不产生任何事件</b>：生物"站在场里"既不是 tick 事件、也不是交互——
 * 它只是几何事实（{@code EnergyField#contains}）。少了这条节拍，{@link EntityFieldBridge}
 * 与那一组数值<b>永远没有调用者</b>，而静态关卡全绿、编译全绿、日志里一个错都没有
 * （本仓反复踩的"静默少一条"形状）。</p>
 *
 * <h2>★ 驱动形状：与 {@link ChargeResidueHandler} 同形（本仓刻意只留两种）</h2>
 * <ul>
 *   <li>{@code @EventBusSubscriber(modid = CoeCore.MOD_ID)} + 一个 {@code @SubscribeEvent}
 *       静态方法（<b>同一个形状</b>，不新造第三种）；</li>
 *   <li>事件是 {@link ServerTickEvent.Post}：一次拿到 {@code MinecraftServer}，
 *       {@code getAllLevels()} 遍历<b>已加载的维度</b>，再对每个维度的实体表取活着的生物。
 *       —— 与 {@code ChargeResidueHandler}（按维度推残留）以及既有
 *       {@code MedallionEffectHandler#onServerTick}（按维度扫实体）逐行同形；
 *       「每只实体一个事件」的 {@code EntityTickEvent.Post} 不在这里用：
 *       {@code ChargeContactHandler} 那条链的判据是"这只实体自己"，而受场的判据是
 *       **实体 × 它所在维度的场表**，走维度遍历才与场的作用域对齐。</li>
 *   <li><b>modid 必须是本类所在 mod 文件的 id</b>（{@code CoeCore.MOD_ID}）：住在
 *       {@code :coe}（第一层，永远在场）；写错 id 会<b>静默不注入</b>（无警告、无报错、
 *       编译全绿），本仓对两种形态都有断言。</li>
 * </ul>
 *
 * <h2>本类只做一件事</h2>
 * <p>转交：{@code entity instanceof LivingEntity} ⇒ {@link EntityFieldBridge#tick(LivingEntity)}。
 * <b>判据一律在门面里</b>（带电 / 水里 / 飞行 / 限幅），本类不带任何数值、不碰任何效果、
 * 不写日志（每 tick 每生物打一行会刷屏；既有同类驱动 {@code ChargeContactHandler} /
 * {@code MedallionEffectHandler} 也都不打），免得"谁该受力"出现第二处口径。</p>
 *
 * <h2>性能</h2>
 * <p>一次服务端 tick 遍历全部已加载维度的实体表，每只实体一次 {@code instanceof}；
 * 只有活着的生物才进门面，而门面的第一道真判据是"带不带电"（两次 {@code HashMap} 命中）
 * ⇒ 常态开销与实体总数同阶，与 {@code MedallionEffectHandler} 已有的那次全表扫描同一量级。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class EntityFieldHandler {

	private EntityFieldHandler() {
	}

	/**
	 * 每个服务端 tick、每个已加载维度一次：把该维度里活着的生物交给受场链
	 * （{@code Post}：与既有 {@code ArmorSkillHandler} / {@code MedallionEffectHandler} /
	 * {@code ChargeResidueHandler} 同一档位）。
	 *
	 * @param event 服务端 tick 事件
	 */
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		for (ServerLevel level : event.getServer().getAllLevels()) {
			for (Entity entity : level.getEntities().getAll()) {
				if (entity instanceof LivingEntity living) {
					EntityFieldBridge.tick(living);
				}
			}
		}
	}
}
