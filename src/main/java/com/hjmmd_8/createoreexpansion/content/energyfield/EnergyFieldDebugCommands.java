package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 临时调试命令（验证能量场用，控制器方块做好后移除）：
 * <ul>
 *   <li>{@code /createoreexpansion field accel &lt;pos&gt; &lt;size&gt; &lt;dx&gt; &lt;dy&gt; &lt;dz&gt; &lt;strength&gt;}
 *       —— 在以 pos 为中心、边长 size 的立方区域注册匀强加速场，方向 (dx,dy,dz)，强度 strength；</li>
 *   <li>{@code /createoreexpansion field deflect &lt;pos&gt; &lt;size&gt; &lt;dx&gt; &lt;dy&gt; &lt;dz&gt; &lt;strength&gt;} —— 偏转场；</li>
 *   <li>{@code /createoreexpansion field clear} —— 清除本维度所有临时场。</li>
 * </ul>
 * 测试步骤：先建场 → 用蓝宝石充能器发射一道波 → 观察是否偏转/加速（需波带电？当前实现
 * 对所有波都作用——本命令场默认把波当正电荷，见 {@link EnergyFields} 注释）。
 */
public final class EnergyFieldDebugCommands {

	public static LiteralArgumentBuilder<CommandSourceStack> register() {
		return Commands.literal("createoreexpansion").then(Commands.literal("field")
			.then(Commands.literal("accel")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("size", DoubleArgumentType.doubleArg(1, 64))
						.then(Commands.argument("dx", DoubleArgumentType.doubleArg(-1, 1))
							.then(Commands.argument("dy", DoubleArgumentType.doubleArg(-1, 1))
								.then(Commands.argument("dz", DoubleArgumentType.doubleArg(-1, 1))
									.then(Commands.argument("strength", DoubleArgumentType.doubleArg(0.1, 100))
										.executes(ctx -> spawn(ctx.getSource(), EnergyFieldType.ACCELERATION,
											BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
											DoubleArgumentType.getDouble(ctx, "size"),
											new Vec3(DoubleArgumentType.getDouble(ctx, "dx"),
												DoubleArgumentType.getDouble(ctx, "dy"),
												DoubleArgumentType.getDouble(ctx, "dz")),
											DoubleArgumentType.getDouble(ctx, "strength"))))))))))
			.then(Commands.literal("deflect")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.then(Commands.argument("size", DoubleArgumentType.doubleArg(1, 64))
						.then(Commands.argument("dx", DoubleArgumentType.doubleArg(-1, 1))
							.then(Commands.argument("dy", DoubleArgumentType.doubleArg(-1, 1))
								.then(Commands.argument("dz", DoubleArgumentType.doubleArg(-1, 1))
									.then(Commands.argument("strength", DoubleArgumentType.doubleArg(0.1, 100))
										.executes(ctx -> spawn(ctx.getSource(), EnergyFieldType.DEFLECTION,
											BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
											DoubleArgumentType.getDouble(ctx, "size"),
											new Vec3(DoubleArgumentType.getDouble(ctx, "dx"),
												DoubleArgumentType.getDouble(ctx, "dy"),
												DoubleArgumentType.getDouble(ctx, "dz")),
											DoubleArgumentType.getDouble(ctx, "strength"))))))))))
			.then(Commands.literal("charge")
				.then(Commands.argument("polarity", StringArgumentType.word())
					.executes(ctx -> chargeWave(ctx.getSource(), StringArgumentType.getString(ctx, "polarity")))))
			.then(Commands.literal("clear")
				.executes(ctx -> {
					net.minecraft.server.level.ServerLevel server =
						ctx.getSource().getLevel() instanceof net.minecraft.server.level.ServerLevel s ? s : null;
					EnergyFields.clear(ctx.getSource()
						.getLevel());
					ctx.getSource()
						.sendSuccess(() -> Component.literal("已清除本维度能量场"), true);
					// 同步给客户端：清空镜像，指示框随之消失
					if (server != null)
						EnergyFieldSyncPayload.broadcastClear(server);
					return 1;
				})));
	}

	/** 给执行者前方最近的能量波赋电荷（临时测试）。 */
	private static int chargeWave(CommandSourceStack src, String polarity) {
		ChargePolarity charge = polarity.equalsIgnoreCase("neg") || polarity.equalsIgnoreCase("negative")
			? ChargePolarity.NEGATIVE
			: ChargePolarity.POSITIVE;
		Level level = src.getLevel();
		if (!(src.getEntity() instanceof net.minecraft.world.entity.Entity looker)) {
			src.sendFailure(Component.literal("该命令需由实体执行"));
			return 0;
		}
		Vec3 eye = looker.getEyePosition();
		Vec3 look = looker.getLookAngle();
		var waves = level.getEntitiesOfClass(
			com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity.class,
			new net.minecraft.world.phys.AABB(eye, eye.add(look.scale(64))).inflate(4), e -> e.isAlive());
		if (waves.isEmpty()) {
			src.sendFailure(Component.literal("前方 64 格内没有能量波"));
			return 0;
		}
		waves.sort(java.util.Comparator.comparingDouble(w -> w.distanceToSqr(eye)));
		waves.get(0)
			.setCharge(charge);
		src.sendSuccess(() -> Component.literal("已给最近的波赋 " + charge + " 电荷"), true);
		CreateOreExpansion.LOGGER.info("[能量场调试] 给波赋 {} 电荷: pos={}, 波等级={}, 距玩家={} 格",
			charge,
			waves.get(0).position(),
			waves.get(0).getWaveLevel(),
			String.format("%.1f", Math.sqrt(waves.get(0).distanceToSqr(eye))));
		return 1;
	}

	private static int spawn(CommandSourceStack src, EnergyFieldType type, BlockPos pos, double size,
		Vec3 direction, double strength) {
		if (!(src.getLevel() instanceof ServerLevel server))
			return 0;
		Vec3 c = Vec3.atCenterOf(pos);
		Vec3 half = new Vec3(size / 2, size / 2, size / 2);
		AABB region = new AABB(c.x - half.x, c.y - half.y, c.z - half.z,
			c.x + half.x, c.y + half.y, c.z + half.z);
		EnergyFields.add(server, new EnergyField(type, region, direction, strength));
		src.sendSuccess(() -> Component.literal("已注册 " + type + " 场 (强度 " + strength
			+ ", 方向 " + direction + ", 区域 " + region + ")"), true);
		CreateOreExpansion.LOGGER.info("[能量场调试] 注册 {} 场: 中心={}, 边长={}, 方向={}, 强度={}, 区域={}",
			type, pos, size, direction, strength, region);
		// 同步给客户端（护目镜场域指示框的数据源）
		EnergyFieldSyncPayload.broadcastToDimension(server);
		return 1;
	}

	// 供外部一次性注册（避免未用变量告警）
	static void unused(List<String> l) {
	}
}
