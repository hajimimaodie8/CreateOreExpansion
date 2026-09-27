package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;

import java.util.List;

/**
 * 缴械风暴（弓技能二）配置 —— 命中后以目标为中心范围缴械 + 怪物扒装备。
 *
 * <p>数值来源统一为 {@link BowDisarmConfigs}（集中修改点），本类只负责
 * 字段声明与 NBT 序列化。</p>
 */
public class BowDisarmConfig extends AutoSkillConfig {

    /** 单次技能能量消耗 */
    public int energyCost;
    /** 技能冷却秒数 */
    public int cooldownSeconds;
    /** 作用范围半径（AABB inflate 值，1.0 = 3×3，2.0 = 5×5） */
    public float rangeRadius;
    /** 主手武器进入射手背包的概率（0~1，剩余概率掉落在地） */
    public float intoInventoryChance;
    /** 掉落模式下是否对怪物额外扒副手 + 全部防具 */
    public boolean stripMonsterArmor;

    public BowDisarmConfig(int energyCost, int cooldownSeconds, float rangeRadius,
                           float intoInventoryChance, boolean stripMonsterArmor) {
        this.energyCost = energyCost;
        this.cooldownSeconds = cooldownSeconds;
        this.rangeRadius = rangeRadius;
        this.intoInventoryChance = intoInventoryChance;
        this.stripMonsterArmor = stripMonsterArmor;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("Cost", () -> energyCost, v -> energyCost = v),
                ofInt("Cooldown", () -> cooldownSeconds, v -> cooldownSeconds = v),
                ofFloat("Range", () -> rangeRadius, v -> rangeRadius = v),
                ofFloat("IntoInventoryChance", () -> intoInventoryChance, v -> intoInventoryChance = v),
                ofBool("StripMonsterArmor", () -> stripMonsterArmor, v -> stripMonsterArmor = v)
        );
    }
}
