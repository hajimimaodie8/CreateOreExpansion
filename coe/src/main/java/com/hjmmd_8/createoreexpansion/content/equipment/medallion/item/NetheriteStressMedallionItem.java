package com.hjmmd_8.createoreexpansion.content.equipment.medallion.item;

/**
 * 下界合金凝能佩（纯物品支线）：无能量回收。
 *
 * <p>佩戴时永久抗火（时长 60 tick 续期，图标不闪烁）是<b>饰品槽</b>行为，
 * 实现已搬到只在装了 Curios 时才会被加载的饰品支线
 * {@code compat.curios.CurioMedallionItems.Netherite#curioTick}（逻辑逐字不变）。
 * 未装 Curios 时本类不含任何 Curios 引用，照常注册/合成/持有。</p>
 */
public class NetheriteStressMedallionItem extends BaseStressMedallionItem {

    public NetheriteStressMedallionItem(Properties properties) {
        super(properties, 0);
    }
}
