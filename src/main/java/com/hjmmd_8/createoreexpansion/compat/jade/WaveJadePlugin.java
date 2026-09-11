package com.hjmmd_8.createoreexpansion.compat.jade;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity;
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
 * Jade 集成：在准星指向能量波实体时显示波特点（等级/速度/寿命）。
 *
 * <p><b>可选依赖（重要）</b>：本类<b>只在 Jade 存在时才会被加载</b>——
 * 入口 {@code WaveJadeCompat} 用 {@code ModList.isLoaded("jade")} + 反射
 * {@code Class.forName} 触发本类加载；未安装 Jade 时本类永不被 JVM 加载，
 * 不会出现"标注 optional 仍硬编码调用导致崩溃"的问题（农夫乐事教训）。</p>
 *
 * <p><b>显示内容</b>：</p>
 * <ul>
 *   <li>等级：低/高/伽马，文字颜色随波种类（{@link AbstractChargerWaveEntity#getWaveRenderColor()}）；</li>
 *   <li>运行速度（格/秒，含波速调节器修正）；</li>
 *   <li>剩余寿命（秒）；</li>
 *   <li>变体波（{@link StellarWaveEntity}）额外显示<b>可加工配方类型清单</b>（逐条译名，
 *       与波变器护目镜"最近波可加工"同表，见 {@link RecipeTypeNames}）与<b>携带载荷</b>：
 *       辅料物品（名称 ×数量）、电量（FE）、流体（名称+量）、避雷针释放机会次数、
 *       <b>携带加热档位</b>（变器扫描半径内点燃的烈焰燃烧室，见
 *       {@link StellarWaveEntity#getCarriedHeat()}）——各块独立判空，有才显示。</li>
 * </ul>
 *
 * <p><b>能力/载荷同步</b>：配方类型与载荷都是变器在服务端转换波时附加的<b>运行态字段</b>
 * （不落盘、不进 {@code SynchedEntityData}），客户端实体实例读不到。故本插件额外实现
 * {@link IServerDataProvider}，把两者经 Jade 服务端数据通道按需下发：data provider 只对
 * {@link StellarWaveEntity} 注册——普通波（{@link AbstractChargerWaveEntity} 其余实现）无
 * 对应 provider，Jade 根本不会为其发起数据请求，输出保持原样。</p>
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
	/** 物品行最多列出的种类数，超出以 "…" 省略。 */
	private static final int MAX_SHOWN_ITEM_KINDS = 3;

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

		// 等级：颜色随波种类（渲染颜色 RGB 转十六进制）
		String levelName = switch (wave.getWaveLevel()) {
			case 2 -> "高";
			case 3 -> "伽马";
			case 4 -> "伊普西龙";
			case 5 -> "欧米伽";
			default -> "低";
		};
		int color = colorOf(wave.getWaveRenderColor());
		tooltip.add(Component.translatable("createoreexpansion.jade.wave_level", levelName)
			.withStyle(ChatFormatting.WHITE)
			.withStyle(style -> style.withColor(color)));

		// 电荷状态（能量场作用前提）：正电荷 / 负电荷 / 未带电
		var charge = wave.getChargePolarity();
		MutableComponent chargeLine = charge == null
			? Component.translatable("createoreexpansion.jade.wave_charge_none")
			: Component.translatable("createoreexpansion.jade.wave_charge",
				Component.translatable(charge == com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE
					? "createoreexpansion.jade.charge_positive"
					: "createoreexpansion.jade.charge_negative"));
		tooltip.add(chargeLine.withStyle(charge == null ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW));

		// 运行速度（格/秒）
		tooltip.add(Component.translatable("createoreexpansion.jade.wave_speed",
			String.format("%.1f", wave.getWaveSpeed()))
			.withStyle(ChatFormatting.GRAY));

		// 剩余寿命（秒）
		double remaining = wave.getRemainingLifetime() / 20.0d;
		tooltip.add(Component.translatable("createoreexpansion.jade.wave_lifetime",
			String.format("%.1f", remaining))
			.withStyle(ChatFormatting.GRAY));

		// 变体波（星辉波变器产物）：先显示可加工配方类型（能力），再显示携带载荷。
		// 两类数据均为服务端运行态，客户端实例无值，一律读 Jade 服务端数据通道快照
		// （appendServerData）；各块独立判空，有才加行。
		if (wave instanceof StellarWaveEntity && accessor.getServerData() != null) {
			CompoundTag serverData = accessor.getServerData();

			// ===== 可加工配方类型（逐条列出；空则不加） =====
			ListTag types = serverData.getList(RECIPE_TYPES_KEY, Tag.TAG_STRING);
			if (!types.isEmpty()) {
				tooltip.add(Component.translatable("createoreexpansion.jade.stellar_wave_can_process")
					.withStyle(ChatFormatting.GRAY));
				for (int i = 0; i < types.size(); i++) {
					ResourceLocation id = ResourceLocation.tryParse(types.getString(i));
					if (id == null)
						continue;
					tooltip.add(Component.literal(" · ")
						.append(RecipeTypeNames.displayName(id))
						.withStyle(ChatFormatting.GRAY));
				}
			}

			// ===== 携带载荷（全空则不加） =====
			CompoundTag payload = serverData.getCompound(PAYLOAD_KEY);
			if (!payload.isEmpty()) {
				ListTag items = payload.getList(KEY_ITEMS, Tag.TAG_COMPOUND);
				if (!items.isEmpty()) {
					tooltip.add(Component.translatable("createoreexpansion.jade.stellar_wave_payload_items",
						payloadItemListText(items)).withStyle(ChatFormatting.GRAY));
				}
				// 携带加热（变器扫描半径内的烈焰燃烧室）：档位名 + 该档配色
				if (payload.contains(KEY_HEAT, Tag.TAG_INT)) {
					BlazeBurnerBlock.HeatLevel heat = HeatLevelNames.byOrdinal(payload.getInt(KEY_HEAT));
					tooltip.add(Component.translatable("createoreexpansion.jade.stellar_wave_payload_heat",
						HeatLevelNames.displayName(heat))
						.withStyle(style -> style.withColor(HeatLevelNames.colorOf(heat))));
				}
				// 携带转速（= 变器转速；Vintage 抛光 speed_limits 档位的判定依据）
				if (payload.contains(KEY_RPM, Tag.TAG_FLOAT)) {
					tooltip.add(Component.translatable("createoreexpansion.jade.stellar_wave_payload_rpm",
						(int) payload.getFloat(KEY_RPM)).withStyle(ChatFormatting.GRAY));
				}
				if (payload.contains(KEY_ENERGY, Tag.TAG_INT)) {
					tooltip.add(Component.translatable("createoreexpansion.jade.stellar_wave_payload_energy",
						payload.getInt(KEY_ENERGY)).withStyle(ChatFormatting.GRAY));
				}
				if (payload.contains(KEY_FLUID, Tag.TAG_STRING)) {
					Component fluidName = fluidNameComponent(payload.getString(KEY_FLUID));
					if (fluidName != null) {
						tooltip.add(Component.translatable("createoreexpansion.jade.stellar_wave_payload_fluid",
							fluidName, payload.getInt(KEY_FLUID_AMOUNT)).withStyle(ChatFormatting.GRAY));
					}
				}
				if (payload.contains(KEY_RODS, Tag.TAG_INT)) {
					tooltip.add(Component.translatable("createoreexpansion.jade.stellar_wave_payload_rods",
						payload.getInt(KEY_RODS)).withStyle(ChatFormatting.GRAY));
				}
			}
		}
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

	/** 物品行文本："名称 ×N" 逗号分隔，最多前 {@link #MAX_SHOWN_ITEM_KINDS} 种，超出省略为 "…"。 */
	private static Component payloadItemListText(ListTag items) {
		MutableComponent line = Component.empty();
		int shown = 0;
		boolean more = false;
		for (int i = 0; i < items.size(); i++) {
			CompoundTag entry = items.getCompound(i);
			ResourceLocation id = ResourceLocation.tryParse(entry.getString(KEY_ITEM_ID));
			if (id == null)
				continue;
			Item item = BuiltInRegistries.ITEM.getOptional(id)
				.orElse(Items.AIR);
			if (item == Items.AIR)
				continue; // 客户端注册表里找不到的物品：跳过该条目
			if (shown >= MAX_SHOWN_ITEM_KINDS) {
				more = true;
				break;
			}
			if (shown > 0)
				line.append(", ");
			line.append(item.getName(ItemStack.EMPTY)
				.copy()
				.append(" ×")
				.append(String.valueOf(entry.getInt(KEY_ITEM_COUNT))));
			shown++;
		}
		if (more)
			line.append(", …");
		return line;
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
