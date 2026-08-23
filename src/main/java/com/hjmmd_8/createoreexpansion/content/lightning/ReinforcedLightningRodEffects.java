package com.hjmmd_8.createoreexpansion.content.lightning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

import org.joml.Vector3f;

/**
 * 强化避雷针效果工具（与方块实体业务解耦，附属模组可直接调用）：
 * 闪电生成（规避骷髅陷阱马漏洞）、金色电光粒子播放。
 */
public final class ReinforcedLightningRodEffects {

	private ReinforcedLightningRodEffects() {
	}

	/** 在方块上方生成一道闪电（visualOnly：纯视觉，不引燃、不生成骷髅陷阱马，规避原版漏洞） */
	public static void spawnLightning(Level level, BlockPos pos) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt == null)
			return;
		bolt.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
		// 纯视觉闪电：不生成骷髅陷阱马、不引燃方块/伤害实体
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
	}

	/** 播放金色电光粒子（服务端用 sendParticles，客户端用 addParticle，两端皆可） */
	public static void spawnChargeParticles(Level level, BlockPos pos) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 1.05;
		double z = pos.getZ() + 0.5;
		DustParticleOptions particle = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.1f), 0.6f);
		if (level instanceof ServerLevel server) {
			server.sendParticles(particle, x, y, z, 6, 0.3, 0.35, 0.3, 0.05);
		} else {
			level.addParticle(particle, x, y, z, 0, 0.03, 0);
		}
	}
}
