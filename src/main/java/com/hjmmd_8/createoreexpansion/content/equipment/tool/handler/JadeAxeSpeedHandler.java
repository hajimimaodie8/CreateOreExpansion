package com.hjmmd_8.createoreexpansion.content.equipment.tool.handler;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import java.util.Set;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class JadeAxeSpeedHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllItems.JADE_AXE.get())) return;
        if (!ToolSkillCooldown.isReady(player, held)) return;

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;

        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) return;

        Level level = player.level();
        Set<BlockPos> treeBlocks = AllSkills.JADE_AXE_AOE.calculateTreeBlocks(level, pos);

                int treeSize = 0;
        for (BlockPos bp : treeBlocks) {
            if (level.getBlockState(bp).is(BlockTags.LOGS)) treeSize++;
        }
        if (treeSize <= 0) return;

        // 翡翠工具等级低：每 5 块一个档位，封顶 15 倍
        float multiplier = 1.0f / Math.max(1, (float) Math.ceil(treeSize / 5.0));
        if (multiplier < 1.0f / 15.0f) multiplier = 1.0f / 15.0f;

        float newSpeed = event.getOriginalSpeed() * multiplier;
        event.setNewSpeed(newSpeed);
    }
}
