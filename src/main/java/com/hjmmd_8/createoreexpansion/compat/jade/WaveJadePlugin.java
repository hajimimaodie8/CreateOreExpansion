package com.hjmmd_8.createoreexpansion.compat.jade;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.hjmmd_8.createoreexpansion.util.RecipeTypeNames;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 集成：在准星指向能量波实体时显示<b>波情四项</b>（波速 / 波级 / 波载荷 / 波型）。
 *
 * <p><b>可选依赖（重要）</b>：本类<b>只在 Jade 存在时才会被加载</b>——
 * 入口 {@code WaveJadeCompat} 用 {@code ModList.isLoaded("jade")} + 反射
 * {@code Class.forName} 触发本类加载；未安装 Jade 时本类永不被 JVM 加载，
 * 不会出现"标注 optional 仍硬编码调用导致崩溃"的问题（农夫乐事教训）。</p>
 *
 * <p><b>显示内容</b>（行序固定，前四项为"波情"成组，其后才是附加读数）：</p>
 * <ol>
 *   <li><b>波速</b>（{@code jade.wave_speed}：格/秒，含波速调节器与能量场修正）；</li>
 *   <li><b>波级</b>（{@code jade.wave_level}：<b>只显示希腊字母</b> α/β/γ/ε/ω，
 *       符号由 {@link WaveLevels#glyph(int)} 统一给出，本类不做等级 switch；
 *       文字颜色随波种，见 {@link AbstractChargerWaveEntity#getWaveRenderColor()}）；</li>
 *   <li><b>波载荷</b>（{@code jade.wave_payload}：物品 n/上限 件 · m/上限 种 · 流体 · 电量，
 *       口径与护目镜"辅料载荷"一致；下方缩进一级挂明细：物品清单、携带加热、携带转速、引雷次数。
 *       空载显示"无（空载）"）；</li>
 *   <li><b>波型</b>（{@code jade.wave_type}：普通波/全能波/攻击波，名字查
 *       {@code WaveType#displayName()}——<b>不在本类里对波型 id 写 switch</b>）；</li>
 *   <li>其后依次：变体波"可加工"配方类型清单（{@link StellarWaveEntity} 独有）、
 *       剩余寿命（秒）、电荷状态（能量场作用前提）。</li>
 * </ol>
 *
 * <p><b>排版</b>：每行都经 {@link GoggleUtil#indented} 取护目镜同口径缩进
 * （Jade 的 {@code ITooltip} 不是 List，故不走 {@code forGoggles}，但缩进实现同源），
 * 明细行缩进一级，整个波情块左边界对齐。</p>
 *
 * <p><b>能力/载荷同步</b>：配方类型与载荷都是变器在服务端转换波时附加的<b>运行态字段</b>
 * （不落盘、不进 {@code SynchedEntityData}），客户端实体实例读不到。故本插件额外实现
 * {@link IServerDataProvider}，把两者经 Jade 服务端数据通道按需下发：data provider 只对
 * {@link StellarWaveEntity} 注册——普通波（{@link AbstractChargerWaveEntity} 其余实现）无
 * 对应 provider，Jade 根本不会为其发起数据请求，于是"波载荷"行按空载显示。</p>
 */
@WailaPlugin
public class WaveJadePlugin implements IWailaPlugin, IEntityComponentProvider, IServerDataProvider<EntityAccessor> {

	private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("createoreexpansion", "wave");

	/** 服务端载荷数据在共享 NBT 里的子键（独立键空间，避免与其它插件冲突）。 */
	private static final String PAYLOAD_KEY = "createoreexpansion:wave_payload";
	/** 服务端"可加工配方类型"数据在共享 NBT 里的子键（独立于载荷：无载荷的波也能显示能力）。 */
	private static final String RECIPE_TYPES_KEY = "createoreexpansion:wave_recipe_types";
	private static final String KEY_ITEMS = "items";
	private static final String KEY_ITEM_ID = "id";
	private static final String KEY_ITEM_COUNT = "count";
	private static final String KEY_FLUID = "fluid";
	private static final String KEY_FLUID_AMOUNT = "fluidAmount";
	private static final String KEY_ENERGY = "energy";
	private static final String KEY_RODS = "rods";
	/** 变器携带的加热档位（序号；见 {@code StellarWaveEntity#getCarriedHeat}）。 */
	private static final String KEY_HEAT = "carriedHeat";
	/** 变器携带的加工转速（RPM；转速档判定的依据）。 */
	private static final String KEY_RPM = "carriedRpm";
	/**
	 * 物品明细行最多列出的种类数（安全上限；实际种类上限由配置 {@code wave.maxPayloadKinds} 决定，
	 * 默认 5，这里留到 8 以免配置调大后又被截断）。
	 *
	 * <p>件数与种类数的完整口径见 {@link ItemPayload#of(ListTag)}。</p>
	 */
	private static final int MAX_SHOWN_ITEM_KINDS = 8;

	@Override
	public void register(IWailaCommonRegistration registration) {
		// 等级/速度/寿命在实体同步数据上可读，无需服务端数据；
		// 但变体波载荷（辅料/流体/电量/避雷针机会）为服务端运行态字段，客户端实例读不到，
		// 需经服务端数据通道按需下发（仅指向 StellarWaveEntity 时才发起请求与同步）。
		registration.registerEntityDataProvider(this, StellarWaveEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerEntityComponent(this, AbstractChargerWaveEntity.class);
	}

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		if (!(accessor.getEntity() instanceof AbstractChargerWaveEntity wave))
			return;

		// ===== 波情四项（统一成组；行序固定：波速 → 波级 → 波载荷 → 波型） =====
		// 每行都经 GoggleUtil.indented 取护目镜同口径缩进，块内左边界对齐；
		// 四项之外的信息（可加工/寿命/电荷）一律排在波情块之后。

		// 波情① 波速（格/秒）：含波速调节器叠加修正与能量场加速/偏转后的真实速率
		tooltip.add(GoggleUtil.indented(Component
			.translatable("createoreexpansion.jade.wave_speed", String.format("%.1f", wave.getWaveSpeed()))
			.withStyle(ChatFormatting.GRAY)));

		// 波情② 波级：只显示希腊字母（α/β/γ/ε/ω）——符号的唯一实现在 WaveLevels#glyph，
		// 本类不写等级 switch；文字颜色随波种（渲染颜色 RGB 转十六进制）
		tooltip.add(GoggleUtil.indented(Component
			.translatable("createoreexpansion.jade.wave_level", WaveLevels.displayName(wave.getWaveLevel()))
			.withStyle(ChatFormatting.WHITE)
			.withStyle(style -> style.withColor(colorOf(wave.getWaveRenderColor())))));

		// 波情③ 波载荷：概要行 + 缩进一级的明细行。
		// 载荷（辅料物品/流体/电量/避雷针机会/加热/转速）是变器在服务端转换波时附加的
		// 运行态字段，客户端实体实例读不到，一律读 Jade 服务端数据通道快照
		// （appendServerData）；非变体波无 provider，这里读到空载荷 → 显示"无（空载）"。
		CompoundTag serverData = accessor.getServerData();
		CompoundTag payload = serverData == null ? new CompoundTag() : serverData.getCompound(PAYLOAD_KEY);
		ItemPayload items = ItemPayload.of(payload.getList(KEY_ITEMS, Tag.TAG_COMPOUND));
		tooltip.add(GoggleUtil.indented(Component.translatable("createoreexpansion.jade.wave_payload",
			payloadSummary(payload, items))
			.withStyle(ChatFormatting.GRAY)));
		// 明细①：物品清单（"名称 ×N"，最多列 MAX_SHOWN_ITEM_KINDS 种，超出补 "…"）
		if (items.kinds() > 0) {
			tooltip.add(GoggleUtil.indented(1,
				Component.translatable("createoreexpansion.jade.stellar_wave_payload_item_list", items.list())
					.withStyle(ChatFormatting.GRAY)));
		}
		// 明细②：携带加热（变器扫描半径内点燃的烈焰燃烧室）：档位名 + 该档配色
		if (payload.contains(KEY_HEAT, Tag.TAG_INT)) {
			BlazeBurnerBlock.HeatLevel heat = HeatLevelNames.byOrdinal(payload.getInt(KEY_HEAT));
			tooltip.add(GoggleUtil.indented(1,
				Component.translatable("createoreexpansion.jade.stellar_wave_payload_heat",
					HeatLevelNames.displayName(heat))
					.withStyle(style -> style.withColor(HeatLevelNames.colorOf(heat)))));
		}
		// 明细③：携带转速（= 变器转速；Vintage 抛光 speed_limits 档位的判定依据）
		if (payload.contains(KEY_RPM, Tag.TAG_FLOAT)) {
			tooltip.add(GoggleUtil.indented(1,
				Component.translatable("createoreexpansion.jade.stellar_wave_payload_rpm",
					(int) payload.getFloat(KEY_RPM))
					.withStyle(ChatFormatting.GRAY)));
		}
		// 明细④：引雷次数（变器穿波时从蓄满的强化避雷针抽取；波打中哪里就在哪里落雷）
		if (payload.contains(KEY_RODS, Tag.TAG_INT)) {
			tooltip.add(GoggleUtil.indented(1,
				Component.translatable("createoreexpansion.jade.stellar_wave_payload_rods",
					payload.getInt(KEY_RODS))
					.withStyle(ChatFormatting.GRAY)));
		}

		// 波情④ 波型：普通波/全能波/攻击波——名字查 WaveType#displayName()（词条
		// createoreexpansion.wave_type.<path>，缺词条回退 path），本类不对波型 id 写 switch
		tooltip.add(GoggleUtil.indented(Component
			.translatable("createoreexpansion.jade.wave_type", wave.getWaveType()
				.displayName())
			.withStyle(ChatFormatting.GRAY)));

		// ===== 波情块之后的附加读数 =====

		// 可加工配方类型（变体波的能力清单，逐条译名；与波变器护目镜"最近波可加工"同表，
		// 见 RecipeTypeNames）；空则不加
		if (wave instanceof StellarWaveEntity && serverData != null) {
			ListTag types = serverData.getList(RECIPE_TYPES_KEY, Tag.TAG_STRING);
			if (!types.isEmpty()) {
				tooltip.add(GoggleUtil.indented(Component.translatable("createoreexpansion.jade.stellar_wave_can_process")
					.withStyle(ChatFormatting.GRAY)));
				for (int i = 0; i < types.size(); i++) {
					ResourceLocation id = ResourceLocation.tryParse(types.getString(i));
					if (id == null)
						continue;
					tooltip.add(GoggleUtil.indented(1, Component.literal(" · ")
						.append(RecipeTypeNames.displayName(id))
						.withStyle(ChatFormatting.GRAY)));
				}
			}
		}

		// 剩余寿命（秒）
		double remaining = wave.getRemainingLifetime() / 20.0d;
		tooltip.add(GoggleUtil.indented(Component
			.translatable("createoreexpansion.jade.wave_lifetime", String.format("%.1f", remaining))
			.withStyle(ChatFormatting.GRAY)));

		// 电荷状态（能量场作用前提）：正电荷 / 负电荷 / 未带电
		var charge = wave.getChargePolarity();
		MutableComponent chargeLine = charge == null
			? Component.translatable("createoreexpansion.jade.wave_charge_none")
			: Component.translatable("createoreexpansion.jade.wave_charge",
				Component.translatable(charge == com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE
					? "createoreexpansion.jade.charge_positive"
					: "createoreexpansion.jade.charge_negative"));
		tooltip.add(GoggleUtil.indented(chargeLine.withStyle(charge == null ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW)));
	}

	/**
	 * 服务端：把变体波携带的可加工配方类型 + 载荷写入共享数据（仅对
	 * {@link StellarWaveEntity} 注册；客户端指向普通波时不会有 provider 命中，
	 * 也就不存在本调用）。
	 *
	 * <p>配方类型与载荷分开两键下发：类型是波"能加工什么"的能力信息（即使没有
	 * 辅料/流体/电量载荷也要显示），载荷是波携带的物料资源——互不依赖。</p>
	 */
	@Override
	public void appendServerData(CompoundTag data, EntityAccessor accessor) {
		if (!(accessor.getEntity() instanceof StellarWaveEntity stellar))
			return;

		// ===== 可加工配方类型（波实际可执行全集，含状态选择与电量额外类型） =====
		try {
			ListTag types = new ListTag();
			for (var type : stellar.getActiveRecipeTypes()) {
				if (type == null || type.getId() == null)
					continue;
				String id = type.getId().toString();
				if (!containsString(types, id))
					types.add(StringTag.valueOf(id));
			}
			if (!types.isEmpty())
				data.put(RECIPE_TYPES_KEY, types);
		} catch (Throwable ignored) {
			// 集成异常（如可选 mod 缺失）：能力列表缺省为空，不影响载荷
		}

		CompoundTag payload = new CompoundTag();
		ListTag items = new ListTag();
		for (ItemStack stack : stellar.getPayloadItems()) {
			if (stack.isEmpty() || stack.getItem() == Items.AIR)
				continue;
			CompoundTag entry = new CompoundTag();
			entry.putString(KEY_ITEM_ID, BuiltInRegistries.ITEM.getKey(stack.getItem())
				.toString());
			entry.putInt(KEY_ITEM_COUNT, stack.getCount());
			items.add(entry);
		}
		if (!items.isEmpty())
			payload.put(KEY_ITEMS, items);

		var fluid = stellar.getPayloadFluid();
		if (!fluid.isEmpty()) {
			payload.putString(KEY_FLUID, BuiltInRegistries.FLUID.getKey(fluid.getFluid())
				.toString());
			payload.putInt(KEY_FLUID_AMOUNT, fluid.getAmount());
		}
		if (stellar.getPayloadEnergy() > 0)
			payload.putInt(KEY_ENERGY, stellar.getPayloadEnergy());
		if (stellar.getRodCharges() > 0)
			payload.putInt(KEY_RODS, stellar.getRodCharges());
		// 变器携带的加热档位（热源不是动能机器，客户端实体读不到：同样经服务端通道下发）
		if (stellar.getCarriedHeat() != BlazeBurnerBlock.HeatLevel.NONE)
			payload.putInt(KEY_HEAT, stellar.getCarriedHeat().ordinal());
		// 变器携带的加工转速（Vintage 抛光 speed_limits 转速档判定用；0 = 未携带，不显示）
		if (stellar.getCarriedRpm() > 0f)
			payload.putFloat(KEY_RPM, stellar.getCarriedRpm());

		if (!payload.isEmpty())
			data.put(PAYLOAD_KEY, payload);
	}

	/** ListTag（TAG_STRING）是否已含该字符串。 */
	private static boolean containsString(ListTag list, String value) {
		for (int i = 0; i < list.size(); i++)
			if (value.equals(list.getString(i)))
				return true;
		return false;
	}

	/**
	 * 波载荷概要行内容："物品 n/上限 件 · m/上限 种 · 流体 水 500 mB · 电量 1200 FE"
	 * （与护目镜"辅料载荷：n/上限 个 · m/上限 种"同口径）。
	 *
	 * <p>各段独立判空、有才拼；<b>一段都没有</b>（普通波/攻击波/空载变体波）时给"无（空载）"——
	 * 波情四项成组显示，空载也要占位说明，否则玩家无从分辨"没带东西"与"提示没读出来"。</p>
	 *
	 * <p>物品件数与种类数取自 {@code items}（与清单同一次解析，口径一致）；流体 id 解不出名称时
	 * 该段整段不显示（与旧行为一致，不显示裸 id）。</p>
	 */
	private static Component payloadSummary(CompoundTag payload, ItemPayload items) {
		MutableComponent summary = Component.empty();
		boolean any = false;
		if (items.kinds() > 0) {
			summary.append(Component.translatable("createoreexpansion.jade.stellar_wave_payload_items",
				items.count(), AllConfig.waveMaxPayloadItems, items.kinds(), AllConfig.waveMaxPayloadKinds));
			any = true;
		}
		Component fluidName = fluidNameComponent(payload.getString(KEY_FLUID));
		if (fluidName != null) {
			summary.append(separator(any))
				.append(Component.translatable("createoreexpansion.jade.stellar_wave_payload_fluid", fluidName,
					payload.getInt(KEY_FLUID_AMOUNT)));
			any = true;
		}
		if (payload.contains(KEY_ENERGY, Tag.TAG_INT)) {
			summary.append(separator(any))
				.append(Component.translatable("createoreexpansion.jade.stellar_wave_payload_energy",
					payload.getInt(KEY_ENERGY)));
			any = true;
		}
		return any ? summary : Component.translatable("createoreexpansion.jade.wave_payload_none");
	}

	/** 载荷概要段分隔符（" · "；首段前不加）。 */
	private static MutableComponent separator(boolean afterFirst) {
		return afterFirst ? Component.literal(" · ") : Component.empty();
	}

	/**
	 * 载荷物品的解析结果：可显示清单 + 件数 + 种类数（一次遍历同时给出三者）。
	 *
	 * <p>清单为"名称 ×N"逗号分隔，最多前 {@code MAX_SHOWN_ITEM_KINDS} 种，超出补 "…"；
	 * <b>件数与种类数始终按全量统计</b>（清单截断不影响到总量读数）。客户端注册表里找不到的条目
	 * 两边都不计——同一次解析保证"列出来的种类"与概要里的"m 种"不会互相矛盾。</p>
	 *
	 * <p><b>为什么概要必须给"n/上限 件 · m/上限 种"</b>（2026-09-11 修正，勿回退）：此前只在
	 * 清单后补 "…"，于是"箱子里有 3 种、每样还很充足"时玩家读到的是"波一次只能带 3 个物品"
	 * （用户实测反馈），而实际载荷是 5 件 3 种。</p>
	 */
	private record ItemPayload(MutableComponent list, int count, int kinds) {

		/** 解析载荷物品列表（{@code {id, count}} 复合列表）。 */
		static ItemPayload of(ListTag items) {
			MutableComponent list = Component.empty();
			int count = 0;
			int kinds = 0;
			boolean truncated = false;
			for (int i = 0; i < items.size(); i++) {
				CompoundTag entry = items.getCompound(i);
				ResourceLocation id = ResourceLocation.tryParse(entry.getString(KEY_ITEM_ID));
				if (id == null)
					continue;
				Item item = BuiltInRegistries.ITEM.getOptional(id)
					.orElse(Items.AIR);
				if (item == Items.AIR)
					continue; // 客户端注册表里找不到的物品：跳过该条目（件数/种类数同样不计）
				kinds++;
				count += entry.getInt(KEY_ITEM_COUNT);
				if (kinds > MAX_SHOWN_ITEM_KINDS) {
					truncated = true;
					continue;
				}
				if (kinds > 1)
					list.append(", ");
				list.append(item.getName(ItemStack.EMPTY)
					.copy()
					.append(" ×")
					.append(String.valueOf(entry.getInt(KEY_ITEM_COUNT))));
			}
			if (truncated)
				list.append(", …");
			return new ItemPayload(list, count, kinds);
		}
	}

	/** 按流体注册表 id 取本地化名称（找不到返回 null = 不显示该行）。 */
	private static Component fluidNameComponent(String fluidId) {
		ResourceLocation id = ResourceLocation.tryParse(fluidId);
		if (id == null)
			return null;
		Fluid fluid = BuiltInRegistries.FLUID.getOptional(id)
			.orElse(null);
		return fluid == null ? null : fluid.getFluidType()
			.getDescription();
	}

	/** RGB(0-1) → 十六进制颜色（用于等级文字着色）。 */
	private static int colorOf(Vec3 rgb) {
		if (rgb == null)
			return 0xFFFFFF;
		int r = (int) (rgb.x * 255) & 0xFF;
		int g = (int) (rgb.y * 255) & 0xFF;
		int b = (int) (rgb.z * 255) & 0xFF;
		return (r << 16) | (g << 8) | b;
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}
}
