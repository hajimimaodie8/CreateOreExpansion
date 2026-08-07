package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * AOE 范围渲染策略 - 用于镐和铲的范围挖掘
 */
public class AreaRenderStrategy implements RenderStrategy {
    private final int width;
    private final int height;
    private final int depth;

    /**
     * 创建范围渲染策略
     * @param width 横向宽度（如 3 表示 3 格宽）
     * @param height 纵向高度（如 3 表示 3 格高）
     * @param depth 挖掘深度（如 1 表示 1 格深）
     */
    public AreaRenderStrategy(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    /**
     * 简化构造器 - 宽高相同
     */
    public AreaRenderStrategy(int size, int depth) {
        this(size, size, depth);
    }

    @Override
    public Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player) {
        // 使用DualDirection的collect方法直接收集所有位置
        DualDirection dualDirection = DualDirection.fromBlockFace(player, hit);
        return dualDirection.collect(center, width, height, depth);
    }
}
