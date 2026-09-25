package com.hjmmd_8.createoreexpansion.content.equipment.medallion;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.energy.MedallionLink;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * <b>凝能佩联动契约（core）的层内实现</b>（P3p）。
 *
 * <p>{@link MedallionLink} 是共享库里「工具能量门面需要凝能佩做的 5 件事」的接口，
 * 因为库不能 import 本包里的 {@link IMedallion}。本类把那 5 个方法逐个转发给
 * {@code IMedallion} 的原有实现——<b>转发体与旧 {@code ToolEnergy} 里的写法一一对应</b>，
 * 没有任何新逻辑：</p>
 * <ul>
 *   <li>{@link #findBound} = 旧 {@code IMedallion.findBoundMedallion}（静态方法，含 Curios 查询）；</li>
 *   <li>{@link #isMedallion} = 旧 {@code item instanceof IMedallion}；</li>
 *   <li>{@link #isChargeMode} = 旧 {@code im.isChargeMode(medallion)}；</li>
 *   <li>{@link #consumeToolEnergy} / {@link #chargeBoundTools} = 旧 {@code im.consumeToolEnergy}
 *       / {@code im.chargeBoundTools}。</li>
 * </ul>
 *
 * <p>注入点由 {@code CreateOreExpansion} 构造器显式调用（库没有生命周期，
 * 「谁来实现契约」必须是根侧写死的一件事）。</p>
 */
public final class MedallionEnergyLink implements MedallionLink {

    private MedallionEnergyLink() {
    }

    /** 把本实现注入 core 的契约持有者；未注入时 core 走 {@link MedallionLink#NONE}（等价"没有佩体系"）。 */
    public static void install() {
        MedallionLink.install(new MedallionEnergyLink());
        CoeCore.LOGGER.debug("[COE] 凝能佩联动契约已注入 core（common.energy.MedallionLink）");
    }

    @Override
    public ItemStack findBound(Player player, ItemStack tool) {
        return IMedallion.findBoundMedallion(player, tool);
    }

    @Override
    public boolean isMedallion(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof IMedallion;
    }

    @Override
    public boolean isChargeMode(ItemStack medallion) {
        return medallion.getItem() instanceof IMedallion im && im.isChargeMode(medallion);
    }

    @Override
    public void consumeToolEnergy(ItemStack medallion, ItemStack tool, int amount) {
        if (medallion.getItem() instanceof IMedallion im) {
            im.consumeToolEnergy(medallion, tool, amount);
        }
    }

    @Override
    public void chargeBoundTools(Player player, ItemStack medallion) {
        if (medallion.getItem() instanceof IMedallion im) {
            im.chargeBoundTools(player, medallion);
        }
    }
}
