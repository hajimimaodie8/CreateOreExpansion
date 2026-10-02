package com.hjmmd_8.createoreexpansion.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.field.StressInjectorBlockEntity;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>让手摇曲柄的护目镜信息转到旁边的应力注入器上</b>（用户 2026-10-02 验收标准：
 * "护目镜对着曲柄就能读出这次注入的应力"）。
 *
 * <h2>为什么必须这样绕一下（Create 的事实）</h2>
 * <ul>
 *   <li>动力部件（曲柄 / 轴 / 齿轮）的护目镜只显示<b>它自己的</b>容量：
 *       {@code GeneratingKineticBlockEntity#addToGoggleTooltip} 用
 *       {@code calculateAddedStressCapacity() * getTheoreticalSpeed()} 算，前者来自
 *       {@code BlockStressValues.getCapacity(getStressConfigKey())}，曲柄那一项被 Create 钉死成
 *       {@code CStress.setCapacity(8.0)}（{@code AllBlocks.HAND_CRANK}）⇒ 曲柄那两行永远只能读出
 *       8×32 = <b>256 SU</b>，物理上不可能显示我们注入的 8192；</li>
 *   <li>Create 自己的口径写在它的提示文案里（{@code item.create.goggles.tooltip.behaviour1}）：
 *       "<i>Kinetic components</i> show added <i>Stress Impact</i> or <i>Capacity</i>.
 *       <i>Stressometers</i> show statistics of their <i>attached kinetic network</i>."
 *       —— 网络总量只有应力表会显示；</li>
 *   <li>用户要的是"对着曲柄读" ⇒ 唯一的接口就是 Create 留给附属模组的
 *       {@link IProxyHoveringInformation}：{@code GoggleOverlayRenderer#proxiedOverlayPosition}
 *       会把<b>被瞄准方块的</b>信息源换成 {@code getInformationSource} 返回的那一格
 *       （原版用例：{@code WaterWheelStructuralBlock} 把结构方块的信息转到水车本体）。</li>
 * </ul>
 *
 * <h2>行为</h2>
 * <p>瞄准曲柄时：旁边六格里若有一台<b>正在供能</b>的注入器（{@link
 * StressInjectorBlockEntity#isProvidingDisplay()}：客户端靠同步过来的转速/显示 SU 判定），
 * 就把信息源换成它 ⇒ 玩家看到的是
 * {@link StressInjectorBlockEntity#addToGoggleTooltip} 那两段
 * （我们自己的 8192/16384/32768 SU + 整张网络的总容量）。</p>
 * <p>否则（技能没开 / 注入器没在供能）<b>原样返回曲柄自己的坐标</b> ⇒ 曲柄的护目镜读数
 * 与没装本模组时完全一致（Create 自己的 256 SU / 或未接应力时的 0 SU）。</p>
 *
 * <p>⚠ 这是本模组对 Create 的 {@code HandCrankBlock} 做的唯一一处 Mixin；它<b>只加一个接口</b>、
 * 不改任何既有方法，也不碰曲柄的旋转/应力逻辑。</p>
 *
 * @see StressInjectorBlockEntity#addToGoggleTooltip
 * @since 1.0.0
 */
@Mixin(HandCrankBlock.class)
public abstract class HandCrankGoggleProxyMixin implements IProxyHoveringInformation {

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide) {
            // 服务端不需要这层代理（护目镜只在客户端画）。
            return pos;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            if (!level.isLoaded(neighbour)) {
                continue;
            }
            if (!level.getBlockState(neighbour)
                .is(CoeBlocks.STRESS_INJECTOR.get())) {
                continue;
            }
            if (level.getBlockEntity(neighbour) instanceof StressInjectorBlockEntity injector
                && injector.isProvidingDisplay()) {
                return neighbour;
            }
        }
        return pos;
    }
}
