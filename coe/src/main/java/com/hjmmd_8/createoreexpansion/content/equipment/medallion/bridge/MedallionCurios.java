package com.hjmmd_8.createoreexpansion.content.equipment.medallion.bridge;

import com.hjmmd_8.createoreexpansion.content.equipment.medallion.item.BaseStressMedallionItem;

import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 凝能佩可选联动（Curios）的<b>桥接注册表 + 两支物品类选择器</b>（本类不 import 任何 Curios 类型）。
 *
 * <p><b>未装 Curios</b>：{@link #get()} 返回 {@code null} →
 * {@link #item(String, NonNullFunction)} 交回"纯物品支线"工厂、{@link #findEquipped} 恒返回空；
 * <b>装了 Curios</b>：主类 {@code Class.forName} 触发
 * {@link com.hjmmd_8.createoreexpansion.compat.curios.CurioMedallionBridge} 的静态块调用 {@link #set}，
 * 之后物品走"饰品支线"、找佩走 Curios 库存。</p>
 *
 * <p>注册 id / 显示名 / 贴图 / 组件 / 标签两支一律相同 —— 玩家侧无感，老存档读进来还是同一个物品。</p>
 */
public final class MedallionCurios {

	/** 支线类型标识（{@link MedallionCuriosBridge#create} 的分发键，必须与注册 id 一一对应）。 */
	public static final String KIND_JADE = "jade";
	public static final String KIND_TOPAZ = "topaz";
	public static final String KIND_SAPPHIRE = "sapphire";
	public static final String KIND_NETHERITE = "netherite";
	public static final String KIND_STELLARSTONE = "stellarstone";
	public static final String KIND_THUNDERITE = "thunderite";

	private static volatile MedallionCuriosBridge instance;

	private MedallionCurios() {
	}

	/** 注册桥接实现（仅 {@code compat.curios.CurioMedallionBridge} 的静态块在 Curios 已安装时调用）。 */
	public static void set(MedallionCuriosBridge bridge) {
		instance = bridge;
	}

	/** 当前桥接实现；{@code null} = Curios 未安装（调用方必须判空）。 */
	@Nullable
	public static MedallionCuriosBridge get() {
		return instance;
	}

	/** Curios 是否在场（仅用于日志/诊断，不要拿它替代判空）。 */
	public static boolean isPresent() {
		return instance != null;
	}

	/**
	 * <b>两支物品类的选择器</b>：装了 Curios 用饰品支线，否则用调用方给的纯物品支线。
	 *
	 * <p>本方法在 {@code CoeItems} 的静态块里被调用（物品工厂构造期），此时只读 {@link #instance}，
	 * <b>不触碰任何 Curios 类</b>；真正的 {@code create(...)} 发生在注册期，桥接早已就绪。</p>
	 *
	 * @param kind     佩种标识（{@code KIND_*}）
	 * @param fallback 纯物品支线的工厂（无 Curios 时原样返回）
	 * @return 实际使用的物品工厂
	 */
	@SuppressWarnings("unchecked")
	public static <T extends BaseStressMedallionItem> NonNullFunction<Item.Properties, T> item(
		String kind, NonNullFunction<Item.Properties, T> fallback) {
		MedallionCuriosBridge bridge = instance;
		if (bridge == null) {
			return fallback;
		}
		// 唯一一处未检查转换：桥接实现按 kind 返回的饰品支线类，都 extends 对应的纯物品支线类
		// （CurioMedallionItems.Jade extends JadeStressMedallionItem 等），kind 与调用点一一对应。
		return properties -> (T) bridge.create(kind, properties);
	}

	/** 玩家 Curios 槽位中的指定物品；未装 Curios / 未佩戴一律返回 {@link ItemStack#EMPTY}。 */
	public static ItemStack findEquipped(@Nullable Player player, @Nullable Item item) {
		MedallionCuriosBridge bridge = instance;
		if (bridge == null || player == null || item == null) {
			return ItemStack.EMPTY;
		}
		return bridge.findEquipped(player, item);
	}
}
