package com.hjmmd_8.createoreexpansion.common;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * 所有工具Tier的枚举类。
 *
 * @see AllMyItems
 * @see net.minecraft.world.item.Tiers
 * @see net.minecraft.world.item.Tier
 * @author Leaf
 */
public enum AllTiers implements Tier {

    //     ↓耐久基数        速度                伤害                附魔值
    JADE(1600, 8.5F, 3.5F, 15,
    //                不可挖掘方块                                   修复材料
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(AllMyItems.JADE_INGOT.get())),
    TOPAZ(1828, 9.5F, 4.0F, 14,
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(AllMyItems.TOPAZ_INGOT.get())),
    SAPPHIRE(2048, 10.0F, 4.5F, 16,
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, () -> Ingredient.of(AllMyItems.SAPPHIRE_INGOT.get()));

    private final int uses;
    private final float speed;
    private final float attackDamage;
    private final int enchantmentValue;
    private final TagKey<Block> incorrectBlocks;
    private final Supplier<Ingredient> repairIngredient;

    /**
     * 创建一个新的 工具Tier.
     * @param uses 耐久基数，耐久基数加上工具的耐久修正值才是耐久度。
     * @param speed 挖掘速度
     * @param attackDamage 攻击伤害
     * @param enchantmentValue 附魔能力，具体可以参考维基百科和 {@link net.minecraft.world.item.Tiers}
     * @param incorrectBlocks 不可挖掘的方块，这个用现有的就行，
     *                        参考 {@link BlockTags#INCORRECT_FOR_IRON_TOOL}
     * @param repairIngredient 修复材料，如铁套的修复材料是铁锭: {@link net.minecraft.world.item.Tiers#IRON}
     *
     * @see net.minecraft.world.item.Tier
     * @see net.minecraft.world.item.Tiers
     */
    AllTiers(int uses, float speed, float attackDamage, int enchantmentValue,
             TagKey<Block> incorrectBlocks, Supplier<Ingredient> repairIngredient) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamage = attackDamage;
        this.enchantmentValue = enchantmentValue;
        this.incorrectBlocks = incorrectBlocks;
        this.repairIngredient = repairIngredient;
    }

    /**
     * 注册，这个方法必须被调用，否则这个Tier不会被注册。
     */
    public static void register() {}

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.attackDamage;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return this.incorrectBlocks;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}
