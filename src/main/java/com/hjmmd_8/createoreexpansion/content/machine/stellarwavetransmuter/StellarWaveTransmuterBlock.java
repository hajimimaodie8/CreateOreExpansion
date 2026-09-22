package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.MachineFaceQuadrants;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
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
 * <p><b>开口规则（用户定义）</b>：只有本机<b>自己的 4 个侧面</b>可开（灯盘面与轴口面没有波口），
 * 且 4 面互不干扰。波的水平入口面关闭 → 按撞关闭机壳（撞墙消散）；入口开而<b>对面出口关</b> →
 * 原路遣返（等级不变、不转换）；入口开且对面出口开 → 生成变体波从对侧穿出。判定见
 * {@code StellarWaveTransmuterPass#tryConvert}。</p>
 *
 * <p><b>2026-09-14 修正（"4 个口开着却点不掉"）</b>：这条规则原先写成"只有<b>世界水平</b> 4 侧可开"
 * ——本机是六向放置的，躺倒放置时它自己的 4 个侧面里有 2 个朝上/朝下，于是那 2 个口默认开着
 * （模型显示开、灯也亮）却怎么点都切不动。现在口径落在机器自己的侧面上（见
 * {@link #isOpenable(BlockState, Direction)}），与差波器一致；正立/倒置放置时新旧口径结果完全相同，
 * 波判定侧的结果也不变（波只会水平入射）。</p>
 *
 * <p><b>交互分工（用户定义，两件互不重叠的事）</b>：</p>
 * <ul>
 *   <li><b>空手右键 = 波口开关</b>（{@link #useWithoutItem}）：点 4 个侧面直接切换该侧；
 *       点灯盘面（FACING 面）按点击位置沿对角线分 4 区域（与差波器机壳面点击共用
 *       {@link MachineFaceQuadrants#worldDirectionAt} 一份分区数学），切换对应侧面开口——
 *       用户要求"哪个面开口，对应那边的灯就亮"，即灯位点击 = 切换该侧开口；
 *       点轴口面（FACING 反面，传动轴面）不切换任何面。开关逻辑只有
 *       {@link #toggleWavePort} 一处实现；</li>
 *   <li><b>扳手右键 = 切换处理模式</b>（{@link #onWrenched}）：加工波变态 ↔ 攻击波变态
 *       （见 {@link TransmuterMode}）。扳手<b>不再旋转朝向</b>（放置时自动对轴）。</li>
 * </ul>
 *
 * <p><b>2026-09 三条规格（用户定稿）</b>：① <b>进入攻击波变态时无条件把 4 个口强制打开</b>
 * （进入模式的动作为模式自报，见 {@link TransmuterMode#onEnter} → {@link #forceAllPortsOpen}）；
 * ② 攻击态下空手右键<b>不改口、也不出开关音效</b>（{@link #toggleWavePort} 的锁口分支在音效之前
 * 返回）；③ <b>放置时四口默认全关</b>（{@link #getStateForPlacement}）——但 blockstate 默认值
 * 仍保持全开，因为它是老存档缺 {@code open_*} 键时的回退值（理由见构造器注释）。</p>
 *
 * <p><b>工作原理简述</b>：接入应力后按扫描半径快照周边加工机器（见
 * {@link StellarWaveTransmuterBlockEntity}）；经过的能量波被附加扫描到的全部
 * 加工属性后从对侧穿出，命中物品即远程执行链式加工。</p>
 */
public class StellarWaveTransmuterBlock extends DirectionalKineticBlock
	implements IBE<StellarWaveTransmuterBlockEntity>, com.hjmmd_8.createoreexpansion.content.machine.CewsMachine {

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
		// 方块状态的"默认值"保持四面全开——**不要**因为"放置默认全关"（规格 3）就一起改成全关：
		// 这个默认值同时是"存档 / 结构 NBT 里缺 open_* 键时的回退值"（原版 NbtUtils.readBlockState
		// 从 block.defaultBlockState() 起算、再逐个覆盖 NBT 里确实存在的属性），改它会让早于这四个
		// 属性存在的老存档机器在升级后静默变成四口全关。用户只要求"放置时全关"，所以放置路径由
		// getStateForPlacement 单独覆盖（唯一改动点），本默认值原样不动。
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
		BlockState placed = (context.getPlayer() != null && context.getPlayer()
			.isShiftKeyDown()) || preferred == null
				? super.getStateForPlacement(context)
				: defaultBlockState().setValue(FACING, preferred.getOpposite());
		// 规格 3（放置默认）：先由上面的分支（含 super 分支）拿到 FACING，再把四个波口一律置为关闭。
		// 只动这条放置路径；registerDefaultState 保持全开（理由见构造器注释）。
		return withAllPorts(placed, false);
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

	/**
	 * 该<b>世界方向</b>是否属于"可开的波口"——即它能否对应到本机模型上的一个<b>侧面</b>。
	 *
	 * <p><b>口径必须落在机器自己的 4 个侧面上，不能写"世界水平"</b>（2026-09-14 修正）：本机六向
	 * 放置，<b>4 个侧面朝哪四个世界方向随 FACING 一起转</b>。旧实现只认"世界水平方向"，于是躺倒放置
	 * （FACING 水平）的机器上，它自己那 4 个侧面里有 <b>2 个朝上/朝下</b>——这 2 个口按旧口径
	 * "不可开"：blockstate 默认值仍是开（模型显示开、灯亮），<b>空手怎么点都切不动</b>
	 * （玩家实测："4 个开口默认是开的、4 个灯也都亮的、用手点是关不掉的"），而且灯盘面 4 分区里有
	 * 2 个分区也落在这些"不可开"的方向上 → 点了没反应。现在改为"能在模型上找到侧面就可开"，
	 * 与差波器 {@link EnergyWaveDisperserBlock}（本机复用它那张世界方向↔模型侧面的表）的口径完全一致。</p>
	 *
	 * <p><b>对正立/倒置机器零变化</b>：FACING = UP/DOWN 时，模型 4 侧面正对世界 4 个水平方向，
	 * 而灯盘面（UP/DOWN）在模型上根本没有侧面对应 → 新旧口径结果逐项相同。</p>
	 *
	 * <p><b>对波判定同样零变化</b>：波只会水平飞行（竖直分量判定见
	 * {@code StellarWaveTransmuterPass#tryConvert}），故 {@code isOpen} 的调用方传进来的永远是水平
	 * 世界方向；对这些方向新口径与旧口径给出同一答案（唯一差别是"躺倒机器的灯盘面/轴口面"那两种
	 * 方向——旧口径先按"水平"放行、再在 {@code propertyForWorld} 里返回 null 而判为关闭，
	 * 新口径直接判为关闭，结论一致）。</p>
	 *
	 * @param state     本方块状态（取 FACING 判断模型朝向）
	 * @param worldSide 被点/被波照射的世界方向
	 */
	public static boolean isOpenable(BlockState state, Direction worldSide) {
		return state != null && state.getBlock() instanceof StellarWaveTransmuterBlock
			&& modelFaceOf(state.getValue(FACING), worldSide) != null;
	}

	/**
	 * 查询某<b>世界方向</b>的波口是否开启（供波判定/灯指示用）。
	 * <p>灯盘面（FACING 面）与轴口面（FACING 反面）恒返回 false——那两面在模型上没有波口；
	 * 其余 4 个方向按 FACING 换算到模型侧面后取属性值（口径见
	 * {@link #isOpenable(BlockState, Direction)}）。</p>
	 */
	public static boolean isOpen(BlockState state, Direction worldSide) {
		BooleanProperty property = propertyForWorld(state, worldSide);
		return property != null && state.getValue(property);
	}

	/**
	 * 世界方向 → 开口属性（<b>唯一换算处</b>：世界方向 → 模型侧面 → 属性；不可开的方向返回 null）。
	 *
	 * <p>返回 null 只有两种情形：给定的世界方向是<b>灯盘面</b>或<b>轴口面</b>（模型上没有侧面对应），
	 * 或者传入的不是本方块。调用方据此判定"这次点击/这道波与该面无关"。</p>
	 */
	public static BooleanProperty propertyForWorld(BlockState state, Direction worldSide) {
		if (state == null || !(state.getBlock() instanceof StellarWaveTransmuterBlock))
			return null;
		Direction modelSide = modelFaceOf(state.getValue(FACING), worldSide);
		return modelSide == null ? null : propertyFor(modelSide);
	}

	// ========== 四口批量置位（放置默认 / 进入攻击波变态，两条规格共用一处实现） ==========

	/**
	 * <b>把 4 个波口一律置为 {@code open}</b>（本类唯一一处"批量改开口"的实现；只动开口属性，
	 * FACING 与其它属性原样保留）。
	 *
	 * <p>两个调用点、两种语义：</p>
	 * <ul>
	 *   <li>放置默认（规格 3）：{@code getStateForPlacement} 传 {@code false} = 新放下的机器四口全关；</li>
	 *   <li>进入攻击波变态（规格 1）：{@link TransmuterMode#onEnter} 借 {@link #forceAllPortsOpen}
	 *       传 {@code true} = 无论原来什么状态都强制全开。</li>
	 * </ul>
	 */
	public static BlockState withAllPorts(BlockState state, boolean open) {
		return state.setValue(NORTH, open)
			.setValue(EAST, open)
			.setValue(SOUTH, open)
			.setValue(WEST, open);
	}

	/**
	 * <b>把该位置的变器 4 个波口强制全部打开</b>（规格 1：进入攻击波变态时无条件开放）。
	 *
	 * <p>一次性 {@code setBlockState}：只改 blockstate（开口），<b>不动</b>方块实体里的模式——模式由
	 * 调用方（{@code StellarWaveTransmuterBlockEntity#cycleMode()}）自己写并 {@code notifyUpdate()}。
	 * 状态本来就全开时不下发（避免无谓的方块更新与网络包）。</p>
	 *
	 * <p>调用方刻意不判断"是不是攻击模式"：本方法由 {@link TransmuterMode#onEnter} 按模式自报调用，
	 * 与 {@link TransmuterMode#locksWavePorts()} 同属"模式常量体自己声明的一条行为"。</p>
	 */
	public static void forceAllPortsOpen(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof StellarWaveTransmuterBlock))
			return;
		BlockState opened = withAllPorts(state, true);
		if (!opened.equals(state))
			level.setBlock(pos, opened, Block.UPDATE_CLIENTS);
	}

	// ========== 交互分工：空手 = 波口开关，扳手 = 处理模式 ==========

	/**
	 * <b>空手右键：切换"该面"的波口开关</b>。
	 *
	 * <ul>
	 *   <li>点 4 个侧面 → 直接切换该侧；</li>
	 *   <li>点灯盘面（FACING 面）→ 按点击位置分 4 区域，切换对应侧面（见
	 *       {@link #worldDirForLampClick}）；</li>
	 *   <li>点轴口面（FACING 反面）→ 不切换任何面（那个方向不是波口）。</li>
	 * </ul>
	 *
	 * <p><b>与扳手的分工（用户定义）</b>：空手只管波口开关；处理模式（加工波变态 ↔ 攻击波变态）
	 * 归扳手，见 {@link #onWrenched}。两条路径互不重叠，且开关状态只在服务端改、由同步包下发
	 * （多人下不会双端不一致）。</p>
	 *
	 * <p>状态修改刻意不复制一份：分区与"世界方向 → 开口属性"的换算只有
	 * {@link #toggleWavePort} 一处实现。</p>
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
		BlockHitResult hitResult) {
		// 真正"空手"才管波口：手里拿着东西（方块/工具/扳手）时放行给默认逻辑，
		// 否则右键变器会挡住放方块（useWithoutItem 在手握物品时也会被调用）。
		if (!player.getMainHandItem()
			.isEmpty())
			return InteractionResult.PASS;
		return toggleWavePort(state, level, pos, hitResult.getDirection(), hitResult.getLocation());
	}

	/**
	 * <b>扳手右键：只做一件事——切换处理模式</b>（加工波变态 ↔ 攻击波变态）。
	 *
	 * <p>与旧版的分工变化（用户要求）：扳手<b>不再负责旋转朝向</b>（原
	 * {@code super.onWrenched} 的旋转分支已移除；本机放置时自动对轴，
	 * 见 {@link #getStateForPlacement}），也不再负责波口开关（那是空手右键）。</p>
	 *
	 * <p>切换在<b>服务端</b>执行并靠同步包下发（模式存在方块实体里，见
	 * {@link StellarWaveTransmuterBlockEntity#cycleMode()}）：客户端只播本地预测的
	 * 音效/动画，不自行改状态，避免多人下两端不一致。</p>
	 */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof StellarWaveTransmuterBlockEntity be))
			return InteractionResult.SUCCESS;
		if (!level.isClientSide) {
			be.cycleMode();
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * <b>波口开关的唯一实现</b>（空手交互调用；任何其它入口要开/关波口都走这里，不许再抄一遍）。
	 *
	 * <p>参数刻意是显式值（方块状态 + 点中的世界方向 + 点的精确位置），而不是某个交互上下文对象：
	 * 这样"世界方向 → 模型侧面属性"的换算、灯盘面 4 分区判定、服务端改状态与音效全都只有一处。</p>
	 *
	 * @param clickedFace   玩家点中的方块面（世界方向）
	 * @param clickLocation 玩家点中的精确位置（灯盘面分区用；其它面不读）
	 * @return 恒 SUCCESS（客户端不预测；状态改动只在服务端执行，由同步包更新）；
	 *         <b>攻击波变态下恒定不改任何状态、且一声不响</b>（模式锁口，见
	 *         {@code TransmuterMode#locksWavePorts()}：判定在音效/改状态之前直接返回）
	 */
	private InteractionResult toggleWavePort(BlockState state, Level level, BlockPos pos, Direction clickedFace,
		Vec3 clickLocation) {
		// 模式锁口（用户 2026-09 规格）：攻击波变态下 4 个波口恒开，空手右键不得把它关上。
		// 判定由模式自报（TransmuterMode#locksWavePorts），此处刻意不写"是不是攻击模式"。
		// 规格 2：这条 return 必须保持在**音效之前**——开关音效只在本方法末尾的
		// `if (!level.isClientSide)` 块里播（与 setBlock 同一处），锁定时整段不执行，
		// 两端都不出声、也不产生任何其它副作用（无 setBlock / 无粒子 / 无动画）。
		if (TransmuterMode.at(level, pos)
			.locksWavePorts())
			return InteractionResult.SUCCESS;
		Direction facing = state.getValue(FACING);
		// 世界方向 → 模型侧面：水平侧面 = 点哪面切哪面；灯盘面（FACING 面）= 按点击位置分区取一侧
		Direction worldDir = clickedFace == facing ? worldDirForLampClick(state, pos, clickLocation) : clickedFace;
		// 灯盘面/轴口面（含灯盘面分区落到这两面时）在模型上没有波口 → 本次点击不切换任何面。
		// 判定与属性换算只有 propertyForWorld 一处（可开的方向集合随 FACING 一起转，
		// 见 isOpenable 的说明：躺倒放置时朝上/朝下的那 2 个侧面同样是可开的）。
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
	 * <p>分区数学<b>只有 {@link MachineFaceQuadrants#worldDirectionAt} 一处实现</b>——
	 * 与差波器 {@code EnergyWaveDisperserBlock#sideForShellClick} 共用同一份（已游戏实测校准：
	 * 机壳朝 Y 时用 x/z 平面，朝 X 时用 y/z 平面，朝 Z 时用 x/y 平面）；
	 * 本方法只做参数传递，保留原签名供 {@link #toggleWavePort} 调用。</p>
	 *
	 * @param pos           变器方块位置（取方块中心作为分区原点）
	 * @param clickLocation 玩家点中的精确位置（世界坐标）
	 * @return 命中的世界方向（可能是 UP/DOWN——躺倒放置时机壳面内含有竖直分区的落点）
	 */
	private static Direction worldDirForLampClick(BlockState state, BlockPos pos, Vec3 clickLocation) {
		return MachineFaceQuadrants.worldDirectionAt(state.getValue(FACING), pos, clickLocation);
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
	/**
	 * 本机有特殊切换模式 ⇒ 扳手只做这一件事（切模式），不参与 Ctrl+扳手旋转。
	 * 见 {@link com.hjmmd_8.createoreexpansion.content.machine.CewsMachine} 的统一交互规则 ③。
	 */
	@Override
	public boolean hasModeSwitch() {
		return true;
	}

}
