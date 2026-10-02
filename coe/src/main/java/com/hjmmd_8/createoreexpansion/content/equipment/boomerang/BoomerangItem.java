package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>回旋镖物品</b>（四把镖共用一个类，数值与实体类型由构造参数给）。
 *
 * <p>投掷流程（服务端权威）：能量预检 → 扣投掷能量 → 造实体（记 owner / 镖本身 / 背包槽位）
 * → {@code shootFromRotation(1.5, 1.0)} → 入世界 → <b>把手上那一格清空</b> → 上冷却 + 统计。
 * 清空是必要的：镖回来时会走"原槽 → 背包 → 掉地上"三步交还（见
 * {@link AbstractBoomerangEntity}），不先清空就会在手上复制出一把。</p>
 *
 * <h2>为什么 {@code getDestroySpeed} 恒 0</h2>
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
 * <h2>耐久（作者 2026-10-02 批 1：需求 §3.1 / §3.8）</h2>
 * <p>四把镖各有耐久上限（{@link BoomerangTier#durability()} = 1000 / 2000 / 3500 / 3500），
 * 由构造器交给原版 {@link Item.Properties#durability}（形态照 {@code JadeTopazBowItem:76}），
 * <b>出生即满耐久</b>（该 API 自己会写 {@code DAMAGE = 0}；见原版 {@code Item.java:360-365}）。</p>
 * <ul>
 *   <li><b>不走 {@code hurtAndBreak}</b>：那会把栈当场打成空，做不到需求 §3.8 的
 *       「耐久见底时不立即归零、不当场销毁，等回到玩家身上再批量结算」。
 *       批 1 只搭好读写接口 {@link #getDurability} / {@link #setDurability} / {@link #addWear}；
 *       <b>扣减时机（投掷 −2/−5、每命中/挖块 −1、回程结算）排在批 2</b>。</li>
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
 */
public class BoomerangItem extends Item implements EnergyGradientTool {

	/** 投掷初速（照抄 Quark Pickarang）。 */
	public static final float THROW_SPEED = 1.5F;
	/** 投掷散布（照抄 Quark Pickarang）。 */
	public static final float THROW_INACCURACY = 1.0F;

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
	 * 回到玩家身上时再调用一次；若累计损耗 &gt; 剩余耐久，调用方负责让镖<b>爆掉</b>
	 * （销毁物品，而不是留下一个 0 耐久的镖）。这些调用点排在批 2。</p>
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

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide) {
			// 客户端只做手臂动画；实体由服务端生成（否则两端各造一个）
			return InteractionResultHolder.success(stack);
		}
		if (player.getCooldowns().isOnCooldown(this)) {
			return InteractionResultHolder.fail(stack);
		}
		if (!ToolEnergy.canAfford(player, stack, tier.throwCost())) {
			ToolEnergy.sendLowEnergy(player, stack);
			return InteractionResultHolder.fail(stack);
		}
		EntityType<? extends AbstractBoomerangEntity> type = entityType.get();
		AbstractBoomerangEntity boomerang = type.create(level);
		if (boomerang == null) {
			return InteractionResultHolder.fail(stack);
		}
		// 先扣能，再取镖的那一份 ⇒ 镖上带的能量就是"扣过投掷费"的账
		ToolEnergy.consume(player, stack, tier.throwCost());
		boomerang.setOwner(player);
		boomerang.setItemStack(stack);
		boomerang.setSlot(hand == InteractionHand.MAIN_HAND
			? player.getInventory().selected
			: Inventory.SLOT_OFFHAND);
		boomerang.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
		boomerang.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
			THROW_SPEED, THROW_INACCURACY);
		level.addFreshEntity(boomerang);

		// 手上那一格清空：镖在回程交还时才不会凭空多出一把
		player.setItemInHand(hand, ItemStack.EMPTY);
		player.getCooldowns().addCooldown(this, tier.cooldownTicks());
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.success(stack);
	}
}
