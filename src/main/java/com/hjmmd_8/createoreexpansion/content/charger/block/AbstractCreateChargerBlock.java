package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 应力充能器抽象基类：六向应力机器（可水平/竖直放置）。
 *
 * <p>子类（翡翠充能器/雷鸣充能器）只需实现 {@link IBE} 的方块实体绑定即可复用
 * 放置朝向（优先对准相邻传动轴）、MODE 展示属性、传动轴接入方向等全部通用行为。</p>
 *
 * <p>光照规则（仿 Create 机器，如动力锯/冲压机）：机器方块不阻挡光照——
 * 天空光可无衰减穿透（{@link #propagatesSkylightDown} = true），自身面不发暗
 * （{@link #getShadeBrightness} = 1.0）。配合注册时的 noOcclusion()，
 * getLightBlock 自动为 0，与 Create 机器光照行为完全一致。
 * 另：{@link #getShape} 返回非全方块形状，避免 AO 遮蔽判定把本机器当遮蔽物导致面发黑。</p>
 */
public abstract class AbstractCreateChargerBlock extends DirectionalKineticBlock {

	/** 模式：0=未接入应力（展示），1/2/3=蓄力阶段（低/高/伽马） */
	public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 3);

	/** 非全方块形状（x/z 内缩 0.5px，仿 Create 机器）。
	 * <p>碰撞形状若是全方块，AO 遮蔽判定（isCollisionShapeFullBlock）会把本方块当遮蔽物，
	 * 导致机器面在紧邻方块时整面发黑。内缩后 isCollisionShapeFullBlock=false，行为与 Create 机器一致。</p> */
	private static final VoxelShape SHAPE = box(0.5, 0, 0.5, 15.5, 16, 15.5);

	protected AbstractCreateChargerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(MODE, 0));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(MODE);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 仿创造马达：优先对准相邻传动轴方向放置
		Direction preferred = getPreferredFacing(context);
		if ((context.getPlayer() != null && context.getPlayer()
			.isShiftKeyDown()) || preferred == null)
			return super.getStateForPlacement(context);
		return defaultBlockState().setValue(FACING, preferred);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING)
			.getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		// 传动轴从 FACING 对面（底端）接入，与发射头（FACING 方向）同一轴线的另一端
		return face == state.getValue(FACING)
			.getOpposite();
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
}
