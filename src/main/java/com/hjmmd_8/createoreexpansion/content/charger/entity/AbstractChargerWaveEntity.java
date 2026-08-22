package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 应力充能器能量波实体抽象基类：不渲染模型（视觉靠粒子）。
 *
 * <p>通用行为（模板方法）：服务端飞行、命中判定（生物伤害/掉落物加工/置物台加工/撞墙消散）、
 * 粒子拖尾与命中绽放、加工完成音效；客户端在消散时补充球面均匀扩散绽放。</p>
 *
 * <p>子类（翡翠充能波/雷鸣充能波）只需覆写 {@link #getWaveColor()} 定制三档充能态颜色，
 * 粒子统一用原版染色粒子（DustParticleOptions），无需注册任何自定义粒子类型。</p>
 *
 * <p>等级：1=低充能（2 格/秒、4 伤害）、2=高充能（4 格/秒、6 伤害）、3=伽马（6 格/秒、10 伤害）。</p>
 */
public abstract class AbstractChargerWaveEntity extends Entity {

	protected int waveLevel;
	protected Vec3 movement = Vec3.ZERO;

	protected AbstractChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	protected AbstractChargerWaveEntity(EntityType<?> type, Level level, Vec3 pos, Direction facing, int waveLevel) {
		this(type, level);
		this.waveLevel = waveLevel;
		this.movement = Vec3.atLowerCornerOf(facing.getNormal());
		setPos(pos);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();

		if (level().isClientSide)
			return;

		// 移动（速度随等级）
		setPos(position().add(movement.scale(getSpeedBlocks() / 20d)));

		// 飞行拖尾粒子（密集，沿移动方向散布）
		if (level() instanceof ServerLevel server) {
			Vec3 color = getWaveColor();
			server.sendParticles(getWaveParticle(color, 0.45f), getX(), getY(), getZ(), 6,
				movement.x * 0.12, movement.y * 0.12, movement.z * 0.12, 0.03);
		}

		// 命中生物：造成伤害
		List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox()
			.inflate(0.1), e -> e.isAlive());
		if (!entities.isEmpty()) {
			LivingEntity target = entities.get(0);
			if (!(target instanceof Player player) || !player.isCreative()) {
				target.hurt(level().damageSources()
					.indirectMagic(this, null), getDamage());
			}
			burst();
			discard();
			return;
		}

		// 命中掉落物：给能量工具充能 / 普通物品按配方转化（检测范围覆盖移动路径，避免高速跳过）
		AABB sweep = getBoundingBox().expandTowards(movement.x * getSpeedBlocks() / 20d,
			movement.y * getSpeedBlocks() / 20d, movement.z * getSpeedBlocks() / 20d)
			.inflate(0.3);
		List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, sweep, e -> e.isAlive());
		if (!items.isEmpty()) {
			processItem(items.get(0));
			burst();
			discard();
			return;
		}

		// 命中方块：先尝试置物台/工作台（有物品槽的方块）充能，其余方块视为撞墙消散
		boolean hitSolid = false;
		for (BlockPos pos : BlockPos.betweenClosed(
			Mth.floor(getBoundingBox().minX), Mth.floor(getBoundingBox().minY), Mth.floor(getBoundingBox().minZ),
			Mth.floor(getBoundingBox().maxX), Mth.floor(getBoundingBox().maxY), Mth.floor(getBoundingBox().maxZ))) {
			BlockState state = level().getBlockState(pos);
			if (state.isAir())
				continue;
			IItemHandler handler = level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler != null) {
				if (chargeInHandler(handler)) {
					burst();
					discard();
					return;
				}
				continue; // 有物品槽但无匹配物品：穿过
			}
			hitSolid = true;
		}
		if (hitSolid) {
			burst();
			discard();
		}
	}

	/**
	 * 查询物品对应的充能配方：遍历全部 charging 配方，取「等级 ≤ 本能量波等级」中等级最高者。
	 *
	 * <p>例如伽马波（3）命中工具：匹配 level 3 配方（充 1000 点）；低波（1）命中：只匹配
	 * level 1 配方（充 100 点）。等级不够的配方（level &gt; 本波等级）跳过，不加工。</p>
	 *
	 * @return 匹配到的配方，无匹配返回 null
	 */
	private ChargingRecipe findRecipe(ItemStack stack) {
		if (stack.isEmpty())
			return null;
		@SuppressWarnings("unchecked")
		RecipeType<ChargingRecipe> type = (RecipeType<ChargingRecipe>) (RecipeType<?>) AllRecipeTypes.CHARGING.getType();
		ChargingRecipe best = null;
		for (RecipeHolder<ChargingRecipe> holder : level().getRecipeManager()
			.getAllRecipesFor(type)) {
			ChargingRecipe recipe = holder.value();
			if (recipe.getLevel() > waveLevel)
				continue; // 本波等级不够：不加工
			if (!recipe.matches(new SingleRecipeInput(stack), level()))
				continue;
			if (best == null || recipe.getLevel() > best.getLevel())
				best = recipe;
		}
		return best;
	}

	/** 对命中掉落物执行：能量工具充能 / 普通物品按配方消耗 1 个并产出结果 */
	private void processItem(ItemEntity item) {
		ItemStack stack = item.getItem();
		ChargingRecipe recipe = findRecipe(stack);
		if (recipe == null)
			return;

		// 能量工具/凝能佩：充能（封顶到最大能量），不消耗物品
		if (ToolEnergy.hasEnergy(stack)) {
			ToolEnergy.setEnergy(stack, ToolEnergy.getEnergy(stack) + ChargingRecipe.energyForLevel(waveLevel));
			// setItem 传副本（新引用）：ItemEntity.setItem 内部按引用判断是否更新，
			// 原地修改 + setItem(同引用) 不会触发数据刷新，导致 Jade 等外部读取显示旧能量
			item.setItem(stack.copy());
			return;
		}

		// 普通物品：消耗 1 个输入，按配方产出结果（落在原位置）
		List<ItemStack> results = recipe.rollResults(level().random);
		if (results.isEmpty())
			return;

		stack.shrink(1);
		if (stack.isEmpty())
			item.discard();
		Vec3 pos = item.position();
		for (ItemStack result : results) {
			ItemEntity out = new ItemEntity(level(), pos.x, pos.y, pos.z, result);
			out.setDeltaMovement(item.getDeltaMovement());
			level().addFreshEntity(out);
		}
	}

	/** 尝试给置物台/工作台（方块物品槽）中的物品执行充能加工。成功返回 true。 */
	private boolean chargeInHandler(IItemHandler handler) {
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
					ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), remainder);
					drop.setDeltaMovement(Vec3.ZERO);
					level().addFreshEntity(drop);
				}
				return true;
			}

			// 普通物品：消耗 1 个输入，按配方产出结果（放回槽位，放不下则掉落）
			ItemStack input = handler.extractItem(slot, 1, false);
			if (input.isEmpty())
				return false;
			List<ItemStack> results = recipe.rollResults(level().random);
			for (ItemStack result : results) {
				ItemStack remainder = handler.insertItem(slot, result, false);
				if (!remainder.isEmpty()) {
					ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), remainder);
					drop.setDeltaMovement(Vec3.ZERO);
					level().addFreshEntity(drop);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		// 客户端在实体消散时补充球面均匀扩散绽放
		if (reason == RemovalReason.DISCARDED && level().isClientSide) {
			burstParticles();
		}
		super.remove(reason);
	}

	/** 服务端命中绽放：对应颜色向外扩散的染色粒子（大散布 + 速度，近似球面扩散）+ 加工完成音效（紫水晶共鸣） */
	private void burst() {
		if (!(level() instanceof ServerLevel server))
			return;
		Vec3 color = getWaveColor();
		server.sendParticles(getWaveParticle(color, 0.6f), getX(), getY(), getZ(), 30,
			0.5, 0.5, 0.5, 0.3);
		server.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
			SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** 客户端球面均匀扩散绽放（对应颜色） */
	private void burstParticles() {
		Vec3 color = getWaveColor();
		RandomSource random = level().random;
		for (int i = 0; i < 30; i++) {
			Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
				.normalize();
			level().addParticle(getWaveParticle(color, 0.5f), getX(), getY(), getZ(),
				dir.x * 0.35, dir.y * 0.35, dir.z * 0.35);
		}
	}

	/** 移动速度（格/秒）：低 2、高 4、伽马 6 —— 子类可覆写定制（如雷鸣波更快） */
	protected double getSpeedBlocks() {
		return switch (waveLevel) {
			case 2 -> 4;
			case 3 -> 6;
			default -> 2;
		};
	}

	/** 命中伤害：低 4、高 6、伽马 10 —— 子类可覆写定制（如雷鸣波更高） */
	protected float getDamage() {
		return switch (waveLevel) {
			case 2 -> 6f;
			case 3 -> 10f;
			default -> 4f;
		};
	}

	/** 能量波颜色（RGB 0-1），随充能等级变化 —— 子类各自配色（如翡翠黄/绿/蓝、雷鸣紫系） */
	protected abstract Vec3 getWaveColor();

	/** 能量波粒子数据：原版染色粒子（可配 RGB，客户端无需任何注册） */
	protected ParticleOptions getWaveParticle(Vec3 color, float scale) {
		return new DustParticleOptions(
			new Vector3f((float) color.x, (float) color.y, (float) color.z), scale);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		waveLevel = tag.getInt("WaveLevel");
		movement = new Vec3(tag.getDouble("MoveX"), tag.getDouble("MoveY"), tag.getDouble("MoveZ"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("WaveLevel", waveLevel);
		tag.putDouble("MoveX", movement.x);
		tag.putDouble("MoveY", movement.y);
		tag.putDouble("MoveZ", movement.z);
	}
}
