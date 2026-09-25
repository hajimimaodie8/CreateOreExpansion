package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.common.machine.MachineRotatePayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <b>core 模块的 {@code @Mod} 入口</b>（P3b：四模块拆分）。
 *
 * <p><b>这个类承担什么</b>：把「全模组共用的地基」从 {@code CreateOreExpansion} 里搬出来，
 * 让 CEWS（能量波阵学）等<b>不与矿物拓展互相编译依赖</b>的模块也能拿到同一套命名空间：</p>
 * <ul>
 *   <li><b>注册命名空间</b> {@link #REGISTRY_NAMESPACE} —— 永远是 {@code createoreexpansion}，
 *       与 mod id <b>解耦</b>。任何 {@code ResourceLocation} 的 namespace、注册 id 拼接、
 *       语言键前缀、数据包路径都必须走它，<b>不许</b>再拿某个 mod 的 id 当命名空间用。</li>
 *   <li>{@link #modLoc(String)} —— 本模组唯一的 {@code ResourceLocation} 工厂（原
 *       {@code CreateOreExpansion.modLoc}）。</li>
 *   <li>{@link #LOGGER} —— 全模组唯一日志出口（logger 名仍是 {@code createoreexpansion}，
 *       所以日志前缀与拆分前逐字一致）。</li>
 *   <li>配置（{@code AllConfig.SPEC}）、数据组件（{@code AllDataComponents}）、
 *       实体类型（{@code AllEntityTypes}）、风扇加工类型（{@code AllFanProcessingTypes}）、
 *       统一交互的旋转载荷（{@code MachineRotatePayload}）的注册触发。</li>
 * </ul>
 *
 * <p><b>mod id 与命名空间为什么必须拆开</b>：{@code CreateOreExpansion.MOD_ID} 原先既当 mod id
 * 又当命名空间用，两者恰好是同一个字符串。四模块拆分后 {@code createoreexpansion} <b>只代表
 * 矿物拓展（COE）这一个模块</b>，而所有注册内容仍属于同一个命名空间 {@code createoreexpansion}——
 * 于是「命名空间」这件事必须提升到不依赖任何单一模块的 core 层。</p>
 *
 * <p><b>配置文件名为什么仍写在 createoreexpansion 的容器上</b>：{@code ModContainer.registerConfig}
 * 生成的文件名 = {@code <modid>-common.toml}。若挂到 {@code coe_core} 的容器上，老玩家的
 * {@code createoreexpansion-common.toml} 会被静默弃用（设置"丢一次"）。因此这里把注册<b>动作</b>放在
 * core（职责归 core），但<b>目标容器</b>仍是 {@code createoreexpansion}，保证磁盘上的配置文件名一字不变。</p>
 */
@Mod(CoeCore.MOD_ID)
public class CoeCore {

    /** core 模块自己的 mod id（只用于 FML 装载，<b>不是</b>注册命名空间）。 */
    public static final String MOD_ID = "coe_core";

    /**
     * <b>全模组唯一的注册命名空间</b>：{@code createoreexpansion}。
     *
     * <p>所有 {@code ResourceLocation.fromNamespaceAndPath(...)}、注册 id、配置键、语言键、
     * 数据包路径都用它——与任何 mod id 无关，因此四个模块（各自 mod id 不同）产出的注册 id
     * 仍然全部落在 <b>同一个</b> 命名空间里，老存档/老数据包零影响。</p>
     */
    public static final String REGISTRY_NAMESPACE = "createoreexpansion";

    /**
     * 全模组唯一的日志出口。logger 名刻意用 {@link #REGISTRY_NAMESPACE}（= 拆分前
     * {@code CoeCore.REGISTRY_NAMESPACE} 的值），所以日志里的 {@code [createoreexpansion/]} 前缀不变。
     */
    public static final Logger LOGGER = LogManager.getLogger(REGISTRY_NAMESPACE);

    /** core 模块收到的 mod 事件总线（调试用；注册动作都在构造器里做完）。 */
    public static IEventBus MOD_BUS;

    public CoeCore(IEventBus modEventBus, ModContainer modContainer) {
        MOD_BUS = modEventBus;

        AllDataComponents.register(modEventBus);
        AllEntityTypes.register(modEventBus);
        // 风扇加工类型（Create 的注册表）：沿用拆分前的写法，RegisterEvent 每次触发都调用
        // （init() 自身幂等），行为与拆分前逐字一致。
        modEventBus.addListener(CoeCore::onRegister);
        // 统一交互规则第 4 条：Ctrl + 扳手右键 = 旋转本模组机器（客户端拦截 → 服务端校验并旋转）
        modEventBus.addListener(MachineRotatePayload::registerPayloads);

        // 配置：注册动作归 core，目标容器仍是 createoreexpansion（保住 createoreexpansion-common.toml）。
        // 注意 AllConfig 自身是 @EventBusSubscriber(modid = createoreexpansion)，
        // 只有在 createoreexpansion 容器上注册，ModConfigEvent 才会送达它——这也是必须挂该容器的第二个理由。
        ModList.get().getModContainerById("createoreexpansion")
            .ifPresentOrElse(
                container -> container.registerConfig(ModConfig.Type.COMMON, AllConfig.SPEC),
                () -> {
                    throw new IllegalStateException(
                        "[core] 找不到 createoreexpansion 的 ModContainer，无法注册 AllConfig.SPEC");
                });

        LOGGER.info("[core] mod 初始化完成（mod id={}，注册命名空间={}）：配置/数据组件/实体类型/风扇加工/旋转载荷已接线",
            MOD_ID, REGISTRY_NAMESPACE);
    }

    /** {@code RegisterEvent} 上的风扇加工类型注册钩子（语义与拆分前 {@code CreateOreExpansion.onRegister} 相同）。 */
    public static void onRegister(RegisterEvent event) {
        AllFanProcessingTypes.init();
    }

    /**
     * 本模组唯一的 {@code ResourceLocation} 工厂。
     *
     * <p>命名空间恒为 {@link #REGISTRY_NAMESPACE}——<b>不是</b>任何 mod id，
     * 所以四个模块（{@code coe_core} / {@code createoreexpansion} / {@code cews} / {@code transmutation}）
     * 都能安全地共用它。</p>
     *
     * @param path 命名空间内的路径（如 {@code "block/jade_ore"}）
     * @return {@code createoreexpansion:<path>}
     */
    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(REGISTRY_NAMESPACE, path);
    }
}
