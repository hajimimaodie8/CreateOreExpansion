package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.config.PlunderConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.HitSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.EntityStrategy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 夺取技能 —— 剑类攻击时夺取装备 + 吸血。
 *
 * 每次释放（能量预检查通过后）：
 * <ol>
 *     <li><b>装备夺取</b>：支持两种模式——
 *     <ul>
 *         <li>互斥权重模式（Lv1~3）：空 / 夺武器 / 夺装备 / 同时两者 互斥择一；</li>
 *         <li>独立判定模式（Lv4~5）：武器必夺 + 装备按概率独立判定。</li>
 *     </ul>
 *     夺到的装备按 {@link LootDisposition} 处置（掉落物 / 收缴进背包）；</li>
 *     <li><b>吸血</b>：敌人额外扣除 {@code lifestealAmount} 点血，自身回复等量；
 *     满血时改为伤害吸收（时长/等级见配置）。</li>
 * </ol>
 *
 * 数值见 {@code PlunderConfigs}。
 */
public class PlunderSkill extends AbstractStrategySkill<Entity, EntityStrategy>
        implements ConfigSkill<HitSkillContext, PlunderConfig> {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private int plunderWeaponWeight;
    private int plunderArmorWeight;
    private int plunderBothWeight;
    private boolean alwaysWeapon;
    private float armorChance;
    private LootDisposition weaponDisposition;
    private LootDisposition armorDisposition;
    private int lifestealAmount;
    private int absorptionTicks;
    private int absorptionAmplifier;
    private int cooldownSeconds;
    private int energyCost;

    public PlunderSkill(EntityStrategy strategy) {
        super(strategy);
    }

    @Override
    public SkillType getType() {
        return SkillType.HIT_SKILL;
    }

    @Override
    public void release(HitSkillContext context) {
        LivingEntity target = context.target();
        if (target.level().isClientSide()) return;

        Player player = context.player();
        if (player == null) return;

        // 真正生效前消耗能量
        if (!ToolEnergy.tryConsume(player, player.getMainHandItem(), this)) return;

        // 1. 装备夺取（双模式）
        rollPlunder(target, player);

        // 2. 吸血：敌人扣血 + 自身回血（满血转伤害吸收）
        int actual = Math.min(lifestealAmount, (int) target.getHealth());
        if (actual > 0) {
            target.hurt(target.damageSources().playerAttack(player), actual);
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, absorptionTicks, absorptionAmplifier));
        } else {
            player.heal(lifestealAmount);
        }
    }

    // ========== 装备夺取（双模式） ==========

    /**
     * 独立判定模式（Lv4~5）：武器必夺 + 装备按概率独立判定。
     */
    private boolean isIndependentMode() {
        return alwaysWeapon;
    }

    private void rollPlunder(LivingEntity target, Player player) {
        if (isIndependentMode()) {
            rollIndependent(target, player);
        } else {
            rollWeighted(target, player);
        }
    }

    /** 互斥权重模式（Lv1~3）：空 / 武器 / 装备 / 两者 */
    private void rollWeighted(LivingEntity target, Player player) {
        int total = plunderWeaponWeight + plunderArmorWeight + plunderBothWeight;
        if (total <= 0) return;

        int roll = target.getRandom().nextInt(total);
        if (roll < plunderWeaponWeight) {
            plunderWeapon(target, player);
            return;
        }
        roll -= plunderWeaponWeight;
        if (roll < plunderArmorWeight) {
            plunderArmor(target, player);
            return;
        }
        roll -= plunderArmorWeight;
        if (roll < plunderBothWeight) {
            plunderWeapon(target, player);
            plunderArmor(target, player);
        }
        // 剩余权重 = 「空」
    }

    /** 独立判定模式（Lv4~5）：武器必夺 + 装备概率夺 */
    private void rollIndependent(LivingEntity target, Player player) {
        plunderWeapon(target, player);
        if (target.getRandom().nextFloat() < armorChance) {
            plunderArmor(target, player);
        }
    }

    private void plunderWeapon(LivingEntity target, Player player) {
        plunderSlot(target, player, EquipmentSlot.MAINHAND, weaponDisposition);
    }

    private void plunderArmor(LivingEntity target, Player player) {
        plunderSlot(target, player, ARMOR_SLOTS[target.getRandom().nextInt(ARMOR_SLOTS.length)], armorDisposition);
    }

    /**
     * 从指定槽位夺取物品，按处置方式处理（空槽位无效果）。
     * 收缴进背包时背包已满则退回掉落物。
     */
    private void plunderSlot(LivingEntity target, Player player, EquipmentSlot slot, LootDisposition disposition) {
        ItemStack stolen = target.getItemBySlot(slot).copy();
        if (stolen.isEmpty()) return;

        target.setItemSlot(slot, ItemStack.EMPTY);

        if (disposition == LootDisposition.PICKUP && tryPickup(player, stolen)) {
            return; // 已收缴进背包
        }
        dropItem(target, stolen);
    }

    /** 尝试将物品收缴进玩家背包；成功（或已全放）返回 true */
    private boolean tryPickup(Player player, ItemStack stack) {
        boolean allAdded = player.getInventory().add(stack);
        return allAdded || stack.isEmpty();
    }

    /** 以掉落物形式生成在目标位置 */
    private void dropItem(LivingEntity target, ItemStack stack) {
        Level level = target.level();
        level.addFreshEntity(new ItemEntity(
                level, target.getX(), target.getY() + 0.5, target.getZ(), stack));
    }

    @Override
    public void load(PlunderConfig config, DataSkill data) {
        this.plunderWeaponWeight = config.plunderWeaponWeight;
        this.plunderArmorWeight = config.plunderArmorWeight;
        this.plunderBothWeight = config.plunderBothWeight;
        this.alwaysWeapon = config.alwaysWeapon;
        this.armorChance = config.armorChance;
        this.weaponDisposition = LootDisposition.valueOf(config.weaponDisposition);
        this.armorDisposition = LootDisposition.valueOf(config.armorDisposition);
        this.lifestealAmount = config.lifestealAmount;
        this.absorptionTicks = config.absorptionTicks;
        this.absorptionAmplifier = config.absorptionAmplifier;
        this.cooldownSeconds = config.cooldownSeconds;
        this.energyCost = config.energyCost;
    }

    /** 夺取技能自身冷却（Lv1~Lv5 = 4/6/8/10/12 秒），供双技能独立冷却使用 */
    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public Class<PlunderConfig> getConfigType() {
        return PlunderConfig.class;
    }

    @Override
    public int getCost() {
        return energyCost;
    }
}
