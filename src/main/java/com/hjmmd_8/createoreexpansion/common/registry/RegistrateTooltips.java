package com.hjmmd_8.createoreexpansion.common.registry;

import com.hjmmd_8.createoreexpansion.client.ChargerKineticTooltip;
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
 * <p><b>充能器专用提示（{@link ChargerKineticTooltip}）归 CEWS</b>：它的
 * {@code create(item)} 只对 {@code JadeStressChargerBlock} / {@code SapphireStressChargerBlock}
 * 返回非空，这两种机器都是 CEWS 的（见 {@code common/registry/cews/CewsBlocks}），
 * 所以只有 CEWS 的实例需要把它接进链子。COE / TRANS 的物品本来就会拿到 {@code null}，
 * 不接进链子与接进去（{@code mapNull(null) == EMPTY}）逐字等价——这里选择"不接"，
 * 让"哪台机器归哪层"这件事在代码里一眼可见。</p>
 */
public final class RegistrateTooltips {

    private RegistrateTooltips() {}

    /**
     * 给一个 Registrate 装上 Create 的标准物品提示链。
     *
     * @param registrate       目标 Registrate（本层自己的实例）
     * @param chargers         {@code true} = 本层注册应力充能器，需要额外接上
     *                         {@link ChargerKineticTooltip}（CEWS）；{@code false} = 只装通用两级
     */
    public static void install(CreateRegistrate registrate, boolean chargers) {
        registrate.setTooltipModifierFactory(item -> {
            TooltipModifier modifier = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
            if (chargers) {
                // 充能器：三实心方块 + 自定义区间应力提示（隐藏 Create 默认静态行，见 ChargerKineticTooltip）
                modifier = modifier.andThen(TooltipModifier.mapNull(ChargerKineticTooltip.create(item)));
            }
            return modifier;
        });
    }
}
