package com.hjmmd_8.createoreexpansion.common.registry.coe.charger;

import com.hjmmd_8.createoreexpansion.client.renderer.CreateChargerRenderer;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.content.charger.block.ChargerBlockSlots;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * <b>三台应力充能器的方块实体登记（第一层侧）</b>（W6-c）。
 *
 * <h2>一、它为什么存在</h2>
 * <p>与 {@link CoeChargerBlocks} 同源：这三条 {@code BlockEntityType} 原先住在
 * {@code common/registry/cews/CewsBlockEntityTypes}（第二层的类），但充能器的方块实体
 * <b>属于第一层</b>。{@code CewsBlockEntityTypes} 因此被按层拆成两半：这一半随 {@code :coe} 走，
 * 另外 11 台机器（能量场控制器 / 星辉波变器 / 调级器 ×3 / 波速调节器 ×3 / 差波器 ×3）留在原类。</p>
 *
 * <p><b>纯搬运</b>：三条登记的注册 id、{@code validBlocks}、渲染器绑定与拆分前逐字相同；
 * 改动只有三处、都在"层归属"上：① Registrate 实例换成 {@link CoeRegistrate#REGISTRATE}；
 * ② {@code validBlocks} 指向 {@link CoeChargerBlocks} 的同名条目（同一个对象）；
 * ③ 渲染器 {@link CreateChargerRenderer} 随第一层渲染栈搬进
 * {@code client.renderer}（原先在 {@code client.renderer.cews}）。</p>
 *
 * <h2>二、静态块为什么在类尾</h2>
 * <p>把 3 台充能器的方块实体槽位注入第一层表 {@link ChargerBlockSlots}
 * （W6-a 建的间接层；三台充能器的 Block 的 {@code IBE#getBlockEntityType()} 只认那张表）。
 * 用 {@code Supplier} 而不是直接 {@code .get()}：本类静态初始化期 {@code DeferredHolder}
 * 尚未绑定，取值必须延迟到运行期真正被读的那一刻。</p>
 */
public final class CoeChargerBlockEntityTypes {

	public static final BlockEntityEntry<JadeStressChargerBlockEntity> JADE_STRESS_CHARGER = CoeRegistrate.REGISTRATE
		.blockEntity("jade_stress_charger", JadeStressChargerBlockEntity::new)
		.validBlocks(CoeChargerBlocks.JADE_STRESS_CHARGER)
		.renderer(() -> CreateChargerRenderer::new)
		.register();

	/** 蓝宝石应力充能器方块实体（双模式：普通连续发射 / 储存簇射；渲染与翡翠共用 CreateChargerRenderer） */
	public static final BlockEntityEntry<SapphireStressChargerBlockEntity> SAPPHIRE_STRESS_CHARGER =
		CoeRegistrate.REGISTRATE
			.blockEntity("sapphire_stress_charger", SapphireStressChargerBlockEntity::new)
			.validBlocks(CoeChargerBlocks.SAPPHIRE_STRESS_CHARGER)
			.renderer(() -> CreateChargerRenderer::new)
			.register();

	/** 星辉石应力充能器方块实体（手动发射等级 + 双模式；渲染与翡翠/蓝宝石共用 CreateChargerRenderer） */
	public static final BlockEntityEntry<StellarstoneStressChargerBlockEntity> STELLARSTONE_STRESS_CHARGER =
		CoeRegistrate.REGISTRATE
			.blockEntity("stellarstone_stress_charger", StellarstoneStressChargerBlockEntity::new)
			.validBlocks(CoeChargerBlocks.STELLARSTONE_STRESS_CHARGER)
			.renderer(() -> CreateChargerRenderer::new)
			.register();

	/** 把 3 台充能器的方块实体槽位注入第一层表（见类注释"二"）。 */
	static {
		ChargerBlockSlots.installJadeBlockEntity(() -> JADE_STRESS_CHARGER.get());
		ChargerBlockSlots.installSapphireBlockEntity(() -> SAPPHIRE_STRESS_CHARGER.get());
		ChargerBlockSlots.installStellarstoneBlockEntity(() -> STELLARSTONE_STRESS_CHARGER.get());
	}

	/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private CoeChargerBlockEntityTypes() {
	}
}
