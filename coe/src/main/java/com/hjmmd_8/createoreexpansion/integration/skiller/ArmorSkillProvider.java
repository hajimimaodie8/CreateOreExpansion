package com.hjmmd_8.createoreexpansion.integration.skiller;

import java.util.LinkedHashMap;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSkillRuntime;
import com.leaf.skiller.AllSkillInstanceFactories;
import com.leaf.skiller.api.registry.SkillerBuiltInRegistries;
import com.leaf.skiller.foundation.SkillData;
import com.leaf.skiller.foundation.skill.ISkillInstance;

import net.minecraft.nbt.CompoundTag;import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.leaf.skiller.content.skill.SkillComponent;
import com.leaf.skiller.foundation.provider.SkillCollector;
import com.leaf.skiller.foundation.provider.SkillProvider;
import com.leaf.skiller.foundation.skill.SkillBundle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

/**
 * <b>装备（四套盔甲）技能来源</b>：把"玩家当前完整穿着的那一套"的技能交给 Skiller 内核。
 *
 * <h2>它解决什么问题（用户 2026-10-01 提出的紊乱风险）</h2>
 * <p>内核的释放路径是「遍历<b>全部</b>绑定，按槽位号是否按下判定」（见 {@code CoeSkillRelease#release}）。
 * 因此工具技能与装备技能<b>不能共用槽位号</b>：共用时按一次键一，槽位 0 上会同时存在工具与装备
 * 两个 {@link SkillBundle}，两个技能一起放 —— 能量双扣、两个效果同一 tick 生效。</p>
 * <p>本类把装备技能放进<b>另一段槽位空间</b>：工具/物品沿用 {@code 0/1/2}（由
 * {@link CoeSkillProvider} 提供），装备占 {@link #SLOT_BASE} 起的 {@link #SLOT_COUNT} 个槽位。
 * 客户端在按住装备修饰键时才让这段槽位报"按下"（见 {@code CoeSkillClient}），
 * 于是<b>服务端释放路径与内核都不需要任何改动</b>。</p>
 *
 * <h2>与「必须佩戴全套才有效果」的关系</h2>
 * <p>{@link ArmorSet#wornSet(Player)} 就是本类的唯一开关：<b>不成套 ⇒ 一个技能都不贡献、
 * 一个槽位都不声明</b>。那条规则因此不必在 7 个技能里各写一遍（也正是 2026-10-01 先做 G1 的原因）。</p>
 *
 * <h2>本轮状态：雷鸣套槽位 1/2 已落地，槽位 3 待做</h2>
 * <p>表里现有四套的登记：翠玉 2 条、宝石 3 条、星界 3 条、<b>雷鸣 2 条</b>
 * （用户 2026-10-02「雷鸣套装：1、2 通星界」⇒ 槽位 1 = 衡元择势、槽位 2 = 临域充力 II，
 * 两条都<b>复用星界的技能 id</b>，只把基准等级抬到封顶 3，见 {@code ArmorSkillLevels}）。</p>
 * <p><b>雷鸣套的槽位 3（雷鸣威震）故意不登记</b>（作者本轮先欠着）：因此装备段的
 * <b>键三对雷鸣套不响应</b>、HUD 只列两行 —— 这是<b>有意为之，不是漏做</b>。
 * 补它时要一次改三处并保持槽位同序：{@link #SET_SKILL_IDS}、
 * {@code ArmorSkillRuntime#skillId(ArmorSet, int)} 的 THUNDER 分支、
 * {@code ArmorSkillLevels#baseLevelOf} 的 THUNDER 分支（最后那处末尾还要补
 * {@code // === SET-BRANCH-END: THUNDER ===} 哨兵，否则关卡 §21b 的分支提取会假红）。</p>
 *
 * @since 1.0.0
 */
public final class ArmorSkillProvider implements SkillProvider {

    /** 装备技能的槽位号起点（工具/物品占 0/1/2，装备从 3 开始，两段空间不重叠）。 */
    public static final int SLOT_BASE = 3;

    /** Skiller 实例 NBT 里资源与等级所用的键（与 CoeSkillProvider 逐字一致）。 */
    private static final String RESOURCE_KEY = "resource";
    private static final String LEVEL_KEY = "level";

    /** （套 + 等级）→ 已构造组件。只承载按键轮询/HUD 列表，不含每玩家状态，故可全局缓存。 */
    private static final java.util.Map<String, SkillComponent> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** 装备最多 3 个技能（用户 2026-10-01：""装备最多再 3 个技能""）。 */
    public static final int SLOT_COUNT = 3;

    /**
     * 套装 → 该套的技能 id 表（槽位 = 表内下标 + {@link #SLOT_BASE}，每套最多 {@link #SLOT_COUNT} 条）。
     *
     * <p>顺序即槽位顺序：键一 = 下标 0、键二 = 1、键三 = 2。用户 2026-10-01 逐套给的技能表正在
     * 逐个落地，<b>落地一个就往这里加一个</b>（加了之后客户端才开始轮询那个槽位、HUD 才列出来）。</p>
     *
     * <p>⚠ 这张表的<b>顺序</b>必须与 {@code ArmorSkillRuntime#skillId(ArmorSet, int)} 的 switch 逐字同序：
     * 前者决定"客户端轮询/列哪几个槽位"，后者决定"按下那个槽位跑哪个技能"。</p>
     */
    private static final Map<ArmorSet, List<ResourceLocation>> SET_SKILL_IDS = Map.of(
        ArmorSet.JADE_TOPAZ, List.of(ArmorSkillRuntime.FALL_GUARD_ID, ArmorSkillRuntime.CHARGE_DASH_ID),
        // 宝石套（用户 2026-10-01 更正后的编排，共三条）：槽位 1 = 绝境守护、槽位 2 = 蓄能疾骋
        // （**从翠玉套移植**：同一个技能 id、同一套数值，翠玉套那份保持不动）、槽位 3 = 临域充力。
        // ⚠ 顺序即槽位顺序，必须与 ArmorSkillRuntime#skillId(ArmorSet, int) 的 SAPPHIRE_RUBY 分支逐字同序。
        ArmorSet.SAPPHIRE_RUBY, List.of(ArmorSkillRuntime.LAST_STAND_ID, ArmorSkillRuntime.CHARGE_DASH_ID,
            ArmorSkillRuntime.FIELD_CHARGE_ID),
        // 星界套（用户 2026-10-02 星界轮，共三条）：槽位 1 = 衡元择势、槽位 2 = 临域充力
        // （**同一个技能 id 的高等级形态**：星界套的基准是 2、宝石套是 1，数值同一套）、
        // 槽位 3 = 星芒嬗震。
        // ⚠ ASTRAL 已登记在这张表里（2026-10-02 星界轮落地）；下面三行的顺序即槽位序，
        //   必须与 ArmorSkillRuntime#skillId 的 ASTRAL 分支逐字同序（顺序错会**静默取错配置表**）。
        ArmorSet.ASTRAL, List.of(ArmorSkillRuntime.BALANCE_CHOICE_ID, ArmorSkillRuntime.FIELD_CHARGE_ID,
            ArmorSkillRuntime.STAR_SHOCK_ID),
        // 雷鸣套（用户 2026-10-02 雷鸣轮）：「雷鸣套装：1、2 通星界」⇒ 槽位 1 = 衡元择势、
        // 槽位 2 = 临域充力 II（**同一个技能 id 的高等级形态**），只换基准等级：3 / 3
        // （雷鸣套的套基准是 4，而装备技能 3 级封顶 ⇒ "通星界"给到封顶 3，见 ArmorSkillLevels）。
        // ⚠ **只有两条**，而且顺序就是槽位序（槽位 1 = 表内下标 0、槽位 2 = 1），必须与
        //   ArmorSkillRuntime#skillId(ArmorSet, int) 的 THUNDER 分支逐字同序。
        //   **槽位 3（雷鸣威震）故意不登记**（作者本轮先欠着）⇒ 装备段键三对雷鸣套不响应、
        //   HUD 只列两行；这正是本轮的有意状态，不是遗漏。
        ArmorSet.THUNDER, List.of(ArmorSkillRuntime.BALANCE_CHOICE_ID, ArmorSkillRuntime.FIELD_CHARGE_ID));

    /**
     * 该套在装备段暴露的技能 id（<b>按槽位顺序</b>）—— 供显示层共用（护甲 tooltip / HUD）。
     *
     * <p>把这张表暴露出来，而不是让显示层各写一份技能清单：清单只有一处，
     * 将来补"临域充力 / 星芒嬗震 / 雷鸣威震"时，tooltip 与 HUD 自动跟着变。</p>
     *
     * @param set 套装；不在表里时返回空表（不是 null）
     */
    public static List<ResourceLocation> skillIdsOf(ArmorSet set) {
        return SET_SKILL_IDS.getOrDefault(set, List.of());
    }

    /**
     * 由 {@code SkillerIntegration} 在注册期各建一个实例（{@code SkillProviders.register}）。
     * 与 {@link CoeSkillProvider} 一样：本类<b>无状态</b>（表是静态常量），实例本身不承载任何东西。
     */
    public ArmorSkillProvider() {
        // 无状态；构造器公开只为注册
    }

    /** 该槽位号是否属于装备段（工具段是 0..2；两段不许重叠，客户端键源按此分流）。 */
    public static boolean isEquipmentSlot(int slot) {
        return slot >= SLOT_BASE && slot < SLOT_BASE + SLOT_COUNT;
    }

    /** 该槽位号是不是工具/物品段（0..2）。 */
    public static boolean isHeldItemSlot(int slot) {
        return slot >= 0 && slot < SLOT_BASE;
    }

    @Override
    public void collectSkills(SkillCollector collector, Player player) {
        SkillComponent component = componentOf(player);
        if (!component.bindings().isEmpty()) {
            collector.add(component);
        }
    }

    /**
     * 声明本来源用到的槽位号。
     *
     * <p>只在<b>真的有技能</b>时声明（表空 ⇒ 什么都不加），避免客户端为不存在的技能轮询与发包。</p>
     */
    @Override
    public void collectKeys(Set<Integer> keys, Player player) {
        keys.addAll(componentOf(player).bindings().keySet());
    }

    /**
     * 玩家当前<b>生效</b>那一套的技能组件（槽位 = {@link #SLOT_BASE} + 表内下标）。
     *
     * <p>用 {@link ArmorSet#effectiveSet(Player)} 而不是 {@code wornSet}：3 件同套 + 散构聚能
     * 时前者才是"生效的套"（后者为 {@code null}）。</p>
     *
     * <p>每条绑定的等级是 {@link ArmorSkillRuntime#levelOf(Player, String)} —— <b>逐技能</b>
     * 求值（规格 §0.1：每个技能有自己的基准等级，再逐件加减取最大）。等级 ≤ 0 的技能
     * 不产生绑定（不会出现"0 级技能占着槽位"）。</p>
     *
     * @param player 目标玩家；{@code null}、无生效套、或该套尚无技能时返回 {@link SkillComponent#EMPTY}
     */
    public static SkillComponent componentOf(@Nullable Player player) {
        if (player == null) {
            return SkillComponent.EMPTY;
        }
        ArmorSet worn = ArmorSet.effectiveSet(player);
        if (worn == null) {
            return SkillComponent.EMPTY;
        }
        List<ResourceLocation> ids = SET_SKILL_IDS.get(worn);
        if (ids == null || ids.isEmpty()) {
            return SkillComponent.EMPTY;
        }
        // 等级 = 逐技能生效等级（该技能自己的基准 + 技艺提升 − 记忆回溯，钳 1~3），
        // 与 HUD / tooltip 同一入口 —— 客户端看到的等级因此就是实际生效等级。
        List<Integer> levels = new java.util.ArrayList<>(ids.size());
        boolean any = false;
        for (ResourceLocation id : ids) {
            int level = ArmorSkillRuntime.levelOf(player, id.getPath());
            levels.add(level);
            any |= level > 0;
        }
        if (!any) {
            return SkillComponent.EMPTY;
        }
        return cached(worn, levels, ids);
    }

    /**
     * 按（套 + 逐技能等级 + 技能条数）缓存已构造的组件。
     *
     * <p>为什么可以缓存：装备技能的<b>执行</b>在 {@link ArmorSkillRuntime}，这些实例只承担
     * "客户端据此轮询槽位 3/4/5 并发按键包"和"HUD 据此列技能名"两件事，不承载每玩家的状态；
     * 而 {@code componentOf} 会被按键轮询高频调用（每 tick），每次都建实例是纯浪费。</p>
     */
    private static SkillComponent cached(ArmorSet set, List<Integer> levels, List<ResourceLocation> ids) {
        String key = set.name() + "#" + levels + "#" + ids.size();
        SkillComponent hit = CACHE.get(key);
        if (hit != null) {
            return hit;
        }
        SkillComponent built = build(levels, ids);
        CACHE.put(key, built);
        return built;
    }

    /** 真正构造：照 {@code CoeSkillProvider#toInstance} 的口径建实例（资源 = 护甲能量，等级逐技能）。 */
    private static SkillComponent build(List<Integer> levels, List<ResourceLocation> ids) {
        ResourceLocation factoryId = defaultFactoryId();
        if (factoryId == null) {
            return SkillComponent.EMPTY;
        }
        Map<Integer, SkillBundle> bindings = new LinkedHashMap<>();
        for (int index = 0; index < ids.size() && index < SLOT_COUNT; index++) {
            int level = index < levels.size() ? levels.get(index) : 0;
            if (level <= 0) {
                continue;
            }
            CompoundTag nbt = new CompoundTag();
            // 资源 = 护甲能量（本模组四套的储能池）；等级写 "level"（内核的键名，与工具一致）
            nbt.putString(RESOURCE_KEY, CoeArmorEnergyResource.ID.toString());
            nbt.putInt(LEVEL_KEY, level);
            ISkillInstance<?> instance =
                ISkillInstance.fromData(new SkillData(ids.get(index), factoryId, nbt));
            if (instance != null) {
                bindings.put(SLOT_BASE + index, new SkillBundle(List.of(instance)));
            }
        }
        return bindings.isEmpty() ? SkillComponent.EMPTY : new SkillComponent(bindings);
    }

    /** 与 {@code CoeSkillProvider#defaultFactoryId} 同源：内核的默认实例工厂。 */
    private static ResourceLocation defaultFactoryId() {
        return SkillerBuiltInRegistries.SKILL_FACTORIES.getKey(AllSkillInstanceFactories.DEFAULT.getFactory());
    }
}
