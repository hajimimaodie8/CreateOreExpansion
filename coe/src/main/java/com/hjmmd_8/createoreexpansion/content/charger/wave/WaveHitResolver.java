package com.hjmmd_8.createoreexpansion.content.charger.wave;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 能量波的<b>方块命中解析器</b>：把"波撞到了什么 → 应该发生什么"的分支链从波基类里独立出来。
 *
 * <p><b>W6-a：机器分支已改为"问登记表"</b>。波闸 / 三种差波器 / 星辉波变器的判定属于第二层
 * （CEWS）的机器，原先直接按机器方块类型分支（那时是 {@code L1 → L2} 禁止边）；现在统一交给
 * {@link WaveMachineHandlers}——第二层的处理器自己认领方块状态并给出处置结果，
 * 本类只按结果执行引擎侧的动作（burst + discard / 推出方块外 / 记 hitSolid）。
 * 本类因此<b>不再 import 任何机器类</b>，判定顺序仍由登记顺序保持（波闸 → 差波器家族 → 变器）。</p>
 *
 * <p>分支顺序即优先级（与原实现一致）：</p>
 * <ol>
 *   <li><b>强化避雷针</b>：≥3 级波命中即 +1 充能，波消散；</li>
 *   <li><b>机器方块</b>（登记表）：波闸（调级器 / 波速调节器）→ 差器三种（四面 / 六面 / 八面）
 *       → 星辉波变器，逐个交处理器的返回结果执行；</li>
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
	 * @param contraptionCollisions contraption 场景判定（主世界无命中时才询问）
	 * @param subLevelCollisions   Sable sub-level 场景判定（同上）
	 */
	public static void resolve(AbstractChargerWaveEntity wave, WaveContraptionCollisions contraptionCollisions,
		WaveSubLevelCollisions subLevelCollisions) {
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
			// γ 级能量加工（≥3 级）：γ/ε/ω 波命中强化避雷针各 +1 充能，波消散
			if (wave.level()
				.getBlockEntity(pos) instanceof ReinforcedLightningRodBlockEntity rod) {
				if (wave.getWaveLevel() >= 3)
					rod.onGammaWaveHit();
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
				wave.discard();
				return;
			}
			// 机器方块（波闸 / 差波器家族 / 星辉波变器）：由第二层的处理器认领并处置。
			// 未被认领（NOT_MINE）时按原逻辑继续往下走（带物品槽方块 → 撞墙）。
			switch (WaveMachineHandlers.dispatchWorld(wave, pos, state)) {
				case BURST -> {
					// 撞墙湮灭（机器入口关闭 / 机壳面 / 波级不足分裂）
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
					wave.discard();
					return;
				}
				case PUSH -> {
					// 穿过/遣返：把波从方块中心推出方块外，确保碰撞盒完全离开，
					// 避免下一 tick 仍在方块内触发二次判定（二次判定会让降级波按 1 级消失、升级延迟被反复重置）
					wave.setPos(Vec3.atCenterOf(pos)
						.add(wave.getMovement()
							.scale(1.0d)));
					return;
				}
				case CONSUMED -> {
					// 处理器已处理完（变器穿波转换/原路遣返；差器分裂时母波已静默 discard）
					return;
				}
				case BLOCKED -> {
					// 变器波口关闭 / 撞灯盘·轴口面：不可穿，波撞墙消散（原实现 continue 继续扫描）
					hitSolid = true;
					continue;
				}
				case SKIP -> {
					// 变器对波透明：当作这里没有方块（原实现 continue）
					continue;
				}
				case NOT_MINE -> {
					// 不是机器：继续下面的带物品槽方块 / 撞墙分支
				}
			}
			IItemHandler handler = wave.level()
				.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler != null) {
				// 命中带物品槽方块（置物台/工作台/工作盆…）。普通波：只按 charging 配方加工；
				// 变体波覆写本钩子，用自己携带的加工机配方类型处理槽内物品（链式远程加工）。
				if (wave.handleItemInventoryBlock(handler, pos))
					return; // 钩子已处理（可能加工成功并继续/已消散），本 tick 结束
				// 无匹配物品：同样视为撞墙，波在此消散（不穿过置物台/工作台）
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
				wave.discard();
				return;
			}
			hitSolid = true;
			solidPos = pos.immutable(); // 记录撞到的普通方块（引雷用）
		}
		if (hitSolid) {
			wave.onSolidBlockHit(solidPos); // 钩子：变体波在此引雷（见方法注释）
			ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
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
