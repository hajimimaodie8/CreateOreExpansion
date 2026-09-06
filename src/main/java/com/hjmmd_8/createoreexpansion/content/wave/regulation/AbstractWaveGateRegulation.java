package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.frame.WaveGateFrame;
import com.simibubi.create.content.kinetics.base.IRotate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波闸（Wave Gate）核心判定逻辑基类：面板通道 + 应力调制方向判定。
 *
 * <p><b>方向铁律（务必记住，切勿颠倒）</b>：
 * <ul>
 *   <li>FACING = 齿轮轴方向（齿轮绕 FACING 轴旋转）</li>
 *   <li>顶面板（RECEIVER_TOP）朝向 FACING；底面板（RECEIVER_BOTTOM）朝向 FACING 对面</li>
 *   <li>齿轮的 4 个侧面（垂直于 FACING）没有面板，能量波撞上 = 撞墙消失</li>
 *   <li>波必须沿 FACING 轴方向（面板端）进入/穿出；任何明显的垂直分量都视为撞上齿轮端</li>
 * </ul>
 *
 * <p><b>通道判定（本基类，先于调制，无应力/有应力都适用）</b>：
 * <pre>
 * 入口面板关闭（单开口的关闭侧 / 两侧关闭）→ 如撞正常方块，波消失
 * 波前偏移入口面板中心区域（斜射/偏移射擦边）→ 如撞正常方块，波消失
 * 两侧开口 → 通道，波可穿过（调制见下）
 * 仅入口开口 → 波进入后原路遣返（反弹）
 * </pre>
 *
 * <p><b>入口判定区域（本基类）</b>：波前中心必须落在入口面板的<b>中心正方形
 * 4×4</b> 区域内（面宽 16 单位 → 半宽 {@value #ENTRY_CENTER_HALF} 方块）才算有效进入；
 * 面内偏移超限的波按撞墙消失。判定用 {@link WaveGateFrame} 把波位置换算到机器本地坐标
 * （静态方块由 FACING 推导；Create 动态结构 / 航空学结构各自实现该接口）。</p>
 *
 * <p><b>应力调制方向（本基类，仅接入应力且转速达标时生效；不接入应力则无调制）</b>：
 * 从能量波前进方向看齿轮：
 * <ul>
 *   <li>正转速（speed &gt; 0）= 从 +FACING 看逆时针旋转（JOML 右手定则）</li>
 *   <li>波沿 +FACING 前进时，波前方视线为 -FACING，看到的旋转是镜像：顺时针</li>
 *   <li><b>增强</b>（等级+1 / 加速） ⟺ 从波前方看齿轮顺时针 ⟺ movement·FACING 与 speed 同号（dot * speed &gt; 0）</li>
 *   <li><b>减弱</b>（等级-1 / 减速） ⟺ 从波前方看齿轮逆时针 ⟺ movement·FACING 与 speed 异号</li>
 * </ul>
 *
 * <p><b>调制效果由子类实现</b>（{@link #applyModulation}）：
 * 调级器 = 等级 ±1（升级/降级 + 伽马/低级爆炸边缘）；波速调节器 = 速度 ±offset（按转速分档）。
 * 基类只判定"通道动作 + 是否调制 + 调制方向"，具体效果交子类。</p>
 */
public abstract class AbstractWaveGateRegulation {

	/**
	 * 入口中心判定区域半宽：中心正方形边长 4 单位（机器面板 16×16）→ 半宽 2/16 = 0.125 方块。
	 * 波前中心在入口面板上的面内偏移（本地 X/Y）超过此值 → 视为斜射/偏移射擦边，不触发入口判定。
	 */
	public static final double ENTRY_CENTER_HALF = 2.0 / 16.0;

	/** 通道动作：波如何处理。 */
	public enum Channel {
		/** 撞墙消失：齿轮端 / 入口面板关闭。 */
		VANISH,
		/** 单开口：原路遣返（反弹）。 */
		BOUNCE,
		/** 双开口：穿过。 */
		PASS;
	}

	/**
	 * 一次判定的结构化结果：通道动作 + 调制信息。
	 *
	 * @param channel    通道动作（VANISH / BOUNCE / PASS）
	 * @param modulate   是否需要应力调制（仅接入应力且转速达标、且通道为 PASS/BOUNCE 时 true）
	 * @param boost      调制方向：true = 增强（等级+1 / 加速）；false = 减弱（等级-1 / 减速）。
	 *                   {@code modulate=false} 时无意义。
	 */
	public record Result(Channel channel, boolean modulate, boolean boost) {

		/** 撞墙消失（无调制）。 */
		public static Result vanish() {
			return new Result(Channel.VANISH, false, false);
		}

		/** 反弹，无调制（无应力单开口）。 */
		public static Result bounce() {
			return new Result(Channel.BOUNCE, false, false);
		}

		/** 穿过，无调制（无应力双开口）。 */
		public static Result pass() {
			return new Result(Channel.PASS, false, false);
		}

		/** 反弹 + 应力调制（有应力单开口）。 */
		public static Result bounceModulated(boolean boost) {
			return new Result(Channel.BOUNCE, true, boost);
		}

		/** 穿过 + 应力调制（有应力双开口）。 */
		public static Result passModulated(boolean boost) {
			return new Result(Channel.PASS, true, boost);
		}
	}

	protected AbstractWaveGateRegulation() {
	}

	/**
	 * 对命中波闸的能量波执行一次通道 + 调制方向判定。
	 *
	 * <p><b>状态参数化</b>：判定只依赖方块状态/位置/转速，不直接依赖 BE 实例——
	 * 便于 Create 动态结构（contraption）上无 BE 实例时复用（传 state + 转速 0）。</p>
	 *
	 * @param state    波闸方块状态（面板开闭/朝向）
	 * @param pos      波闸方块位置（与 wavePos 同坐标系）
	 * @param speed    当前转速（绝对值，0 = 无应力/未调制）
	 * @param wavePos  波前中心（与 pos 同坐标系）：用于入口中心区域判定
	 * @param movement 波的飞行方向（单位向量）
	 * @return 结构化结果：通道动作 + 是否调制 + 调制方向
	 */
	public Result handle(BlockState state, BlockPos pos, float speed, Vec3 wavePos, Vec3 movement) {
		return handle(state, pos, speed, IRotate.SpeedLevel.FAST.getSpeedValue(), wavePos, movement);
	}

	/**
	 * 带机型最低调制转速的判定（翡翠 = FAST 100；蓝宝石 = 64）。
	 *
	 * @param threshold 该机型的最低调制转速（RPM），由 BE 机型参数提供；
	 *                  纯 state 场景（contraption，speed=0）传任意值不影响（0 转速永不达标）
	 */
	public Result handle(BlockState state, BlockPos pos, float speed, float threshold, Vec3 wavePos, Vec3 movement) {
		Direction facing = state.getValue(AbstractWaveGateBlock.FACING);
		boolean topOpen = state.getValue(AbstractWaveGateBlock.RECEIVER_TOP);
		boolean bottomOpen = state.getValue(AbstractWaveGateBlock.RECEIVER_BOTTOM);

		Vec3 facingVec = Vec3.atLowerCornerOf(facing.getNormal());
		double dot = movement.dot(facingVec);

		// 1. 进入端必须是面板端：波必须沿 FACING 轴方向飞行。
		//    任何明显的垂直分量都意味着波撞上了齿轮侧面 → 撞墙消失。
		if (Math.abs(dot) < 0.9d) {
			return Result.vanish();
		}

		// 2. 入口位置判定：波前中心必须落在入口面板的中心 4×4 区域。
		//    把波位置换算到机器本地坐标（静态 = FACING 推导；动态结构 = 矩阵实现），
		//    本地 X/Y 为面板面内偏移——超限的斜射/偏移射按撞墙消失，不触发入口。
		WaveGateFrame frame = WaveGateFrame.staticFrame(pos, facing);
		Vec3 local = frame.toLocal(wavePos);
		if (Math.abs(local.x) > ENTRY_CENTER_HALF || Math.abs(local.y) > ENTRY_CENTER_HALF) {
			return Result.vanish();
		}

		// dot > 0：波沿 +FACING 前进 → 从 x 小处来，先接触 -FACING 侧的面 = 底面板
		//           （底面板朝 FACING 对面）
		// dot < 0：波沿 -FACING 前进 → 先接触 +FACING 侧的面 = 顶面板（朝 FACING）
		boolean fromTop = dot < 0d;

		// 3. 入口面板：关闭则如撞正常方块，波消失（覆盖单开口的关闭侧、两侧关闭）。
		boolean entryOpen = fromTop ? topOpen : bottomOpen;
		if (!entryOpen) {
			return Result.vanish();
		}

		boolean exitOpen = fromTop ? bottomOpen : topOpen;

		// 4. 双开口：正常通道 / 应力调制方向判定。
		if (exitOpen) {
			if (!isFastEnough(speed, threshold)) {
				// 未接入应力或转速不足（低于机型门槛）：
				// 机器只是普通能量波通道，无调制（护目镜会显示转速不足提示）。
				return Result.pass();
			}
			// 从波前方看齿轮：
			//   顺时针（增强） ⟺ movement·FACING 与 speed 同号（dot * speed > 0）
			boolean boost = dot * speed > 0d;
			return Result.passModulated(boost);
		}

		// 5. 单开口：无应力或转速不足 → 纯反弹；有应力且达标 → 反弹 + 调制方向。
		if (!isFastEnough(speed, threshold)) {
			return Result.bounce();
		}
		boolean boost = dot * speed > 0d;
		return Result.bounceModulated(boost);
	}

	/**
	 * 转速是否达标：|speed| ≥ threshold（RPM）。
	 * 翡翠默认 FAST（100）；蓝宝石机型 64（由 BE 机型参数传入）。
	 */
	protected static boolean isFastEnough(float speed, float threshold) {
		return Math.abs(speed) >= threshold;
	}
}
