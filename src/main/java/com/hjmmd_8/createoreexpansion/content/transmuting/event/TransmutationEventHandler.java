package com.hjmmd_8.createoreexpansion.content.transmuting.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationFluids;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * <b>嬗变液接触玩家的“嬗乱”发作</b>（TRANS 层的特性）。
 *
 * <p><b>P3i 归位</b>：本类原先住在共享路径 {@code foundation/TransmutationEventHandler}，
 * 但内容<b>全部是 TRANS 的</b>——接触的流体是 {@code TransmutationFluids.TRANSMUTATION_FLUID}（嬗变液），
 * 施加的效果是 {@code TransmutationEffects.TRANSMUTATION_DISORDER}（嬗乱），二者都属于机械嬗变化
 * （{@code content/transmuting/**}）。共享路径只是历史巧合；分层体检按路径把它算成 SHARED，
 * 于是"TRANS 的特性借道共享层"这件事在工具里看不见。搬到 {@code content/transmuting/event/}
 * 后它才真正归 TRANS 层，方向是 TRANS → COE（读 {@code CoeItems.STELLARSTONE_STRESS_MEDALLION}，
 * 允许方向），键位 {@code @EventBusSubscriber(modid = CoeCore.MOD_ID)} 与
 * {@code MOD_ID} 的值一律未改（P3q：字段从根侧 {@code CreateOreExpansion} 的转发常量
 * 改为直接读 core 里的同一个常量）。<b>P3t</b>：本类同时把嬗变液的引用从集成层别名
 * {@code common.hub.AllFluids} 换成<b>本层自己的声明</b> {@code TransmutationFluids}
 * （同一个 {@code DeferredHolder} 对象，只是不再绕 hub——层文件不再 import 集成层），
 * 流体的判定表达式与注册时机一字未动。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class TransmutationEventHandler {

	private static final Map<UUID, Integer> FLUID_CONTACT_TICKS = new HashMap<>();

	private TransmutationEventHandler() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide)
			return;
		if (player.isSpectator())
			return;

		if (player.getFluidTypeHeight(TransmutationFluids.TRANSMUTATION_FLUID.get().getFluidType()) > 0.0D) {
			// 佩戴星辉石凝能佩时免疫嬗乱（双保险：这里直接拦截 + MobEffectEvent.Applicable 兜底）
			if (IMedallion.isWearing(player, CoeItems.STELLARSTONE_STRESS_MEDALLION.get()))
				return;
			int contactTicks = FLUID_CONTACT_TICKS.merge(player.getUUID(), 1, Integer::sum);
			int level = 1 + contactTicks / (15 * 20);
			player.addEffect(new MobEffectInstance(TransmutationEffects.TRANSMUTATION_DISORDER, 60, level - 1));
		} else {
			FLUID_CONTACT_TICKS.remove(player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof Player player)
			FLUID_CONTACT_TICKS.remove(player.getUUID());
	}

}
