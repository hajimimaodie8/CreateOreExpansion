package com.hjmmd_8.createoreexpansion.common;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <b>core 共享库</b>的命名空间 / 日志出口（P3d：core 已从 mod 降级为<b>普通库</b>）。
 *
 * <p><b>这个类不是 {@code @Mod} 入口</b>，也没有构造器：core 模块由根 {@code build.gradle} 的
 * {@code jarJar(implementation(project(':core')))} 嵌进 {@code META-INF/jarjar/}，
 * 运行期是一个 unnamed module 上的普通 classpath 条目，<b>不参与 JPMS 模块解析、
 * 不出现在 mod 列表里</b>。它的包因此可以与 {@code createoreexpansion} 重叠——
 * 这正是 P3d 把 core 从 mod 改成库的原因（mod 文件之间不允许同包，见 core/build.gradle 的注释）。</p>
 *
 * <p><b>它留下什么</b>（三个静态成员，500+ 处引用，因此<b>原地不动</b>）：</p>
 * <ul>
 *   <li><b>注册命名空间</b> {@link #REGISTRY_NAMESPACE} —— 永远是 {@code createoreexpansion}，
 *       与 mod id <b>解耦</b>。任何 {@code ResourceLocation} 的 namespace、注册 id 拼接、
 *       语言键前缀、数据包路径都必须走它，<b>不许</b>再拿某个 mod 的 id 当命名空间用。</li>
 *   <li>{@link #modLoc(String)} —— 本模组唯一的 {@code ResourceLocation} 工厂（原
 *       {@code CreateOreExpansion.modLoc}）。</li>
 *   <li>{@link #LOGGER} —— 全模组唯一日志出口（logger 名仍是 {@code createoreexpansion}，
 *       所以日志前缀与拆分前逐字一致）。</li>
 * </ul>
 *
 * <p><b>原来挂在这里的注册动作去哪儿了</b>：数据组件（{@code AllDataComponents}）、
 * 实体类型（{@code AllEntityTypes}）、风扇加工类型（{@code AllFanProcessingTypes}）、
 * 统一交互的旋转载荷（{@code MachineRotatePayload}）以及配置（{@code AllConfig.SPEC}）
 * 的注册触发，已整体搬进 {@code CreateOreExpansion}（COE 的 {@code @Mod}）的构造器——
 * 那些本来就是那个 mod 的生命周期职责，库没有生命周期可言。</p>
 *
 * <p><b>配置文件名为什么仍挂在 createoreexpansion 的容器上</b>：{@code ModContainer.registerConfig}
 * 生成的文件名 = {@code <modid>-common.toml}。若挂到别的容器上，老玩家的
 * {@code createoreexpansion-common.toml} 会被静默弃用（设置"丢一次"）。
 * 因此注册动作搬到了 COE 的构造器里，但<b>目标容器</b>仍是 {@code createoreexpansion}，
 * 保证磁盘上的配置文件名一字不变。</p>
 */
public final class CoeCore {

    /**
     * <b>全模组唯一的注册命名空间</b>：{@code createoreexpansion}。
     *
     * <p>所有 {@code ResourceLocation.fromNamespaceAndPath(...)}、注册 id、配置键、语言键、
     * 数据包路径都用它——与任何 mod id 无关，因此各模块（各自 mod id 不同）产出的注册 id
     * 仍然全部落在 <b>同一个</b> 命名空间里，老存档/老数据包零影响。</p>
     */
    public static final String REGISTRY_NAMESPACE = "createoreexpansion";

    /**
     * 全模组唯一的日志出口。logger 名刻意用 {@link #REGISTRY_NAMESPACE}，
     * 所以日志里的 {@code [createoreexpansion/]} 前缀不变。
     */
    public static final Logger LOGGER = LogManager.getLogger(REGISTRY_NAMESPACE);

    /** 工具类，不允许实例化。 */
    private CoeCore() {
    }

    /**
     * 本模组唯一的 {@code ResourceLocation} 工厂。
     *
     * <p>命名空间恒为 {@link #REGISTRY_NAMESPACE}——<b>不是</b>任何 mod id，
     * 所以各模块（{@code createoreexpansion} / {@code cews} / {@code transmutation}）
     * 都能安全地共用它。</p>
     *
     * @param path 命名空间内的路径（如 {@code "block/jade_ore"}）
     * @return {@code createoreexpansion:<path>}
     */
    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, path);
    }
}
