package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry.MachineRegistration;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 星辉波变器"加工机注册中心"（唯一入口，统一三张旧表：认可名单 / 方块→配方类型 /
 * 状态选择器）。
 *
 * <p><b>快速接入</b>：任何 mod（含本仓库）要让自己的一台动能机器可被星辉波变器识别，
 * 只需一行链式注册——其它逻辑（应力口径、载荷排除、变体波配方执行）全部自动接入：</p>
 *
 * <pre>{@code
 * // 压机（Create 原生）
 * StellarWaveMachineRegistry.register(AllBlocks.MECHANICAL_PRESS.get())
 *     .addTypes(AllRecipeTypes.PRESSING);
 *
 * // 真空室（Vintage，可附加模式选择器）
 * StellarWaveMachineRegistry.register(VintageBlocks.VACUUM_CHAMBER.get())
 *     .addTypes(VintageRecipes.PRESSURIZING, VintageRecipes.VACUUMIZING)
 *     .withSelector(VintageImprovementsMachineIntegration::vacuumChamberTypes);
 * }</pre>
 *
 * <p><b>与全库执行的关系</b>：变体波的配方执行已是全库检索（Create RecipeFinder），
 * 本表不再作为执行开关；注册的 {@code types} 仅用于<b>展示口径</b>（护目镜/Jade
 * "最近波可加工"）与<b>模式选择器</b>（如真空室加压/抽真空按机器实时 mode 二选一）。</p>
 *
 * <p>未注册的动能处理机仍会被扫描启发式识别（见变器 BE），本表负责精确名单与选择器。</p>
 */
public final class StellarWaveMachineRegistry {

	/** 一台机器的注册档案：展示类型 + 可选状态选择器。 */
	public record MachineEntry(ResourceLocation blockId, List<IRecipeTypeInfo> types, MachineStateSelector selector) {
	}

	/** 链式注册对象：{@code register(block).addTypes(...).withSelector(...)}。 */
	public static final class MachineRegistration {
		private final ResourceLocation blockId;
		private final List<IRecipeTypeInfo> types = new ArrayList<>();
		private MachineStateSelector selector;

		private MachineRegistration(ResourceLocation blockId) {
			this.blockId = blockId;
		}

		/** 追加该机器可执行的配方类型（展示/模式口径；可多次调用）。 */
		public MachineRegistration addTypes(IRecipeTypeInfo... newTypes) {
			if (newTypes == null)
				return this;
			for (IRecipeTypeInfo t : newTypes)
				if (t != null && !types.contains(t))
					types.add(t);
			return this;
		}

		/** 可选：按机器实时状态裁剪/替换配方类型（例：真空室 mode → 加压/抽真空）。 */
		public MachineRegistration withSelector(MachineStateSelector s) {
			this.selector = s;
			return this;
		}

		/** 完成注册（内部落表；幂等——同方块重复注册覆盖旧档案）。 */
		public void register() {
			ENTRIES.put(blockId,
				new MachineEntry(blockId, List.copyOf(types), selector));
		}
	}

	private static final Map<ResourceLocation, MachineEntry> ENTRIES = new HashMap<>();

	private StellarWaveMachineRegistry() {
	}

	// ================= 公开入口 =================

	/** 开始注册一台加工机（链式：{@code register(block).addTypes(...).register()}）。 */
	public static MachineRegistration register(Block block) {
		return new MachineRegistration(BuiltInRegistries.BLOCK.getKey(block));
	}

	/** 开始注册一台加工机（按方块 id；供无法直接引用方块对象的场景）。 */
	public static MachineRegistration register(ResourceLocation blockId) {
		return new MachineRegistration(blockId);
	}

	// ================= 查询（供扫描/展示/载荷口径） =================

	/** 该方块是否是被注册认可的加工机。 */
	public static boolean isRegistered(Block block) {
		return block != null && ENTRIES.containsKey(BuiltInRegistries.BLOCK.getKey(block));
	}

	/** 该方块 id 是否是被注册认可的加工机。 */
	public static boolean isRegistered(ResourceLocation blockId) {
		return blockId != null && ENTRIES.containsKey(blockId);
	}

	/** 该机器静态档案的配方类型（只读；未注册返回空表）。 */
	public static List<IRecipeTypeInfo> typesFor(ResourceLocation blockId) {
		MachineEntry e = ENTRIES.get(blockId);
		return e == null ? List.of() : e.types();
	}

	/** 按机器实时状态解析出的当前配方类型（应用选择器；未注册 = 空表）。 */
	public static List<IRecipeTypeInfo> resolve(KineticBlockEntity machine, ResourceLocation blockId) {
		MachineEntry e = ENTRIES.get(blockId);
		if (e == null)
			return List.of();
		List<IRecipeTypeInfo> base = e.types();
		if (e.selector() == null)
			return base;
		try {
			List<IRecipeTypeInfo> resolved = e.selector()
				.typesFor(machine, base);
			return resolved == null ? base : resolved;
		} catch (Throwable ignored) {
			return base; // 选择器异常（类缺失等）：沿用静态档案
		}
	}

	/** 只读快照（诊断/护目镜显示用）。 */
	public static Map<ResourceLocation, MachineEntry> entries() {
		return Collections.unmodifiableMap(ENTRIES);
	}

	/** 已注册机器数（护目镜显示 / 统计用）。 */
	public static int count() {
		return ENTRIES.size();
	}

	/** 拉取某配方类型在当前世界的全部配方（引擎用；数据包重载后自动取新列表）。 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<RecipeHolder<?>> recipesOf(Level level, IRecipeTypeInfo type) {
		return new ArrayList<>(
			(List) level.getRecipeManager()
				.getAllRecipesFor(type.getType()));
	}
}
