package com.hjmmd_8.createoreexpansion.content.charger.wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <b>机器命中处置的登记表（第一层侧）</b>（W6-a）。
 *
 * <p>登记方向恒为「第二层（机器）→ 本表」，读取方向恒为「第一层（引擎）→ 本表」，
 * 因此 {@code L1 → L2} 这条禁止边不存在（范式与 {@code common/registry/WaveRecipeCapabilities}
 * 相同）。登记由第二层的 {@code @Mod} 入口（{@code CewsMod}）按<b>判定优先级顺序</b>完成：
 * 能量波闸 → 差波器家族 → 星辉波变器，与原实现在同一次方块遍历里的 if 链顺序逐项一致。</p>
 *
 * <p>未装第二层时表为空：{@code dispatch*} 全部返回 {@link WaveMachineHandler.Outcome#NOT_MINE}，
 * 引擎按"没有机器"的原逻辑继续（只装第一层时那些机器方块本来也不存在）。</p>
 */
public final class WaveMachineHandlers {

	private static final List<WaveMachineHandler> HANDLERS = new CopyOnWriteArrayList<>();

	private WaveMachineHandlers() {
	}

	/**
	 * 登记一个处理器（由第二层在自己的 {@code @Mod} 构造器里调用）。
	 *
	 * <p>幂等：同一个实例重复登记只保留一份，且<b>保持首次登记的先后</b>
	 * （判定顺序 = 登记顺序，换序会改玩法）。</p>
	 */
	public static void register(WaveMachineHandler handler) {
		if (handler != null && !HANDLERS.contains(handler))
			HANDLERS.add(handler);
	}

	/** 已登记的处理器数量（诊断用）。 */
	public static int count() {
		return HANDLERS.size();
	}

	/** 主世界：按登记顺序问第一个认领者；无人认领返回 {@code NOT_MINE}。 */
	public static WaveMachineHandler.Outcome dispatchWorld(AbstractChargerWaveEntity wave, BlockPos pos,
		BlockState state) {
		for (WaveMachineHandler handler : HANDLERS) {
			WaveMachineHandler.Outcome outcome = handler.onWorldHit(wave, pos, state);
			if (outcome != WaveMachineHandler.Outcome.NOT_MINE)
				return outcome;
		}
		return WaveMachineHandler.Outcome.NOT_MINE;
	}

	/** Create contraption：同上。 */
	public static WaveMachineHandler.Outcome dispatchContraption(AbstractChargerWaveEntity wave, BlockPos pos,
		BlockState state, AbstractContraptionEntity entity, Vec3 localCenter) {
		for (WaveMachineHandler handler : HANDLERS) {
			WaveMachineHandler.Outcome outcome = handler.onContraptionHit(wave, pos, state, entity, localCenter);
			if (outcome != WaveMachineHandler.Outcome.NOT_MINE)
				return outcome;
		}
		return WaveMachineHandler.Outcome.NOT_MINE;
	}

	/** Sable 物理结构：同上。 */
	public static WaveMachineHandler.Outcome dispatchSubLevel(AbstractChargerWaveEntity wave, BlockPos pos,
		BlockState state, SubLevelBridge bridge, SubLevelBridge.Hit hit) {
		for (WaveMachineHandler handler : HANDLERS) {
			WaveMachineHandler.Outcome outcome = handler.onSubLevelHit(wave, pos, state, bridge, hit);
			if (outcome != WaveMachineHandler.Outcome.NOT_MINE)
				return outcome;
		}
		return WaveMachineHandler.Outcome.NOT_MINE;
	}
}
