package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 能量场调试命令注册（临时；控制器方块完成前用于验证波在场中的加速/偏转）。
 *
 * <p>场的可视化已从"服务端粒子线框"升级为<b>客户端 Create 风格指示框</b>
 * （携带工程师护目镜时显示，见 {@link EnergyFieldGoggleOutlineRenderer}），
 * 数据经 {@link EnergyFieldSyncPayload} 从服务端同步。故本类只负责命令注册与
 * 登录/换维度时的补发同步。</p>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class EnergyFieldCommandRegistration {

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher()
			.register(EnergyFieldDebugCommands.register());
	}

	/** 玩家登录：补发其所在维度的场快照（护目镜渲染的数据源）。 */
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			EnergyFieldSyncPayload.sendToPlayer(player);
	}

	/** 玩家换维度：补发新维度的场快照（客户端镜像按维度 key 区分，不冲突）。 */
	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			EnergyFieldSyncPayload.sendToPlayer(player);
	}

	/** 维度加载：把该维度存档中的场恢复进内存表（退出重进后场仍在）。 */
	@SubscribeEvent
	public static void onLevelLoad(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
		if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			EnergyFields.restore(serverLevel);
		}
	}

	/** 每维度"上次是否发过非空场快照"：从有→无时补发一次空快照，客户端据此清掉残留指示框。 */
	private static final java.util.Map<net.minecraft.resources.ResourceLocation, Boolean> HAD_FIELDS =
		new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * 结构场世界同步（每 2 tick）：把各 Sable 结构上的场换算成世界坐标视图，
	 * 与真实世界场合并后广播给该维度玩家 —— 护目镜场域指示随结构移动实时跟随。
	 * 结构/机器拆净后由"有场 → 无场"翻转补发<b>空快照</b>清掉客户端残留框；
	 * 失效结构（换算抛异常 = 已被移除）从表里兜底清除。
	 */
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		net.minecraft.server.MinecraftServer server = event.getServer();
		if ((server.getTickCount() & 1) != 0)
			return;
		SubLevelBridge bridge = SableBridges.get();
		if (bridge == null || !bridge.isActive())
			return;
		Map<ServerLevel, List<EnergyField>> perLevel = new HashMap<>();
		for (Object host : EnergyFields.structureHosts()) {
			SubLevelBridge.Hit hit = new SubLevelBridge.Hit(host);
			try {
				Level lvl = bridge.worldLevel(hit);
				if (!(lvl instanceof ServerLevel sl) || sl.isClientSide)
					continue;
				perLevel.computeIfAbsent(sl, k -> new ArrayList<>())
					.addAll(EnergyFields.structureWorldView(bridge, hit));
			} catch (Throwable t) {
				// 结构已被移除（位姿/世界访问失效）：清掉其残留场，避免幽灵框
				EnergyFields.clearSub(host);
			}
		}
		for (Map.Entry<ServerLevel, List<EnergyField>> e : perLevel.entrySet()) {
			ServerLevel sl = e.getKey();
			List<EnergyField> combined = new ArrayList<>(EnergyFields.in(sl)); // 真实场一并携带（镜像整表覆盖）
			combined.addAll(e.getValue());
			net.minecraft.resources.ResourceLocation dim = sl.dimension().location();
			if (!combined.isEmpty()) {
				EnergyFieldSyncPayload.broadcastWorldView(sl, combined);
				HAD_FIELDS.put(dim, true);
			} else if (HAD_FIELDS.put(dim, false) == Boolean.TRUE) {
				// 从"有场"变"无场"：补发空快照清客户端残留
				EnergyFieldSyncPayload.broadcastClear(sl);
			}
		}
		// 兜底：本 tick 没有任何结构场 entry 的维度（如结构场已被机器/结构清理干净），
		// 若此前发过场而现在真实场也为空 → 补发空快照，杜绝客户端"幽灵指示框"残留。
		for (ServerLevel sl : server.getAllLevels()) {
			net.minecraft.resources.ResourceLocation dim = sl.dimension().location();
			if (!HAD_FIELDS.getOrDefault(dim, false))
				continue;
			List<EnergyField> stillThere = new ArrayList<>(EnergyFields.in(sl));
			List<EnergyField> structs = perLevel.get(sl);
			if (structs != null)
				stillThere.addAll(structs);
			if (stillThere.isEmpty()) {
				HAD_FIELDS.put(dim, false);
				EnergyFieldSyncPayload.broadcastClear(sl);
			}
		}
	}

	private EnergyFieldCommandRegistration() {
	}
}
