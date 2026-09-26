package com.hjmmd_8.createoreexpansion.foundation;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * <b>common 配置的订阅方</b>（P7a：从 core 的 {@code common/AllConfig} 搬出来的那一层注解）。
 *
 * <h2>一、为什么必须有这么一个"薄类"</h2>
 * <p>{@link AllConfig} 原先自己标着 {@code @EventBusSubscriber(modid = CoeCore.MOD_ID)}，而它住在
 * <b>core</b>。core 在发布形态里是一个带 {@code FMLModType: GAMELIBRARY} 的 JarJar 嵌套 jar：
 * {@code JarModsDotTomlModFileReader} 走 {@code manifestParser} 分支，那个分支的
 * {@code DefaultModFileInfo.getMods()} <b>硬编码返回空列表</b> ⇒ core 这个 mod 文件没有
 * {@code ModContainer} ⇒ {@code AutomaticEventSubscriber.inject} 对它整体不发生。</p>
 *
 * <p>而 {@code ModConfigEvent} 只发往 {@code modConfig.container}（即
 * {@code createoreexpansion} 那个容器）⇒ 生产里 {@link AllConfig#refresh()} <b>一次都不会被调用</b>，
 * 16 个静态缓存恒为硬编码默认值：玩家在 {@code createoreexpansion-common.toml} 里改的设置
 * 一条都不生效，而且没有任何报错。dev 里看不出来，因为 {@code fml.modFolders} 把 core 的输出
 * 并进了根 mod 文件，{@code IntegrationBootstrap} 的"补挂被孤立的订阅者"能扫到它。</p>
 *
 * <p><b>修法</b>：订阅动作住进本类（{@code :coe} 的文件，mod id = {@code createoreexpansion}），
 * 与它所在 mod 文件的 id <b>天然配对</b>（红线的两种形态都不成立），调 core 的
 * {@link AllConfig#refresh()}。core 那边彻底不含注解 —— 库不该假设"事件会送到我这里"。</p>
 *
 * <h2>二、为什么是 mod 总线而不是 game 总线</h2>
 * <p>{@code ModConfigEvent implements IModBusEvent}（loader 源码实测），所以
 * {@code AutomaticEventSubscriber} 会把它路由到 <b>mod 总线</b>；本类没有
 * {@code value = Dist.…}，两个 Dist 都注入（配置在两个端都要读）。</p>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class AllConfigSubscriber {

    private AllConfigSubscriber() {}

    /**
     * 配置加载 / 重载 / 卸载都会派发 {@code ModConfigEvent}；与拆分前的
     * {@code AllConfig#onLoad} 逐字同义（不做 Loading/Reloading 分支，保持行为不变）。
     */
    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        AllConfig.refresh();
        CoeCore.LOGGER.debug("[P7a] common 配置缓存已刷新（{}）", event.getConfig().getFileName());
    }
}
