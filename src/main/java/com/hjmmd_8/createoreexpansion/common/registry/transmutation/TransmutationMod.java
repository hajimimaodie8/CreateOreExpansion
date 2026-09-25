package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * <b>TRANS（Create: Mechanical Transmutation，机械嬗化学）的 {@code @Mod} 入口</b>
 * （P3b：四模块拆分）。
 *
 * <p>本层目前只有物品（{@link TransmutationItems}），职责很薄——但 mod 类与 Registrate 都必须独立，
 * 否则 TRANS 永远无法脱离 COE 单独编译。注册命名空间仍是 {@code createoreexpansion}，
 * 所以注册 id / 语言键 / 数据包路径一个都没变。</p>
 *
 * <p>本层不引入可选桥接（Jade 的两个插件分别归 CEWS 与 COE，Sable 归 CEWS，
 * Curios 归 COE 的凝能佩线）。</p>
 */
@Mod(TransmutationMod.MOD_ID)
public class TransmutationMod {

    /** TRANS 模块的 mod id（<b>不是</b>注册命名空间——命名空间恒为 {@code createoreexpansion}）。 */
    public static final String MOD_ID = "transmutation";

    public TransmutationMod(IEventBus modEventBus, ModContainer modContainer) {
        TransmutationRegistrate.REGISTRATE.registerEventListeners(modEventBus);

        // 按层显式触发物品注册类初始化（拆分前本层排在 CEWS 物品之后）
        TransmutationItems.register();

        CoeCore.LOGGER.info("[TRANS] mod 初始化完成（mod id={}，注册命名空间={}）：嬗变化合物件已注册",
            MOD_ID, CoeCore.REGISTRY_NAMESPACE);
    }
}
