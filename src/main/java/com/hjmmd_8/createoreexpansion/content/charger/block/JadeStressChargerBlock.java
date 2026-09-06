package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 翡翠应力充能器：六向应力机器（可水平/竖直放置）。
 *
 * <p>继承 {@link AbstractCreateChargerBlock} 复用全部通用行为（放置朝向、MODE、传动轴接入），
 * 仅绑定翡翠充能器方块实体。</p>
 */
public class JadeStressChargerBlock extends AbstractCreateChargerBlock implements IBE<JadeStressChargerBlockEntity> {

	public JadeStressChargerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<JadeStressChargerBlockEntity> getBlockEntityClass() {
		return JadeStressChargerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends JadeStressChargerBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.JADE_STRESS_CHARGER.get();
	}
}
