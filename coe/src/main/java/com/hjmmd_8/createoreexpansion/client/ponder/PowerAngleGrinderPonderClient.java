package com.hjmmd_8.createoreexpansion.client.ponder;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * <b>Ponder 插件的客户端入口（第一层侧）</b>—— 本类只做一件事：
 * 把 {@link PowerAngleGrinderPonderPlugin} 交给 Ponder 库。
 *
 * <h2>一、为什么必须是客户端、且走 {@code FMLClientSetupEvent}</h2>
 * <p>Ponder 的注册表 {@code PonderIndex} 在静态初始化时就摸 {@code Minecraft.getInstance()}
 * （{@code PonderSceneRegistry#compile} 等处），所以整条链<b>只能存在客户端</b>；
 * 也因此本类与 Create 的 {@code CreateClient} 是同一形状——Create 在自己的客户端入口里
 * 直接调 {@code PonderIndex.addPlugin(new CreatePonderPlugin())}。</p>
 *
 * <h2>二、时序（为什么 {@code FMLClientSetupEvent} 来得及）</h2>
 * <ol>
 *   <li>{@code FMLClientSetupEvent}（本类，MOD 总线）——只在此时把插件<b>塞进列表</b>，
 *       此时 Ponder 还没有编译任何场景；</li>
 *   <li>{@code FMLLoadCompleteEvent} → Ponder 的 {@code NeoForgePonderClient.ModBusClientEvents#loadCompleted}
 *       → {@code PonderClient.modLoadCompleted()} → {@code PonderIndex.reload()}
 *       → {@code registerAll()} 才逐个插件调 {@code registerScenes}。</li>
 * </ol>
 * <p>⇒ 只要插件在 {@code FMLLoadCompleteEvent} 之前交进去即可，客户端 setup 阶段满足；
 * {@code PonderIndex.addPlugin} 自身还对插件列表加锁（MOD 加载是并行的），所以这里不需要额外同步。</p>
 *
 * <h2>三、为什么 modid 是 {@code CoeCore.MOD_ID} 而不是别的</h2>
 * <p>FML 的 {@code @EventBusSubscriber} 自动注入是<b>按 mod 文件</b>作用域的（{@code AutomaticEventSubscriber.inject}
 * 只喂本文件的扫描结果、并只挂 {@code mod.getModId() == modid} 的类）。本类住在
 * {@code createoreexpansion} 那个 mod 文件里（{@code :coe}），若标成别的层的 id 就会
 * <b>静默不注入</b>——无警告、无报错、编译全绿，症状是"悬停物品按住 W 没反应"。
 * 这条规则与 {@code client/WaveEntityRendererRegistration} 逐字相同。</p>
 *
 * <h2>四、⚠ 本批（批 A）只能进游戏验的部分</h2>
 * <p>"插件真的被 Ponder 收到并编译出场景"这件事<b>任何静态关卡都看不见</b>
 * （{@code compileJava} / {@code runData} 都不装配界面、不构造 Ponder 场景）：
 * 必须进游戏悬停"动力角磨床"物品、看到 {@code Hold [W] to Ponder} 并按住 W 才算通过。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class PowerAngleGrinderPonderClient {

    private PowerAngleGrinderPonderClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(PowerAngleGrinderPonderPlugin.create());
    }
}
