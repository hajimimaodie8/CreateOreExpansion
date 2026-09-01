package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 能量感应灯（Energy Sensing Lamp）：被能量波击中后切换为对应波级的亮态，
 * 产生 15 级光照并向四周输出对应强度的红石信号；亮态持续一段时间后自动熄灭。
 *
 * <p><b>状态</b>（{@link #LAMP_STATE}，0~3）：</p>
 * <ul>
 *   <li><b>0 = 熄灭</b>：无光照、无红石信号（默认）；</li>
 *   <li><b>1 = 低能量波命中</b>：黄色，光照 15，红石信号 5，持续 1 秒；</li>
 *   <li><b>2 = 高能量波命中</b>：绿色，光照 15，红石信号 10，持续 2 秒；</li>
 *   <li><b>3 = 伽马能量波命中</b>：蓝色，光照 15，红石信号 15，持续 3 秒。</li>
 * </ul>
 *
 * <p><b>渲染</b>：blockstate 变体模型按状态切换贴图（0~3 四种纹理），无自定义渲染器。
 * <b>计时</b>：轻量方块实体 {@link EnergySensingLampBlockEntity} 记录点亮时刻并 tick 熄灭
 * （仅计时，不参与渲染；与强化避雷针 BE 同款轻量模式）。</p>
 */
public class EnergySensingLampBlock extends Block implements IBE<EnergySensingLampBlockEntity> {

	/** 灯状态：0=熄灭、1=低(黄)、2=高(绿)、3=伽马(蓝) */
	public static final IntegerProperty LAMP_STATE = IntegerProperty.create("lamp_state", 0, 3);

	/** 各状态的红石信号强度：0→0、1→5、2→10、3→15 */
	private static final int[] REDSTONE_BY_STATE = { 0, 5, 10, 15 };

	/** 各状态的亮态持续时间（tick）：1→20（1秒）、2→40（2秒）、3→60（3秒） */
	private static final int[] LIT_TICKS_BY_STATE = { 0, 20, 40, 60 };

	/** 满格碰撞/选中框（与模型一致） */
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

	public EnergySensingLampBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LAMP_STATE, 0));
	}

	@Override
	public Class<EnergySensingLampBlockEntity> getBlockEntityClass() {
		return EnergySensingLampBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends EnergySensingLampBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENERGY_SENSING_LAMP.get();
	}

	/** 方块实体 ticker：服务端逐 tick 检查亮态是否到点熄灭（仅计时，无渲染逻辑） */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : (l, p, s, be) -> {
			if (be instanceof EnergySensingLampBlockEntity lamp)
				lamp.tick();
		};
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return simpleCodec(EnergySensingLampBlock::new);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LAMP_STATE);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	// ========== 光照 ==========

	/** 亮态（state &gt; 0）时产生 15 级光照，熄灭（0）不发光。 */
	@Override
	public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(LAMP_STATE) > 0 ? 15 : 0;
	}

	// ========== 红石信号 ==========

	/** 弱红石信号（相邻方块可读取）：按灯状态输出 0/5/10/15。 */
	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return REDSTONE_BY_STATE[state.getValue(LAMP_STATE)];
	}

	/** 强红石信号（可直连红石线/中继器）：与弱信号一致。 */
	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return REDSTONE_BY_STATE[state.getValue(LAMP_STATE)];
	}

	// ========== 能量波命中 ==========

	/**
	 * 被能量波击中：按波级切换灯状态（1=低黄、2=高绿、3=伽马蓝）并刷新亮态计时，
	 * 波级非法（≤0）时不改变状态。亮态持续时间见 {@link #LIT_TICKS_BY_STATE}。
	 *
	 * @param waveLevel 波的等级（1=低，2=高，3=伽马）
	 */
	public static void onWaveHit(Level level, BlockPos pos, int waveLevel) {
		if (waveLevel < 1 || waveLevel > 3)
			return;
		BlockState state = level.getBlockState(pos);
		BlockState next = state.setValue(LAMP_STATE, waveLevel);
		if (next != state)
			level.setBlock(pos, next, Block.UPDATE_ALL);
		// 刷新计时：点亮时刻 = 当前游戏时间 + 持续时间（被更强/更弱波再次命中时重新计时）
		if (level.getBlockEntity(pos) instanceof EnergySensingLampBlockEntity lamp)
			lamp.refreshLit(waveLevel);
	}
}
