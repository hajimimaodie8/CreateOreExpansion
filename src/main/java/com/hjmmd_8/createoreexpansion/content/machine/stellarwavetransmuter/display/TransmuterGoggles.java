package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.display;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.hjmmd_8.createoreexpansion.util.RecipeTypeNames;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>星辉波变器的护目镜面板</b>（2026-09 从 {@code StellarWaveTransmuterBlockEntity} 抽出，分包整理的一部分）。
 *
 * <p>只做"把读数渲染成行"这件事：读数由方块实体在调用点打包成 {@link Readout}，
 * 所以本类<b>不反向依赖方块实体的私有字段</b>，也不需要实例。</p>
 *
 * <p><b>读数口径（历史修复，勿回退）</b>：一律只报"<b>读到了什么</b>"——加热档位、设备台数、
 * 储能量、载荷量、配方类型数；<b>绝不显示方块坐标</b>（用户 2026-09 明确要求）。</p>
 *
 * <p>行序：半径 → 加热 → 载荷源设备（物品/流体容器、储能）→ 加工机数与应力 → 波加工转速 →
 * 绑定机器可加工配方（Shift 展开清单）→ 载荷概览（类型数/辅料·流体·电量/避雷针）→
 * 最近一波可加工属性（Shift）。</p>
 */
public final class TransmuterGoggles {

	private TransmuterGoggles() {
	}

	/** 配方类型清单最多列出的项数（超出补"等"），避免逐条列把提示框挤满缩窄。 */
	private static final int MAX_LISTED_TYPES = 10;

	/**
	 * 护目镜面板所需的全部读数（调用点打包，见 {@code StellarWaveTransmuterBlockEntity#addToGoggleTooltip}）。
	 *
	 * @param scanRadius        当前扫描半径（能量场档位决定）
	 * @param speed             本机转速（绝对值；= 波加工转速）
	 * @param heat              半径内最高热档（NONE = 没读到点着的烈焰燃烧室）
	 * @param machineCount      读到的加工机台数
	 * @param machineStress     被扫动能机实时应力之和（不含乘子）
	 * @param scannedTypeIds    当前可加工配方类型 id（Shift 展开）
	 * @param recipeTypeCount   可加工配方类型数
	 * @param payloadItemCount  载荷辅料件数 / {@code payloadTypeCount} 种类数
	 * @param payloadFluidMb    载荷流体 mB / {@code payloadEnergyFe} 载荷电量 FE
	 * @param rodCount          半径内已蓄满待释放的强化避雷针台数
	 * @param lastWaveTypeIds   最近一波穿出时实际可执行的全部类型（Shift）
	 */
	public record Readout(int scanRadius, float speed, BlazeBurnerBlock.HeatLevel heat,
		int itemContainers, int fluidContainers, int energyStorages, int energyStoredFe,
		int machineCount, float machineStress, List<ResourceLocation> scannedTypeIds, int recipeTypeCount,
		int payloadItemCount, int payloadTypeCount, int payloadFluidMb, int payloadEnergyFe, int rodCount,
		List<ResourceLocation> lastWaveTypeIds) {
	}

	/**
	 * 渲染面板（"半径"那一行起，直到"最近一波"行）。
	 *
	 * @param sneaking 玩家是否正按住 Shift（护目镜渲染时机即为按键状态）
	 * @return 恒 true（与 {@code IHaveGoggleInformation} 的约定一致）
	 */
	public static boolean append(List<Component> tooltip, Readout r, boolean sneaking) {
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_radius",
			r.scanRadius()).withStyle(ChatFormatting.AQUA));
		// 加热读数（烈焰燃烧室）：范围内有没有点着火的燃烧室、是哪一档——
		// 修复前这里什么都不显示，玩家只能看到"加热配方不生效"却查不出原因。
		if (r.heat() == BlazeBurnerBlock.HeatLevel.NONE) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_heat_none")
					.withStyle(ChatFormatting.DARK_GRAY));
		} else {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_heat",
					HeatLevelNames.displayName(r.heat()))
					.withStyle(style -> style.withColor(HeatLevelNames.colorOf(r.heat()))));
		}
		// 载荷源设备读数（只报种类/数量，不报坐标）：读到才显示，没扫到不占版面
		if (r.itemContainers() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_item_containers",
					r.itemContainers()).withStyle(ChatFormatting.GRAY));
		}
		if (r.fluidContainers() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_fluid_containers",
					r.fluidContainers()).withStyle(ChatFormatting.GRAY));
		}
		if (r.energyStorages() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_energy_storages",
					r.energyStorages(), r.energyStoredFe()).withStyle(ChatFormatting.GRAY));
		}
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_machines",
			r.machineCount()).withStyle(ChatFormatting.GRAY));
		// 波加工转速（= 本机转速）：Vintage 抛光配方的 speed_limits 档位判定依据（见设计文档 §5）
		GoggleUtil.forGoggles(tooltip,
			Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_wave_rpm",
				(int) Math.abs(r.speed())).withStyle(ChatFormatting.GRAY));
		if (r.machineCount() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_stress",
					(int) r.machineStress()).withStyle(ChatFormatting.GRAY));
		}
		// ===== 绑定机器可加工配方（Shift 提示行；按住时该行高亮并逐条列出配方清单） =====
		if (r.machineCount() > 0) {
			var keyShift = Component.translatable("create.tooltip.keyShift")
				.withStyle(sneaking ? ChatFormatting.WHITE : ChatFormatting.GRAY);
			var bindLine = Component.translatable(sneaking
				? "createoreexpansion.goggles.stellar_wave_transmuter_bind_hint_shift"
				: "createoreexpansion.goggles.stellar_wave_transmuter_bind_hint", r.machineCount(), keyShift);
			if (sneaking)
				bindLine.withStyle(ChatFormatting.WHITE);
			GoggleUtil.forGoggles(tooltip, bindLine);
			if (sneaking && !r.scannedTypeIds()
				.isEmpty()) {
				// 单行顿号连接（最多 10 项，超出补"等"）——避免逐条列把提示框撑窄挤满
				GoggleUtil.forGoggles(tooltip, 1, Component.literal(" · ")
					.append(joinedTypeNames(r.scannedTypeIds()))
					.withStyle(ChatFormatting.GRAY));
			}
		}
		// ===== 载荷概览摘要（配方类型/辅料·流体·电量/避雷针） =====
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_types",
			r.recipeTypeCount()).withStyle(ChatFormatting.GOLD));
		if (r.payloadItemCount() > 0) {
			// 与 Jade 同口径：带上"上限"（2026-09 审计修复）——否则配置调小上限后
			// 看不出"是抽满了还是只抽到这些"，也会与 Jade 的 "n/上限 件" 对不上。
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_items",
					r.payloadItemCount(), AllConfig.waveMaxPayloadItems, r.payloadTypeCount(),
					AllConfig.waveMaxPayloadKinds).withStyle(ChatFormatting.GRAY));
		}
		if (r.payloadFluidMb() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_fluid", r.payloadFluidMb())
					.withStyle(ChatFormatting.GRAY));
		}
		if (r.payloadEnergyFe() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_energy", r.payloadEnergyFe())
					.withStyle(ChatFormatting.GRAY));
		}
		if (r.rodCount() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_rods", r.rodCount())
					.withStyle(ChatFormatting.GRAY));
		}
		// ===== 最近波携带的可加工属性（仅按住 Shift 时显示；单行顿号连接） =====
		if (sneaking && !r.lastWaveTypeIds()
			.isEmpty()) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_last_wave")
					.withStyle(ChatFormatting.GRAY));
			GoggleUtil.forGoggles(tooltip, 1, Component.literal(" · ")
				.append(joinedTypeNames(r.lastWaveTypeIds()))
				.withStyle(ChatFormatting.GRAY));
		}
		return true;
	}

	/** 配方类型显示名**单行汇总**：顿号连接，最多 {@value #MAX_LISTED_TYPES} 项，超出补"等"。 */
	public static Component joinedTypeNames(List<ResourceLocation> typeIds) {
		MutableComponent line = Component.empty();
		int shown = Math.min(typeIds.size(), MAX_LISTED_TYPES);
		for (int i = 0; i < shown; i++) {
			if (i > 0)
				line.append(Component.translatable("createoreexpansion.goggles.list_separator"));
			line.append(RecipeTypeNames.displayName(typeIds.get(i)));
		}
		if (typeIds.size() > MAX_LISTED_TYPES)
			line.append(Component.translatable("createoreexpansion.goggles.list_etc"));
		return line;
	}
}
