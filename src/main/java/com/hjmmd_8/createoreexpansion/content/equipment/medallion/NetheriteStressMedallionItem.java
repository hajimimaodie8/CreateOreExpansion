package com.hjmmd_8.createoreexpansion.content.equipment.medallion;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;

/** 下界合金凝能佩：无能量回收；佩戴时永久抗火（时长 60 tick 续期，图标不闪烁） */
public class NetheriteStressMedallionItem extends BaseStressMedallionItem {

    public NetheriteStressMedallionItem(Properties properties) {
        super(properties, 0);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        Entity entity = slotContext.entity();
        if (entity instanceof LivingEntity living && !living.level().isClientSide) {
            MobEffectInstance effect = living.getEffect(MobEffects.FIRE_RESISTANCE);
            if (effect == null || effect.getDuration() < 30) {
                living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
            }
        }
    }
}