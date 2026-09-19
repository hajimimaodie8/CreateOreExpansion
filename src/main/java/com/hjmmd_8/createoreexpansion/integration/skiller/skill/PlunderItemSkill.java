package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.LootDisposition;
import com.hjmmd_8.createoreexpansion.content.skill.config.PlunderConfig;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 夺取技能（新内核版）——剑类攻击时夺取装备 + 吸血。
 *
 * <p>与旧 {@code content/skill/PlunderSkill} 同源，逐条对应：</p>
 * <table border="1">
 *   <caption>与旧实现的差异</caption>
 *   <tr><th>旧实现</th><th>新实现</th></tr>
 *   <tr><td>技能键判定</td><td><b>去掉</b>（新路由只对按下的槽位调用）</td></tr>
 *   <tr><td>{@code tryConsume} 在 {@code release} 开头（"真正生效前"）</td>
 *       <td>移到 {@link #consumeResource}：新内核统一在 {@code release} 之前扣费。
 *           扣费时机与旧实现一致——本技能没有"先掷骰再决定是否扣费"的门槛
 *           （{@code rollPlunder} 掷的是夺哪件，不是是否触发），所以直接扣即可</td></tr>
 *   <tr><td>冷却判定在 {@code HurtLivingEntityHandler}</td>
 *       <td>移到本技能（{@code consumeResource} 与 {@code release} 两处同判），
 *           真正做完才 {@code startTicks}；时长口径见 {@link CoeSkillSupport#cooldownTicks}</td></tr>
 *   <tr><td>其余（互斥权重模式 / 独立判定模式 / 处置方式 / 吸血与满血转伤害吸收）</td>
 *       <td>逐条保留</td></tr>
 * </table>
 *
 * <p>与 {@link SkinItemSkill} 一样，新实现不实现 {@code StrategySkill}：旧
 * {@code EntityStrategy} 只服务客户端描边预览（{@code calculate} 恒返回空集合），
 * 迁移期预览仍由旧渲染器提供，W5 再统一补策略与渲染器。</p>
 *
 * @since 1.0.0
 */
public class PlunderItemSkill implements ItemSkill<HitSkillContext> {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @Override
    public void release(HitSkillContext context, ISkillInstance<HitSkillContext> instance) {
        LivingEntity target = context.target();
        Player player = context.getPlayer();
        if (target == null || player == null || target.level().isClientSide()) {
            return;
        }
        ItemStack sword = player.getMainHandItem();
        PlunderConfig config = CoeSkillSupport.configForLevel(sword, instance, PlunderConfig.class);
        if (config == null) {
            return;
        }
        // 冷却中：不执行（consumeResource 里同样跳过，不会白扣能量）
        if (CoeSkillSupport.onCooldown(player, sword)) {
            return;
        }

        // 1. 装备夺取（双模式）
        rollPlunder(config, target, player);

        // 2. 吸血：敌人扣血 + 自身回血（满血转伤害吸收）
        int actual = Math.min(config.lifestealAmount, (int) target.getHealth());
        if (actual > 0) {
            target.hurt(target.damageSources().playerAttack(player), actual);
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.ABSORPTION, config.absorptionTicks, config.absorptionAmplifier));
        } else {
            player.heal(config.lifestealAmount);
        }

        // 真正执行过才进冷却
        int ticks = CoeSkillSupport.cooldownTicks(sword, config.cooldownSeconds);
        if (ticks > 0) {
            ToolSkillCooldown.startTicks(player, sword, ticks);
        }
    }

    /** 夺取的消耗：本技能没有"先掷骰再决定是否扣费"的门槛，冷却通过即扣。 */
    @Override
    public void consumeResource(HitSkillContext context, Consumable consumable,
                                ISkillInstance<HitSkillContext> instance) {
        Player player = context.getPlayer();
        LivingEntity target = context.target();
        if (player == null || target == null || target.level().isClientSide()) {
            return;
        }
        ItemStack sword = player.getMainHandItem();
        PlunderConfig config = CoeSkillSupport.configForLevel(sword, instance, PlunderConfig.class);
        if (config == null) {
            return;
        }
        if (CoeSkillSupport.onCooldown(player, sword)) {
            return;
        }
        int cost = CoeSkillSupport.cost(sword, config.energyCost,
                CoeSkillSupport.effectiveLevel(sword, instance));
        CoeSkillSupport.consume(player, sword, consumable, cost);
    }

    // ========== 装备夺取（双模式，与旧实现逐行同源） ==========

    private static void rollPlunder(PlunderConfig config, LivingEntity target, Player player) {
        if (config.alwaysWeapon) {
            rollIndependent(config, target, player);
        } else {
            rollWeighted(config, target, player);
        }
    }

    /** 互斥权重模式（Lv1~3）：空 / 武器 / 装备 / 两者。 */
    private static void rollWeighted(PlunderConfig config, LivingEntity target, Player player) {
        int total = config.plunderWeaponWeight + config.plunderArmorWeight + config.plunderBothWeight;
        if (total <= 0) {
            return;
        }
        int roll = target.getRandom().nextInt(total);
        if (roll < config.plunderWeaponWeight) {
            plunderWeapon(config, target, player);
            return;
        }
        roll -= config.plunderWeaponWeight;
        if (roll < config.plunderArmorWeight) {
            plunderArmor(config, target, player);
            return;
        }
        roll -= config.plunderArmorWeight;
        if (roll < config.plunderBothWeight) {
            plunderWeapon(config, target, player);
            plunderArmor(config, target, player);
        }
        // 剩余权重 = 「空」
    }

    /** 独立判定模式（Lv4~5）：武器必夺 + 装备按概率独立判定。 */
    private static void rollIndependent(PlunderConfig config, LivingEntity target, Player player) {
        plunderWeapon(config, target, player);
        if (target.getRandom().nextFloat() < config.armorChance) {
            plunderArmor(config, target, player);
        }
    }

    private static void plunderWeapon(PlunderConfig config, LivingEntity target, Player player) {
        plunderSlot(target, player, EquipmentSlot.MAINHAND, dispositionOf(config.weaponDisposition));
    }

    private static void plunderArmor(PlunderConfig config, LivingEntity target, Player player) {
        plunderSlot(target, player,
                ARMOR_SLOTS[target.getRandom().nextInt(ARMOR_SLOTS.length)],
                dispositionOf(config.armorDisposition));
    }

    /**
     * 从指定槽位夺取物品，按处置方式处理（空槽位无效果）。
     * 收缴进背包时背包已满则退回掉落物。
     */
    private static void plunderSlot(LivingEntity target, Player player,
                                    EquipmentSlot slot, LootDisposition disposition) {
        ItemStack stolen = target.getItemBySlot(slot).copy();
        if (stolen.isEmpty()) {
            return;
        }
        target.setItemSlot(slot, ItemStack.EMPTY);

        if (disposition == LootDisposition.PICKUP && tryPickup(player, stolen)) {
            return; // 已收缴进背包
        }
        dropItem(target, stolen);
    }

    /** 尝试将物品收缴进玩家背包；成功（或已全放）返回 true。 */
    private static boolean tryPickup(Player player, ItemStack stack) {
        boolean allAdded = player.getInventory().add(stack);
        return allAdded || stack.isEmpty();
    }

    /** 以掉落物形式生成在目标位置。 */
    private static void dropItem(LivingEntity target, ItemStack stack) {
        Level level = target.level();
        level.addFreshEntity(new ItemEntity(
                level, target.getX(), target.getY() + 0.5, target.getZ(), stack));
    }

    /** 配置里存的是枚举名；名字非法时按"掉落物"处置（旧实现在 {@code load} 里直接 valueOf，会抛异常）。 */
    private static LootDisposition dispositionOf(String name) {
        try {
            return LootDisposition.valueOf(name);
        } catch (RuntimeException e) {
            return LootDisposition.DROP;
        }
    }
}
