package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.AllTags;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 动力角磨床：依靠机械动力（应力）运转的加工机器。
 *
 * <p>水平朝向（{@link HorizontalKineticBlock}，FACING 仅水平 4 向，随玩家放置）；
 * 水平旋转轴沿 FACING 轴，传动轴从 FACING 前方（齿轮箱纹理背板侧）接入；
 * 盖板在 FACING 对面一侧，开盖时朝那一侧翻起（那一格被方块阻挡则提示无法开盖）。</p>
 */
public class PowerAngleGrinderBlock extends HorizontalKineticBlock implements IBE<PowerAngleGrinderBlockEntity>, IWrenchable {

	/** 开盖状态：false=关盖，true=开盖（对应开盖模型 power_angle_grinder_rotated） */
	public static final BooleanProperty OPEN = BooleanProperty.create("open");

	public PowerAngleGrinderBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(OPEN);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING)
			.getAxis(); // 水平轴：沿机器朝向
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		// 传动轴接口在齿轮箱纹理背板侧（模型 z+ = FACING 前方）；盖板侧（FACING 背后）不是传动轴面
		return face == state.getValue(HORIZONTAL_FACING);
	}

	@Override
	public Class<PowerAngleGrinderBlockEntity> getBlockEntityClass() {
		return PowerAngleGrinderBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PowerAngleGrinderBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.POWER_ANGLE_GRINDER.get();
	}

	// ========== 扳手开盖/关盖 ==========

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();

		boolean open = state.getValue(OPEN);
		if (!open && isCoverBlocked(level, pos, state.getValue(HORIZONTAL_FACING)
			.getOpposite())) {
			// 开盖方向（盖板在 FACING 对面一侧翻起）那一格有方块阻挡：不开盖，提示空间不足
			if (player != null) {
				player.displayClientMessage(
					Component.translatable("createoreexpansion.msg.cannot_open_cover"), true);
			}
			return InteractionResult.SUCCESS;
		}

		// 切换开盖状态
		KineticBlockEntity.switchToBlockState(level, pos, state.cycle(OPEN));
		if (!level.isClientSide) {
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		}
		return InteractionResult.SUCCESS;
	}

	/** 开盖方向（盖板在 FACING 对面一侧）那一格是否被不可替换方块阻挡 */
	private static boolean isCoverBlocked(Level level, BlockPos pos, Direction facing) {
		BlockState front = level.getBlockState(pos.relative(facing));
		return !front.isAir() && !front.canBeReplaced();
	}

	// ========== 角磨轮安装/取出 ==========

	@Override
	public void updateEntityAfterFallOn(net.minecraft.world.level.BlockGetter level, net.minecraft.world.entity.Entity entity) {
		super.updateEntityAfterFallOn(level, entity);
		if (!(entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity))
			return;
		if (entity.level().isClientSide)
			return;
		BlockPos pos = entity.blockPosition();
		withBlockEntityDo(entity.level(), pos, be -> {
			if (be.getSpeed() == 0)
				return;
			be.insertItem(itemEntity);
		});
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											  Player player, InteractionHand hand, BlockHitResult hitResult) {
		boolean open = state.getValue(OPEN);

		// 手持角磨轮：开盖时安装
		if (stack.is(AllTags.AllItemTags.GRINDING_WHEELS.tag)) {
			if (!open) {
				// 未开盖：提示先开盖
				if (player != null && !level.isClientSide) {
					player.displayClientMessage(
						Component.translatable("createoreexpansion.msg.need_open_cover"), true);
				}
				return ItemInteractionResult.SUCCESS;
			}
			return onBlockEntityUseItemOn(level, pos, be -> {
				if (!be.installWheel(stack))
					return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
				if (!level.isClientSide && player != null && !player.isCreative()) {
					stack.shrink(1);
				}
				return ItemInteractionResult.SUCCESS;
			});
		}

		// 扳手：交给 Create 处理（开盖/关盖）
		if (com.simibubi.create.AllItems.WRENCH.isIn(stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		// 开盖：空手取出角磨轮；手持物品放入机器（手动加工）
		if (open) {
			return onBlockEntityUseItemOn(level, pos, be -> {
				if (stack.isEmpty()) {
					if (be.getWheel() == null)
						return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
					if (!level.isClientSide) {
						Item wheelItem = BuiltInRegistries.ITEM.get(be.getWheel());
						if (wheelItem != null && player != null) {
							ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(wheelItem));
						}
						be.removeWheel();
					}
					return ItemInteractionResult.SUCCESS;
				}
				// 手持物品：放入机器（合盖后自动加工）
				if (!level.isClientSide && player != null) {
					be.insertItem(stack.copy());
					if (!player.isCreative()) {
						stack.shrink(1);
					}
				}
				return ItemInteractionResult.SUCCESS;
			});
		}

		// 合盖：空手右键取出库存中的物品（成品）
		if (stack.isEmpty()) {
			return onBlockEntityUseItemOn(level, pos, be -> {
				if (be.inventory.isEmpty())
					return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
				if (!level.isClientSide && player != null) {
					be.retrieveAll(player);
				}
				return ItemInteractionResult.SUCCESS;
			});
		}

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}
}
