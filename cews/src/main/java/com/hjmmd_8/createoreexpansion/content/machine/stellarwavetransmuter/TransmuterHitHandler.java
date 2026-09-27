package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveMachineHandler;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <b>星辉波变器的波命中处置</b>（W6-a）。
 *
 * <p>原先这段分支住在第一层的 {@code content/charger/wave/WaveHitResolver} 里，
 * 直接读第二层的 {@link TransmuterMode} 与 {@link StellarWaveTransmuterBlock}。
 * W6-a 把它搬进第二层并实现第一层的 {@link WaveMachineHandler} 契约；
 * 三个处置的语义与原实现的 switch 逐分支一一对应：</p>
 * <ul>
 *   <li>{@code CONSUMED} → {@link Outcome#CONSUMED}：穿波转换 / 原路遣返已做完，本 tick 结束；</li>
 *   <li>{@code BLOCKED} → {@link Outcome#BLOCKED}：波口未开 / 撞不可穿机壳面 ⇒ 调用方记 hitSolid
 *       继续扫描（原实现就是"不 return 而是 continue"）；</li>
 *   <li>{@code TRANSPARENT} → {@link Outcome#SKIP}：变器对波透明（攻击波变态恒如此；加工波变态在
 *       未接入应力时也走这条）⇒ 当作这里没有方块。</li>
 * </ul>
 *
 * <p>contraption 与 Sable 物理结构两条路径原实现<b>没有</b>变器分支（结构上的变器按"其它方块"
 * 处理），因此这两个入口恒返回 {@link Outcome#NOT_MINE}。</p>
 */
public final class TransmuterHitHandler implements WaveMachineHandler {

	/** 无状态处理器，登记一次即可。 */
	public static final TransmuterHitHandler INSTANCE = new TransmuterHitHandler();

	private TransmuterHitHandler() {
	}

	@Override
	public Outcome onWorldHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state) {
		// 星辉波变器：按变器当前处理模式分流（加工波变态 / 攻击波变态）。
		if (!(state.getBlock() instanceof StellarWaveTransmuterBlock))
			return Outcome.NOT_MINE;
		return switch (TransmuterMode.at(wave.level(), pos)
			.onWaveHit(wave, pos)) {
			case CONSUMED -> Outcome.CONSUMED;
			case BLOCKED -> Outcome.BLOCKED;
			case TRANSPARENT -> Outcome.SKIP;
		};
	}

	@Override
	public Outcome onContraptionHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		AbstractContraptionEntity entity, Vec3 localCenter) {
		return Outcome.NOT_MINE;
	}

	@Override
	public Outcome onSubLevelHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		SubLevelBridge bridge, SubLevelBridge.Hit hit) {
		return Outcome.NOT_MINE;
	}
}
