package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.machine.CewsMachine;
import com.hjmmd_8.createoreexpansion.content.machine.MachineRotatePayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
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
 * <p><b>只拦本模组机器</b>：{@code block instanceof CewsMachine} 且该机器<b>没有</b>特殊切换模式
 * （有模式的机器扳手只切模式，不旋转）。其它方块（Create 自己的机器、原版方块）一律放行，
 * 保持 Create/原版手感不变。</p>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID, value = Dist.CLIENT)
public final class MachineRotateClient {

	private MachineRotateClient() {}

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
			.getBlock() instanceof CewsMachine machine))
			return;
		if (machine.hasModeSwitch())
			return; // 有模式的机器：扳手只切模式
		BlockHitResult hit = event.getHitVec();
		event.setCanceled(true);
		net.neoforged.neoforge.network.PacketDistributor.sendToServer(
			new MachineRotatePayload(event.getPos(), hit.getDirection(), hit.getLocation()));
	}
}
