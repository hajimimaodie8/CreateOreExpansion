package com.hjmmd_8.createoreexpansion.compat.jade;

import java.util.LinkedHashMap;
import java.util.Map;

import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * <b>工作盆内容的"实时"Jade 行</b>（可选 Jade 集成的第二个插件，见 {@code CreateOreExpansion} 的反射加载）。
 *
 * <p><b>为什么需要它</b>：Jade 的方块物品列表走的是<b>服务端数据快照</b>——只在它认为需要时向服务端要一次数据
 * （其内置物品存储提供者的 {@code shouldRequestData} 受自身配置与方块类型约束），所以"盯着盆看"时它可能一直
 * 显示你第一次瞄上它时的旧内容；而工程师护目镜读的是客户端实时副本（Create 每次内容变化都会
 * {@code SyncedStackHandler.onContentsChanged → notifyUpdate} 推送），因此表现为"Jade 旧、护目镜新"。</p>
 *
 * <p><b>做法</b>：给工作盆挂一个本模组的方块数据提供者，并在 {@link #shouldRequestData} 里恒返回 true——
 * Jade 于是<b>逐 tick</b>重新向服务端取数据；本提供者把盆内物品（输入 9 槽 + 输出 9 槽，按物品聚合）
 * 与流体按<b>当下</b>状态写进独立键空间，客户端渲染成"工作盆内容（实时）"一行标题 + 每物品一行。
 * 这样星辉波变器加工完盆内物品后，Jade 会立刻跟着变。</p>
 *
 * <p><b>与 Jade 自带行的关系</b>：本行是<b>额外</b>的（不改动、也无法改动 Jade 自己的行）；
 * 若只想看实时行，可在 Jade 的插件配置里关掉它自带的物品行。</p>
 */
@WailaPlugin
public class BasinLiveJadePlugin implements IWailaPlugin, IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

	private static final ResourceLocation UID =
		ResourceLocation.fromNamespaceAndPath("createoreexpansion", "basin_live");

	/** 服务端数据键（独立键空间，避免与其它插件冲突）。 */
	private static final String KEY_ITEMS = "createoreexpansion:basin_live_items";
	private static final String KEY_FLUIDS = "createoreexpansion:basin_live_fluids";

	/** 最多列出的物品种类数（超出以 "…" 省略），避免提示框被撑爆。 */
	private static final int MAX_ITEM_KINDS = 8;

	@Override
	public void register(IWailaCommonRegistration registration) {
		// 服务端数据提供者：挂在工作盆方块实体上（Jade 的 Class<?> 参数接受 BE 类）
		registration.registerBlockDataProvider(this, BasinBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(this, BasinBlock.class);
	}

	/**
	 * 逐 tick 取数：<b>本方法返回 true 就等于"这个方块我要实时数据"</b>——
	 * 这是让 Jade 跟随变器加工结果刷新的关键（默认为按需快照）。
	 */
	@Override
	public boolean shouldRequestData(BlockAccessor accessor) {
		return true;
	}

	/** 服务端：把工作盆<b>当下</b>的内容（物品按 物品id=数量 聚合；流体按 流体id=mb）写进共享 NBT。 */
	@Override
	public void appendServerData(CompoundTag data, BlockAccessor accessor) {
		if (!(accessor.getBlockEntity() instanceof BasinBlockEntity basin))
			return;
		if (basin.getLevel() == null)
			return;
		try {
			Map<ResourceLocation, Integer> byItem = new LinkedHashMap<>();
			countItems(basin.getInputInventory(), byItem);
			countItems(basin.getOutputInventory(), byItem);
			if (!byItem.isEmpty()) {
				ListTag list = new ListTag();
				for (Map.Entry<ResourceLocation, Integer> e : byItem.entrySet())
					if (e.getKey() != null && e.getValue() > 0)
						list.add(StringTag.valueOf(e.getKey() + "=" + e.getValue()));
				if (!list.isEmpty())
					data.put(KEY_ITEMS, list);
			}
			IFluidHandler tanks =
				basin.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, basin.getBlockPos(), null);
			if (tanks != null) {
				Map<ResourceLocation, Integer> byFluid = new LinkedHashMap<>();
				for (int i = 0; i < tanks.getTanks(); i++) {
					FluidStack fs = tanks.getFluidInTank(i);
					if (fs.isEmpty())
						continue;
					byFluid.merge(BuiltInRegistries.FLUID.getKey(fs.getFluid()), fs.getAmount(), Integer::sum);
				}
				if (!byFluid.isEmpty()) {
					ListTag list = new ListTag();
					for (Map.Entry<ResourceLocation, Integer> e : byFluid.entrySet())
						if (e.getKey() != null && e.getValue() > 0)
							list.add(StringTag.valueOf(e.getKey() + "=" + e.getValue()));
					if (!list.isEmpty())
						data.put(KEY_FLUIDS, list);
				}
			}
		} catch (Throwable ignored) {
			// 数据收集异常：本行不显示（不影响 Jade 其它内容）
		}
	}

	/** 把一个物品容器按"物品 id → 总数"累加。 */
	private static void countItems(IItemHandler handler, Map<ResourceLocation, Integer> out) {
		if (handler == null)
			return;
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack == null || stack.isEmpty())
				continue;
			out.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), Integer::sum);
		}
	}

	/** 客户端：渲染"工作盆内容（实时）"标题 + 每个物品一行（名称 ×数量）。 */
	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		CompoundTag data = accessor.getServerData();
		ListTag items = data.getList(KEY_ITEMS, Tag.TAG_STRING);
		ListTag fluids = data.getList(KEY_FLUIDS, Tag.TAG_STRING);
		if (items.isEmpty() && fluids.isEmpty())
			return;

		tooltip.add(Component.translatable("createoreexpansion.jade.basin_live")
			.withStyle(ChatFormatting.GRAY));

		int shown = 0;
		for (int i = 0; i < items.size(); i++) {
			String entry = items.getString(i);
			int at = entry.lastIndexOf('=');
			if (at <= 0)
				continue;
			if (shown >= MAX_ITEM_KINDS) {
				tooltip.add(Component.literal(" · …").withStyle(ChatFormatting.DARK_GRAY));
				break;
			}
			ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, at));
			Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id)
				.orElse(null);
			if (item == null || item == Items.AIR)
				continue;
			tooltip.add(Component.literal(" · ")
				.append(Component.translatable(item.getDescriptionId())
					.withStyle(ChatFormatting.GRAY))
				.append(Component.literal(" ×" + entry.substring(at + 1))
					.withStyle(ChatFormatting.GREEN)));
			shown++;
		}

		for (int i = 0; i < fluids.size(); i++) {
			String entry = fluids.getString(i);
			int at = entry.lastIndexOf('=');
			if (at <= 0)
				continue;
			ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, at));
			if (id == null)
				continue;
			net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID.getOptional(id)
				.orElse(null);
			if (fluid == null)
				continue;
			tooltip.add(Component.literal(" · ")
				.append(fluid.getFluidType()
					.getDescription()
					.copy()
					.withStyle(ChatFormatting.GRAY))
				.append(Component.literal(" " + entry.substring(at + 1) + " mB")
					.withStyle(ChatFormatting.BLUE)));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}
}
