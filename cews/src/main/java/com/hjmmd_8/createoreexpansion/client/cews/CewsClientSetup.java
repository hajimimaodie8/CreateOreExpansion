package com.hjmmd_8.createoreexpansion.client.cews;

import com.hjmmd_8.createoreexpansion.client.renderer.cews.EmptyEntityRenderer;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.common.machine.MachineRotateClient;
import com.hjmmd_8.createoreexpansion.common.registry.cews.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsMod;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * <b>CEWS 的客户端初始化</b>（P7a 新建）—— 两个能量波实体的空渲染器注册。
 *
 * <h2>一、它为什么必须存在（P7a 最要紧的一处）</h2>
 * <p>这两个 {@code EntityRenderers.register} 调用原先住在根工程的
 * {@code client/JadeTopazBowModelRegistration} 里，而那是个"根工程才有的类"。
 * CEWS 的 {@code @Mod} 自己会注册 {@code AllEntityTypes.CHARGER_WAVE} / {@code STELLAR_WAVE}
 * 两个实体类型，却没有任何人注册对应的渲染器。而 {@code :cews} 的 {@code mods.toml} 把
 * {@code createoreexpansion} 声明为 <b>optional</b> ⇒ "只装 cews.jar"是设计内的组合，
 * 那个组合下的后果是<b>硬崩</b>（不是静默）：</p>
 * <ul>
 *   <li>{@code LevelRenderer.renderEntity} 在调 {@code entityRenderDispatcher.render(...)} 之前，
 *       先算 {@code getPackedLightCoords(entity, partialTick)}；</li>
 *   <li>{@code EntityRenderDispatcher.getPackedLightCoords} →
 *       {@code getRenderer(entity).getPackedLightCoords(...)}，而
 *       {@code getRenderer} 在未注册时直接 {@code renderers.get(type)} 返回 {@code null}；</li>
 *   <li>⇒ 波实体第一次进入视野即 NPE。</li>
 * </ul>
 *
 * <p>所以本类必须住 {@code :cews}（谁注册实体谁注册渲染器），且 {@code EmptyEntityRenderer}
 * 这个类也必须能在"没装 coe.jar"时被解析到 —— 它因此从 {@code :coe} 搬进了本模块
 * （见 {@link EmptyEntityRenderer} 的类注释）。</p>
 *
 * <h2>二、{@code AllPartialModels.init()} 在这里也调一次</h2>
 * <p>{@code AllPartialModels} 住 core，同时持有 CEWS 的充能器快门 / 波闸灯 / 色散器灯 /
 * 差波器灯部件模型；{@code init()} 是纯类加载触发器（空方法），唯一要求是"早于 Flywheel 烘焙"。
 * 原先它的唯一调用点也在那个根侧类里 ⇒ 单装 cews.jar 时 CEWS 的部件模型会在烘焙之后才注册，
 * 渲染成紫黑缺失方块（不崩、无日志）。两边各调一次是幂等的。</p>
 *
 * <h2>三、为什么这里还要装 {@code MachineRotateClient}</h2>
 * <p>Ctrl+扳手旋转的客户端拦截逻辑住 core（{@code common.machine.MachineRotateClient}），
 * 但 core 在发布形态里没有 {@code ModContainer}，它不能带 {@code @EventBusSubscriber}。
 * 客户端专属的薄入口就是安装点：本类与 {@code :coe} 的对应类各调一次，幂等。</p>
 */
@EventBusSubscriber(modid = CewsMod.MOD_ID, value = Dist.CLIENT)
public final class CewsClientSetup {

    private CewsClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 早期触发 PartialModel 类加载，确保 Flywheel 模型烘焙前收集到（CEWS 的）部件模型
        AllPartialModels.init();

        // 充能器能量波：空渲染器（视觉靠粒子）——翡翠/蓝宝石都需注册，否则波实体进入视野即 NPE
        EntityRenderers.register(AllEntityTypes.CHARGER_WAVE.get(), EmptyEntityRenderer::new);
        // 星辉波变器变体波：同样为空渲染器（视觉靠粒子）
        EntityRenderers.register(AllEntityTypes.STELLAR_WAVE.get(), EmptyEntityRenderer::new);

        // Ctrl + 扳手右键的客户端拦截（core 逻辑 + 本模块负责安装）
        MachineRotateClient.install();
    }
}
