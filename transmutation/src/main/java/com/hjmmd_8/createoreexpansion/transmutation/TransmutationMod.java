package com.hjmmd_8.createoreexpansion.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * <b>TRANS（Create: Mechanical Transmutation，机械嬗化学）的 {@code @Mod} 入口 —— 现在是一个空壳</b>
 * （W6-b2：按用户裁定，嬗化的<b>全部内容</b>整块搬进了第一层 {@code :coe}）。
 *
 * <h2>一、为什么这个类还在这里（而不是跟着内容一起搬走或删掉）</h2>
 * <p>用户 2026-09-27 的裁定是「第三层什么都<b>不要留</b>，但第三层这个<b>模组</b>要保留注册」：
 * 嬗化的 16 个 Java 文件 + 24 个资源文件全部进 {@code :coe}（包名逐字不变），
 * 而 {@code transmutation} 这个 <b>mod id 继续存在</b>——它是"以后往这里加新配方"的容器。</p>
 *
 * <p>FML 对 {@code [[mods]]} 与 {@code @Mod} 是<b>双向硬约束</b>（见 {@code common.hub.IntegrationMod}
 * 类注释"一"）：</p>
 * <ul>
 *   <li><b>有条目、没类</b> → 这个 mod 出现在 mod 列表里却永不构造 = 入口静默消失，
 *       日志里的 {@code [TRANS] mod 初始化完成} 一行随之消失（没有任何报错）；</li>
 *   <li><b>有类、没条目</b> → {@code dangling_entrypoint} 硬错。</li>
 * </ul>
 * <p>所以只要 {@code transmutation/src/main/templates/META-INF/neoforge.mods.toml} 还声明
 * {@code [[mods]] modId = "transmutation"}，这个类就必须在<b>这个 jar 里</b>存在，
 * 且包名必须与 {@code [[mods]]} 所在文件一致（{@code @Mod} 的类必须住在它自己那个 mod 文件里）。</p>
 *
 * <h2>二、构造器为什么只剩一行日志</h2>
 * <p>原先住在这里的六处注册触发<b>全部搬回 {@code CreateOreExpansion} 构造器</b>（它们现在都是
 * 第一层自己的东西了）：</p>
 * <ul>
 *   <li>{@code TransmutationFluids.register()}（嬗变液，仍挂 {@code CoeRegistrate.REGISTRATE}）；</li>
 *   <li>{@code TransmutationEffects.register(modEventBus)}（嬗乱效果）；</li>
 *   <li>{@code AllModPotions.register(modEventBus)} + {@code AllModPotions::registerBrewingRecipes}；</li>
 *   <li>{@code AllFanProcessingTypes.init()}（挂在 mod 总线的 {@code RegisterEvent} 上）；</li>
 *   <li>{@code TransmutationRegistrate.REGISTRATE.registerEventListeners(modEventBus)}；</li>
 *   <li>{@code TransmutationItems.register()}（嬗化构件两个物品）。</li>
 * </ul>
 * <p><b>为什么必须搬</b>：这些类现在住在 {@code :coe}，而 {@code :transmutation} 的
 * {@code build.gradle} 只 {@code compileOnly(project(':coe'))}（不 jarJar、不 implementation）。
 * 只装 {@code coe.jar} 时这个构造器根本不在场，若不搬，嬗变液 / 嬗乱 / 药水 / 风扇加工类型
 * 一个都不会注册（W6-b 的实测结论，见 {@code build/patch/w6b-EVIDENCE.txt} §5.1）。
 * <b>并且不许两处都留</b>：同一个 {@code DeferredRegister} 挂两次总线会在 {@code RegisterEvent}
 * 上重复注册。</p>
 *
 * <h2>三、为什么空壳仍然要调 {@code LayerBootstrap.ensureAttached}</h2>
 * <p>红线口径「每个 {@code @Mod} 构造器的第一条语句」在这里<b>继续有效</b>，三条理由：</p>
 * <ol>
 *   <li><b>幂等</b>：{@code ensureAttached} 用一把静态锁把"检查 + 落地"做成临界区
 *       （{@code LayerBootstrap} 的 {@code LOCK} / {@code attached}），
 *       第二个调用者直接返回——所以壳里这一句与 {@code CreateOreExpansion} 里那一句
 *       不会重复挂注册表、不会重复注册载荷。</li>
 *   <li><b>顺序语义与调用方无关</b>：配方类型的注册顺序由 {@code wakeLayers()} 自己的
 *       固定名单（{@code LAYER_RECIPE_TYPE_CLASSES}）决定，而不是由"谁先调"决定。
 *       所以空壳先构造还是后构造，{@code BuiltInRegistries.RECIPE_TYPE} 的条目顺序逐字相同。</li>
 *   <li><b>FML 的 mod 构造是并行派发的</b>（{@code dispatchParallelTask}），
 *       "谁先到"不可预测；把共享接线的唯一入口留在每个 {@code @Mod} 里，
 *       等价于"谁在场谁负责"，与 P7a 定下的控制反转口径一致。
 *       同时 {@code tools/check-module-selfsufficiency.ps1} 的 <b>D1</b>
 *       （每个模块源码必须出现 {@code LayerBootstrap.ensureAttached(}）也要求它。</li>
 * </ol>
 * <p>日志那一行也不是装饰：它是"这个 {@code @Mod} 入口真的被构造了"的<b>唯一可观测证据</b>
 * （判据见 {@code w6b2-EVIDENCE.txt} 的闸 2），所以刻意保留。</p>
 */
@Mod(TransmutationMod.MOD_ID)
public final class TransmutationMod {

    /** TRANS 模块的 mod id（<b>不是</b>注册命名空间——命名空间恒为 {@code createoreexpansion}）。 */
    public static final String MOD_ID = "transmutation";

    public TransmutationMod(IEventBus modEventBus, ModContainer modContainer) {
        // ── P7a：共享接线（必须是构造器的第一条语句，见 LayerBootstrap 类注释"四"）───────────
        // 本类已空壳化，但这一句按类注释"三"的三条理由保留。
        LayerBootstrap.ensureAttached(modEventBus);

        // 本层不再注册任何东西：嬗化的 16 个 Java 文件与 24 个资源文件都已搬进 :coe，
        // 六处注册触发出现在 CreateOreExpansion 构造器里（见类注释"二"）。
        CoeCore.LOGGER.info("[TRANS] mod 初始化完成（mod id={}，注册命名空间={}）：本模块为空壳，"
                + "嬗化内容（流体/效果/药水/风扇加工/嬗化构件）已随 W6-b2 归入第一层",
            MOD_ID, CoeCore.REGISTRY_NAMESPACE);
    }
}
