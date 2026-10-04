package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.AllTags;
import com.hjmmd_8.createoreexpansion.content.grinding.behaviour.GrinderInventory;
import com.hjmmd_8.createoreexpansion.content.grinding.behaviour.SidedItemHandlers;
import com.hjmmd_8.createoreexpansion.content.grinding.effect.GrindingWheelEffect;
import com.hjmmd_8.createoreexpansion.content.grinding.effect.GrindingWheelEffects;
import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelTier;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;

import net.createmod.catnip.math.VecHelper;
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

/**
 * 动力角磨床方块实体：应力驱动加工（仿动力锯）。
 *
 * <p>触发方式：物品从上方丢入、或从侧面漏斗/传送带输入；
 * 有应力（转速）时角磨轮工作，物品在轮上处理（粒子沿移动方向偏移表现缓慢移动），
 * 完成后经漏斗引出或向输出方向抛出（方向随转速正负，从盖往轮看）。</p>
 *
 * <h2>本类的形状（2026-10-05 行为零变化拆分）</h2>
 * <p>本类保留<b>公开/受保护形状与运行态</b>（物品与电容的进出、角磨轮的装卸与查询、护目镜与悬停
 * 文案、持久化、粒子），按职责域拆出的同包类只做搬运：</p>
 * <ul>
 *   <li>{@link GrinderProcessing} —— 每 tick 的加工流程（合盖 / 装轮且转速够 / 转速非 0 三道门、
 *       {@code remainingTime} 推进、完成与"无配方"两条收尾）；</li>
 *   <li>{@link GrinderRecipeRunner} —— 配方解析与产出落库（匹配哪些配方、产出什么、成品放哪儿）；</li>
 *   <li>{@link GrinderGoggles} —— 护目镜 / 悬停栏的文案装配（两个 {@code @Override} 仍在本类，
 *       只保留 {@code super} 调用与转交）。</li>
 * </ul>
 * <p>构造期只做字段初始化（{@code inventory} / {@code recipeIndex}），没有调用任何被搬出去的 helper。</p>
 */
public class PowerAngleGrinderBlockEntity extends KineticBlockEntity implements Clearable {

	public FilteringBehaviour filtering;
	public GrinderInventory inventory;

	int recipeIndex;
	/** 已安装的角磨轮物品 id（null = 未安装） */
	private ResourceLocation wheel;
	/** 当前是否为序列装配的角磨步骤（序列加工时禁用轮子产出类效果） */
	boolean sequenceStep;

	public PowerAngleGrinderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new GrinderInventory(this::start);
		inventory.remainingTime = -1;
		recipeIndex = 0;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
			Capabilities.ItemHandler.BLOCK,
			CoeBlockEntityTypes.POWER_ANGLE_GRINDER.get(),
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

		// 加工流程（合盖 / 装轮且转速够 / 转速非 0 三道门 + remainingTime 推进 + 两条收尾）
		// 逐字搬到 GrinderProcessing#tick；本方法只剩基类记账与这次转交。
		GrinderProcessing.tick(this);
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

		List<RecipeHolder<? extends Recipe<?>>> recipes = GrinderRecipeRunner.getRecipes(this);
		// 加工耗时由角磨轮等级与当前转速决定（线性插值）再乘轮子效果倍率
		float time = tier.getProcessingTime(Math.abs(getSpeed())) * 20 * getWheelEffect().getTimeMultiplier();

		if (recipes.isEmpty()) {
			// 有配方过滤器：物品无匹配配方（输出不匹配过滤器）→ 拒绝加工，物品保留在槽 0
			// （如过滤器设为小块矿石，钻石等无对应输出配方的物品不会被磨掉）
			if (!filtering.getFilter()
				.isEmpty()) {
				inventory.remainingTime = -1;
				sendData();
				return;
			}
			// 无过滤器：仍按正常节奏加工（有粒子、逐个消耗），只是不产出任何物品
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

	/**
	 * 当前安装角磨轮的等级；未安装或未知轮返回 null。
	 *
	 * <p><b>公开只读入口</b>：角磨轮的等级决定这台机器<b>能执行哪些配方类型</b>
	 * （见 {@link GrinderRecipeTypes}），所以星辉波变器"读取周围机器"时要读它——
	 * 变器档案的选择器即调本方法（见 {@code StellarWaveMachineCatalog}）。</p>
	 */
	public GrindingWheelTier getWheelTier() {
		ResourceLocation wheelId = getWheel();
		if (wheelId == null)
			return null;
		Item item = BuiltInRegistries.ITEM.get(wheelId);
		return item != null && item != Items.AIR ? GrindingWheelTier.from(new ItemStack(item)) : null;
	}

	/** 当前安装角磨轮的特殊效果；未安装返回无效果 */
	GrindingWheelEffect getWheelEffect() {
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
		super.addToTooltip(tooltip, isPlayerSneaking);
		// 悬停提示的文案装配逐字搬到 GrinderGoggles#appendSupportedTypes（返回值恒为 true，与原来同值）
		GrinderGoggles.appendSupportedTypes(this, tooltip);
		return true;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		// 护目镜行的装配逐字搬到 GrinderGoggles#appendGoggleLines（added 由宿主带进去、原样带出来）
		return GrinderGoggles.appendGoggleLines(this, tooltip, added);
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
