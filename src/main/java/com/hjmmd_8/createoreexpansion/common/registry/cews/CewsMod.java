package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.EnergyWaveStudyTab;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFieldSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

/**
 * <b>CEWS（Create: Energy Wave Studies，能量波阵学）的 {@code @Mod} 入口</b>（P3b：四模块拆分）。
 *
 * <p>本类只负责"CEWS 这一层"的事，且全部落在一个 jar 里与另外三个 mod 共存：</p>
 * <ul>
 *   <li>本层自己的 {@link CewsRegistrate} 事件接线（注册命名空间仍是 {@code createoreexpansion}）；</li>
 *   <li>按层显式触发方块 → 方块实体 → 物品的类初始化（顺序与拆分前逐层一致）；</li>
 *   <li>{@link EnergyWaveStudyTab} 的页签内容构建（{@code onBuildContents}，清单只有一处）；</li>
 *   <li>{@link EnergyFieldSyncPayload} 能量场同步载荷；</li>
 *   <li>可选桥接：<b>Jade 的波实体提示插件</b> 与 <b>Sable 物理结构桥接</b>（判据与日志文案原样搬运）。</li>
 * </ul>
 *
 * <p><b>为什么不把 CEWS 的 Registrate 挂到 COE 的 mod 总线上</b>：NeoForge 的
 * {@code DatagenModLoader} 只为 {@code --mod} 指定的那个 mod 执行生成器，所以三个 Registrate 的
 * <b>datagen 提供器</b>由 {@code data/CreateOreExpansionDatagen} 统一在 createoreexpansion 的生成器上创建
 * （见 {@code common/registry/LayerRegistrate} 类注释）；而 <b>注册事件</b>
 * （{@code RegisterEvent}）是发往<b>每个</b> mod 总线的，挂在本 mod 自己的总线上同样会触发，
 * 所以这里按"每层一个 mod 总线"的干净写法接线。</p>
 */
@Mod(CewsMod.MOD_ID)
public class CewsMod {

    /** CEWS 模块的 mod id（<b>不是</b>注册命名空间——命名空间恒为 {@code createoreexpansion}）。 */
    public static final String MOD_ID = "cews";

    public CewsMod(IEventBus modEventBus, ModContainer modContainer) {
        CewsRegistrate.REGISTRATE.registerEventListeners(modEventBus);

        // 创造标签页：往 CEWS 页放清单、从基础页剔除（清单唯一处 = EnergyWaveStudyTab.CONTENTS）
        modEventBus.addListener(EnergyWaveStudyTab::onBuildContents);

        // 按层显式触发类初始化。顺序必须保持"方块 → 方块实体"（与拆分前逐层一致）。
        CewsBlocks.register();
        CewsBlockEntityTypes.register();
        CewsItems.register();

        // 能量场（加速/偏转/赋能）同步载荷
        modEventBus.addListener(EnergyFieldSyncPayload::registerPayloads);

        bootstrapJade();
        String sableCriterion = bootstrapSable();

        CoeCore.LOGGER.info("[CEWS] mod 初始化完成（mod id={}，注册命名空间={}）：能量波阵学机器已注册{}",
            MOD_ID, CoeCore.REGISTRY_NAMESPACE,
            sableCriterion == null ? "" : "，Sable 结构桥接判据=" + sableCriterion);
    }

    /**
     * Jade 可选集成：仅当 Jade 已安装时才反射加载插件类（未安装时绝不触碰 Jade 类，
     * 避免"标注 optional 仍硬编码调用导致崩溃"——见 compat.jade.WaveJadePlugin 注释）。
     *
     * <p><b>归属判定</b>：{@code WaveJadePlugin} 显示的是能量波实体/波情等 CEWS 内容，
     * 所以随 CEWS 模块走。另一个 Jade 插件 {@code BasinLiveJadePlugin}（工作盆物品行实时化）
     * 属于矿物拓展的加工线，留在 COE。</p>
     */
    private static void bootstrapJade() {
        if (ModList.get().isLoaded("jade")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.jade.WaveJadePlugin");
                CoeCore.LOGGER.info("[Jade] 能量波信息显示插件已加载（CEWS）");
            } catch (Throwable t) {
                CoeCore.LOGGER.warn("[Jade] 能量波信息显示插件加载失败（不影响游戏运行）", t);
            }
        }
    }

    /**
     * Sable 可选集成：仅当 Sable（航空学物理结构库）已安装时才反射加载桥接实现，
     * 让能量波与物理结构上的机器（充能器/波闸/差波器）通过位姿矩阵勾连（世界↔本地坐标）。
     * 未装 Sable 时绝不触碰 Sable 类（compat.sable.SableSubLevelBridge 直接引用 Sable 类型）。
     * 桥接注册与物理属性验证均在 SableSubLevelBridge 静态块内完成（Class.forName 触发）。
     *
     * <p>判据（2026-09-20 修正，原样搬运）：原先只看 modId "sable"，但实测 jar 里
     * libs/sable-companion-common-1.21.1-1.6.0.jar 的 modId 是 "sablecompanion"，
     * 而 libs/aeronautics-neoforge-1.21.1-1.3.0.jar 才声明依赖 modId "sable"；
     * 我们真正使用的类是 dev.ryanhcode.sable.companion.math.Pose3dc（来自前者）。
     * 用户实例里主 sable 未加载（或被 bundled 进嵌套 jar）→ 旧判据为假 → 整条物理结构链路惰性。
     * 新判据：① 先看类在不在（首选，直接对应我们依赖的东西）；② 再退化为任一 modId 命中。</p>
     *
     * <p><b>归属判定</b>：整条物理结构链路服务的是能量波（CEWS），因此桥接随 CEWS 走。</p>
     *
     * @return 命中的判据文案（未命中返回 {@code null}）
     */
    private static String bootstrapSable() {
        String sableByClass = detectSableByClass();
        String sableCriterion = (sableByClass != null) ? sableByClass : detectSableByModId();
        if (sableCriterion != null) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.sable.SableSubLevelBridge");
                CoeCore.LOGGER.info("[Sable] 物理结构桥接已加载（判据：{}）", sableCriterion);
            } catch (Throwable t) {
                CoeCore.LOGGER.warn("[Sable] 物理结构桥接加载失败（判据：{} 已命中，不影响游戏运行）", sableCriterion, t);
            }
        } else {
            CoeCore.LOGGER.warn("[Sable] 物理结构桥接未加载：类 dev.ryanhcode.sable.companion.math.Pose3dc 不在场，"
                + "且 modId sable / sablecompanion / aeronautics 均未加载（哪条判据都没命中）");
        }
        return sableCriterion;
    }

    /**
     * 判据①：我们真正依赖的 Sable 类在不在（运行期反射探测，编译期无需该类在场）。
     *
     * <p>用 {@code Class.forName(name, false, loader)} <b>不初始化</b>目标类，避免副作用；
     * 依次尝试上下文 / 本模组 / Minecraft（即游戏层，聚合了所有 mod jar）的类加载器，
     * 任一能加载到即视为"物理结构库在场"。</p>
     *
     * @return 命中时的判据文案（写进日志），未命中返回 {@code null}
     */
    private static String detectSableByClass() {
        // 与 compat.sable.SablePose 的 import 一致：dev.ryanhcode.sable.companion.math.Pose3dc
        final String probe = "dev.ryanhcode.sable.companion.math.Pose3dc";
        ClassLoader[] candidates = new ClassLoader[] {
            Thread.currentThread().getContextClassLoader(),
            CewsMod.class.getClassLoader(),
            net.minecraft.world.level.Level.class.getClassLoader(),
            ClassLoader.getSystemClassLoader(),
        };
        for (ClassLoader loader : candidates) {
            if (loader == null) {
                continue;
            }
            try {
                Class.forName(probe, false, loader);
                return "类 " + probe + " 在场";
            } catch (Throwable ignored) {
                // 换下一个类加载器继续探测
            }
        }
        return null;
    }

    /** 判据②（兜底）：Sable 主 jar / companion / 航空学 任一 modId 已加载。未命中返回 {@code null}。 */
    private static String detectSableByModId() {
        ModList modList = ModList.get();
        String[] modIds = { "sable", "sablecompanion", "aeronautics" };
        for (String modId : modIds) {
            if (modList.isLoaded(modId)) {
                return "modId " + modId + " 已加载";
            }
        }
        return null;
    }
}
