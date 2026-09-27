package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;

import java.util.List;

/**
 * 凋零诅咒（弓技能一）配置 —— 命中后附加凋零 + 缓慢 + 药水云。
 *
 * <p>数值来源统一为 {@link BowCurseConfigs}（集中修改点），本类只负责
 * 字段声明与 NBT 序列化。</p>
 */
public class BowCurseConfig extends AutoSkillConfig {

    /** 单次技能能量消耗 */
    public int energyCost;
    /** 技能冷却秒数 */
    public int cooldownSeconds;
    /** 凋零/缓慢基础持续 tick */
    public int durationTicks;
    /** 持续 tick 额外随机浮动（0~durationVariance） */
    public int durationVariance;
    /** 升级为 II 级效果的概率（0~1） */
    public float upgradeChance;
    /** 药水云半径 */
    public float cloudRadius;
    /** 药水云缓慢效果等级（0 = I 级） */
    public int cloudSlowAmplifier;

    public BowCurseConfig(int energyCost, int cooldownSeconds, int durationTicks, int durationVariance,
                          float upgradeChance, float cloudRadius, int cloudSlowAmplifier) {
        this.energyCost = energyCost;
        this.cooldownSeconds = cooldownSeconds;
        this.durationTicks = durationTicks;
        this.durationVariance = durationVariance;
        this.upgradeChance = upgradeChance;
        this.cloudRadius = cloudRadius;
        this.cloudSlowAmplifier = cloudSlowAmplifier;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("Cost", () -> energyCost, v -> energyCost = v),
                ofInt("Cooldown", () -> cooldownSeconds, v -> cooldownSeconds = v),
                ofInt("Duration", () -> durationTicks, v -> durationTicks = v),
                ofInt("DurationVariance", () -> durationVariance, v -> durationVariance = v),
                ofFloat("UpgradeChance", () -> upgradeChance, v -> upgradeChance = v),
                ofFloat("CloudRadius", () -> cloudRadius, v -> cloudRadius = v),
                ofInt("CloudSlow", () -> cloudSlowAmplifier, v -> cloudSlowAmplifier = v)
        );
    }
}
