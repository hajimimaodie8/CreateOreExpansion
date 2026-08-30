package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 应力充能器能量波的核心加工逻辑（独立抽取）。
 *
 * <p>翡翠应力充能器发射能量波 → 波命中<b>掉落物 / 置物台（方块物品槽）</b>时，
 * 按 {@code createoreexpansion:charging} 配方执行加工。本类把「配方匹配 →
 * 消耗输入 → 产出结果」这套核心逻辑从 {@link AbstractChargerWaveEntity} 中独立出来，
 * 能量波实体只负责飞行与命中调度，加工判定与执行全部委托本类。</p>
 *
 * <p>加工规则：</p>
 * <ul>
 *     <li><b>配方匹配</b>（{@link #findRecipe}）：遍历全部 charging 配方，取「等级 ≤ 当前能量波等级」
 *         中等级最高者——伽马波（3）可加工 level 1/2/3 配方，低波（1）只加工 level 1 配方；</li>
 *     <li><b>能量工具/凝能佩</b>（{@link ToolEnergy#hasEnergy}）：不消耗物品，直接充能
 *         （充能点数 = {@link ChargingRecipe#energyForLevel}）；</li>
 *     <li><b>普通物品</b>：消耗 1 个输入，按配方 {@code rollResults} 产出（掉落物原地掉落 /
 *         容器放回槽位，放不下掉落在地）。</li>
 * </ul>
 */
public final class ChargerWaveProcessor {

	private final Level level;
	/** 当前能量波等级（1=低、2=高、3=伽马），决定可匹配的配方等级上限 */
	private final int waveLevel;

	public ChargerWaveProcessor(Level level, int waveLevel) {
		this.level = level;
		this.waveLevel = waveLevel;
	}

	/**
	 * 处理单个掉落物：能量工具充能（不消耗）或普通物品按配方转化。
	 * 命中即返回 true（无论是否真的有配方匹配——命中即消耗波）。
	 *
	 * @return 是否命中加工目标（调用方据此让波绽放/消散）
	 */
	public boolean processItemEntity(ItemEntity item) {
		ItemStack stack = item.getItem();
		ChargingRecipe recipe = findRecipe(stack);
		if (recipe == null)
			return false;

		// 能量工具/凝能佩：充能（封顶到最大能量），不消耗物品
		if (ToolEnergy.hasEnergy(stack)) {
			ToolEnergy.setEnergy(stack, ToolEnergy.getEnergy(stack) + ChargingRecipe.energyForLevel(waveLevel));
			// setItem 传副本（新引用）：ItemEntity.setItem 内部按引用判断是否更新，
			// 原地修改 + setItem(同引用) 不会触发数据刷新，导致 Jade 等外部读取显示旧能量
			item.setItem(stack.copy());
			return true;
		}

		// 普通物品：消耗 1 个输入，按配方产出结果（落在原位置）
		List<ItemStack> results = recipe.rollResults(level.random);
		if (results.isEmpty())
			return false;

		stack.shrink(1);
		if (stack.isEmpty())
			item.discard();
		Vec3 pos = item.position();
		for (ItemStack result : results) {
			ItemEntity out = new ItemEntity(level, pos.x, pos.y, pos.z, result);
			out.setDeltaMovement(item.getDeltaMovement());
			level.addFreshEntity(out);
		}
		return true;
	}

	/**
	 * 尝试给方块物品槽（置物台/工作台等，{@link IItemHandler}）中的物品执行充能加工。
	 *
	 * @param pos 命中方块位置（用于生成放不下的掉落物位置）
	 * @return 是否有物品匹配加工（成功或该槽有匹配物品但无结果——命中即消耗波）
	 */
	public boolean processBlockHandler(IItemHandler handler, BlockPos pos) {
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			ChargingRecipe recipe = findRecipe(stack);
			if (recipe == null)
				continue;

			// 能量工具/凝能佩：取出 → 充能 → 放回（IItemHandler 无通用 setStackInSlot 语义，
			// 置物台/工作台均以 extract/insert 为准）
			if (ToolEnergy.hasEnergy(stack)) {
				int count = stack.getCount();
				ItemStack extracted = handler.extractItem(slot, count, false);
				if (extracted.isEmpty())
					return false;
				ToolEnergy.setEnergy(extracted, ToolEnergy.getEnergy(extracted) + ChargingRecipe.energyForLevel(waveLevel));
				// insert 传副本（新引用）：与 setItem 同理，确保容器/外部显示（Jade 等）刷新
				ItemStack remainder = handler.insertItem(slot, extracted.copy(), false);
				if (!remainder.isEmpty()) {
					// 放回失败（槽满等）：掉落在地，不吞物品
					dropAt(remainder, pos);
				}
				return true;
			}

			// 普通物品：消耗 1 个输入，按配方产出结果（放回槽位，放不下则掉落）
			ItemStack input = handler.extractItem(slot, 1, false);
			if (input.isEmpty())
				return false;
			List<ItemStack> results = recipe.rollResults(level.random);
			for (ItemStack result : results) {
				ItemStack remainder = handler.insertItem(slot, result, false);
				if (!remainder.isEmpty()) {
					dropAt(remainder, pos);
				}
			}
			return true;
		}
		return false;
	}

	/** 查询物品对应的充能配方：遍历全部 charging 配方，取「等级 ≤ 本能量波等级」中等级最高者。
	 * <p>例如伽马波（3）命中工具：匹配 level 3 配方（充 1000 点）；低波（1）命中：只匹配
	 * level 1 配方（充 100 点）。等级不够的配方（level &gt; 本波等级）跳过，不加工。</p>
	 * @return 匹配到的配方，无匹配返回 null */
	public ChargingRecipe findRecipe(ItemStack stack) {
		if (stack.isEmpty())
			return null;
		@SuppressWarnings("unchecked")
		RecipeType<ChargingRecipe> type = (RecipeType<ChargingRecipe>) (RecipeType<?>) AllRecipeTypes.CHARGING.getType();
		ChargingRecipe best = null;
		for (RecipeHolder<ChargingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(type)) {
			ChargingRecipe recipe = holder.value();
			if (recipe.getLevel() > waveLevel)
				continue; // 本波等级不够：不加工
			if (!recipe.matches(new SingleRecipeInput(stack), level))
				continue;
			if (best == null || recipe.getLevel() > best.getLevel())
				best = recipe;
		}
		return best;
	}

	/** 在指定位置生成掉落物（放不回的产物/剩余物） */
	private void dropAt(ItemStack stack, BlockPos pos) {
		ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		drop.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(drop);
	}

	/** 判断方块是否是可加工的有物品槽方块（置物台/工作台等） */
	public static boolean hasItemHandler(Level level, BlockPos pos, BlockState state) {
		return !state.isAir() && level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
	}
}
