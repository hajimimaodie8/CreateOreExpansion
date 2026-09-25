package com.hjmmd_8.createoreexpansion.compat.curios;

import com.hjmmd_8.createoreexpansion.content.equipment.medallion.BaseStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.bridge.MedallionCurios;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.bridge.MedallionCuriosBridge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * 凝能佩 ↔ Curios 的桥接<b>实现</b>：本模组唯一允许出现 Curios API 调用的地方
 * （另一处是同样在 {@code compat.curios} 下的 {@link CurioMedallionItems} 的类型声明）。
 *
 * <p><b>加载约束</b>：本类直接引用 Curios 类型，只能在 Curios 已安装时被加载 ——
 * 主类 {@code CreateOreExpansion} 在 {@code ModList.isLoaded("curios")} 为真时
 * {@code Class.forName} 触发本类的静态块，把 {@link #INSTANCE} 注册进 {@link MedallionCurios}。
 * 未装 Curios 时本类永不进入 JVM（与 {@code compat.sable.SableSubLevelBridge} 的隔离范式一致）。</p>
 */
public final class CurioMedallionBridge implements MedallionCuriosBridge {

	public static final CurioMedallionBridge INSTANCE = new CurioMedallionBridge();

	static {
		// 类被加载（主类 Class.forName 触发）即注册到桥接注册表
		MedallionCurios.set(INSTANCE);
	}

	private CurioMedallionBridge() {
	}

	/** 按佩种挑选饰品支线类（注册 id 与纯物品支线完全一致，只有实现类不同）。 */
	@Override
	public BaseStressMedallionItem create(String kind, Item.Properties properties) {
		return switch (kind) {
			case MedallionCurios.KIND_JADE -> new CurioMedallionItems.Jade(properties);
			case MedallionCurios.KIND_TOPAZ -> new CurioMedallionItems.Topaz(properties);
			case MedallionCurios.KIND_SAPPHIRE -> new CurioMedallionItems.Sapphire(properties);
			case MedallionCurios.KIND_NETHERITE -> new CurioMedallionItems.Netherite(properties);
			case MedallionCurios.KIND_STELLARSTONE -> new CurioMedallionItems.Stellarstone(properties);
			case MedallionCurios.KIND_THUNDERITE -> new CurioMedallionItems.Thunderite(properties);
			default -> throw new IllegalArgumentException("未知的凝能佩支线类型: " + kind);
		};
	}

	/** 逐字沿用改前 {@code IMedallion#findBoundMedallion / #isWearing} 里的 Curios 查询写法。 */
	@Override
	public ItemStack findEquipped(Player player, Item item) {
		return CuriosApi.getCuriosInventory(player)
			.flatMap(inv -> inv.findFirstCurio(item))
			.map(SlotResult::stack)
			.orElse(ItemStack.EMPTY);
	}
}
