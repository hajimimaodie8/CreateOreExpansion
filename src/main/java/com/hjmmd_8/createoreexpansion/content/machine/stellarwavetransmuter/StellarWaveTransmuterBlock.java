package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
 * 星辉波变器：六向应力机器（可水平/竖直放置）。
 *
 * <p><b>放置/轴口约定照能量场控制器（{@code EnergyFieldControllerBlock}）</b>——
 * "六向"指 FACING 六个放置朝向，<b>传动轴口始终只有一个</b>：从 FACING 反面（底部）接入
 * （{@code hasShaftTowards} 只认 FACING 反面），放置自动对轴。
 * 本机能量波从<b>四个水平侧面</b>穿入/穿出（轴口面与灯盘面不可穿）。</p>
 *
 * <p><b>模型分面材质约定</b>（见 {@code models/block/energy_wave_machine/stellarstone_wave_transmuter_*.json}）：</p>
 * <ul>
 *   <li>4 个侧面 = 星辉石能量接收面：关 = {@code stellarstone_wave_receiver_close}，
 *       开 = {@code ..._open}（<b>整面切换</b>，16 变体静态模型）；</li>
 *   <li>顶面（FACING 面）= 灯盘 {@code stellarstone_disperser_lamp}；</li>
 *   <li>轴口面（FACING 反面）= {@code stellarstone_gearbox}，机座边框 = {@code stellarstone_casing}。</li>
 * </ul>
 *
 * <p><b>开口规则（用户定义）</b>：只有<b>世界水平 4 侧</b>可开，且 4 面互不干扰。波的水平入口面
 * 关闭 → 按撞关闭机壳（撞墙消散）；入口开而<b>对面出口关</b> → 原路遣返（等级不变、不转换）；
 * 入口开且对面出口开 → 生成变体波从对侧穿出。判定见
 * {@code StellarWaveTransmuterPass#tryConvert}。</p>
 *
 * <p><b>扳手交互</b>（{@link #onWrenched}，本机<b>没有</b>拨叉/齿轮切换机制）：</p>
 * <ul>
 *   <li>点 4 个侧面 → 切换该侧开/关（模型面映射到世界方向后，非水平方向不切换）；</li>
 *   <li>点灯盘面（FACING 面）→ 按点击位置沿对角线分 4 区域（照抄差波器
 *       {@code EnergyWaveDisperserBlock#sideForShellClick}），切换对应侧面开口；
 *       用户要求"哪个面开口，对应那边的灯就亮"，即灯位点击 = 切换该侧开口；</li>
 *   <li>点轴口面（FACING 反面）→ 旋转朝向（轴口面是传动轴面，不可作为波口打开）。</li>
 * </ul>
 *
 * <p><b>工作原理简述</b>：接入应力后按扫描半径快照周边加工机器（见
 * {@link StellarWaveTransmuterBlockEntity}）；经过的能量波被附加扫描到的全部
 * 加工属性后从对侧穿出，命中物品即远程执行链式加工。</p>
 */
public class StellarWaveTransmuterBlock extends DirectionalKineticBlock
	implements IBE<StellarWaveTransmuterBlockEntity> {

	/** 六向朝向（同 BlockStateProperties.FACING，与 Create DirectionalKineticBlock.FACING 同一实例） */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/** 模型北侧波口：false=close（关闭）、true=open（打开） */
	public static final BooleanProperty NORTH = BooleanProperty.create("open_north");
	/** 模型东侧波口 */
	public static final BooleanProperty EAST = BooleanProperty.create("open_east");
	/** 模型南侧波口 */
	public static final BooleanProperty SOUTH = BooleanProperty.create("open_south");
	/** 模型西侧波口 */
	public static final BooleanProperty WEST = BooleanProperty.create("open_west");

	/** 非全方块形状（xz 内缩 0.5px，仿 Create 机器避免 AO 遮蔽发黑）。 */
	private static final VoxelShape SHAPE = box(0.5, 0, 0.5, 15.5, 16, 15.5);

	public StellarWaveTransmuterBlock(Properties properties) {
		super(properties);
		// 默认四面全开：与旧版（尚无开口概念时，水平四面恒可穿）行为保持一致，
		// 避免升级后老存档里的波立刻撞墙、也避免新放置机器"看起来能用但波不穿"。
		// 注意：差波器/六面差波器的默认是"全关"，本机刻意不同（理由见上）。
		registerDefaultState(defaultBlockState()
			.setValue(FACING, Direction.UP)
			.setValue(NORTH, true)
			.setValue(EAST, true)
			.setValue(SOUTH, true)
			.setValue(WEST, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		// FACING 由 DirectionalKineticBlock#createBlockStateDefinition 加入（勿重复添加）
		super.createBlockStateDefinition(builder);
		builder.add(NORTH, EAST, SOUTH, WEST);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 自动对轴（同充能器/场控）：getPreferredFacing 返回相邻传动轴所在侧；本机轴口在
		// FACING 反面，故 FACING 取 preferred 的反方向，让轴口自动扣住相邻传动轴。
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
		// 传动轴从 FACING 对面（底部）接入——本机只有一个轴口
		return face == state.getValue(FACING)
			.getOpposite();
	}

	@Override
	public Class<StellarWaveTransmuterBlockEntity> getBlockEntityClass() {
		return StellarWaveTransmuterBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StellarWaveTransmuterBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.STELLAR_WAVE_TRANSMUTER.get();
	}

	// ========== 开口属性 ↔ 世界方向 ==========

	/**
	 * 模型侧面 ↔ 开口属性。<b>注意属性名（open_north…）指的是"模型面"，不是世界方向</b>——
	 * 与能量波差器一致，世界方向需经 {@link EnergyWaveDisperserBlock#worldDirOf} 换算。
	 */
	public static BooleanProperty propertyFor(Direction modelSide) {
		return switch (modelSide) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			default -> null; // 灯盘面/轴口面不可开
		};
	}

	/** 世界方向 → 模型侧面（复用差波器已游戏实测校准的表；返回 null = 非水平侧面）。 */
	public static Direction modelFaceOf(Direction facing, Direction worldDir) {
		return EnergyWaveDisperserBlock.modelFaceOf(facing, worldDir);
	}

	/** 模型侧面 → 世界方向（复用差波器表）。 */
	public static Direction worldDirOf(Direction facing, Direction modelSide) {
		return EnergyWaveDisperserBlock.worldDirOf(facing, modelSide);
	}

	/** 该世界方向是否属于"可开的水平侧面"（UP/DOWN 恒不可开）。 */
	public static boolean isOpenable(Direction worldSide) {
		return worldSide != null && worldSide.getAxis()
			.isHorizontal();
	}

	/**
	 * 查询某<b>世界方向</b>的波口是否开启（供波判定/灯指示用）。
	 * <p>UP/DOWN（灯盘面/轴口面）恒返回 false——本机只有 4 个水平侧面可开。</p>
	 */
	public static boolean isOpen(BlockState state, Direction worldSide) {
		if (!(state.getBlock() instanceof StellarWaveTransmuterBlock) || !isOpenable(worldSide))
			return false;
		Direction modelSide = modelFaceOf(state.getValue(FACING), worldSide);
		BooleanProperty property = modelSide == null ? null : propertyFor(modelSide);
		return property != null && state.getValue(property);
	}

	/** 世界方向 → 开口属性（非水平侧面返回 null）。 */
	public static BooleanProperty propertyForWorld(BlockState state, Direction worldSide) {
		if (!isOpenable(worldSide))
			return null;
		Direction modelSide = modelFaceOf(state.getValue(FACING), worldSide);
		return modelSide == null ? null : propertyFor(modelSide);
	}

	// ========== 扳手交互 ==========

	/**
	 * 扳手右键：
	 * <ul>
	 *   <li>4 个侧面 → 切换该侧波口开/关；</li>
	 *   <li>灯盘面（FACING 面）→ 按点击位置 4 分区切换对应侧面波口；</li>
	 *   <li>轴口面（FACING 反面）→ 旋转朝向（轴口面不可开）。</li>
	 * </ul>
	 * 状态修改只在服务端执行（客户端由同步包更新，避免多人下双端不一致）。
	 */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction clicked = context.getClickedFace();
		Direction facing = state.getValue(FACING);

		// 1) 轴口面（FACING 反面）：旋转朝向（传动轴面，不允许作为波口打开）
		if (clicked == facing.getOpposite())
			return super.onWrenched(state, context);

		// 2) 世界方向 → 模型侧面（水平侧面 = 直接点该侧；灯盘面 = 按点击位置分区）
		Direction worldDir = clicked == facing ? worldDirForLampClick(state, context) : clicked;
		if (!isOpenable(worldDir))
			return InteractionResult.SUCCESS; // 映射落到 UP/DOWN：该点击不切换任何面

		BooleanProperty property = propertyForWorld(state, worldDir);
		if (property == null)
			return InteractionResult.SUCCESS;

		if (!level.isClientSide) {
			level.setBlock(pos, state.setValue(property, !state.getValue(property)), Block.UPDATE_CLIENTS);
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * 灯盘面（FACING 面）分区点击：以方块中心为原点，点击位置沿两条对角线分成 4 区域，
	 * 命中区域先得<b>世界方向</b>（与灯位一致），再由 {@link #propertyForWorld} 换到模型面属性。
	 *
	 * <p>分区逻辑与差波器 {@code EnergyWaveDisperserBlock#sideForShellClick} 完全一致
	 * （已游戏实测校准）：机壳朝 Y 时用 x/z 平面，朝 X 时用 y/z 平面，朝 Z 时用 x/y 平面。</p>
	 *
	 * @return 命中的世界方向（可能是 UP/DOWN——躺倒放置时机壳面内含有竖直分区的落点）
	 */
	private Direction worldDirForLampClick(BlockState state, UseOnContext context) {
		Vec3 rel = context.getClickLocation()
			.subtract(Vec3.atCenterOf(context.getClickedPos()));
		Direction facing = state.getValue(FACING);

		if (facing.getAxis() == Axis.Y) {
			// 灯盘朝上/下：用 x/z 偏移，上北（-Z）下南（+Z）左西（-X）右东（+X）
			if (Math.abs(rel.x) > Math.abs(rel.z))
				return rel.x > 0 ? Direction.EAST : Direction.WEST;
			return rel.z > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		if (facing.getAxis() == Axis.X) {
			// 灯盘朝东西（躺倒）：分区落在 y/z 平面
			if (Math.abs(rel.y) > Math.abs(rel.z))
				return rel.y > 0 ? Direction.UP : Direction.DOWN;
			return rel.z > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		// 灯盘朝南北，分区落在 x/y 平面
		if (Math.abs(rel.y) > Math.abs(rel.x))
			return rel.y > 0 ? Direction.UP : Direction.DOWN;
		return rel.x > 0 ? Direction.EAST : Direction.WEST;
	}

	// ========== 光照规则（仿 Create 机器，同充能器/场控） ==========

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}
}
