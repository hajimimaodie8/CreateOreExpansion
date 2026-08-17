package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * 伐树（斧类连锁砍树）技能 —— 统一配置类（搜索范围 / 方块上限 / 砍伐对象 / 能量 / 速度衰减 集中修改点）。
 *
 * 所有使用 {@code FellingSkill} 的砍树技能（斧）的搜索范围、方块数量上限、
 * 砍伐对象、能量消耗、速度衰减都在本文件统一定义，{@code AllSkills} /
 * {@code AllItems} 只负责引用，不改数值。
 *
 * 等级说明：
 * <ul>
 *     <li>Lv1 翡翠斧 —— 只砍树干（IS_LOG），现状勿动</li>
 *     <li>Lv2 黄玉斧 —— 连树干带叶一起砍（IS_TREE），有范围限制，现状勿动</li>
 *     <li>Lv3 蓝宝石斧 —— 范围进一步扩大，现状勿动</li>
 *     <li>Lv4 —— 范围再扩大（预留）</li>
 *     <li>Lv5 —— 无视范围限制，直接砍（预留，searchRange 用 {@link #UNLIMITED_RANGE}）</li>
 * </ul>
 */
public final class FellingConfigs {

    private FellingConfigs() {
    }

    /** 搜索范围 <= 0 表示无限（不限制搜索半径，仅受 maxBlocks 上限约束） */
    public static final int UNLIMITED_RANGE = 0;

    /**
     * 单级砍树技能定义。
     *
     * @param searchRange    曼哈顿搜索半径（&lt;= 0 表示无限，如 Lv5）
     * @param maxBlocks      方块数量上限（防止无限搜索；达到上限时本次放弃）
     * @param predicate      砍伐对象（IS_LOG=只砍树干 / IS_TREE=树干+树叶）
     * @param energyCost     单次技能能量消耗
     * @param logResistance  树干速度衰减因子（越大砍得越慢；Lv1 最大，Lv5 无衰减）
     * @param leafResistance 树叶速度衰减因子
     * @param cooldownSeconds 砍伐后冷却秒数（创造模式无冷却；首项 3 公差 2 的等差数列）
     */
    public record Level(int searchRange, int maxBlocks, FellingConfig.BlockPredicate predicate,
                        int energyCost, float logResistance, float leafResistance, int cooldownSeconds) {
    }

    // ========== 五个等级（Lv4/Lv5 预留，将来启用时在 AllSkills 注册对应技能即可） ==========

    /** Lv1 翡翠斧 —— 只砍树干；搜索半径 50，方块上限 15；衰减因子最大；冷却 3 秒 */
    public static final Level LEVEL_1 = new Level(50, 15,
            FellingConfig.BlockPredicate.IS_LOG, 100, .12f, .12f, 3);

    /** Lv2 黄玉斧 —— 连树干带叶；搜索半径 75，方块上限 50；冷却 5 秒 */
    public static final Level LEVEL_2 = new Level(75, 50,
            FellingConfig.BlockPredicate.IS_TREE, 100, .09f, .09f, 5);

    /** Lv3 蓝宝石斧 —— 搜索半径 100，方块上限 100；冷却 7 秒 */
    public static final Level LEVEL_3 = new Level(100, 100,
            FellingConfig.BlockPredicate.IS_TREE, 100, .06f, .06f, 7);

    /** Lv4 预留 —— 搜索半径 150，方块上限 200；冷却 9 秒 */
    public static final Level LEVEL_4 = new Level(150, 200,
            FellingConfig.BlockPredicate.IS_TREE, 100, .03f, .03f, 9);

    /** Lv5 预留 —— 无视范围限制直接砍；搜索半径无限，方块数量无上限；无速度衰减；冷却 11 秒 */
    public static final Level LEVEL_5 = new Level(UNLIMITED_RANGE, Integer.MAX_VALUE,
            FellingConfig.BlockPredicate.IS_TREE, 100, 0f, 0f, 11);

    /**
     * 由等级定义构造 {@link FellingConfig}，供 {@code AllSkills} 注册砍树技能使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static FellingConfig config(Level level) {
        return new FellingConfig(level.searchRange(), level.maxBlocks(), level.predicate(),
                level.energyCost(), level.logResistance(), level.leafResistance(), level.cooldownSeconds());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 伐树按等级取配置（Lv1~5） */
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
