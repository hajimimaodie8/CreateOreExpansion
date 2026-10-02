package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <b>回旋镖物品</b>（四把镖共用一个类，数值与实体类型由构造参数给）。
 *
 * <h2>一、两种投掷模式（2026-10-02 批 2：需求 §3.2 / §3.3）</h2>
 * <p>按键时长分界（作者原话"右键时间 &lt; 0.5 秒 = 点按；&gt; 0.5 秒 = 长按"，
 * 0.5 秒 = 10 tick）：</p>
 * <ul>
 *   <li><b>点按</b>（{@code heldTicks < 10}）：直线飞行，消耗与冷却按档取
 *       {@link BoomerangTier#tapCost()} / {@link BoomerangTier#tapCooldownTicks()}；</li>
 *   <li><b>长按</b>（{@code heldTicks >= 10}，<b>含等号</b>）：花瓣曲线飞行，取
 *       {@link BoomerangTier#holdCost()} / {@link BoomerangTier#holdCooldownTicks()}。</li>
 * </ul>
 * <p>机制照抄 {@code JadeTopazBowItem:107-108}（作者裁定 D4）：{@link #use} 里
 * {@code player.startUsingItem(hand)} + 返回 {@code consume}，松手走原版的
 * {@link #releaseUsing}。<b>"轻点"（同一 tick 按下抬起）也走同一条路</b>：那一 tick 里
 * {@code useItemRemaining} 还是 {@code MAX_USE_DURATION} ⇒ {@code heldTicks = 0 < 10} ⇒ 点按照投
 * （不会出现"轻点没反应"）。</p>
 *
 * <h2>二、蓄力封顶 40 tick（2 秒）—— 之后与 2 秒那一刻完全等价（裁定 D5-A）</h2>
 * <p>{@link #chargeTicks} 把按住 tick 钳在 {@link #HOLD_CHARGE_CAP_TICKS} 上。它只喂
 * <b>姿态</b>（客户端握手模型"往里收"的进度）；<b>轨迹、消耗、耐久都不吃蓄力进度</b>
 * （模式消耗是平坦的一个数、花瓣曲线只由模式决定、耐久只在回程结算）⇒ 2 秒之后继续按着
 * <b>零额外影响</b>，只是姿态停住。{@link #onUseTick} 因此是一个<b>刻意的空覆写</b>
 * （占住"按住期间每 tick"这个服务端钩子，并在此写明为什么它无事可做）。</p>
 *
 * <h2>三、判定与扣款都在同一处（松手那一 tick）</h2>
 * <p>{@link #use} <b>不做任何</b>冷却/能量/耐久判定——模式要等松手才知道，按下就判会变成
 * "两份口径"（按下判一次、松手再判一次，迟早漂移）。投掷的全部判定与扣款收在
 * {@link #releaseUsing}：</p>
 * <ol>
 *   <li>冷却中 ⇒ <b>不投掷、不扣耐久</b>（裁定 D13；冷却圈本身在原版 {@code ItemCooldowns} 上
 *       显示，就是给玩家的反馈）；</li>
 *   <li>能量不够 ⇒ {@code ToolEnergy.sendLowEnergy} + 不投掷、<b>不扣耐久</b>（现状口径）；</li>
 *   <li>都过了 ⇒ <b>同一处</b>算总消耗（{@link #throwCost(ItemStack, Player, boolean)} =
 *       模式消耗 + 穿刺附加费 20L，批 4 再加环绕 15L）→ 扣 → 造实体 → 记耐久损耗 → 上冷却。</li>
 * </ol>
 * <p>⚠ <b>点按与长按共用同一条 {@code ItemCooldowns} 键</b>（原版按物品计）⇒ 两组冷却
 * <b>互相覆盖</b>：点按之后 5 秒内连长按也投不出去。这是<b>预期行为</b>（一条右键操作路径，
 * 不按模式分键）。</p>
 *
 * <h2>四、投掷流程</h2>
 * <p>能量预检 → 扣能量 → 造实体（记 owner / 镖本身 / 背包槽位 / 模式）→ 点按走
 * {@code shootFromRotation(1.5, 1.0)}（<b>逐字沿用现状</b>）、长按走
 * {@code startPetalFlight(...)} → 入世界 → <b>把手上那一格清空</b> → 上冷却 + 统计。
 * 清空是必要的：镖回来时会走"原槽 → 背包 → 掉地上"三步交还（见
 * {@link AbstractBoomerangEntity}），不先清空就会在手上复制出一把。</p>
 *
 * <h2>五、为什么 {@code getDestroySpeed} 恒 0</h2>
 * <p>⚠ 本节原文的前提是「镖<b>不吃耐久、吃能量</b>」——<b>该前提已被作者 2026-10-02 推翻</b>
 * （批 1 给镖加了真耐久，见下节；依据：开工需求 §3.1 数值表 + §3.8 耐久扣减/回程结算 + §3.9）。
 * 但下面这条<b>结论仍然成立</b>，只是理由换了：挖方块<b>不是</b>靠"手持左键"，而是实体去程/回程命中时
 * 用它自己那一档的 {@code digSpeed} 复刻原版进度——所以手上一律不许有挖掘速度。所以：</p>
 * <ul>
 *   <li>{@link #getDestroySpeed} 恒 {@code 0.0F} ⇒ 原版挖掘进度恒 0 ⇒ <b>拿在手上永远挖不动方块</b>；
 *       真正挖方块只发生在实体回程/去程命中时（{@link AbstractBoomerangEntity} 用它自己那一档的
 *       {@code digSpeed} 去复刻原版进度，那才是唯一入口）。</li>
 *   <li>{@link #isCorrectToolForDrops} 仍然按档位答"是"——它决定
 *       {@code ServerPlayerGameMode#destroyBlock} <b>掉不掉东西</b>（原版口径：需要正确工具的方块，
 *       手上不算正确工具就只掉经验不给掉落物），以及原版进度里的 30/100 因子。没有这一条，
 *       镖挖石头/矿石<b>什么都不会掉</b>。</li>
 * </ul>
 *
 * <p>能量条与能量文案走 {@link EnergyGradientTool}：渐变两端色取<b>本档自己的配色</b>
 * （{@link #energyGradientStops} → {@link BoomerangTier#color()}），四把各不相同。</p>
 *
 * <h2>六、耐久（作者 2026-10-02 批 1 建面、批 2 接扣减；需求 §3.1 / §3.8）</h2>
 * <p>四把镖各有耐久上限（{@link BoomerangTier#durability()} = 1000 / 2000 / 3500 / 3500），
 * 由构造器交给原版 {@link Item.Properties#durability}（形态照 {@code JadeTopazBowItem:76}），
 * <b>出生即满耐久</b>（该 API 自己会写 {@code DAMAGE = 0}；见原版 {@code Item.java:360-365}）。</p>
 * <ul>
 *   <li><b>不走 {@code hurtAndBreak}</b>：那会把栈当场打成空，做不到需求 §3.8 的
 *       「耐久见底时不立即归零、不当场销毁，等回到玩家身上再批量结算」。
 *       读写接口是 {@link #getDurability} / {@link #setDurability} / {@link #addWear}。</li>
 *   <li><b>投掷的损耗不在这里写</b>：批 2 起投掷把"点按 −2 / 长按 −5"交给实体累计
 *       （{@code AbstractBoomerangEntity#addFlightWear}，数值取
 *       {@link BoomerangTier#throwWear(boolean)}），挖方块与命中生物也各 −1
 *       （{@link BoomerangTier#WEAR_PER_HIT}）。<b>飞行期间一次都不写回</b>，回到玩家怀里时
 *       由 {@code settleWear} 一次结算（够了就扣、不够就爆）。</li>
 *   <li><b>不给原版耐久条</b>：覆写 {@link #isBarVisible} 恒 {@code false} —— 本模组口径是
 *       <b>能量才是条</b>（能量条画在 tooltip 里），耐久只以一行文字表达
 *       （{@link #appendHoverText}），否则同一个格子会同时出现"耐久条 + 能量条"两种资源条。</li>
 *   <li><b>可附魔，但只认本模组附魔</b>：加了 {@code MAX_DAMAGE} 之后原版
 *       {@code Item#isEnchantable}（{@code 堆叠 1 && 有 MAX_DAMAGE}）自动为真 ⇒ 镖变成可附魔物；
 *       因此覆写 {@link #supportsEnchantment} 只放行本模组命名空间的附魔（原版"耐久/经验修补"
 *       这类耐久类附魔与全部第三方附魔一律被挡）。<b>具体哪几条本模组附魔仍然只由附魔 JSON 的
 *       {@code supported_items}（{@code #createoreexpansion:skill_boostable}）决定</b>——
 *       这里不再登记第二份名单（依据见 {@code AnvilEnchantmentGuard} 的注释：第二份表示必然漂移）。
 *       批 1 已把四把镖加进 {@code #skill_boostable}（需求 §3.7 的前置）。</li>
 * </ul>
 *
 * <h2>七、技能（2026-10-02 批 3：穿刺；需求 §3.5 / §3.7；裁定 D11）</h2>
 * <p>四把镖都绑了 <b>{@code createoreexpansion:pierce}</b>（穿刺），基准等级取本档
 * {@link BoomerangTier#baseSkillLevel()}（1 / 2 / 3 / 3），<b>不需要开关</b>：只要投掷就生效
 * （需求 §六 推断值 #9）。等级的实际读取点是
 * {@link #effectiveSkillLevel(ItemStack, int)}（唯一一处），消费点是
 * {@link #throwCost(ItemStack, Player, boolean)}（附加费 20L）与实体侧的穿透额度（3L / 5L）。</p>
 * <p>⚠ <b>为什么右键投掷不会"顺带释放"这条技能</b>：技能类型是
 * {@code SkillType.USE_SKILL}，而 {@code UseItemHandler} 会在
 * {@code PlayerInteractEvent.RightClickItem} 上把主手物品的 USE 族技能送进内核释放 ——
 * 对镖来说那就是"右键 = 又扣一次能量"。因此那里按<b>类型</b>（不是物品 id）给
 * {@link BoomerangItem} 开了一条豁免，理由与弓的同形（弓的技能在松手射击那一刻自己释放）。
 * 详见 {@code UseItemHandler#release} 里的注释。</p>
 */
public class BoomerangItem extends Item implements EnergyGradientTool {

	/** 投掷初速（照抄 Quark Pickarang）—— <b>点按专用</b>；长按走花瓣曲线自己的速率剖面。 */
	public static final float THROW_SPEED = 1.5F;
	/** 投掷散布（照抄 Quark Pickarang）—— 同上，点按专用。 */
	public static final float THROW_INACCURACY = 1.0F;

	/**
	 * <b>点按 / 长按的分界</b>（tick）：按住 {@code >= 它} 算长按（<b>含等号</b>）。
	 *
	 * <p>10 tick = 0.5 秒（作者口径"&lt; 0.5 秒 = 点按、&gt; 0.5 秒 = 长按"；边界按"含等号算长按"
	 * 裁定，即恰好 0.5 秒算长按）。</p>
	 */
	public static final int HOLD_THRESHOLD_TICKS = 10;

	/**
	 * <b>蓄力封顶</b>（tick）：40 tick = 2.0 秒（需求 §3.3"到 2 秒的时候停止"）。
	 *
	 * <p>只钳<b>姿态</b>：2 秒之后继续按着对轨迹/消耗/耐久<b>零额外影响</b>（裁定 D5-A）。</p>
	 */
	public static final int HOLD_CHARGE_CAP_TICKS = 40;

	/**
	 * {@link #getUseDuration} 的返回值（原版弓同口径 72000）。
	 *
	 * <p>它只决定"最多能按住多久"：本模组<b>不</b>自动松手（按到封顶之后玩家可以继续按着，
	 * 也可以立刻松手），所以取一个远大于 2 秒的数即可。⚠ 它<b>不是</b>蓄力上限——蓄力上限是
	 * {@link #HOLD_CHARGE_CAP_TICKS}。</p>
	 */
	public static final int MAX_USE_DURATION = 72000;

	private final BoomerangTier tier;
	private final Supplier<EntityType<? extends AbstractBoomerangEntity>> entityType;

	public BoomerangItem(BoomerangTier tier, Supplier<EntityType<? extends AbstractBoomerangEntity>> entityType,
						 Properties properties) {
		// 耐久上限来自档位表（本档唯一真源），形态照 JadeTopazBowItem 的 properties.durability(...)：
		// 它同时写入 MAX_DAMAGE 与 DAMAGE = 0（原版 Item.java:360-365）⇒ 新造出来的镖满耐久。
		super(properties.durability(tier.durability()));
		this.tier = tier;
		this.entityType = entityType;
	}

	public BoomerangTier tier() {
		return tier;
	}

	// ========== 两模式：按下 / 每 tick / 松手（2026-10-02 批 2） ==========

	/**
	 * <b>按下右键</b>：只做一件事——进入"使用中"（模式要等松手才知道）。
	 *
	 * <p>形态照 {@code JadeTopazBowItem:107-108}：{@code player.startUsingItem(hand)} +
	 * {@code InteractionResultHolder.consume(stack)}。两端都调（客户端要靠它进入使用姿态、
	 * 服务端要靠它让 {@code releaseUsing} 有东西可松）。</p>
	 *
	 * <p>⚠ <b>这里刻意不判冷却、不判能量、不扣款、不扣耐久</b>（见类注释第三节）：
	 * 按下就判会让同一件事出现两份口径；投掷的全部判定收在 {@link #releaseUsing}。</p>
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	/**
	 * 最多能按住多久（tick）—— 见 {@link #MAX_USE_DURATION}（<b>不是</b>蓄力上限）。
	 *
	 * <p>不覆写它的话原版默认返回 0（{@code Item.getUseDuration} 只认食物），
	 * {@code startUsingItem} 会立刻把"使用中"结束掉，{@code releaseUsing} 根本不会被调到
	 * ——长按与轻点<b>都会没反应</b>。</p>
	 */
	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return MAX_USE_DURATION;
	}

	/**
	 * <b>按住期间每 tick 的服务端回调</b>—— 本类<b>刻意空着</b>（裁定 D5-A 的可执行形态）。
	 *
	 * <p>为什么不做事：模式消耗是平坦的一个数（点按 10/9/8/8 或长按 50/45/40/40，与按住多久无关）、
	 * 轨迹只由模式决定、耐久只在回程结算 ⇒ <b>超过 2 秒之后继续按着对玩法零影响</b>。
	 * 真正吃蓄力进度的只有客户端姿态，它直接读 {@link #chargeTicks(int)}（同一个封顶常量），
	 * 不需要服务端每 tick 写任何状态。</p>
	 *
	 * <p>留这个覆写的意义是<b>把钩子显式占住</b>：批 3/4 若要"按住期间"做点什么（技能预览之类），
	 * 位置就是这里；而"投掷的判定与扣款"永远只有 {@link #releaseUsing} 一处。</p>
	 */
	@Override
	public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
		// 有意为空：见本方法的 Javadoc（封顶之后与 2 秒那一刻完全等价）。
	}

	/**
	 * <b>松手</b>：点按 / 长按的<b>唯一判定与扣款处</b>（含冷却、能量、耐久记账）。
	 *
	 * <p>{@code timeLeft} 是原版给的"剩余使用时长"，按住 tick 数 =
	 * {@code getUseDuration − timeLeft}（与翠玉弓 {@code JadeTopazBowModelRegistration} 算
	 * {@code pull} 的同一个口径）。<b>同一 tick 按下抬起</b>时 {@code timeLeft} 等于
	 * {@code MAX_USE_DURATION} ⇒ 按住 0 tick ⇒ 点按，照投。</p>
	 */
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
		if (level.isClientSide || !(entity instanceof Player player)) {
			return; // 实体由服务端生成（否则两端各造一个）
		}
		int heldTicks = heldTicks(stack, entity, timeLeft);
		boolean longHold = isLongHold(heldTicks);

		if (player.getCooldowns().isOnCooldown(this)) {
			// 裁定 D13：冷却中 ⇒ 不投掷、不扣耐久（连能量也不扣）。冷却圈画在原版 Cooldowns 上，
			// 就是给玩家的反馈；这里不再另发一条文案。
			return;
		}
		// 唯一一处"算总消耗"：模式消耗 + 穿刺附加费（批 4 的环绕也加在这一行）。
		int cost = throwCost(stack, player, longHold);
		if (!ToolEnergy.canAfford(player, stack, cost)) {
			ToolEnergy.sendLowEnergy(player, stack);
			return; // 能量不够 ⇒ 不投掷，且**不扣耐久**
		}
		EntityType<? extends AbstractBoomerangEntity> type = entityType.get();
		AbstractBoomerangEntity boomerang = type.create(level);
		if (boomerang == null) {
			return;
		}
		InteractionHand hand = player.getUsedItemHand();
		// 先扣能，再取镖的那一份 ⇒ 镖上带的能量就是"扣过投掷费"的账
		ToolEnergy.consume(player, stack, cost);
		boomerang.setOwner(player);
		boomerang.setItemStack(stack);
		boomerang.setSlot(hand == InteractionHand.MAIN_HAND
			? player.getInventory().selected
			: Inventory.SLOT_OFFHAND);
		Vec3 spawn = new Vec3(player.getX(), player.getEyeY() - 0.1D, player.getZ());
		boomerang.setPos(spawn.x, spawn.y, spawn.z);
		if (longHold) {
			// 长按：花瓣曲线（§3.4）。投掷那一刻的水平朝向就是曲线基准角 ψ
			// （数学角 atan2(朝向.z, 朝向.x) ⇒ 与需求 §3.4.3 的 x = P.x + r·cos(ψ+φ) 逐字同形）。
			Vec3 look = player.getLookAngle();
			boomerang.startPetalFlight(spawn, Math.atan2(look.z, look.x));
		} else {
			// 点按：**逐字沿用现状**（直线 + 1.5 初速 + 1.0 散布；不准改，改动就在这一行）。
			boomerang.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
				THROW_SPEED, THROW_INACCURACY);
		}
		// 耐久：只把本次投掷的损耗**记到实体上**（−2 / −5），飞行期间一次都不写回物品
		// （需求 §3.8：见底不当场归零）；挖方块/命中生物再各追加 −1。
		boomerang.addFlightWear(tier.throwWear(longHold));
		level.addFreshEntity(boomerang);

		// 手上那一格清空：镖在回程交还时才不会凭空多出一把
		player.setItemInHand(hand, ItemStack.EMPTY);
		player.getCooldowns().addCooldown(this, tier.cooldownTicks(longHold));
		player.awardStat(Stats.ITEM_USED.get(this));
	}

	/** 按住 tick 数 = {@code getUseDuration − 松手时的剩余时长}（钳到 ≥ 0；与弓的 pull 同一算法）。 */
	public static int heldTicks(ItemStack stack, LivingEntity entity, int timeLeft) {
		return Math.max(0, stack.getUseDuration(entity) - timeLeft);
	}

	/** 模式判据（<b>唯一一处</b>）：按住 {@code >=} {@link #HOLD_THRESHOLD_TICKS} ⇒ 长按（含等号）。 */
	public static boolean isLongHold(int heldTicks) {
		return heldTicks >= HOLD_THRESHOLD_TICKS;
	}

	/**
	 * 蓄力进度用的 tick（钳在 {@link #HOLD_CHARGE_CAP_TICKS} 上）—— <b>只喂姿态</b>。
	 *
	 * <p>客户端握手模型"往里收"的进度 = {@code chargeTicks(heldTicks) / HOLD_CHARGE_CAP_TICKS}
	 * （见 {@code BoomerangChargeClient}）；轨迹/消耗/耐久都不读它。</p>
	 */
	public static int chargeTicks(int heldTicks) {
		return Math.min(Math.max(0, heldTicks), HOLD_CHARGE_CAP_TICKS);
	}

	/**
	 * <b>本次投掷的总能量消耗（唯一一处）</b>：模式消耗 + 技能附加费。
	 *
	 * <p>批 2 只有模式消耗那一项（点按 10/9/8/8、长按 50/45/40/40）。批 3 把<b>穿刺</b>的附加费
	 * 加进来（{@code 20×等级}，需求 §3.7）——批 4 的环绕（{@code 15×等级}）也加在这一行。
	 * 这样"判合计"与"扣合计"永远在同一处（需求 §3.7 的 ⚠，也是陷阱清单 #7）。</p>
	 *
	 * <p>⚠ <b>签名在批 3 多了一个 {@code stack}</b>：技能等级要读这条栈上的附魔
	 * （技艺提升 / 技艺回溯），而等级又决定附加费 —— 少一个入参就只能去别处读，
	 * 那正是"同一件事两处口径"的开端。调用点只有 {@link #releaseUsing} 一处。</p>
	 *
	 * @param stack    投掷的那一把镖（读附魔取有效等级）
	 * @param longHold {@code true} = 长按（花瓣曲线）
	 */
	public int throwCost(ItemStack stack, Player player, boolean longHold) {
		// 模式消耗（档位表）+ 穿刺附加费（20×等级）。player 目前不参与计算，
		// 留着是因为"谁投的"将来可能进折扣/状态判定（批 4 的环绕也走这一行）。
		return tier.throwCost(longHold) + BoomerangSkillConfigs.pierceEnergyCost(effectiveSkillLevel(stack));
	}

	// ========== 技能等级（2026-10-02 批 3：穿刺；读取点唯一） ==========

	/**
	 * <b>本把镖的有效技能等级</b>（<b>唯一读取点</b>；物品侧与实体侧共用它）。
	 *
	 * <p>口径 = {@code SkillEnergyCost.effectiveLevel(stack, 基准, MAX_SKILL_LEVEL)}：
	 * {@code min(基准 + 技艺提升 − 技艺回溯, 5)} 且不低于 1（两个附魔 3 级及以上一律按 2 计）。</p>
	 *
	 * <p>谁用它：{@link #throwCost}({@code 20×等级}) 与实体的穿透额度
	 * （{@code AbstractBoomerangEntity#pierceMobQuota/pierceBlockQuota}，{@code 3L/5L}）。
	 * 两边都<b>现读</b>同一份栈 ⇒ 不会出现"收 20L 的钱、给 3(L-1) 的额度"这种漂移。</p>
	 *
	 * <p>⚠ 基准等级来自档位（{@link BoomerangTier#baseSkillLevel()}：1/2/3/3）——
	 * 与护甲的雷鸣=4 <b>不同</b>，这里绝不引用护甲那张表。</p>
	 */
	public static int effectiveSkillLevel(ItemStack stack, int baseLevel) {
		return SkillEnergyCost.effectiveLevel(stack, baseLevel, BoomerangSkillConfigs.MAX_SKILL_LEVEL);
	}

	/** 本实例（按自己的档位取基准等级）的有效技能等级；见 {@link #effectiveSkillLevel(ItemStack, int)}。 */
	public int effectiveSkillLevel(ItemStack stack) {
		return effectiveSkillLevel(stack, tier.baseSkillLevel());
	}

	// ========== 耐久：本档上限 + 读写接口（2026-10-02 批 1） ==========

	/** 这一档的耐久上限（读 {@link BoomerangTier#durability()}；它不是从栈上读的）。 */
	public int getMaxDurability() {
		return tier.durability();
	}

	/**
	 * 剩余耐久 = 耐久上限 − 原版 {@code DAMAGE} 组件（{@code damage} 即"已扣掉的量"）。
	 *
	 * <p>读的是栈上的 {@code MAX_DAMAGE}/{@code DAMAGE}（原版 {@code ItemStack#getMaxDamage} →
	 * {@code Item#getMaxDamage(ItemStack)}），所以它同时覆盖"老存档里没有该组件的旧镖"——
	 * 那些栈按物品自身的组件默认值补齐（见报告 §①，不改写任何存档）。</p>
	 */
	public int getDurability(ItemStack stack) {
		return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
	}

	/**
	 * 把剩余耐久写成 {@code remaining}（钳到 {@code [0, 上限]}），<b>直接写 {@code DAMAGE} 组件</b>。
	 *
	 * <p>⚠ 这是本模组唯一的耐久写入口，<b>刻意不用原版 {@code hurtAndBreak}</b>：后者在
	 * {@code damage >= maxDamage} 时会当场把栈打成空并抛 {@code ITEM_BREAK} 事件，
	 * 做不到需求 §3.8「见底时不立即归零 / 不当场销毁」。</p>
	 */
	public void setDurability(ItemStack stack, int remaining) {
		int max = stack.getMaxDamage();
		stack.setDamageValue(max - Mth.clamp(remaining, 0, max));
	}

	/**
	 * 累加一次磨损（{@code amount} 为本次要扣掉的点数，负数按 0 处理），返回写回后的剩余耐久。
	 *
	 * <p>它<b>只负责"把损耗记到栈上"</b>，不判断"该不该爆"。需求 §3.8 的批量结算口径是：
	 * 飞行期间把损耗<b>累计在实体上</b>（不逐次调用它，否则耐久会被提前写到 0），
	 * 回到玩家身上时再调用一次；若累计损耗 ≥ 剩余耐久，调用方负责让镖<b>爆掉</b>
	 * （销毁物品，而不是留下一个 0 耐久的镖）。调用点在
	 * {@code AbstractBoomerangEntity#settleWear}（唯一一处）。</p>
	 */
	public int addWear(ItemStack stack, int amount) {
		setDurability(stack, getDurability(stack) - Math.max(0, amount));
		return getDurability(stack);
	}

	/**
	 * <b>只让能量条显示，不给原版耐久条</b>（作者 2026-10-02 批 1：需求 §3.8 的表现面）。
	 *
	 * <p>原版默认是 {@code return stack.isDamaged()}（{@code Item.java:160-162}）——加了耐久之后，
	 * 那把镖一旦被扣过耐久就会在物品格上多画一条"耐久条"，与 tooltip 里的能量条形成<b>两条资源条</b>。
	 * 本模组口径：<b>条只画能量</b>（tooltip 的渐变条，见 {@link #energyGradientStops}），
	 * 耐久改由 tooltip 的一行文字表达（{@link #appendHoverText}）。</p>
	 *
	 * <p>⚠ 这里恒 {@code false}，不是 {@code super.isBarVisible(stack)}：后者一旦被改回默认，
	 * 耐久条就会重新出现（关卡 §29j 钉着本方法体的形状）。</p>
	 */
	@Override
	public boolean isBarVisible(ItemStack stack) {
		return false;
	}

	/**
	 * 耐久读数：<b>只加一行</b>，用原版自己的 {@code item.durability} 翻译键（中英都有，零新增语言键）。
	 *
	 * <p>⚠ 刻意不动 tooltip 布局：技能区 / 能量区 / 绑定行都由 {@code ClientEvents#onItemTooltip}
	 * 在 index 1 起往下插（本行由原版在 {@code getTooltipLines} 里追加，落在末尾）。</p>
	 */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.durability", getDurability(stack), stack.getMaxDamage())
			.withStyle(ChatFormatting.GRAY));
	}

	/**
	 * <b>只有本模组命名空间的附魔能上这把镖</b>（作者 2026-10-02 批 1：加耐久 ⇒ 物品变可附魔）。
	 *
	 * <p>为什么需要它：原版 {@code Item#isEnchantable} 的判据是「堆叠 1 && 有 {@code MAX_DAMAGE}」
	 * （{@code Item.java:292-294}），所以加了耐久之后四把镖自动变成可附魔物；而本模组口径只要
	 * 「技艺提升 / 技艺回溯」这一条线（需求 §3.7），不要原版耐久类附魔（耐久 / 经验修补）——
	 * 它们正是"用不坏"的那两条，会把本轮新加的耐久设计当场抹平。</p>
	 *
	 * <p>判据分两层，<b>不重复登记名单</b>：</p>
	 * <ol>
	 *   <li>本层：附魔必须是<b>本模组命名空间</b>（{@link CoeCore#REGISTRY_NAMESPACE}）的
	 *       ⇒ 原版与第三方附魔一律被挡；</li>
	 *   <li>下一层：交给默认实现（{@code IItemExtension#supportsEnchantment}，判据 = 该附魔 JSON 的
	 *       {@code supported_items}）⇒ <b>"到底哪几条本模组附魔可用"仍然只有一处真源</b>，
	 *       即 {@code #createoreexpansion:skill_boostable}（批 1 已加进四把镖）。
	 *       这正是 {@code AnvilEnchantmentGuard} 注释里记着的教训：别在本类里再抄一份
	 *       "附魔 → 允许物品"的表，否则它会与标签漂移。</li>
	 * </ol>
	 *
	 * <p>覆盖到的入口（各自都直接调它）：铁砧逐条校验（NeoForge 对 {@code AnvilMenu} 的补丁）、
	 * 附魔台候选（{@code EnchantmentHelper#getAvailableEnchantmentResults} 走
	 * {@code isPrimaryItemFor} → 默认实现回头调本方法）、{@code /enchant} 命令、随机战利品附魔。
	 * ⚠ 创造模式铁砧仍然放行任意附魔（NeoForge 在 {@code AnvilMenu} 里对
	 * {@code instabuild} 直接置真，是刻意语义，本类不干预）。</p>
	 */
	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		ResourceKey<Enchantment> key = enchantment.unwrapKey().orElse(null);
		if (key == null || !CoeCore.REGISTRY_NAMESPACE.equals(key.location().getNamespace())) {
			return false;
		}
		return super.supportsEnchantment(stack, enchantment);
	}

	/**
	 * <b>能量条/能量文案的渐变色标 = 本档自己的配色</b>（左→右：亮色 → 暗色）。
	 *
	 * <p>两端色一律读 {@link BoomerangTier#color()}，也就是
	 * {@code ToolEnergyColorConfig.JADE / SAPPHIRE / STELLARSTONE / THUNDERITE}
	 * 这四个常量本身（与同材质工具/盔甲同一个数据源，这里不复制任何一个色值）。</p>
	 *
	 * <p>⚠ 这个方法就是作者 2026-10-02 报的那个 bug（"剩下那三种类型的回旋镖的能量条并不与
	 * 它们的类型匹配"）的修复点：{@link EnergyGradientTool} 原先没有取色方法，两个消费方
	 * 各自写死翠玉之弓那条黄绿渐变，于是<b>四把镖共用同一条颜色</b>——翠玉那把"看着对"，
	 * 只是因为它的档位色恰好等于那条渐变的起点。删掉这个覆写 = 四把又变回同一个色。</p>
	 */
	@Override
	public List<Color> energyGradientStops(ItemStack stack) {
		return List.of(tier.color().light, tier.color().dark);
	}

	/**
	 * 需求 10：恒 0 —— 镖不是普通工具（拿在手上挖不动任何东西）。
	 *
	 * <p>⚠ 这一条与"镖能挖方块"<b>不冲突</b>：挖掘发生在实体侧，用的是
	 * {@link BoomerangTier#digSpeed()}（那一档自己的数字），不是从物品读的。</p>
	 */
	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return 0.0F;
	}

	/**
	 * 「这把镖算不算该方块的正确工具」——决定 {@code destroyBlock} 掉不掉落物，
	 * 以及原版挖掘进度里的 {@code i = 30/100}。
	 *
	 * <p>判据与原版 {@code Tier#getIncorrectBlocksForDrops} <b>逐字同源</b>：方块得是镐类，
	 * 且不在本档的 {@code INCORRECT_FOR_*_TOOL} 里（{@link BoomerangTier#incorrectBlocks()}，
	 * 与 {@code AllTiers} 用同一批标签）。</p>
	 */
	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(tier.incorrectBlocks());
	}
}
