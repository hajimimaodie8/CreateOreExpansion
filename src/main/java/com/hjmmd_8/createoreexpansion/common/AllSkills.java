package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.helper.*;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class AllSkills {
    private static final Map<ResourceLocation, ItemSkill> SKILLS = new HashMap<>();
    private static final Map<ItemSkill, ResourceLocation> SKILL_IDS = new HashMap<>();

    // 公共技能实例 - 供物品注册时使用，确保使用相同的实例
    public static final JadeAxeAoeSkill JADE_AXE_AOE
            = skill(CreateOreExpansion.modLoc("jade_axe_aoe"), JadeAxeAoeSkill::new);
    public static final JadePickaxeAoeSkill JADE_PICKAXE_AOE
            = skill(CreateOreExpansion.modLoc("jade_pickaxe_aoe"), JadePickaxeAoeSkill::new);
    public static final JadeShovelAoeSkill JADE_SHOVEL_AOE
            = skill(CreateOreExpansion.modLoc("jade_shovel_aoe"), JadeShovelAoeSkill::new);

    public static final SapphireAxeAoeSkill SAPPHIRE_AXE_AOE
            = skill(CreateOreExpansion.modLoc("sapphire_axe_aoe"), SapphireAxeAoeSkill::new);
    public static final SapphirePickaxeAoeSkill SAPPHIRE_PICKAXE_AOE
            = skill(CreateOreExpansion.modLoc("sapphire_pickaxe_aoe"), SapphirePickaxeAoeSkill::new);
    public static final SapphireShovelAoeSkill SAPPHIRE_SHOVEL_AOE
            = skill(CreateOreExpansion.modLoc("sapphire_shovel_aoe"), SapphireShovelAoeSkill::new);

    public static final TopazAxeAoeSkill TOPAZ_AXE_AOE
            = skill(CreateOreExpansion.modLoc("topaz_axe_aoe"), TopazAxeAoeSkill::new);
    public static final TopazPickaxeAoeSkill TOPAZ_PICKAXE_AOE
            = skill(CreateOreExpansion.modLoc("topaz_pickaxe_aoe"), TopazPickaxeAoeSkill::new);
    public static final TopazShovelAoeSkill TOPAZ_SHOVEL_AOE
            = skill(CreateOreExpansion.modLoc("topaz_shovel_aoe"), TopazShovelAoeSkill::new);

    public static <T extends ItemSkill> T skill(ResourceLocation id, Supplier<T> factory) {
        T skill = factory.get();
        SKILLS.put(id, skill);
        SKILL_IDS.put(skill, id);
        return skill;
    }

    public static ItemSkill get(ResourceLocation id) {
        if (id == null) return null;
        return SKILLS.get(id);
    }

    public static ResourceLocation getId(ItemSkill skill) {
        if (skill == null) return null;
        return SKILL_IDS.get(skill);
    }

    public static void register() {}
}
