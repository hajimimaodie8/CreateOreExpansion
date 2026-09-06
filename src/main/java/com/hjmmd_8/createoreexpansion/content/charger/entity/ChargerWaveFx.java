package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.joml.Vector3f;

/**
 * 能量波视觉效果工具 —— 命中绽放/范围爆炸/粒子构造，与波实体状态解耦（参数化）。
 *
 * <p>从 {@link AbstractChargerWaveEntity} 拆出（该实体曾同时承担实体状态/碰撞判定/
 * 机器交互/视觉效果的职责）；本类只负责"波产生的声光效果"，无实体字段依赖。</p>
 */
final class ChargerWaveFx {

	private ChargerWaveFx() {
	}

	/**
	 * 服务端命中绽放：对应颜色向外扩散的染色粒子（大散布 + 速度，近似球面扩散）
	 * + 加工完成音效（紫水晶共鸣）。
	 *
	 * @param level 波所在世界（服务端）
	 * @param pos   绽放中心（世界坐标）
	 * @param color 粒子颜色（RGB 0-1）
	 */
	static void burst(Level level, Vec3 pos, Vec3 color) {
		if (!(level instanceof ServerLevel server))
			return;
		server.sendParticles(waveParticle(color, 0.6f), pos.x, pos.y, pos.z, 30,
			0.5, 0.5, 0.5, 0.3);
		server.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_RESONATE,
			SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** 客户端球面均匀扩散绽放（对应颜色，实体消散时补充）。 */
	static void burstParticles(Level level, Vec3 pos, Vec3 color) {
		net.minecraft.util.RandomSource random = level.random;
		for (int i = 0; i < 30; i++) {
			Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
				.normalize();
			level.addParticle(waveParticle(color, 0.5f), pos.x, pos.y, pos.z,
				dir.x * 0.35, dir.y * 0.35, dir.z * 0.35);
		}
	}

	/**
	 * 触发一次范围能量爆炸（不破坏地形）：
	 * <ul>
	 *   <li><b>爆炸半径 = 爆炸等级（格）</b>：水平正方形范围半径（1→3×3、2→5×5、3→7×7、
	 *       4→9×9、5→11×11）——4 级伊普西龙波爆炸 4 格、5 级欧米伽波爆炸 5 格；</li>
	 *   <li>区域内生物受该等级撞击伤害（查 {@link net.minecraft.world.damagesource.DamageSource} 前
	 *       见 {@link #damageForLevel}：4/6/8/10/12）；</li>
	 *   <li>区域内掉落物 / 置物台物品按该等级直接充能加工；</li>
	 *   <li><b>≥3 级爆炸</b>额外给范围内强化避雷针 +1 伽马充能；</li>
	 *   <li>密集粒子扩散（比撞墙 30 个更密）+ 爆炸音效。</li>
	 * </ul>
	 *
	 * @param level     波所在世界
	 * @param source    伤害源（能量波实体，作为 indirectMagic 攻击者）
	 * @param center    爆炸中心（世界坐标）
	 * @param color     主粒子颜色（可选第二色混合，null 则单色）
	 * @param color2    第二粒子颜色（null = 单色）
	 * @param boomLevel 爆炸等级（1~5；两波碰撞取较低等级）
	 */
	static void triggerBoom(Level level, Entity source, Vec3 center, Vec3 color, Vec3 color2, int boomLevel) {
		if (level instanceof ServerLevel server) {
			// 密集球面扩散粒子（等级越高越密）
			int count = 30 + boomLevel * 25; // 55 / 80 / 105 个
			server.sendParticles(waveParticle(color, 0.7f), center.x, center.y, center.z, count,
				1.2, 1.2, 1.2, 0.15);
			if (color2 != null) {
				// 混合第二色，增强视觉层次
				server.sendParticles(waveParticle(color2, 0.5f), center.x, center.y, center.z, count / 2,
					1.0, 1.0, 1.0, 0.12);
			}
			// 能量冲击音效（不破坏地形，仅声光效果）
			server.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE,
				SoundSource.BLOCKS, 1.0F, 1.0F);
		}

		// 水平正方形范围（半径 = 爆炸等级），按等级处理生物伤害与物品加工
		double r = boomLevel;
		AABB area = new AABB(center.x - r, center.y - 0.5, center.z - r,
			center.x + r, center.y + 0.5, center.z + r);
		ChargerWaveProcessor boomProcessor = new ChargerWaveProcessor(level, boomLevel);

		// 范围内生物：受到该等级波对应的撞击伤害（低 4 / 高 6 / 伽马 8 / 伊普西龙 10 / 欧米伽 12）
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive())) {
			if (!(target instanceof Player player) || !player.isCreative()) {
				target.hurt(level.damageSources()
					.indirectMagic(source, null), damageForLevel(boomLevel));
			}
		}

		// 范围内掉落物：按爆炸等级直接加工
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive())) {
			boomProcessor.processItemEntity(item);
		}

		// 范围内方块（置物台/工作台等有物品槽者）：按爆炸等级直接加工
		int minX = Mth.floor(center.x - r);
		int maxX = Mth.floor(center.x + r);
		int minZ = Mth.floor(center.z - r);
		int maxZ = Mth.floor(center.z + r);
		int y = Mth.floor(center.y);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				BlockPos pos = new BlockPos(x, y, z);
				// 伽马能量加工（等级 ≥3）：范围内强化避雷针获得 1 次伽马充能
				if (boomLevel >= 3
					&& level.getBlockEntity(pos) instanceof ReinforcedLightningRodBlockEntity rod) {
					rod.onGammaWaveHit();
				}
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
				if (handler != null)
					boomProcessor.processBlockHandler(handler, pos);
			}
		}
	}

	/** 按等级取命中伤害（低 4 / 高 6 / 伽马 8 / 伊普西龙 10 / 欧米伽 12）。 */
	static float damageForLevel(int level) {
		return WaveLevels.damage(level);
	}

	/** 能量波粒子数据：原版染色粒子（可配 RGB，客户端无需任何注册）。 */
	static ParticleOptions waveParticle(Vec3 color, float scale) {
		return new DustParticleOptions(
			new Vector3f((float) color.x, (float) color.y, (float) color.z), scale);
	}
}
