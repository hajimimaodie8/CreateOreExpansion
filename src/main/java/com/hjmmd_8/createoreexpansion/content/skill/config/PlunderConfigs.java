package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * 夺取（剑类夺取装备 + 吸血）技能 —— 统一配置类（装备夺取概率 / 吸血 / 吸收 / 能量 / 冷却 集中修改点）。
 *
 * 所有使用 {@code PlunderSkill} 的夺取技能（剑）的数值都在本文件统一定义，
 * {@code AllSkills} / {@code AllItems} 只负责引用，不改数值。
 *
 * 等级说明（冷却首项 4 公差 2；吸血首项 3 公差 1；吸收时长首项 3 公差 2）：
 * <ul>
 *     <li>Lv1 —— 装备：50% 空 / 25% 武器 / 25% 装备；吸血 3；吸收II·3秒；冷却 4 秒</li>
 *     <li>Lv2 —— 装备：40% 空 / 30% 武器 / 30% 装备；吸血 4；吸收II·5秒；冷却 6 秒</li>
 *     <li>Lv3 —— 装备：30% 空 / 30% 武器 / 30% 装备 / 10% 两者；吸血 5；吸收III·7秒；冷却 8 秒</li>
 *     <li>Lv4 —— 装备：武器 100% 夺 + 装备 50% 夺；吸血 6；吸收III·9秒；冷却 10 秒</li>
 *     <li>Lv5 —— 武器 100% 收缴进背包（满则掉落）；装备：25% 收缴进背包 / 50% 只夺不收缴 / 25% 空；
 *     吸血 7；吸收IV·11秒；冷却 12 秒</li>
 * </ul>
 */
public final class PlunderConfigs {

    private PlunderConfigs() {
    }

    /** 处置方式字符串：掉落物 */
    public static final String DISP_DROP = "DROP";
    /** 处置方式字符串：收缴进背包（满则掉落） */
    public static final String DISP_PICKUP = "PICKUP";

    /**
     * 单级夺取技能定义。
     *
     * @param plunderWeaponWeight 互斥权重：夺武器（Lv1~3）
     * @param plunderArmorWeight  互斥权重：夺装备（Lv1~3）
     * @param plunderBothWeight   互斥权重：同时夺武器+装备（Lv1~3）
     * @param alwaysWeapon        武器必夺（Lv4~5）
     * @param armorChance         装备独立夺取概率 0~1（Lv4~5）
     * @param weaponDisposition   武器处置方式（DROP/PICKUP）
     * @param armorDisposition    装备处置方式（DROP/PICKUP）
     * @param lifestealAmount     吸血量（敌人扣血 = 自身回血）
     * @param absorptionTicks     满血时伤害吸收时长（tick）
     * @param absorptionAmplifier 伤害吸收等级（0=吸收I ... 3=吸收IV）
     * @param cooldownSeconds     技能冷却秒数
     * @param energyCost          单次技能能量消耗
     */
    public record Level(int plunderWeaponWeight, int plunderArmorWeight, int plunderBothWeight,
                        boolean alwaysWeapon, float armorChance,
                        String weaponDisposition, String armorDisposition,
                        int lifestealAmount, int absorptionTicks, int absorptionAmplifier,
                        int cooldownSeconds, int energyCost) {
    }

    // ========== 五个等级（Lv1/Lv2 已绑定，Lv3~Lv5 预留） ==========

    /** Lv1 黄玉剑 —— 50%空/25%武器/25%装备；吸血3；吸收II·3秒；冷却4秒 */
    public static final Level LEVEL_1 = new Level(25, 25, 0, false, 0F,
            DISP_DROP, DISP_DROP, 3, 3 * 20, 1, 4, 100);

    /** Lv2 蓝宝石剑 —— 40%空/30%武器/30%装备；吸血4；吸收II·5秒；冷却6秒 */
    public static final Level LEVEL_2 = new Level(30, 30, 0, false, 0F,
            DISP_DROP, DISP_DROP, 4, 5 * 20, 1, 6, 100);

    /** Lv3 预留 —— 30%空/30%武器/30%装备/10%两者；吸血5；吸收III·7秒；冷却8秒 */
    public static final Level LEVEL_3 = new Level(30, 30, 10, false, 0F,
            DISP_DROP, DISP_DROP, 5, 7 * 20, 2, 8, 100);

    /** Lv4 预留 —— 武器100%夺 + 装备50%夺；吸血6；吸收III·9秒；冷却10秒 */
    public static final Level LEVEL_4 = new Level(0, 0, 0, true, 0.5F,
            DISP_DROP, DISP_DROP, 6, 9 * 20, 2, 10, 100);

    /** Lv5 预留 —— 武器100%收缴背包；装备25%收缴/50%只夺/25%空；吸血7；吸收IV·11秒；冷却12秒 */
    public static final Level LEVEL_5 = new Level(0, 0, 0, true, 0.75F,
            DISP_PICKUP, DISP_PICKUP, 7, 11 * 20, 3, 12, 100);

    /**
     * 由等级定义构造 {@link PlunderConfig}，供 {@code AllSkills} 注册夺取技能使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static PlunderConfig config(Level level) {
        return new PlunderConfig(level.plunderWeaponWeight(), level.plunderArmorWeight(), level.plunderBothWeight(),
                level.alwaysWeapon(), level.armorChance(),
                level.weaponDisposition(), level.armorDisposition(),
                level.lifestealAmount(), level.absorptionTicks(), level.absorptionAmplifier(),
                level.cooldownSeconds(), level.energyCost());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 夺取按等级取配置（Lv1~5） */
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
