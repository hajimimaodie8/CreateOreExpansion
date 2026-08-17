package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * 剥取（剑类额外掉落）技能 —— 统一配置类（掉落概率 / 掉落数量分布 / 能量 / 冷却 集中修改点）。
 *
 * 所有使用 {@code SkinSkill} 的剥取技能（剑）的掉落概率、掉落数量分布、
 * 能量消耗、冷却时间都在本文件统一定义，{@code AllSkills} / {@code AllItems}
 * 只负责引用，不改数值。
 *
 * 等级说明：
 * <ul>
 *     <li>Lv1 翡翠剑 —— 掉落概率 65%，固定掉 1 个</li>
 *     <li>Lv2 黄玉剑 —— 掉落概率 72%，固定掉 1 个</li>
 *     <li>Lv3 蓝宝石剑 —— 掉落概率 80%，30% 掉 0 / 35% 掉 1 / 35% 掉 2</li>
 *     <li>Lv4 —— 掉落概率 87%，25% 掉 0 / 25% 掉 1 / 50% 掉 2（预留）</li>
 *     <li>Lv5 —— 掉落概率 95%，25% 掉 1 / 50% 掉 2 / 25% 掉 3（预留）</li>
 * </ul>
 *
 * 掉落概率从 Lv1~Lv5 线性递增插值（65→72→80→87→95%）；冷却 3/5/7/9/10 秒。
 */
public final class SkinConfigs {

    private SkinConfigs() {
    }

    /**
     * 单级剥取技能定义。
     *
     * @param dropChance    本次技能是否掉落的概率（0~1）
     * @param energyCost    单次技能能量消耗
     * @param cooldownSeconds 技能冷却秒数（创造模式无冷却）
     * @param drop0Weight   掉落数量分布权重：掉 0 个
     * @param drop1Weight   掉落数量分布权重：掉 1 个
     * @param drop2Weight   掉落数量分布权重：掉 2 个
     * @param drop3Weight   掉落数量分布权重：掉 3 个
     */
    public record Level(float dropChance, int energyCost, int cooldownSeconds,
                        int drop0Weight, int drop1Weight, int drop2Weight, int drop3Weight) {
    }

    // ========== 五个等级（Lv4/Lv5 预留，将来启用时在 AllSkills 注册对应技能即可） ==========

    /** Lv1 翡翠剑 —— 掉落概率 65%，固定掉 1 个；冷却 3 秒 */
    public static final Level LEVEL_1 = new Level(0.65F, 100, 3, 0, 100, 0, 0);

    /** Lv2 黄玉剑 —— 掉落概率 72%，固定掉 1 个；冷却 5 秒 */
    public static final Level LEVEL_2 = new Level(0.72F, 100, 5, 0, 100, 0, 0);

    /** Lv3 蓝宝石剑 —— 掉落概率 80%，30% 掉 0 / 35% 掉 1 / 35% 掉 2；冷却 7 秒 */
    public static final Level LEVEL_3 = new Level(0.80F, 100, 7, 30, 35, 35, 0);

    /** Lv4 预留 —— 掉落概率 87%，25% 掉 0 / 25% 掉 1 / 50% 掉 2；冷却 9 秒 */
    public static final Level LEVEL_4 = new Level(0.87F, 100, 9, 25, 25, 50, 0);

    /** Lv5 预留 —— 掉落概率 95%，25% 掉 1 / 50% 掉 2 / 25% 掉 3；冷却 10 秒 */
    public static final Level LEVEL_5 = new Level(0.95F, 100, 10, 0, 25, 50, 25);

    /**
     * 由等级定义构造 {@link SkinConfig}，供 {@code AllSkills} 注册剥取技能使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static SkinConfig config(Level level) {
        return new SkinConfig(level.dropChance(), level.energyCost(), level.cooldownSeconds(),
                level.drop0Weight(), level.drop1Weight(), level.drop2Weight(), level.drop3Weight());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 剥取按等级取配置（Lv1~5） */
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
