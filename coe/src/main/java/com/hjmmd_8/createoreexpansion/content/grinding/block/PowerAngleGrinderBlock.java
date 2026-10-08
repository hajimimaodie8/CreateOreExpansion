package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 动力角磨床：依靠机械动力（应力）运转的加工机器。
 *
 * <p>水平朝向（{@link HorizontalKineticBlock}，FACING 仅水平 4 向，随玩家放置）；
 * 水平旋转轴沿 FACING 轴，传动轴从 FACING 前方（齿轮箱纹理背板侧）接入；
 * 盖板在 FACING 对面一侧，开盖时朝那一侧翻起（那一格被方块阻挡则提示无法开盖，
 * 见 {@link #onWrenched}）。</p>
 *
 * <p><b>盖那一格的双向保护</b>（{@link #coverPos} 是这一格的唯一出处）：
 * ① 开盖前——那一格被不可替换方块挡住 ⇒ 拒绝开盖 + 提示（{@link #isCoverBlocked}，
 * <b>只豁免本模组自己的占位方块</b>）；
 * ② 开盖后——盖翻起后那一格原本是纯 AIR（盖是同一个方块的模型部件，不占邻格），
 * 但本批（COE 批 22）改成：开盖时<b>在那一格放一个真正的占位方块</b>
 * （{@link GrinderCoverPlaceholderBlock}，无碰撞、不可替换、{@code PushReaction.BLOCK}、
 * 挡流体、不掉落、无物品）。于是「不许放置」「活塞推不动」「流体进不来」三件事
 * <b>一次解决</b>，不再需要任何事件取消（批 21 的放置守卫已随之删除）。
 * 关盖时把占位方块移除，那一格照常可放。
 *
 * <p><b>占位方块的生命周期</b>（放/清的唯一出处 {@link #syncCoverPlaceholder}）：
 * 开盖 ⇒ {@link #placeCoverPlaceholder}；关盖 ⇒ {@link #clearCoverPlaceholder}；
 * 机器被拆掉 ⇒ {@link #onRemove} 清；机器还开着但占位方块被抹掉（爆炸 / {@code /setblock}）
 * ⇒ {@link PowerAngleGrinderBlockEntity#tick} 每一 tick 补回来；机器不存在了的孤儿
 * ⇒ {@link GrinderCoverPlaceholderBlockEntity} 自己删。</p>
 */
public class PowerAngleGrinderBlock extends HorizontalKineticBlock implements IBE<PowerAngleGrinderBlockEntity>, com.hjmmd_8.createoreexpansion.common.machine.MachineInteraction {

	/** 开盖状态：false=关盖，true=开盖（对应开盖模型 power_angle_grinder_rotated） */
	public static final BooleanProperty OPEN = BooleanProperty.create("open");

	/** 非全方块形状（x/z 内缩 0.5px，仿 Create 机器）。
	 * <p>碰撞形状若是全方块，AO 遮蔽判定（isCollisionShapeFullBlock）会把本方块当遮蔽物，
	 * 导致盖板等面在紧邻方块时整面发黑（开盖后更严重）。内缩后 isCollisionShapeFullBlock=false，
	 * 与 Create 机器（形状均非全方块）行为一致：不遮蔽自己、不遮蔽相邻方块。</p> */
	private static final VoxelShape SHAPE = box(0.5, 0, 0.5, 15.5, 16, 15.5);

	public PowerAngleGrinderBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(OPEN, false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
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
		return CoeBlockEntityTypes.POWER_ANGLE_GRINDER.get();
	}

	// ========== 扳手开盖/关盖 ==========

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();

		boolean open = state.getValue(OPEN);
		if (!open && isCoverBlocked(level, coverPos(pos, state))) {
			// 开盖方向（盖板在 FACING 对面一侧翻起）那一格有方块阻挡：不开盖，提示空间不足
			if (player != null) {
				player.displayClientMessage(
					Component.translatable("createoreexpansion.msg.cannot_open_cover"), true);
			}
			return InteractionResult.SUCCESS;
		}

		// 切换开盖状态
		BlockState toggled = state.cycle(OPEN);
		KineticBlockEntity.switchToBlockState(level, pos, toggled);
		// 开盖 ⇒ 在盖那一格放占位方块；关盖 ⇒ 移除它。
		// 顺序承重：必须在这一句<b>之后</b>放——占位方块自己的存活判据
		// （isOpenCoverCell）要求邻机此刻已经是 OPEN，先放会被判成孤儿。
		syncCoverPlaceholder(level, pos, toggled);
		if (!level.isClientSide) {
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * 机器被换成别的方块时（被挖掉 / 被爆炸清掉 / 被别的模组替换 / 被 {@code /setblock}），
	 * 把盖那一格的占位方块一起清掉，<b>绝不留孤儿</b>。
	 *
	 * <p><b>⚠ 为什么必须判 {@code !newState.is(this)} 而不能无条件清</b>：
	 * {@code LevelChunk#setBlockState:272-274} 在服务端<b>无条件</b>调用
	 * {@code blockstate.onRemove(...)}——只要状态真的变了，{@code cycle(OPEN)}
	 * 这种"同一个方块换状态"也走这里。若在这里无条件清占位方块，就会出现
	 * 「开盖刚放好、下一句 onRemove 又把它清掉」的自相矛盾（表现是那一格永远占不住）。</p>
	 *
	 * <p>跨区块：只有在盖那一格所在区块<b>已载入</b>时才动手（{@code level.getBlockState}
	 * 对未载入区块会同步载入它，在方块移除路径里强载邻区块是不可接受的）。
	 * 没载入就不清——那一格所在区块下次载入时，占位方块自己的方块实体会发现无人认领并自删。</p>
	 */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!newState.is(this)) {
			BlockPos cover = coverPos(pos, state);
			if (level.isLoaded(cover)) {
				clearCoverPlaceholder(level, cover);
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}

	/**
	 * <b>盖板那一格</b>（本仓唯一出处）：盖板在 FACING 对面一侧，所以它就是本机
	 * 朝 FACING 反方向相邻的那一格。
	 *
	 * <p>开盖前的空间检查（{@link #isCoverBlocked}）、占位方块的放/清
	 * （{@link #syncCoverPlaceholder}）与孤儿判据的参照点（{@link #isOpenCoverCell}）
	 * 读的都是这一格，必须共用这一个定义——两处各写一遍 {@code getOpposite()} 迟早会分家，
	 * 而分家的表现是静默的（开盖被拒却允许放置，或反过来）。</p>
	 */
	public static BlockPos coverPos(BlockPos pos, BlockState state) {
		return pos.relative(state.getValue(HORIZONTAL_FACING)
			.getOpposite());
	}

	/**
	 * <b>开盖前的空间检查</b>：盖板那一格是不是被别的方块挡住了（只读，不改世界）。
	 *
	 * <p><b>⚠ 本模组自己的占位方块是唯一的豁免</b>（COE 批 22）：
	 * 不豁免的话，「占位方块还在 + 盖的状态是 CLOSED」这一态会让 {@code !open &&
	 * isCoverBlocked(...)} 恒真 ⇒ <b>那一台角磨床此后永远开不了盖</b>。
	 * 而这一态是真实可出现的：跨区块自动存档的两半不保证同时落盘、{@code /setblock}、
	 * 备份还原、别的模组直写，任何一条都能造出来。</p>
	 *
	 * <p><b>豁免面严格等于"本模组这一个方块"</b>：不按 {@code isAir}、不按标签、
	 * 不按"可替换"去泛化——那会把别的方块也一起放行，正是这条守则要防的走样。</p>
	 */
	private static boolean isCoverBlocked(Level level, BlockPos coverPos) {
		BlockState front = level.getBlockState(coverPos);
		if (isCoverPlaceholder(front)) {
			return false;
		}
		return !front.isAir() && !front.canBeReplaced();
	}

	/** 这一格方块状态是不是本模组的角磨床盖侧占位方块（豁免判据的唯一出处）。 */
	public static boolean isCoverPlaceholder(BlockState state) {
		return state.is(CoeMachines.GRINDER_COVER_PLACEHOLDER.get());
	}

	/**
	 * <b>盖侧判据</b>（COE 批 22 沿用批 21 的口径）：{@code pos} 是不是某台<b>已开盖</b>
	 * 角磨床的盖板那一格。
	 *
	 * <p><b>为什么需要它</b>：占位方块（{@link GrinderCoverPlaceholderBlock}）自己要知道
	 * "还有没有人认领我"，放置提示也要知道"这一格是不是被开盖规则占着"。</p>
	 *
	 * <p><b>为什么是"沿 4 个水平方向找邻机"</b>：邻机若满足
	 * {@code HORIZONTAL_FACING == 该方向}，则它朝 FACING 反方向的那一格
	 * （{@link #coverPos}）正是 {@code pos}。FACING 是水平的
	 * （{@link HorizontalKineticBlock}），所以只看 {@link Direction.Plane#HORIZONTAL}：
	 * 既不会把上下两格算进来，也不会扩大到"机器周围一圈 / 一个盒子"。</p>
	 *
	 * <p><b>关盖不算</b>：条件里 {@link #OPEN} 是<b>正向</b>要求（{@code getValue(OPEN)}
	 * 为 true 才命中），所以 {@code OPEN == false} 的机器永远返回 false。</p>
	 *
	 * <p><b>⚠ 邻格没载入时返回 true（按"被认领"处理）——这一条是必须的，不是保守</b>：
	 * 本判据有两个调用方，其中一个是占位方块自己的方块实体 tick（每 tick 跑）。
	 * {@code Level#getBlockState} 对未载入区块会<b>同步载入</b>它
	 * （{@code Level#getBlockState} → {@code getChunkAt} → {@code ChunkStatus.FULL, true}），
	 * 在方块实体的 tick 里做这件事会把邻区块永久钉住。所以先问 {@link Level#isLoaded}，
	 * 只要有一个邻格没载入就直接返回 true（"判不了 ⇒ 当它是被认领的"）：
	 * <b>宁可漏清一次孤儿，也绝不在机器所在区块此刻没载入时把它的占位方块删掉。</b>
	 * 放走的那一次由"邻区块载入后本判据给出真答案"和"角磨床自己的 tick 会补回占位方块"
	 * 两头收敛。提示那条调用方永远不会落到这一支：玩家点得到的那一格，邻格必然已载入。</p>
	 *
	 * @param level 世界（必须是 {@link Level}：要读 {@link Level#isLoaded}）
	 * @param pos   待判定的那一格
	 * @return 是"已开盖机器的盖侧那一格"（或邻格未载入、判不了）返回 true
	 */
	public static boolean isOpenCoverCell(Level level, BlockPos pos) {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockPos machinePos = pos.relative(facing);
			if (!level.isLoaded(machinePos)) {
				// 邻格没载入 ⇒ 判不了（也绝不能因此去载入它）⇒ 按被认领处理
				return true;
			}
			BlockState neighbour = level.getBlockState(machinePos);
			if (neighbour.getBlock() instanceof PowerAngleGrinderBlock
				&& neighbour.getValue(OPEN)
				&& neighbour.getValue(HORIZONTAL_FACING) == facing) {
				return true;
			}
		}
		return false;
	}

	// ========== 占位方块的放 / 清（生命周期唯一出处） ==========

	/**
	 * 把"盖那一格的占位方块"与盖的开合状态对齐（<b>幂等</b>；服务端才写世界）。
	 *
	 * <p>开盖 ⇒ {@link #placeCoverPlaceholder}；关盖 ⇒ {@link #clearCoverPlaceholder}。
	 * 调用点：{@link #onWrenched}（扳手切换时）与
	 * {@link PowerAngleGrinderBlockEntity#tick}（每 tick 复核，把被炸掉/被抹掉的补回来）。</p>
	 *
	 * <p>跨区块守卫：盖那一格所在区块没载入时一个字都不写（{@code Level#setBlock}
	 * 对未载入区块会同步载入它）。没补上的那一次由"区块载入后角磨床自己的 tick"收敛。</p>
	 */
	public static void syncCoverPlaceholder(Level level, BlockPos pos, BlockState state) {
		if (level.isClientSide) {
			return;
		}
		BlockPos cover = coverPos(pos, state);
		if (!level.isLoaded(cover)) {
			return;
		}
		if (state.getValue(OPEN)) {
			placeCoverPlaceholder(level, cover);
		} else {
			clearCoverPlaceholder(level, cover);
		}
	}

	/**
	 * 在盖那一格放占位方块（幂等）。
	 *
	 * <p><b>只在"空位或可替换"时占</b>（与 {@link #isCoverBlocked} 同一条判据的放宽形式）：
	 * 那一格要是已经站着别的方块（玩家用 {@code /setblock} 塞的、外部工具改的），
	 * <b>一个字都不动</b>——不抢、不毁玩家的东西。此后那台机器开盖会被正常拒绝，
	 * 提示语就是既有的 {@code cannot_open_cover}。</p>
	 */
	public static void placeCoverPlaceholder(Level level, BlockPos cover) {
		BlockState existing = level.getBlockState(cover);
		if (isCoverPlaceholder(existing)) {
			return;
		}
		if (!existing.isAir() && !existing.canBeReplaced()) {
			return;
		}
		level.setBlock(cover, CoeMachines.GRINDER_COVER_PLACEHOLDER.getDefaultState(), Block.UPDATE_ALL);
	}

	/**
	 * 清掉盖那一格的占位方块（幂等；只清"那一格确实是本模组占位方块"的情况，
	 * 绝不动别的方块）。
	 *
	 * <p>用 {@code Level#removeBlock(pos, false)} 而不是 {@code destroyBlock} ⇒ 不产生任何掉落
	 * （方块本身还有 {@code noLootTable()}，双保险）。</p>
	 */
	public static void clearCoverPlaceholder(Level level, BlockPos cover) {
		if (isCoverPlaceholder(level.getBlockState(cover))) {
			level.removeBlock(cover, false);
		}
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

		// 开盖：空手右键优先取物品（成品 → 原料，分批批量，同合盖逻辑）；
		// 机器内无物品时取角磨轮；手持物品放入机器（手动加工）
		if (open) {
			return onBlockEntityUseItemOn(level, pos, be -> {
				if (stack.isEmpty()) {
					// 有物品：优先取出（成品区 → 原料，分批）
					if (!be.inventory.isEmpty()) {
						if (!level.isClientSide && player != null) {
							be.retrieveAll(player);
						}
						return ItemInteractionResult.SUCCESS;
					}
					// 无物品：取出角磨轮
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
				// 手持物品：放入机器（合盖后自动加工）；槽 0 已有物品时放入失败，物品保留在玩家手中
				if (!level.isClientSide && player != null) {
					if (be.insertItem(stack.copy()) && !player.isCreative()) {
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

	// ========== 光照规则（仿 Create 机器：动力锯/冲压机等不挡光） ==========

	/**
	 * 天空光可无衰减穿透本方块（仿 Create 机器）。
	 *
	 * <p>Create 的冲压机/动力锯/创造马达形状均为非全方块，故默认实现的
	 * {@code !isShapeFullBlock(getShape())} 为 true，天空光直穿、下方方块不因
	 * 机器压顶而变暗。本类模型是近全方块形状，直接返回 true 复刻该行为。</p>
	 */
	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	/**
	 * 自身面不做环境光遮蔽变暗（仿 Create 机器）。
	 *
	 * <p>Create 机器碰撞形状非全方块，{@code getShadeBrightness} 默认实现返回
	 * 1.0（面不发暗）；近全方块形状会返回 0.2。此处直接返回 1.0 保持机器观感。</p>
	 */
	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}
	/**
	 * 本机有特殊切换模式 ⇒ 扳手只做这一件事（切模式），不参与 Ctrl+扳手旋转。
	 * 见 {@link com.hjmmd_8.createoreexpansion.common.machine.MachineInteraction} 的统一交互规则 ③。
	 */
	@Override
	public boolean hasModeSwitch() {
		return true;
	}

}
