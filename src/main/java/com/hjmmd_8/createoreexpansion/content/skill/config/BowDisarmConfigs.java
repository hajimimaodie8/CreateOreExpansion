package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * 缴械风暴（弓技能二）—— 统一配置类（能量 / 冷却 / 范围 / 进包概率 / 扒防具 集中修改点）。
 *
 * <p>所有使用 {@code BowDisarmSkill} 的缴械风暴技能的效果数值都在本文件统一定义，
 * {@code AllSkills} / {@code AllItems} 只负责引用，不改数值。</p>
 *
 * <p>等级说明（等级越高范围越大、进包概率越高）：</p>
 * <ul>
 *     <li>Lv1 —— 3×3，50% 武器进背包，怪物扒防具；冷却 7s</li>
 *     <li>Lv2 —— 3×3，55% 武器进背包，怪物扒防具；冷却 6s</li>
 *     <li>Lv3 —— 3×3，60% 武器进背包，怪物扒防具；冷却 5s</li>
 *     <li>Lv4 —— 5×5，65% 武器进背包，怪物扒防具；冷却 5s</li>
 *     <li>Lv5 —— 5×5，75% 武器进背包，怪物扒防具；冷却 4s</li>
 * </ul>
 */
public final class BowDisarmConfigs {

    private BowDisarmConfigs() {
    }

    /**
     * 单级缴械风暴定义。
     *
     * @param energyCost        单次技能能量消耗
     * @param cooldownSeconds   技能冷却秒数
     * @param rangeRadius       作用范围半径（AABB inflate 值，1.0 = 3×3，2.0 = 5×5）
     * @param intoInventoryChance 主手武器进入射手背包的概率（0~1，剩余概率掉落在地）
     * @param stripMonsterArmor 掉落模式下是否对怪物额外扒副手 + 全部防具
     */
    public record Level(int energyCost, int cooldownSeconds, float rangeRadius,
                        float intoInventoryChance, boolean stripMonsterArmor) {
    }

    // ========== 五个等级（Lv1/Lv2/Lv3 已绑定，Lv4/Lv5 预留） ==========

    /** Lv1 —— 3×3，50% 进包；冷却 7s */
    public static final Level LEVEL_1 = new Level(100, 7, 1.0F, 0.50F, true);

    /** Lv2 —— 3×3，55% 进包；冷却 6s */
    public static final Level LEVEL_2 = new Level(100, 6, 1.0F, 0.55F, true);

    /** Lv3 —— 3×3，60% 进包；冷却 5s */
    public static final Level LEVEL_3 = new Level(100, 5, 1.0F, 0.60F, true);

    /** Lv4 预留 —— 5×5，65% 进包；冷却 5s */
    public static final Level LEVEL_4 = new Level(100, 5, 2.0F, 0.65F, true);

    /** Lv5 预留 —— 5×5，75% 进包；冷却 4s */
    public static final Level LEVEL_5 = new Level(100, 4, 2.0F, 0.75F, true);

    /**
     * 由等级定义构造 {@link BowDisarmConfig}，供 {@code AllSkills} 注册缴械风暴技能使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static BowDisarmConfig config(Level level) {
        return new BowDisarmConfig(level.energyCost(), level.cooldownSeconds(), level.rangeRadius(),
                level.intoInventoryChance(), level.stripMonsterArmor());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 缴械风暴按等级取配置（Lv1~5） */
    public static Level level(int level) {
        return switch (level) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            case 4 -> LEVEL_4;
            default -> LEVEL_5;
        };
    }
}
