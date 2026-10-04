package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowCurseConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowDisarmConfig;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 翠玉之弓「命中效果」的落点（2026-09-30 技能换核第 3 阶段）。
 *
 * <p><b>为什么单独一个类</b>：这两段效果原先住在旧技能实现
 * （{@code BowCurseSkill#applyTo} / {@code BowDisarmSkill#applyTo}）里，由旧的
 * {@code JadeTopazBowEventHandler} 通过 {@code instanceof} 分发。换核后旧实现类被删除，
 * 效果本体搬到这里，改为<b>按技能 id 字符串</b>分发（见
 * {@code JadeTopazBowEventHandler#onProjectileImpact}）。</p>
 *
 * <p><b>行为逐字保留</b>：概率、持续时间、药水云参数、缴械范围与「扒怪物装备」的判定
 * 全部与搬迁前一致；配置类型仍复用 {@link BowCurseConfig} / {@link BowDisarmConfig}
 * （它们是数据持有者，新内核也在读同一批实例）。</p>
 */
public final class BowHitEffects {

    private BowHitEffects() {
    }

    /**
     * 凋零诅咒命中：按等级配置附加凋零 + 缓慢，并生成药水云。
     *
     * <p>与旧 {@code BowCurseSkill#applyTo} 逐行等价。</p>
     */
    public static void applyCurse(Player player, LivingEntity target, BowCurseConfig config) {
        boolean upgraded = target.level().getRandom().nextFloat() < config.upgradeChance;
        int duration = config.durationTicks + target.level().getRandom().nextInt(config.durationVariance + 1);
        int amplifier = upgraded ? 1 : 0;

        target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));

        AreaEffectCloud cloud = new AreaEffectCloud(target.level(), target.getX(), target.getY(), target.getZ());
        cloud.setRadius(config.cloudRadius);
        cloud.setWaitTime(10);
        cloud.setDuration(duration);
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, config.cloudSlowAmplifier));
        target.level().addFreshEntity(cloud);
    }

    /**
     * 缴械风暴命中：范围内缴械（可入施放者背包）+ 可选扒掉怪物全身装备。
     *
     * <p>与旧 {@code BowDisarmSkill#applyTo} 逐行等价。</p>
     */
    public static void applyDisarm(Player player, LivingEntity target, BowDisarmConfig config) {
        boolean intoInventory = target.level().getRandom().nextFloat() < config.intoInventoryChance;
        AABB area = new AABB(target.blockPosition()).inflate(config.rangeRadius);
        List<LivingEntity> entities = target.level()
                .getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player);

        for (LivingEntity entity : entities) {
            ItemStack held = entity.getMainHandItem();
            if (!held.isEmpty()) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                if (intoInventory) {
                    if (!player.getInventory().add(held)) {
                        entity.spawnAtLocation(held);
                    }
                } else {
                    entity.spawnAtLocation(held);
                }
            }

            if (!intoInventory && config.stripMonsterArmor && entity instanceof Monster) {
                EquipmentSlot[] armorSlots = {EquipmentSlot.OFFHAND,
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                for (EquipmentSlot slot : armorSlots) {
                    ItemStack stack = entity.getItemBySlot(slot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    entity.setItemSlot(slot, ItemStack.EMPTY);
                    entity.spawnAtLocation(stack);
                }
            }
        }
    }
}
