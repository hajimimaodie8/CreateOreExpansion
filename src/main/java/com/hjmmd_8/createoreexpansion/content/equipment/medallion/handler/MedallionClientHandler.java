package com.hjmmd_8.createoreexpansion.content.equipment.medallion.handler;

import com.hjmmd_8.createoreexpansion.common.AllFluids;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 星辉石系列客户端效果：物品实体处于虚空（y < -3）或液体（岩浆/嬗化液）中时，
 * 照搬 Create 暗影钢的白色粒子（END_ROD），方便定位。
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class MedallionClientHandler {

    private MedallionClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null)
            return;
        RandomSource random = level.random;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item))
                continue;
            if (!MedallionEffectHandler.isStellarstoneItem(item.getItem()))
                continue;
            boolean inVoid = item.getY() < -3;
            boolean inFluid = item.isInLava()
                || item.getFluidTypeHeight(AllFluids.TRANSMUTATION_FLUID.get().getFluidType()) > 0.0D;
            if (inVoid || inFluid) {
                if (random.nextFloat() > 0.25f)
                    continue; // 约每 4 tick 一个粒子
                // 照搬暗影钢 NoGravMagicalDohickyItem：位置随机偏移，速度 (0, -0.1, 0)
                Vec3 ppos = item.position().offsetRandom(random, 0.5f);
                level.addParticle(ParticleTypes.END_ROD, ppos.x, item.getY(), ppos.z, 0, -0.1, 0);
            }
        }
    }
}
