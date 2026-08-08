package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 技能组件 - 实现 OwnedBySkills 接口
 * 用于 ItemStack 的 SKILLS data component，直接存储和管理 ItemSkill 对象
 *
 * <p>此类是不可变的，所有方法都返回不可变视图或新实例。</p>
 */
public class SkillsComponent implements OwnedBySkills {

    private final Map<SkillType, List<ItemSkill>> skillsMap;

    /**
     * 空技能组件
     */
    public static final SkillsComponent EMPTY = new SkillsComponent();

    /**
     * 空构造器 - 创建空技能组件
     */
    public SkillsComponent() {
        this.skillsMap = Collections.emptyMap();
    }

    /**
     * 从技能列表创建
     * @param skills 技能列表（可以为null）
     */
    public SkillsComponent(List<ItemSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            this.skillsMap = Collections.emptyMap();
        } else {
            this.skillsMap = groupSkillsByType(skills);
        }
    }

    /**
     * 从技能ID字符串列表创建
     * @param skillIds 技能ID列表（可以为null）
     */
    public static SkillsComponent fromStrings(List<String> skillIds) {
        return new SkillsComponent(getSkills(skillIds));
    }

    /**
     * 将技能列表转换为ID字符串列表
     * @param skills 技能列表（可以为null）
     */
    public static List<String> getStrings(List<ItemSkill> skills) {
        if (skills == null) return Collections.emptyList();
        return skills.stream()
                .map(AllSkills::getId)
                .filter(Objects::nonNull)
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());
    }

    /**
     * 从技能ID字符串列表解析为技能对象列表
     * @param strings 技能ID列表（可以为null）
     */
    public static List<ItemSkill> getSkills(List<String> strings) {
        if (strings == null) return Collections.emptyList();
        return strings.stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .map(AllSkills::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ========== OwnedBySkills 接口实现 ==========

    @Override
    public Map<SkillType, List<ItemSkill>> skills() {
        // 返回不可变视图，确保外部无法修改
        if (skillsMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<SkillType, List<ItemSkill>> unmodifiableMap = new HashMap<>(skillsMap.size());
        skillsMap.forEach((type, list) ->
            unmodifiableMap.put(type, Collections.unmodifiableList(list))
        );
        return Collections.unmodifiableMap(unmodifiableMap);
    }

    // ========== 实用查询方法 ==========

    /**
     * 获取所有技能的扁平列表
     * @return 不可变列表
     */
    public List<ItemSkill> getAllSkills() {
        if (skillsMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemSkill> all = new ArrayList<>();
        for (List<ItemSkill> skillList : skillsMap.values()) {
            all.addAll(skillList);
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return skillsMap.isEmpty();
    }

    /**
     * 获取技能总数
     */
    public int size() {
        return getAllSkills().size();
    }

    // ========== Object 方法重写 ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillsComponent that)) return false;
        return getAllSkills().equals(that.getAllSkills());
    }

    @Override
    public int hashCode() {
        return getAllSkills().hashCode();
    }

    @Override
    public String toString() {
        return "SkillsComponent{" +
                "skills=" + getStrings(getAllSkills()) +
                '}';
    }

    // ========== 私有辅助方法 ==========

    /**
     * 按类型分组技能，并返回不可变Map
     */
    private static Map<SkillType, List<ItemSkill>> groupSkillsByType(List<ItemSkill> skills) {
        Map<SkillType, List<ItemSkill>> result = new HashMap<>();

        for (ItemSkill skill : skills) {
            result.computeIfAbsent(skill.getType(), k -> new ArrayList<>()).add(skill);
        }

        // 转换为不可变
        Map<SkillType, List<ItemSkill>> immutable = new HashMap<>(result.size());
        result.forEach((type, list) ->
            immutable.put(type, Collections.unmodifiableList(list))
        );

        return Collections.unmodifiableMap(immutable);
    }
}
