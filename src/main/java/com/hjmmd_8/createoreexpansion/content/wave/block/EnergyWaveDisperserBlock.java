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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 能量波差器（Energy Wave Disperser）：无应力被动机器，将进入的能量波向多个开口均摊分发。
 *
 * <p><b>模型</b>：上下两面翡翠机壳（{@code jade_casing}），4 个侧面为能量接收面
 * （{@code wave_receiver_close}/{@code wave_receiver_open}），由 blockstate 旋转到六向朝向。</p>
 *
 * <p><b>运行逻辑</b>（4 个侧面开口可独立开关，见 {@link #NORTH}/{@link #EAST}/{@link #SOUTH}/{@link #WEST}）：</p>
 * <ul>
 *   <li><b>1 个开口</b>：波进入后原路遣返，等级不变（同调级器无应力单开口）；</li>
 *   <li><b>2 个开口</b>：波从一开口进、另一开口出，等级不变（方向无关）；</li>
 *   <li><b>3 或 4 个开口</b>：波从入口进，其余每个开口各发一个<b>降一级</b>的波
 *       （均摊分发）；1 级波进入因降级无路可降而湮灭。</li>
 *   <li>波撞向关闭的开口/机壳面 → 如撞墙消失。</li>
 * </ul>
 *
 * <p><b>扳手交互</b>：点击 4 个侧面切换该侧开/关；点击机壳面（顶/底）旋转朝向。
 * 实现 {@link IWrenchable}，与 Create 机器交互一致。</p>
 *
 * <p><b>方块实体</b>：实现 {@link IBE} 绑定 {@link EnergyWaveDisperserBlockEntity}——
 * 纯静态无数据，仅承载客户端渲染器（按 4 侧开闭状态在顶/底灯盘叠加灯位）。</p>
 */
public class EnergyWaveDisperserBlock extends Block implements IWrenchable, IBE<EnergyWaveDisperserBlockEntity> {

	/** 六向朝向（同 BlockStateProperties.FACING，与 Create DirectionalKineticBlock.FACING 同一实例） */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/** 北侧开口：false=close（关闭）、true=open（打开） */
	public static final BooleanProperty NORTH = BooleanProperty.create("open_north");
	/** 东侧开口 */
	public static final BooleanProperty EAST = BooleanProperty.create("open_east");
	/** 南侧开口 */
	public static final BooleanProperty SOUTH = BooleanProperty.create("open_south");
	/** 西侧开口 */
	public static final BooleanProperty WEST = BooleanProperty.create("open_west");

	/** 满格碰撞/选中框（与模型一致） */
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

	public EnergyWaveDisperserBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(FACING, Direction.UP)
			.setValue(NORTH, false)
			.setValue(EAST, false)
			.setValue(SOUTH, false)
			.setValue(WEST, false));
	}

	@Override
	public Class<EnergyWaveDisperserBlockEntity> getBlockEntityClass() {
		return EnergyWaveDisperserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends EnergyWaveDisperserBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENERGY_WAVE_DISPERSER.get();
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return simpleCodec(EnergyWaveDisperserBlock::new);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, NORTH, EAST, SOUTH, WEST);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 朝玩家视线放置（六向），与调级器一致
		return defaultBlockState()
			.setValue(FACING, context.getNearestLookingDirection()
				.getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// ========== 扳手交互：4 侧面切换开/关；机壳面（顶/底）分区点击切换对应侧面 ==========

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction clicked = context.getClickedFace();
		Direction facing = state.getValue(FACING);
		Direction modelSide = modelFaceOf(facing, clicked);

		// 4 个侧面（模型 north/east/south/west）：切换该侧开口开关（世界方向先映射到模型面）
		if (modelSide != null) {
			boolean open = state.getValue(propertyFor(modelSide));
			// 状态修改只在服务端执行（客户端由同步包更新，避免多人下双端不一致）
			if (!level.isClientSide) {
				level.setBlock(pos, state.setValue(propertyFor(modelSide), !open), Block.UPDATE_CLIENTS);
				AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
			}
			return InteractionResult.SUCCESS;
		}

		// 机壳面（顶/底）：按点击位置沿对角线划分 4 区域（上北下南、左西右东），
		// 切换对应模型侧面的开口——机壳面永不紧贴，可随时点选任一侧面（解决紧贴面无法开口）。
		Direction target = sideForShellClick(state, context);
		if (target != null) {
			boolean open = state.getValue(propertyFor(target));
			// 状态修改只在服务端执行（客户端由同步包更新）
			if (!level.isClientSide) {
				level.setBlock(pos, state.setValue(propertyFor(target), !open), Block.UPDATE_CLIENTS);
				AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
			}
			return InteractionResult.SUCCESS;
		}

		// 理论上到不了（机壳面必有分区命中）
		return InteractionResult.SUCCESS;
	}

	/**
	 * 机壳面分区点击：以方块中心为原点，点击位置沿两条对角线分成 4 个区域，
	 * 对应模型 4 侧面（上=模型北、下=模型南、左=模型西、右=模型东）。
	 * <p>判断基于<b>世界坐标偏移</b>，命中区域先得世界方向，再经
	 * {@link #modelFaceOf} 映射到模型面（与灯材质位序一致）。</p>
	 *
	 * @return 命中的模型侧面；无法判定时返回 null
	 */
	private Direction sideForShellClick(BlockState state, UseOnContext context) {
		Vec3 hit = context.getClickLocation();
		Vec3 rel = hit.subtract(Vec3.atCenterOf(context.getClickedPos()));
		Direction facing = state.getValue(FACING);

		Direction worldDir;
		if (facing.getAxis() == Direction.Axis.Y) {
			// 机壳面朝上/下：用 x/z 偏移，上北（-Z）下南（+Z）左西（-X）右东（+X）
			if (Math.abs(rel.x) > Math.abs(rel.z))
				worldDir = rel.x > 0 ? Direction.EAST : Direction.WEST;
			else
				worldDir = rel.z > 0 ? Direction.SOUTH : Direction.NORTH;
		} else if (facing.getAxis() == Direction.Axis.X) {
			// 机壳朝东西（躺倒），模型 up 面朝东西：分区落在 y/z 平面
			if (Math.abs(rel.y) > Math.abs(rel.z))
				worldDir = rel.y > 0 ? Direction.UP : Direction.DOWN;
			else
				worldDir = rel.z > 0 ? Direction.SOUTH : Direction.NORTH;
		} else {
			// 机壳朝南北，分区落在 x/y 平面
			if (Math.abs(rel.y) > Math.abs(rel.x))
				worldDir = rel.y > 0 ? Direction.UP : Direction.DOWN;
			else
				worldDir = rel.x > 0 ? Direction.EAST : Direction.WEST;
		}
		// 世界方向 → 模型面（该世界方向的侧面开口）
		return modelFaceOf(facing, worldDir);
	}

	public static BooleanProperty propertyFor(Direction modelSide) {
		return switch (modelSide) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> null;
		};
	}

	/**
	 * 将模型侧面映射为世界方向。
	 * <p>由 blockstate 旋转（rotationX/rotationY）推导并用 JOML 实测校准的表——
	 * 等效旋转 q = rotationY(toYRot) * rotationX(-270)（水平时），UP 恒等、DOWN 绕 X 180°。
	 * 已用游戏实测验证（FACING=NORTH 时模型 E 面朝世界 WEST 等）：</p>
	 * <pre>
	 * FACING   模型N      模型E      模型S      模型W
	 * UP      NORTH     EAST      SOUTH     WEST
	 * DOWN    SOUTH     EAST      NORTH     WEST
	 * NORTH   UP        WEST      DOWN      EAST
	 * SOUTH   UP        EAST      DOWN      WEST
	 * EAST    UP        NORTH     DOWN      SOUTH
	 * WEST    UP        SOUTH     DOWN      NORTH
	 * </pre>
	 *
	 * @param facing    差器机壳朝向（FACING）
	 * @param modelSide 模型侧面（NORTH/EAST/SOUTH/WEST）
	 * @return 世界方向；modelSide 为机壳方向（顶/底）时返回 null
	 */
	public static Direction worldDirOf(Direction facing, Direction modelSide) {
		return switch (facing) {
			case UP -> switch (modelSide) {
				case NORTH -> Direction.NORTH;
				case EAST -> Direction.EAST;
				case SOUTH -> Direction.SOUTH;
				case WEST -> Direction.WEST;
				default -> null;
			};
			case DOWN -> switch (modelSide) {
				case NORTH -> Direction.SOUTH;
				case EAST -> Direction.EAST;
				case SOUTH -> Direction.NORTH;
				case WEST -> Direction.WEST;
				default -> null;
			};
			case NORTH -> switch (modelSide) {
				case NORTH -> Direction.UP;
				case EAST -> Direction.WEST;
				case SOUTH -> Direction.DOWN;
				case WEST -> Direction.EAST;
				default -> null;
			};
			case SOUTH -> switch (modelSide) {
				case NORTH -> Direction.UP;
				case EAST -> Direction.EAST;
				case SOUTH -> Direction.DOWN;
				case WEST -> Direction.WEST;
				default -> null;
			};
			case EAST -> switch (modelSide) {
				case NORTH -> Direction.UP;
				case EAST -> Direction.NORTH;
				case SOUTH -> Direction.DOWN;
				case WEST -> Direction.SOUTH;
				default -> null;
			};
			case WEST -> switch (modelSide) {
				case NORTH -> Direction.UP;
				case EAST -> Direction.SOUTH;
				case SOUTH -> Direction.DOWN;
				case WEST -> Direction.NORTH;
				default -> null;
			};
		};
	}

	/**
	 * 将世界方向映射为模型侧面（{@link #worldDirOf} 的逆，查表实现）。
	 *
	 * @param facing   差器机壳朝向（FACING）
	 * @param worldDir 波/扳手的世界方向
	 * @return 对应模型侧面（NORTH/EAST/SOUTH/WEST）；若是机壳方向（顶/底）返回 null
	 */
	public static Direction modelFaceOf(Direction facing, Direction worldDir) {
		for (Direction side : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
			if (worldDirOf(facing, side) == worldDir)
				return side;
		}
		return null; // 机壳方向（模型顶/底）
	}

	/** 查询某方向的侧面开口是否开启（供波实体判定）。 */
	public static boolean isOpen(BlockState state, Direction side) {
		return switch (side) {
			case NORTH -> state.getValue(NORTH);
			case EAST -> state.getValue(EAST);
			case SOUTH -> state.getValue(SOUTH);
			case WEST -> state.getValue(WEST);
			default -> false; // 机壳面（顶/底）无开口
		};
	}
}
