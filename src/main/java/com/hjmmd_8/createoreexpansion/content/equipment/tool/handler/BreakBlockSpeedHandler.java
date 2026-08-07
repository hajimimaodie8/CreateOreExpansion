package com.hjmmd_8.createoreexpansion.content.equipment.tool.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllModifiableAttributes;
import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.FellingSkill;
import com.hjmmd_8.createoreexpansion.content.skill.attribute.BreakBlockSpeedModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemStackSkillHelper;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.AttributeModifierCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class BreakBlockSpeedHandler {
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;

        ItemStack held = player.getMainHandItem();
        if (!ItemStackSkillHelper.hasSkill(held, SkillType.EXCAVATION_SKILL)) return;
        if (!ToolSkillCooldown.isReady(player, held)) return;

        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;

        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) return;

        Level level = player.level();
        AttributeModifierCollector collector = new AttributeModifierCollector();

        for (ItemSkill skill : ItemStackSkillHelper.getSkills(held, SkillType.EXCAVATION_SKILL)) {
            if (skill instanceof FellingSkill fellingSkill) {
                collector.collect(fellingSkill);
            }
        }
        if (collector.count == 0) return;
        Float newSpeed = AllSkills.modifier(AllModifiableAttributes.BREAK_BLOCK_SPEED, collector,
                BreakBlockSpeedModifiableAttribute.ctx(level, pos, event.getOriginalSpeed()));

        event.setNewSpeed(newSpeed);
    }
}
