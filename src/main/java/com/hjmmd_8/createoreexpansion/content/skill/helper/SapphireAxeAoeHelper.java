package com.hjmmd_8.createoreexpansion.content.skill.helper;

import com.hjmmd_8.createoreexpansion.tool.ToolEnergy;
import com.hjmmd_8.createoreexpansion.tool.ToolSkillCooldown;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * 蓝宝石斧 - 连锁砍树入口（含树叶）
 *
 * <p>按住 Shift 时，破坏原木触发 BFS 遍历（26 方向），
 * 将整棵树（全部相连的原木 + 树叶）连锁破坏。
 * 搜索范围 24x24x24，无方块上限。</p>
 *
 * <hr>
 * <h3>新工具适配参数</h3>
 * <ul>
 *   <li>SEARCH_RANGE — 搜索半径（12 = 24x24x24）</li>
 *   <li>BFS 条件 — BlockTags.LOGS / BlockTags.LEAVES</li>
 * </ul>
 */
public class SapphireAxeAoeHelper {

    private static final int SEARCH_RANGE = 12;

    /**
     * BFS 搜索相连的原木和树叶（26 方向，含斜角）。
     * 无方块上限，整棵树全部遍历。
     */
    public static Set<BlockPos> calculateTreeBlocks(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        result.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (Math.abs(neighbor.getX() - startPos.getX()) > SEARCH_RANGE) continue;
                        if (Math.abs(neighbor.getY() - startPos.getY()) > SEARCH_RANGE) continue;
                        if (Math.abs(neighbor.getZ() - startPos.getZ()) > SEARCH_RANGE) continue;
                        if (result.contains(neighbor)) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.is(BlockTags.LOGS) || neighborState.is(BlockTags.LEAVES)) {
                            result.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack axe, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!player.isShiftKeyDown()) return;
        if (!state.is(BlockTags.LOGS)) return;
        if (!ToolSkillCooldown.isReady(player, axe)) return;

        Set<BlockPos> toDestroy = calculateTreeBlocks(level, pos);
        if (toDestroy.size() > 1) {
            if (ToolEnergy.hasEnergy(axe) && !ToolEnergy.consumeForSkill(player, axe, ToolEnergy.AXE_COST))
                return;
            BlockBreaker.breakPositions(toDestroy, pos, axe, level, player);
            ToolSkillCooldown.start(player, axe, 8);
        }
    }
}
