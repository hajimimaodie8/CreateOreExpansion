package com.hjmmd_8.createoreexpansion.content.wave.block;

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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 八面能量波差器（Octa Energy Wave Disperser）：无朝向固定机器。
 *
 * <p>8 个接收方向可独立开关：
 * <ul>
 *   <li><b>4 个正交面</b>（北/东/南/西）：对应模型侧面<b>中心 6×16</b> 区域
 *       （贴图 {@code sapphire_wave_differencer_small_side_open/close}）；</li>
 *   <li><b>4 个 45° 斜向</b>（东北/西北/东南/西南）：对应模型<b>斜板</b>，
 *       波从某正交面<b>两侧各 5×16</b> 区域进入时命中（贴图
 *       {@code sapphire_wave_differencer_big_side_open/close}）。</li>
 * </ul>
 *
 * <p>开口数量语义：1 开口=反弹、2 开口=对穿通道、3~4=分裂降 1 级、
 * 5~6=分裂降 2 级、7~8=分裂降 3 级（判定见 OctaEnergyWaveDispersal）。</p>
 *
 * <p>渲染：256 个 blockstate 变体模型（8 面 open 组合，每面整面 close/open 纹理）；
 * 顶/底面（{@code octa_energy_wave_difference} + {@code special_light} 灯位）
 * 由 blockstate 顶底贴图承载。碰撞/选中框为整方块。</p>
 *
 * <p><b>扳手交互</b>：侧面点击按 6px 中心 / 两侧 5px 细分选择正交面或相邻斜面；
 * 顶/底面点击按 45° 8 扇区（第 1 扇 -22.5°~22.5° 指向北，顺时针递增）选择方向开关。</p>
 */
public class OctaEnergyWaveDifferencerBlock extends Block implements IWrenchable, IBE<OctaEnergyWaveDifferencerBlockEntity> {

	@Override
	public Class<OctaEnergyWaveDifferencerBlockEntity> getBlockEntityClass() {
		return OctaEnergyWaveDifferencerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends OctaEnergyWaveDifferencerBlockEntity> getBlockEntityType() {
		return com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes.OCTA_ENERGY_WAVE_DIFFERENCER.get();
	}

	/** 北面开口（正交，模型侧面中心 6×16） */
	public static final BooleanProperty NORTH = BooleanProperty.create("open_north");
	/** 东面开口 */
	public static final BooleanProperty EAST = BooleanProperty.create("open_east");
	/** 南面开口 */
	public static final BooleanProperty SOUTH = BooleanProperty.create("open_south");
	/** 西面开口 */
	public static final BooleanProperty WEST = BooleanProperty.create("open_west");
	/** 东北斜向开口（45° 斜板） */
	public static final BooleanProperty NORTH_EAST = BooleanProperty.create("open_ne");
	/** 西北斜向开口 */
	public static final BooleanProperty NORTH_WEST = BooleanProperty.create("open_nw");
	/** 东南斜向开口 */
	public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("open_se");
	/** 西南斜向开口 */
	public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("open_sw");

	/** 放置姿态（轴向）：X/Y/Z 三种，无正反 —— 方块自身坐标系（斜板/扇区）随姿态整体旋转 */
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	/** 满格碰撞/选中框（用户要求：正常方形） */
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

	public OctaEnergyWaveDifferencerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(AXIS, Direction.Axis.Y)
			.setValue(NORTH, false)
			.setValue(EAST, false)
			.setValue(SOUTH, false)
			.setValue(WEST, false)
			.setValue(NORTH_EAST, false)
			.setValue(NORTH_WEST, false)
			.setValue(SOUTH_EAST, false)
			.setValue(SOUTH_WEST, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return simpleCodec(OctaEnergyWaveDifferencerBlock::new);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, NORTH, EAST, SOUTH, WEST, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 3 姿态：轴向 = 点击面所在轴（X/Y/Z，无正反）；所有开口初始关闭
		return defaultBlockState().setValue(AXIS, context.getClickedFace()
			.getAxis());
	}

	/**
	 * 姿态旋转（供世界↔本地换算与渲染对齐）：base 模型 = 站姿（axis=Y）。
	 * axis=Y 原样；axis=X/Z 分别按对应旋转矩阵整体翻转，保证"本地北"总是渲染后朝北的面。
	 */
	public static net.minecraft.world.level.block.Rotation rotationOf(Direction.Axis axis) {
		return switch (axis) {
			case X -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
			case Z -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
			default -> net.minecraft.world.level.block.Rotation.NONE;
		};
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// ========== 属性查询 ==========

	/** 正交世界方向（北/东/南/西）→ 开口属性。 */
	public static BooleanProperty propertyFor(Direction side) {
		return switch (side) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> throw new IllegalArgumentException("八面差波器正交面仅支持水平方向: " + side);
		};
	}

	/** 斜向枚举 → 开口属性。 */
	public static BooleanProperty propertyFor(OctaCorner corner) {
		return switch (corner) {
			case NORTH_EAST -> NORTH_EAST;
			case NORTH_WEST -> NORTH_WEST;
			case SOUTH_EAST -> SOUTH_EAST;
			case SOUTH_WEST -> SOUTH_WEST;
		};
	}

	/** 查询正交方向开口是否开启（供波实体判定）。 */
	public static boolean isOpen(BlockState state, Direction side) {
		return state.getValue(propertyFor(side));
	}

	/** 查询斜向开口是否开启。 */
	public static boolean isOpen(BlockState state, OctaCorner corner) {
		return state.getValue(propertyFor(corner));
	}

	/** 统计当前开口数（0~8）。 */
	public static int openCount(BlockState state) {
		int count = 0;
		for (BooleanProperty p : new BooleanProperty[] { NORTH, EAST, SOUTH, WEST,
			NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST }) {
			if (state.getValue(p))
				count++;
		}
		return count;
	}

	// ========== 扳手交互：侧面 6px 中心/两侧 5px + 顶/底 45° 8 扇区 ==========

	/**
	 * 解析本地坐标系下的命中点要切换的开口属性（无副作用；供主世界
	 * {@code onWrenched} 与 contraption/结构移动交互共用同一套判定）。
	 *
	 * @param clicked 命中的<b>本地</b>面（站姿语义：顶/底=UP/DOWN，侧面=N/E/S/W）
	 * @param rel     命中点相对方块中心的偏移（本地坐标，±0.5）
	 * @return 要切换的开口属性（恒非 null：顶/底与侧面都能映射到 8 口中一个）
	 */
	public static BooleanProperty resolveTarget(Direction clicked, Vec3 rel) {
		if (clicked.getAxis()
			.isVertical()) {
			// 顶/底面：45° 8 扇区。第 1 扇 -22.5°~22.5° 指向本地北(N)，顺时针 N→NE→E→SE→S→SW→W→NW
			double angle = Math.toDegrees(Math.atan2(rel.x, -rel.z));
			int idx = (int) Math.floor(((angle + 22.5 + 360.0) % 360.0) / 45.0);
			return switch (idx) {
				case 1 -> NORTH_EAST;
				case 2 -> EAST;
				case 3 -> SOUTH_EAST;
				case 4 -> SOUTH;
				case 5 -> SOUTH_WEST;
				case 6 -> WEST;
				case 7 -> NORTH_WEST;
				default -> NORTH;
			};
		}
		// 水平正交面（北/东/南/西）：面内横向 u（右为正，±0.5）——
		// 中心 6px(|u|≤3/16)=本面；右/左两侧各 5px=相邻斜板
		double u = rel.dot(Vec3.atLowerCornerOf(rightOf(clicked)
			.getNormal()));
		if (Math.abs(u) <= 3.0 / 16.0)
			return propertyFor(clicked);
		return propertyFor(u > 0 ? rightCorner(clicked) : leftCorner(clicked));
	}

	/** 正交面右侧(面内正横向)相邻斜向；面内负横向取另一角。 */
	private static OctaCorner rightCorner(Direction face) {
		return switch (face) {
			case NORTH -> OctaCorner.NORTH_EAST;
			case EAST -> OctaCorner.SOUTH_EAST;
			case SOUTH -> OctaCorner.SOUTH_WEST;
			case WEST -> OctaCorner.NORTH_WEST;
			default -> throw new IllegalArgumentException();
		};
	}

	private static OctaCorner leftCorner(Direction face) {
		return switch (face) {
			case NORTH -> OctaCorner.NORTH_WEST;
			case EAST -> OctaCorner.NORTH_EAST;
			case SOUTH -> OctaCorner.SOUTH_EAST;
			case WEST -> OctaCorner.SOUTH_WEST;
			default -> throw new IllegalArgumentException();
		};
	}

	/** 正交面视角的"右方向"（面内正横向）：北面看右=东、东面看右=南、南面看右=西、西面看右=北。 */
	private static Direction rightOf(Direction face) {
		return switch (face) {
			case NORTH -> Direction.EAST;
			case EAST -> Direction.SOUTH;
			case SOUTH -> Direction.WEST;
			case WEST -> Direction.NORTH;
			default -> throw new IllegalArgumentException();
		};
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction.Axis poseAxis = state.getValue(AXIS);
		// 世界点击 → 本地(站姿)参考系：8 个 open 属性永远是方块本地方向，
		// blockstate 渲染时按 rotationX/rotationY 把模型转到躺姿；交互处做逆变换，
		// 保证"点哪个面/扇区就开哪个本地口"（三个姿态共用同一套站姿判定）。
		Direction clicked = toLocalDir(poseAxis, context.getClickedFace());
		Vec3 rel = toLocalVec(poseAxis, context.getClickLocation()
			.subtract(Vec3.atCenterOf(pos)));
		BooleanProperty target = resolveTarget(clicked, rel);

		if (!level.isClientSide) {
			level.setBlock(pos, state.setValue(target, !state.getValue(target)), Block.UPDATE_CLIENTS);
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		}
		return InteractionResult.SUCCESS;
	}

	// ========== 姿态：世界 ↔ 本地（与 blockstate rotationX/rotationY 严格一致） ==========
	// 站姿 base 模型坐标 = 本地坐标。blockstate 变体旋转 = MC 内部
	// BlockModelRotation(x,y) = Quaternionf().rotateYXZ(-y, -x, 0)，应用顺序先绕 X 后绕 Y。
	// 以下权威映射表按该四元数逐方向算得，代码/渲染/判定共用同一来源：
	//   axis=Y(0,0)    axis=X(90,90)            axis=Z(90,0)
	//   local→world:   U→E D→W N→D S→U E→S W→N   U→N D→S N→D S→U E→E W→W
	// world→local:     U←S D←N N←W S←E E←U W←D   U←S D←N N←U S←D E←E W←W

	/** 世界方向 → 本地方向（按放置姿态）。供扳手与世界↔本地换算共用。 */
	public static Direction toLocalDir(Direction.Axis poseAxis, Direction worldDir) {
		if (poseAxis == Direction.Axis.Y)
			return worldDir;
		if (poseAxis == Direction.Axis.X) {
			return switch (worldDir) {
				case UP -> Direction.SOUTH;
				case DOWN -> Direction.NORTH;
				case NORTH -> Direction.WEST;
				case SOUTH -> Direction.EAST;
				case EAST -> Direction.UP;
				case WEST -> Direction.DOWN;
			};
		}
		// axis=Z
		return switch (worldDir) {
			case UP -> Direction.SOUTH;
			case DOWN -> Direction.NORTH;
			case NORTH -> Direction.UP;
			case SOUTH -> Direction.DOWN;
			default -> worldDir; // E→E W→W
		};
	}

	/** 世界坐标差(相对方块中心) → 本地坐标差。旋转为正交矩阵，按上述映射表的矩阵转置。 */
	public static Vec3 toLocalVec(Direction.Axis poseAxis, Vec3 w) {
		if (poseAxis == Direction.Axis.Y)
			return w;
		if (poseAxis == Direction.Axis.X) {
			// R(本地→世界) 列向量 (lx,ly,lz)→(ly,lz,lx)，故逆 (wx,wy,wz)→(wz,wx,wy)
			return new Vec3(w.z, w.x, w.y);
		}
		// axis=Z: (lx,ly,lz)→(lx,lz,-ly)，逆 (wx,wy,wz)→(wx,-wz,wy)
		return new Vec3(w.x, -w.z, w.y);
	}

	/** 本地方向 → 世界方向（按放置姿态；与 {@link #toLocalVec} 互逆）。 */
	public static Vec3 toWorldVec(Direction.Axis poseAxis, Vec3 l) {
		if (poseAxis == Direction.Axis.Y)
			return l;
		if (poseAxis == Direction.Axis.X)
			return new Vec3(l.y, l.z, l.x);
		return new Vec3(l.x, l.z, -l.y);
	}
}
