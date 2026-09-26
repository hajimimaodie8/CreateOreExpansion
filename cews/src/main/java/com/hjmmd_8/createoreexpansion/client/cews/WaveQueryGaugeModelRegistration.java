package com.hjmmd_8.createoreexpansion.client.cews;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsItems;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsMod;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 波情查询仪的"查询中"item property：使默认模型在冷却期内被 overrides 换成动画模型。
 *
 * <p>贴图本身是 8 帧竖排动画条，默认模型用的是静态第 0 帧；查询后物品进入冷却
 * （服务端 {@code addCooldown} → 原版 {@code ServerItemCooldowns} 同步给客户端），
 * 客户端渲染时谓词返回 1，于是切到 layer0 = 动画贴图的模型，动画正好播一个完整循环。</p>
 *
 * <p><b>谓词只在客户端跑</b>（渲染期每帧调用）：只读原版客户端也有的信息
 * （玩家冷却表 {@code Player#getCooldowns}），<b>不碰任何服务端专属状态</b>，
 * 也不收发任何网络包。原版 {@code ItemOverrides.resolve} 是渲染期现查
 * {@code ItemProperties#getProperty}，所以在本事件里注册一定来得及。</p>
 *
 * <p><b>P7a</b>：本类从根工程 {@code client/} 搬进 {@code :cews}（它只服务
 * {@link CewsItems#WAVE_QUERY_GAUGE}），包名随之改为 {@code client.cews}。
 * 注解的 {@code modid} 也从 {@code CoeCore.MOD_ID}（= {@code createoreexpansion}）改成
 * {@link CewsMod#MOD_ID}（= {@code cews}）—— 这正是红线里"<b>类的 modid 必须等于它所在 mod
 * 文件的 id</b>"那条：搬进 {@code :cews} 却继续标 {@code createoreexpansion} 就是"镜像形态"
 * 错配，FML 会静默不注入（无警告、无报错、编译全绿）。</p>
 */
@EventBusSubscriber(modid = CewsMod.MOD_ID, value = Dist.CLIENT)
public class WaveQueryGaugeModelRegistration {

	/**
	 * item property 名。模型的 overrides 谓词键要写<b>全名</b>
	 * （{@code "createoreexpansion:scanning"}）——原版 {@code ItemOverride.Deserializer}
	 * 用 {@code ResourceLocation.parse} 解析谓词键，不带命名空间会落到 {@code minecraft:}。
	 * <b>命名空间恒为注册命名空间</b>（{@link CoeCore#REGISTRY_NAMESPACE}），与 mod id 分家无关。
	 */
	public static final ResourceLocation SCANNING = ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE,
		"scanning");

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> ItemProperties.register(CewsItems.WAVE_QUERY_GAUGE.get(), SCANNING,
			(stack, level, entity, seed) -> entity instanceof Player player
				&& player.getCooldowns()
					.isOnCooldown(stack.getItem()) ? 1.0F : 0.0F));
	}
}
