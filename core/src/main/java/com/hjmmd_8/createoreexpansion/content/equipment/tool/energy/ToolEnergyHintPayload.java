package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * <b>服务端 → 客户端：工具能量文案</b>（剩余能量读数 / 能量不足警告）。
 *
 * <h2>为什么要自己发包，而不是继续用动作栏</h2>
 * <p>用户 2026-10-02 的第三条要求：「能量消耗在快捷栏上的提示，离快捷栏的距离都太大了，可以适当缩小」。
 * 原实现走的是 {@code Player#displayClientMessage(component, true)}（= 原版动作栏），而动作栏的 y
 * 由原版 {@code Gui#renderOverlayMessage} 写死（{@code guiHeight - 68} 再 {@code -4}），
 * <b>模组无法单独挪动自己那一条</b> —— 改它就得连带改原版/它模组的全部动作栏文案。
 * 因此这里把这条文案<b>收成自己的图层</b>（{@link ToolEnergyHintClient} + 客户端 HUD 层），
 * 位置由模组自己定（见 {@code client.hud.ToolEnergyHintHud} 的几何注释）。</p>
 *
 * <h2>为什么载荷里传的是已经拼好的 {@code Component}</h2>
 * <p>文案的颜色是<b>逐段</b>的（佩段用佩色、工具段用工具色、翠玉之弓还有黄→绿逐字渐变，
 * 见 {@link ToolEnergy#sendRemainingEnergyWithMedallion}）/ 警告用工具能量色。传原始数字再在客户端
 * 重拼等于把同一段拼装逻辑写两遍，且两边一旦漂移就会出现「看到的颜色和扣能量的那件东西不一致」。
 * 直接传组件则<b>颜色与文字只有一个来源</b>（服务端那一份），客户端只负责画。</p>
 *
 * <p>注册点与 {@code MachineRotatePayload} 同一处（core 的幂等入口
 * {@code LayerBootstrap#ensureAttached}）：二次注册会抛
 * {@code UnsupportedOperationException}，因此「恰一次」必须由那一个入口保证。</p>
 *
 * @since 1.0.0
 */
public record ToolEnergyHintPayload(Component line) implements CustomPacketPayload {

    /** 通道名（完整 id = {@code createoreexpansion:tool_energy_hint}）。 */
    public static final CustomPacketPayload.Type<ToolEnergyHintPayload> TYPE =
        new CustomPacketPayload.Type<>(CoeCore.modLoc("tool_energy_hint"));

    /**
     * 编解码器：直接用原版组件编解码器（{@code RegistryFriendlyByteBuf} 就是 NeoForge 给载荷的缓冲类型）。
     *
     * <p>不用 {@code TRUSTED_*} 那一族：这里传的都是本模组自己拼的字面量组件，走带注册表校验的
     * 常规路径即可，没有任何"信任输入"的必要。</p>
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolEnergyHintPayload> STREAM_CODEC =
        StreamCodec.composite(ComponentSerialization.STREAM_CODEC, ToolEnergyHintPayload::line,
            ToolEnergyHintPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端收包：交给 {@link ToolEnergyHintClient} 记下并开始倒计时。
     *
     * <p>引用客户端缓存类的代码<b>只在客户端执行</b>（{@code enqueueWork} 的 lambda 在专用服务端
     * 那条路径上永远不会被调用），与 {@code EquipCooldownPayload} → {@code ArmorCooldownClient}
     * 是同一个写法。</p>
     */
    public static void handle(ToolEnergyHintPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ToolEnergyHintClient.show(payload.line()));
    }

    /** 网络注册（由 core 的 {@code LayerBootstrap#ensureAttached} 挂，见类注释）。 */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(TYPE, STREAM_CODEC, ToolEnergyHintPayload::handle);
    }
}
