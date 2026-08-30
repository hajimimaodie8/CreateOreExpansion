package com.hjmmd_8.createoreexpansion.content.crystal;

/**
 * 可生长水晶 —— 统一生长参数配置（模组内唯一修改点，改数值即改生长速度，无需配置文件）。
 *
 * <p>4 种宝石（翡翠/黄玉/蓝宝石/星辉石）的生长速度都在本文件统一定义，
 * {@code AllBlocks} 只负责引用，不改数值。</p>
 *
 * <p>参数说明：</p>
 * <ul>
 *     <li><b>芽床保底生芽间隔（秒）</b>：芽床方块实体进度制，每 N 秒保证尝试生成一颗小芽
 *     （不受随机刻运气影响；另有随机刻自然生长叠加，AE2 催生器加速走随机刻入口）；</li>
 *     <li><b>芽每阶段平均生长时间（秒）</b>：小芽→中芽→大芽→簇 每阶段一次，
 *     由随机刻按此秒数换算推进概率（随机刻平均约 68.27 秒一次；默认 340 秒 ≈ 1/5，原版节奏）。</li>
 * </ul>
 */
public final class CrystalGrowthConfigs {

	private CrystalGrowthConfigs() {
	}

	// ========== 芽床保底生芽间隔（秒） ==========

	/** 翡翠芽床保底生芽间隔（秒） */
	public static final int JADE_BUDDING_SECONDS = 120;
	/** 黄玉芽床保底生芽间隔（秒） */
	public static final int TOPAZ_BUDDING_SECONDS = 240;
	/** 蓝宝石芽床保底生芽间隔（秒） */
	public static final int SAPPHIRE_BUDDING_SECONDS = 300;
	/** 星辉石芽床保底生芽间隔（秒） */
	public static final int STELLARSTONE_BUDDING_SECONDS = 400;

	// ========== 芽每阶段平均生长时间（秒） ==========

	/** 翡翠芽每阶段平均生长时间（秒） */
	public static final float JADE_BUD_STAGE_SECONDS = 680.0F;
	/** 黄玉芽每阶段平均生长时间（秒） */
	public static final float TOPAZ_BUD_STAGE_SECONDS = 1380.0F;
	/** 蓝宝石芽每阶段平均生长时间（秒） */
	public static final float SAPPHIRE_BUD_STAGE_SECONDS = 1680.0F;
	/** 星辉石芽每阶段平均生长时间（秒） */
	public static final float STELLARSTONE_BUD_STAGE_SECONDS = 2200.0F;
}
