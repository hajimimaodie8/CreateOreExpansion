package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 六面能量波差器（Six-Face Energy Wave Disperser）：无朝向固定机器，6 个面（上/下/北/东/南/西）
 * 全部为能量接收面板（{@code wave_receiver_close}/{@code wave_receiver_open}），可独立开关。
 *
 * <p><b>与四面差波器差异</b>：无 FACING 属性、6 面全部可开口（模型固定不旋转），
 * 开口/反弹/传播/分裂逻辑一致；<b>新增规则</b>：当 5 或 6 个面已开口时，
 * 波传入后分裂为<b>等级降二</b>的子波从剩余开口传出（3-4 开口仍为降一级均摊）。
 * 判定逻辑见 {@code SixFaceDispersal}。</p>
 *
 * <p><b>渲染</b>：与四面差波器同款——64 个 blockstate 变体模型（6 面 open 组合），
 * 每面整面切换 close/open 纹理，无需方块实体/渲染器。</p>
 */
public class SixFaceDisperserBlock extends Block implements IWrenchable, IBE<SixFaceDisperserBlockEntity> {

	/** 上面开口：false=close（关闭）、true=open（打开） */
	public static final BooleanProperty UP = BooleanProperty.create("open_up");
	/** 下面开口 */
	public static final BooleanProperty DOWN = BooleanProperty.create("open_down");
	/** 北面开口 */
	public static final BooleanProperty NORTH = BooleanProperty.create("open_north");
	/** 东面开口 */
	public static final BooleanProperty EAST = BooleanProperty.create("open_east");
	/** 南面开口 */
	public static final BooleanProperty SOUTH = BooleanProperty.create("open_south");
	/** 西面开口 */
	public static final BooleanProperty WEST = BooleanProperty.create("open_west");

	/** 满格碰撞/选中框（与模型一致） */
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

	public SixFaceDisperserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(UP, false)
			.setValue(DOWN, false)
			.setValue(NORTH, false)
			.setValue(EAST, false)
			.setValue(SOUTH, false)
			.setValue(WEST, false));
	}

	@Override
	public Class<SixFaceDisperserBlockEntity> getBlockEntityClass() {
		return SixFaceDisperserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SixFaceDisperserBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SIX_FACE_DISPERSER.get();
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return simpleCodec(SixFaceDisperserBlock::new);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 无朝向：所有面初始关闭
		return defaultBlockState();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// ========== 扳手交互：每面 5 区域（中心控制本面，4 个梯形控制相邻 4 面） ==========

	/**
	 * 扳手右键点击某面：按点击位置在该面内的偏移划分 5 个区域——
	 * <ul>
	 *   <li><b>中心区域</b>：切换该面本身的开/关；</li>
	 *   <li><b>上/下/左/右 4 个等腰梯形</b>（对角线划分）：切换与该面相邻的对应方向的面
	 *       （上→该面上方的面，右→右方，左→左方，下→下方）。</li>
	 * </ul>
	 */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction clicked = context.getClickedFace();
		Vec3 hit = context.getClickLocation();
		Vec3 rel = hit.subtract(Vec3.atCenterOf(pos));

		// 面内坐标：u = 面内横向偏移、v = 面内纵向偏移（以面中心为原点，-0.5..0.5）
		Direction upDir = upOf(clicked);
		Direction rightDir = rightOf(clicked, upDir);
		double u = rel.dot(Vec3.atLowerCornerOf(rightDir.getNormal()));
		double v = rel.dot(Vec3.atLowerCornerOf(upDir.getNormal()));

		Direction target;
		double threshold = 0.18; // 中心区域半宽（约 3/16 格）
		if (Math.abs(u) < threshold && Math.abs(v) < threshold) {
			target = clicked; // 中心：控制本面
		} else if (Math.abs(u) > Math.abs(v)) {
			target = u > 0 ? rightDir : rightDir.getOpposite(); // 右/左梯形
		} else {
			target = v > 0 ? upDir : upDir.getOpposite(); // 上/下梯形
		}

		BooleanProperty property = propertyFor(target);
		boolean open = state.getValue(property);
		// 状态修改只在服务端执行（客户端由同步包更新，避免多人下双端不一致）
		if (!level.isClientSide) {
			level.setBlock(pos, state.setValue(property, !open), Block.UPDATE_CLIENTS);
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		}
		return InteractionResult.SUCCESS;
	}

	/** 该面的"上方向"（面内纵向正方向）：水平面为上 UP；顶面/底面约定为北。 */
	public static Direction upOf(Direction face) {
		return switch (face) {
			case NORTH, EAST, SOUTH, WEST -> Direction.UP;
			default -> Direction.NORTH; // UP/DOWN 面：约定上=北
		};
	}

	/** 该面的"右方向"（面内横向正方向）：从该面视角的右手边。 */
	public static Direction rightOf(Direction face, Direction up) {
		return switch (face) {
			case NORTH -> Direction.EAST;   // 看北面：右=东
			case SOUTH -> Direction.WEST;   // 看南面：右=西
			case EAST -> Direction.SOUTH;   // 看东面：右=南
			case WEST -> Direction.NORTH;   // 看西面：右=北
			case UP -> Direction.EAST;      // 看顶面（约定上=北）：右=东
			case DOWN -> Direction.WEST;    // 看底面（约定上=北）：右=西
		};
	}

	/** 世界方向 → 对应开口属性（6 面皆有）。 */
	public static BooleanProperty propertyFor(Direction side) {
		return switch (side) {
			case UP -> UP;
			case DOWN -> DOWN;
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
		};
	}

	/** 查询某世界方向的开口是否开启（供波实体判定）。 */
	public static boolean isOpen(BlockState state, Direction side) {
		return state.getValue(propertyFor(side));
	}
}
