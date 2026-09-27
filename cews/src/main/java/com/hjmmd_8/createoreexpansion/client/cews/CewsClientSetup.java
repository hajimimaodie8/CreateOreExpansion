package com.hjmmd_8.createoreexpansion.client.cews;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.common.machine.MachineRotateClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * <b>CEWS 的客户端初始化</b>（P7a 新建；<b>W6-c 拆成两半后的 L2 半边</b>）。
 *
 * <h2>一、W6-c 拆走了什么</h2>
 * <p>P7a 建本类是为了补上"两个能量波实体类型有人注册、没有渲染器"那个洞——那段路径是
 * <b>硬崩</b>（{@code EntityRenderDispatcher.getRenderer} 返回 {@code null} ⇒
 * {@code getPackedLightCoords} NPE），完整机理与本类的历史见第一层的新家
 * {@code com.hjmmd_8.createoreexpansion.client.WaveEntityRendererRegistration}。</p>
 * <p>W6-c 之后两个波实体（{@code AllEntityTypes.CHARGER_WAVE} / {@code STELLAR_WAVE}）
 * 与 {@code EmptyEntityRenderer} 都在第一层，所以那两行注册随它们进
 * {@code :coe}；本类只剩"CEWS 自己的客户端初始化"两件事。</p>
 *
 * <h2>二、{@code AllPartialModels.init()} 在这里也调一次</h2>
 * <p>{@code AllPartialModels} 住 core，同时持有波闸灯 / 色散器灯 / 差波器灯 / 调级器齿轮与
 * 充能器快门等部件模型；{@code init()} 是纯类加载触发器（空方法），唯一要求是"早于 Flywheel 烘焙"。
 * 第一层与第二层各调一次是幂等的（纯类加载触发器，重复调用没有副作用）。</p>
 *
 * <h2>三、为什么这里还要装 {@code MachineRotateClient}</h2>
 * <p>Ctrl+扳手旋转的客户端拦截逻辑住 core（{@code common.machine.MachineRotateClient}），
 * 但 core 在发布形态里没有 {@code ModContainer}，它不能带 {@code @EventBusSubscriber}。
 * 客户端专属的薄入口就是安装点：本类与 {@code :coe} 的对应类各调一次，幂等。</p>
 *
 * <h2>四、modid 为什么仍是 {@code "cews"}</h2>
 * <p>判据 = "类的 modid == 它所在 mod 文件的 id"（{@code AutomaticEventSubscriber} 按 mod 文件
 * 过滤）。本类留在 {@code :cews}，所以标 {@code cews}（字面量；W6-a 起就不引用
 * {@code CewsMod} 常量，免得写成 {@code L2 → L2} 以外的形状）。第一层那半标
 * {@code CoeCore.MOD_ID}。</p>
 */
@EventBusSubscriber(modid = "cews", value = Dist.CLIENT)
public final class CewsClientSetup {

    private CewsClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 早期触发 PartialModel 类加载，确保 Flywheel 模型烘焙前收集到（CEWS 的）部件模型
        // （波闸灯 / 差波器灯 / 调级器齿轮…；充能器那几件由第一层自己的客户端入口调一次，
        //  两边都是幂等的类加载触发器）
        AllPartialModels.init();

        // W6-c：两个能量波实体的渲染器注册（原先就在本方法里）已随实体类型搬进第一层的
        // com.hjmmd_8.createoreexpansion.client.WaveEntityRendererRegistration ——
        // 注册者与渲染器必须同一个 jar，否则只装 coe.jar 时波一进视野就 NPE。

        // Ctrl + 扳手右键的客户端拦截（core 逻辑 + 本模块负责安装）
        MachineRotateClient.install();
    }
}
