package com.hjmmd_8.createoreexpansion.content.energyfield;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * <b>电荷残留的驱动点：每服务端 tick 把每个维度的残留推一格</b>
 * （coe-charge 批 7；需求 §3.5 #6~#9 的「计时 / 稀疏粒子 / 接触传染」三件事都要一个节拍）。
 *
 * <h2>为什么需要它（而不是把残留挂在实体或方块上）</h2>
 * <p>本批的残留是<b>纯数据</b>（{@link ChargeResidues} 的类注释写了为什么不用实体）：
 * 没有实体就没有 {@code EntityTickEvent} 来推它，所以必须有一条自己的节拍。
 * 少了这一条，残留会「生成出来、永远不消失、也永远不给电」—— 而且<b>一点都不报错</b>：
 * 静态关卡全绿、日志里那句「留下残留」照打（本仓反复踩的静默少一条形状）。</p>
 *
 * <h2>为什么是 {@code ServerTickEvent.Post}（不是逐实体 tick、也不是逐维度事件）</h2>
 * <ul>
 *   <li><b>不是 {@code EntityTickEvent.Post}</b>（{@code ChargeContactHandler} 那条）：
 *       残留是<b>按维度</b>记账的区域，逐实体会让「这个维度有没有残留」被问 N 遍，
 *       而且没有实体的区块里的残留就永远不老化；</li>
 *   <li><b>一次拿到全部维度</b>：{@code ServerTickEvent.Post} 给的是
 *       {@code MinecraftServer}，{@code getAllLevels()} 一次遍历即可；
 *       {@code ChargeResidues.tick} 对「从来没有过残留的维度」只花一次可空查表
 *       （{@code ChargeResidueData.find} 返回 null），所以空世界的开销是每维度一次哈希查表。</li>
 * </ul>
 *
 * <h2>modid 与位置</h2>
 * <p>住在 {@code :coe}（第一层，永远在场），{@code @EventBusSubscriber} 的 {@code modid}
 * 必须是本类所在 mod 文件的 id（{@code CoeCore.MOD_ID}）—— 这条错配会<b>静默不注入</b>
 * （无警告、无报错、编译全绿），关卡里对两种形态都有断言。服务端权威：残留的寿命、粒子与
 * 染电全在服务端结算，客户端不参与（{@code ServerTickEvent} 本身只在服务端派发）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class ChargeResidueHandler {

	private ChargeResidueHandler() {
	}

	/**
	 * 每个服务端 tick、每个维度一次：把该维度的残留推一格
	 * （先删到期的，再对留下的发稀疏粒子、查接触传染）。
	 *
	 * @param event 服务端 tick 事件（{@code Post}：与既有 {@code ArmorSkillHandler} /
	 *              {@code MedallionEffectHandler} / {@code EnergyFieldCommandRegistration}
	 *              同一档位）
	 */
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		for (ServerLevel level : event.getServer().getAllLevels()) {
			ChargeResidues.tick(level);
		}
	}
}
