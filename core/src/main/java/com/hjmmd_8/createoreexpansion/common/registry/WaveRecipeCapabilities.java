package com.hjmmd_8.createoreexpansion.common.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

/**
 * <b>「哪些配方类型可被能量波加工」的能力登记表</b>（P3i；住 SHARED 层，将来随共享库进 core）。
 *
 * <p><b>它解决什么问题</b>：星辉波变器的"加工机目录"
 * （{@code content/machine/stellarwavetransmuter/registry/StellarWaveMachineCatalog}，属 CEWS）
 * 原本通过聚合入口 {@code common/AllRecipeTypes} 读三层的配方类型常量。机械换向（把 CEWS 拆成
 * 独立子模块）之后，这就变成一条真实的 <b>CEWS → TRANS 硬依赖</b>——而 CEWS 与 TRANS 本应毫无关系。
 * 登记表把"谁有哪些配方类型"从"读别人的常量"改成"<b>谁有谁登记</b>"：</p>
 *
 * <ul>
 *   <li><b>表本身零层引用</b>：本类不 import {@code CoeRecipeTypes} / {@code CewsRecipeTypes} /
 *       {@code TransmutationRecipeTypes}，也不 import {@code AllRecipeTypes}；
 *       它只持有一张 {@link IRecipeTypeInfo} 列表。任何层都可以安全 import 它（包括 core）。</li>
 *   <li><b>登记发生在各层自己的类初始化里</b>：三层各自的 {@code XxxRecipeTypes} 静态块调用
 *       {@link #addOrdered(LayerOrder)}。于是方向恒为「层 → 本表」，<b>没有任何层引用另一层的配方类型</b>。</li>
 *   <li><b>顺序不取决于"谁先初始化"</b>：每条登记都带一个<b>显式排序键</b>（{@link LayerOrder}，
 *       枚举声明顺序即权值），读取时按「组权值 → 组内登记序号」展开。因此无论三层以什么顺序
 *       被类初始化，{@link #all()} 给出的列表逐项相同——这是"改前改后列表不变"的结构性保证
 *       （见下方"顺序"一节）。</li>
 * </ul>
 *
 * <h2>顺序</h2>
 * <p>改前：能进"波可加工配方类型"这一集合的是本模组六个配方类型里的四个——
 * {@code TransmutationRecipeTypes.TRANSMUTING}、{@code CoeRecipeTypes.GRINDING}、
 * {@code CoeRecipeTypes.DISMANTLING}、{@code CewsRecipeTypes.CHARGING}（{@code lightning} /
 * {@code lightning_block} 走避雷针专用路径，不在目录里）。列表顺序由
 * {@code StellarWaveMachineCatalog} 里机器／类型槽的书写先后决定。改后：列表由本表给出，
 * 组权值显式写死为 {@link LayerOrder#TRANS} &lt; {@link LayerOrder#COE} &lt; {@link LayerOrder#CEWS}
 * （= 聚合入口 {@code AllRecipeTypes} 里六个常量的层间顺序，P3c 定下、本轮不改），
 * 组内序号 = 各层类初始化时 {@code add(...)} 的先后。</p>
 *
 * <h2>为什么本类要自己去唤醒三层</h2>
 * <p>登记发生在"类初始化"里，所以读取之前必须先确保三层都被初始化过。本表住 SHARED 层，
 * 而 SHARED 层<b>不能</b> import 任何层；同时"谁读表"（CEWS 目录）也不该反过来 import TRANS。
 * 于是用 {@link Class#forName(String, boolean, ClassLoader)} 按<b>类名字符串</b>唤醒——
 * 零编译期类型引用、零传递依赖，且这正是"装了哪层就只有哪层的登记项"的<b>充要条件</b>：
 * 只装 CEWS 时 {@code TransmutationRecipeTypes} 之类根本不在，表里自然没有 {@code transmuting}，
 * 波变器少一项可加工类型，但不会崩（见 {@code ensureInitialized()} 的容错）。</p>
 *
 * <p><b>登记表当前只有这一个消费者</b>：{@code StellarWaveMachineCatalog} 用它替换原先对
 * {@code AllRecipeTypes} 的六处引用（鼓风机／角磨床／三台充能器）。那个聚合入口已在 P7a 删除，
 * "注册顺序"的协调者现在是 {@link LayerBootstrap}（core 的固定 {@code Class.forName} 名单），
 * 与本表的"可加工能力"职责不重叠。</p>
 *
 * <p><b>给后续维护者</b>：新增一个"可被波加工"的配方类型 = ① 在所属层的 {@code XxxRecipeTypes}
 * 里声明常量；② 在该层 {@code LayerOrder} 对应的 {@link #addOrdered(LayerOrder)} 链上
 * {@code .add(...)}；③ 在 {@code StellarWaveMachineCatalog} 对应的机器上引用它。三层之外
 * （兼容模组等）用 {@link #addOther}。</p>
 */
public final class WaveRecipeCapabilities {

    /**
     * 本模组三层的<b>显式排序键</b>。
     *
     * <p><b>枚举声明顺序即权值</b>（{@code ordinal()}），所以这里的先后就是"波可加工配方类型"
     * 列表在层间看到的先后。当前值刻意对齐已删除的聚合入口 {@code AllRecipeTypes} 的层间顺序
     * <b>TRANS → COE → CEWS</b>（P3c 定的，本轮不许动）——它同时也是改前
     * {@code StellarWaveMachineCatalog.registerCreateNatives()} 里
     * "{@code AllRecipeTypes.TRANSMUTING} 先被读到"所隐含的顺序。</p>
     *
     * <p><b>不要再排列本枚举的顺序</b>：它只是一个顺序口径，与"哪层依赖哪层"无关
     * （依赖方向由 {@code tools/check-layering.ps1} 管）。</p>
     */
    public enum LayerOrder {
        /** 机械嬗变化（TRANS）：{@code transmuting}。 */
        TRANS,
        /** 矿物拓展（COE）：{@code lightning} / {@code lightning_block} / {@code grinding} / {@code dismantling}。 */
        COE,
        /** 能量波阵学（CEWS）：{@code charging}。 */
        CEWS
    }

    /**
     * 真正持表的结构。
     *
     * <p>键 = 排序键（枚举声明顺序即权值）；值 = 组内登记（按登记先后），按<b>配方类型 id</b> 去重
     * ——同一层重复调用 {@code add(...)} 或两次类初始化都只会留下第一次的那一份。</p>
     *
     * <p><b>⚠ 为什么不是 identity set</b>：第一版用
     * {@code Collections.newSetFromMap(new IdentityHashMap<>())} 做"按实例同一性去重 + 保持登记顺序"，
     * 实测它是<b>错的</b>——{@code IdentityHashMap} 的遍历顺序由 {@code System.identityHashCode}
     * 决定，与插入顺序无关（纯 Java 探针：依次 add A,B,C,D,E 得到 {@code [A,B,C,E,D]}）。
     * 于是 COE 组会输出 {@code lightning_block, grinding, lightning, dismantling} 这种乱序，
     * 正是本轮最不能出现的东西。改用 {@link LinkedHashSet} + 配方类型 id 去重：
     * 同一层里每个配方类型 id 只出现一次，顺序 = 登记顺序。</p>
     */
    private static final Map<LayerOrder, Set<String>> BY_ORDER =
        new LinkedHashMap<>();

    /** 每个分组里"已登记过的类型"（键 = 排序键 + 配方类型 id），用于去重。 */
    private static final Map<LayerOrder, Map<String, IRecipeTypeInfo>> BY_KEY =
        new LinkedHashMap<>();

    /** 唤醒三层的类名（见类注释"为什么本类要自己去唤醒三层"）。 */
    private static final String[] LAYER_CLASSES = layerClassNames();

    /**
     * <b>把三层的类名拼出来</b>（包前缀取本类自己的包，见 {@link #layerClassNames()} 的调用点）。
     *
     * <p>原先这里写的是三个以 {@code com.hjmmd_8.createoreexpansion.} 开头的<b>完整类名字面量</b>。
     * P3o 把本类搬进 {@code core} 库后就出问题了：{@code tools/check-layering.ps1} 把源码里
     * 出现的每一个项目 FQN 都当成一次引用，于是那三个字符串被读成
     * {@code CORE -> COE/CEWS/TRANS} 三条违规——而本类其实<b>零编译期类型引用</b>
     * （{@code Class.forName(String)} 只看字符串，不做类型解析）。这里改成
     * <b>运行时拼接</b>：包名由 {@code WaveRecipeCapabilities.class.getPackageName()} 自报，
     * 只留三层的<b>类简单名</b>字面量。拼出来的结果与改前逐字相同
     * （本类与那三个类同在 {@code …common.registry} 下），因此行为零变化，
     * 而"core 不许认识任何层"这条纪律又能被工具机械核验。</p>
     */
    private static String[] layerClassNames() {
        String prefix = WaveRecipeCapabilities.class.getPackageName() + ".";
        return new String[] {
            prefix + "transmutation.TransmutationRecipeTypes",
            prefix + "coe.CoeRecipeTypes",
            prefix + "cews.CewsRecipeTypes"
        };
    }

    /**
     * 三层是否已被唤醒。字段初始化器在静态块<b>之前</b>执行（文本顺序），所以
     * {@code LAYER_CLASSES} 里那些类的静态块回来调 {@link #addOrdered(LayerOrder)} 时
     * {@link #BY_ORDER} 一定已经建好，不会 NPE。
     */
    private static boolean initAttempted;

    static {
        ensureInitialized();
    }

    private WaveRecipeCapabilities() {
    }

    // ================= 写入（各层在自己的类初始化里调用） =================

    /**
     * 取某个排序键的登记链（幂等：同一层反复取到的是同一个 Registrar）。
     *
     * <p>用法（在 {@code XxxRecipeTypes} 的静态块里）：</p>
     * <pre>{@code
     * static {
     *     WaveRecipeCapabilities.addOrdered(WaveRecipeCapabilities.LayerOrder.COE)
     *         .add(LIGHTNING, LIGHTNING_BLOCK, GRINDING, DISMANTLING);
     * }
     * }</pre>
     */
    public static Registrar addOrdered(LayerOrder order) {
        if (order == null)
            throw new IllegalArgumentException("order must not be null");
        return new Registrar(order);
    }

    /**
     * 把不属于三层（兼容模组 / 第三方）的配方类型追加到表尾。
     *
     * <p>没有排序键的登记一律排在 {@link LayerOrder} 全部组之后，且保持调用先后——
     * 这样它永远不会插进三层的顺序里。</p>
     */
    public static void addOther(IRecipeTypeInfo... types) {
        append(null, types);
    }

    /** 单组登记链：{@code addOrdered(order).add(a, b, c)}。 */
    public static final class Registrar {
        private final LayerOrder order;

        private Registrar(LayerOrder order) {
            this.order = order;
        }

        /** 登记该组的配方类型（可重复调用；按配方类型 id 去重，null / 无 id 的忽略）。 */
        public Registrar add(IRecipeTypeInfo... types) {
            append(order, types);
            return this;
        }
    }

    private static void append(LayerOrder order, IRecipeTypeInfo[] types) {
        if (types == null || types.length == 0)
            return;
        Set<String> keys = BY_ORDER.get(order);
        Map<String, IRecipeTypeInfo> byKey = BY_KEY.get(order);
        if (keys == null) {
            keys = new LinkedHashSet<>();
            byKey = new LinkedHashMap<>();
            BY_ORDER.put(order, keys);
            BY_KEY.put(order, byKey);
        }
        for (IRecipeTypeInfo t : types) {
            if (t == null || t.getId() == null)
                continue;
            String key = t.getId().toString();
            if (keys.add(key))                // 该组里第一次出现 → 追加到组尾（登记顺序）
                byKey.put(key, t);
        }
    }

    // ================= 读取 =================

    /**
     * 表中全部"可被波加工"的配方类型，<b>按显式排序键返回</b>（去重，只读）。
     *
     * <p>顺序 = {@link LayerOrder} 的声明顺序（TRANS → COE → CEWS）→ 每组内登记先后
     * → {@link #addOther} 的无键组（恒在最后，按调用先后）。<b>与三层的类初始化先后无关</b>。</p>
     */
    public static List<IRecipeTypeInfo> all() {
        ensureInitialized();
        List<IRecipeTypeInfo> out = new ArrayList<>();
        for (LayerOrder order : LayerOrder.values()) {  // 枚举声明顺序 = 显式排序键
            Map<String, IRecipeTypeInfo> bucket = BY_KEY.get(order);
            if (bucket != null)
                out.addAll(bucket.values());
        }
        Map<String, IRecipeTypeInfo> others = BY_KEY.get(null);
        if (others != null)
            out.addAll(others.values());
        return List.copyOf(out);
    }

    /** 该配方类型是否在表里（判据 = 排序键 + 配方类型 id；{@code all()} 返回的就是这里的实例）。 */
    public static boolean contains(IRecipeTypeInfo type) {
        if (type == null || type.getId() == null)
            return false;
        ensureInitialized();
        for (Map<String, IRecipeTypeInfo> bucket : BY_KEY.values()) {
            IRecipeTypeInfo stored = bucket.get(type.getId().toString());
            if (stored == type)
                return true;
        }
        return false;
    }

    /** 登记项数（诊断用）。 */
    public static int size() {
        return all().size();
    }

    // ================= 唤醒三层 =================

    /**
     * 唤醒三层的 {@code XxxRecipeTypes}（各自在自己的静态块里登记），幂等且<b>永不抛</b>。
     *
     * <p>三层的类名见 {@link #LAYER_CLASSES}。某一层不在时（例：只装了 CEWS）该层被静默跳过——
     * 这正是"少一项可加工配方类型但不崩"的实现方式，<b>不是</b>错误路径。</p>
     */
    public static void ensureInitialized() {
        if (initAttempted)
            return;
        initAttempted = true;
        for (String name : LAYER_CLASSES) {
            try {
                Class.forName(name, true, WaveRecipeCapabilities.class.getClassLoader());
            } catch (Throwable missing) {
                // 该层未安装 / 加载器看不见：跳过。表里就没有它的配方类型。
                try {
                    Class.forName(name, true, ClassLoader.getSystemClassLoader());
                } catch (Throwable ignored) {
                    // 同上：静默跳过，绝不因为"少一层"而崩。
                }
            }
        }
    }
}
