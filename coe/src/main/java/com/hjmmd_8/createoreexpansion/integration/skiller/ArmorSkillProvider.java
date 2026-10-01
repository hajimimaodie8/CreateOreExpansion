package com.hjmmd_8.createoreexpansion.integration.skiller;

import java.util.List;
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
 * <h2>本轮状态：表是空的（有意为之，不是漏做）</h2>
 * <p>四套的 7 个套装技能<b>尚未实现</b>（虚衡坠护 / 蓄能疾骋 / 绝境守护 / 临域充力 / 衡元择势 /
 * 临域充力 II / 星芒嬗震 / 雷鸣威震），因此 {@link #SET_SKILL_IDS} 为空 ⇒ {@link #componentOf}
 * 恒返回 {@link SkillComponent#EMPTY}、{@link #collectKeys} 一个槽位都不声明
 * （没有技能时客户端也就不会去轮询 3/4/5、不会发无用的按键包）。</p>
 * <p>但<b>槽位空间、全套门槛、与客户端的接线都已按最终形态落地</b>：第一个套装技能落地时
 * 要做的是两件事 —— ① 把技能 id 填进 {@link #SET_SKILL_IDS}；② 在下面那段
 * {@code TODO} 处补实例构造。第 ② 步被一个<b>尚未裁定的玩法问题</b>挡住：
 * <b>装备技能的能量从哪来</b>（工具/武器/佩的能量是物品自己的 {@code ToolEnergy} 组件；
 * 护甲上是否也挂能量、还是共用背包里某件装备的能量，用户还没定）。因此这里不预先发明口径。</p>
 *
 * @since 1.0.0
 */
public final class ArmorSkillProvider implements SkillProvider {

    /** 装备技能的槽位号起点（工具/物品占 0/1/2，装备从 3 开始，两段空间不重叠）。 */
    public static final int SLOT_BASE = 3;

    /** 装备最多 3 个技能（用户 2026-10-01："装备最多再 3 个技能"）。 */
    public static final int SLOT_COUNT = 3;

    /**
     * 套装 → 该套的技能 id 表（槽位 = 表内下标 + {@link #SLOT_BASE}，每套最多 {@link #SLOT_COUNT} 条）。
     *
     * <p>目前为空。只放<b>数据</b>（id），不放实例：实例化需要"能量资源"口径，那个还没裁定
     * （见类注释最后一节）。</p>
     */
    private static final Map<ArmorSet, List<ResourceLocation>> SET_SKILL_IDS = Map.of();

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
        // 表非空时的实例构造还没写：它被"装备技能的能量从哪来"这个未裁定问题挡住
        // （见类注释最后一节）。照 CoeSkillProvider#toInstance 建实例时，等级取
        // worn.wornLevel(player)（= 该套基准等级 1..4），NBT 写 "level" 与 "resource"。
        // 在那之前刻意<b>不</b>产出实例：资源口径未定的技能一旦被释放，consumeResource
        // 阶段的行为是未定义的。
        return SkillComponent.EMPTY;
    }
}
