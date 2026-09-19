package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            return SkillComponent.EMPTY;
        }

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
        if (data == null || data.skill == null) {
            return null;
        }
        ResourceLocation skillId = AllSkills.getId(data.skill);
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
