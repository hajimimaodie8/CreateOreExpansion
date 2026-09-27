package com.hjmmd_8.createoreexpansion.content.charger.wave;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <b>波命中一台"机器方块"时的处置契约（第一层侧）</b>（W6-a）。
 *
 * <p><b>它解决什么问题</b>：波引擎（第一层 / COE）的命中分派原先直接按机器方块类型分支
 * （波闸 / 三种差波器 / 星辉波变器），那些机器是<b>第二层</b>（CEWS）。机械搬运后这就是
 * {@code L1 → L2} 禁止边（见 {@code build/patch/w6-restructure-PLAN.md} §2.2 的 E1–E4）。
 * 契约把"某个方块状态归谁管、由谁处置"从"引擎认机器类"改成"<b>机器自报 + 引擎只问登记表</b>"——
 * 与 {@code common/registry/WaveRecipeCapabilities}（P3i 消掉 {@code CEWS → TRANS}）同形。</p>
 *
 * <p><b>为什么三个场景各一个入口</b>：主世界、Create contraption（结构本地坐标系 + 无方块实体）、
 * Sable 物理结构（结构本地坐标系 + 位姿矩阵）三处的判定虽然共用同一套规则，但坐标换算与
 * "拿 BE"的方式不同，原实现就是三条独立分支；逐场景委托才能保证逐行等价。</p>
 *
 * <p><b>处置结果的语义（由第一层的三个调用点解释）</b>：</p>
 * <ul>
 *   <li>{@link Outcome#NOT_MINE} —— 这个方块不归我管，调用方按原逻辑继续（找下一个处理器，
 *       最后落到引擎自己的分支）；</li>
 *   <li>{@link Outcome#SKIP} —— 认领了，但本次不处理，调用方 {@code continue} 到下一个方块
 *       （主世界的变器"透明"分支、contraption 上判定结果异常的兜底分支）；</li>
 *   <li>{@link Outcome#CONSUMED} —— 已处理完（主世界：波已被安置，直接结束本 tick；
 *       contraption/物理结构：处理者自己完成推出与特效，调用方结束本 tick）；</li>
 *   <li>{@link Outcome#BURST} —— 撞墙湮灭：调用方 burst + discard 后结束（仅主世界）；</li>
 *   <li>{@link Outcome#PUSH} —— 穿过/遣返：调用方把波推出方块外后结束（仅主世界）；</li>
 *   <li>{@link Outcome#BLOCKED} —— 不可穿（变器波口关闭/机壳面）：调用方记 hitSolid 后继续扫描
 *       （仅主世界，对应原实现里那个"不 return 而是 continue"的分支）。</li>
 * </ul>
 */
public interface WaveMachineHandler {

	/** 处置结果，语义见类注释。 */
	enum Outcome {
		NOT_MINE,
		SKIP,
		CONSUMED,
		BURST,
		PUSH,
		BLOCKED
	}

	/**
	 * 主世界命中处置。
	 *
	 * <p>实现方必须自己做"是不是我的方块"的完整判定（含方块<b>实体</b>是否就位，
	 * 因为原实现在 BE 缺失时会落回引擎自己的撞墙分支），不认领就返回
	 * {@link Outcome#NOT_MINE}。</p>
	 */
	Outcome onWorldHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state);

	/**
	 * Create contraption 命中处置（contraption 本地坐标系，无方块实体）。
	 *
	 * @param entity      contraption 实体（坐标换算与"随结构旋转"用）
	 * @param localCenter 波中心在 contraption 本地坐标系下的位置
	 */
	Outcome onContraptionHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		AbstractContraptionEntity entity, Vec3 localCenter);

	/**
	 * Sable 物理结构（sub-level）命中处置（结构本地坐标系）。
	 *
	 * @param bridge 位姿矩阵桥（世界 ↔ 结构本地）
	 * @param hit     波中心命中的那个结构
	 */
	Outcome onSubLevelHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		SubLevelBridge bridge, SubLevelBridge.Hit hit);
}
