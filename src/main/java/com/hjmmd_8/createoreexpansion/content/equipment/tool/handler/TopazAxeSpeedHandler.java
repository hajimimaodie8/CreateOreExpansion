package com.hjmmd_8.createoreexpansion.content.equipment.tool.handler;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemStackSkillHelper;
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

/**
 * 黄玉斧 — 挖掘速度修正
 *
 * <p>按住 Shift 挖掘原木时，根据树木大小延长挖掘时间：
 * <ul>
 *   <li>1-10 块：正常速度</li>
 *   <li>11-20 块：2 倍减速</li>
 *   <li>91-100+ 块：10 倍减速（封顶）</li>
 * </ul>
 * </p>
 *
 * <hr>
 * <h3>新建斧子技能需修改：</h3>
 * <ul>
 *   <li>AllItems.xxx → 对应物品注册名</li>
 *   <li>速度公式参考 multiplier 计算</li>
 * </ul>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class TopazAxeSpeedHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllItems.TOPAZ_AXE.get())) return;
        if (!ToolSkillCooldown.isReady(player, held)) return;
        if (!ItemStackSkillHelper.hasSkill(held, AllSkills.TOPAZ_AXE_AOE)) return;
        if (!ToolEnergy.canUseSkill(player, held, AllSkills.TOPAZ_AXE_AOE)) return;

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;

        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) return;

        Level level = player.level();
        Set<BlockPos> treeBlocks = AllSkills.TOPAZ_AXE_AOE.calculateTreeBlocks(level, pos);

                int treeSize = 0;
        for (BlockPos bp : treeBlocks) {
            if (level.getBlockState(bp).is(BlockTags.LOGS)) treeSize++;
        }
        if (treeSize <= 0) return;

        // 速度乘数 = 1 / ceil(树木大小 / 10)，封顶 10 倍减速
        float multiplier = 1.0f / Math.max(1, (float) Math.ceil(treeSize / 10.0));
        if (multiplier < 0.1f) multiplier = 0.1f;

        float newSpeed = event.getOriginalSpeed() * multiplier;
        event.setNewSpeed(newSpeed);
    }
}
