package com.hjmmd_8.createoreexpansion.foundation;

import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.common.AllModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class JadeTopazBowEventHandler {

	@SubscribeEvent
	public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide)
			return;
		if (!(event.getProjectile() instanceof Arrow arrow))
			return;
		if (!(arrow.getOwner() instanceof Player player))
			return;

		String skill = arrow.getPersistentData().getString(JadeTopazBowItem.TAG_SKILL);
		if (skill.isEmpty())
			return;
		if (!(event.getRayTraceResult() instanceof EntityHitResult hit))
			return;
		if (!(hit.getEntity() instanceof LivingEntity target))
			return;

		applyBaseEffects(player, target);
		if ("A".equals(skill))
			applySkillA(player, target);
		else if ("B".equals(skill))
			applySkillB(player, target);
	}

	private static void applyBaseEffects(Player player, LivingEntity target) {
		float roll = target.level().getRandom().nextFloat();
		if (roll < 0.5F) {
			// 50% 无效果，也不掉落主手
        } else if (roll < 0.7F) {
			addEffect(target, MobEffects.WITHER, 60, 0);
		} else if (roll < 0.9F) {
			addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 0);
		} else {
			addEffect(target, AllModEffects.TRANSMUTATION_DISORDER, 60, 0);

			ItemStack held = target.getMainHandItem();
			if (!held.isEmpty()) {
				target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				target.spawnAtLocation(held);
			}
		}
	}

	private static void applySkillA(Player player, LivingEntity target) {
		boolean upgraded = target.level().getRandom().nextFloat() < 0.5F;
		int duration = 100 + target.level().getRandom().nextInt(101);
		int amplifier = upgraded ? 1 : 0;

		addEffect(target, MobEffects.WITHER, duration, amplifier);
		addEffect(target, MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier);

		AreaEffectCloud cloud = new AreaEffectCloud(target.level(), target.getX(), target.getY(), target.getZ());
		cloud.setRadius(1.5F);
		cloud.setWaitTime(10);
		cloud.setDuration(duration);
		cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2));
		target.level().addFreshEntity(cloud);
	}

	private static void applySkillB(Player player, LivingEntity target) {
		boolean intoInventory = target.level().getRandom().nextFloat() < 0.5F;
		AABB area = new AABB(target.blockPosition()).inflate(1.0);
		List<LivingEntity> entities = target.level()
			.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != player);

		for (LivingEntity entity : entities) {
			ItemStack held = entity.getMainHandItem();
			if (!held.isEmpty()) {
				entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				if (intoInventory) {
					if (!player.getInventory().add(held))
						entity.spawnAtLocation(held);
				} else {
					entity.spawnAtLocation(held);
				}
			}

			if (!intoInventory && entity instanceof Monster) {
				EquipmentSlot[] armorSlots = { EquipmentSlot.OFFHAND,
					EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
				for (EquipmentSlot slot : armorSlots) {
					ItemStack stack = entity.getItemBySlot(slot);
					if (stack.isEmpty())
						continue;
					entity.setItemSlot(slot, ItemStack.EMPTY);
					entity.spawnAtLocation(stack);
				}
			}
		}
	}

	private static void dropAllEquipment(LivingEntity entity) {
		EquipmentSlot[] slots = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
		for (EquipmentSlot slot : slots) {
			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.isEmpty())
				continue;
			entity.setItemSlot(slot, ItemStack.EMPTY);
			entity.spawnAtLocation(stack);
		}
	}

	private static void addEffect(LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
		int duration, int amplifier) {
		target.addEffect(new MobEffectInstance(effect, duration, amplifier));
	}

}