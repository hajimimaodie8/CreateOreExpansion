package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 能量调级器：六向应力机器（可水平/竖直放置，齿轮轴沿 FACING 方向）。
 *
 * <p><b>方向铁律（核心逻辑实现时必须严格遵守，勿弄反）</b>——模型各部件经
 * blockstate 旋转（{@code AllBlocks} 中 xRot/yRot）后的世界方向：</p>
 * <ul>
 *     <li><b>齿轮轴</b>：沿 {@code FACING} 方向（{@link #getRotationAxis()} = FACING 的轴）；
 *         传动轴从 FACING 对面接入（{@link #hasShaftTowards()}）。</li>
 *     <li><b>顶能量接收面板</b>（模型 up 面，{@code wave_receiver} 纹理）：<b>朝 {@code FACING}</b>——
 *         竖直放置（UP）朝上、倒放（DOWN）朝下、水平放置朝 FACING 方向（模型 xRot=270 躺倒 + yRot 转向）。</li>
 *     <li><b>底能量接收面板</b>（模型 down 面）：<b>朝 {@code FACING} 对面</b>（顶面板对立面）。</li>
 *     <li><b>齿轮 4 侧面</b>（模型 north/south/east/west）：无面板、扳手不响应。</li>
 * </ul>
 *
 * <p>继承 {@link DirectionalKineticBlock} 复用放置朝向（优先对准相邻传动轴）、
 * 齿轮轴接入方向等通用行为；实现 {@link IBE} 绑定方块实体，
 * 齿轮由渲染器用 Create 标准旋转逻辑随应力旋转。</p>
 *
 * <p><b>实现 {@link ICogWheel}</b>：作为小齿轮参与 Create 齿轮啮合——
 * 旁边放置齿轮时显示啮合位置预览（虚线框），应力网络识别后可与相邻齿轮
 * 啮合反向传动（用默认 {@code isSmallCog()=true}、{@code isDedicatedCogWheel()=false}）。</p>
 *
 * <p><b>能量接收面（齿轮对侧两面）</b>：顶面（朝 FACING）与底面（朝 FACING 对面）
 * 各有一个能量接收面板，初始为 {@code close}（{@code wave_receiver_close} 纹理）；
 * 用扳手右键对应面切换 {@code open}/{@code close}（{@link #RECEIVER_TOP} /
 * {@link #RECEIVER_BOTTOM}），两面状态独立并存入方块实体 NBT
 * （退出存档重进仍保持）。</p>
 *
 * <p>光照规则（仿 Create 机器，同翡翠应力充能器）：机器方块不阻挡光照——
 * 天空光可无衰减穿透（{@link #propagatesSkylightDown} = true），自身面不发暗
 * （{@link #getShadeBrightness} = 1.0）。配合注册时的 noOcclusion()，
 * getLightBlock 自动为 0。{@link #getShape} 返回满格形状与机身一致。</p>
 */
public class EnergyWaveRegulatorBlock extends DirectionalKineticBlock
	implements IBE<EnergyWaveRegulatorBlockEntity>, ICogWheel, IWrenchable {

	/** 顶面能量接收面板：false=close（关闭）、true=open（打开） */
	public static final BooleanProperty RECEIVER_TOP = BooleanProperty.create("receiver_top");
	/** 底面能量接收面板：false=close（关闭）、true=open（打开） */
	public static final BooleanProperty RECEIVER_BOTTOM = BooleanProperty.create("receiver_bottom");

	/**
	 * 满格碰撞/选中框（0~16）：本机模型是满格立方体（机座从 0 到 16），
	 * 碰撞箱与模型完全一致，选中框贴合机身外沿（不内缩）。
	 * <p>注：此机器模型满格且已 {@code noOcclusion()} + {@code ambientocclusion=false}，
	 * 不会因全方块碰撞形状引发 AO 遮蔽发黑（充能器内缩是因为其模型本身不满格）。</p>
	 */
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

	public EnergyWaveRegulatorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(RECEIVER_TOP, false)
			.setValue(RECEIVER_BOTTOM, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(RECEIVER_TOP, RECEIVER_BOTTOM);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public Class<EnergyWaveRegulatorBlockEntity> getBlockEntityClass() {
		return EnergyWaveRegulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends EnergyWaveRegulatorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENERGY_WAVE_REGULATOR.get();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// 仿创造马达/充能器：优先对准相邻传动轴方向放置
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

	/**
	 * 最低转速要求：FAST（默认 100 RPM，跟随 Create 配置 {@code fastSpeed}）。
	 * <p>转速不足时：{@code KineticBlockEntity.isSpeedRequirementFulfilled()} 返回 false，
	 * 护目镜自动显示与搅拌器等 Create 机器完全一致的"转速不足"提示
	 * （{@code tooltip.speedRequirement} + {@code gui.contraptions.not_fast_enough}），
	 * 且 {@code EnergyWaveRegulation} 判定时低于此门槛不进行波级调制。</p>
	 */
	@Override
	public IRotate.SpeedLevel getMinimumRequiredSpeedLevel() {
		return IRotate.SpeedLevel.FAST;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		// 传动轴从 FACING 对面（底端）接入，与齿轮轴同一轴线的另一端
		return face == state.getValue(FACING)
			.getOpposite();
	}

	// ========== 扳手交互：侧面分区切换顶/底面板，机壳面直接切换 ==========

	@Override
	public InteractionResult onWrenched(BlockState state, net.minecraft.world.item.context.UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		// 调级器只有顶/底两个开口方向（顶面板朝 FACING、底面板朝 FACING 对面）。
		// 点击 4 个侧面（base_side 贴图面）：按点击位置上下分区——上半 → 顶口、下半 → 底口
		// （侧面永不紧贴、易点选，解决了顶/底被遮挡时无法开口的问题）；
		// 点击机壳面（顶面板方向或底面板方向）：直接切换该侧面板。
		Direction facing = state.getValue(FACING);
		Direction clicked = context.getClickedFace();

		BooleanProperty property;
		if (clicked.getAxis() == facing.getAxis()) {
			// 机壳面（顶面板方向 FACING 或其对面）：直接切换该侧面板
			property = clicked == facing ? RECEIVER_TOP : RECEIVER_BOTTOM;
		} else {
			// 齿轮 4 个侧面：按点击位置上下分区（偏向 FACING 一侧=上 → 顶口；偏向 FACING 对面=下 → 底口）
			property = sideForPanelClick(state, context);
		}

		boolean open = state.getValue(property);
		EnergyWaveRegulatorBlockEntity.switchToBlockState(level, pos,
			state.setValue(property, !open));
		if (!level.isClientSide) {
			AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.random.nextFloat() + .5f);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * 侧面分区点击：以方块中心为原点，点击位置沿<b>FACING 轴</b>方向的上下分区——
	 * 偏向 FACING 一侧（模型 up 面所在方向）= 顶口；偏向 FACING 对面 = 底口。
	 * <p>实测校准：竖直放置（FACING=UP）时，点击侧面<b>视觉上半</b>（rel.y&gt;0）
	 * 即"偏向 FACING 一侧"，应开<b>顶口</b>（朝上、用户视觉上半的口）；点视觉下半开底口。</p>
	 *
	 * @return 命中的面板属性（RECEIVER_TOP / RECEIVER_BOTTOM）
	 */
	private BooleanProperty sideForPanelClick(BlockState state, UseOnContext context) {
		Vec3 hit = context.getClickLocation();
		Vec3 rel = hit.subtract(Vec3.atCenterOf(context.getClickedPos()));
		Direction facing = state.getValue(FACING);
		// 相对中心沿 FACING 方向的偏移：正 = 偏向 FACING 一侧（视觉上半）→ 顶口
		double along = rel.dot(Vec3.atLowerCornerOf(facing.getNormal()));
		return along >= 0 ? RECEIVER_TOP : RECEIVER_BOTTOM;
	}

	// ========== 光照规则（仿 Create 机器） ==========

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}
}
