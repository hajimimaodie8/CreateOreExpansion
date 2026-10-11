package com.hjmmd_8.createoreexpansion.client.ponder;

import com.hjmmd_8.createoreexpansion.client.ponder.scenes.PowerAngleGrinderInstallScenes;
import com.hjmmd_8.createoreexpansion.client.ponder.scenes.PowerAngleGrinderTierScenes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>动力角磨床两幕思索的登记表</b>（Ponderer JSON {@code power_angle_grinder.json} 的 Java 落点）。
 *
 * <h2>一、三个字符串各自决定什么（这是整条链最容易写歪的一环）</h2>
 * <table border="1">
 *   <caption>命名换算</caption>
 *   <tr><th>字符串</th><th>取值</th><th>决定了</th></tr>
 *   <tr>
 *     <td>component（登记键）</td>
 *     <td>{@code createoreexpansion:power_angle_grinder}</td>
 *     <td>哪一件物品的 tooltip 会出 {@code Hold [W] to Ponder}。
 *         Ponder 的取键处是 {@code PonderTooltipHandler} 对 {@code stack.getItem()} 的注册名，
 *         即<b>物品 id</b>（与方块 id 同名）。本类用 Registrate 的
 *         {@link RegistryEntry#getId()} 取它，避免再抄一遍字符串。</td>
 *   </tr>
 *   <tr>
 *     <td>storyboard path</td>
 *     <td>{@link #INSTALL_AND_RUN} / {@link #WHEEL_TIERS}</td>
 *     <td>① 结构资源路径 = {@code assets/createoreexpansion/ponder/<path>.nbt}
 *         （{@code PonderSceneRegistry#loadSchematic} 拼的是 {@code "ponder/" + path + ".nbt"}）；
 *         ② 场景类里 {@code scene.title(<同一串>, ...)} 的 <b>lang 键段</b>
 *         （{@code "...ponder." + path + ".header"}）。</td>
 *   </tr>
 * </table>
 * <p>⚠ 结构资源路径与 lang 键段是<b>两个独立字符串</b>，只是本模组刻意让它们逐字相同
 * （共用下面两个常量），所以"标题显示原始键名"这种静默故障在这里不可能发生。</p>
 *
 * <h2>二、为什么用 {@code withKeyFunction}</h2>
 * <p>{@code helper} 的泛型是 {@code ResourceLocation}（Ponder 的默认键类型），而我们要登记的
 * 是 Registrate 的 {@code BlockEntry}。Create 官方在所有 {@code addStoryBoard} 调用点都做同一步
 * 换算（{@code AllCreatePonderScenes} 第 72 行：{@code helper.withKeyFunction(RegistryEntry::getId)}），
 * 本类照抄 —— 好处是"物品 id 只在注册处写一次"，改注册名不会让思索静默失联。</p>
 *
 * <h2>三、两幕的 id 与来源</h2>
 * <ul>
 *   <li>{@link #INSTALL_AND_RUN} ← Ponderer JSON 第一幕（{@code scenes[0].id == "scene_1"}，
 *       作者原话：安装动力角磨轮并使之运作）；</li>
 *   <li>{@link #WHEEL_TIERS} ← 第二幕（{@code scenes[1].id == "s_6cb3b"}，不同等级的角磨轮及其功能）。</li>
 * </ul>
 * <p>Ponderer 自动生成的那两个 id（{@code scene_1} / {@code s_6cb3b}）<b>不带模组语义</b>，
 * 这里按作者裁定改成正式名，并且与 scene.title / 结构资源路径三者对齐。</p>
 */
public final class PowerAngleGrinderPonderScenes {

    /**
     * 本模组 Ponder 语言键的<b>前缀</b>（{@code <命名空间>.ponder.}）。
     *
     * <p>⚠ 它<b>必须</b>等于 {@code CoeCore.REGISTRY_NAMESPACE + ".ponder."}（值相同、写死成字面量），
     * 因为：① 语言键是红线——本仓所有 lang 键本来就以字面量写进两份 {@code LanguageProvider}
     * （见 {@code data/lang/{English,Chinese}LangProvider}），这里保持一致；② Ponder 侧拼键的公式在
     * {@code PonderLocalization.langKeyForSpecific}（{@code PonderLocalization.java:64-66}），
     * 前缀只由 {@code getModId()} 决定，不认任何"命名空间常量"。改这一行必须同时改四份 lang 产物，
     * 否则玩家看到的是裸键——关卡 {@code check-module-selfsufficiency.ps1} 的 P4/P5 会红。</p>
     */
    public static final String LANG_PREFIX = "createoreexpansion.ponder.";

    /** 第一幕：底板 → 装入角磨床 → 开盖装轮 → 合盖 → 搭动力 → 投料加工 → 漏斗进出。 */
    public static final String INSTALL_AND_RUN = "power_angle_grinder/install_and_run";

    /** 第二幕：三个等级的角磨轮各自支持什么、各需要多少转速。 */
    public static final String WHEEL_TIERS = "power_angle_grinder/wheel_tiers";

    // ===== lang 键（⚠ 代码里**只许**出现键，人可读文案一律写在两份 LanguageProvider 里）=====
    // Ponder 的两个"文案入口"都**要求**一个 String 参数，但它在运行时**不显示**：
    //   scene.title(id, fallback) → PonderSceneBuilder.java:141-143 只把它登记为
    //       localization 的默认值（registerSpecific(sceneId, "header", fallback)）；
    //   .text(fallback)           → TextWindowElement.java:78-81 → PonderScene.registerText
    //       （PonderScene.java:404-410）同样只登记为 "text_N" 的默认值。
    // 运行时真正显示的是 lang 文件：PonderLocalization.getSpecific（:101-109）在非编辑态走
    // I18n.get(langKeyForSpecific(sceneId, key))，即 createoreexpansion.ponder.<path>.<key>。
    // ⇒ 所以这里传**键本身**：既不把英文句子镶嵌进 Java，又保住了 API 要求的非 null 入参。
    // 唯一会看到"键被当文案显示"的场合是 Ponder 自己的编辑模式
    // （PonderConfig.client().editingMode，默认关）与 Ponder 自己的 lang datagen
    // （provideLang，本模组从不调用它——全仓无调用点）。

    /** 第一幕标题键（= 结构资源路径段 + {@code .header}；也就是 {@code scene.title} 的 fallback 入参）。 */
    public static final String INSTALL_AND_RUN_HEADER = LANG_PREFIX + INSTALL_AND_RUN + ".header";

    // 第一幕 9 条文案键：编号 = 本幕第 N 次 showText 的调用顺序（PonderScene.java:404-410）。
    // ⚠ 顺序由代码执行顺序决定，与 JSON 的 text 步顺序必须一一对应 —— 批 B 里每处 showText
    //   旁边都写了 "text_N ← JSON steps[i]"；JSON scene_1 的 text 步依次是
    //   steps[4] / [7] / [11] / [14] / [19] / [35] / [39] / [53] / [59]。
    /** 第一幕 {@code text_1} 键（← JSON {@code steps[4]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_1 = LANG_PREFIX + INSTALL_AND_RUN + ".text_1";

    /** 第一幕 {@code text_2} 键（← JSON {@code steps[7]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_2 = LANG_PREFIX + INSTALL_AND_RUN + ".text_2";

    /** 第一幕 {@code text_3} 键（← JSON {@code steps[11]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_3 = LANG_PREFIX + INSTALL_AND_RUN + ".text_3";

    /** 第一幕 {@code text_4} 键（← JSON {@code steps[14]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_4 = LANG_PREFIX + INSTALL_AND_RUN + ".text_4";

    /** 第一幕 {@code text_5} 键（← JSON {@code steps[19]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_5 = LANG_PREFIX + INSTALL_AND_RUN + ".text_5";

    /** 第一幕 {@code text_6} 键（← JSON {@code steps[35]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_6 = LANG_PREFIX + INSTALL_AND_RUN + ".text_6";

    /** 第一幕 {@code text_7} 键（← JSON {@code steps[39]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_7 = LANG_PREFIX + INSTALL_AND_RUN + ".text_7";

    /** 第一幕 {@code text_8} 键（← JSON {@code steps[53]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_8 = LANG_PREFIX + INSTALL_AND_RUN + ".text_8";

    /** 第一幕 {@code text_9} 键（← JSON {@code steps[59]}）。 */
    public static final String INSTALL_AND_RUN_TEXT_9 = LANG_PREFIX + INSTALL_AND_RUN + ".text_9";

    /** 第二幕标题键。 */
    public static final String WHEEL_TIERS_HEADER = LANG_PREFIX + WHEEL_TIERS + ".header";

    // 第二幕 5 条文案键：同样按本幕第 N 次 showText 的调用顺序编号。
    // ⚠ JSON s_6cb3b 的 text 步依次是 steps[17] / [19] / [21] / [35] / [46]，
    //   而且这 5 步**全都**带 attachKeyFrame=true（第二幕的 5 个关键帧就是它们）。
    /** 第二幕 {@code text_1} 键（← JSON {@code s_6cb3b steps[17]}，三级总述之一）。 */
    public static final String WHEEL_TIERS_TEXT_1 = LANG_PREFIX + WHEEL_TIERS + ".text_1";

    /** 第二幕 {@code text_2} 键（← JSON {@code s_6cb3b steps[19]}）。 */
    public static final String WHEEL_TIERS_TEXT_2 = LANG_PREFIX + WHEEL_TIERS + ".text_2";

    /** 第二幕 {@code text_3} 键（← JSON {@code s_6cb3b steps[21]}）。 */
    public static final String WHEEL_TIERS_TEXT_3 = LANG_PREFIX + WHEEL_TIERS + ".text_3";

    /** 第二幕 {@code text_4} 键（← JSON {@code s_6cb3b steps[35]}，一级轮演示之后）。 */
    public static final String WHEEL_TIERS_TEXT_4 = LANG_PREFIX + WHEEL_TIERS + ".text_4";

    /** 第二幕 {@code text_5} 键（← JSON {@code s_6cb3b steps[46]}，二级轮演示之后）。 */
    public static final String WHEEL_TIERS_TEXT_5 = LANG_PREFIX + WHEEL_TIERS + ".text_5";

    /**
     * <b>场景索引页</b>的标题键：索引页按 <b>component（物品 id 的 path）</b> 取键，
     * 不是按某一幕的 sceneId，所以它<b>不带斜杠后缀</b>、也没有任何 Java 代码引用它
     * （{@code PonderLocalization.langKeyForSpecific(component, "header")}）。
     * 声明在这里只是为了把"第 5 条键"摆在它的定义处；真正必须存在的地方是两份 LanguageProvider。
     */
    public static final String GRINDER_INDEX_HEADER = LANG_PREFIX + "power_angle_grinder.header";

    private PowerAngleGrinderPonderScenes() {}

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Create 官方换算：ResourceLocation 键 ← Registrate 条目的注册 id
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> registrate = helper.withKeyFunction(RegistryEntry::getId);

        // 原 JSON "items": ["createoreexpansion:power_angle_grinder"] ⇒ 两幕都挂同一件物品
        // （原 JSON "tags": [] 空 ⇒ 不传 tags 变参，与 Ponderer 行为一致：不进索引页）。
        registrate.addStoryBoard(CoeMachines.POWER_ANGLE_GRINDER, INSTALL_AND_RUN,
            PowerAngleGrinderInstallScenes::installAndRun);
        registrate.addStoryBoard(CoeMachines.POWER_ANGLE_GRINDER, WHEEL_TIERS,
            PowerAngleGrinderTierScenes::wheelTiers);
    }
}
