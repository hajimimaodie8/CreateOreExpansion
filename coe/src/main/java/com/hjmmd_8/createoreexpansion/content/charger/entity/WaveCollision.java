package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

import net.minecraft.world.phys.Vec3;

import static com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity.sameFiringBatch;

/**
 * <b>波波碰撞</b>（2026-10-06 行为零变化拆分，从 {@link AbstractChargerWaveEntity} 的
 * {@code handleWaveCollision} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>两枚波在命中盒里相遇时怎么办</b>——同批豁免，否则
 * 相互湮灭 + 在相遇点触发范围能量爆炸。等级对与最终效果由这里唯一的
 * {@code min} 决定，<b>与波级/波型无关</b>（AGENTS 长期口径）。</p>
 *
 * <p><b>两个触发点共用这一个入口</b>：① 主动侧——本波在自己的 {@code tick()} 里查到命中盒内
 * 还有一只波（{@code waves.get(0)}）并调用本方法；② 被动侧——对方自己的 {@code tick()} 也跑了
 * 同一段查询，于是它同样会调用本方法（由 {@code collided} 标记防重）。判据放在<b>本方法最开头</b>：
 * 只要两波同批 ⇒ 双方都直接 return ⇒ 无论"谁先跑到"，结果都是"谁都不爆"。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、宿主字段/方法被搬动
 * 逼出的 {@code host.} 限定（{@code waveLevel} / {@code collided} / {@code renderColor}
 * 三个字段仍住在 {@link AbstractChargerWaveEntity}，只是由 {@code private} 放宽到包级私有）；
 * {@code sameFiringBatch} 是宿主的<b>公开</b>静态方法，本类改用静态导入（签名与实现一个字未动）。
 * <b>豁免范围（只有双方都非 0 的同一批次）、爆炸等级取 min、碰撞点取两波中心中点、
 * 以及 {@code triggerBoom → 日志 → 双方 collided → 双方 discard} 的顺序一个字未动。</b></p>
 */
final class WaveCollision {

	private WaveCollision() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 波波碰撞：两个能量波相遇时相互湮灭，在相遇点触发范围能量爆炸。
	 *
	 * <p>爆炸特性：</p>
	 * <ul>
	 *   <li><b>爆炸等级</b> = 两个波等级的较小值（min）；</li>
	 *   <li><b>爆炸范围</b> = 以碰撞点为中心、半径 = 爆炸等级的水平正方形区域
	 *       （半径 1 → 3×3，半径 2 → 5×5，半径 3 → 7×7），不破坏地形；</li>
	 *   <li><b>区域内生物</b>：受到该等级波撞击生物的等量伤害（α 4 / β 6 / γ 8）；</li>
	 *   <li><b>区域内掉落物 / 置物台物品</b>：按爆炸等级直接执行充能加工（复用
	 *       {@link ChargerWaveProcessor}，含能量工具充能与普通物品配方转化）；</li>
	 *   <li><b>粒子</b>：比撞墙绽放（30 个）更密集的爆炸扩散粒子。</li>
	 * </ul>
	 *
	 * @param host  本次遍历自己这一侧的波
	 * @param other 碰撞的另一个波
	 */
	static void handleWaveCollision(AbstractChargerWaveEntity host, AbstractChargerWaveEntity other) {
		// ================== 同批豁免（用户 2026-10-02 星界轮，需求 §3.3(f)） ==================
		// 两侧触发点都用得着这一条，理由见下面"两侧触发点"那段注释：
		//   ① 主动侧：本波在自己的 tick 里查到命中盒内还有一只波（tick() 里的 waves.get(0)）并调用本方法；
		//   ② 被动侧：本波自己的 tick 也跑了同一段查询，于是它同样会调用本方法（由 collided 标记防重）。
		// 判据放在**本方法最开头**：只要两波同批 ⇒ 双方都直接 return ——
		// 不触发 triggerBoom、不记 waveDiag、不置 collided、不 discard。于是无论"谁先跑到"，
		// 结果都是"谁都不爆"（这正是"双向"的含义：豁免不是靠某一侧的特判，而是两波共用的同一个入口）。
		// ⚠ 豁免范围**只有**同批次：批次号 0（机器波、别人的波）不参与，见 sameFiringBatch 的说明。
		if (sameFiringBatch(host, other)) {
			// 诊断日志：豁免不是"没撞上"，而是"撞上了但按同批跳过"——出事时这一行能直接区分两者。
			// 节流：并排飞的多枚波彼此一直在命中盒里，若每 tick 打一行会把事件流日志刷爆。
			if (host.tickCount % 20 == 0) {
				WaveDiag.trace("波波碰撞豁免（同一次发射，批次 {}）：{} 级 × {} 级 相遇但互不爆炸、互不湮灭",
					host.getFiringBatch(), WaveLevels.glyph(host.waveLevel), WaveLevels.glyph(other.waveLevel));
			}
			return;
		}
		// =====================================================================================

		int boomLevel = Math.min(host.waveLevel, other.waveLevel);
		// 碰撞点取两波中心中点
		Vec3 center = host.position().add(other.position()).scale(0.5);

		// 1. 范围爆炸：粒子 + 音效 + 区域效果
		ChargerWaveFx.triggerBoom(host.level(), host, center, host.trailStyle(), host.renderColor,
			other.renderColor, boomLevel);

		// 轨迹日志（事件流：一次碰撞一行）：用户口径是"任意两列波（不管波级）撞上就必须有影响"，
		// 这一行把"到底撞没撞上、按哪一级结算"写进日志——出事时能直接分辨"没撞上"与"撞上了没效果"。
		// 粒子数走 ChargerWaveFx.boomParticleCount（唯一算式），日志里的数与真实发出的数必然一致。
		WaveDiag.trace(
			"波波碰撞：{} 级 × {} 级（波型 {} × {}）→ 爆炸等级 {}：半径 {} 格范围伤害 {}、范围内掉落物/置物台按该级加工、粒子 {} 颗、不破坏地形",
			WaveLevels.glyph(host.waveLevel), WaveLevels.glyph(other.waveLevel), host.getWaveType()
				.id()
				.getPath(),
			other.getWaveType()
				.id()
				.getPath(),
			WaveLevels.glyph(boomLevel), boomLevel, (int) WaveLevels.damage(boomLevel),
			ChargerWaveFx.boomParticleCount(boomLevel));

		// 2. 两波相互湮灭（标记防对方同 tick 重复触发）
		host.collided = true;
		other.collided = true;
		other.discard();
		host.discard();
	}
}
