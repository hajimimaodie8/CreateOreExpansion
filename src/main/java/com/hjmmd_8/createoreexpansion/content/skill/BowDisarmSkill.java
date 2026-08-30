package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.BowDisarmConfig;
import com.hjmmd_8.createoreexpansion.content.skill.context.BowShootContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.AbstractSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 翠玉之弓技能 B —— 缴械风暴（一技能多等级，数值统一在 {@link BowDisarmConfigs} 修改）。
 *
 * <p>射击时释放（BowShootContext）：按有效等级加载配置并消耗能量 + 进入冷却，
 * 在弓上标记本次射击携带的技能 id 与等级；箭命中目标后由
 * {@code JadeTopazBowEventHandler} 读取标记、按等级取配置调用 {@link #applyTo} 生效。</p>
 *
 * <p>效果：以目标为中心按配置范围对所有非玩家生物缴械（主手武器）；
 * 按配置概率武器直接进入射手背包（背包满则掉落），剩余概率掉落在地；
 * 掉落模式下按配置决定是否对怪物额外扒副手 + 全部防具。</p>
 */
public class BowDisarmSkill extends AbstractSkill implements ConfigSkill<BowShootContext, BowDisarmConfig> {

	private int energyCost;
	private int cooldownSeconds;
	private float rangeRadius;
	private float intoInventoryChance;
	private boolean stripMonsterArmor;
	private DataSkill data;

	public BowDisarmSkill() {
		super();
	}

	@Override
	public SkillType getType() {
		return SkillType.USE_SKILL;
	}

	@Override
	public int getCost() {
		return energyCost;
	}

	@Override
	public int getCooldownSeconds() {
		return cooldownSeconds;
	}

	@Override
	public void load(BowDisarmConfig config, DataSkill data) {
		this.energyCost = config.energyCost;
		this.cooldownSeconds = config.cooldownSeconds;
		this.rangeRadius = config.rangeRadius;
		this.intoInventoryChance = config.intoInventoryChance;
		this.stripMonsterArmor = config.stripMonsterArmor;
		this.data = data;
	}

	@Override
	public Class<BowDisarmConfig> getConfigType() {
		return BowDisarmConfig.class;
	}

	@Override
	public void release(BowShootContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null)
			return;

		// 真正生效前消耗能量；不足则放弃（提示由 ToolEnergy 统一发送）
		if (!ToolEnergy.tryConsume(player, ctx.bow(), this))
			return;
		ToolSkillCooldown.startTicks(player, ctx.bow(), cooldownSeconds * 20);

		// 标记本次射击携带的技能 id 与有效等级（发射时写入箭，命中时按等级取配置）
		ctx.bow().update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
			net.minecraft.world.item.component.CustomData.EMPTY,
			custom -> custom.update(tag -> {
				tag.putString(JadeTopazBowItem.TAG_SKILL, AllSkills.getId(this).toString());
				tag.putInt(JadeTopazBowItem.TAG_SKILL_LEVEL, effectiveLevel(ctx.bow()));
			}));
	}

	/** 本次射击携带的技能有效等级（含技能提升附魔；无数据时按 1 级） */
	private int effectiveLevel(ItemStack bow) {
		return data != null ? SkillEnergyCost.effectiveLevel(bow, data) : 1;
	}

	/** 箭命中目标后调用：按等级配置范围缴械 + 怪物扒装备 */
	public static void applyTo(Player player, LivingEntity target, BowDisarmConfig config) {
		boolean intoInventory = target.level().getRandom().nextFloat() < config.intoInventoryChance;
		AABB area = new AABB(target.blockPosition()).inflate(config.rangeRadius);
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

			if (!intoInventory && config.stripMonsterArmor && entity instanceof Monster) {
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
}
