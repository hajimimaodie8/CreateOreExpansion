package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

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

	public static final String TAG_ENERGY = "energy";
	public static final String TAG_SKILL = "jade_topaz_skill";

	public static final int MAX_ENERGY = 100;
	public static final int SKILL_COST = 10;
	public static final int NO_ARROW_COST = 2;
	public static final int SKILL_THRESHOLD = 20;
	public static final int SKILL_COOLDOWN = 5 * 20; // 5 秒

	public static final float DAMAGE_MULTIPLIER = 2.0F;
	public static final float SKILL_B_MULTIPLIER = 4.0F;
	public static final float MAX_PULL_TIME = 25.0F;

	public JadeTopazBowItem(Properties properties) {
		super(properties.durability(384 * 4));
	}

	public static float getPowerForTime(int charge) {
		float f = charge / MAX_PULL_TIME;
		f = (f * f + f * 2.0F) / 3.0F;
		return Math.min(f, 1.0F);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean hasArrows = !player.getProjectile(stack).isEmpty();

		// 事件钩子
		InteractionResultHolder<ItemStack> ret = EventHooks.onArrowNock(stack, level, player, hand, hasArrows);
		if (ret != null) return ret;

		// 检查能否射击
		if (!player.hasInfiniteMaterials() && !hasArrows && getEnergy(stack) < NO_ARROW_COST) {
			return InteractionResultHolder.fail(stack);
		}

		// 记录技能模式
		setSkill(stack, detectSkill(player));
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	// ========== 松手：射击 ==========
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (!(entity instanceof Player player)) return;

		// 1. 计算蓄力
		int pullTime = getUseDuration(stack, entity) - timeLeft;
		pullTime = EventHooks.onArrowLoose(stack, level, player, pullTime,
				player.getProjectile(stack).isEmpty());
		if (pullTime < 0) return;

		float power = getPowerForTime(pullTime);
		if (power < 0.1F) return;

		// 2. 处理技能
		String skill = resolveSkill(player, stack);

		// 3. 准备弹药
		List<ItemStack> projectiles = prepareProjectiles(stack, player);
		if (projectiles == null || projectiles.isEmpty()) return;

		// 4. 发射
		if (level instanceof ServerLevel serverLevel) {
			shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles,
					power * 3.0F, 1.0F, power >= 1.0F, null);
		}

		// 5. 后处理
		ToolEnergy.sendEnergyActionBar(player, stack);
		playShootSound(level, player, power);
		player.awardStat(Stats.ITEM_USED.get(this));
	}

	private List<ItemStack> prepareProjectiles(ItemStack bow, Player player) {
		ItemStack ammo = player.getProjectile(bow);
		boolean infinite = player.hasInfiniteMaterials();

		// 有实体箭
		if (!ammo.isEmpty()) {
			return draw(bow, ammo, player);
		}

		// 无箭但能量够 → 魔法箭
		if (getEnergy(bow) >= NO_ARROW_COST || infinite) {
			if (!infinite) {
				setEnergy(bow, getEnergy(bow) - NO_ARROW_COST);
			}
			ItemStack magicArrow = Items.ARROW.getDefaultInstance();
			magicArrow.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
			return List.of(magicArrow);
		}

		return List.of();
	}

	private String detectSkill(Player player) {
		if (player.isShiftKeyDown()) return "A";
		if (player.isSprinting()) return "B";
		return "NONE";
	}

	private String resolveSkill(Player player, ItemStack stack) {
		String skill = getSkill(stack);
		boolean wantsSkill = !skill.equals("NONE");

		// 没有请求技能
		if (!wantsSkill) return "NONE";

		// 检查冷却和能量
		boolean canUseSkill = !player.getCooldowns().isOnCooldown(this)
				&& getEnergy(stack) >= SKILL_THRESHOLD;

		if (!canUseSkill) {
			ToolEnergy.sendLowEnergy(player);
			return "NONE";
		}

		// 消耗
		setEnergy(stack, getEnergy(stack) - SKILL_COST);
		player.getCooldowns().addCooldown(this, SKILL_COOLDOWN);
		setSkill(stack, skill);
		return skill;
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index,
								   float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {

		projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle,
				0.0F, velocity, inaccuracy);

		if (projectile instanceof Arrow arrow && shooter instanceof Player player) {
			String skill = getSkill(player.getUseItem());
			float multiplier = "B".equals(skill) ? SKILL_B_MULTIPLIER : DAMAGE_MULTIPLIER;

			arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
			arrow.getPersistentData().putString(TAG_SKILL, skill);
		}
	}

	public static int getEnergy(ItemStack stack) {
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return tag.getInt(TAG_ENERGY) != 0 || tag.contains(TAG_ENERGY)
				? tag.getInt(TAG_ENERGY)
				: MAX_ENERGY;
	}

	public static void setEnergy(ItemStack stack, int energy) {
		int clamped = Math.clamp(energy, 0, MAX_ENERGY);
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> tag.putInt(TAG_ENERGY, clamped)));
	}

	public static String getSkill(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getString(TAG_SKILL);
	}

	public static void setSkill(ItemStack stack, String skill) {
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> tag.putString(TAG_SKILL, skill)));
	}

	private void playShootSound(Level level, Player player, float power) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
	}
}