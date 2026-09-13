package com.hjmmd_8.createoreexpansion.content.charger.wave;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveTransmuterPass;
import com.hjmmd_8.createoreexpansion.content.charger.entity.WaveContraptionCollisions;
import com.hjmmd_8.createoreexpansion.content.charger.entity.WaveMachineActions;
import com.hjmmd_8.createoreexpansion.content.charger.entity.WaveSubLevelCollisions;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 能量波的<b>方块命中解析器</b>：把"波撞到了什么 → 应该发生什么"的分支链从波基类里独立出来。
 *
 * <p>分支顺序即优先级（与波基类原实现完全一致，逐行等价搬迁）：</p>
 * <ol>
 *   <li><b>强化避雷针</b>：≥3 级波命中即 +1 充能，波消散；</li>
 *   <li><b>能量波闸</b>（调级器 / 波速调节器）：按 {@code modulatesWaveLevel()} 分流，遣返或推出方块外；</li>
 *   <li><b>差器</b>三种（四面 / 六面 / 八面）：遣返、转向、均摊分裂；</li>
 *   <li><b>星辉波变器</b>：入口开口 → 穿波转换（转成变体波 / 原路遣返 / 撞墙）；</li>
 *   <li><b>带物品槽方块</b>：交给 {@link AbstractChargerWaveEntity#handleItemInventoryBlock} 钩子；</li>
 *   <li>其余：按撞墙处理（{@link AbstractChargerWaveEntity#onSolidBlockHit} 钩子 + 绽放消散）。</li>
 * </ol>
 *
 * <p>主世界一个都没命中时，再补做动态结构判定（Create contraption → Sable sub-level）。</p>
 */
public final class WaveHitResolver {

	private WaveHitResolver() {
	}

	/**
	 * 解析本 tick 的方块命中。
	 *
	 * @param wave                 被解析的波（原实现里是 {@code this}，故本类只读取它的公开状态）
	 * @param actions              机器动作执行器（波闸 / 三种差器）
	 * @param contraptionCollisions contraption 场景判定（主世界无命中时才询问）
	 * @param subLevelCollisions   Sable sub-level 场景判定（同上）
	 */
	public static void resolve(AbstractChargerWaveEntity wave, WaveMachineActions actions,
		WaveContraptionCollisions contraptionCollisions, WaveSubLevelCollisions subLevelCollisions) {
		boolean hitSolid = false;
		// 撞到的那个"不带物品槽的普通方块"的位置（供 onSolidBlockHit 引雷用；只有真撞到才非空）
		BlockPos solidPos = null;
		for (BlockPos pos : BlockPos.betweenClosed(
			Mth.floor(wave.getBoundingBox().minX), Mth.floor(wave.getBoundingBox().minY),
			Mth.floor(wave.getBoundingBox().minZ),
			Mth.floor(wave.getBoundingBox().maxX), Mth.floor(wave.getBoundingBox().maxY),
			Mth.floor(wave.getBoundingBox().maxZ))) {
			BlockState state = wave.level()
				.getBlockState(pos);
			if (state.isAir())
				continue;
			// 伽马能量加工（≥3 级）：伽马/伊普西龙/欧米伽波命中强化避雷针各 +1 充能，波消散
			if (wave.level()
				.getBlockEntity(pos) instanceof ReinforcedLightningRodBlockEntity rod) {
				if (wave.getWaveLevel() >= 3)
					rod.onGammaWaveHit();
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
				wave.discard();
				return;
			}
			// 能量波闸（调级器/波速调节器，翡翠/蓝宝石）：面板通道 + 应力调制。
			// 用基类 Block 统一命中，按 modulatesWaveLevel() 区分走等级调制还是速度调制
			if (state.getBlock() instanceof AbstractWaveGateBlock waveGate
				&& wave.level()
					.getBlockEntity(pos) instanceof AbstractWaveGateBlockEntity gateBe) {
				boolean isRegulator = waveGate.modulatesWaveLevel();
				boolean vanish;
				if (isRegulator) {
					vanish = actions.handleRegulator(gateBe, pos, wave.getBoundingBox()
						.getCenter());
				} else {
					vanish = actions.handleWaveSpeedRegulator(gateBe, pos, wave.getBoundingBox()
						.getCenter());
				}
				if (vanish) {
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
					wave.discard();
					return;
				}
				// 穿过/遣返：把波从方块中心推出方块外，确保碰撞盒完全离开，
				// 避免下一 tick 仍在方块内触发二次判定（二次判定会让降级波按 1 级消失、升级延迟被反复重置）
				wave.setPos(Vec3.atCenterOf(pos)
					.add(wave.getMovement()
						.scale(1.0d)));
				return;
			}
			// 能量波差器：多开口均摊分发（1 开口遣返 / 2 开口穿过 / ≥3 开口均摊降级）
			if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
				boolean vanish = actions.handleDisperser(state, pos, wave.getBoundingBox()
					.getCenter(), null);
				if (vanish) {
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
					wave.discard();
					return;
				}
				// 分裂：母波已在执行器内静默 discard（无撞墙特效），直接结束本 tick
				if (!wave.isAlive())
					return;
				wave.setPos(Vec3.atCenterOf(pos)
					.add(wave.getMovement()
						.scale(1.0d)));
				return;
			}
			// 六面能量波差器：无朝向，6 面独立开关（1 遣返 / 2 穿过 / 3-4 降一级均摊 / 5-6 降二级均摊）
			if (state.getBlock() instanceof SixFaceDisperserBlock) {
				boolean vanish = actions.handleSixFaceDisperser(state, pos, wave.getBoundingBox()
					.getCenter(), null);
				if (vanish) {
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
					wave.discard();
					return;
				}
				if (!wave.isAlive())
					return; // 分裂：母波静默消散
				wave.setPos(Vec3.atCenterOf(pos)
					.add(wave.getMovement()
						.scale(1.0d)));
				return;
			}
			// 八面能量波差器：8 口（4 正交 + 4 斜），1 遣返 / 2 转向 / 3-4 降 1 / 5-6 降 2 / 7-8 降 3
			if (state.getBlock() instanceof OctaEnergyWaveDifferencerBlock) {
				boolean vanish = actions.handleOctaDisperser(state, pos, wave.getBoundingBox()
					.getCenter(), null);
				if (vanish) {
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
					wave.discard();
					return;
				}
				if (!wave.isAlive())
					return; // 分裂：母波静默消散
				wave.setPos(Vec3.atCenterOf(pos)
					.add(wave.getMovement()
						.scale(1.0d)));
				return;
			}
			// 星辉波变器：入口侧开口则处理——对面开口→变体波携带扫描属性从对侧穿出；
			// 对面关闭→原路遣返（等级不变）；入口关闭/机壳面/竖直撞击→按撞墙消散
			if (state.getBlock() instanceof StellarWaveTransmuterBlock) {
				StellarWaveTransmuterPass.Result pass = StellarWaveTransmuterPass.tryConvert(wave, pos);
				if (pass == StellarWaveTransmuterPass.Result.HIT_WALL) {
					hitSolid = true; // 波口关闭 / 撞灯盘·轴口面：不可穿，波撞墙消散
					continue;
				}
				// CONVERTED：已转成变体波（原波静默 discard）；
				// BOUNCED：已反向并推出方块外（等级不变），继续飞行
				return;
			}
			IItemHandler handler = wave.level()
				.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler != null) {
				// 命中带物品槽方块（置物台/工作台/工作盆…）。普通波：只按 charging 配方加工；
				// 变体波覆写本钩子，用自己携带的加工机配方类型处理槽内物品（链式远程加工）。
				if (wave.handleItemInventoryBlock(handler, pos))
					return; // 钩子已处理（可能加工成功并继续/已消散），本 tick 结束
				// 无匹配物品：同样视为撞墙，波在此消散（不穿过置物台/工作台）
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
				wave.discard();
				return;
			}
			hitSolid = true;
			solidPos = pos.immutable(); // 记录撞到的普通方块（引雷用）
		}
		if (hitSolid) {
			wave.onSolidBlockHit(solidPos); // 钩子：变体波在此引雷（见方法注释）
			ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
			wave.discard();
			return;
		}

		// 阶段 2：动态结构判定（补充，不劫持主世界）——主世界无命中时：
		// a) Create contraption（动力轴承/矿车装配站装配）；b) Sable sub-level（物理结构）
		if (contraptionCollisions.tryHandle())
			return;
		subLevelCollisions.tryHandle();
	}
}
