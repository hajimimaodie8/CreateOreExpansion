package com.hjmmd_8.createoreexpansion.content.skill.helper;

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

public class JadeAxeAoeHelper {

    private static final int SEARCH_RANGE = 8;
    private static final int MAX_BLOCKS = 200;

    public static Set<BlockPos> calculateTreeBlocks(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        result.add(startPos);

        while (!queue.isEmpty() && result.size() < MAX_BLOCKS) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos nb = current.offset(dx, dy, dz);
                        if (Math.abs(nb.getX() - startPos.getX()) > SEARCH_RANGE) continue;
                        if (Math.abs(nb.getY() - startPos.getY()) > SEARCH_RANGE) continue;
                        if (Math.abs(nb.getZ() - startPos.getZ()) > SEARCH_RANGE) continue;
                        if (result.contains(nb)) continue;
                        BlockState ns = level.getBlockState(nb);
                        if (ns.is(BlockTags.LOGS)) {
                            result.add(nb);
                            queue.add(nb);
                        }
                    }
                }
            }
        }

        if (result.size() >= MAX_BLOCKS) return new HashSet<>();
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
            BlockBreaker.breakPositions(toDestroy, pos, axe, level, player);
            ToolSkillCooldown.start(player, axe, 3);
        }
    }
}
