package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.util.AreaUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.hjmmd_8.createoreexpansion.CreateOreExpansion.LOGGER;

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
        Set<BlockPos> positions = new HashSet<>();
        var bb = AreaUtil.getAreaOfEffect(center, hit.getDirection(), width, height, depth);

        Iterator<BlockPos> it = BlockPos.betweenClosedStream(bb).iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next().immutable();
            if (!pos.equals(center)) {
                positions.add(pos);
            }
        }
        return positions;
    }
}
