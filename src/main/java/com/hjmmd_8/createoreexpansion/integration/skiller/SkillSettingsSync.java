package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 玩家登录时补发一次技能设置（{@link SkillSettingsPayload}）。
 *
 * <p><b>为什么必须有这一步</b>：开关的值只活在服务端存档里，客户端平时收不到任何东西。
 * 若只在"值变化时"广播，玩家重进世界后客户端镜像会停在
 * {@link SkillSettingsPayload#clientValue} 的初值上——界面显示的是缺省值，
 * 而服务端实际用的是存档里的值，两边<b>不一致</b>。登录补发把这两者对齐。</p>
 *
 * <p>挂 game bus（{@code @EventBusSubscriber} 默认），与
 * {@code EnergyFieldCommandRegistration} 的登录补发同一写法。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class SkillSettingsSync {

    private SkillSettingsSync() {
        throw new AssertionError("This class should not be instantiated");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SkillSettingsPayload.sendToPlayer(player);
        }
    }
}
