package com.hjmmd_8.createoreexpansion.item;

import com.hjmmd_8.createoreexpansion.tool.ToolEnergy;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

public class JadeTopazBowItem extends BowItem {

	public static final String ENERGY_TAG = "energy";
	public static final String SKILL_TAG = "jade_topaz_skill";
	public static final int MAX_ENERGY = 100;
	public static final int SKILL_ENERGY_COST = 10;
	public static final int MISSING_ARROW_ENERGY_COST = 2;
	public static final int SKILL_ENERGY_THRESHOLD = 20;
	public static final int SKILL_COOLDOWN = 5 * 20;
	public static final float DAMAGE_MULTIPLIER = 2.0F;
	public static final float SKILL_B_DAMAGE_MULTIPLIER = 4.0F;

	public JadeTopazBowItem(Properties properties) {
		super(properties.durability(384 * 4));
	}

	public static float getPowerForTime(int charge) {
		float f = (float) charge / 25.0F;
		f = (f * f + f * 2.0F) / 3.0F;
		if (f > 1.0F)
			f = 1.0F;
		return f;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean hasArrows = !player.getProjectile(stack).isEmpty();

		InteractionResultHolder<ItemStack> ret = EventHooks.onArrowNock(stack, level, player, hand, hasArrows);
		if (ret != null)
			return ret;

		if (!player.hasInfiniteMaterials() && !hasArrows && getEnergy(stack) < MISSING_ARROW_ENERGY_COST)
			return InteractionResultHolder.fail(stack);

		setSkill(stack, player.isShiftKeyDown() ? "A" : player.isSprinting() ? "B" : "NONE");
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player))
			return;

		ItemStack ammo = player.getProjectile(stack);
		boolean hasAmmo = !ammo.isEmpty();
		boolean infinite = player.hasInfiniteMaterials();
		if (!hasAmmo && !infinite && getEnergy(stack) < MISSING_ARROW_ENERGY_COST)
			return;

		int i = this.getUseDuration(stack, entity) - timeLeft;
		i = EventHooks.onArrowLoose(stack, level, player, i, hasAmmo || infinite);
		if (i < 0)
			return;

		float power = getPowerForTime(i);
		if (power < 0.1F)
			return;

		String skill = resolveSkill(player, stack);
		boolean skillActive = !"NONE".equals(skill)
			&& !player.getCooldowns().isOnCooldown(this)
			&& getEnergy(stack) >= SKILL_ENERGY_THRESHOLD;
		if (!skillActive) {
			if (!"NONE".equals(skill))
				ToolEnergy.sendLowEnergy(player);
			skill = "NONE";
		}

		if (skillActive) {
			setEnergy(stack, getEnergy(stack) - SKILL_ENERGY_COST);
			player.getCooldowns().addCooldown(this, SKILL_COOLDOWN);
		}
		setSkill(stack, skill);

		if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
			List<ItemStack> projectiles;
			if (hasAmmo) {
				projectiles = draw(stack, ammo, player);
			} else {
				ItemStack magicArrow = Items.ARROW.getDefaultInstance();
				magicArrow.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
				if (!infinite)
					setEnergy(stack, getEnergy(stack) - MISSING_ARROW_ENERGY_COST);
				projectiles = List.of(magicArrow);
			}

			if (!projectiles.isEmpty()) {
				shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles,
					power * 3.0F, 1.0F, power == 1.0F, null);
			}
		}

		ToolEnergy.sendEnergyActionBar(player, stack);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
			1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
		player.awardStat(Stats.ITEM_USED.get(this));
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index,
		float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
		projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F, velocity, inaccuracy);

		if (projectile instanceof Arrow arrow && shooter instanceof Player player) {
			String skill = getSkill(player.getUseItem());
			if ("B".equals(skill)) {
				arrow.setBaseDamage(arrow.getBaseDamage() * SKILL_B_DAMAGE_MULTIPLIER);
				arrow.getPersistentData().putString(SKILL_TAG, "B");
			} else {
				arrow.setBaseDamage(arrow.getBaseDamage() * DAMAGE_MULTIPLIER);
				arrow.getPersistentData().putString(SKILL_TAG, skill);
			}
		}
	}

	private static String resolveSkill(Player player, ItemStack stack) {
		String stored = getSkill(stack);
		boolean a = "A".equals(stored) || player.isShiftKeyDown();
		boolean b = "B".equals(stored) || player.isSprinting();
		if (a)
			return "A";
		if (b)
			return "B";
		return "NONE";
	}

	public static int getEnergy(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.contains(ENERGY_TAG) ? tag.getInt(ENERGY_TAG) : MAX_ENERGY;
	}

	public static void setEnergy(ItemStack stack, int energy) {
		int value = Math.max(0, Math.min(MAX_ENERGY, energy));
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
			data -> data.update(tag -> tag.putInt(ENERGY_TAG, value)));
	}

	public static String getSkill(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.getString(SKILL_TAG);
	}

	public static void setSkill(ItemStack stack, String skill) {
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
			data -> data.update(tag -> tag.putString(SKILL_TAG, skill)));
	}

}