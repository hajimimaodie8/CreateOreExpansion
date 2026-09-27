package com.hjmmd_8.createoreexpansion.content.charger.block;

import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>三台应力充能器的方块 / 方块实体槽位表（第一层侧）</b>（W6-a）。
 *
 * <p><b>它解决什么问题</b>：充能器的方块与方块实体登记目前在
 * {@code common/registry/cews/CewsBlocks} / {@code CewsBlockEntityTypes} 里，而那两个类
 * <b>横跨两层</b>（充能器 3 条属于第一层，波机器 14/11 条属于第二层）——W6-c 会把它们拆开
 * （见 {@code build/patch/w6-restructure-PLAN.md} §1.2 的"拆分 4 个"）。在拆开之前，
 * 第一层的 {@code ChargingRecipe} / 三台充能器 Block 直接读那两个类，就是一条
 * {@code L1 → L2} 禁止边（§2.2 的 E13 / E14）。</p>
 *
 * <p><b>做法</b>：把"充能器那几条登记"提前抽成一张<b>第一层自己的槽位表</b>（本类），
 * 由登记方在类初始化时 {@code install*} 注入（方向恒为「L2 登记类 → L1 表」，合法）；
 * 第一层的读取方只认本表。W6-c 把充能器登记整体搬进第一层之后，注入方随之变成第一层
 * 自己的登记类，<b>读取方一行都不用改</b>——正是"先让工具看见目标边界"的落点。</p>
 *
 * <p>未注入时访问器返回 {@code null}：那意味着"本环境没有第二层，充能器方块根本没有被登记"，
 * 读取方都做了空值跳过（不会 NPE）。</p>
 */
public final class ChargerBlockSlots {

	private static Supplier<JadeStressChargerBlock> jadeBlock;
	private static Supplier<SapphireStressChargerBlock> sapphireBlock;
	private static Supplier<StellarstoneStressChargerBlock> stellarstoneBlock;

	private static Supplier<BlockEntityType<? extends JadeStressChargerBlockEntity>> jadeBlockEntity;
	private static Supplier<BlockEntityType<? extends SapphireStressChargerBlockEntity>> sapphireBlockEntity;
	private static Supplier<BlockEntityType<? extends StellarstoneStressChargerBlockEntity>> stellarstoneBlockEntity;

	/** 默认方块状态（JEI 动画用；这两台的方块状态由登记方以 {@code BlockEntry#getDefaultState()} 给出）。 */
	private static Supplier<BlockState> jadeDefaultState;
	private static Supplier<BlockState> sapphireDefaultState;

	private ChargerBlockSlots() {
	}

	// ================= 注入（登记方调用；幂等，只覆盖引用） =================

	public static void installJadeBlock(Supplier<JadeStressChargerBlock> block) {
		jadeBlock = block;
	}

	public static void installSapphireBlock(Supplier<SapphireStressChargerBlock> block) {
		sapphireBlock = block;
	}

	public static void installStellarstoneBlock(Supplier<StellarstoneStressChargerBlock> block) {
		stellarstoneBlock = block;
	}

	public static void installJadeBlockEntity(Supplier<BlockEntityType<? extends JadeStressChargerBlockEntity>> type) {
		jadeBlockEntity = type;
	}

	public static void installSapphireBlockEntity(
		Supplier<BlockEntityType<? extends SapphireStressChargerBlockEntity>> type) {
		sapphireBlockEntity = type;
	}

	public static void installStellarstoneBlockEntity(
		Supplier<BlockEntityType<? extends StellarstoneStressChargerBlockEntity>> type) {
		stellarstoneBlockEntity = type;
	}

	public static void installJadeDefaultState(Supplier<BlockState> state) {
		jadeDefaultState = state;
	}

	public static void installSapphireDefaultState(Supplier<BlockState> state) {
		sapphireDefaultState = state;
	}

	// ================= 读取（第一层） =================

	/** 翡翠应力充能器方块（未注入返回 {@code null}）。 */
	public static JadeStressChargerBlock jadeBlock() {
		return jadeBlock == null ? null : jadeBlock.get();
	}

	/** 蓝宝石应力充能器方块（未注入返回 {@code null}）。 */
	public static SapphireStressChargerBlock sapphireBlock() {
		return sapphireBlock == null ? null : sapphireBlock.get();
	}

	/** 星辉石应力充能器方块（未注入返回 {@code null}）。 */
	public static StellarstoneStressChargerBlock stellarstoneBlock() {
		return stellarstoneBlock == null ? null : stellarstoneBlock.get();
	}

	/** 与 {@link #jadeBlock()} 同物，供 JEI"所需机器"列表用。 */
	public static ItemLike jadeMachine() {
		return jadeBlock();
	}

	/** 与 {@link #sapphireBlock()} 同物，供 JEI"所需机器"列表用。 */
	public static ItemLike sapphireMachine() {
		return sapphireBlock();
	}

	public static BlockEntityType<? extends JadeStressChargerBlockEntity> jadeBlockEntity() {
		return jadeBlockEntity == null ? null : jadeBlockEntity.get();
	}

	public static BlockEntityType<? extends SapphireStressChargerBlockEntity> sapphireBlockEntity() {
		return sapphireBlockEntity == null ? null : sapphireBlockEntity.get();
	}

	public static BlockEntityType<? extends StellarstoneStressChargerBlockEntity> stellarstoneBlockEntity() {
		return stellarstoneBlockEntity == null ? null : stellarstoneBlockEntity.get();
	}

	/** 翡翠充能器默认方块状态（JEI 动画；未注入返回 {@code null}）。 */
	public static BlockState jadeDefaultState() {
		return jadeDefaultState == null ? null : jadeDefaultState.get();
	}

	/** 蓝宝石充能器默认方块状态（JEI 动画；未注入返回 {@code null}）。 */
	public static BlockState sapphireDefaultState() {
		return sapphireDefaultState == null ? null : sapphireDefaultState.get();
	}
}
