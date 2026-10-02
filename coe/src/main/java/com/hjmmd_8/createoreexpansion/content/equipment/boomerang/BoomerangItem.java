package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.energy.EnergyGradientTool;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * <p>作者裁定：镖<b>不吃耐久、吃能量</b>。既然能量才是资源，就不能让玩家把它当成一把普通镐
 * 左键去挖矿（那样既不扣能、又能无限挖）。所以：</p>
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
		super(properties);
		this.tier = tier;
		this.entityType = entityType;
	}

	public BoomerangTier tier() {
		return tier;
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
