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
 * 黄玉斧 — 连锁砍树入口
 *
 * <p>技能逻辑：按住 Shift 时，破坏原木触发 BFS 遍历（26 方向），
 * 将整棵树（全部相连的原木 + 树叶）连锁破坏。搜索范围 16x16x16，
 * 超过 {@link #MAX_TREE_BLOCKS}（200）个方块则不触发。</p>
 *
 * <p>使用的标签：{@link BlockTags#LOGS}（原木）、{@link BlockTags#LEAVES}（树叶）</p>
 *
 * <hr>
 * <h3>新建斧子技能需修改的参数：</h3>
 * <table>
 *   <tr><th>修改项</th><th>说明</th></tr>
 *   <tr><td>SEARCH_RANGE</td><td>搜索半径（8 = 16x16x16）</td></tr>
 *   <tr><td>MAX_TREE_BLOCKS</td><td>最大连锁方块数，超限不触发</td></tr>
 *   <tr><td>AllItems.xxx</td><td>改成对应物品注册名</td></tr>
 * </table>
 */
public class TopazAxeAoeHelper {

    /** 搜索半径（8 = 16x16x16 区域 = -8 ~ +8） */
    private static final int SEARCH_RANGE = 8;
    /** 超过此数量的树不触发连锁（太大挖不动） */
    

    /**
     * BFS 搜索相连的原木和树叶（26 方向，包含斜角）。
     * @return 树所含的全部方块集合；超出 MAX_TREE_BLOCKS 返回空集合
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

    /**
     * Mixin 入口。由 TopazAxeAoeMixin 在 destroyBlock() 时调用。
     * @param level      世界
     * @param pos        玩家点击的方块位置
     * @param state      方块状态
     * @param axe        当前手持工具
     * @param livingEntity 挖掘者
     */
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
            ToolSkillCooldown.start(player, axe, 5);
        }
    }
}
