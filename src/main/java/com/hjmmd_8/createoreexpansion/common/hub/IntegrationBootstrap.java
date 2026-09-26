package com.hjmmd_8.createoreexpansion.common.hub;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerCreativeTab;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * <b>根侧集成触发器</b>（P3w）——{@code :coe} 拆出去之后，hub 的注册触发与
 * 被孤立的 {@code @EventBusSubscriber} 的补挂都住在这里。
 *
 * <h2>一、为什么需要它</h2>
 * <p>P3w 把矿物拓展（176 个文件 + {@code @Mod} 入口 {@code CreateOreExpansion}）整体搬进
 * Gradle 子模块 {@code :coe}，而 {@code :coe} 只允许编译期依赖 {@code :core}
 * （root → :coe 已经是单向边，反向再加一条就是构图期的
 * {@code Circular dependency between the following tasks}）。于是原先写在
 * {@code CreateOreExpansion} 构造器里的 9 处跨层触发点必须搬出 {@code :coe}：</p>
 * <ul>
 *   <li>CEWS 自己的 {@code AllEntityTypes.register} → {@code CewsMod} 构造器；</li>
 *   <li>TRANS 自己的 {@code AllModPotions} / {@code AllFanProcessingTypes} → {@code TransmutationMod}；</li>
 *   <li>hub 的 5 处（见"三"）→ <b>本类</b>。</li>
 * </ul>
 *
 * <h2>二、为什么是"事件触发"而不是"CEWS / TRANS 构造器直接调一下"</h2>
 * <p>本类住 {@code common/hub}（SHARED 层）。如果让 {@code CewsMod} / {@code TransmutationMod}
 * 直接 import 本类并调用，就会造出本工程<b>第一条</b> {@code CEWS -> 根侧 SHARED} 与
 * {@code TRANS -> 根侧 SHARED} 的源码边 —— 而 {@code tools/layer-usage.ps1} 的 LAYER-NO
 * 判据正是"该层文件的整个项目闭包必须落在 CORE + 本层 + 允许的下层"，根侧 SHARED
 * 一律算 <b>blocker</b>（P3q/P3s：那是"这一层还不能独立成模块"的工作清单）。
 * 实测：直接调用会让 CEWS 与 TRANS 各多出一个 LAYER-NO 文件（基线是 0）。
 * 所以改成<b>事件触发</b>：本类自己挂 {@code @EventBusSubscriber}（P3z 起挂根工程自己的 mod
 * {@code coe_integration}），FML 在<b>那个 mod</b> 构造完成、自动订阅者注入之后立刻派发
 * {@code FMLConstructModEvent}
 * （{@code FMLModContainer#constructMod} 与 {@code ModLoader#constructMods} 的次序），
 * 而 {@code RegisterEvent} 是<b>全部 mod 构造完之后</b>才由 NeoForge 派发的 ——
 * 于是"挂 DeferredRegister"这件事照样早于 {@code RegisterEvent}，
 * 同时 CEWS / TRANS 两个层文件对本类<b>零引用</b>。</p>
 * <p>附带好处：SHARED 文件从不被两个工具当作 source 层判定，所以这条接线本身
 * 不会给任何层加 blocker；而它要做的两件事（hub 触发 / 补挂订阅者）本来就属于集成层。</p>
 *
 * <h2>三、hub 触发</h2>
 * <p>顺序与拆分前 {@code CreateOreExpansion} 构造器里逐条一致：
 * {@code installTabRegistrar} → {@code AllCreativeModeTabs.register} → {@code AllFluids.register}
 * → {@code AllModEffects.register} → {@code AllRecipeTypes.register}。
 * 其中只有第一个真正对时序敏感，而 {@code LayerCreativeTab.installTabRegistrar} 对
 * "注入之前就来过的请求"会<b>补跑</b>（见其类注释），所以触发时机（甚至 mod 构造顺序）
 * 都不影响结果。</p>
 *
 * <h2>四、补挂被孤立的 {@code @EventBusSubscriber}（P3w 实测发现）</h2>
 * <p><b>FML 的 {@code @EventBusSubscriber} 自动注入是"按 mod 文件"作用域的</b>：
 * {@code FMLModContainer} 构造完 mod 之后调
 * {@code AutomaticEventSubscriber.inject(this, this.scanResults, layer)}，用的是
 * <b>该文件</b>的扫描结果，并且只挂 {@code Objects.equals(mod.getModId(), modId)} 的那些类
 * （loader 4.0.42 的 {@code AutomaticEventSubscriber} / {@code FMLJavaModLanguageProvider}；
 * 实测 {@code run/logs/debug.log}：createoreexpansion / transmutation / cews 各被 inject 一次，
 * 用的是同一份 scanResults）。</p>
 * <p>P3w 之后根工程的模板不再声明 {@code createoreexpansion}（它随 {@code :coe} 走），
 * 于是<b>所有仍住根工程、却写着 {@code modid = createoreexpansion} 的订阅类都不会再被注入</b>。
 * 这些类一个都不能丢：数据生成入口、配置重载、Ctrl+扳手旋转、能量场命令、
 * 嬗变玩家 tick、凝能佩弓事件……所以在这里<b>显式</b>补挂到 createoreexpansion 容器的总线上，
 * 路由规则与 {@code AutomaticEventSubscriber} 逐字同构（{@code IModBusEvent} 的子类挂 mod 总线，
 * 其余挂 game 总线；{@code Dist} 过滤在<b>不加载目标类</b>的前提下从扫描数据里判，
 * 免得专用服务器上加载客户端类）。</p>
 * <p><b>维护提醒</b>：以后凡是<b>住根工程</b>却写 {@code @EventBusSubscriber(modid = CoeCore.MOD_ID)}
 * 的新类都会被自动补挂（本类按扫描数据遍历，不需要改名单）。
 * <b>反面同样要查（P3y/P3z 实证）</b>：住在<b>某个层模块</b>里、却标着另一个 modid 的类
 * （镜像形态）既不被本文件所在的容器注入、也不在本类的扫描数据里，会静默失效 ——
 * 判据是"类的 modid == 它所在 mod 文件的 id"，搬完任何一层之后都要重跑这项审计。</p>
 * <p>P3z 起根工程自己是一个 mod（{@link IntegrationMod}），所以本类仍有一个容器可挂 ——
 * 若根文件的 {@code [[mods]]} 为空，本类连注入都不会发生（见 {@link IntegrationMod} 类注释"一"）。</p>
 */
@EventBusSubscriber(modid = IntegrationBootstrap.ROOT_MOD_ID)
public final class IntegrationBootstrap {

    /**
     * 根工程那个 mod 文件的 mod id（P3z 起 = {@link IntegrationMod#MOD_ID}，即 {@code coe_integration}；
     * 见 {@code src/main/templates/META-INF/neoforge.mods.toml}）。本类作为共享层文件挂它的
     * mod 总线：那个容器既是"触发时机"的来源，也是"扫描结果"的来源。
     */
    public static final String ROOT_MOD_ID = IntegrationMod.MOD_ID;

    /** 幂等标志：mod 构造是并行派发的，只做一次。 */
    private static boolean installed;

    private IntegrationBootstrap() {
    }

    /**
     * 触发点：FML 在<b>每个</b> mod 构造完成、自动订阅者注入之后派发
     * {@code FMLConstructModEvent}；本类只挂在 {@link #ROOT_MOD_ID} 的 mod 总线上（见类注释"二"）。
     * 这一步一定早于任何 {@code RegisterEvent}。
     */
    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
        // 事件本身不提供容器（ModLifecycleEvent#getContainer() 是包私有的），按 mod id 从 ModList 取。
        ModContainer rootMod = ModList.get().getModContainerById(ROOT_MOD_ID).orElse(null);
        if (rootMod == null) {
            CoeCore.LOGGER.error("[P3w] 找不到 {} 的 ModContainer，hub 注册触发与订阅者补挂都无法进行", ROOT_MOD_ID);
            return;
        }
        install(rootMod);
    }

    /**
     * @param rootMod 触发者所在的 mod 容器（根工程的 mod 文件，P3z 起它的 mod id 是
     *                {@link IntegrationMod#MOD_ID}）。既提供挂 DeferredRegister 的总线，
     *                也提供那个文件的扫描结果。
     */
    private static void install(ModContainer rootMod) {
        if (installed) {
            return;
        }
        installed = true;

        IEventBus rootBus = rootMod.getEventBus();

        // ── ① hub 触发（顺序与拆分前 CreateOreExpansion 构造器内逐条一致）──────────────
        // P3q：创造页的登记动作由根侧注入（层的 Registrate 发请求、hub 按 COE → CEWS 顺序登记）。
        LayerCreativeTab.installTabRegistrar(AllCreativeModeTabs::registerTabs);
        AllCreativeModeTabs.register(rootBus);
        AllFluids.register();
        AllModEffects.register(rootBus);
        AllRecipeTypes.register(rootBus);

        // ── ② 补挂被孤立的 @EventBusSubscriber ─────────────────────────────────────────
        reattachOrphanedSubscribers(rootMod);
    }

    /**
     * 把"住根工程、却声明 {@code modid = createoreexpansion}"的 {@code @EventBusSubscriber}
     * 类补挂到 createoreexpansion 容器的总线上（见类注释"四"）。
     */
    private static void reattachOrphanedSubscribers(ModContainer rootMod) {
        Optional<? extends ModContainer> coeContainer = ModList.get().getModContainerById(CoeCore.MOD_ID);
        if (coeContainer.isEmpty()) {
            CoeCore.LOGGER.error("[P3w] 找不到 {} 的 ModContainer，无法补挂被孤立的 @EventBusSubscriber"
                + "（订阅类会整体失效，请检查 :coe 的 mods.toml）", CoeCore.MOD_ID);
            return;
        }
        IEventBus coeBus = coeContainer.get().getEventBus();
        if (coeBus == null) {
            CoeCore.LOGGER.error("[P3w] {} 的 ModContainer 还没有事件总线，补挂跳过", CoeCore.MOD_ID);
            return;
        }
        // game 总线取 FML 自己的那个实例（AutomaticEventSubscriber 也是取它），
        // 保证两条路径挂到同一个总线上。
        IEventBus gameBus = FMLLoader.getBindings().getGameBus();

        ModFileScanData scan;
        try {
            scan = rootMod.getModInfo().getOwningFile().getFile().getScanResult();
        } catch (Throwable t) {
            CoeCore.LOGGER.error("[P3w] 取不到根 mod 文件的扫描结果，补挂被孤立的 @EventBusSubscriber 跳过", t);
            return;
        }
        if (scan == null) {
            CoeCore.LOGGER.error("[P3w] 根 mod 文件扫描结果为 null，补挂被孤立的 @EventBusSubscriber 跳过");
            return;
        }

        Set<String> seen = new HashSet<>();
        List<String> attached = new ArrayList<>();
        for (ModFileScanData.AnnotationData ad : scan.getAnnotations()) {
            if (ad.targetType() != ElementType.TYPE) {
                continue;
            }
            if (!EventBusSubscriber.class.getName().equals(ad.annotationType().getClassName())) {
                continue;
            }
            // 只处理**显式写了** modid = createoreexpansion 的：
            // 没写 modid 的类由 FML 回退到"本文件第一个 modId"照常注入，不要重复挂；
            // 写别的 modid 的更不归我们管。
            if (!CoeCore.MOD_ID.equals(ad.annotationData().get("modid"))) {
                continue;
            }
            if (!matchesCurrentDist(ad)) {
                continue;
            }
            String className = ad.clazz().getClassName();
            if (!seen.add(className)) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className, true, IntegrationBootstrap.class.getClassLoader());
                attach(clazz, coeBus, gameBus);
                attached.add(clazz.getName());
            } catch (Throwable t) {
                CoeCore.LOGGER.error("[P3w] 补挂 @EventBusSubscriber 失败：{}（该类的监听会整体失效）", className, t);
            }
        }
        CoeCore.LOGGER.info("[P3w] 已补挂 {} 个被孤立的 @EventBusSubscriber（modid={}）：{}",
            attached.size(), CoeCore.MOD_ID, attached);
    }

    /**
     * 扫描数据里的 {@code @EventBusSubscriber.value()} 是否包含当前 Dist
     * （<b>不加载目标类</b>；元素的运行时类型是 loader 内部的 {@code ModAnnotation.EnumHolder}，
     * 这里只用反射读它的 {@code value()}，免得把那个内部包拉进编译面）。
     * 注解没写 {@code value} 时等于两个 Dist 都要（与 {@code AutomaticEventSubscriber.getSides(null)} 同义）。
     */
    private static boolean matchesCurrentDist(ModFileScanData.AnnotationData ad) {
        Object raw = ad.annotationData().get("value");
        if (!(raw instanceof List<?> sides) || sides.isEmpty()) {
            return true;
        }
        for (Object side : sides) {
            try {
                Object value = side.getClass().getMethod("value").invoke(side);
                if (FMLEnvironment.dist.name().equals(String.valueOf(value))) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
                // 读不出来就当作命中，宁可多挂一个也不静默丢
                return true;
            }
        }
        return false;
    }

    /**
     * 路由规则与 {@code AutomaticEventSubscriber} 同构：纯 mod 总线 → 整类挂 mod 总线；
     * 纯 game 总线 → 整类挂 game 总线；两者混装 → 逐方法分别挂。
     */
    private static void attach(Class<?> clazz, IEventBus modBus, IEventBus gameBus) throws ReflectiveOperationException {
        List<Method> modBusListeners = new ArrayList<>();
        List<Method> gameBusListeners = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                throw new IllegalArgumentException("Method " + method + " annotated with @SubscribeEvent must have exactly one parameter");
            }
            if (IModBusEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                modBusListeners.add(method);
            } else {
                gameBusListeners.add(method);
            }
        }
        if (modBusListeners.isEmpty()) {
            gameBus.register(clazz);
        } else if (gameBusListeners.isEmpty()) {
            modBus.register(clazz);
        } else {
            for (Method method : modBusListeners) {
                modBus.register(method);
            }
            for (Method method : gameBusListeners) {
                gameBus.register(method);
            }
        }
    }
}
