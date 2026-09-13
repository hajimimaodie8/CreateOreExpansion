package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry.MachineRegistration;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

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

	/**
	 * 目录里出现过的<b>全部配方类型</b>（去重，按首次出现顺序）。
	 *
	 * <p>用途：变体波读档恢复时把存下来的类型 id 还原成 {@code IRecipeTypeInfo} 档案
	 * （见 {@code StellarWaveEntity#readAdditionalSaveData}）——避免读档后类型集为空、
	 * 类型门退回"按机器 id 展开静态档案"而丢掉状态选择器（真空室 mode / 杠杆锤锤下方块 / 磨轮等级）。</p>
	 */
	public static List<IRecipeTypeInfo> allRecipeTypes() {
		List<IRecipeTypeInfo> out = new ArrayList<>();
		for (MachineEntry e : ENTRIES.values())
			for (IRecipeTypeInfo t : e.types())
				if (t != null && t.getId() != null && !out.contains(t))
					out.add(t);
		return out;
	}

	/**
	 * <b>"这个位置的方块算不算加工机"——变器扫描与波侧取料排除的<b>唯一口径</b></b>
	 * （2026-09 统一：此前两边各写一份，导致 registry 登记的<b>非动能</b>加工机
	 * ——Create 注液器 Spout / 物品排放器 Item Drain——被变器当载荷源抽、却被波侧跳过）。
	 *
	 * <p>判据：① 目录已登记（含非动能机）→ 是；② 否则若是 Create 动能方块，排除纯传动/结构件
	 * （轴/齿轮箱/大小齿轮/传送带/离合/变速器/活塞/轴承/龙门/底盘等）后视为加工机候选，
	 * 使"任意 mod 的动能加工机摆在旁边即可被识别"，无需逐 mod 登记。</p>
	 *
	 * <p>调用方需自行保证区块已加载（本方法直接读方块状态）。</p>
	 */
	public static boolean isMachinery(Level level, BlockPos pos) {
		if (level == null || pos == null)
			return false;
		BlockState state = level.getBlockState(pos);
		if (isRegistered(state.getBlock()))
			return true;
		if (!(state.getBlock() instanceof com.simibubi.create.content.kinetics.base.KineticBlock))
			return false;
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock())
			.toString();
		// 纯传动/结构件黑名单：不算加工机（变器不能靠一根轴就"识别出加工能力"）。
		// 2026-09 补齐漏项：vertical_gearbox / adjustable_chain_gearshift / sequenced_gearshift
		// （它们不以 gearbox/gearshift 开头，旧的前缀匹配漏掉了）。
		if (id.startsWith("create:vertical_gearbox") || id.startsWith("create:adjustable_chain_gearshift")
			|| id.startsWith("create:sequenced_gearshift"))
			return false;
		return !(id.startsWith("create:shaft") || id.startsWith("create:gearbox")
			|| id.startsWith("create:cogwheel") || id.startsWith("create:large_cogwheel")
			|| id.startsWith("create:belt") || id.startsWith("create:clutch")
			|| id.startsWith("create:gearshift") || id.startsWith("create:mechanical_piston")
			|| id.startsWith("create:mechanical_bearing") || id.startsWith("create:gantry")
			|| id.startsWith("create:linear_chassis") || id.startsWith("create:radial_chassis"));
	}

	/**
	 * 按机器实时状态解析出的当前配方类型（应用选择器；未注册 = 空表）。
	 *
	 * <p>参数是 {@link BlockEntity} 而非 {@code KineticBlockEntity}：加工机不全是动能机
	 * （Create 注液器 Spout / 物品排放器 Item Drain 都是 {@code SmartBlockEntity}），
	 * 只收动能机会导致 {@code filling}/{@code emptying} 类型永远进不了波（2026-09 修正）。</p>
	 */
	public static List<IRecipeTypeInfo> resolve(BlockEntity machine, ResourceLocation blockId) {
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
