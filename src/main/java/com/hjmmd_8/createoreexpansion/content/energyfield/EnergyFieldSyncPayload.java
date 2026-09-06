package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 → 客户端：某维度能量场的全量快照（护目镜场域指示用）。
 *
 * <p>场本体只存在于服务端 {@link EnergyFields}；客户端渲染（Create Outliner 指示框）
 * 需要知道场的位置/朝向/类型。每当场的集合变化（调试命令建场/清场，后续控制器方块
 * 刷新场）就把该维度全部场打包发往在线玩家；玩家登录/换维度时补发一次。</p>
 *
 * <p>载荷轻（场数量少）：每个场 = 类型名 + 区域 AABB 六分量 + 方向三分量 + 强度。</p>
 */
public record EnergyFieldSyncPayload(ResourceLocation dimension, List<FieldData> fields)
	implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<EnergyFieldSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "energy_field_sync"));

	public static final StreamCodec<FriendlyByteBuf, EnergyFieldSyncPayload> STREAM_CODEC =
		StreamCodec.of(EnergyFieldSyncPayload::write, EnergyFieldSyncPayload::read);

	/** 单个场的可序列化拷贝。source 可空（真实场 controller:… / 结构场 structure:…）；
	 * frame 可空：仅结构场携带 8 个世界角点（24 doubles），渲染端按位邻接连 12 条棱旋转框。 */
	public record FieldData(String type, double minX, double minY, double minZ,
		double maxX, double maxY, double maxZ, double dx, double dy, double dz, double strength,
		String source, double[] frame) {

		static FieldData of(EnergyField f) {
			AABB r = f.region();
			Vec3 d = f.direction();
			return new FieldData(f.type().name(), r.minX, r.minY, r.minZ,
				r.maxX, r.maxY, r.maxZ, d.x, d.y, d.z, f.strength(), f.source(), f.frame());
		}

		EnergyField toField() {
			String src = source == null || source.isEmpty() ? null : source;
			return new EnergyField(EnergyFieldType.valueOf(type),
				new AABB(minX, minY, minZ, maxX, maxY, maxZ),
				new Vec3(dx, dy, dz), strength, src, frame);
		}
	}

	/** 由服务端场注册表快照构造载荷。 */
	public static EnergyFieldSyncPayload of(ResourceLocation dimension, List<EnergyField> fields) {
		List<FieldData> data = new ArrayList<>(fields.size());
		for (EnergyField f : fields)
			data.add(FieldData.of(f));
		return new EnergyFieldSyncPayload(dimension, data);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** 客户端收包：写入镜像（enqueueWork 保证主线程执行）。 */
	@OnlyIn(Dist.CLIENT)
	public void applyToClient() {
		List<EnergyField> rebuilt = new ArrayList<>(fields.size());
		for (FieldData data : fields)
			rebuilt.add(data.toField());
		EnergyFieldClientState.receive(dimension, rebuilt);
	}

	/** 客户端收包：把整维度快照（可能为空=清场）交给镜像。 */
	@OnlyIn(Dist.CLIENT)
	public static void handle(EnergyFieldSyncPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
		context.enqueueWork(payload::applyToClient);
	}

	/** 网络注册（mod bus，见 {@link com.hjmmd_8.createoreexpansion.CreateOreExpansion#CreateOreExpansion}）。 */
	public static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
		net.neoforged.neoforge.network.registration.PayloadRegistrar registrar =
			event.registrar("1").optional();
		registrar.playToClient(TYPE, STREAM_CODEC, EnergyFieldSyncPayload::handle);
	}

	/**
	 * 服务端：把某维度全部场广播给该维度所有在线玩家。
	 * 在能量场集合变化处调用（调试命令建场/清场、后续控制器方块刷新场）。
	 */
	public static void broadcastToDimension(net.minecraft.server.level.ServerLevel level) {
		List<EnergyField> fields = EnergyFields.in(level);
		if (fields.isEmpty())
			return; // 无场：客户端镜像维持现状（清场也应在 clear 后调，但空列表无需发送——见 clear 处理）
		net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level,
			EnergyFieldSyncPayload.of(level.dimension().location(), fields));
	}

	/** 服务端：给单个玩家补发其所在维度的场（登录/换维度后客户端需要同步一次）。 */
	public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player) {
		net.minecraft.server.level.ServerLevel level = player.serverLevel();
		List<EnergyField> fields = EnergyFields.in(level);
		if (fields.isEmpty())
			return;
		net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
			EnergyFieldSyncPayload.of(level.dimension().location(), fields));
	}

	/** 服务端：广播"清场"（空列表载荷，客户端据此清空该维度镜像）。 */
	public static void broadcastClear(net.minecraft.server.level.ServerLevel level) {
		net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level,
			new EnergyFieldSyncPayload(level.dimension().location(), List.of()));
	}

	private static void write(FriendlyByteBuf buf, EnergyFieldSyncPayload payload) {
		buf.writeResourceLocation(payload.dimension());
		List<FieldData> list = payload.fields();
		buf.writeVarInt(list.size());
		for (FieldData f : list) {
			buf.writeUtf(f.type());
			buf.writeDouble(f.minX());
			buf.writeDouble(f.minY());
			buf.writeDouble(f.minZ());
			buf.writeDouble(f.maxX());
			buf.writeDouble(f.maxY());
			buf.writeDouble(f.maxZ());
			buf.writeDouble(f.dx());
			buf.writeDouble(f.dy());
			buf.writeDouble(f.dz());
			buf.writeDouble(f.strength());
			buf.writeUtf(f.source() == null ? "" : f.source());
			double[] frame = f.frame();
			buf.writeBoolean(frame != null);
			if (frame != null)
				for (double v : frame)
					buf.writeDouble(v);
		}
	}

	private static EnergyFieldSyncPayload read(FriendlyByteBuf buf) {
		ResourceLocation dim = buf.readResourceLocation();
		int size = buf.readVarInt();
		List<FieldData> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			// 严格镜像 write：type→6(min/max)→dir3→strength→source→hasFrame→frame(24)
			String type = buf.readUtf();
			double minX = buf.readDouble();
			double minY = buf.readDouble();
			double minZ = buf.readDouble();
			double maxX = buf.readDouble();
			double maxY = buf.readDouble();
			double maxZ = buf.readDouble();
			double dx = buf.readDouble();
			double dy = buf.readDouble();
			double dz = buf.readDouble();
			double strength = buf.readDouble();
			String source = buf.readUtf();
			double[] frame = null;
			if (buf.readBoolean()) {
				frame = new double[24];
				for (int k = 0; k < 24; k++)
					frame[k] = buf.readDouble();
			}
			list.add(new FieldData(type, minX, minY, minZ, maxX, maxY, maxZ,
				dx, dy, dz, strength, source, frame));
		}
		return new EnergyFieldSyncPayload(dim, list);
	}

	/**
	 * 服务端：把某维度<b>全部可见场</b>（真实世界场 + 各结构换算成世界坐标后的场）快照
	 * 广播给该维度在线玩家（客户端整表覆盖刷新）。
	 */
	public static void broadcastWorldView(net.minecraft.server.level.ServerLevel level,
		List<EnergyField> combinedWorldFields) {
		if (combinedWorldFields.isEmpty())
			return;
		net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level,
			EnergyFieldSyncPayload.of(level.dimension().location(), combinedWorldFields));
	}
}
