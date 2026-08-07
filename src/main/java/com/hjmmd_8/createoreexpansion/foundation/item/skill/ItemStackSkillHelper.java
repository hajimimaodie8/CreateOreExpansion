package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.AllSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ItemStack 技能辅助类
 * 由于 ItemStack 是 final 类，无法通过 mixin 添加接口实现，因此使用辅助类
 * <p>
 * 性能优化：
 * 1. 缓存技能加载结果，通过 skill component 内容哈希验证缓存有效性
 * 2. 使用不可变空集合减少对象创建
 * 3. 优化 map 查找次数
 */
public final class ItemStackSkillHelper {

    /** 技能缓存，key 为 skill component 的字符串哈希 */
    private static final Cache<Integer, Map<SkillType, List<ItemSkill>>> SKILLS_CACHE =
        CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    /** 空技能列表常量 */
    private static final Map<SkillType, List<ItemSkill>> EMPTY_SKILLS = Collections.emptyMap();

    /** 空技能列表常量 */
    private static final List<ItemSkill> EMPTY_SKILL_LIST = Collections.emptyList();

    /**
     * 检查物品是否有指定类型的技能
     */
    public static boolean hasSkill(ItemStack itemStack, SkillType type) {
        Map<SkillType, List<ItemSkill>> skills = getOrLoadSkills(itemStack);
        return skills.containsKey(type);
    }

    public static boolean hasSkill(ItemStack itemStack, ItemSkill skill) {
        Map<SkillType, List<ItemSkill>> skills = getOrLoadSkills(itemStack);
        SkillType type = skill.getType();
        List<ItemSkill> skillList = skills.get(type);
        return skillList != null && skillList.contains(skill);
    }

    public static boolean hasSkill(ItemStack stack) {
        return !getOrLoadSkills(stack).isEmpty();
    }

    public static List<ItemSkill> getSkills(ItemStack stack, SkillType type) {
        Map<SkillType, List<ItemSkill>> skills = getOrLoadSkills(stack);
        List<ItemSkill> skillList = skills.get(type);
        return skillList != null ? skillList : EMPTY_SKILL_LIST;
    }

    public static List<ItemSkill> getSkills(ItemStack stack) {
        Map<SkillType, List<ItemSkill>> skills = getOrLoadSkills(stack);
        List<ItemSkill> result = new ArrayList<>();
        skills.values().forEach(result::addAll);
        return result;
    }

    /**
     * 释放物品的指定类型技能
     */
    public static void releaseSkills(ItemStack itemStack, SkillType type, Object context) {
        Map<SkillType, List<ItemSkill>> skills = getOrLoadSkills(itemStack);
        List<ItemSkill> skillList = skills.get(type);

        if (skillList == null || skillList.isEmpty()) {
            return;
        }

        for (ItemSkill skill : skillList) {
            if (!skill.getType().cast(context))
                throw new ClassCastException("Skill context type mismatch");
            skill.release(context);
        }
    }

    /**
     * 获取或加载技能（带缓存）
     * 使用 skill component 内容哈希作为缓存键，确保只在技能真正变化时才重新加载
     */
    private static Map<SkillType, List<ItemSkill>> getOrLoadSkills(ItemStack itemStack) {
        // 获取 skill component 并计算哈希值
        List<String> skillIds = itemStack.getComponents().get(AllDataComponents.SKILLS);
        int componentHash = getSkillComponentHash(skillIds);

        // 尝试从缓存获取
        Map<SkillType, List<ItemSkill>> cached = SKILLS_CACHE.getIfPresent(componentHash);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，加载技能
        Map<SkillType, List<ItemSkill>> skills = loadSkills(skillIds);

        // 加入缓存（即使是空的也缓存，避免重复检查）
        SKILLS_CACHE.put(componentHash,
            skills.isEmpty() ? EMPTY_SKILLS : createImmutableSkillsCopy(skills));

        return skills.isEmpty() ? EMPTY_SKILLS : skills;
    }

    /**
     * 计算 skill component 的哈希值用于缓存键
     * 使用 List 的 toString() 方法，因为它会输出格式化后的字符串
     */
    private static int getSkillComponentHash(List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return 0;
        }
        // 直接使用 List 的 toString，它会生成 "[id1, id2, ...]" 格式的字符串
        return skillIds.toString().hashCode();
    }

    /**
     * 创建技能的不可变副本
     */
    private static Map<SkillType, List<ItemSkill>> createImmutableSkillsCopy(
            Map<SkillType, List<ItemSkill>> skills) {
        Map<SkillType, List<ItemSkill>> immutableMap = new HashMap<>(skills.size());
        skills.forEach((type, list) ->
            immutableMap.put(type, Collections.unmodifiableList(new ArrayList<>(list)))
        );
        return Collections.unmodifiableMap(immutableMap);
    }

    /**
     * 从技能 ID 列表加载技能
     * 优化：使用 computeIfAbsent 减少 map 查找次数
     */
    private static Map<SkillType, List<ItemSkill>> loadSkills(List<String> skillIds) {
        Map<SkillType, List<ItemSkill>> result = new HashMap<>();

        if (skillIds == null || skillIds.isEmpty()) {
            return result;
        }

        for (String skillId : skillIds) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(skillId);
            if (resourceLocation == null) {
                continue;
            }

            ItemSkill skill = AllSkills.get(resourceLocation);
            if (skill != null) {
                SkillType skillType = skill.getType();
                // 使用 computeIfAbsent 减少查找次数
                result.computeIfAbsent(skillType, k -> new ArrayList<>()).add(skill);
            }
        }

        return result;
    }

    /**
     * 手动清除指定 ItemStack 的缓存
     * 用于在技能变化后手动失效缓存
     */
    public static void invalidateCache(ItemStack stack) {
        List<String> skillIds = stack.getComponents().get(AllDataComponents.SKILLS);
        int componentHash = getSkillComponentHash(skillIds);
        SKILLS_CACHE.invalidate(componentHash);
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        SKILLS_CACHE.invalidateAll();
    }

    /**
     * 获取缓存统计信息（用于调试）
     */
    public static String getCacheStats() {
        return "Size: " + SKILLS_CACHE.size() + ", Stats: " + SKILLS_CACHE.stats();
    }
}