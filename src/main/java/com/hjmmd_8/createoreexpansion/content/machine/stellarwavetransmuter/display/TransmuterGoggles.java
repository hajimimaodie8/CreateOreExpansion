package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.display;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.TransmuterMode;
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
 * <p>行序（用户 2026-09 规格：<b>机器名称行放最前面</b>）：调用方先输出<b>名称行</b>
 * （{@code createoreexpansion.goggles.stellar_wave_transmuter}，灰色）→ 再输出<b>模式行</b>
 * （{@link #appendModeLine}，全类唯一输出点）→ 然后才是我方读数行：加工态由 {@link #append} 渲染
 * （半径 → 加热 → 载荷源设备（物品/流体容器、储能）→ 加工机数与应力 → 波加工转速 →
 * 绑定机器可加工配方（Shift 展开清单）→ 载荷概览（类型数/辅料·流体·电量/避雷针）→
 * 最近一波可加工属性（Shift）），攻击态由 {@link #appendAttackReadout} 渲染（转速 + 需求区间 →
 * 档位 + 场盒半径 → 场作用说明）→ 最后由调用方追加 Create 的动能行
 * （{@code super.addToGoggleTooltip} 的"动能统计/应力影响"），<b>排在末尾</b>。</p>
 *
 * <p><b>两态读数不混</b>：哪一套读数由<b>模式自报</b>
 * （{@link TransmuterMode#appendReadout}）——攻击态按住 Shift 看到的是攻击态的三行，
 * <b>不会串到加工态那堆读数</b>，调用方一行 {@code if (mode == ATTACK)} 都没有。</p>
 */
public final class TransmuterGoggles {

	private TransmuterGoggles() {
	}

	/** 配方类型清单最多列出的项数（超出补"等"），避免逐条列把提示框挤满缩窄。 */
	private static final int MAX_LISTED_TYPES = 10;

	/**
	 * 护目镜面板所需的全部读数（调用点打包，见 {@code StellarWaveTransmuterBlockEntity#addToGoggleTooltip}）。
	 *
	 * <p><b>不含处理模式</b>：模式行由调用方经 {@link #appendModeLine} 单独输出（它必须在
	 * 任何状态下都显示，而本 record 只在"按 Shift 且门槛满足"时才会被构造）。</p>
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
	 * <b>模式行</b>（本机信息块的第一行）——全类<b>只有这一处</b>输出"当前是什么模式"。
	 *
	 * <p>由调用方（{@code StellarWaveTransmuterBlockEntity#addToGoggleTooltip} 与
	 * {@code #addToTooltip}）在<b>进入面板之前</b>先调一次：这台变器现在在干什么，两态对波的行为
	 * 完全不同（一个加工、一个是攻击场），玩家第一眼要看到的就是它；而且它在"没按 Shift"、
	 * "转速为 0"、"转速不足"这几种面板内容各不相同的状态下都必须出现。</p>
	 *
	 * <p><b>为什么从 {@link #append} 里挪出来</b>：{@link #append} 只在"已接入应力且转速达到本模式
	 * 门槛"时才被调用，而这行要求恒显示；两边各写一遍就会在按 Shift 时出现<b>两行模式</b>。
	 * 所以模式行收在本方法里、由调用方统一先输出，{@link #append} 从"半径"行起。</p>
	 */
	public static void appendModeLine(List<Component> tooltip, TransmuterMode mode) {
		GoggleUtil.forGoggles(tooltip,
			Component.translatable(mode.translationKey())
				.withStyle(mode.displayColor()));
	}

	/**
	 * 渲染面板（从"半径"那一行起，直到"最近一波"行）。
	 *
	 * <p><b>调用时机（2026-09-14 用户定稿，取代上一版"三档按次数展开"）</b>：只有玩家<b>按住 Shift</b>
	 * <b>且</b>本机转速达到了当前模式的门槛（{@code StellarWaveTransmuterBlockEntity#isSpeedRequirementFulfilled()}）
	 * 时才会被调用——没按住时调用方只放"机器名 + 模式行 + 一行『按住 Shift 查看机器详情』"。
	 * 按住 Shift 就<b>一次性把全部读数显示出来</b>，不再有"再按一次才给全量"的隐藏档位
	 * （上一版那个机制不可发现：用户实测反馈"面板里出现『查看概要』字样，可它本来就要按住 Shift 才看得见"）。</p>
	 *
	 * <p><b>模式行不在这里</b>：它由调用方用 {@link #appendModeLine} 先输出（见该方法的说明），
	 * 本方法从"半径"行开始，避免同一个面板里出现两行模式。</p>
	 *
	 * @param sneaking 玩家是否正按住 Shift（护目镜渲染时机即为按键状态；恒为 true 才走到这里）
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
		// ===== 绑定机器可加工配方（按住 Shift 展开时整份清单直接列出） =====
		if (r.machineCount() > 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_bind_hint", r.machineCount())
					.withStyle(ChatFormatting.WHITE));
			if (!r.scannedTypeIds()
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
		// ===== 最近波携带的可加工属性（单行顿号连接） =====
		if (!r.lastWaveTypeIds()
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

	/**
	 * <b>攻击波变态的读数（按住 Shift 时）</b>——用户 2026-09 规格：攻击态按住 Shift
	 * <b>不该显示加工态的内容</b>，只显示攻击态自己的三行。
	 *
	 * <p>三行（顺序即显示顺序）：</p>
	 * <ol>
	 *   <li><b>转速行</b>：当前转速 + 需求区间（区间两端取模式自报的
	 *       {@link TransmuterMode#minimumRpm()} 与 {@link TransmuterMode#attackTierCeilingRpm()}，
	 *       现为 128 ~ 256 RPM）；</li>
	 *   <li><b>档位/攻击场半径行</b>：第 X 档 + 场盒半径 N 格；</li>
	 *   <li><b>说明行</b>：范围内的普通波穿过即被点燃为攻击波。</li>
	 * </ol>
	 *
	 * <p><b>档位与半径读的是规格一的那个映射本身</b>（{@link TransmuterMode#attackTier} /
	 * {@link TransmuterMode#attackFieldRadius}）——与攻击场判定<b>同一处实现</b>，
	 * 本方法一个数字都不重算；唯一的加工是把 0 基的档位序号 {@code +1} 变成玩家看到的"第 X 档"。</p>
	 *
	 * <p>调用链：{@code StellarWaveTransmuterBlockEntity#addToGoggleTooltip} →
	 * {@link TransmuterMode#appendReadout}（模式自报）→ 本方法（攻击态那一支）。</p>
	 *
	 * @param mode  当前模式（必为攻击波变态；档位/半径/区间都由它自报，本方法不判断模式）
	 * @param speed 本机当前转速（RPM，可能为负；显示与判档都取绝对值）
	 */
	public static void appendAttackReadout(List<Component> tooltip, TransmuterMode mode, float speed) {
		// ① 转速行：当前转速 + 需求区间（两个端点都自报，显示层不写 128/256）
		GoggleUtil.forGoggles(tooltip,
			Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_attack_rpm",
				(int) Math.abs(speed), (int) mode.minimumRpm(), (int) mode.attackTierCeilingRpm())
				.withStyle(ChatFormatting.GRAY));
		// ② 档位/半径行：两个数都来自 attackTier 那一个映射（半径就是档位序号，见 attackFieldRadius）
		GoggleUtil.forGoggles(tooltip,
			Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_attack_tier",
				mode.attackTier(speed) + 1, mode.attackFieldRadius(speed)).withStyle(ChatFormatting.RED));
		// ③ 说明行：这个场到底做什么（攻击态没有加工态那些"半径/加热/载荷"读数可说）
		GoggleUtil.forGoggles(tooltip,
			Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_attack_field_hint")
				.withStyle(ChatFormatting.DARK_GRAY));
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
