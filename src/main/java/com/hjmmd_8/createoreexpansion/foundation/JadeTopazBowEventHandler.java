package com.hjmmd_8.createoreexpansion.foundation;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.skill.BowCurseSkill;
import com.hjmmd_8.createoreexpansion.content.skill.BowDisarmSkill;
import com.hjmmd_8.createoreexpansion.common.AllModEffects;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowCurseConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowDisarmConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * 翠玉之弓命中效果处理器。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>读取箭上携带的技能标记（发射时写入），命中生物实体后分发到对应技能效果；</li>
 *     <li>所有命中（含普通箭）都会滚动一次基础概率效果（凋零/缓慢/转化紊乱+缴械）。</li>
 * </ul>
 *
 * <p>技能效果逻辑在各技能类（{@link BowCurseSkill} / {@link BowDisarmSkill}）内，
 * 此处只负责按技能 id 分发。</p>
 */
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
		if (!(event.getRayTraceResult() instanceof EntityHitResult hit))
			return;
		if (!(hit.getEntity() instanceof LivingEntity target))
			return;

		// 基础概率效果（普通箭与技能箭均触发）
		applyBaseEffects(player, target);

		// 技能效果分发：按箭上携带的技能 id 与等级（一技能多等级，等级决定效果数值）
		String skillId = arrow.getPersistentData().getString(JadeTopazBowItem.TAG_SKILL);
		if (skillId.isEmpty())
			return;
		int level = arrow.getPersistentData().getInt(JadeTopazBowItem.TAG_SKILL_LEVEL);
		ItemSkill skill = AllSkills.get(ResourceLocation.tryParse(skillId));
		if (skill instanceof BowCurseSkill) {
			BowCurseConfig config = configFor(skill, level, BowCurseConfig.class);
			if (config != null)
				BowCurseSkill.applyTo(player, target, config);
		} else if (skill instanceof BowDisarmSkill) {
			BowDisarmConfig config = configFor(skill, level, BowDisarmConfig.class);
			if (config != null)
				BowDisarmSkill.applyTo(player, target, config);
		}
	}

	/** 按技能 id 与等级取该等级的实际配置（一技能多等级；未知技能/无配置返回 null） */
	private static <C extends SkillConfig> C configFor(ItemSkill skill, int level, Class<C> type) {
		AllSkills.RegisteredDataSkill registered = AllSkills.getData(AllSkills.getId(skill));
		if (registered == null)
			return null;
		SkillConfig config = registered.configForLevel(Math.max(1, level));
		return config != null ? type.cast(config) : null;
	}

	/** 基础概率效果：50% 无；20% 凋零；20% 缓慢；10% 转化紊乱 + 缴械主手 */
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

	private static void addEffect(LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
		int duration, int amplifier) {
		target.addEffect(new MobEffectInstance(effect, duration, amplifier));
	}
}
