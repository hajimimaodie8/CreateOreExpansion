package com.hjmmd_8.createoreexpansion.content.equipment.armor.energy;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * <b>服务端 → 客户端：装备技能进入冷却</b>（技能 id + 秒数）。
 *
 * <p>起因（用户 2026-10-01）："进入冷却期之后，最下面那一行字根本就没有被替换" ——
 * 冷却存在服务端持久数据里（{@code getPersistentData()} 不同步），客户端恒读 0。
 * 于是服务端起冷却时通知一次，客户端自己倒数（见 {@link ArmorCooldownClient}）：
 * <b>一个冷却只发一个包</b>，之后零流量。</p>
 *
 * @since 1.0.0
 */
public record EquipCooldownPayload(String skillId, int seconds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EquipCooldownPayload> TYPE =
        new CustomPacketPayload.Type<>(CoeCore.modLoc("equip_cooldown"));

    public static final StreamCodec<FriendlyByteBuf, EquipCooldownPayload> STREAM_CODEC =
        StreamCodec.of(EquipCooldownPayload::write, EquipCooldownPayload::read);

    private static void write(FriendlyByteBuf buffer, EquipCooldownPayload payload) {
        buffer.writeUtf(payload.skillId);
        buffer.writeVarInt(payload.seconds);
    }

    private static EquipCooldownPayload read(FriendlyByteBuf buffer) {
        return new EquipCooldownPayload(buffer.readUtf(), buffer.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 处理：只在客户端执行（引用 {@link ArmorCooldownClient} 的代码不会在服务端跑）。 */
    public static void handle(EquipCooldownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ArmorCooldownClient.apply(payload.skillId(), payload.seconds()));
    }

    /**
     * 注册（由根侧 {@code @Mod} 入口在 {@code RegisterPayloadHandlersEvent} 里调用，
     * 与机器旋转/能量场同步那两条载荷同一配方）。
     */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(TYPE, STREAM_CODEC, EquipCooldownPayload::handle);
    }
}
