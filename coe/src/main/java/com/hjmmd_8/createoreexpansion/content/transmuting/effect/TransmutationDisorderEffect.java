package com.hjmmd_8.createoreexpansion.content.transmuting.effect;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllModItemTags;
import com.hjmmd_8.createoreexpansion.common.SeriesTraits;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class TransmutationDisorderEffect extends MobEffect {

	public TransmutationDisorderEffect() {
		super(MobEffectCategory.HARMFUL, 0xD13FFF);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.level().isClientSide)
			return true;

		if (entity instanceof Player player && player.isCreative())
			return true;

		int level = amplifier + 1;

		if (entity.tickCount % 20 == 0)
			applyDamage(entity, level);

		if (entity instanceof Player player) {
			damageRandomTool(player, level);
			reduceInventoryItems(player, level);
		}
		return true;
	}

	private void applyDamage(LivingEntity entity, int level) {
		float damage = 1.0F + (float) Math.log(level);
		if (level >= 2 || entity.getHealth() > 2.0F)
			entity.hurt(entity.damageSources().magic(), damage);
	}

	private void damageRandomTool(Player player, int level) {
		Inventory inventory = player.getInventory();
		List<ItemStack> candidates = new ArrayList<>();
		for (ItemStack stack : inventory.items) {
			if (isTool(stack))
				candidates.add(stack);
		}

		if (candidates.isEmpty())
			return;

		ItemStack target = candidates.get(player.getRandom().nextInt(candidates.size()));
		target.hurtAndBreak(level, player, EquipmentSlot.MAINHAND);
	}

	private boolean isTool(ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageableItem())
			return false;

		Item item = stack.getItem();
		if (item instanceof DiggerItem || item instanceof SwordItem)
			return true;

		return !(item instanceof ArmorItem) && !(item instanceof BlockItem) && !(item instanceof ElytraItem);
	}

	private void reduceInventoryItems(Player player, int level) {
		int interval = Math.max(1, 20 / level);
		if (player.tickCount % interval != 0)
			return;

		Inventory inventory = player.getInventory();
		List<ItemStack> candidates = new ArrayList<>();
		for (ItemStack stack : inventory.items) {
			if (canTransmutationDestroy(stack))
				candidates.add(stack);
		}

		if (candidates.isEmpty())
			return;

		int affected = Math.min(candidates.size(), 2 * level);
		RandomSource random = player.getRandom();
		for (int i = 0; i < affected; i++) {
			ItemStack target = candidates.remove(random.nextInt(candidates.size()));
			target.shrink(1);
		}
	}

	boolean canTransmutationDestroy(ItemStack stack) {
		if (stack.isEmpty())
			return false;
		if (stack.getMaxStackSize() <= 1)
			return false;
		if (stack.getCount() <= 1)
			return false;
		if (isContainerLike(stack))
			return false;
		if (stack.is(AllModItemTags.TRANSMUTATION_PROTECTED))
			return false;
		// 星辉石 / 雷鸣合金两个系列（含方块物品：机壳、矿物块…）同样免疫嬗乱销毁（用户 2026-09-15 定稿：
		// "需要加进去"）。这里用系列判定而不是往 TRANSMUTATION_PROTECTED 标签里塞条目，原因是
		// **方块物品进不了物品标签**——机壳的物品由 Create 的 casing 变换器注册，注册链上拿不到
		// ItemBuilder 去挂 .tag(...)，而 datagen 标签又不接受手工条目（手写文件会与 datagen 撞名而构建失败）。
		// 上面那条标签判定仍然保留：整合包要额外保护别的物品，照旧往那个标签里加即可。
		if (SeriesTraits.isStellarstone(stack) || SeriesTraits.isThunderite(stack))
			return false;
		return !isSpecialItem(stack);
	}

	private boolean isContainerLike(ItemStack stack) {
		if (stack.has(DataComponents.CONTAINER) || stack.has(DataComponents.CONTAINER_LOOT)
			|| stack.has(DataComponents.BLOCK_ENTITY_DATA))
			return true;

		if (stack.getItem() instanceof BlockItem blockItem) {
			net.minecraft.world.level.block.Block block = blockItem.getBlock();
			return block instanceof AbstractChestBlock || block instanceof ShulkerBoxBlock
				|| block instanceof EnderChestBlock;
		}
		return false;
	}

	private boolean isSpecialItem(ItemStack stack) {
		if (stack.isEnchanted())
			return true;

		ItemEnchantments storedEnchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
			ItemEnchantments.EMPTY);
		if (!storedEnchantments.isEmpty())
			return true;

		if (stack.getRarity() != Rarity.COMMON)
			return true;

		return stack.has(DataComponents.CUSTOM_NAME);
	}

}