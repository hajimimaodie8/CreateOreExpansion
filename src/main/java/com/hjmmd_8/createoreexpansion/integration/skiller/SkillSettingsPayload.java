package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 技能设置开关的网络包：<b>同一个 record 两个方向</b>
 * （{@code playBidirectional}，靠 {@link IPayloadContext#flow()} 分流）。
 *
 * <h2>C2S（客户端 → 服务端）</h2>
 * <p>玩家在 {@code SkillSettingsScreen} 里点开关时发送。服务端<b>只把它当"意愿"</b>：
 * 先校验 {@code context.player() instanceof ServerPlayer}（真人玩家，而不是伪造包/控制台），
 * 再写进存档（{@link SkillSettings#setConsumeInCreative}），最后把权威值广播回去。
 * 客户端本地先乐观切换只是为了手感，真正的值永远以服务端回包为准。</p>
 *
 * <h2>S2C（服务端 → 客户端）</h2>
 * <p>两个时机：① 值变化后广播；② <b>玩家登录时补发一次</b>
 * （{@link #sendToPlayer}，由 {@code SkillSettingsSync} 挂在 {@code PlayerLoggedInEvent} 上）。
 * 没有第 ② 条，界面里显示的永远是缺省值而不是存档里的真实值。</p>
 *
 * <p>客户端把最近一次收到的值存在 {@link #clientValue}，供界面显示——
 * 这是本模组既有的做法（见 {@code EnergyFieldClientState} 之类的客户端镜像），
 * 不在客户端再存一份"自己的"设置。</p>
 *
 * @since 1.0.0
 */
public record SkillSettingsPayload(boolean consumeInCreative) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SkillSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "skill_settings"));

    public static final StreamCodec<FriendlyByteBuf, SkillSettingsPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeBoolean(payload.consumeInCreative()),
                    buf -> new SkillSettingsPayload(buf.readBoolean()));

    /**
     * 客户端最近一次从服务端收到的权威值（界面显示用）。
     *
     * <p>初值 = 服务端缺省值：进世界后、登录补发包到达前的那一瞬间，界面显示的就是
     * 缺省 {@code true}，不会出现"空白/沿用上局世界"的错觉。</p>
     */
    public static volatile boolean clientValue = SkillSettings.DEFAULT_CONSUME_IN_CREATIVE;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 双向分发：服务端侧写存档 + 广播，客户端侧只更新本地镜像。
     *
     * <p>{@code context.flow()} 就是方向判据——{@code SERVERBOUND} 表示包是客户端发来的
     * （当前端是服务端），{@code CLIENTBOUND} 反之。这样同一个 record 只需要一个 handler。</p>
     */
    public static void handle(SkillSettingsPayload payload, IPayloadContext context) {
        boolean serverbound = context.flow() == PacketFlow.SERVERBOUND;
        context.enqueueWork(() -> {
            if (serverbound) {
                if (!(context.player() instanceof ServerPlayer player)) {
                    return; // 非服务端玩家（例如控制台/伪造包）：静默丢弃
                }
                SkillSettings.setConsumeInCreative(player.serverLevel(), payload.consumeInCreative());
                broadcast(player.getServer(), payload.consumeInCreative());
            } else {
                clientValue = payload.consumeInCreative();
            }
        });
    }

    /** 网络注册（mod bus，见 {@code CreateOreExpansion} 构造器）。 */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playBidirectional(TYPE, STREAM_CODEC, SkillSettingsPayload::handle);
    }

    /**
     * 服务端：把当前权威值发给该玩家（登录时调用）。
     *
     * <p>读的是玩家所在维度 → 内部归一到主世界存档，见 {@link SkillSettings}。</p>
     */
    public static void sendToPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new SkillSettingsPayload(SkillSettings.consumeInCreative(player.serverLevel())));
    }

    /** 服务端：值变化后广播给所有在线玩家（保证大家界面上看到的是同一个权威值）。 */
    private static void broadcast(net.minecraft.server.MinecraftServer server, boolean value) {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(online, new SkillSettingsPayload(value));
        }
    }
}
