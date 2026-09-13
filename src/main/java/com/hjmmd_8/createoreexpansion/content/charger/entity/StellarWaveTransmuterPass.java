package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlock;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 星辉波变器对路过能量波的"转换执行器"（单次命中判定，无实体 tick 职责）。
 *
 * <p><b>规则（用户定义）</b>：能量波只能从变器的<b>四个水平侧面</b>穿入/穿出，轴口面/灯盘面不可穿。
 * 命中机器块时按"入口面是否开口 + 对面出口是否开口"三态处理（见 {@link Result}）：</p>
 * <ul>
 *   <li><b>入口面关闭</b>（或竖直方向撞上下面/撞灯盘·轴口面）→ {@link Result#HIT_WALL}
 *       ：按撞关闭机壳处理，调用方 burst + discard；</li>
 *   <li><b>入口开、对面出口也开</b> → {@link Result#CONVERTED}：把原波<b>静默转为变体波</b>
 *       （{@link StellarWaveEntity}，携带扫描属性快照/机器状态配方类型/辅料载荷/避雷针释放机会，
 *       等级不变）并沿原方向从对侧推出，原波 discard；</li>
 *   <li><b>入口开、对面出口关闭</b> → {@link Result#BOUNCED}：<b>原路返回</b>（movement 反向、
 *       位置推出方块外），<b>不生成变体波、不改变等级、不消散</b>。</li>
 * </ul>
 *
 * <p>避雷针释放机会在穿波瞬间<b>真正抽取</b>：把"已蓄满待释放"的避雷针储层清零
 * （波拿到的是一次模拟闪电加工额度；波不引雷、不能远程让避雷针真引雷）。</p>
 *
 * <p>与 {@link WaveMachineActions} 同包，可读写 {@link AbstractChargerWaveEntity} 的
 * package/protected 状态（movement/speedOffset/renderColor 等），保持实体字段封装。</p>
 */
public final class StellarWaveTransmuterPass {

	/**
	 * 变器命中判定三态。
	 */
	public enum Result {
		/** 已转换为变体波（生成新波并从对侧穿出，原波已 discard）——调用方结束本 tick 处理。 */
		CONVERTED,
		/** 入口开但对面出口关：已原路遣返（movement 反向 + 推出方块外）——调用方结束本 tick，波继续飞。 */
		BOUNCED,
		/** 未开口 / 不可穿的机壳面（轴口面·灯盘面·竖直撞击）：调用方按撞墙处理（burst + discard）。 */
		HIT_WALL;
	}

	/** 避雷针蓄能字段（私有 int；反射清零 = 抽取释放机会）。 */
	private static java.lang.reflect.Field rodProgressField;
	private static java.lang.reflect.Field rodReadyField;

	private StellarWaveTransmuterPass() {
	}

	/**
	 * 尝试把波转换穿过变器。
	 *
	 * @param wave 命中的能量波（服务端实例）
	 * @param pos  变器方块位置
	 * @return 三态结果（{@link Result}）：调用方按 CONVERTED/BOUNCED → return、
	 *         HIT_WALL → 按撞墙处理
	 */
	public static Result tryConvert(AbstractChargerWaveEntity wave, BlockPos pos) {
		if (wave == null || wave.level().isClientSide)
			return Result.HIT_WALL;
		// 只允许水平方向穿侧（上下面不可穿）：竖直行进 = 撞灯盘/轴口面，按墙；
		// 含 45° 斜向（差波器对角波）仍按水平分量判定入口面
		if (Math.abs(wave.movement.y) > 0.2d)
			return Result.HIT_WALL;

		Level level = wave.level();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof StellarWaveTransmuterBlock)
			|| !(level.getBlockEntity(pos) instanceof StellarWaveTransmuterBlockEntity be))
			return Result.HIT_WALL;

		// 入口 = 运动方向的水平主方向（波接触的面即运动反方向那侧）；
		// isOpen 内部把模型面换成世界方向，且对 UP/DOWN 恒返回 false
		Direction inDir = Direction.getNearest(wave.movement.x, 0, wave.movement.z);
		Direction outDir = inDir.getOpposite();
		if (!StellarWaveTransmuterBlock.isOpen(state, inDir))
			return Result.HIT_WALL; // 入口侧未开口：撞关闭机壳

		if (!StellarWaveTransmuterBlock.isOpen(state, outDir)) {
			// 入口开、对面出口关：原路返回（照 WaveMachineActions 反弹写法），不转换、不降级、不消散
			wave.movement = wave.movement.scale(-1);
			// 推到方块中心外侧一格，确保碰撞盒完全离开，避免下一 tick 二次判定
			wave.setPos(Vec3.atCenterOf(pos)
				.add(wave.movement.scale(1.0d)));
			return Result.BOUNCED;
		}

		// 变体波：等级/方向不变，携带扫描属性快照与载荷（辅料物品/流体/电量）；
		// 链式上限 = 波等级；出生在对侧外推半格多，确保离开机器盒
		Vec3 exit = Vec3.atCenterOf(pos)
			.add(wave.movement.scale(0.65d));
		StellarWaveEntity variant = new StellarWaveEntity(level, exit, wave.movement, wave.waveLevel);
		variant.setAttributes(be.getScannedMachineIds());
		variant.setRecipeTypes(be.getScannedRecipeTypes());
		// 加热能力随波携带（变器扫描半径内点燃的烈焰燃烧室）：命中时满足 HEATED/SUPERHEATED
		// 配方。热源不是动能机器、不进机器表，是变器的独立读数（见 scanHeatSources）
		variant.setCarriedHeat(be.getScannedHeat());
		// 加工转速随波携带 = 变器自身转速（用户拍板口径）：供"转速档优先级"使用
		// （Vintage 抛光 speed_limits ↔ 变器转速档，见 VintageRecipeSpeed）
		variant.setCarriedRpm(be.getSpeed());
		// 读取半径随波携带：命中后"就地补料"的生效范围与变器读取范围一致（用户 2026-09 定义）
		variant.setCarriedRadius(be.getScanRadius());
		variant.setChain(wave.waveLevel);
		// 载荷在"波穿过的这一瞬间"才真正从扫描区容器抽取（上限：物品 5 个/5 种、
		// 流体 500 mB、可抽电量全抽）。扫描阶段只做估算，避免箱子被持续抽空。
		// 同时带上"取料来源位置"：载荷消散时把剩余物还回这些容器（而不是丢进正在加工的工作盆）。
		StellarWaveTransmuterBlockEntity.Payload payload = be.collectPayloadForWave();
		variant.attachPayload(payload.items(), payload.fluid(), payload.energy(), payload.sources());
		variant.setRodCharges(drainRodCredit(level, be.getChargedRodPositions()));
		// 继承速度修正/电荷/渲染色，保证表现连续（波速调节器与能量场修正贯穿转换）
		variant.addSpeedOffset(wave.speedOffset);
		variant.setCharge(wave.getChargePolarity());
		variant.renderColor = wave.renderColor;

		// 记录"最近穿出变体波"当时实际可执行的全部配方类型到变器（护目镜面板逐条展示；
		// 取波对象全集 = 携带扫描类型 + 载荷电量额外类型，见 StellarWaveEntity#getActiveRecipeTypes）
		be.recordPassedWaveRecipeTypes(variant.getActiveRecipeTypes());

		level.addFreshEntity(variant);
		wave.discard();
		return Result.CONVERTED;
	}

	/** 依次尝试从已蓄满的避雷针抽取释放机会（清空储层）；成功返回 1，否则 0。 */
	private static int drainRodCredit(Level level, List<BlockPos> rods) {
		for (BlockPos p : rods) {
			if (!(level.getBlockEntity(p) instanceof ReinforcedLightningRodBlockEntity rod))
				continue;
			if (!rod.hasReadyCharge())
				continue;
			if (resetRodCharge(rod))
				return 1;
		}
		return 0;
	}

	/** 反射清零避雷针蓄能进度与待释放次数（抽取本次释放机会；不触发真闪电）。 */
	private static boolean resetRodCharge(ReinforcedLightningRodBlockEntity rod) {
		try {
			java.lang.reflect.Field f = rodProgressField;
			if (f == null) {
				f = ReinforcedLightningRodBlockEntity.class.getDeclaredField("gammaChargeProgress");
				f.setAccessible(true);
				rodProgressField = f;
			}
			java.lang.reflect.Field g = rodReadyField;
			if (g == null) {
				g = ReinforcedLightningRodBlockEntity.class.getDeclaredField("readyCharges");
				g.setAccessible(true);
				rodReadyField = g;
			}
			f.setInt(rod, 0);
			g.setInt(rod, 0);
			rod.setChanged();
			// 必须同步客户端（2026-09 审计修复）：setChanged 只标服务端脏，不含 BE 数据包，
			// 客户端 gammaChargeProgress/readyCharges 仍是满格 → 护目镜进度条不降、就绪粒子继续播。
			// 与 ReinforcedLightningRodBlockEntity#syncToClient 同口径（块更新包）。
			if (rod.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)
				serverLevel.getChunkSource()
					.blockChanged(rod.getBlockPos());
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
}
