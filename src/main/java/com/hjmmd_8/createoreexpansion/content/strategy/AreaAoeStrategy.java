package com.hjmmd_8.createoreexpansion.content.strategy;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.BreakBlockSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 范围AOE策略 - 用于镐子和铲子的范围挖掘
 *
 * <p>支持：
 * <ul>
 *   <li>平面范围 - 3x3, 5x5 等</li>
 *   <li>深度范围 - 多层挖掘</li>
 *   <li>线性挖掘 - 一列挖掘</li>
 * </ul>
 *
 * <p>基于 {@link DualDirection} 计算范围，与实际挖掘行为一致。
 */
public class AreaAoeStrategy implements AreaStrategy {

    private final int width;
    private final int height;
    private final int depth;
    private final DualDirection.From directionSource;

    /**
     * 创建范围AOE策略
     * @param width 横向宽度（如 3 表示 3 格宽）
     * @param height 纵向高度（如 3 表示 3 格高）
     * @param depth 挖掘深度（如 1 表示 1 格深）
     */
    public AreaAoeStrategy(int width, int height, int depth) {
        this(width, height, depth, DualDirection.From.BLOCK_FACE);
    }

    /**
     * 创建范围AOE策略（指定方向源）
     * @param width 横向宽度
     * @param height 纵向高度
     * @param depth 挖掘深度
     * @param directionSource 方向源（基于方块面或玩家偏航角）
     */
    public AreaAoeStrategy(int width, int height, int depth, DualDirection.From directionSource) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.directionSource = directionSource;
    }

    /**
     * 简化构造器 - 宽高相同
     */
    public AreaAoeStrategy(int size, int depth) {
        this(size, size, depth, DualDirection.From.BLOCK_FACE);
    }

    @Override
    public Set<BlockPos> calculatePositions(ItemSkill skill, BlockPos center, BlockHitResult hit, Player player) {
        DualDirection dualDirection = DualDirection.from(player, hit, directionSource);
        return dualDirection.collect(center, width, height, depth);
    }

    @Override
    public boolean shouldRender(ItemSkill skill, ClientLevel world, BlockPos pos, BlockState state, Player player) {
        if (!(skill instanceof BreakBlockSkill<?, ?> breakSkill)) return false;
        return state.is(breakSkill.mineableTag);
    }

    // Getters
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
    public DualDirection.From getDirectionSource() { return directionSource; }
}
