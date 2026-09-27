package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 *
 * <p><b>P3z：modid 改成 {@code CewsMod.MOD_ID}</b>（镜像形态的错配修复）。
 * 本类随 CEWS 搬进 Gradle 子模块 {@code :cews}，于是它住在 <b>cews 那个 mod 文件</b>里，
 * 而 FML 的 {@code @EventBusSubscriber} 自动注入是"按 mod 文件"作用域的：
 * {@code FMLModContainer.constructMod()} 只喂本文件的扫描结果、并只挂
 * {@code mod.getModId() == modid} 的类。原先这里写的是 {@code CoeCore.MOD_ID}
 * （= createoreexpansion），搬完之后既不会被 cews 容器注入（id 不等）、
 * 也不在根文件的扫描数据里（{@code IntegrationBootstrap} 的补挂遍历够不着）
 * —— 会<b>静默失效</b>、无任何警告。改标自己文件的 id 后由 FML 直接注入，dev 与生产同一路径。
 * 本类只订阅 game 总线事件（命令注册 / 玩家登录），modid 只决定"哪个容器的总线"，语义不变。</p>
 *
 * <p><b>W6-a：modid 由 {@code CewsMod.MOD_ID} 改成字面量 {@code "cews"}</b>。
 * {@code CewsMod.MOD_ID} 是另一个类里的编译期常量（javac 直接内联，运行期逐字等价），
 * 但它让本类在源码上引用了第二层的 {@code @Mod} 入口类
 * （{@code common.registry.cews.CewsMod}），而本类属第一层
 * （{@code content/energyfield/**}，W6-c 搬进 {@code :coe}）⇒ 一条 {@code L1 → L2} 禁止边。
 * 注解取值与注入行为逐字不变（同一串字符）。<b>W6-c 把本类收进 {@code :coe} 之后，
 * 这个字面量必须随之改成 {@code "createoreexpansion"}</b>——否则"住在 coe 文件里却标 cews 的 id"
 * 会静默不注入（AGENTS 红线："类的 modid == 它所在 mod 文件的 id"）。</p>
 */
@EventBusSubscriber(modid = "cews")
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
