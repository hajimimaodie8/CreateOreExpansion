package com.hjmmd_8.createoreexpansion.common.hub;

import net.neoforged.fml.common.Mod;

/**
 * <b>根工程自己的 {@code @Mod} 入口</b>（P3z）—— 集成层（integration layer）的载体。
 *
 * <h2>一、它为什么存在</h2>
 * <p>P3z 把最后一块内容层 CEWS（能量波阵学，133 个文件 + {@code @Mod} 入口
 * {@code CewsMod}）也搬进了 Gradle 子模块 {@code :cews}。至此根工程里
 * <b>一个本模组的 {@code @Mod} 类都不剩</b>：{@code CreateOreExpansion} 随 {@code :coe}（P3w）、
 * {@code TransmutationMod} 随 {@code :transmutation}（P3y）、{@code CewsMod} 随 {@code :cews}（P3z）。</p>
 * <p>而 FML 对 {@code [[mods]]} 与 {@code @Mod} 的约束是双向的：</p>
 * <ul>
 *   <li><b>有条目、没类</b> → 这个 mod 出现在 mod 列表里却永不构造，也就是"入口静默消失"，
 *       没有任何报错；</li>
 *   <li><b>有类、没条目</b> → {@code dangling_entrypoint} 硬错。</li>
 * </ul>
 * <p>反过来，把根模板的 {@code [[mods]]} 清空也不行：一个 mod 文件若一个 mod 都不声明，
 * 就没有对应的 {@code ModContainer}，FML 的
 * {@code AutomaticEventSubscriber.inject(...)} 对这个文件整体不发生 ——
 * 根侧的 datagen 入口、{@code IntegrationBootstrap} 自身、Ctrl+扳手、配置重载、JEI 插件
 * 会<b>集体静默失效</b>（{@code runData} 第一个死）。所以根工程必须自己是一个 mod。</p>
 *
 * <h2>二、它是什么（以及不是什么）</h2>
 * <p>它是<b>组装/集成载体</b>：名下装着集成层 Java（{@code common/hub/**} 的聚合入口、{@code data/**}
 * 的 datagen 驱动与两份 LangProvider、{@code compat/**} 的 Curios / Jade / 唯一 {@code @JeiPlugin}、
 * {@code mixin/**} 的类）、根工程的全部资源（{@code assets/**}、{@code data/**}、{@code models/**}、
 * {@code src/generated/**}）、以及发布时的 JarJar 组装（{@code META-INF/jarjar/} 里的
 * core + coe + transmutation + cews + skiller）。</p>
 * <p>它<b>不是</b>内容 mod，也<b>不是</b>第五个"模块"：注册命名空间一字未变
 * （仍是 {@link com.hjmmd_8.createoreexpansion.common.CoeCore#REGISTRY_NAMESPACE}），
 * 它自己不注册任何本模组的游戏内容；真正的注册都在三个层模块里。modId 取
 * {@code coe_integration} 只是为了给"根文件的集成层"一个身份 ——
 * 根工程原来的 {@code mod_id=createoreexpansion} 已随 {@code :coe} 走，不能再声明一次
 * （同一个 mod id 出现在两个 mod 文件里 = {@code fml.modloadingissue.duplicate_mod} 硬崩）。</p>
 *
 * <h2>三、构造器为什么是空的</h2>
 * <p>根侧的接线动作全部由 {@link IntegrationBootstrap} 在
 * {@code FMLConstructModEvent} 上完成（hub 的五个注册触发 + 补挂被孤立的
 * {@code @EventBusSubscriber}）—— 那是 P3w 就定下的形态：它按<b>扫描数据</b>工作，
 * 自维护、不需要名单，也不给任何层加 {@code LAYER-NO} 阻塞。
 * 本类只负责"让 FML 有一个入口可以构造"。{@code [[mods]]} 里的
 * {@code modId = "coe_integration"} 与本类的 {@link #MOD_ID} 必须一致
 * （见 {@code src/main/templates/META-INF/neoforge.mods.toml}）。</p>
 */
@Mod(IntegrationMod.MOD_ID)
public final class IntegrationMod {

    /**
     * 根工程那个 mod 文件的 mod id（集成层；<b>不是</b>注册命名空间 —— 命名空间恒为
     * {@code createoreexpansion}）。{@link IntegrationBootstrap#ROOT_MOD_ID} 直接取本常量，
     * 保证"事件挂哪个容器"与"模板声明哪个 id"不会分家。
     */
    public static final String MOD_ID = "coe_integration";

    public IntegrationMod() {
        // 见类注释"三"：接线在 IntegrationBootstrap 的 FMLConstructModEvent 上完成。
    }
}
