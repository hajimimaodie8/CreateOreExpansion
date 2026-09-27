package com.hjmmd_8.createoreexpansion.common.registry;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.machine.MachineRotatePayload;

import net.neoforged.bus.api.IEventBus;

/**
 * <b>三个内容模块共用的「幂等接线入口」</b>（P7a：取代已删除的四个 hub 聚合入口
 * {@code AllRecipeTypes} / {@code AllCreativeModeTabs} / {@code AllFluids} / {@code AllModEffects}）。
 *
 * <h2>一、它解决什么问题</h2>
 * <p>拆成"可单独安装的三个 jar"之后，有三件事<b>必须恰好发生一次</b>，而任何单一模块都不保证在场：</p>
 * <ol>
 *   <li>把共享的 {@code DeferredRegister} 挂到 mod 事件总线
 *       （{@link LayerRecipeType#registerOn} 挂两张、{@link LayerCreativeTab#registerOn} 挂一张）；</li>
 *   <li>注册跨层共用的网络载荷（{@link MachineRotatePayload}，Ctrl+扳手右键的服务端半边）；</li>
 *   <li>按<b>固定顺序</b>唤醒三层的配方类型声明类，让条目进注册表的顺序与拆分前逐字相同。</li>
 * </ol>
 *
 * <p>这三件事原先住在根工程的 {@code common/hub/**} 聚合入口里，由 {@code IntegrationBootstrap} 在
 * {@code FMLConstructModEvent} 上跑。那个形态在"只发单模块 jar"的终局下不成立：根工程降级为
 * dev-only，发布形态里根本没有它。于是把它改成<b>控制反转</b>——动作搬进本类（core，零层引用），
 * 由三个模块各自的 {@code @Mod} 构造器各调一次 {@link #ensureAttached(IEventBus)}，
 * 第一个到的做实事，其余直接返回。</p>
 *
 * <h2>二、为什么必须幂等（不是"最好幂等"）</h2>
 * <p>以下两条都是<b>硬崩</b>，不是警告（源码实测，NeoForge 21.1.248）：</p>
 * <ul>
 *   <li>{@code DeferredRegister#register(IEventBus)} 二次调用 →
 *       {@code IllegalStateException("Cannot register DeferredRegister to more than one event bus.")}；</li>
 *   <li>{@code NetworkRegistry} 对同一个 payload id 二次注册 →
 *       {@code UnsupportedOperationException("... as it is already registered.")}
 *       —— 而 {@code RegisterPayloadHandlersEvent} 是发往<b>每一个</b> mod 容器的。</li>
 * </ul>
 *
 * <h2>三、为什么是 {@code synchronized} 而不是裸 {@code boolean}</h2>
 * <p><b>FML 的 mod 构造是并行派发的</b>：{@code ModLoader.constructMods} 走
 * {@code dispatchParallelTask("Mod Construction", parallelExecutor, …)}，三个 {@code @Mod}
 * 构造器在装载器的线程池里<b>同时</b>开跑。裸 {@code if (attached) return; attached = true;} 存在
 * 真实竞态窗口：两个线程都通过检查 ⇒ 上面的两条硬崩之一。所以这里用一把静态锁把"检查 + 落地"
 * 做成临界区，输的那个线程会阻塞到赢家做完，然后看到 {@code attached == true} 平静返回。</p>
 *
 * <h2>四、调用纪律（非常重要）</h2>
 * <p><b>{@link #ensureAttached(IEventBus)} 必须是每个 {@code @Mod} 构造器的第一条语句。</b>
 * 理由有二：</p>
 * <ol>
 *   <li><b>顺序</b>：配方类型条目进注册表的顺序由 {@link #wakeLayers()} 的固定名单决定；
 *       只要某一层在进本方法之前先碰了自己的 {@code XxxRecipeTypes}，那一层的条目就会插到前面，
 *       顺序就变了（可观测影响只有 {@code BuiltInRegistries.RECIPE_TYPE} / {@code RECIPE_SERIALIZER}
 *       的枚举顺序——它们不进存档、不参与网络同步——但"逐字相同"这条口径就断了）。</li>
 *   <li><b>死锁窗口</b>：本方法在持锁期间做 {@code Class.forName}（类初始化）。若某个模块先启动了
 *       别的类初始化、再回头进本方法，就可能与另一个正在本方法里唤醒同一批类的线程形成
 *       "A 持类锁等本锁 / B 持本锁等类锁"的环。放在构造器第一条语句时，任何线程都不可能在
 *       持着本模组类初始化锁的情况下等这把锁。</li>
 * </ol>
 *
 * <h2>五、零层引用（红线）</h2>
 * <p>本类<b>不 import</b> 任何 {@code common.registry.coe|cews|transmutation} 的类，也不出现它们的
 * 全限定名字面量：{@link #wakeLayers()} 的类名由本类自己的包名（{@code getPackageName()}）
 * 加<b>简单名</b>在运行时拼出来（与 {@link WaveRecipeCapabilities} 同一手法）。
 * 因此 {@code tools/check-layering.ps1} 的 {@code CORE -> COE/CEWS/TRANS} 判据在这里恒不成立，
 * 而 layering 断言仍能机械核验"core 不许认识任何层"。</p>
 *
 * <p><b>永不抛</b>：某一层不在场时（例如只装了 cews.jar）{@code Class.forName} 抛
 * {@code ClassNotFoundException}，这里一律吞掉并继续——"少一层"是设计内的组合，不是错误路径。
 * 这正是 {@link WaveRecipeCapabilities#ensureInitialized()} 的既有范式。</p>
 */
public final class LayerBootstrap {

    /** 临界区锁（见类注释"三"）。 */
    private static final Object LOCK = new Object();

    /** 幂等标志（只在 {@link #LOCK} 的临界区内读写）。 */
    private static boolean attached;

    /**
     * 要唤醒的配方类型声明类（<b>固定顺序 = 拆分前 {@code AllRecipeTypes} 里六个常量的层间顺序</b>：
     * TRANS → COE → CEWS）。
     *
     * <p><b>W6-b2 起名单收短成两条</b>（方案 §2 的读法 (ii)）：嬗化机制整块搬进 {@code :coe} 之后，
     * {@code transmutation.TransmutationRecipeTypes} 与 {@code coe.CoeRecipeTypes} 同属第一层，
     * 名单里不再需要单独一项——{@code CoeRecipeTypes} 的<b>第一个字段</b>就是
     * {@code TRANSMUTING} 的转发声明，读它会先把 {@code TransmutationRecipeTypes} 初始化掉，
     * 所以 {@code transmuting} 仍然排在 {@code lightning} 之前（六个条目的数值注册 id 7..12
     * 逐字不变，实测见 {@code build/patch/w6b2-EVIDENCE.txt}）。
     * <b>顺序语义依旧承重</b>：{@code cews.CewsRecipeTypes}（{@code charging}）必须留在最后。</p>
     *
     * <p>字段初始化器在静态块之前执行（文本顺序），且本类是纯静态工具、没有静态块，
     * 所以这里不会踩到"类初始化未完成就取字段"的坑。</p>
     */
    private static final String[] LAYER_RECIPE_TYPE_CLASSES = layerRecipeTypeClassNames();

    private LayerBootstrap() {
    }

    /**
     * 幂等地完成三件共享接线（见类注释"一"）。任何模块的 {@code @Mod} 构造器都可以调，且必须
     * <b>放在构造器第一条语句</b>（见类注释"四"）。
     *
     * @param modEventBus 调用方自己那个 mod 文件的 mod 事件总线。挂哪一条不影响结果：
     *                    {@code RegisterEvent} 与 {@code RegisterPayloadHandlersEvent} 都是发往
     *                    <b>每一个</b> mod 容器的；而"恰一次"由本类的幂等标志保证。
     */
    public static void ensureAttached(IEventBus modEventBus) {
        if (modEventBus == null) {
            // 宁可什么都不做也不静默挂到 null 上：缺总线时下游会以"注册表未挂"的形式炸得很难看。
            throw new IllegalArgumentException("modEventBus must not be null");
        }
        synchronized (LOCK) {
            if (attached) {
                return;
            }
            attached = true;

            // ① 共享注册表挂总线（每张只此一次；二次挂 = IllegalStateException）
            LayerRecipeType.registerOn(modEventBus);
            LayerCreativeTab.registerOn(modEventBus);

            // ② 跨层共用的网络载荷（二次注册 = UnsupportedOperationException）。
            //    原先这一句写在 :coe 的 CreateOreExpansion 构造器里 ⇒ 只装 cews.jar 时
            //    Ctrl+扳手在 CEWS 机器上完全没反应（载荷不在注册表里）。搬到幂等入口后，
            //    "在场且第一个构造"的那个模块负责注册它。
            modEventBus.addListener(MachineRotatePayload::registerPayloads);

            // ③ 按固定顺序唤醒三层（顺序理由见类注释"四"1.）
            wakeLayers();
        }
    }

    /** 本类的接线是否已经完成（诊断用；模块自身不该依赖它做分支）。 */
    public static boolean isAttached() {
        synchronized (LOCK) {
            return attached;
        }
    }

    /**
     * 拼出配方类型声明类的全名（见类注释"五"）。
     *
     * <p>包前缀由本类自己的包自报（本类与那两个类同住 {@code …common.registry} 的不同子包），
     * 只留<b>子包 + 简单名</b>的字面量，因此源码里不出现任何层的全限定名。
     * 拼出来的结果与拆分前逐字相同，行为零变化。</p>
     *
     * <p><b>W6-b2</b>：第一项 {@code transmutation.TransmutationRecipeTypes} 已从名单里删掉
     * （它现在与 {@code coe.CoeRecipeTypes} 同住 {@code :coe}，由后者的第一个字段转发触发，
     * 见 {@link #LAYER_RECIPE_TYPE_CLASSES}）。<b>不要把它加回来</b>：那会让
     * {@code transmuting} 被"提前"唤醒一次，虽然条目顺序不变（同一张 {@code DeferredRegister}
     * 按 id 去重），但名单与 {@code WaveRecipeCapabilities} 的口径就不再一一对应了。</p>
     */
    private static String[] layerRecipeTypeClassNames() {
        String prefix = LayerBootstrap.class.getPackageName() + ".";
        return new String[] {
            prefix + "coe.CoeRecipeTypes",
            prefix + "cews.CewsRecipeTypes"
        };
    }

    /**
     * 按 {@link #LAYER_RECIPE_TYPE_CLASSES} 的顺序唤醒三层的配方类型声明类（各自在自己的静态块里
     * 创建 {@link LayerRecipeType} 常量 ⇒ 条目按同样顺序进共享注册表）。
     *
     * <p>幂等且永不抛：某一层不在场时该层被静默跳过（见类注释"五"）。
     * 类加载器先试本类自己的（与 {@link WaveRecipeCapabilities} 同款），失败再退到系统类加载器。</p>
     */
    private static void wakeLayers() {
        for (String name : LAYER_RECIPE_TYPE_CLASSES) {
            try {
                Class.forName(name, true, LayerBootstrap.class.getClassLoader());
            } catch (Throwable missing) {
                try {
                    Class.forName(name, true, ClassLoader.getSystemClassLoader());
                } catch (Throwable ignored) {
                    CoeCore.LOGGER.debug("[P7a] 层接线：{} 不在场（该层未安装），跳过", name);
                }
            }
        }
    }
}
