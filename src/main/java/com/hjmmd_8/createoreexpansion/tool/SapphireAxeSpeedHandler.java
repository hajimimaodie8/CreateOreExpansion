package com.hjmmd_8.createoreexpansion.tool;

import com.hjmmd_8.createoreexpansion.content.skill.helper.SapphireAxeAoeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.hjmmd_8.createoreexpansion.common.AllMyItems;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import java.util.Set;

/**
 * 蓝宝石斧 - 挖掘速度修正
 *
 * <p>按住 Shift 挖掘原木时，根据树干数量延长挖掘时间：
 * <ul>
 *   <li>1-15 块：正常速度</li>
 *   <li>16-30 块：2 倍减速</li>
 *   <li>136-150+ 块：5 倍减速（封顶）</li>
 * </ul>
 * </p>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class SapphireAxeSpeedHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllMyItems.SAPPHIRE_AXE.get())) return;
        if (!ToolSkillCooldown.isReady(player, held)) return;
        if (ToolEnergy.hasEnergy(held) && !ToolEnergy.canUseSkill(player, held)) {
            ToolEnergy.sendLowEnergy(player);
            return;
        }

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;

        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) return;

        Level level = player.level();
        Set<BlockPos> tree = SapphireAxeAoeHelper.calculateTreeBlocks(level, pos);

        // 只计原木数，不计树叶
        int logCount = 0;
        for (BlockPos bp : tree) {
            if (level.getBlockState(bp).is(BlockTags.LOGS)) logCount++;
        }
        if (logCount <= 0) return;

        // 每 15 个原木一档减速，封顶 5 倍
        float multiplier = 1.0f / Math.max(1, (float) Math.ceil(logCount / 15.0));
        if (multiplier < 1.0f / 5.0f) multiplier = 1.0f / 5.0f;

        event.setNewSpeed(event.getOriginalSpeed() * multiplier);
    }
}
