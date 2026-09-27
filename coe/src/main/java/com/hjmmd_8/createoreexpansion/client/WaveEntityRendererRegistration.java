package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.client.renderer.EmptyEntityRenderer;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.AllEntityTypes;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * <b>两个能量波实体的渲染器注册（第一层侧）</b>（W6-c；{@code client.cews.CewsClientSetup}
 * 拆出来的 L1 半边）。
 *
 * <h2>一、为什么它必须随 {@code :coe} 发货</h2>
 * <p>两个波实体类型（{@code AllEntityTypes.CHARGER_WAVE} / {@code STELLAR_WAVE}）是波引擎的一部分，
 * W6-c 起住 {@code :coe}；而"谁注册实体谁注册渲染器"这条不变量如果被拆开，后果是<b>硬崩</b>
 * （不是静默）：</p>
 * <ul>
 *   <li>{@code LevelRenderer.renderEntity} 在调 {@code entityRenderDispatcher.render(...)} 之前，
 *       先算 {@code getPackedLightCoords(entity, partialTick)}；</li>
 *   <li>{@code EntityRenderDispatcher.getPackedLightCoords} →
 *       {@code getRenderer(entity).getPackedLightCoords(...)}，而 {@code getRenderer} 在未注册时
 *       直接 {@code renderers.get(type)} 返回 {@code null}；</li>
 *   <li>⇒ 波实体第一次进入视野即 NPE。</li>
 * </ul>
 * <p>这正是 P7a 那次实测的路径（当时 {@code CewsClientSetup} 就是为了补这个洞而新建的）。
 * W6-c 把它按层一分为二：本类（L1）= 两行渲染器注册 + {@code AllPartialModels.init()}；
 * {@code client.cews.CewsClientSetup}（L2）= {@code AllPartialModels.init()} +
 * {@code MachineRotateClient.install()}。{@code init()} 两边各调一次是幂等的（纯类加载触发器）。</p>
 *
 * <h2>二、为什么 modid 是 {@code CoeCore.MOD_ID}</h2>
 * <p>FML 的 {@code @EventBusSubscriber} 自动注入是<b>按 mod 文件</b>作用域的：
 * {@code AutomaticEventSubscriber.inject} 只喂本文件的扫描结果、并只挂
 * {@code mod.getModId() == modid} 的类。本类现在住在 {@code createoreexpansion} 那个 mod 文件里，
 * 标 {@code "cews"}（它在旧类里的取值）会<b>静默不注入</b>——无警告、无报错、编译全绿，
 * 症状就是上面那条 NPE 复活。{@code CoeCore.MOD_ID} 是 core 里的编译期常量（JLS 15.29），
 * 可以出现在注解里。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class WaveEntityRendererRegistration {

    private WaveEntityRendererRegistration() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 早期触发 PartialModel 类加载，确保 Flywheel 模型烘焙前收集到部件模型
        // （充能器快门 / 传动轴短轴就在 AllPartialModels 里，见 CreateChargerRenderer）。
        AllPartialModels.init();

        // 充能器能量波：空渲染器（视觉靠粒子）——翡翠/蓝宝石/星辉石都需注册，否则波实体进入视野即 NPE
        EntityRenderers.register(AllEntityTypes.CHARGER_WAVE.get(), EmptyEntityRenderer::new);
        // 星辉波变器变体波：同样为空渲染器（视觉靠粒子）
        EntityRenderers.register(AllEntityTypes.STELLAR_WAVE.get(), EmptyEntityRenderer::new);
    }
}
