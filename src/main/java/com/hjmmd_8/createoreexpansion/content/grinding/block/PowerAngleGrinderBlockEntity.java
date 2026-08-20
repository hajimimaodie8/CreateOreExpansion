package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.common.AllTags;
import com.hjmmd_8.createoreexpansion.content.grinding.GrindingWheelTier;
import com.hjmmd_8.createoreexpansion.content.grinding.behaviour.GrinderInventory;
import com.hjmmd_8.createoreexpansion.content.grinding.behaviour.SidedItemHandlers;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 动力角磨床方块实体：应力驱动加工（仿动力锯）。
 *
 * <p>触发方式：物品从上方丢入、或从侧面漏斗/传送带输入；
 * 有应力（转速）时角磨轮工作，物品在轮上处理（粒子沿移动方向偏移表现缓慢移动），
 * 完成后经漏斗引出或向输出方向抛出（方向随转速正负，从盖往轮看）。</p>
 */
public class PowerAngleGrinderBlockEntity extends KineticBlockEntity implements Clearable {

	private static final Object grindingRecipesKey = new Object();
	private static final Object advancedGrindingRecipesKey = new Object();
	private static final Object dismantlingRecipesKey = new Object();

	public FilteringBehaviour filtering;
	public GrinderInventory inventory;

	private int recipeIndex;
	/** 已安装的角磨轮物品 id（null = 未安装） */
	private ResourceLocation wheel;

	public PowerAngleGrinderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new GrinderInventory(this::start);
		inventory.remainingTime = -1;
		recipeIndex = 0;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
			Capabilities.ItemHandler.BLOCK,
			AllBlockEntityTypes.POWER_ANGLE_GRINDER.get(),
			(be, context) -> {
				// 无方向访问（管道等）：全允许
				if (context == null)
					return be.inventory;
				Direction facing = be.getBlockState()
					.getValue(PowerAngleGrinderBlock.HORIZONTAL_FACING);
				// 左右两侧（水平且垂直于 FACING 轴）：输入/输出都行
				if (context.getAxis()
					.isHorizontal() && context.getAxis() != facing.getAxis())
					return be.inventory;
				// 顶部漏斗（context=UP，漏斗嘴朝上，目标在下方机器）：只能输入
				if (context == Direction.UP)
					return SidedItemHandlers.inputOnly(be.inventory);
				// 底面漏斗（context=DOWN，漏斗嘴朝下，目标在上方机器）：只能输出
				if (context == Direction.DOWN)
					return SidedItemHandlers.outputOnly(be.inventory);
				// 盖板侧（FACING 对面）与传动轴侧（FACING）：禁止
				return null;
			}
		);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		filtering = new FilteringBehaviour(this, new GrinderFilterSlot()).forRecipes();
		behaviours.add(filtering);
		behaviours.add(new DirectBeltInputBehaviour(this));
	}

	// ========== 加工流程（仿动力锯） ==========

	@Override
	public void tick() {
		super.tick();

		// 合盖时才能加工
		if (getBlockState().getValue(PowerAngleGrinderBlock.OPEN))
			return;
		// 必须安装角磨轮且转速达到该等级最低要求才能加工
		GrindingWheelTier tier = getWheelTier();
		if (tier == null || Math.abs(getSpeed()) < tier.minRpm)
			return;
		if (getSpeed() == 0)
			return;

		if (inventory.remainingTime == -1) {
			if (!inventory.isEmpty() && !inventory.appliedRecipe)
				start(inventory.getStackInSlot(0));
			return;
		}

		float processingSpeed = Mth.clamp(Math.abs(getSpeed()) / 24, 1, 128);
		inventory.remainingTime -= processingSpeed;

		if (inventory.remainingTime > 0)
			spawnParticles(inventory.getStackInSlot(0));

		if (inventory.remainingTime < 5 && !inventory.appliedRecipe) {
			if (level.isClientSide)
				return;
			if (applyRecipe()) {
				inventory.appliedRecipe = true;
				inventory.recipeDuration = 20;
				inventory.remainingTime = 20;
				sendData();
			} else {
				// 无匹配配方（或结果为空）：吞掉，什么都不输出
				inventory.clear();
				inventory.remainingTime = -1;
				sendData();
			}
			return;
		}

		if (inventory.remainingTime > 0)
			return;
		inventory.remainingTime = 0;

		// 加工完成：成品（槽 1+）留在库存由输出口漏斗抽取；成品抽空后继续加工槽 0 剩余输入
		if (inventory.appliedRecipe) {
			if (isOutputEmpty()) {
				inventory.remainingTime = -1;
				inventory.appliedRecipe = false;
				sendData();
			}
			return;
		}

		// 无匹配配方（不可加工物品）：吞掉，什么都不输出
		inventory.clear();
		inventory.remainingTime = -1;
		sendData();
	}

	/** 输入物品（上方丢入/漏斗/传送带） */
	public void insertItem(ItemStack stack) {
		if (inventory.isEmpty() && !stack.isEmpty()) {
			inventory.clear();
			inventory.insertItem(0, stack.copy(), false);
			sendData();
		}
	}

	public void insertItem(ItemEntity entity) {
		if (!entity.isAlive())
			return;
		if (level.isClientSide)
			return;
		insertItem(entity.getItem());
		entity.discard();
	}

	/** 合盖时空手右键：取出库存中全部物品交给玩家 */
	public void retrieveAll(Player player) {
		if (level.isClientSide)
			return;
		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			ItemHandlerHelper.giveItemToPlayer(player, stack);
			inventory.setStackInSlot(i, ItemStack.EMPTY);
		}
		inventory.remainingTime = -1;
		inventory.appliedRecipe = false;
		sendData();
	}

	/** 开始加工（匹配角磨配方；未安装角磨轮时不加工） */
	public void start(ItemStack inserted) {
		if (inventory.isEmpty())
			return;
		GrindingWheelTier tier = getWheelTier();
		if (tier == null)
			return;
		if (level.isClientSide)
			return;

		List<RecipeHolder<? extends Recipe<?>>> recipes = getRecipes();
		float time = 50;

		if (recipes.isEmpty()) {
			inventory.remainingTime = inventory.recipeDuration = 10;
			inventory.appliedRecipe = false;
			sendData();
			return;
		}

		recipeIndex++;
		if (recipeIndex >= recipes.size())
			recipeIndex = 0;

		// 加工耗时由角磨轮等级与当前转速决定（线性插值），不再使用配方 processingTime
		time = tier.getProcessingTime(Math.abs(getSpeed())) * 20;

		inventory.remainingTime = time;
		inventory.recipeDuration = inventory.remainingTime;
		inventory.appliedRecipe = false;
		sendData();
	}

	/** 应用配方产出成品；无匹配配方或结果为空返回 false（由调用方吞掉输入） */
	private boolean applyRecipe() {
		List<RecipeHolder<? extends Recipe<?>>> recipes = getRecipes();
		if (recipes.isEmpty())
			return false;
		if (recipeIndex >= recipes.size())
			recipeIndex = 0;

		Recipe<?> recipe = recipes.get(recipeIndex).value();
		List<ItemStack> results = new java.util.ArrayList<>();
		if (recipe instanceof ProcessingRecipe<?, ?> processing) {
			processing.rollResults(level.random)
				.forEach(stack -> results.add(stack));
		} else if (recipe instanceof DismantlingRecipe dismantling) {
			ItemStack out = dismantling.getResult(inventory.getStackInSlot(0));
			if (!out.isEmpty())
				results.add(out);
		}
		if (results.isEmpty())
			return false;

		// 一次加工消耗 1 个输入（槽 0）
		ItemStack input = inventory.getStackInSlot(0);
		input.shrink(1);
		if (input.isEmpty())
			inventory.setStackInSlot(0, ItemStack.EMPTY);

		// 成品放入槽 1+（堆叠或空槽），不影响剩余输入继续加工
		for (ItemStack result : results) {
			insertToOutput(result);
		}
		return true;
	}

	/** 成品插入槽 1+（堆叠或空槽）；全部槽满时剩余部分丢弃（输出槽 32 格，一般不会满） */
	private void insertToOutput(ItemStack stack) {
		for (int slot = 1; slot < inventory.getSlots(); slot++) {
			ItemStack left = inventory.insertItem(slot, stack, false);
			if (left.isEmpty())
				return;
			stack = left;
		}
	}

	/** 成品区（槽 1+）是否已空 */
	private boolean isOutputEmpty() {
		for (int slot = 1; slot < inventory.getSlots(); slot++)
			if (!inventory.getStackInSlot(slot)
				.isEmpty())
				return false;
		return true;
	}

	private List<RecipeHolder<? extends Recipe<?>>> getRecipes() {
		// 序列装配：物品处于某个序列加工配方中且当前步骤是角磨步骤（仿动力锯）
		Optional<RecipeHolder<GrindingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level,
			inventory.getStackInSlot(0), AllRecipeTypes.GRINDING.getType(), GrindingRecipe.class);
		if (assemblyRecipe.isPresent() && filtering.test(assemblyRecipe.get()
			.value()
			.getResultItem(level.registryAccess())))
			return List.of(assemblyRecipe.get());

		List<RecipeHolder<? extends Recipe<?>>> recipes = new java.util.ArrayList<>();

		// 基础角磨配方（全部等级可用）
		recipes.addAll(RecipeFinder.get(grindingRecipesKey, level,
			RecipeConditions.isOfType(AllRecipeTypes.GRINDING.getType())));

		// 高级角磨（≥2 级轮）：可执行 Create 粉碎轮/石磨的全部配方
		GrindingWheelTier tier = getWheelTier();
		if (tier != null && tier.level >= 2) {
			Predicate<RecipeHolder<? extends Recipe<?>>> advanced = RecipeConditions
				.isOfType(com.simibubi.create.AllRecipeTypes.CRUSHING.getType())
				.or(RecipeConditions.isOfType(com.simibubi.create.AllRecipeTypes.MILLING.getType()));
			recipes.addAll(RecipeFinder.get(advancedGrindingRecipesKey, level, advanced));
		}

		// 拆磨（≥3 级轮）：装备/武器/马鞍拆解
		if (tier != null && tier.level >= 3) {
			recipes.addAll(RecipeFinder.get(dismantlingRecipesKey, level,
				RecipeConditions.isOfType(AllRecipeTypes.DISMANTLING.getType())));
		}

		return recipes.stream()
			.filter(RecipeConditions.outputMatchesFilter(filtering))
			.filter(RecipeConditions.firstIngredientMatches(inventory.getStackInSlot(0)))
			.filter(r -> !AllRecipeTypes.shouldIgnoreInAutomation(r))
			.collect(Collectors.toList());
	}

	/** 物品移动方向：横向（垂直于轮轴），方向随转速正负（从盖往轮看） */
	public Vec3 getItemMovementVec() {
		Direction facing = getBlockState().getValue(PowerAngleGrinderBlock.HORIZONTAL_FACING);
		boolean facingZ = facing.getAxis() == Direction.Axis.Z;
		int offset = getSpeed() < 0 ? -1 : 1;
		return new Vec3(offset * (facingZ ? 1 : 0), 0, offset * (facingZ ? 0 : 1));
	}

	/** 加工粒子（物品在轮上缓慢移动的视觉，仿动力锯） */
	protected void spawnParticles(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return;

		ParticleOptions particleData = null;
		float speed = 1;
		if (stack.getItem() instanceof BlockItem)
			particleData = new BlockParticleOption(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock()
				.defaultBlockState());
		else {
			particleData = new ItemParticleOption(ParticleTypes.ITEM, stack);
			speed = .125f;
		}

		RandomSource r = level.random;
		Vec3 vec = getItemMovementVec();
		Vec3 pos = VecHelper.getCenterOf(this.worldPosition);
		float offset = inventory.recipeDuration != 0 ? (float) (inventory.remainingTime) / inventory.recipeDuration : 0;
		offset /= 2;
		if (inventory.appliedRecipe)
			offset -= .5f;
		// y 基准取轮位置（模型底座一致，一般无需调整）
		level.addParticle(particleData, pos.x() + -vec.x * offset, pos.y() + .45f, pos.z() + -vec.z * offset,
			-vec.x * speed, r.nextFloat() * speed, -vec.z * speed);
	}

	// ========== 角磨轮 ==========

	public ResourceLocation getWheel() {
		return wheel;
	}

	/** 当前安装角磨轮的等级；未安装或未知轮返回 null */
	private GrindingWheelTier getWheelTier() {
		ResourceLocation wheelId = getWheel();
		return wheelId == null ? null : GrindingWheelTier.from(wheelId);
	}

	public boolean installWheel(ItemStack stack) {
		if (!stack.is(AllTags.AllItemTags.GRINDING_WHEELS.tag))
			return false;
		wheel = stack.getItemHolder()
			.unwrapKey()
			.map(key -> key.location())
			.orElse(null);
		if (wheel == null)
			return false;
		notifyUpdate();
		return true;
	}

	public void removeWheel() {
		wheel = null;
		notifyUpdate();
	}

	// ========== 护目镜信息 ==========

	@Override
	public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToTooltip(tooltip, isPlayerSneaking);

		// 悬停提示：当前支持的加工类型（随安装的角磨轮等级变化）
		GrindingWheelTier tier = getWheelTier();
		if (tier == null) {
			tooltip.add(Component.translatable("createoreexpansion.tooltip.no_wheel_type")
				.withStyle(ChatFormatting.GRAY));
			return true;
		}
		tooltip.add(Component.translatable("createoreexpansion.tooltip.supported_types")
			.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("createoreexpansion.tooltip.type_grinding")
			.withStyle(ChatFormatting.GRAY));
		if (tier.level >= 2) {
			tooltip.add(Component.translatable("createoreexpansion.tooltip.type_advanced")
				.withStyle(ChatFormatting.GRAY));
		}
		if (tier.level >= 3) {
			tooltip.add(Component.translatable("createoreexpansion.tooltip.type_dismantling")
				.withStyle(ChatFormatting.GRAY));
		}
		return true;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		tooltip.add(Component.translatable("createoreexpansion.goggles.angle_grinder")
			.withStyle(ChatFormatting.GRAY));
		added = true;

		ResourceLocation wheelId = getWheel();
		if (wheelId == null) {
			tooltip.add(Component.translatable("createoreexpansion.goggles.no_wheel")
				.withStyle(ChatFormatting.RED));
			return added;
		}

		Item wheelItem = BuiltInRegistries.ITEM.get(wheelId);
		Component wheelName = wheelItem != null && wheelItem != Items.AIR
			? wheelItem.getDescription()
			: Component.literal(wheelId.toString());
		tooltip.add(Component.translatable("createoreexpansion.goggles.installed_wheel", wheelName)
			.withStyle(ChatFormatting.WHITE));

		GrindingWheelTier tier = GrindingWheelTier.from(wheelId);
		if (tier == null)
			return added;

		tooltip.add(Component.translatable("createoreexpansion.goggles.required_speed", tier.minRpm)
			.withStyle(ChatFormatting.GOLD));

		float speed = Math.abs(getSpeed());
		if (speed < tier.minRpm) {
			tooltip.add(Component.translatable("createoreexpansion.goggles.speed_too_low")
				.withStyle(ChatFormatting.RED));
		} else {
			tooltip.add(Component.translatable("createoreexpansion.goggles.processing_time",
				String.format(Locale.ROOT, "%.1f", tier.getProcessingTime(speed)))
				.withStyle(ChatFormatting.AQUA));
		}
		return added;
	}

	// ========== 持久化 ==========

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.put("Inventory", inventory.serializeNBT(registries));
		compound.putInt("RecipeIndex", recipeIndex);
		if (wheel != null) {
			compound.putString("Wheel", wheel.toString());
		}
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
		recipeIndex = compound.getInt("RecipeIndex");
		wheel = compound.contains("Wheel")
			? ResourceLocation.tryParse(compound.getString("Wheel"))
			: null;
	}

	@Override
	public void clearContent() {
		inventory.clear();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(level, worldPosition, inventory);
	}
}
