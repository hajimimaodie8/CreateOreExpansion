package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.common.machine.MachineRotateClient;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * <b>翠玉黄玉弓的 item property 注册 + COE 侧的客户端初始化</b>。
 *
 * <h2>P7a：本类原来还注册了两个能量波实体的空渲染器，那部分已搬到 CEWS</h2>
 * <p>拆分前本类一个类干两件互不相干的事：① 弓的 {@code pull}/{@code pulling} item property
 * （COE 的物品）；② {@code EntityRenderers.register(AllEntityTypes.CHARGER_WAVE / STELLAR_WAVE,
 * EmptyEntityRenderer::new)}（<b>CEWS</b> 的实体）。后者是"单装 cews.jar 就崩"的根因：
 * CEWS 自己会把两个波实体类型注册出来，却没有任何人注册渲染器 ⇒
 * {@code EntityRenderDispatcher.getRenderer} 返回 {@code null} ⇒ 波一进视野
 * {@code LevelRenderer} 里先解引用即 NPE。</p>
 *
 * <p>现在两个波实体的渲染器注册（连同 {@code EmptyEntityRenderer} 这个类）都住
 * {@code :cews} 的 {@code client.cews.CewsClientSetup}；本类只剩 COE 自己的那一半。</p>
 *
 * <h2>为什么两个模块都还调 {@code AllPartialModels.init()}</h2>
 * <p>{@code AllPartialModels} 住 core，同时持有 COE（角磨轮/主轴）与 CEWS（充能器快门、
 * 波闸灯、色散器灯、差波器灯）的部件模型，而 {@code init()} 是纯类加载触发器（空方法）——
 * 唯一的要求是"在 Flywheel 烘焙部件模型之前"。两边各调一次是幂等的；
 * 只装某一个 jar 时，那一边自己就能把这一批模型提前加载起来（否则渲染成紫黑缺失方块，
 * 不崩、无日志）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public class JadeTopazBowModelRegistration {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 早期触发 PartialModel 类加载，确保 Flywheel 模型烘焙前收集到部件模型
        AllPartialModels.init();

        // P7a：Ctrl + 扳手右键的客户端拦截逻辑住 core（common.machine.MachineRotateClient），
        // 但它不能带 @EventBusSubscriber（core 在发布形态里没有 ModContainer），所以由本类
        // 这个"客户端专属、modid 与文件 id 配对"的订阅者显式安装一次。幂等。
        MachineRotateClient.install();

        event.enqueueWork(() -> {
            ItemProperties.register(CoeItems.JADE_TOPAZ_BOW.get(),
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null)
                        return 0.0F;
                    return entity.getUseItem() != stack ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 25.0F;
                });

            ItemProperties.register(CoeItems.JADE_TOPAZ_BOW.get(),
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

}
