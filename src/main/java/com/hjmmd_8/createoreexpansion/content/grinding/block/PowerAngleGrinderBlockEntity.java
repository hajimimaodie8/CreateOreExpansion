package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.common.AllTags;
import com.hjmmd_8.createoreexpansion.content.grinding.behaviour.GrinderInventory;
import com.hjmmd_8.createoreexpansion.content.grinding.behaviour.SidedItemHandlers;
import com.hjmmd_8.createoreexpansion.content.grinding.effect.GrindingWheelEffect;
import com.hjmmd_8.createoreexpansion.content.grinding.effect.GrindingWheelEffects;
import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelTier;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrinderRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
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
import java.util.stream.Collectors;

/**
 * 动力角磨床方块实体：应力驱动加工（仿动力锯）。
 *
 * <p>触发方式：物品从上方丢入、或从侧面漏斗/传送带输入；
 * 有应力（转速）时角磨轮工作，物品在轮上处理（粒子沿移动方向偏移表现缓慢移动），
 * 完成后经漏斗引出或向输出方向抛出（方向随转速正负，从盖往轮看）。</p>
 */
public class PowerAngleGrinderBlockEntity extends KineticBlockEntity implements Clearable {

	public FilteringBehaviour filtering;
	public GrinderInventory inventory;

	private int recipeIndex;
	/** 已安装的角磨轮物品 id（null = 未安装） */
	private ResourceLocation wheel;
	/** 当前是否为序列装配的角磨步骤（序列加工时禁用轮子产出类效果） */
	private boolean sequenceStep;

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
		if (tier == null || Math.abs(getSpeed()) < tier.getMinRpm())
			return;
		if (getSpeed() == 0)
			return;

		if (inventory.remainingTime == -1) {
			// 仅槽 0 有输入才启动加工（成品区槽 1+ 有成品不影响新输入）
			if (!inventory.getStackInSlot(0)
				.isEmpty() && !inventory.appliedRecipe)
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
				// 无匹配配方（或结果为空）：消耗 1 个输入但不产出（物品一点一点减少，不瞬间消失）
				consumeInputNoOutput();
			}
			return;
		}

		if (inventory.remainingTime > 0)
			return;
		inventory.remainingTime = 0;

		// 加工完成：成品已存入槽 1+（insertToOutput），立即重置并继续加工槽 0 剩余输入。
		// 成品区 32 格可堆叠、漏斗可随时抽取（见 GrinderInventory.extractItem），
		// 无需阻塞等待成品被抽走（无漏斗时也能连续加工，槽满时 insertToOutput 会掉落不丢失）
		if (inventory.appliedRecipe) {
			inventory.remainingTime = -1;
			inventory.appliedRecipe = false;
			sendData();
			return;
		}

		// 无匹配配方（不可加工物品）：消耗 1 个输入但不产出，保留成品区（槽 1+）
		consumeInputNoOutput();
	}

	/** 消耗 1 个输入但不产出任何物品（无匹配配方：物品逐个减少，加工节奏/粒子正常，不瞬间消失） */
	private void consumeInputNoOutput() {
		ItemStack input = inventory.getStackInSlot(0);
		input.shrink(1);
		if (input.isEmpty())
			inventory.setStackInSlot(0, ItemStack.EMPTY);
		inventory.remainingTime = -1;
		sendData();
	}

	/**
	 * 输入物品（上方丢入/漏斗/传送带/开盖放入）：仅当槽 0 空时放入（成品区有成品不影响新输入）。
	 * @return 是否成功放入（槽 0 已有物品时返回 false，物品保留在原处不丢失）
	 */
	public boolean insertItem(ItemStack stack) {
		if (inventory.getStackInSlot(0)
			.isEmpty() && !stack.isEmpty()) {
			inventory.setStackInSlot(0, stack.copy());
			sendData();
			return true;
		}
		return false;
	}

	/** 顶面掉落物输入：仅成功放入时才销毁物品实体（槽 0 已有物品时物品保留在掉落物，不消失） */
	public void insertItem(ItemEntity entity) {
		if (!entity.isAlive())
			return;
		if (level.isClientSide)
			return;
		if (insertItem(entity.getItem()))
			entity.discard();
	}

	/** 合盖时空手右键（分批取出，均批量）：
	 * 第一次右键取成品区（槽 1+，全部加工产物）；成品区已空时再右键取原料
	 * （槽 0，误放进去的未加工物品，整组取出）。未取出的部分不影响加工。 */
	public void retrieveAll(Player player) {
		if (level.isClientSide)
			return;
		boolean taken = false;
		// 第一次右键：优先取成品区（槽 1+）
		for (int i = 1; i < inventory.getSlots(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack.isEmpty())
				continue;
			ItemHandlerHelper.giveItemToPlayer(player, stack);
			inventory.setStackInSlot(i, ItemStack.EMPTY);
			taken = true;
		}
		// 成品区已空：第二次右键取原料（槽 0，整组）
		if (!taken) {
			ItemStack stack = inventory.getStackInSlot(0);
			if (!stack.isEmpty()) {
				ItemHandlerHelper.giveItemToPlayer(player, stack);
				inventory.setStackInSlot(0, ItemStack.EMPTY);
			}
		}
		sendData();
	}

	/** 开始加工（匹配角磨配方；未安装角磨轮时不加工） */
	public void start(ItemStack inserted) {
		// 仅槽 0 有输入才启动（成品区槽 1+ 有成品不影响新输入，避免空转误清成品）
		if (inventory.getStackInSlot(0)
			.isEmpty())
			return;
		GrindingWheelTier tier = getWheelTier();
		if (tier == null)
			return;
		if (level.isClientSide)
			return;

		List<RecipeHolder<? extends Recipe<?>>> recipes = getRecipes();
		// 加工耗时由角磨轮等级与当前转速决定（线性插值）再乘轮子效果倍率
		float time = tier.getProcessingTime(Math.abs(getSpeed())) * 20 * getWheelEffect().getTimeMultiplier();

		if (recipes.isEmpty()) {
			// 无匹配配方：仍按正常节奏加工（有粒子、逐个消耗），只是不产出任何物品
			inventory.remainingTime = inventory.recipeDuration = time;
			inventory.appliedRecipe = false;
			sendData();
			return;
		}

		recipeIndex++;
		if (recipeIndex >= recipes.size())
			recipeIndex = 0;

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

		// 轮子特殊效果：额外产出/数量提升/双倍等（序列加工步骤不应用，避免序列配方产出爆炸）
		if (!sequenceStep)
			getWheelEffect().onProcessCompleted(input, results, level.random);

		// 成品统一存入库存（槽 1+）：有输出漏斗由漏斗抽取，无漏斗时玩家空手右键取出
		// （机器本质是容器；手动放入与漏斗输入走同一套输出逻辑，行为一致）
		for (ItemStack result : results) {
			insertToOutput(result);
		}
		return true;
	}

	/** 待渲染的物品（仿置物台显示）：未加工（槽 0）优先；槽 0 空时取成品区（槽 1+）第一格。
	 * 多个物品（未加工 + 已加工）时只渲染未加工；只有一种物品（无论未加工/已加工）时渲染该种。 */
	public ItemStack getRenderedItem() {
		ItemStack stack = inventory.getStackInSlot(0);
		if (!stack.isEmpty())
			return stack;
		for (int i = 1; i < inventory.getSlots(); i++) {
			stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty())
				return stack;
		}
		return ItemStack.EMPTY;
	}

	/** 成品插入槽 1+（堆叠或空槽）；全部槽满时剩余部分掉落到机器上方，不再静默丢失。
	 *
	 * <p>用 {@code setStackInSlot} 直接存入：父类 {@code ProcessingInventory.isItemValid}
	 * 只允许物品插入槽 0（输入区），走 {@code insertItem} 到成品区会被拒绝并返回原物品，
	 * 导致成品静默消失；直接设置槽位绕过该输入验证（成品区为内部输出，外部漏斗
	 * 的插入通道仍受 isItemValid 限制，不会污染输出槽）。</p> */
	private void insertToOutput(ItemStack stack) {
		for (int slot = 1; slot < inventory.getSlots(); slot++) {
			ItemStack existing = inventory.getStackInSlot(slot);
			if (existing.isEmpty()) {
				inventory.setStackInSlot(slot, stack);
				return;
			}
			if (ItemStack.isSameItemSameComponents(existing, stack)) {
				int space = existing.getMaxStackSize() - existing.getCount();
				if (stack.getCount() <= space) {
					existing.grow(stack.getCount());
					inventory.setStackInSlot(slot, existing);
					return;
				}
				existing.grow(space);
				inventory.setStackInSlot(slot, existing);
				stack = stack.copy();
				stack.shrink(space);
			}
		}
		// 全部槽满：剩余部分掉落到机器上方（不静默丢失）
		if (!stack.isEmpty()) {
			ItemEntity drop = new ItemEntity(level, worldPosition.getX() + .5, worldPosition.getY() + 1,
				worldPosition.getZ() + .5, stack);
			drop.setDeltaMovement(Vec3.ZERO);
			level.addFreshEntity(drop);
		}
	}

	private List<RecipeHolder<? extends Recipe<?>>> getRecipes() {
		// 序列装配：物品处于某个序列加工配方中且当前步骤是角磨步骤（仿动力锯）
		Optional<RecipeHolder<GrindingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level,
			inventory.getStackInSlot(0), AllRecipeTypes.GRINDING.getType(), GrindingRecipe.class);
		if (assemblyRecipe.isPresent() && filtering.test(assemblyRecipe.get()
			.value()
			.getResultItem(level.registryAccess()))) {
			sequenceStep = true;
			return List.of(assemblyRecipe.get());
		}
		sequenceStep = false;

		List<RecipeHolder<? extends Recipe<?>>> recipes = new java.util.ArrayList<>();

		// 按角磨轮等级遍历配方类型注册表（开闭：新增配方类型只需 GrinderRecipeTypes.register，
		// 本方法不感知具体类型；1 级=GRINDING，2 级+=CRUSHING/MILLING，3 级+=DISMANTLING）
		GrindingWheelTier tier = getWheelTier();
		if (tier != null) {
			for (IRecipeTypeInfo typeInfo : GrinderRecipeTypes.getFor(tier.level)) {
				// key 用 typeInfo 对象本身（枚举常量，对象身份唯一稳定）：
				// RecipeFinder 缓存按 key 全局缓存，用 ResourceLocation 作 key 可能与其他代码缓存冲突
				recipes.addAll(RecipeFinder.get(typeInfo, level,
					RecipeConditions.isOfType(typeInfo.getType())));
			}
		}

		return recipes.stream()
			.filter(RecipeConditions.outputMatchesFilter(filtering))
			.filter(RecipeConditions.firstIngredientMatches(inventory.getStackInSlot(0)))
			.filter(r -> !AllRecipeTypes.shouldIgnoreInAutomation(r))
			.collect(Collectors.toList());
	}

	/**
	 * 加工粒子（细腻化）：碎屑沿角磨轮<b>切向</b>飞溅，方向跟随轮子旋转。
	 *
	 * <p>Create 转速约定：{@code getSpeed() > 0} = 从轴正方向（FACING）看<b>逆时针</b>
	 * （渲染 angle = time×speed 绕轴正方向旋转）。玩家从盖板（FACING 对面）看时，
	 * {@code getSpeed() < 0} 为<b>顺时针</b>，此时粒子切向 = {@code radial × axis}；
	 * 反之（逆时针）切向 = {@code axis × radial} —— 粒子始终与轮子同向飞溅。</p>
	 *
	 * <p>每 tick 生成 2~3 个碎屑：位置在轮缘附近（轮心=方块中心，轮半径 4px→0.25 格），
	 * 速度以切向为主（随转速增强）并加随机散布模拟飞溅扩散。</p>
	 */
	protected void spawnParticles(ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return;

		ParticleOptions particleData = null;
		if (stack.getItem() instanceof BlockItem)
			particleData = new BlockParticleOption(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock()
				.defaultBlockState());
		else
			particleData = new ItemParticleOption(ParticleTypes.ITEM, stack);

		RandomSource r = level.random;
		Direction facing = getBlockState().getValue(PowerAngleGrinderBlock.HORIZONTAL_FACING);
		boolean axisZ = facing.getAxis() == Direction.Axis.Z;
		Vec3 axis = Vec3.atLowerCornerOf(facing.getNormal()); // 轮轴（FACING 方向）
		Vec3 center = VecHelper.getCenterOf(this.worldPosition); // 轮心（轮模型中心≈方块中心）
		float radius = 0.25f; // 轮半径 4px → 0.25 格
		float spin = Math.abs(getSpeed());
		// 飞溅速度随转速增强：0.12 ~ 0.35 格/秒
		float base = 0.12f + Math.min(spin / 256f, 1f) * 0.23f;

		// 每 tick 2~3 个碎屑
		int count = 2 + r.nextInt(2);
		for (int i = 0; i < count; i++) {
			// 轮面内随机径向（轮缘附近 75%~100% 半径）
			double a = r.nextDouble() * Math.PI * 2;
			double rr = radius * (0.75 + r.nextDouble() * 0.25);
			Vec3 radial = axisZ ? new Vec3(Math.cos(a) * rr, Math.sin(a) * rr, 0)
				: new Vec3(0, Math.cos(a) * rr, Math.sin(a) * rr);

			// 切向：getSpeed<0（从盖看顺时针）→ radial×axis，否则（逆时针）→ axis×radial
			Vec3 tangent = getSpeed() < 0 ? radial.cross(axis) : axis.cross(radial);

			// 粒子位置：轮心 + 径向 + 少量轴向散布（轮厚 4px）
			Vec3 spawn = center.add(radial)
				.add(axis.scale((r.nextDouble() - 0.5) * 0.25));

			// 速度：切向为主 + 随机散布（飞溅扩散）
			double vx = tangent.x * base + r.nextGaussian() * 0.03;
			double vy = tangent.y * base + r.nextGaussian() * 0.03;
			double vz = tangent.z * base + r.nextGaussian() * 0.03;
			level.addParticle(particleData, spawn.x, spawn.y, spawn.z, vx, vy, vz);
		}
	}

	// ========== 角磨轮 ==========

	public ResourceLocation getWheel() {
		return wheel;
	}

	/** 当前安装角磨轮的等级；未安装或未知轮返回 null */
	private GrindingWheelTier getWheelTier() {
		ResourceLocation wheelId = getWheel();
		if (wheelId == null)
			return null;
		Item item = BuiltInRegistries.ITEM.get(wheelId);
		return item != null && item != Items.AIR ? GrindingWheelTier.from(new ItemStack(item)) : null;
	}

	/** 当前安装角磨轮的特殊效果；未安装返回无效果 */
	private GrindingWheelEffect getWheelEffect() {
		ResourceLocation wheelId = getWheel();
		return wheelId == null ? GrindingWheelEffect.NONE : GrindingWheelEffects.get(wheelId);
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

		// 轮子特殊效果（护目镜可见）
		Component effectDescription = getWheelEffect().getDescription();
		if (!effectDescription.getString()
			.isEmpty())
			tooltip.add(effectDescription.copy()
				.withStyle(ChatFormatting.AQUA));

		GrindingWheelTier tier = getWheelTier();
		if (tier == null)
			return added;

		tooltip.add(Component.translatable("createoreexpansion.goggles.required_speed", tier.getMinRpm())
			.withStyle(ChatFormatting.GOLD));

		float speed = Math.abs(getSpeed());
		if (speed < tier.getMinRpm()) {
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
