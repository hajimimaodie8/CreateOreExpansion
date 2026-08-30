package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * 凋零诅咒（弓技能一）—— 统一配置类（能量 / 冷却 / 效果持续 / 升级概率 / 药水云 集中修改点）。
 *
 * <p>所有使用 {@code BowCurseSkill} 的凋零诅咒技能的效果数值都在本文件统一定义，
 * {@code AllSkills} / {@code AllItems} 只负责引用，不改数值。</p>
 *
 * <p>等级说明（等级越高效果越强）：</p>
 * <ul>
 *     <li>Lv1 —— 持续 5~7.5s，40% 升级 II，药水云半径 1.5、缓慢 II；冷却 5s</li>
 *     <li>Lv2 —— 持续 6~9s，50% 升级 II，药水云半径 1.5、缓慢 III；冷却 5s</li>
 *     <li>Lv3 —— 持续 7~10.5s，55% 升级 II，药水云半径 2.0、缓慢 III；冷却 4s</li>
 *     <li>Lv4 —— 持续 8~12s，60% 升级 II，药水云半径 2.0、缓慢 IV；冷却 4s</li>
 *     <li>Lv5 —— 持续 10~15s，70% 升级 II，药水云半径 2.5、缓慢 IV；冷却 3s</li>
 * </ul>
 */
public final class BowCurseConfigs {

    private BowCurseConfigs() {
    }

    /**
     * 单级凋零诅咒定义。
     *
     * @param energyCost       单次技能能量消耗
     * @param cooldownSeconds  技能冷却秒数
     * @param durationTicks    凋零/缓慢基础持续 tick
     * @param durationVariance 持续 tick 额外随机浮动（0~durationVariance）
     * @param upgradeChance    升级为 II 级效果的概率（0~1）
     * @param cloudRadius      药水云半径
     * @param cloudSlowAmplifier 药水云缓慢效果等级（0 = I 级）
     */
    public record Level(int energyCost, int cooldownSeconds, int durationTicks, int durationVariance,
                        float upgradeChance, float cloudRadius, int cloudSlowAmplifier) {
    }

    // ========== 五个等级（Lv1/Lv2/Lv3 已绑定，Lv4/Lv5 预留） ==========

    /** Lv1 —— 持续 5~7.5s，40% 升级 II，云 1.5/缓慢 II；冷却 5s */
    public static final Level LEVEL_1 = new Level(100, 5, 100, 50, 0.40F, 1.5F, 1);

    /** Lv2 —— 持续 6~9s，50% 升级 II，云 1.5/缓慢 III；冷却 5s */
    public static final Level LEVEL_2 = new Level(100, 5, 120, 60, 0.50F, 1.5F, 2);

    /** Lv3 —— 持续 7~10.5s，55% 升级 II，云 2.0/缓慢 III；冷却 4s */
    public static final Level LEVEL_3 = new Level(100, 4, 140, 70, 0.55F, 2.0F, 2);

    /** Lv4 预留 —— 持续 8~12s，60% 升级 II，云 2.0/缓慢 IV；冷却 4s */
    public static final Level LEVEL_4 = new Level(100, 4, 160, 80, 0.60F, 2.0F, 3);

    /** Lv5 预留 —— 持续 10~15s，70% 升级 II，云 2.5/缓慢 IV；冷却 3s */
    public static final Level LEVEL_5 = new Level(100, 3, 200, 100, 0.70F, 2.5F, 3);

    /**
     * 由等级定义构造 {@link BowCurseConfig}，供 {@code AllSkills} 注册凋零诅咒技能使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static BowCurseConfig config(Level level) {
        return new BowCurseConfig(level.energyCost(), level.cooldownSeconds(), level.durationTicks(),
                level.durationVariance(), level.upgradeChance(), level.cloudRadius(), level.cloudSlowAmplifier());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 凋零诅咒按等级取配置（Lv1~5） */
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
