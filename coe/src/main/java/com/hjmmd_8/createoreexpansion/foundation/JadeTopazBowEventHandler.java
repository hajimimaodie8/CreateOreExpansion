package com.hjmmd_8.createoreexpansion.foundation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowHitEffects;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowCurseConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowDisarmConfig;
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
 *     <li>读取箭上携带的技能标记（发射时写入），命中生物实体后分发到对应效果；</li>
 *     <li>所有命中（含普通箭）都会滚动一次基础概率效果（凋零/缓慢/转化紊乱+缴械）。</li>
 * </ul>
 *
 * <p><b>分发已改成按 id 字符串</b>（2026-09-30 技能换核第 3 阶段）：原先用
 * {@code skill instanceof BowCurseSkill} 判断，为此必须保留旧技能实现类；现在改为比较
 * {@code createoreexpansion:bow_curse} / {@code :bow_disarm} 两个 id，
 * 效果本体搬到 {@link BowHitEffects}，旧实现类因此可以整体删除。</p>
 *
 * <p><b>存档兼容</b>：箭上的标记键 {@link JadeTopazBowItem#TAG_SKILL} 与
 * {@link JadeTopazBowItem#TAG_SKILL_LEVEL} 一字未动，老存档里的飞行中箭矢照旧生效；
 * 等级仍经 {@link AllSkills#getData(ResourceLocation)} 取该等级的实际配置。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public class JadeTopazBowEventHandler {

	/** 凋零诅咒技能 id（原 {@code BowCurseSkill}）。 */
	private static final ResourceLocation BOW_CURSE_ID = CoeCore.modLoc("bow_curse");
	/** 缴械风暴技能 id（原 {@code BowDisarmSkill}）。 */
	private static final ResourceLocation BOW_DISARM_ID = CoeCore.modLoc("bow_disarm");

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
		ResourceLocation id = ResourceLocation.tryParse(skillId);
		if (id == null)
			return;
		int level = arrow.getPersistentData().getInt(JadeTopazBowItem.TAG_SKILL_LEVEL);

		if (BOW_CURSE_ID.equals(id)) {
			BowCurseConfig config = configFor(id, level, BowCurseConfig.class);
			if (config != null)
				BowHitEffects.applyCurse(player, target, config);
		} else if (BOW_DISARM_ID.equals(id)) {
			BowDisarmConfig config = configFor(id, level, BowDisarmConfig.class);
			if (config != null)
				BowHitEffects.applyDisarm(player, target, config);
		}
	}

	/** 按技能 id 与等级取该等级的实际配置（一技能多等级；未知技能/无配置返回 null） */
	private static <C extends SkillConfig> C configFor(ResourceLocation id, int level, Class<C> type) {
		AllSkills.RegisteredDataSkill registered = AllSkills.getData(id);
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
			// W6-b2：嬗化机制整块搬进 :coe 之后，这条引用<b>本来就是同层</b>——
			// TransmutationEffects 与 TransmutationFluids 现在住 coe/src/main/java
			// （包名仍是 common.registry.transmutation），所以可以直接读 holder。
			// 恢复成拆分前 9e7b9dc1~1 的原文形状：直接传 holder、<b>不判空</b>
			// （同层静态字段永不为 null，addEffect 的形参本身就是 Holder<MobEffect>）。
			// 被撤掉的中间态是 core 的嬗化窄契约（原住 core/.../common/transmutation/，
			// W6-b2 整类删除；历史名称见 build/patch/w6b2-EVIDENCE.txt），
			// 它的"未注入 = 跳过嬗乱"降级语义随契约一起消失。
			addEffect(target, TransmutationEffects.TRANSMUTATION_DISORDER, 60, 0);

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
