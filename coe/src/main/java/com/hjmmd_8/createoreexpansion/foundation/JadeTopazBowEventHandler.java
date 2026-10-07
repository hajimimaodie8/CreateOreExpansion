package com.hjmmd_8.createoreexpansion.foundation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveEssenceEffects;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowAstralBarrageLauncher;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowHitEffects;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowMetaArrowTrait;
import com.hjmmd_8.createoreexpansion.content.equipment.item.BowThunderMightLauncher;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowCurseConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowDisarmConfig;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 *     <li>读取箭上携带的技能标记（发射时写入），命中生物实体后分发到对应效果
 *         （凋零诅咒 / 缴械风暴 / <b>雷鸣神力</b> —— 最后这条是 2026-10-07 批 19 加进来的，
 *         见下面「批 19」那段）；</li>
 *     <li>本模组四把弓射出的箭（箭上有 {@link JadeTopazBowItem#TAG_SOURCE_BOW} 来源标记）命中时
 *         滚动一次基础概率效果（凋零/缓慢/转化紊乱+缴械）；</li>
 *     <li>被动技能「元矢自生」（2026-10-04 批 3）：箭上带着魔素标记
 *         （{@link JadeTopazBowItem#TAG_META_ESSENCE}，只有"无箭射击"会写）时，把它交给魔素层
 *         "对生物"的那一支（{@code WaveEssenceEffects#applyEssenceOnCreatureHit}）；</li>
 *     <li><b>星界弓「星元波置」的弹幕箭不许伤害施放者</b>（2026-10-05 批 6）：
 *         {@code BowAstralBarrageLauncher#isBarrageArrow} 为真且命中者就是箭的主人时，
 *         <b>取消这一次命中</b>（原版语义：这次命中不处理、箭继续飞）。判据与施加面都在
 *         那一处，本类不重复写"谁是主人"。</li>
 * </ul>
 *
 * <p><b>基础效果的生效范围（2026-10-03 弓技能批 1 第 5 条）</b>：原先"任何玩家的任何箭"都会滚
 * 那套 50%/20%/20%/10% —— 原版弓、别家模组的弓射出的箭也带这套效果。现在收窄为
 * "<b>箭来自本模组四把弓</b>"，判据是发射点无条件写下的来源标记
 * （{@link JadeTopazBowItem#isFromOurBow}，唯一判据）。技能那一段不变：它的判据是
 * {@link JadeTopazBowItem#TAG_SKILL}（只有本模组弓会写）。</p>
 *
 * <p><b>分发已改成按 id 字符串</b>（2026-09-30 技能换核第 3 阶段）：原先用
 * {@code skill instanceof BowCurseSkill} 判断，为此必须保留旧技能实现类；现在改为比较
 * {@code createoreexpansion:bow_curse} / {@code :bow_disarm} 两个 id，
 * 效果本体搬到 {@link BowHitEffects}，旧实现类因此可以整体删除。</p>
 *
 * <p><b>存档兼容</b>：箭上的标记键 {@link JadeTopazBowItem#TAG_SKILL} 与
 * {@link JadeTopazBowItem#TAG_SKILL_LEVEL} 一字未动，老存档里的飞行中箭矢照旧生效；
 * 等级仍经 {@link AllSkills#getData(ResourceLocation)} 取该等级的实际配置。</p>
 *
 * <p><b>2026-10-07 批 19：雷鸣神力（「像那把特殊的剑」）</b>—— 作者原话（逐字）：
 * 「<b>先给它暂时改成那种特殊的剑，击中生物之后会释放电荷效果。</b>」⇒ 雷鸣弓那一发
 * <b>不再</b>在松手那一刻用射线结算，而是<b>照常飞出一支可见的箭</b>；电荷与真雷改在
 * <b>这支箭命中生物时</b>、于<b>箭的命中点</b>结算。落法：本处理器新增
 * {@link #BOW_THUNDER_MIGHT_ID} 一支（与上面两条继承技能<b>同一个</b> {@code TAG_SKILL} 分发），
 * 把 {@code hit.getLocation()}（{@code EntityHitResult} 的精确命中点）交给
 * {@code BowThunderMightLauncher#strikeAt}。</p>
 * <p>⚠ <b>"引爆的那个点"与"施加的那个面"分工没变</b>：范围（边长 2/3/4 的水平方形）、
 * 垂直容差、真雷概率、以及"电荷一律经 {@code LightningEssenceHitCharge#applyOnHit} →
 * {@code ChargeApi.applyRandom}"这条唯一施加面，全部一个字未改（那就是雷鸣合金剑那条既有途径
 * 用的同一个面）；变的只有<b>谁在什么时候把落点交出来</b>。</p>
 * <p>⚠ <b>打空 / 打墙没有表现</b>：本处理器最上面那几道闸门要求事件结果是
 * {@code EntityHitResult} 且命中一个 {@link LivingEntity} ⇒ 触发条件就是作者那句
 * "击中生物之后"（箭照原版那样继续飞 / 插在地上）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public class JadeTopazBowEventHandler {

	/** 凋零诅咒技能 id（原 {@code BowCurseSkill}）。 */
	private static final ResourceLocation BOW_CURSE_ID = CoeCore.modLoc("bow_curse");
	/** 缴械风暴技能 id（原 {@code BowDisarmSkill}）。 */
	private static final ResourceLocation BOW_DISARM_ID = CoeCore.modLoc("bow_disarm");
	/**
	 * <b>雷鸣神力技能 id</b>（{@code AllSkills.BOW_THUNDER_MIGHT}，批 7 起是正式条目）。
	 *
	 * <p>2026-10-07 批 19 新增这一支：作者把雷鸣神力改成"像那把特殊的剑 —— 击中生物之后才释放
	 * 电荷效果"，于是这一发的技能标记（弓的 {@code stampThunderMightForArrow} 写、
	 * {@code shootProjectile} 搬上箭）从"发射侧自己结算"变成"在这里按 id 分发"。
	 * 判据与上面两条继承技能<b>逐字同形</b>（同一个 {@code TAG_SKILL} + 同一个
	 * {@code TAG_SKILL_LEVEL}），所以它落进的是<b>既有分发</b>，不是第二条通道。</p>
	 */
	private static final ResourceLocation BOW_THUNDER_MIGHT_ID = CoeCore.modLoc("bow_thunder_might");

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

		// 弓技能批 6（星界弓「星元波置」）：弹幕里的药水箭<b>不许伤害施放者</b>。
		// 为什么需要这一支：本技能的圆盘圆心在施放者<b>前方 4 格</b>、半径可达 <b>4 格</b>
		// ⇒ 他自己就站在圆盘边上，而原版箭一旦 leftOwner 就<b>可以</b>命中主人
		// （Projectile#canHitEntity 只排除"同乘"），本模组的波那一侧靠 isOwner 排除、
		// 箭这一侧没有对应机制 ⇒ 在这里按"排除施放者"的既有约定取消这一次命中。
		// 取消的语义就是原版 ProjectileImpactEvent 的语义：<b>这次命中不处理，箭继续飞</b>
		// （箭会照常落下去），所以"主人被自己降下的箭扎一下"在代码里没有入口。
		// ⚠ 这一支必须在下面的 isFromOurBow 来源闸门<b>之前</b>：弹幕箭不是弓射出来的
		// （它不带来源标记），放到闸门之后就永远走不到这里。
		if (BowAstralBarrageLauncher.isBarrageArrow(arrow) && arrow.getOwner() == target) {
			event.setCanceled(true);
			return;
		}

		// 生效范围闸门（2026-10-03 弓技能批 1 第 5 条）：基础概率效果只对"本模组四把弓射出的箭"
		// 生效。发射点在 JadeTopazBowItem#shootProjectile 无条件写下来源标记（四把弓共用那一个点）；
		// 原版弓 / 别家模组的弓 / 别的来源的箭没有这个键 ⇒ 直接返回，不再滚那 50%/20%/20%/10%。
		// ⚠ 只收窄这一段：下面技能那一段的判据是箭上的技能标记（只有本模组弓会写），一个字没动。
		if (!JadeTopazBowItem.isFromOurBow(arrow))
			return;

		// 基础概率效果（普通箭与技能箭均触发）
		applyBaseEffects(player, target);

		// 被动技能「元矢自生」（2026-10-04 弓技能批 3）：这一发箭若带着魔素（只有本模组四把弓的
		// "无箭射击"会写 TAG_META_ESSENCE），命中生物时等效于"一枚带魔素的攻击波打中该生物"。
		// 它与上面那条基础概率效果、下面那条技能分发都互不影响（三个标记各读各的）。
		applyMetaArrowEssence(arrow, target);

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
		} else if (BOW_THUNDER_MIGHT_ID.equals(id) && arrow.level() instanceof ServerLevel serverLevel) {
			// ★ 2026-10-07 批 19：雷鸣弓「雷鸣神力」的<b>结算点</b>（作者原话："先给它暂时改成
			//   那种特殊的剑，击中生物之后会释放电荷效果"）—— 从"松手那一刻的射线 hitscan"
			//   搬到这里：这一发<b>照常飞出一支可见的箭</b>（弓物品侧的 stampThunderMightForArrow
			//   只登记、不吞），等这支箭<b>命中生物</b>时，在<b>箭的命中点</b>结算电荷与真雷。
			//
			//   落点 = hit.getLocation()（EntityHitResult 的精确命中点）：不是施放者坐标，
			//   也不再由发射侧现算射线（那个 pickImpact 已随本批删除）。
			//   触发条件 = 本处理器最上面那几道闸门：事件的结果必须是 EntityHitResult
			//   且命中一个 LivingEntity ⇒ 打空 / 打墙走不到这一支（作者原话为准）；
			//   目标是不是玩家由 ChargeApi / 电荷面自己的既有口径回答，本处理器不重复写。
			//
			//   范围与真雷概率一个字未改：全部按名取自 BowThunderMightConfigs（弓侧传下来的
			//   那个等级就是槽 2 的有效等级，与费用侧同源）。
			BowThunderMightLauncher.strikeAt(serverLevel, player, hit.getLocation(), level);
		}
	}

	/**
	 * <b>被动技能「元矢自生」的命中段</b>（2026-10-04 弓技能批 3）：箭上带着
	 * {@link JadeTopazBowItem#TAG_META_ESSENCE} ⇒ 把那种魔素交给魔素层"对生物"的那一支
	 * （{@code WaveEssenceEffects#applyEssenceOnCreatureHit}：八支与"打中玩家"那一支一一对应，
	 * 其中雷那一支按作者 2026-10-04 的统一标准给随机电荷）。
	 *
	 * <p>三道判据，顺序即优先级：</p>
	 * <ol>
	 *   <li><b>箭上没有魔素标记 ⇒ 什么都不做</b>：原版弓 / 别家模组的弓 / 本模组弓的普通射击
	 *       （有箭）与一切没中签的无箭射击都走这一支 —— 与改造前逐字相同；</li>
	 *   <li><b>标记认不出来 ⇒ 什么都不做</b>：走 {@link BowMetaArrowTrait#essenceByName} 的容错解析
	 *       （不 {@code valueOf}：箭上的字符串是存档里跟着实体走的 NBT，未知值只能读成"没有"，
	 *       不许在命中处理里抛异常）；</li>
	 *   <li><b>玩家目标 ⇒ 什么都不做</b>：这一支是"魔素打中<b>生物</b>"，判据在魔素层
	 *       （{@code applyEssenceOnCreatureHit} 第一句就把 {@link Player} 挡掉，玩家那一侧有它
	 *       自己的入口 {@code applyOnPlayerHit}）—— 本处理器不重复写这条判据。</li>
	 * </ol>
	 *
	 * <p>⚠ 生效范围：标记只由 {@link JadeTopazBowItem#shootProjectile} 写（"无箭射击"那条路），
	 * 而本方法所在的事件处理器在它<b>之前</b>已经过了 {@link JadeTopazBowItem#isFromOurBow} 那道
	 * 来源闸门 ⇒ 非本模组弓的箭连这里都到不了（复用同一条判据，不另立第二套）。</p>
	 */
	private static void applyMetaArrowEssence(Arrow arrow, LivingEntity target) {
		String essence = arrow.getPersistentData()
			.getString(JadeTopazBowItem.TAG_META_ESSENCE);
		if (essence.isEmpty()) {
			return;
		}
		WaveTrailStyle style = BowMetaArrowTrait.essenceByName(essence);
		if (style == null) {
			return;
		}
		WaveEssenceEffects.applyEssenceOnCreatureHit(arrow, target, style);
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
