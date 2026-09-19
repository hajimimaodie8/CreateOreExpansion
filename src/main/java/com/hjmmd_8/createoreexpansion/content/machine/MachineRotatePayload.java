package com.hjmmd_8.createoreexpansion.content.machine;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：<b>Ctrl + 扳手右键 = 旋转本模组机器</b>（用户 2026-09-15 定稿的交互规则第 4 条）。
 *
 * <p><b>为什么需要这个包</b>：Minecraft 的"使用物品于方块"包不带修饰键信息，而 Ctrl 又不是会同步的
 * 玩家状态（只有潜行同步）——服务器<b>无从判断</b>玩家是否按着 Ctrl。于是只能在客户端判定 Ctrl，
 * 命中时取消本地交互并发这个包，由服务器执行旋转。这样"没按 Ctrl，扳手绝不旋转"是<u>服务端权威</u>的。</p>
 *
 * <p><b>服务端校验（缺一不可）</b>：① 方块确实实现 {@link CewsMachine}（本模组机器）；
 * ② 该机器<b>没有</b>特殊切换模式（有模式的机器扳手只切模式，不旋转）；③ 玩家手持 Create 扳手；
 * ④ 在有效交互距离内。任一不满足即静默丢弃该包。</p>
 *
 * <p>旋转本身调 {@link CewsMachine#rotateAsCreate} → 即 {@code IWrenchable} 的默认实现，
 * 与 Create 常规旋转完全同源（不复制逻辑、不自定义角度）。</p>
 */
public record MachineRotatePayload(BlockPos pos, Direction face, Vec3 hitLocation) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MachineRotatePayload> TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "machine_rotate"));

	public static final StreamCodec<FriendlyByteBuf, MachineRotatePayload> STREAM_CODEC =
		StreamCodec.of(MachineRotatePayload::write, MachineRotatePayload::read);

	private static void write(FriendlyByteBuf buf, MachineRotatePayload payload) {
		buf.writeBlockPos(payload.pos);
		buf.writeVarInt(payload.face.get3DDataValue());
		buf.writeDouble(payload.hitLocation.x);
		buf.writeDouble(payload.hitLocation.y);
		buf.writeDouble(payload.hitLocation.z);
	}

	private static MachineRotatePayload read(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		Direction face = Direction.from3DDataValue(buf.readVarInt());
		return new MachineRotatePayload(pos, face, new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** 服务端收包：校验 + 旋转（enqueueWork 保证主线程执行）。 */
	public static void handle(MachineRotatePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			Level level = player.level();
			BlockPos pos = payload.pos();
			if (!level.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0d)
				return; // 距离/加载校验（正常交互距离内；超出视为无效包）
			if (!player.mayBuild())
				return;
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CewsMachine machine))
				return; // 不是本模组机器
			if (machine.hasModeSwitch())
				return; // 有模式的机器：扳手只切模式，不参与旋转
			if (!player.getItemInHand(InteractionHand.MAIN_HAND)
				.isEmpty() && !(player.getItemInHand(InteractionHand.MAIN_HAND)
					.getItem() instanceof com.simibubi.create.content.equipment.wrench.WrenchItem))
				return; // 手上不是扳手（客户端只会用扳手发这个包，这里是服务端兜底）
			BlockHitResult hit = new BlockHitResult(payload.hitLocation(), payload.face(), pos, false);
			machine.rotateAsCreate(state, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		});
	}

	/** 网络注册（mod bus，见 {@code CreateOreExpansion} 构造器）。 */
	public static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
		net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar("1").optional();
		registrar.playToServer(TYPE, STREAM_CODEC, MachineRotatePayload::handle);
	}
}
