package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.leaf.skiller.AllSkillInstanceFactories;
import com.leaf.skiller.api.registry.SkillerBuiltInRegistries;
import com.leaf.skiller.content.skill.SkillComponent;
import com.leaf.skiller.foundation.SkillData;
import com.leaf.skiller.foundation.provider.SkillCollector;
import com.leaf.skiller.foundation.provider.SkillProvider;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkillRegistration;
import com.leaf.skiller.foundation.skill.SkillBundle;
import com.leaf.skiller.server.PlayerPressedKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.hjmmd_8.createoreexpansion.integration.skiller.resource.CoeToolEnergyResource;

/**
 * 旧技能组件 → Skiller 技能组件的桥（{@link SkillProvider} 实现）。
 *
 * <h2>为什么是"桥"而不是"搬家"</h2>
 * <p>本模组的技能数据（{@code createoreexpansion:skills} 组件、NBT 里的
 * {@code Level}/{@code Config}/{@code OutlineColor}）<b>一字不改</b>：换核换的是
 * "谁来解释这些数据"，不是数据本身。因此老存档天然兼容，不需要 DataFixer，
 * 物品注册代码也不用动。</p>
 *
 * <h2>转换口径</h2>
 * <ul>
 *     <li><b>槽位 = 该技能在自己 {@link SkillType} 里的下标</b>（旧语义：键一→槽位 0、
 *         键二→1、键三→2，见旧 {@code SkillsComponent#releaseSkillAt}）。三个类型里
 *         下标相同的技能会被放进<b>同一个</b> {@link SkillBundle}（Skiller 的按键槽位模型
 *         就是"一个键 → 一个包含多类型技能的包"）。</li>
 *     <li><b>只认主手物品</b>：与旧系统四个触发点一致（挖掘/受击/右键/弓都用主手工具）。</li>
 *     <li><b>未迁移的技能直接跳过</b>：{@code skiller:skill} 注册表里查不到这个 id，
 *         就说明它还在旧框架手里 → 返回 null，不产出实例。这条是"增量并行迁移"
 *         的基础，也保证同一个技能不会被新旧两条路径各执行一次。</li>
 *     <li>实例的 NBT 是旧 NBT 的拷贝，另按 Skiller 的约定写入
 *         {@code "resource"}（= {@link CoeToolEnergyResource#ID}）与 {@code "level"}
 *         （取旧 NBT 的 {@code Level}），旧键一个不删——技能实现仍从 {@code Config}
 *         子标签读配置。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CoeSkillProvider implements SkillProvider {

    /** 旧 NBT 里技能等级所用的键（历史格式，不可改） */
    private static final String LEGACY_LEVEL_KEY = "Level";

    /** Skiller 的默认工厂（{@code DefaultSkillFactory}）写/读资源与等级所用的键 */
    private static final String RESOURCE_KEY = "resource";
    private static final String LEVEL_KEY = "level";

    /** 旧类型 → 新类型的映射（保持一一对应；新增类型时这里要同步） */
    private static final Map<SkillType, com.leaf.skiller.foundation.skill.SkillType> TYPE_MAPPING = buildTypeMapping();

    /**
     * 单条目转换结果缓存：键是旧组件的<b>同一个实例</b>。
     *
     * <p>为什么需要：{@link #componentOf} 在"每次造成伤害/每次破坏方块"都会被调一次
     * （见 {@code CoeSkillRelease}），而转换过程要建实例对象。物品不变时
     * {@code ItemStack#get(组件)} 返回的就是同一个对象，所以按住同一样东西连续战斗/挖掘
     * 时这一层缓存把开销压到一次引用比较。</p>
     *
     * <p>只用单条目（不是 Map）：玩家手上真正会变的只有主手这一件，多条目缓存只会留下
     * 一堆再也不会命中的条目。</p>
     */
    private static SkillsComponent cachedLegacy;
    private static SkillComponent cachedResult = SkillComponent.EMPTY;

    private static Map<SkillType, com.leaf.skiller.foundation.skill.SkillType> buildTypeMapping() {
        Map<SkillType, com.leaf.skiller.foundation.skill.SkillType> map = new LinkedHashMap<>();
        map.put(SkillType.EXCAVATION_SKILL, CoeSkillTypes.EXCAVATION);
        map.put(SkillType.HIT_SKILL, CoeSkillTypes.HIT);
        map.put(SkillType.USE_SKILL, CoeSkillTypes.USE);
        return Map.copyOf(map);
    }

    @Override
    public void collectSkills(SkillCollector collector, Player player) {
        collector.add(componentOf(player));
    }

    @Override
    public void collectKeys(Set<Integer> keys, Player player) {
        keys.addAll(componentOf(player).bindings().keySet());
    }

    /**
     * 把玩家主手物品上的旧技能组件转换成 Skiller 的技能组件。
     *
     * @param player 目标玩家；{@code null} 或无技能时返回 {@link SkillComponent#EMPTY}
     * @return 按键槽位 → 技能包；没有任何已迁移技能时返回空组件
     */
    public static SkillComponent componentOf(@Nullable Player player) {
        if (player == null) {
            return SkillComponent.EMPTY;
        }
        SkillsComponent legacy = SkillItemStack.of(player.getMainHandItem()).getSkillsHolder();
        if (legacy == null) {
            cachedLegacy = null;
            cachedResult = SkillComponent.EMPTY;
            return SkillComponent.EMPTY;
        }
        // 同一件物品（同一个组件实例）→ 直接复用上次的转换结果
        if (legacy == cachedLegacy) {
            return cachedResult;
        }
        SkillComponent converted = convert(legacy);
        cachedLegacy = legacy;
        cachedResult = converted;
        return converted;
    }

    /**
     * <b>服务端权威：该玩家主手物品此刻按着的技能槽位</b>（2026-10-05 弓技能批 8 ①）。
     *
     * <p>它回答的是"这一次触发有没有技能意图"，走的是与释放路径 {@code CoeSkillRelease#release}
     * <b>同一份</b>绑定表（{@link #componentOf}）与<b>同一个</b>键位来源（{@link PlayerPressedKeys}
     * —— 由客户端的按键包写入，<b>专用服务器上同样为真</b>）。</p>
     *
     * <h2>为什么必须有这一处</h2>
     * <p>{@code content/skill/input/AllKeys} 是<b>纯客户端</b>对象（服务端没有 keybind，
     * {@code isPressed()} 恒 false）⇒ 专用服务器上"这次拉弓按了技能键没有"这个问题，
     * 客户端读数只有一个答案"没有"⇒ 弓类技能（含三条专属）在专用服务器上<b>永远不释放</b>
     * （单人 / 局域网主机看不出问题：客户端按键状态在同一个进程里可读）。</p>
     *
     * <p>弓侧因此把预检的<b>服务端那一半</b>交给本方法（{@code JadeTopazBowItem} 的两处读数），
     * 客户端那一半仍是 {@code JadeTopazBowItem#detectSkillSlot()}（纯客户端读数，只用于客户端）。</p>
     *
     * <p>多个槽位同时按下时返回<b>绑定表顺序里的第一个</b>（= 槽位号最小的那个）：
     * 调用方只用它回答"<b>有没有</b>"（负数 = 一个都没按）。要问"<b>某一个</b>槽位按着没有"
     * 用 {@link #slotPressed}。</p>
     *
     * @param player 目标玩家
     * @return 主手物品的某个技能槽位号；没有物品 / 没有已迁移技能 / 没按键时 {@code -1}
     */
    public static int pressedSlot(@Nullable ServerPlayer player) {
        if (player == null) {
            return -1;
        }
        for (Map.Entry<Integer, SkillBundle> binding : componentOf(player).bindings().entrySet()) {
            Integer slot = binding.getKey();
            if (slot == null || !PlayerPressedKeys.isPressed(player, slot)) {
                continue;
            }
            if (ArmorSkillProvider.isEquipmentSlot(slot)) {
                continue;
            }
            return slot;
        }
        return -1;
    }

    /**
     * <b>服务端权威：该玩家主手物品的指定技能槽位此刻是否被按住</b>（2026-10-05 弓技能批 8 ②）。
     *
     * <p>"该槽位<b>已绑定技能</b>且此刻被按住"是<b>一个</b>判据（遍历的就是绑定表本身）：
     * 槽位没有绑定任何技能时恒 {@code false} —— 这就是"翠玉之弓没有第三个技能槽"的机器来源
     * （它上面按 G 不会被当成专属技能意图）。</p>
     *
     * <p>装备段槽位（3/4/5）不属于本来源（{@link ArmorSkillProvider} 另管一段），
     * 这里的守卫与 {@code CoeSkillRelease#release} 的遍历逐条同形：形状对齐，不是新语义。</p>
     *
     * @param player 目标玩家
     * @param slot   要问的槽位号
     * @return 该槽位已绑定技能且此刻被按住；否则 {@code false}
     */
    public static boolean slotPressed(@Nullable ServerPlayer player, int slot) {
        if (player == null || ArmorSkillProvider.isEquipmentSlot(slot)) {
            return false;
        }
        return componentOf(player).bindings().containsKey(slot)
                && PlayerPressedKeys.isPressed(player, slot);
    }

    /** 真正的转换：旧组件 → 新组件（结果会被 {@link #componentOf} 缓存）。 */
    private static SkillComponent convert(SkillsComponent legacy) {
        // 槽位 → 该槽位下所有类型的实例（三个类型共用同一条按键）
        Map<Integer, List<ISkillInstance<?>>> bySlot = new LinkedHashMap<>();
        for (Map.Entry<SkillType, com.leaf.skiller.foundation.skill.SkillType> entry : TYPE_MAPPING.entrySet()) {
            List<DataSkill> skills = legacy.getDataSkills(entry.getKey());
            if (skills == null || skills.isEmpty()) {
                continue;
            }
            for (int slot = 0; slot < skills.size(); slot++) {
                ISkillInstance<?> instance = toInstance(skills.get(slot));
                if (instance == null) {
                    continue;
                }
                bySlot.computeIfAbsent(slot, key -> new ArrayList<>()).add(instance);
            }
        }
        if (bySlot.isEmpty()) {
            return SkillComponent.EMPTY;
        }

        Map<Integer, SkillBundle> bindings = new LinkedHashMap<>();
        bySlot.forEach((slot, instances) -> bindings.put(slot, new SkillBundle(instances)));
        return new SkillComponent(bindings);
    }

    /**
     * 单个旧技能数据 → Skiller 技能实例。
     *
     * @return 已迁移的技能实例；未注册进新内核（= 仍归旧框架管）或数据不完整时返回 null
     */
    @Nullable
    private static ISkillInstance<?> toInstance(DataSkill data) {
        if (data == null) {
            return null;
        }
        // 2026-09-30 第 4 阶段：id 现在直接存在 DataSkill 上（序列化真源），
        // 不再走「技能实例 → 反查 id」这条旧内核的迂回路径。
        ResourceLocation skillId = data.id;
        if (skillId == null) {
            return null;
        }
        // 未注册 = 还没迁移：交给旧框架，绝不产出实例（否则会新旧双重生效）
        ItemSkillRegistration<?> registration = SkillerBuiltInRegistries.SKILLS.get(skillId);
        if (registration == null) {
            return null;
        }
        ResourceLocation factoryId = defaultFactoryId();
        if (factoryId == null) {
            return null;
        }

        CompoundTag nbt = data.nbt == null ? new CompoundTag() : data.nbt.copy();
        int level = nbt.contains(LEGACY_LEVEL_KEY) ? nbt.getInt(LEGACY_LEVEL_KEY) : 1;
        nbt.putString(RESOURCE_KEY, CoeToolEnergyResource.ID.toString());
        nbt.putInt(LEVEL_KEY, level);

        return ISkillInstance.fromData(new SkillData(skillId, factoryId, nbt));
    }

    /**
     * Skiller 默认技能工厂（{@code skiller:default}）的注册表 id。
     *
     * <p>工厂枚举已保证 {@code getFactory()} 返回注册时那一个实例（身份查找才成立），
     * 这里取不到时返回 null 并由调用方丢弃该条数据，避免写出 {@code factoryId == null}
     * 的非法实例。</p>
     */
    @Nullable
    private static ResourceLocation defaultFactoryId() {
        return SkillerBuiltInRegistries.SKILL_FACTORIES.getKey(AllSkillInstanceFactories.DEFAULT.getFactory());
    }
}
