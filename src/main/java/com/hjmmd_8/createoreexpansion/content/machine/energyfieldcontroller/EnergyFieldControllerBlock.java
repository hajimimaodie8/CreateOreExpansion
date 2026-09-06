package com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFieldType;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;

/**
 * 能量场控制器（Energy Field Controller）：六向应力机器（可水平/竖直放置）。
 *
 * <p><b>开盖才配对产场</b>：机器有一块可开/关的能量接收盖（FACING 正面的面板，
 * 开盖贴图 = {@code energy_field_controller_receiver_open}，关盖 = {@code ..._close}）：
 * <ul>
 *   <li><b>空手右键</b> → 开盖 / 关盖（{@link #OPEN}）；盖关闭时<b>不参与配对、不产场</b>；</li>
 *   <li><b>扳手右键</b>（{@link IWrenchable}）→ 切换能量场类型：<b>加速场 ↔ 偏转场</b>
 *       （{@link EnergyFieldType}），并在快捷栏上方提示当前类型；</li>
 *   <li>接口极性、当前场强档位可通过<b>护目镜</b>查看（BE 逻辑）。</li>
 * </ul></p>
 *
 * <p><b>传动轴口</b>：传动轴从 FACING 的<b>反面（底部）</b>接入（与充能器同约定，
 * {@code hasShaftTowards} 只认 FACING 反面），放置自动对轴。</p>
 *
 * <p><b>场的产生（配对/极性/机壳扩展规则）</b>见 {@link EnergyFieldControllerBlockEntity} 类注释，
 * 本类只负责放置朝向、轴口、盖开关、类型切换与方块实体绑定。</p>
 */
public class EnergyFieldControllerBlock extends DirectionalKineticBlock
	implements IBE<EnergyFieldControllerBlockEntity>, IWrenchable {

	/** 接收盖开关：false=关盖（不产场）、true=开盖（可配对产场）。 */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty OPEN =
		net.minecraft.world.level.block.state.properties.BooleanProperty.create("open");

	/** 非全方块形状（xz 内缩 0.5px，仿 Create 机器避免 AO 遮蔽发黑）。 */
	private static final VoxelShape SHAPE = box(0.5, 0, 0.5, 15.5, 16, 15.5);

	public EnergyFieldControllerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(OPEN);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 自动对轴：getPreferredFacing 返回相邻传动轴所在侧；本机轴口在 FACING 反面，
		// 故 FACING 取 preferred 的反方向，让轴口自动扣住相邻传动轴。
		Direction preferred = getPreferredFacing(context);
		if ((context.getPlayer() != null && context.getPlayer()
			.isShiftKeyDown()) || preferred == null)
			return super.getStateForPlacement(context);
		return defaultBlockState().setValue(FACING, preferred.getOpposite());
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING)
			.getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		// 传动轴从 FACING 对面（底部）接入
		return face == state.getValue(FACING)
			.getOpposite();
	}

	/**
	 * 空手右键：<b>只点中"口面"（FACING 正面）时</b>开盖 / 关盖
	 * （盖关闭时不参与配对产场）。点传动轴面/侧面不响应。
	 */
	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
		net.minecraft.world.phys.BlockHitResult hitResult) {
		// 只有点中口面（FACING 正面）才开关盖 —— 传动轴面/侧面空手点击不产生任何动作
		if (hitResult.getDirection() != state.getValue(FACING))
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		boolean open = state.getValue(OPEN);
		com.simibubi.create.content.kinetics.base.KineticBlockEntity.switchToBlockState(level, pos,
			state.setValue(OPEN, !open));
		// 开盖/关盖音效（复用扳手转动音：金属机械感）
		AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		player.displayClientMessage(Component.translatable(open
			? "createoreexpansion.field_controller.lid_closed"
			: "createoreexpansion.field_controller.lid_opened"), true);
		return InteractionResult.SUCCESS;
	}

	/**
	 * 手持右键兜底：Create 扳手（非潜行）→ 切换场类型（IWrenchable 分发若被
	 * {@link WrenchItem} 先截走，此处不会被执行——两条路径互斥，不会重复切换）。
	 * 其余物品/潜行扳手 → 放行给默认逻辑（拆机/放置方块等）。
	 */
	@Override
	public net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
		BlockState state, Level level, BlockPos pos, Player player,
		net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
		if (player.isShiftKeyDown() || !player.mayBuild() || !com.simibubi.create.AllItems.WRENCH.isIn(stack))
			return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (level.isClientSide)
			return net.minecraft.world.ItemInteractionResult.SUCCESS;
		toggleFieldType(state, level, pos, player);
		return net.minecraft.world.ItemInteractionResult.SUCCESS;
	}

	/** 扳手右键：切换能量场类型（加速场 ↔ 偏转场），快捷栏上方提示。 */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		EnergyFieldControllerBlockEntity be = getBlockEntity(level, pos);
		if (be == null)
			return InteractionResult.PASS;
		// 切换类型存在 BE（FieldType NBT），无需改动 blockstate —— 状态同步由 notifyUpdate 完成
		toggleFieldType(state, level, pos, context.getPlayer());
		return InteractionResult.SUCCESS;
	}

	/** 切类型共用逻辑：类型存在 BE，本地预测 + 服务端音效/快捷栏提示。 */
	private void toggleFieldType(BlockState state, Level level, BlockPos pos, Player player) {
		EnergyFieldControllerBlockEntity be = getBlockEntity(level, pos);
		if (be == null)
			return;
		EnergyFieldType type = be.cycleFieldType();
		if (!level.isClientSide) {
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
			if (player != null) {
				player.displayClientMessage(Component.translatable(type == EnergyFieldType.ACCELERATION
					? "createoreexpansion.field_controller.type_accel"
					: "createoreexpansion.field_controller.type_deflect"), true);
			}
		}
	}

	@Override
	public Class<EnergyFieldControllerBlockEntity> getBlockEntityClass() {
		return EnergyFieldControllerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends EnergyFieldControllerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENERGY_FIELD_CONTROLLER.get();
	}

	// ========== 光照规则（仿 Create 机器，同充能器） ==========

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}
}
