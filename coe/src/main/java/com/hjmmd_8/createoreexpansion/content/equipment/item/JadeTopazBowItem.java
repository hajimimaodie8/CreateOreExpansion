package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillRelease;
import com.hjmmd_8.createoreexpansion.integration.skiller.CoeSkillTypes;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.factory.BowContextFactory;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import net.minecraft.server.level.ServerPlayer;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergyColors;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import java.awt.Color;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * 弓族物品类 —— <b>四把弓共用</b>（翠玉 {@code jade_topaz_bow} / 宝石 {@code sapphire_ruby_bow}
 * / 星界 {@code astral_bow} / 雷鸣 {@code thunder_bow}），模组组件化能量体系。
 *
 * <p><b>2026-10-03「把弓补齐到 4 把」批 1</b>：本类原先只服务翠玉之弓，现在由四把共用
 * —— 差异<b>只在构造时传进来的 {@link BowTier}</b>（能量上限 / 耐久上限 / 取色 / 能量条色标），
 * 行为逻辑（无箭耗能 {@value #NO_ARROW_COST}、拉弓、发射、技能释放）四把逐字相同。
 * 翠玉那把的注册值也因此一字未变：档位表里 {@code JADE_TOPAZ} 那一行就是它的现行值
 * （能量 2000 / 耐久 1536 / 取色 TOPAZ，见 {@link BowTier} 类注释的"待裁"一节）。</p>
 *
 * <p>能量上限随档走（{@link BowTier#energy()} = 500 / 2000 / 5000 / 5000 的口径表，
 * 翠玉档按红线保持现行 2000），耐久上限同表（{@link BowTier#durability()}）。</p>
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
 * <p><b>2026-10-03 弓技能批 1（技能继承）</b>：三把新弓<b>继承</b>翠玉之弓那两条技能
 * ——<b>复用同一对 id</b>（{@code createoreexpansion:bow_curse} / {@code :bow_disarm}，
 * 不新建 id、不新建技能条目、不动语言键），差异只在<b>起始等级</b>与<b>等级上限</b>，
 * 两者都取自 {@link BowTier#baseSkillLevel()} / {@link BowTier#maxSkillLevel()}
 * （绑定在 {@code CoeItems#inheritedBow}，运行时的唯一读取点是
 * {@link #effectiveSkillLevel(ItemStack)}）。翠玉之弓的两条技能与那四行注册链<b>一字未动</b>。</p>
 *
 * <p>本批同时补了两件事，都在本类：① 箭的来源标记 {@link #TAG_SOURCE_BOW}
 * （把基础概率效果收窄到本模组四把弓，见 {@link #isFromOurBow}）；
 * ② 技能冷却的载体边界 {@link BowTier#perSkillCooldown()}（翠玉按物品记、三把继承弓按技能记）。</p>
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
	/**
	 * 箭 persistentData：<b>射出它的那把弓是哪一个本模组物品</b>（注册 id 字符串，
	 * 例如 {@code createoreexpansion:sapphire_ruby_bow}）。
	 *
	 * <p><b>2026-10-03 弓技能批 1 第 5 条（生效范围补门）</b>：基础概率效果
	 * （{@code JadeTopazBowEventHandler#applyBaseEffects} 的 50%/20%/20%/10%）原先对
	 * <b>任何玩家的任何箭</b>生效 —— 于是原版弓、别家模组的弓射出的箭也会带这套效果。
	 * 现在收窄为「箭来自本模组四把弓」，判据就是本标记：</p>
	 * <ul>
	 *   <li><b>写</b>：{@link #shootProjectile}（本模组四把弓共用的发射点，<b>无条件</b>写，
	 *       与是否按了技能键无关）——形状照 {@link #TAG_SKILL} 的做法：同一个
	 *       {@code persistentData} 上的一个字符串键，发射时写、命中时读；</li>
	 *   <li><b>读</b>：{@link #isFromOurBow} —— 唯一判据，命中处理器拿它给基础效果补门。</li>
	 * </ul>
	 * <p>⚠ 技能那一段<b>不</b>改判据：技能箭本来就带 {@link #TAG_SKILL}（只有本模组弓会写），
	 * 且两段式（松手写标记 / 命中读标记）一个字没动。</p>
	 */
	public static final String TAG_SOURCE_BOW = "jade_topaz_source_bow";

	/** 无箭时发射魔法箭消耗的能量 */
	public static final int NO_ARROW_COST = 10;

	/** 普通箭伤害倍率（弓的基础高伤特性） */
	public static final float DAMAGE_MULTIPLIER = 2.0F;
	/** 技能二（缴械风暴）箭伤害倍率 */
	public static final float SKILL_B_MULTIPLIER = 4.0F;
	/** 拉满所需 tick */
	public static final float MAX_PULL_TIME = 25.0F;

	/**
	 * 这一把所属的<b>档位</b>（能量上限 / 耐久上限 / 取色 / 能量条色标的唯一来源，
	 * 见 {@link BowTier}）。
	 *
	 * <p>⚠ 它在<b>注册期</b>就固定下来（每把弓 = 一个物品实例 + 一份档），不是逐堆可变的组件值。
	 * 本类自 2026-10-03 弓补齐那轮起由<b>四把弓共用</b>（翠玉 / 宝石 / 星界 / 雷鸣），
	 * 档位差异全部落在这一个字段上，行为逻辑四把逐字相同。</p>
	 */
	private final BowTier tier;

	public JadeTopazBowItem(BowTier tier, Properties properties) {
		// 耐久上限随档走（形态就是本类原来的 384 * 4，只是把那个写死的数换成档位表）。
		super(properties.durability(tier.durability()));
		this.tier = tier;
	}

	/** 本把弓的档位（能量/耐久/取色/技能四项派生量的唯一来源，见 {@link BowTier}）。 */
	public BowTier tier() {
		return tier;
	}

	/**
	 * <b>本把弓上技能的有效等级 —— 唯一读取点</b>（2026-10-03 弓技能批 1；形态照
	 * {@code BoomerangItem#effectiveSkillLevel(ItemStack, int)}，回旋镖那轮的同一件事）。
	 *
	 * <p>口径 = {@code SkillEnergyCost.effectiveLevel(stack, 基准, 上限)}：
	 * 基准取 {@link BowTier#baseSkillLevel()}（1/2/3/3），上限取 {@link BowTier#maxSkillLevel()}
	 * （翠玉 5 / 三把继承弓 3），两者都来自档位表 ⇒ 这一行里没有任何等级字面量。
	 * 技艺提升 / 技艺回溯照旧由 {@code SkillEnergyCost} 从物品附魔读
	 * （{@code skill_boostable} 标签含 {@code #createoreexpansion:skill_tools}，四把弓都在里面）。</p>
	 *
	 * <p><b>为什么等级上限住在档位表而不动 {@code AllSkills} 的 {@code .maxLevel(...)}</b>：
	 * 两条弓技能是<b>复用同一对 id</b>绑到四把弓上的（作者第 3 条：不新建 id），而
	 * {@code AllSkills} 的 {@code maxLevel} 是<b>按技能注册</b>的一份值 —— 改成 3 会连
	 * 翠玉之弓一起改（作者第 7 条明令翠玉那两条的 maxLevel 不动 = 默认 5）。上限本来就是
	 * 「哪把弓」的属性，所以落在档位表这一处真源。</p>
	 *
	 * <p>消费点两处、共用这一个读数：{@code BowShootItemSkill#release}（写进弓/箭的
	 * {@link #TAG_SKILL_LEVEL}）与同类的 {@code consumeResource}（按同一等级算能量费）；
	 * 命中段的 {@code JadeTopazBowEventHandler} 只读箭上写好的那个等级
	 * —— <b>两段式必须同改，改一边不改另一边就是静默失效</b>。</p>
	 */
	public int effectiveSkillLevel(ItemStack stack) {
		return SkillEnergyCost.effectiveLevel(stack, tier.baseSkillLevel(), tier.maxSkillLevel());
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
			// 生效范围补门（本批第 5 条）：本模组四把弓射出的箭带来源标记，无条件写
			// （与是否按了技能键无关；命中处理器据它决定要不要滚基础概率效果）。
			// 形状照下面 TAG_SKILL 的做法：persistentData 上一个字符串键，发射时写、命中时读。
			arrow.getPersistentData().putString(TAG_SOURCE_BOW,
					BuiltInRegistries.ITEM.getKey(this).toString());

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

	/**
	 * <b>这支箭是不是本模组四把弓射出来的</b> —— 生效范围闸门的唯一判据
	 * （2026-10-03 弓技能批 1 第 5 条；标记由 {@link #shootProjectile} 写，见 {@link #TAG_SOURCE_BOW}）。
	 *
	 * <p>原版弓 / 别家模组的弓 / 任何别的来源射出的箭都<b>没有</b>这个键 ⇒ 返回 false
	 * ⇒ 命中处理器不滚那套基础概率效果（技能那一段另有它自己的 {@link #TAG_SKILL} 判据，不受影响）。</p>
	 */
	public static boolean isFromOurBow(Arrow arrow) {
		return !arrow.getPersistentData().getString(TAG_SOURCE_BOW).isEmpty();
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

	/**
	 * <b>能量条/能量行的色标——按档问护甲那张取色表</b>（2026-10-03 弓补齐那轮）。
	 *
	 * <p>形态与回旋镖四把<b>逐字相同</b>（{@code BoomerangItem#energyGradientStops}）：
	 * 档 → 同名护甲套（{@link BowTier#armorSet()}）→ 护甲自己的取色源
	 * （{@link ArmorEnergyColors#stopsOf(com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet)}）。
	 * 弓侧<b>一个色值都不复写</b>、也不自己拼渐变，因此色标条数跟着护甲走
	 * （翠玉 2 / 宝石 2 / <b>星界 4</b> / 雷鸣 2），渲染器就是护甲 tooltip 用的那个多段重载。</p>
	 *
	 * <p>⚠ <b>翠玉之弓的渲染结果零变化</b>：翠玉套的色标（{@code 0x55FF55 → 0xFFFF55}）
	 * 与 {@code EnergyTooltipHandler} 里弓那条历史默认<b>逐字同值、同序</b>
	 * （护甲那张表的注释写明"与翠玉之弓同款"，两边本来就是一份）。
	 * 所以本方法只是把"翠玉之弓靠默认值"改成"四把弓都自己回答"，颜色一格没动。</p>
	 */
	@Override
	public List<Color> energyGradientStops(ItemStack stack) {
		return ArmorEnergyColors.stopsOf(tier.armorSet());
	}
}
