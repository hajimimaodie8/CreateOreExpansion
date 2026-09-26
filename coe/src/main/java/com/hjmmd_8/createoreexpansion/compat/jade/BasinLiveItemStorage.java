package com.hjmmd_8.createoreexpansion.compat.jade;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

/**
 * <b>工作盆的"实时内容"物品存储</b>：把 Jade 自带的物品行<b>整条接管</b>，用盆的当下内容渲染
 * （而不是再加一行文字）。服务端与客户端各实现一半，Jade 靠 uid 把两侧对上。
 *
 * <h2>为什么能"顶掉"原生行</h2>
 * Jade 15 的物品数据来自 {@code WailaCommonRegistration.itemStorageProviders} 这个
 * {@code WrappedHierarchyLookup}：它的 {@code wrappedGet} 先按<b>方块类</b>查、再按<b>目标对象类</b>查，
 * 每张表内按优先级升序排列，{@code CommonProxy.getServerExtensionData} 取<b>第一个 getGroups 非 null</b>
 * 的提供者。Jade 自带的通用物品存储注册在 {@code Block.class} 上、优先级 9999（Jade 里<b>数值越小越优先</b>）；
 * 本条注册在 {@code BasinBlock.class} 上、优先级 1000，位置更靠前 + 优先级更高 —— 所以工作盆只会用本条，
 * 原生物品行由本条的数据渲染，形制（图标 + 数量）与原生完全一致
 * （客户端渲染直接复用 {@link ItemStorageProvider.Extension#getClientGroups}）。
 *
 * <p><b>注意注册的类是方块类而不是方块实体类</b>：按方块实体类注册会落到 {@code wrappedGet} 里
 * "最后才查的目标对象表"，那时光方块类的那条（通用物品存储）已经先返回了，优先级再高也轮不到。</p>
 *
 * <h2>为什么"实时"</h2>
 * 原生的 {@code shouldRequestData} 受 Jade 自身配置与缓存（{@code targetCache} 60s /
 * {@code containerCache} 120s）影响，只要它认为"不必再问"，盯着看就一直是你第一次瞄上它时的旧内容；
 * 而护目镜读的是客户端实时副本，于是表现为"Jade 旧、护目镜新"（用户 2026-09 实测）。
 * 本条 {@link #shouldRequestData} <b>恒返回 true</b> → Jade 逐 tick 重新向服务端要一次数据，
 * 数据在 {@link #getGroups} 里<b>当场</b>从盆的能力读（不走 Jade 的 ItemCollector 缓存）。
 */
public final class BasinLiveItemStorage
	implements IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {

	/** 本提供者的 uid：服务端写数据、客户端认数据都靠它（见 {@code ViewGroup.listCodec} 的键）。 */
	public static final ResourceLocation UID =
		ResourceLocation.fromNamespaceAndPath("createoreexpansion", "basin_live_item_storage");

	/** 单例（Jade 只按 uid 认数据，实例复用即可）。 */
	public static final BasinLiveItemStorage INSTANCE = new BasinLiveItemStorage();

	/**
	 * 提供者优先级——<b>Jade 里数值越小越优先</b>（实测字节码：方块名 -10100 最先、通用物品存储与
	 * "模组名" 9999 最后），所以这里取 1000 压过通用物品存储的 9999。
	 */
	private static final int PRIORITY = 1000;

	private BasinLiveItemStorage() {
	}

	@Override
	public ResourceLocation getUid() {
		return UID;
	}

	@Override
	public int getDefaultPriority() {
		return PRIORITY;
	}

	/** 恒 true：工作盆的物品行永远按"当下"取数（这是"实时"的关键，见类注释）。 */
	@Override
	public boolean shouldRequestData(Accessor<?> accessor) {
		return true;
	}

	/**
	 * 服务端：当场读盆的物品能力（= {@code CombinedInvWrapper(输入 9 槽, 输出 9 槽)}，
	 * 与原生行展示的是同一份内容），按"物品 + 组件"跨槽聚合成一件、数量相加。
	 *
	 * @return 分组；<b>null = 交给 Jade 原生行</b>（读不到盆 / 出异常时保守回退，避免整条提示框消失）
	 */
	@Override
	public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
		try {
			BasinBlockEntity basin = basinOf(accessor);
			if (basin == null)
				return null;
			IItemHandler handler = handlerOf(basin, accessor);
			if (handler == null)
				return null;
			List<ItemStack> merged = new ArrayList<>();
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				ItemStack stack = handler.getStackInSlot(slot);
				if (stack == null || stack.isEmpty())
					continue;
				boolean mergedIn = false;
				for (ItemStack acc : merged) {
					if (ItemStack.isSameItemSameComponents(acc, stack)) {
						acc.grow(stack.getCount());
						mergedIn = true;
						break;
					}
				}
				if (!mergedIn)
					merged.add(stack.copy());
			}
			// 空盆返回空分组（= 不显示物品行），与原生"空容器不出行"一致
			return List.of(new ViewGroup<>(merged));
		} catch (Throwable ignored) {
			return null; // 判定异常：交回原生行，绝不因本模组把提示框搞崩
		}
	}

	/** 客户端：渲染口径完全交给 Jade 原生实现（图标 + 数量样式与原生物品行一致）。 */
	@Override
	public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor,
		List<ViewGroup<ItemStack>> groups) {
		return ItemStorageProvider.Extension.INSTANCE.getClientGroups(accessor, groups);
	}

	private static BasinBlockEntity basinOf(Accessor<?> accessor) {
		if (accessor instanceof BlockAccessor block && block.getBlockEntity() instanceof BasinBlockEntity basin)
			return basin;
		return accessor.getTarget() instanceof BasinBlockEntity basin ? basin : null;
	}

	private static IItemHandler handlerOf(BasinBlockEntity basin, Accessor<?> accessor) {
		Level level = basin.getLevel() != null ? basin.getLevel() : accessor.getLevel();
		if (level == null)
			return null;
		BlockPos pos = basin.getBlockPos();
		return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
	}
}
