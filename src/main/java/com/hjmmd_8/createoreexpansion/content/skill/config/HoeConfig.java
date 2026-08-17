package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * 锄头技能配置。
 *
 * 配置项：
 * <ul>
 *     <li>{@link #energyCost} - 每次释放固定消耗的能量（150）</li>
 *     <li>{@link #rangeWidth} / {@link #rangeDepth} - 作用范围（翡翠 3×3、黄玉 3×5、蓝宝石 5×5）</li>
 *     <li>{@link #matureChance} - 收割重种后作物被催熟的概率</li>
 * </ul>
 */
public class HoeConfig extends AutoSkillConfig {

    /** 每次释放技能固定消耗的能量（无论触发收割/播种/犁地哪个分支，均只扣此值，无累加） */
    public static final int ENERGY_COST = 50;

    /** 收割重种后的基础生长阶段（0 = 刚种下） */
    public static final int MIN_STAGE = 0;
    /** 收割重种后催熟可能达到的最大生长阶段 */
    public static final int MAX_STAGE = 4;

    public int energyCost;
    public int rangeWidth;
    public int rangeDepth;
    public float matureChance;
    /** 收割后额外掉落作物的概率（逐级递增，见 {@code HoeConfigs}） */
    public float extraDropChance;

    public HoeConfig(int energyCost, int rangeWidth, int rangeDepth, float matureChance) {
        this(energyCost, rangeWidth, rangeDepth, matureChance, 0F);
    }

    public HoeConfig(int energyCost, int rangeWidth, int rangeDepth, float matureChance, float extraDropChance) {
        this.energyCost = energyCost;
        this.rangeWidth = rangeWidth;
        this.rangeDepth = rangeDepth;
        this.matureChance = matureChance;
        this.extraDropChance = extraDropChance;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("Cost", () -> energyCost, v -> energyCost = v),
                ofInt("RangeWidth", () -> rangeWidth, v -> rangeWidth = v),
                ofInt("RangeDepth", () -> rangeDepth, v -> rangeDepth = v),
                ofFloat("MatureChance", () -> matureChance, v -> matureChance = v),
                ofFloat("ExtraDropChance", () -> extraDropChance, v -> extraDropChance = v)
        );
    }

    /**
     * 随机催熟到的生长阶段。
     *
     * 上限为 {@code maxAge - 1}：收割重种后的作物<b>不应立即回到成熟态</b>，
     * 否则（如甜菜根 maxAge=3）会再次被判定为可收割，造成无限产出。
     */
    public int rollStage(RandomSource random, int maxAge) {
        return Math.min(maxAge - 1, random.nextIntBetweenInclusive(MIN_STAGE, MAX_STAGE));
    }
}
