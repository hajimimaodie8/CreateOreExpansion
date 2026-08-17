package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * 锄头（耕作）技能 —— 统一配置类（作用范围 / 催熟概率 / 收割额外掉落概率 / 能量 集中修改点）。
 *
 * 所有使用 {@code HoeSkill} 的耕作技能（锄）的范围、催熟概率、收割额外掉落概率
 * 都在本文件统一定义，{@code AllSkills} / {@code AllItems} 只负责引用，不改数值。
 *
 * 等级说明（范围逐级扩大，收割后额外掉落概率逐级递增）：
 * <ul>
 *     <li>Lv1 翡翠锄 —— 范围 3×3；额外掉落概率 5%</li>
 *     <li>Lv2 黄玉锄 —— 范围 3×5；额外掉落概率 10%</li>
 *     <li>Lv3 蓝宝石锄 —— 范围 5×5；额外掉落概率 15%</li>
 *     <li>Lv4 预留 —— 范围 5×7；额外掉落概率 20%</li>
 *     <li>Lv5 预留 —— 范围 7×7；额外掉落概率 25%</li>
 * </ul>
 */
public final class HoeConfigs {

    private HoeConfigs() {
    }

    /**
     * 单级锄头技能定义。
     *
     * @param energyCost      每次释放固定消耗能量
     * @param rangeWidth      作用范围宽度
     * @param rangeDepth      作用范围深度
     * @param matureChance    收割重种后催熟概率
     * @param extraDropChance 收割后额外掉落作物概率（逐级递增）
     */
    public record Level(int energyCost, int rangeWidth, int rangeDepth,
                        float matureChance, float extraDropChance) {
    }

    // ========== 五个等级（Lv1/Lv2/Lv3 已绑定，Lv4/Lv5 预留） ==========

    /** Lv1 翡翠锄 —— 3×3；额外掉落 5% */
    public static final Level LEVEL_1 = new Level(50, 3, 3, 0.15F, 0.05F);

    /** Lv2 黄玉锄 —— 3×5；额外掉落 10% */
    public static final Level LEVEL_2 = new Level(50, 3, 5, 0.30F, 0.10F);

    /** Lv3 蓝宝石锄 —— 5×5；额外掉落 15% */
    public static final Level LEVEL_3 = new Level(50, 5, 5, 0.50F, 0.15F);

    /** Lv4 预留 —— 5×7；额外掉落 20% */
    public static final Level LEVEL_4 = new Level(50, 5, 7, 0.65F, 0.20F);

    /** Lv5 预留 —— 7×7；额外掉落 25% */
    public static final Level LEVEL_5 = new Level(50, 7, 7, 0.80F, 0.25F);

    /**
     * 由等级定义构造 {@link HoeConfig}，供 {@code AllSkills} 注册锄头技能使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static HoeConfig config(Level level) {
        return new HoeConfig(level.energyCost(), level.rangeWidth(), level.rangeDepth(),
                level.matureChance(), level.extraDropChance());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 耕作按等级取配置（Lv1~5） */
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
