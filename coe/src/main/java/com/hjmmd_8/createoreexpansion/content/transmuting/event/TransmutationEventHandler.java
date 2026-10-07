package com.hjmmd_8.createoreexpansion.content.transmuting.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationFluids;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * <b>嬗变液接触玩家的“嬗乱”发作</b>（嬗化线的特性；W6-b2 起随嬗化整块住第一层 {@code :coe}）。
 *
 * <p><b>P3i 归位</b>：本类原先住在共享路径 {@code foundation/TransmutationEventHandler}，
 * 但内容<b>全部是 TRANS 的</b>——接触的流体是 {@code TransmutationFluids.TRANSMUTATION_FLUID}（嬗变液），
 * 施加的效果是 {@code TransmutationEffects.TRANSMUTATION_DISORDER}（嬗乱），二者都属于机械嬗变化
 * （{@code content/transmuting/**}）。共享路径只是历史巧合；分层体检按路径把它算成 SHARED，
 * 于是"TRANS 的特性借道共享层"这件事在工具里看不见。搬到 {@code content/transmuting/event/}
 * 后它才真正归 TRANS 层，方向是 TRANS → COE（读 {@code CoeItems.STELLARSTONE_STRESS_MEDALLION}，
 * 允许方向）。<b>P3t</b>：本类同时把嬗变液的引用从集成层别名
 * {@code common.hub.AllFluids} 换成<b>本层自己的声明</b> {@code TransmutationFluids}
 * （同一个 {@code DeferredHolder} 对象，只是不再绕 hub——层文件不再 import 集成层），
 * 流体的判定表达式与注册时机一字未动。</p>
 *
 * <p><b>W6-b2：本类随嬗化整块搬进 {@code :coe}，所以 {@code modid} 又改回 {@code CoeCore.MOD_ID}。</b>
 * 这是本类第二次因"文件归属变了"而改这一行，判据始终是同一条
 * （FML 的 {@code AutomaticEventSubscriber.inject} 是<b>按 mod 文件</b>作用域的：
 * 它只用<b>该文件</b>的扫描结果，并且只挂 {@code Objects.equals(mod.getModId(), modid)} 的类，
 * 见 {@code build/patch/fml-src/.../AutomaticEventSubscriber.java:51-52}）：
 * <b>类的 modid 必须等于它所在那个 mod 文件的 id</b>。</p>
 *
 * <p>本类现在住 {@code coe/src/main/java/**} ⇒ 属于 {@code createoreexpansion} 这个 mod 文件
 * （{@code :coe} 的 mod id 就是 {@code createoreexpansion}，见 {@code CreateOreExpansion.MOD_ID}），
 * 于是注解读 {@link CoeCore#MOD_ID}。若仍写 {@code transmutation}（上一轮的取值），
 * 症状照旧是<b>静默失效</b>：{@code createoreexpansion} 容器因 modid 不匹配跳过本类，
 * 而 {@code transmutation} 容器根本看不到本类的扫描数据（它现在也不在自己那个 jar 里）——
 * 无警告、无报错、编译全绿，只是"碰到嬗变液不再得嬗乱"。</p>
 *
 * <p><b>同时必须删掉那条 {@code import ...TransmutationMod}（编译期硬错，不是风格问题）</b>：
 * 空壳 {@code TransmutationMod} 留在 {@code :transmutation}，而 {@code :coe} 对
 * {@code :transmutation} <b>没有任何依赖</b>（反向依赖是禁止方向），所以那个类型在 {@code :coe}
 * 的编译面上根本不存在。</p>
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
			int level = 1 + contactTicks / (15 * ChargeConfigs.TICKS_PER_SECOND);
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
