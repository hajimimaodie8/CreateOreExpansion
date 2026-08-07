package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.FellingSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttributeType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SkillAttributeModifierHolder;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class AllSkills {
    private static final Map<ResourceLocation, ItemSkill> SKILLS = new HashMap<>();
    private static final Map<ItemSkill, ResourceLocation> SKILL_IDS = new HashMap<>();

    // 公共技能实例 - 供物品注册时使用，确保使用相同的实例
    // 斧头技能 - 使用 FellingSkill
    public static final FellingSkill JADE_AXE_AOE
            = skill(CreateOreExpansion.modLoc("jade_axe_aoe"),
            () -> new FellingSkill(8, 200, 0, FellingSkill.IS_LOG));
    public static final FellingSkill SAPPHIRE_AXE_AOE
            = skill(CreateOreExpansion.modLoc("sapphire_axe_aoe"),
            () -> new FellingSkill(8, 100, FellingSkill.IS_TREE));
    public static final FellingSkill TOPAZ_AXE_AOE
            = skill(CreateOreExpansion.modLoc("topaz_axe_aoe"),
            () -> new FellingSkill(12, 100, FellingSkill.IS_TREE));

    // 镐子技能 - 使用 AreaAoeSkill
    public static final AreaAoeSkill JADE_PICKAXE_AOE
            = skill(CreateOreExpansion.modLoc("jade_pickaxe_aoe"),
            () -> new AreaAoeSkill(3, 1, 1, 0, BlockTags.MINEABLE_WITH_PICKAXE, DualDirection.fromPlayerYaw()));
    public static final AreaAoeSkill SAPPHIRE_PICKAXE_AOE
            = skill(CreateOreExpansion.modLoc("sapphire_pickaxe_aoe"),
            () -> new AreaAoeSkill(5, 5, 1, 50, BlockTags.MINEABLE_WITH_PICKAXE));
    public static final AreaAoeSkill TOPAZ_PICKAXE_AOE
            = skill(CreateOreExpansion.modLoc("topaz_pickaxe_aoe"),
            () -> new AreaAoeSkill(3, 3, 1, 50, BlockTags.MINEABLE_WITH_PICKAXE));

    // 铲子技能 - 使用 AreaAoeSkill
    public static final AreaAoeSkill JADE_SHOVEL_AOE
            = skill(CreateOreExpansion.modLoc("jade_shovel_aoe"),
            () -> new AreaAoeSkill(1, 1, 6, 0, BlockTags.MINEABLE_WITH_SHOVEL));
    public static final AreaAoeSkill SAPPHIRE_SHOVEL_AOE
            = skill(CreateOreExpansion.modLoc("sapphire_shovel_aoe"),
            () -> new AreaAoeSkill(7, 1, 1, 50, BlockTags.MINEABLE_WITH_SHOVEL));
    public static final AreaAoeSkill TOPAZ_SHOVEL_AOE
            = skill(CreateOreExpansion.modLoc("topaz_shovel_aoe"),
            () -> new AreaAoeSkill(1, 1, 8, 50, BlockTags.MINEABLE_WITH_SHOVEL));

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

    public static <C, V> V modifier(ModifiableAttributeType<C, V> type, SkillAttributeModifierHolder holder, C context) {
        ModifiableAttribute<V> attribute = type.create(context);
        holder.modifier(type, attribute);
        return attribute.getValue();
    }

    public static void register() {}
}
