package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import net.minecraft.world.item.ItemStack;

public class SkillItemStack {
    protected final ItemStack stack;
    protected boolean hasSkill = true;
    protected SkillsComponent component;

    protected SkillItemStack(ItemStack stack) {
        this.stack = stack;
        component = stack.get(AllDataComponents.SKILLS);
        if (component == null) hasSkill = false;
    }

    public ItemStack itemStack() {
        return stack;
    }

    public boolean hasSkill() {
        return hasSkill;
    }

    public boolean hasSkill(SkillType type) {
        return hasSkill && component.hasSkill(type);
    }

    public boolean hasSkill(ItemSkill skill) {
        return hasSkill && component.hasSkill(skill.getType()) && component.hasSkill(skill);
    }

    public SkillsComponent getSkillsHolder() {
        return component;
    }

    public static SkillItemStack of(ItemStack stack) {
        return new SkillItemStack(stack);
    }
}
