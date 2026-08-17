package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.data.lang.Translatable;
import net.minecraft.resources.ResourceLocation;

public interface ItemSkill extends Translatable {

    /**
     * 释放技能。技能实现应在真正生效前自行调用
     * {@link com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy#tryConsume} 消耗能量。
     *
     * @param context 技能上下文
     * @param data    技能data
     * @throws ClassCastException 技能上下文类型错误
     */
    void release(Object context, DataSkill data);

    /**
     * 检查技能是否可以释放（在消耗能量前调用）
     *
     * @param context 技能上下文
     * @param data    技能data
     * @return true=可以释放，false=条件不满足
     */
    default boolean canRelease(Object context, DataSkill data) {
        return true; // 默认总是可以释放
    }

    SkillType getType();

    default int getCost() {
        return 100;
    }

    /**
     * 技能自身的冷却秒数（0 = 无自定义，由调用方使用默认/注册表冷却）。
     * 剑类双技能各自返回独立冷却，互不影响。
     */
    default int getCooldownSeconds() {
        return 0;
    }

    @Override
    default String getTranslateKey() {
        ResourceLocation id = AllSkills.getId(this);
        String path = id.getPath();
        // 等级前缀归一：great_/grand_ 统一映射到基础技能名（技能等级由 tooltip 罗马数字显示，
        // 无需为每个等级单独定义翻译键）。如 great_fell / grand_fell → 查 skill.*.fell。
        if (path.startsWith("great_")) {
            path = path.substring("great_".length());
        } else if (path.startsWith("grand_")) {
            path = path.substring("grand_".length());
        }
        return "skill." + id.getNamespace() + "." + path;
    }
}
