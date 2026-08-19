package com.hjmmd_8.createoreexpansion.content.equipment.medallion;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import net.minecraft.world.item.ItemStack;

/** 翡翠凝能佩：供应后 50% 概率恢复消耗值一半的能量 */
public class JadeStressMedallionItem extends BaseStressMedallionItem {

    public JadeStressMedallionItem(Properties properties) {
        super(properties, 0);
    }

    @Override
    public void onSupplyConsumed(ItemStack self, int consumed) {
        if (consumed <= 0 || Math.random() >= 0.5)
            return;
        ToolEnergy.setEnergy(self, ToolEnergy.getEnergy(self) + consumed / 2);
    }
}
