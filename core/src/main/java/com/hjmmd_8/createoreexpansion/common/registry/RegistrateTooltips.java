package com.hjmmd_8.createoreexpansion.common.registry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;

/**
 * 各层 Registrate 共用的<b>物品提示（tooltip）工厂</b>安装器。
 *
 * <p><b>为什么必须三层都装</b>：拆分前全模组的物品都由<b>同一个</b> Registrate 注册，
 * 因此每个物品都自动获得 Create 的 {@link ItemDescription}（描述行）与 {@link KineticStats}
 * （动能统计：应力影响/转速）提示。Registrate 分家后，这些提示是<b>按 Registrate 实例</b>挂的
 * （{@code CreateRegistrate.setTooltipModifierFactory} 是实例字段）——所以 COE / CEWS / TRANS
 * 三个实例都要装同一套，否则自己那一层的物品会整体丢掉描述行与动能行，属于行为变化。</p>
 *
 * <p><b>充能器专用提示归 CEWS，本类不知情</b>：CEWS 的应力充能器物品还多一段自定义应力块
 * （{@code common/registry/cews/ChargerKineticTooltip}）。P3o 把本类搬进 {@code core} 库后，
 * 它<b>不许</b>再认识任何层专属类，于是那段提示改由 {@code CewsRegistrate} 自己<b>串联</b>：
 * 先 {@code install(REGISTRATE)} 装本类的通用两级，再 {@code andThen} 挂它那一段。
 * 组合顺序与拆分前逐字相同（描述 → 动能 → 充能器），COE / TRANS 的实例不接第三级，
 * 与旧的 {@code chargers = false} 分支逐字等价（它们对那段 modifier 本来就返回 {@code null}）。</p>
 */
public final class RegistrateTooltips {

    private RegistrateTooltips() {}

    /**
     * 给一个 Registrate 装上 Create 的标准物品提示链（描述行 + 动能统计两级）。
     *
     * @param registrate 目标 Registrate（本层自己的实例）
     */
    public static void install(CreateRegistrate registrate) {
        registrate.setTooltipModifierFactory(item ->
            new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
    }
}
