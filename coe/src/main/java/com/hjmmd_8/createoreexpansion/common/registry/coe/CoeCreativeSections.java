package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.AllTags;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * <b>{@code base_tab}（矿物拓展创造页）的「页内分区」规则与排布</b>。
 *
 * <p>把那一页从「146 项平铺」改成「一页 + 三条分区横幅」：<b>① 矿物 → ② 机械 → ③ 装备</b>，
 * 每条横幅占一整行空格子（横幅本身是客户端叠加渲染的，不是物品，见
 * {@code client/creative/CoeCreativeSectionBanners}）。</p>
 *
 * <p><b>权威契约</b>：{@code docs/共享经验-盔甲与材料集/08-创造页分区横幅（矿物扩展落地指南）.md}
 * 的 §1（目标形态）、§5 步骤 2/3（判定与排布）、§8（146 项分类表）。本类是该文档的落地实现。</p>
 *
 * <h3>三条硬约束（渲染端只认它们）</h3>
 * <ol>
 *     <li><b>列表里必须有「整行空格子」</b> —— 渲染端靠它定位横幅，不信任行号算术；</li>
 *     <li><b>横幅不是物品</b> —— 横幅不进 {@code displayItems}，由客户端渲染钩子画在最上层；</li>
 *     <li><b>横幅行 = 该分区首个物品的上一整行</b> —— 所以每个分区之后必须「补满当前行 + 再空一整行」。</li>
 * </ol>
 *
 * <h3>判定优先级必须显式写死：③ 装备 → ② 机械 → ① 矿物</h3>
 * <p>一件物品<b>只进第一个命中的区</b>（与 {@code layout()} 的「每件物品只进一个桶」一致，
 * 也是 §8 的核对口径）。例：{@code jade_stress_medallion} 既像材料又像饰品，因 ③ 优先而落 ③；
 * {@code jade_casing} 因 ② 优先于 ① 而落 ②。</p>
 *
 * <h3>为什么 {@code test} 与 {@code sortKey} 在本类是同一个函数</h3>
 * <p>文档 §8 末尾写明：<b>「{@code test} 与 {@code sortKey} 必须同源，否则同一件物品会被数两次或漏数」</b>。
 * 所以这里不做「每条分区各带一个 {@code Predicate} + 一个 {@code ToIntFunction}」的写法，
 * 而是收敛成唯一入口 {@link #classify}：分区归属与区内排序键一次算出来。分区表
 * {@link #BASE_SECTIONS} 只留渲染需要的三件东西（key / 语言键 / 横幅）。</p>
 *
 * <h3>类初始化纪律</h3>
 * <p>本类的静态字段<b>只有纯字符串与常量</b>，绝不读注册表、绝不取物品实例
 * （文档 §6.3 的「类初始化循环 NPE」）。所有 {@code BuiltInRegistries} 访问都在方法体里，
 * 且只发生在页面构建/渲染时。</p>
 */
public final class CoeCreativeSections {

    /** 每行格子数（原版创造页固定 9 列）。 */
    public static final int ITEMS_PER_ROW = 9;

    /** 本页在 {@link #SECTION_ROWS} 里的前缀（与 {@code CoeCreativeTabs.BASE_TAB} 的 id 一致）。 */
    public static final String BASE_TAB_KEY = "base_tab";

    /** 横幅精灵的命名空间（= 本模组命名空间，资源在 {@code textures/gui/sprites/}）。 */
    private static final String NS = CoeCore.REGISTRY_NAMESPACE;

    /** 三条横幅的语言键前缀（中英各一条，由 datagen 语言提供器产出）。 */
    private static final String LANG_PREFIX = "createoreexpansion.creative_section.";

    /** 兜底桶的 key：三区都不认的物品收在这里，<b>不画横幅</b>（宁可堆在末尾，也不让物品凭空消失）。 */
    public static final String REST_KEY = "__rest";

    /**
     * <b>三个分区</b>（用户 2026-10-01 要求"抽象成一个块、形成固定套路"后由 record 升为枚举）。
     *
     * <p>枚举把「分区」这三件东西收成<b>一个具名常量</b>：`key` / 语言键 / 横幅精灵。注册物品时只需
     * 一行 {@code .transform(section(GEAR))}（见 {@link #section}），其余（分区顺序、行内补白、
     * 横幅行、区内族序）全部由本类自动完成 —— 这就是"加一行就自动归栏"的那一行。</p>
     *
     * <p><b>枚举顺序 = 分区顺序</b>：① 矿物 → ② 机械 → ③ 装备（作者明确要求这个顺序），
     * {@link #BASE_SECTIONS} 由它派生，不再手写第二份表。</p>
     *
     * <p>⚠ `key` 是存档无关但**渲染/记录相关**的字符串（进 {@link #SECTION_ROWS} 的 key、
     * 也是 {@link #byKey} 的入参）⇒ 不许改（改了横幅就不再落位）。语言键与精灵名同理。</p>
     */
    public enum CreativeSection {
        /** ① 矿物：原矿 → 粗矿 → 锭 → 粒 → 板 → 杆 → 线 → 碎片 → 块 → 水晶 → 桶。 */
        ORE("ore"),
        /** ② 机械：处理机器 → 机壳 → 波机器（CEWS）→ 机器构件 → 角磨轮。 */
        MACHINE("machine"),
        /** ③ 装备：工具 → 盔甲 → 弓 → 佩（+ 回旋镖等今后同类）。 */
        GEAR("gear");

        /** 分区 key：{@code ore} / {@code machine} / {@code gear}。 */
        public final String key;

        CreativeSection(String key) {
            this.key = key;
        }

        /**
         * 释词的语言键（横幅上画的那两个字）。
         *
         * <p><b>为什么是方法而不是构造期算好的字段</b>：嵌套类/枚举的初始化<b>不会</b>触发外层类初始化
         * （JLS），而枚举常量可能在 {@code CoeCreativeSections} 尚未初始化时就被引用
         * （例如 {@code CoeItems} 里 {@code section(CreativeSection.GEAR)}）——
         * 那时外层的 {@code LANG_PREFIX} / {@code NS} 还是默认值 {@code null}，
         * 构造期取值会得到 {@code "nullegear"} 这种坏键、以及 NPE 的精灵名。
         * 惰性方法每次现读外层常量，顺序问题消失。</p>
         */
        public String langKey() {
            return LANG_PREFIX + key;
        }

        /** 横幅精灵名（{@code blitSprite} 吃精灵名，不带路径前缀与 {@code .png}）。 */
        public ResourceLocation banner() {
            return sprite("section_" + key);
        }
    }

    /**
     * <b>注册期的显式声明表</b>：物品 → 分区（+ 可选区内族序）。<b>声明优先于规则</b>。
     *
     * <p>为什么要这张表：规则（{@link #classify} 的标签 + 注册名后缀）能让"忘声明"的物品也落对区，
     * 但归属<b>不可见</b>——想确认某件东西在哪一栏，得去读规则。声明表让"哪件东西在哪一栏"
     * 就写在<b>它的注册那一行</b>上（用户 2026-10-01 的诉求）。</p>
     *
     * <p>与 {@code ChargingRecipeTools} / {@code SeriesTraits} 同一手法：注册链上<b>一行登记</b>、
     * 判定处<b>一处消费</b>（唯一取值点），因此不需要数据组件、不影响存档格式。</p>
     *
     * <p>并发：注册是并行的，用 {@link ConcurrentHashMap}；同 id 重复登记时后者覆盖（不报错，
     * 因为 in-place 重登记无害）。</p>
     */
    private static final Map<Item, CreativeSection> DECLARED = new ConcurrentHashMap<>();

    /** 显式声明时若规则给不出族序，落在这个组（仍在该分区内，排在规则能判定的物品之后）。 */
    private static final int DECLARED_FALLBACK_ORDER = 50;

    /**
     * <b>把物品登记进指定分区</b>（注册期由 {@link #section} 调用，也可以手写调用）。
     *
     * @param item    已注册的物品（Registrate 的 {@code onRegister} 给的就是它）
     * @param section 目标分区
     */
    public static void declare(Item item, CreativeSection section) {
        if (item != null && section != null) {
            DECLARED.put(item, section);
        }
    }

    /**
     * 该物品是否被<b>显式声明</b>过分区（只读；给探针与工具统计"还有多少件靠规则"用）。
     *
     * @return 声明的分区；没声明过返回 {@code null}（此时归规则判定）
     */
    public static @Nullable CreativeSection declaredOf(@Nullable Item item) {
        return item == null ? null : DECLARED.get(item);
    }

    /** 已显式声明的物品件数（迁移进度指标：靠规则的件数 = 本页总数 − 它）。 */
    public static int declaredCount() {
        return DECLARED.size();
    }

    /**
     * <b>链上一行：把物品登记进指定分区</b>（用户要的那个"块"）。
     *
     * <p>用法（与仓库已有的 {@code .transform(skillItem())}、{@code .addEnergy()} 同一套路数）：</p>
     * <pre>{@code
     * public static final ItemEntry<Item> JADE_INGOT = CoeRegistrate.REGISTRATE
     *         .item("jade_ingot", Item::new)
     *         .transform(section(CreativeSection.ORE))   // ← 就这一行
     *         .model(...)
     *         .register();
     * }</pre>
     *
     * <p>它<b>不改动</b>物品本身（不加组件、不动 NBT），只在注册完成时把"这件东西归哪一栏"写进
     * {@link #DECLARED} —— 因此对存档、网络、datagen 全部零影响。</p>
     *
     * <p>仍然保留规则兜底：<b>没声明</b>的物品照旧由 {@link #classify} 判定，不会掉进兜底桶。
     * 新物品的正确姿势是<b>声明</b>（可见、可查、不依赖后缀巧合）。</p>
     */
    public static <T extends Item, P> NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> section(
            CreativeSection section) {
        return builder -> builder.onRegister(item -> declare(item, section));
    }

    /**
     * <b>一个分区</b>：进 {@link #SECTION_ROWS} 的 key + 释词语言键 + 横幅精灵。
     *
     * @param key     分区 key（{@code ore} / {@code machine} / {@code gear}）
     * @param langKey 释词的语言键（横幅上画的那两个字）
     * @param banner  横幅精灵名（{@code blitSprite} 吃的是精灵名，<b>不带</b> {@code textures/gui/sprites/} 前缀与 {@code .png}）
     * @deprecated 已被 {@link CreativeSection} 枚举取代（枚举同时携带三者）；保留仅为兼容既有调用点，
     *     {@link #BASE_SECTIONS} 现在由枚举派生。
     */
    @Deprecated
    public record Section(String key, String langKey, ResourceLocation banner) {}

    /**
     * <b>分区顺序 = {@link CreativeSection} 的声明顺序</b>：① 矿物 → ② 机械 → ③ 装备（作者明确要求这个顺序）。
     *
     * <p>三个横幅精灵名与三张 162×18 贴图<b>逐字对应</b>：{@code section_ore} / {@code section_machine}
     * / {@code section_gear}（作者提供的素材，落在 {@code coe/src/main/resources/assets/.../textures/gui/sprites/}）。</p>
     */
    public static final List<Section> BASE_SECTIONS = Arrays.stream(CreativeSection.values())
        .map(s -> new Section(s.key, s.langKey(), s.banner()))
        .toList();

    /**
     * <b>每个分区的「首个物品」所在行（0 基）</b>，key = {@code "<页id>|<分区key>"}。
     *
     * <p>由 {@link #layout} 在构建期填写，客户端渲染钩子每帧读取。
     * 它记的是<b>首物品行</b>；横幅行 = 它 − 1（渲染端不直接减，而是用「整行空」自适应判定，
     * 见 {@link #layout} 的注释与客户端渲染器）。</p>
     *
     * <p>只在逻辑客户端有意义（服务端不建创造页内容）；服务端探针读它时得到的是空表，
     * 这是正常的——探针要读的是 {@code getDisplayItems()} 本身。</p>
     */
    public static final Map<String, Integer> SECTION_ROWS = new LinkedHashMap<>();

    /** 一个待排布的桶：{@code sectionKey} 为 {@link #REST_KEY} 时表示「未分区兜底」。 */
    public record Bucket(String key, List<ItemStack> items) {}

    /** 按 key 取分区声明（渲染端用）；未知 key（含 {@link #REST_KEY}）返回 {@code null}。 */
    @Nullable
    public static Section byKey(String key) {
        for (Section section : BASE_SECTIONS) {
            if (section.key().equals(key)) {
                return section;
            }
        }
        return null;
    }

    private static ResourceLocation sprite(String path) {
        return ResourceLocation.fromNamespaceAndPath(NS, path);
    }

    // -----------------------------------------------------------------------
    // 判定：唯一入口 classify(...)
    // -----------------------------------------------------------------------

    /** 判定结果：分区 key + 区内排序键。{@code section == null} 表示三区都不认。 */
    private record Classification(@Nullable String section, int order) {}

    /** ① 矿物的区内族序（§8 ① 的分组顺序）。 */
    private static final int ORE_ORE = 0;
    private static final int ORE_RAW = 1;
    private static final int ORE_INGOT = 2;
    private static final int ORE_NUGGET = 3;
    private static final int ORE_SHEET = 4;
    private static final int ORE_ROD = 5;
    private static final int ORE_WIRE = 6;
    private static final int ORE_SHARD = 7;
    private static final int ORE_BLOCK = 8;
    private static final int ORE_CRYSTAL = 9;
    private static final int ORE_BUCKET = 10;

    /**
     * ② 机械的区内族序（§8 ② 的分组顺序；2026-09-30 两处调整）。
     *
     * <p><b>2026-09-30 用户裁定</b>：① CEWS 的 12 台波机器搬入本区；② <b>10 个角磨轮排到本区最后</b>
     * （"这些都是比较无关紧要的东西"）⇒ 顺序 = 处理机器 → 机壳 → <b>波机器</b> → 机器构件 → <b>角磨轮</b>。</p>
     */
    private static final int MACHINE_MACHINE = 0;
    private static final int MACHINE_CASING = 1;
    /** CEWS 的波机器家族（2026-09-30 用户裁定搬入本页）。 */
    private static final int MACHINE_WAVE = 2;
    private static final int MACHINE_MECHANISM = 3;
    /** 角磨轮：**本区最后**（用户 2026-09-30 裁定「比较无关紧要」）。 */
    private static final int MACHINE_WHEEL = 4;

    /**
     * <b>CEWS 波机器的注册名后缀族</b>（2026-09-30 用户裁定「标签页只有一个」后，
     * 该层 12 台机器由 {@code CewsRegistrate} 直接进本页，全部落「机械」分区）。
     *
     * <p>用后缀族而不是逐个点名：这些是<b>成族出现</b>的机器（三种材质的调级器 / 调节器、
     * 差波器家族…），以后加新材质或新家族成员时会自动跟上 —— 这也是不会退化成
     * "枚举物品"的关键（落地文档 §5 步骤 2 的硬要求）。</p>
     *
     * <p>清单与本页实测对应关系（12 件）：
     * {@code energy_field_controller} / {@code stellar_wave_transmuter} /
     * {@code energy_wave_regulator} · {@code sapphire_wave_regulator} · {@code stellarstone_wave_regulator} /
     * {@code wave_speed_regulator} · {@code sapphire_speed_regulator} · {@code stellarstone_speed_regulator} /
     * {@code energy_wave_disperser} · {@code six_face_disperser} /
     * {@code octa_energy_wave_differencer} / {@code wave_query_gauge}。</p>
     */
    private static final String[] WAVE_MACHINE_SUFFIXES = {
        "_field_controller",    // 能量场控制器
        "_wave_transmuter",     // 星辉波变器
        "_wave_regulator",      // 能量调级器（三种材质）
        "_speed_regulator",     // 波速调节器（三种材质）
        "_disperser",           // 波差器家族（四面 / 六面）
        "_differencer",         // 波差器家族（八面）
        "_query_gauge"          // 波情查询仪（用户裁定：也放机械分区）
    };

    /** ③ 装备的区内族序（§8 ③ 的分组顺序）。 */
    private static final int GEAR_TOOL = 0;
    private static final int GEAR_ARMOR = 1;
    private static final int GEAR_BOW = 2;
    private static final int GEAR_MEDALLION = 3;

    /** 未分区物品的排序键（只影响兜底桶内部顺序，通常为空桶）。 */
    private static final int UNCLASSIFIED = Integer.MAX_VALUE;

    /**
     * <b>分区与区内排序的唯一判定入口</b>。
     *
     * <p><b>判定优先级</b>：</p>
     * <ol>
     *     <li><b>注册期显式声明</b>（{@link #DECLARED}，由 {@link #section} 那一行写入）——
     *         用户 2026-10-01 要求的"加一行就自动归栏"，也是<b>推荐姿势</b>；
     *         区内族序仍由下面那套规则给出（保持分区内部排列不变），规则给不出就用
     *         {@link #DECLARED_FALLBACK_ORDER}，排在本区规则能判定的物品之后；</li>
     *     <li><b>规则兜底</b>：③ 装备 → ② 机械 → ① 矿物，「标签优先、注册名后缀兜底」，
     *         <b>不枚举物品</b>（金属/宝石家族只会越来越多，枚举是死路）。</li>
     * </ol>
     *
     * <p>两者并存是刻意的：声明让归属<b>可见</b>，规则让"忘声明"的物品<b>也不会掉进兜底桶</b>
     * （宁可落对区，也不让物品凭空消失）。</p>
     */
    private static Classification classify(ItemStack stack) {
        String path = pathOf(stack);
        // 规则先算一遍：即使走了显式声明，也需要它给出的区内族序（保持区内排列）
        Classification byRule = classifyByRule(stack, path);
        CreativeSection declared = DECLARED.get(stack.getItem());
        if (declared != null) {
            return new Classification(declared.key,
                byRule.section() != null ? byRule.order() : DECLARED_FALLBACK_ORDER);
        }
        return byRule;
    }

    /** 规则判定（{@link #classify} 的第二优先级；拆出来是为了"声明优先但不丢族序"）。 */
    private static Classification classifyByRule(ItemStack stack, String path) {
        // ---------------- ③ 装备（最高优先级） ----------------
        // 器具：用原版物品标签判（比按名字后缀可靠，§8 分族核对表）
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.PICKAXES)
            || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) {
            return new Classification("gear", GEAR_TOOL);
        }
        // 盔甲 16 件：同样走原版标签
        if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
            || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) {
            return new Classification("gear", GEAR_ARMOR);
        }
        // 翠玉之弓：点名（§8 ③ 列的就是「点名（或它自己的类）」）
        if ("jade_topaz_bow".equals(path)) {
            return new Classification("gear", GEAR_BOW);
        }
        // 凝能佩 6 件：⚠ 它们同时带 Create 的 SKILL_TOOLS 标签，所以「装备」绝不能只靠 SKILL_TOOLS 判
        if (path.endsWith("_stress_medallion")) {
            return new Classification("gear", GEAR_MEDALLION);
        }

        // ---------------- ② 机械 ----------------
        // 处理机器 2 件点名 + 三台应力充能器
        if ("power_angle_grinder".equals(path) || "reinforced_lightning_rod".equals(path)
            || path.endsWith("_stress_charger")) {
            return new Classification("machine", MACHINE_MACHINE);
        }
        // 机壳 3 件（物品由 Create 的 casing 构建器内部产生，见 §4.4 要点 1）
        if (path.endsWith("_casing")) {
            return new Classification("machine", MACHINE_CASING);
        }
        // 角磨轮 10 个：优先用标签（一次拿全），后缀兜底。
        // ⚠ 用的是本模组自己的 AllTags（标签 createoreexpansion:grinding_wheels）——
        //   Create 的 com.simibubi.create.AllTags 里<b>没有</b> GRINDING_WHEELS 这个常量
        //   （本轮用 javap 数过它 48 个常量，确认无此项）。
        if (stack.is(AllTags.AllItemTags.GRINDING_WHEELS.tag)
            || path.endsWith("_grinding_wheel")) {
            return new Classification("machine", MACHINE_WHEEL);
        }
        // 机器构件 4 件：transmute_mechanism / incomplete_transmute_mechanism /
        // energy_mechanism / incomplete_energy_mechanism（后两件的注册在 CEWS 层，
        // 2026-09-30 起该层默认页就是本页，所以它们本来就是本页成员）
        if (path.endsWith("_mechanism")) {
            return new Classification("machine", MACHINE_MECHANISM);
        }
        // CEWS 的波机器 12 件（用户裁定搬入本页）：按后缀族识别，见 WAVE_MACHINE_SUFFIXES
        for (String waveSuffix : WAVE_MACHINE_SUFFIXES) {
            if (path.endsWith(waveSuffix)) {
                return new Classification("machine", MACHINE_WAVE);
            }
        }

        // ---------------- ① 矿物 ----------------
        // ⚠ 顺序敏感：crushed_*_ore 同时以 _ore 结尾，必须先判「粉碎」；
        //   *_budding_block 同时以 _block 结尾，必须先判「芽床/水晶簇」。
        if (path.startsWith("crushed_")) {
            return new Classification("ore", ORE_RAW);
        }
        if (path.endsWith("_ore")) {
            return new Classification("ore", ORE_ORE);
        }
        if (path.endsWith("_budding_block") || path.endsWith("_cluster")) {
            return new Classification("ore", ORE_CRYSTAL);
        }
        if (path.endsWith("_block")) {
            // 矿物块 11（含 raw_*_block；它们先于 raw_ 前缀被认走，正是 §8 的分组）
            return new Classification("ore", ORE_BLOCK);
        }
        if (path.startsWith("raw_")) {
            return new Classification("ore", ORE_RAW);
        }
        if (path.endsWith("_ingot")) {
            return new Classification("ore", ORE_INGOT);
        }
        if (path.endsWith("_nugget")) {
            return new Classification("ore", ORE_NUGGET);
        }
        if (path.endsWith("_sheet")) {
            return new Classification("ore", ORE_SHEET);
        }
        if (path.endsWith("_rod")) {
            return new Classification("ore", ORE_ROD);
        }
        if (path.endsWith("_wire")) {
            return new Classification("ore", ORE_WIRE);
        }
        if (path.endsWith("_small_shard") || path.endsWith("_big_shard")) {
            return new Classification("ore", ORE_SHARD);
        }
        if (path.endsWith("_bucket")) {
            return new Classification("ore", ORE_BUCKET);
        }
        // 无家族标签、也无可辨后缀的两件杂项原料（§8 分族核对表：逐个点名）
        if ("thunderite_scrap".equals(path) || "lucky_dust".equals(path)) {
            return new Classification("ore", ORE_RAW);
        }
        return new Classification(null, UNCLASSIFIED);
    }

    /** 该物品属于哪个分区；{@code null} = 三区都不认（会进兜底桶）。 */
    @Nullable
    public static String sectionOf(ItemStack stack) {
        return classify(stack).section();
    }

    /** 区内排序键（小者在前）；未分区 = {@link #UNCLASSIFIED}。 */
    public static int orderOf(ItemStack stack) {
        return classify(stack).order();
    }

    /** 物品的注册名路径；未注册（理论上不会）时返回空串，判定自然落到兜底。 */
    private static String pathOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.getPath();
    }

    // -----------------------------------------------------------------------
    // 分桶与排布
    // -----------------------------------------------------------------------

    /**
     * 把「游戏真实构建出来的」本页物品按 {@link #BASE_SECTIONS} 的<b>顺序</b>切段。
     *
     * <p><b>纯函数、不读 {@link #SECTION_ROWS}</b>（那是 {@link #layout} 的事）：
     * 这个方法可能被渲染路径反复调用。</p>
     *
     * <p>两条纪律：</p>
     * <ol>
     *     <li><b>空桶跳过</b>：某分区 0 项时既不给它记行号、也不给它留空行
     *         （否则会画出「横幅下面直接是下一条横幅」）。</li>
     *     <li><b>兜底尾桶</b>：三区都没命中的物品收进 {@link #REST_KEY} 桶 —— 它会被排到最后、
     *         <b>不画横幅</b>，但物品一件都不会少（文档：「一件物品凭空消失是最难查的 bug」）。</li>
     * </ol>
     *
     * <p>区内用<b>稳定排序</b>按 {@link #orderOf} 归族，所以同族内部保持注册顺序
     * （翡翠 → 黄玉 → 蓝宝石 → …… 的既有观感不变）。</p>
     */
    public static List<Bucket> bucketize(Collection<ItemStack> real) {
        Map<String, List<ItemStack>> bySection = new LinkedHashMap<>();
        for (Section section : BASE_SECTIONS) {
            bySection.put(section.key(), new ArrayList<>());
        }
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack stack : real) {
            String key = sectionOf(stack);
            List<ItemStack> target = key == null ? rest : bySection.get(key);
            if (target == null) {
                target = rest;
            }
            target.add(stack);
        }

        List<Bucket> buckets = new ArrayList<>();
        for (Section section : BASE_SECTIONS) {
            List<ItemStack> items = bySection.get(section.key());
            if (items.isEmpty()) {
                continue;
            }
            items.sort(Comparator.comparingInt(CoeCreativeSections::orderOf));
            buckets.add(new Bucket(section.key(), List.copyOf(items)));
        }
        if (!rest.isEmpty()) {
            buckets.add(new Bucket(REST_KEY, List.copyOf(rest)));
        }
        return buckets;
    }

    /**
     * <b>排布</b>：把分区桶摊平成「补了空行的物品列表」，并记下每个分区的首物品行号。
     *
     * <p>三个不能改的地方（文档 §5 步骤 3）：</p>
     * <ol>
     *     <li><b>开头补一整行空</b> —— 首个分区的横幅就画在第 0 行；</li>
     *     <li><b>行号在加物品之前取</b> —— {@code out.size() / 9} 才是首物品行；</li>
     *     <li><b>补白一定 ≥ 9</b> = {@code (9 - n % 9) % 9 + 9}：补满本行 + 再空一整行；
     *         最后一区不补（否则页尾会拖一整行空的）。</li>
     * </ol>
     *
     * <p><b>兜底桶不记行号</b>（{@link #byKey} 认不出它的 key）：它只是把物品堆在末尾，
     * 不会被画上横幅。</p>
     *
     * <p>副作用：重写 {@link #SECTION_ROWS} 中属于本页的条目。原版会在切页/搜索时重建内容，
     * 所以每次都要先清掉自己的旧记录（别的页的记录不动）。</p>
     */
    public static List<ItemStack> layout(String tabKey, List<Bucket> buckets) {
        List<ItemStack> out = new ArrayList<>();
        SECTION_ROWS.keySet().removeIf(key -> key.startsWith(tabKey + "|"));

        for (int i = 0; i < ITEMS_PER_ROW; i++) {
            out.add(ItemStack.EMPTY);
        }

        for (int b = 0; b < buckets.size(); b++) {
            Bucket bucket = buckets.get(b);
            if (byKey(bucket.key()) != null) {
                SECTION_ROWS.put(tabKey + "|" + bucket.key(), out.size() / ITEMS_PER_ROW);
            }
            out.addAll(bucket.items());
            if (b == buckets.size() - 1) {
                break;
            }
            int used = bucket.items().size() % ITEMS_PER_ROW;
            int pad = (ITEMS_PER_ROW - used) % ITEMS_PER_ROW + ITEMS_PER_ROW;
            for (int i = 0; i < pad; i++) {
                out.add(ItemStack.EMPTY);
            }
        }
        return out;
    }

    private CoeCreativeSections() {}
}
