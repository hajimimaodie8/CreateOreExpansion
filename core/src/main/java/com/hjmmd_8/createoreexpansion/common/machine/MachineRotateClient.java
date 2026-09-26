package com.hjmmd_8.createoreexpansion.common.machine;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * <b>Ctrl + 扳手右键 = 旋转本模组机器</b>的客户端拦截（交互规则第 4 条的客户端半边，服务端半边见
 * {@link MachineRotatePayload}）。
 *
 * <p><b>为什么必须在这里做</b>：Ctrl 不是会同步的玩家状态（潜行才会同步），服务器读不到；
 * 只能由客户端判定 Ctrl，命中时<b>取消本地方块交互</b>（于是不会发出普通的"使用物品于方块"包），
 * 改为发送 {@link MachineRotatePayload}，由服务器校验并旋转。这样既实现了 Ctrl 语义，
 * 又保证"没按 Ctrl 时扳手不旋转"是服务端权威行为。</p>
 *
 * <p><b>只拦本模组机器</b>：{@code block instanceof MachineInteraction} 且该机器<b>没有</b>特殊切换模式
 * （有模式的机器扳手只切模式，不旋转）。其它方块（Create 自己的机器、原版方块）一律放行，
 * 保持 Create/原版手感不变。</p>
 *
 * <h2>P7a：为什么本类从 {@code client/} 搬进 core，并且<b>没有</b> {@code @EventBusSubscriber}</h2>
 * <p>它只引用 core 自己的 {@link MachineInteraction} / {@link MachineRotatePayload}，本来是"零层引用"
 * 的共享件；但 {@code @EventBusSubscriber} 的注入判据是"类的 modid == 它所在 mod 文件的 id"，
 * 而 core 在<b>发布形态里没有 ModContainer</b>（{@code FMLModType: GAMELIBRARY} ⇒
 * {@code DefaultModFileInfo.getMods()} 恒空），注解在库里永远不会被处理。</p>
 *
 * <p>所以改成"逻辑住 core + 由客户端专属的薄入口显式调 {@link #install()}"：
 * 内容模块里任何<b>客户端</b>{@code @EventBusSubscriber}（{@code Dist.CLIENT}）在其
 * {@code FMLClientSetupEvent} 里调一次即可，幂等。</p>
 *
 * <p><b>为什么不放在各层 {@code @Mod} 构造器里调</b>：本类的方法体直接引用
 * {@code net.minecraft.client.Minecraft} / {@code InputConstants}，专用服务器上不该被加载。
 * 放在 {@code @Mod} 构造器（两个 Dist 都跑）里就得先判 Dist，而"类已被加载"这件事本身在
 * 客户端类被剥离的环境里有风险；挂在 {@code Dist.CLIENT} 的订阅类里则天然只在客户端执行。</p>
 *
 * <p><b>注册到哪条总线</b>：{@code PlayerInteractEvent.RightClickBlock} 是 game 总线事件，
 * 所以 {@link #install()} 挂 {@code NeoForge.EVENT_BUS}
 * （== {@code FMLLoader.getBindings().getGameBus()}，与 {@code AutomaticEventSubscriber}
 * 给无 modid 订阅类用的那条是同一个实例）。</p>
 */
public final class MachineRotateClient {

	private MachineRotateClient() {}

	/** 幂等标志：多个模块的客户端入口都会调 {@link #install()}，只允许真正挂一次。 */
	private static boolean installed;

	/**
	 * 把本类挂到 game 总线（幂等）。<b>只能由客户端专属代码调用</b>（见类注释）。
	 */
	public static void install() {
		if (installed) {
			return;
		}
		installed = true;
		NeoForge.EVENT_BUS.register(MachineRotateClient.class);
		CoeCore.LOGGER.debug("[P7a] Ctrl+扳手旋转的客户端拦截已挂到 game 总线");
	}

	/** Ctrl 是否按下（左右任一）。输入事件里用 {@code InputConstants} 查物理按键状态。 */
	private static boolean ctrlDown() {
		long window = Minecraft.getInstance()
			.getWindow()
			.getWindow();
		return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
			|| InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
	}

	/**
	 * 命中判定与发包（在 {@link PlayerInteractEvent.RightClickBlock} 的客户端阶段执行）。
	 *
	 * <p>用事件而不是"覆写某个 Item"：扳手是 Create 的物品，本模组不可能改它的类；
	 * 用交互事件能在不碰 Create 的前提下加修饰键语义（这也与"可选模组一律隔离"的既有做法一致）。</p>
	 */
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!event.getLevel().isClientSide)
			return; // 服务端半边在 MachineRotatePayload#handle
		if (!ctrlDown())
			return;
		ItemStack stack = event.getItemStack();
		if (stack.isEmpty() || !(stack.getItem() instanceof com.simibubi.create.content.equipment.wrench.WrenchItem))
			return;
		if (!(event.getLevel()
			.getBlockState(event.getPos())
			.getBlock() instanceof MachineInteraction machine))
			return;
		if (machine.hasModeSwitch())
			return; // 有模式的机器：扳手只切模式
		BlockHitResult hit = event.getHitVec();
		event.setCanceled(true);
		net.neoforged.neoforge.network.PacketDistributor.sendToServer(
			new MachineRotatePayload(event.getPos(), hit.getDirection(), hit.getLocation()));
	}
}
