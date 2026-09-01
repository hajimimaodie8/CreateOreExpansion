package com.hjmmd_8.createoreexpansion.compat.sable;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Sable 物理属性验证（调试）—— 服务端启动后打印关键方块的质量/浮空材料，
 * 用于确认 {@code data/.../physics_block_properties/} 数据包是否生效。
 */
public final class SablePhysicsVerify {

	private SablePhysicsVerify() {
	}

	/** 服务端启动后（数据包已加载）验证各方块物理属性并打印。 */
	public static void onServerStarted(ServerStartedEvent event) {
		try {
			ServerLevel level = event.getServer()
				.overworld();
			BlockPos pos = new BlockPos(0, 100, 0);
			var iron = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, Blocks.IRON_BLOCK.defaultBlockState());
			var diamond = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
			var jadeOre = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.JADE_ORE.get()
					.defaultBlockState());
			var jadeBlock = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.JADE_BLOCK.get()
					.defaultBlockState());
			var jadeCasing = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.JADE_CASING.get()
					.defaultBlockState());
			var grinder = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.POWER_ANGLE_GRINDER.get()
					.defaultBlockState());
			var regulator = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.ENERGY_WAVE_REGULATOR.get()
					.defaultBlockState());
			var lamp = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.ENERGY_SENSING_LAMP.get()
					.defaultBlockState());
			var sapphireBlock = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.SAPPHIRE_BLOCK.get()
					.defaultBlockState());
			var topazBlock = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.TOPAZ_BLOCK.get()
					.defaultBlockState());
			var stellarBlock = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getMass(level, pos, com.hjmmd_8.createoreexpansion.common.AllBlocks.STELLARSTONE_BLOCK.get()
					.defaultBlockState());
			var stellarFloat = dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
				.getFloatingMaterial(com.hjmmd_8.createoreexpansion.common.AllBlocks.STELLARSTONE_BLOCK.get()
					.defaultBlockState());
			CreateOreExpansion.LOGGER.info("[Sable] 物理属性验证：铁块={} 翡翠块={} 翡翠矿石={} 翡翠机壳={} 角磨床={} 调级器={} 感应灯={} 蓝宝石块={} 黄玉块={} 星辉石块={} 星辉石浮空={}",
				iron, jadeBlock, jadeOre, jadeCasing, grinder, regulator, lamp, sapphireBlock, topazBlock,
				stellarBlock, stellarFloat == null ? "null" : "OK");
		} catch (Throwable t) {
			CreateOreExpansion.LOGGER.warn("[Sable] 物理属性验证失败", t);
		}
	}
}
