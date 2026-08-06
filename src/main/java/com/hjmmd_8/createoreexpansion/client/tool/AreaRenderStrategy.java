package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.util.AreaUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * AOE 范围渲染策略 - 用于镐和铲的范围挖掘
 */
public class AreaRenderStrategy implements RenderStrategy {
    private final int radius;
    private final int depth;

    public AreaRenderStrategy(int radius, int depth) {
        this.radius = radius;
        this.depth = depth;
    }

    @Override
    public Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player) {
        Set<BlockPos> positions = new HashSet<>();
        var bb = AreaUtil.getAreaOfEffect(center, hit.getDirection(), radius, depth);

        Iterator<BlockPos> it = BlockPos.betweenClosedStream(bb).iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (!pos.equals(center)) {
                positions.add(pos);
            }
        }
        return positions;
    }
}
