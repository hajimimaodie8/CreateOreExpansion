package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * <b>回旋镖四档材质的数值真源</b>（唯一出处：物品注册、实体伤害、挖掘判定、能量扣减全读这里）。
 *
 * <p>四档与既有工具线同一顺序：<b>翠玉（jade_topaz）→ 宝石（sapphire_ruby）→ 星界（astral）→ 雷鸣（thunder）</b>。
 * 数值不是我凭空拍的，而是<b>逐条挂到既有梯度上</b>：</p>
 * <ul>
 *   <li>{@link #energy()} —— 抄 {@code AllTiers} 同名材质的工具能量档（600 / 3000 / 4500 / 5000）。</li>
 *   <li>{@link #miningLevel()} / {@link #incorrectBlocks()} —— 与 {@code AllTiers} <b>同一条原版标签</b>：
 *       翠玉用 {@code INCORRECT_FOR_DIAMOND_TOOL}（= 钻石级，与 {@code AllTiers.JADE/TOPAZ} 一致），
 *       其余三档用 {@code INCORRECT_FOR_NETHERITE_TOOL}（= 下界合金级，与 {@code AllTiers.SAPPHIRE/
 *       STELLARSTONE/THUNDERITE} 一致）。</li>
 *   <li>{@link #damage()} —— 按四档拉开（6 / 8 / 10 / 12），落在既有工具的"材质加成 3.5 / 4.5 / 5 / 5"
 *       与剑的"加成 +4"这两条线之间（镖是投掷物，既不叠手部攻击、也不吃蓄力）。</li>
 *   <li>{@link #throwCost()} / {@link #mineCost()} / {@link #cooldownTicks()} —— 我定的（作者可一句话改）。</li>
 * </ul>
 *
 * <h2>为什么 {@link #digSpeed()} 长这个样子</h2>
 * <p>挖掘判定整段照搬原版的口径（见 {@code AbstractBoomerangEntity#mineBlock}）：
 * {@code 进度 = digSpeed / (hardness * i)}，{@code i = doPlayerHarvestCheck ? 30 : 100}，
 * 原版要求<b>一个 tick 内进度 ≥ 1</b> 才算挖开。把 {@code digSpeed} 取成
 * {@code 30 * maxHardness} 之后，<b>i=30 的分支退化成 {@code hardness <= maxHardness}</b>
 * ——正是需求里"与该把的 maxHardness 比"那句话；i=100 的"用错工具"分支则自动变成
 * {@code hardness <= 0.3 * maxHardness}（原版对用错工具的惩罚，不是新规则）。</p>
 *
 * <p>⚠ {@code Item#getDestroySpeed} 恒 0（需求 10：防止被当普通工具用），所以这里的
 * {@code digSpeed} <b>不是</b>从物品上读的——它是这一档自己的数字，只给实体挖掘判定用。</p>
 */
public enum BoomerangTier {

    /** 翠玉镖：钻石级（与 {@code AllTiers.JADE/TOPAZ} 同标签）、硬度上限 10。 */
    JADE_TOPAZ(6.0F, 600, 10.0F, 3, 25, 5, 20, ToolEnergyColorConfig.JADE),

    /** 宝石镖：下界合金级（与 {@code AllTiers.SAPPHIRE} 同标签）、硬度上限 20（= Quark 默认值）。 */
    SAPPHIRE_RUBY(8.0F, 3000, 20.0F, 4, 50, 10, 15, ToolEnergyColorConfig.SAPPHIRE),

    /** 星界镖：下界合金级、硬度上限 30（够到远古残骸的 30）。 */
    ASTRAL(10.0F, 4500, 30.0F, 4, 75, 15, 10, ToolEnergyColorConfig.STELLARSTONE),

    /** 雷鸣镖：下界合金级、硬度上限 40（仍够不到黑曜石的 50）。 */
    THUNDER(12.0F, 5000, 40.0F, 4, 100, 20, 10, ToolEnergyColorConfig.THUNDERITE);

    /** 命中生物的伤害（{@code hurt} 的原始值，不叠手部攻击/附魔）。 */
    private final float damage;
    /** 能量上限（初始即满，与工具同形：ENERGY == MAX_ENERGY）。 */
    private final int energy;
    /** 能挖动的最大方块硬度（挖方块那条判定的唯一门槛）。 */
    private final float maxHardness;
    /** 挖掘等级 1~4 = 原版石/铁/钻/合金（决定 {@link #incorrectBlocks()}，也用于报告里的对照表）。 */
    private final int miningLevel;
    /** 一次投掷的能量消耗。 */
    private final int throwCost;
    /** 成功挖掉一个方块的能量消耗（耐久 → 能量：这就是"耐久"的替代品）。 */
    private final int mineCost;
    /** 投掷冷却（tick）。 */
    private final int cooldownTicks;
    /** 能量条配色（沿用同材质工具的配色）。 */
    private final ToolEnergyColorConfig color;

    BoomerangTier(float damage, int energy, float maxHardness, int miningLevel,
                  int throwCost, int mineCost, int cooldownTicks, ToolEnergyColorConfig color) {
        this.damage = damage;
        this.energy = energy;
        this.maxHardness = maxHardness;
        this.miningLevel = miningLevel;
        this.throwCost = throwCost;
        this.mineCost = mineCost;
        this.cooldownTicks = cooldownTicks;
        this.color = color;
    }

    public float damage() {
        return damage;
    }

    public int energy() {
        return energy;
    }

    public float maxHardness() {
        return maxHardness;
    }

    public int miningLevel() {
        return miningLevel;
    }

    public int throwCost() {
        return throwCost;
    }

    public int mineCost() {
        return mineCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public ToolEnergyColorConfig color() {
        return color;
    }

    /**
     * 原版挖掘进度里的 {@code digSpeed}（见类注释的推导：{@code 30 * maxHardness}）。
     *
     * <p>取 30 而不是 100：{@code i} 的两个分支里，<b>30 是"算正确工具/不需要正确工具"的那一支</b>
     * （草地上手挖、镐挖石头都是它），也就是 {@code maxHardness} 那句话想表达的那一支。</p>
     */
    public float digSpeed() {
        return 30.0F * maxHardness;
    }

    /**
     * 这一档挖不动的方块标签——<b>与原版 {@code Tier#getIncorrectBlocksForDrops} 同一判据</b>
     * （{@code AllTiers} 用的就是这两个标签，所以镖与镐的挖掘等级天然一致）。
     */
    public TagKey<Block> incorrectBlocks() {
        return switch (miningLevel) {
            case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }
}
