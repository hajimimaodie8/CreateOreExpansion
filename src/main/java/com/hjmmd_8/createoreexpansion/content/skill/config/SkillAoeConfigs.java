package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * AOE 范围类技能 —— 统一配置类（长宽高 / 能量 / 等级 集中修改点）。
 *
 * 所有使用 {@code AreaAoeSkill} 的范围挖掘技能（开岩、引渠、平场等）
 * 的长宽高、能量消耗、朝向规则都在本文件统一定义，{@code AllSkills} /
 * {@code AllItems} 只负责引用，不改数值。
 *
 * 用户叫法为「横向×纵向」（宽×高），如 {@code 5×3 = 横向5格 × 纵向3格}，
 * {@code 5×5×2 = 横向5 × 纵向5 × 深2}。与 {@link AreaUtil} 的
 * width=横向、height=纵向 对应。
 */
public final class SkillAoeConfigs {

    private SkillAoeConfigs() {
    }

    /**
     * 单级范围技能定义。
     *
     * @param energyCost 单次技能能量消耗
     * @param width      横向宽度（格）
     * @param height     纵向高度（格）
     * @param depth      挖掘深度（格，沿挖掘方向）
     * @param direction  范围朝向来源（跟随玩家朝向 / 面向方块面）
     */
    public record Level(int energyCost, int width, int height, int depth, DualDirection.From direction) {
    }

    /**
     * 由「目标标签 + 等级定义」构造 {@link AreaAoeConfig}，供 {@code AllSkills} 注册使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static AreaAoeConfig aoeConfig(TagKey<Block> mineableTag, Level level) {
        return new AreaAoeConfig(level.energyCost(), mineableTag,
                level.width(), level.height(), level.depth(), level.direction());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 开岩按等级取配置（Lv1~5） */
    public static Level breakRockLevel(int level) {
        return switch (level) {
            case 1 -> BREAK_ROCK_1;
            case 2 -> BREAK_ROCK_2;
            case 3 -> BREAK_ROCK_3;
            case 4 -> BREAK_ROCK_4;
            default -> BREAK_ROCK_5;
        };
    }

    /** 引渠按等级取配置（Lv1~5） */
    public static Level channelLevel(int level) {
        return switch (level) {
            case 1 -> CHANNEL_1;
            case 2 -> CHANNEL_2;
            case 3 -> CHANNEL_3;
            case 4 -> CHANNEL_4;
            default -> CHANNEL_5;
        };
    }

    /** 平场按等级取配置（Lv1~3） */
    public static Level gradeLevel(int level) {
        return switch (level) {
            case 1 -> GRADE_1;
            case 2 -> GRADE_2;
            default -> GRADE_3;
        };
    }

    // =====================================================================
    // 开岩（镐）—— 5 级
    // =====================================================================

    /** 开岩目标标签：稿类可挖 */
    public static final TagKey<Block> BREAK_ROCK_TAG = BlockTags.MINEABLE_WITH_PICKAXE;

    /** 开岩 Lv1 翡翠稿 —— 现状勿动 */
    public static final Level BREAK_ROCK_1 = new Level(10, 3, 1, 1, DualDirection.From.PLAYER_YAW);

    /** 开岩 Lv2 黄玉稿 —— 3×3 */
    public static final Level BREAK_ROCK_2 = new Level(10, 3, 3, 1, DualDirection.From.BLOCK_FACE);

    /** 开岩 Lv3 蓝宝石稿 —— 横向5 × 纵向3（原 5×5 改为 5×3） */
    public static final Level BREAK_ROCK_3 = new Level(10, 5, 3, 1, DualDirection.From.BLOCK_FACE);

    /** 开岩 Lv4 预留 —— 横向5 × 纵向5 */
    public static final Level BREAK_ROCK_4 = new Level(10, 5, 5, 1, DualDirection.From.BLOCK_FACE);

    /** 开岩 Lv5 预留 —— 5×5×2（横向5 × 纵向5 × 深2） */
    public static final Level BREAK_ROCK_5 = new Level(10, 5, 5, 2, DualDirection.From.BLOCK_FACE);

    // =====================================================================
    // 引渠（铲）—— 向前挖沟，5 级
    // =====================================================================

    /** 引渠目标标签：铲类可挖 */
    public static final TagKey<Block> CHANNEL_TAG = BlockTags.MINEABLE_WITH_SHOVEL;

    /** 引渠 Lv1 —— 向前挖 4 格（翡翠铲） */
    public static final Level CHANNEL_1 = new Level(10, 1, 1, 4, DualDirection.From.PLAYER_YAW);

    /** 引渠 Lv2 —— 向前挖 6 格（黄玉铲） */
    public static final Level CHANNEL_2 = new Level(10, 1, 1, 6, DualDirection.From.PLAYER_YAW);

    /** 引渠 Lv3 —— 向前挖 7 格（预留） */
    public static final Level CHANNEL_3 = new Level(10, 1, 1, 7, DualDirection.From.PLAYER_YAW);

    /** 引渠 Lv4 —— 向前挖 8 格（预留） */
    public static final Level CHANNEL_4 = new Level(10, 1, 1, 8, DualDirection.From.PLAYER_YAW);

    /** 引渠 Lv5 —— 向前挖 10 格（预留） */
    public static final Level CHANNEL_5 = new Level(10, 1, 1, 10, DualDirection.From.PLAYER_YAW);

    // =====================================================================
    // 平场（铲）—— 平面铲平，3 级（截止，无更多等级）
    // =====================================================================

    /** 平场目标标签：铲类可挖 */
    public static final TagKey<Block> GRADE_TAG = BlockTags.MINEABLE_WITH_SHOVEL;

    /** 平场 Lv1 —— 5×5 平面（蓝宝石铲，以目标为中心半径 2 格） */
    public static final Level GRADE_1 = new Level(10, 5, 5, 1, DualDirection.From.BLOCK_FACE);

    /** 平场 Lv2 —— 5×7 平面 */
    public static final Level GRADE_2 = new Level(10, 5, 7, 1, DualDirection.From.BLOCK_FACE);

    /** 平场 Lv3 —— 7×7 平面 */
    public static final Level GRADE_3 = new Level(10, 7, 7, 1, DualDirection.From.BLOCK_FACE);
}
