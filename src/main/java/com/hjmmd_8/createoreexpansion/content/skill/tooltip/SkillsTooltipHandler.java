package com.hjmmd_8.createoreexpansion.content.skill.tooltip;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.data.lang.COELangProvider;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public class SkillsTooltipHandler {

    private static final String SKILL_TRANSLATE_KEY = "item.createoreexpansion.tool.skill";
    private static final String SKILL_TIP_TRANSLATE_KEY = "item.createoreexpansion.tool.skill_tips";

    public static void addSkillsTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        SkillItemStack skillStack = SkillItemStack.of(stack);

        if (!skillStack.hasSkill())
            return;

        if (Screen.hasAltDown()) {
            event.getToolTip().add(1, Component.translatable(SKILL_TRANSLATE_KEY, AllKeys.SKILL_RELEASE.getBoundKey())
                    .withStyle(ChatFormatting.GRAY));

            int index = 2;
            List<ItemSkill> skills = skillStack.getSkillsHolder().getAllSkills();
            for (ItemSkill skill : skills) {
                event.getToolTip().add(index, Component.literal("  ")
                        .append(Component.translatable(skill.getTranslateKey()))
                        .append(" - ")
                        .append(Component.translatable(skill.getType().translatable.getTranslateKey()))
                );
                index++;
            }
        } else {
            event.getToolTip().add(1, Component.translatable(SKILL_TIP_TRANSLATE_KEY)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static COELangProvider.Builder translate(COELangProvider.Builder builder) {
        return builder
                .add(SKILL_TRANSLATE_KEY,
                        "技能: ", "Skills: ")
                .add(SKILL_TIP_TRANSLATE_KEY,
                        "按住 [Alt] 可查看技能概要", "Hold [Alt] for Skills Summary")
                ;
    }
}
