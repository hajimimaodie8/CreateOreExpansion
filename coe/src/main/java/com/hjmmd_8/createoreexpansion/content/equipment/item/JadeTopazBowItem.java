package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.common.SkillCooldowns;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.BowContextFactory;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import net.minecraft.server.level.ServerPlayer;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
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

/**
 * 翠玉之弓 —— 传说级能量武器（能量上限 2000，模组组件化能量体系）。
 *
 * <p>技能（模组技能体系，绑定于 SKILLS 组件，tooltip 自动显示按键）：</p>
 * <ul>
 *     <li>技能一（键一，默认左 Shift）——凋零诅咒：命中附加凋零+缓慢+药水云；</li>
 *     <li>技能二（键二，默认 R）——缴械风暴：命中范围缴械+怪物扒装备。</li>
 * </ul>
 *
 * <p>释放流程：按下时锁定技能键位 → 松手射击时经 {@link SkillsComponent#releaseSkillAt}
 * 统一释放（能量预检查/消耗/冷却走模组体系）→ 发射时把技能 id 写入箭的 persistentData，
 * 命中后由 {@code JadeTopazBowEventHandler} 读取并调用对应技能效果。</p>
 *
 * <p><b>P3p</b>：实现 {@link EnergyGradientTool} —— 共享库（core）的能量门面/能量 tooltip
 * 不能再 {@code instanceof JadeTopazBowItem}（库不 import 层），改判这个零方法标记契约；
 * 判定结果对现有物品逐个相同。</p>
 */
public class JadeTopazBowItem extends BowItem implements EnergyGradientTool {

	/** 箭 persistentData / 弓暂存标记：本次射击携带的技能 id（ResourceLocation 字符串） */
	public static final String TAG_SKILL = "jade_topaz_skill";
	/** 箭 persistentData / 弓暂存标记：本次射击携带的技能有效等级（一技能多等级，命中时按等级取配置） */
	public static final String TAG_SKILL_LEVEL = "jade_topaz_skill_level";

	/** 无箭时发射魔法箭消耗的能量 */
	public static final int NO_ARROW_COST = 10;

	/** 普通箭伤害倍率（弓的基础高伤特性） */
	public static final float DAMAGE_MULTIPLIER = 2.0F;
	/** 技能二（缴械风暴）箭伤害倍率 */
	public static final float SKILL_B_MULTIPLIER = 4.0F;
	/** 拉满所需 tick */
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

		// 检查能否射击：无箭且能量不足时不允许
		if (!hasArrows && !ToolEnergy.canAfford(stack, NO_ARROW_COST)) {
			return InteractionResultHolder.fail(stack);
		}

		// 锁定本次射击的技能键位（-1=无技能；0=键一凋零诅咒；1=键二缴械风暴）
		// 同时清除上一箭遗留的技能标记（防止普通箭误带技能）
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> {
					tag.putInt("PendingSkillSlot", detectSkillSlot());
					tag.remove(TAG_SKILL);
					tag.remove(TAG_SKILL_LEVEL);
				}));
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

		// 2. 释放技能（若按了技能键）：统一走模组技能释放（能量/冷却/剩余能量提示）
		int pendingSlot = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getInt("PendingSkillSlot");
		// 拉弓时未按技能键（或按键发生在拉弓之后）：松手时实时补测一次，
		// 避免"无箭 + 释放技能"场景下技能被跳过、只扣除魔法箭能量
		if (pendingSlot < 0) {
			pendingSlot = detectSkillSlot();
		}
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
				data -> data.update(tag -> tag.remove("PendingSkillSlot")));
		releaseSkillIfRequested(player, stack, pendingSlot);

		// 3. 准备弹药
		List<ItemStack> projectiles = prepareProjectiles(stack, player);
		if (projectiles == null || projectiles.isEmpty()) return;

		// 4. 发射（shootProjectile 会把弓上的技能标记写入箭）
		if (level instanceof ServerLevel serverLevel) {
			shoot(serverLevel, player, player.getUsedItemHand(), stack, projectiles,
					power * 3.0F, 1.0F, power >= 1.0F, null);
		}
		playShootSound(level, player, power);
		player.awardStat(Stats.ITEM_USED.get(this));
		// 技能标记不清除：下次 use() 会重新覆盖（避免 shoot 时序竞态导致标记提前丢失）
	}

	/**
	 * 按锁定的技能键位释放技能：键一→槽位 0（凋零诅咒）、键二→槽位 1（缴械风暴）。
	 * 能量预检查/消耗/冷却统一由 {@link SkillsComponent#releaseSkillAt} 与技能类完成，
	 * 释放成功后技能类会在弓上写入 {@value #TAG_SKILL} 标记，供发射时写入箭。
	 */
	private void releaseSkillIfRequested(Player player, ItemStack bow, int slot) {
		if (slot < 0) return;

		// 新内核（Skiller）路径：弓技能的执行（耗能 / 冷却 / 写技能标记）全在技能实现里。
		// 弓射击没有对应的 NeoForge 事件 → 用 noEvent + extraData 传弓（绝不能调 getEvent()）。
		//
		// 2026-09-30 第 4 阶段：旧内核分支（SkillsComponent.releaseSkillAt / applySkillBoost /
		// data.skill.getCooldownSeconds）已整体删除 —— 它被迁移闸门全量拦截，属于死代码；
		// 冷却与剩余能量提示现在都由新路径负责。
		if (player instanceof ServerPlayer serverPlayer) {
			CoeSkillRelease.release(serverPlayer, CoeSkillTypes.USE,
					SkillContextEnvironment.noEvent(serverPlayer, serverPlayer.level())
							.extraData(BowContextFactory.KEY_BOW, bow));
		}
	}

	private List<ItemStack> prepareProjectiles(ItemStack bow, Player player) {
		ItemStack ammo = player.getProjectile(bow);

		// 有实体箭
		if (!ammo.isEmpty()) {
			return draw(bow, ammo, player);
		}

		// 无箭但能量够 → 魔法箭（消耗能量，不区分创造模式）
		if (ToolEnergy.canAfford(bow, NO_ARROW_COST)) {
			ToolEnergy.setEnergy(bow, ToolEnergy.getEnergy(bow) - NO_ARROW_COST);
			// 强制物品栏同步，确保客户端立即看到能量变化（与技能消耗逻辑一致）
			player.getInventory().setChanged();
			// 消耗提示（护目镜限定，与技能消耗统一格式：凝能佩行/工具行，佩用佩色、弓用黄→绿渐变）
			ToolEnergy.sendRemainingEnergyWithMedallion(player, bow,
				IMedallion.findBoundMedallion(player, bow));
			ItemStack magicArrow = Items.ARROW.getDefaultInstance();
			magicArrow.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
			return List.of(magicArrow);
		}

		return List.of();
	}

	/** 检测本次射击请求的技能键位：键一=0（凋零诅咒）、键二=1（缴械风暴）、无= -1 */
	private int detectSkillSlot() {
		if (AllKeys.SKILL_RELEASE.isPressed()) return 0;
		if (AllKeys.SKILL_RELEASE_2.isPressed()) return 1;
		return -1;
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index,
								   float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {

		projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle,
				0.0F, velocity, inaccuracy);

		if (projectile instanceof Arrow arrow && shooter instanceof Player player) {
			// 从弓读取本次射击携带的技能与等级（技能类 release 时写入）；无标记则普通箭
			String skill = getSkill(player.getUseItem());
			boolean skillB = "bow_disarm".equals(lastPathOf(skill));

			// 伤害：普通箭 ×2（弓基础高伤）；技能二（缴械风暴）箭 ×4
			float multiplier = skillB ? SKILL_B_MULTIPLIER : DAMAGE_MULTIPLIER;
			arrow.setBaseDamage(arrow.getBaseDamage() * multiplier);
			arrow.getPersistentData().putString(TAG_SKILL, skill);
			arrow.getPersistentData().putInt(TAG_SKILL_LEVEL, getSkillLevel(player.getUseItem()));
		}
	}

	/** 提取技能 id 的最后一段（如 "createoreexpansion:bow_disarm" → "bow_disarm"） */
	private static String lastPathOf(String skillId) {
		if (skillId == null || skillId.isEmpty()) return "";
		int colon = skillId.lastIndexOf(':');
		return colon >= 0 ? skillId.substring(colon + 1) : skillId;
	}

	public static String getSkill(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getString(TAG_SKILL);
	}

	/** 读取弓上暂存的技能有效等级（技能类 release 时写入；无标记为 0） */
	public static int getSkillLevel(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getInt(TAG_SKILL_LEVEL);
	}

	private void playShootSound(Level level, Player player, float power) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
	}
}
