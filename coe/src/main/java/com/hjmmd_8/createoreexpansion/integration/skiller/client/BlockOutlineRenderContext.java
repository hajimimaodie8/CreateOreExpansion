package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.leaf.skiller.foundation.context.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 方块描边预览的渲染上下文（新内核版）。
 *
 * <p>为什么不用现成的 {@link ExcavationSkillContext}：新内核的渲染流程是
 * {@code getContext(...)} 返回什么、{@code render(...)} 就收到什么，而预览额外需要
 * <b>算好的方块集合</b>与<b>描边颜色</b>（颜色来自物品 NBT 的 {@code OutlineColor}）。
 * 把这两样放进上下文，可以让 {@code render} 只负责画线、不重复跑一遍策略。</p>
 *
 * <h2>物理结构（sub-level）</h2>
 * <p>{@link #subLevelHit()} 非空时，{@link #positions()} 里的坐标是<b>结构局部坐标</b>
 * （不是世界坐标），渲染方必须在画之前把"局部 → 世界"的位姿矩阵乘上 poseStack
 * （见 {@code CoeBlockOutlineRenderer}）；为 null 时一切与改动前一致（世界坐标 + 不加矩阵）。</p>
 *
 * @since 1.0.0
 */
public class BlockOutlineRenderContext implements SkillContext {

    private final Player player;
    private final Set<BlockPos> positions;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    /** 结构句柄；null = 普通主世界方块预览 */
    @Nullable
    private final SubLevelBridge.Hit subLevelHit;

    public BlockOutlineRenderContext(Player player, Set<BlockPos> positions,
                                     float red, float green, float blue, float alpha) {
        this(player, positions, red, green, blue, alpha, null);
    }

    public BlockOutlineRenderContext(Player player, Set<BlockPos> positions,
                                     float red, float green, float blue, float alpha,
                                     @Nullable SubLevelBridge.Hit subLevelHit) {
        this.player = player;
        this.positions = positions;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.subLevelHit = subLevelHit;
    }

    /** 结构句柄；null = 普通主世界方块预览（未装 Sable 时恒为 null）。 */
    @Nullable
    public SubLevelBridge.Hit subLevelHit() {
        return subLevelHit;
    }

    public Set<BlockPos> positions() {
        return positions;
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    public float alpha() {
        return alpha;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
