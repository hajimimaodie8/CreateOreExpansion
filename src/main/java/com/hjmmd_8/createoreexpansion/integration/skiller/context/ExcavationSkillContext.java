package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.leaf.skiller.foundation.context.SkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 挖掘类技能上下文（新内核版）。
 *
 * <p>字段与旧
 * {@link com.hjmmd_8.createoreexpansion.content.skill.context.DestroyBlockContext}
 * （{@code record DestroyBlockContext(Level level, BlockPos pos, ItemStack tool, LivingEntity entity)}）
 * 一一对应，只是改为实现 {@link SkillContext}，为 Skiller 的释放路径提供 {@link #getPlayer()}。</p>
 *
 * <p>旧触发点是 {@code mixin/ServerPlayerGameModeMixin}（注入 {@code destroyBlock}，
 * 没有对应的 NeoForge 事件），因此本上下文允许 {@code entity} 为 null：
 * 玩家取不到时 {@link #getPlayer()} 返回 null，调用方（{@code SkillBundle.releaseSkills(type, env)}）
 * 仍以 {@code env.getPlayer()} 为准，不会因此中断。</p>
 *
 * <h2>物理结构（Sable / 航空学 sub-level）</h2>
 * <p>结构上的方块被搬到了虚拟子世界（plot），主世界在那个坐标上什么都没有，因此
 * <b>凡是"读方块状态"的地方都必须走 {@link #blockState(BlockPos)}</b>：
 * 上下文带结构句柄时读结构本地子世界（经 {@link SubLevelBridge}），否则读主世界。
 * 句柄类型是桥接接口自己的 {@code Hit}（<b>不</b>引入任何 Sable 类型），
 * 未装 Sable 时桥接为 null、句柄必为 null，行为与改动前逐字一致。</p>
 *
 * @since 1.0.0
 */
public class ExcavationSkillContext implements SkillContext {

    private final Level level;
    private final BlockPos pos;
    private final ItemStack tool;
    @Nullable
    private final LivingEntity entity;
    /** 本次视野/触发所在的结构句柄；null = 主世界普通方块（含"未装 Sable"的一切情况） */
    @Nullable
    private final SubLevelBridge.Hit subLevelHit;

    /** 旧签名（服务端释放路径、旧触发点用；不带结构句柄）。 */
    public ExcavationSkillContext(Level level, BlockPos pos, ItemStack tool, @Nullable LivingEntity entity) {
        this(level, pos, tool, entity, null);
    }

    /**
     * 带结构句柄的重载（客户端预览用）：{@code pos} 此时是<b>结构局部坐标</b>，
     * 方块读取必须走 {@link #blockState(BlockPos)}。
     */
    public ExcavationSkillContext(Level level, BlockPos pos, ItemStack tool, @Nullable LivingEntity entity,
                                  @Nullable SubLevelBridge.Hit subLevelHit) {
        this.level = level;
        this.pos = pos;
        this.tool = tool == null ? ItemStack.EMPTY : tool;
        this.entity = entity;
        this.subLevelHit = subLevelHit;
    }

    public Level level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }

    public ItemStack tool() {
        return tool;
    }

    @Nullable
    public LivingEntity entity() {
        return entity;
    }

    /** 结构句柄；null = 主世界普通方块（未装 Sable 时恒为 null）。 */
    @Nullable
    public SubLevelBridge.Hit subLevelHit() {
        return subLevelHit;
    }

    /** 本次上下文是否指向物理结构上的方块（未装 Sable 时恒 false）。 */
    public boolean onSubLevel() {
        return subLevelHit != null && SableBridges.get() != null;
    }

    /**
     * 读方块状态的<b>唯一入口</b>（技能策略一律用它，不要直接 {@code level().getBlockState(...)}）。
     *
     * <p>有结构句柄且桥接可用 → 读结构本地子世界（主世界在那些坐标上是空的，读主世界必然读错）；
     * 否则 → 主世界，与改动前完全一致。</p>
     */
    public BlockState blockState(BlockPos pos) {
        SubLevelBridge bridge = SableBridges.get();
        if (subLevelHit != null && bridge != null) {
            return bridge.getBlockState(subLevelHit, pos);
        }
        return level == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : level.getBlockState(pos);
    }

    /** 旧语义：破坏方格的实体即玩家（{@code SkillsComponent.resolvePlayer} 的挖掘分支）。 */
    @Override
    @Nullable
    public Player getPlayer() {
        return entity instanceof Player player ? player : null;
    }
}
