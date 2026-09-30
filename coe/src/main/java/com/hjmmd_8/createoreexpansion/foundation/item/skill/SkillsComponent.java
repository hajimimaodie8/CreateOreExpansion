package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.HitSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 技能组件 - 实现 OwnedBySkills 接口
 * 用于 ItemStack 的 SKILLS data component，直接存储和管理 ItemSkill 对象
 *
 * 此类是不可变的，所有方法都返回不可变视图或新实例。
 */
public class SkillsComponent implements OwnedBySkills {

    private final Map<SkillType, List<ItemSkill>> skillsMap;
    private final Map<SkillType, List<DataSkill>> dataSkills;

    /**
     * 空技能组件
     */
    public static final SkillsComponent EMPTY = new SkillsComponent();

    /**
     * 空构造器 - 创建空技能组件
     */
    public SkillsComponent() {
        this.skillsMap = Collections.emptyMap();
        this.dataSkills = Collections.emptyMap();
    }

    /**
     * 从技能列表创建
     * @param skills 技能列表（可以为null）
     */
    public SkillsComponent(List<DataSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            this.skillsMap = Collections.emptyMap();
            this.dataSkills = Collections.emptyMap();
        } else {
            var pair = groupSkillsByType(skills);
            this.skillsMap  = pair.getFirst();
            this.dataSkills = pair.getSecond();
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
    public static List<String> getStrings(List<DataSkill> skills) {
        if (skills == null) return Collections.emptyList();
        return skills.stream()
                .map(DataSkill::toString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 从技能ID字符串列表解析为技能对象列表
     * @param strings 技能ID列表（可以为null）
     */
    public static List<DataSkill> getSkills(List<String> strings) {
        if (strings == null) return Collections.emptyList();
        return strings.stream()
                .map(DataSkill::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ========== OwnedBySkills 接口实现 ==========

    @Override
    public Map<SkillType, List<ItemSkill>> skills() {
        // groupSkillsByType 已构建不可变视图，直接返回
        return skillsMap;
    }

    /**
     * 按有效等级（基础等级 + 技能提升附魔，受技能满级限制）更新技能配置。
     *
     * <p>保留为静态工具：调用方（如工具/武器侧）需要按当前等级取配置时使用。
     * 2026-09-30 第 4 阶段后，<b>技能执行本身不再经过本类</b>——释放由 Skiller 新内核的
     * {@code CoeSkillRelease} 负责，本方法只剩「取该等级配置并记回 {@link DataSkill#config}」。</p>
     */
    public static void applySkillBoost(ItemStack stack, DataSkill data) {
        if (data == null) return;
        AllSkills.RegisteredDataSkill registered = AllSkills.getData(data.id);
        if (registered == null) return;
        int effective = SkillEnergySpend.effectiveLevel(stack, data);
        SkillConfig levelConfig = registered.configForLevel(effective);
        if (levelConfig != null) {
            data.config = levelConfig;
        }
    }

    // ========== 实用查询方法 ==========

    /**
     * 获取指定类型的技能数据列表（顺序 = 物品绑定技能时的顺序，槽位 0 开始）。
     * 用于剑类双技能：槽位 0=键一、槽位 1=键二。
     */
    public List<DataSkill> getDataSkills(SkillType type) {
        List<DataSkill> list = dataSkills.get(type);
        return list == null ? Collections.emptyList() : list;
    }

    public List<DataSkill> getAllData() {
        if (dataSkills.isEmpty()) {
            return new ArrayList<>();
        }
        List<DataSkill> all = new ArrayList<>();
        for (List<DataSkill> dataList : dataSkills.values()) {
            all.addAll(dataList);
        }
        return all;  // 返回可修改的新列表
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
        // 比较 DataSkill 列表（包含 NBT 和 cost）
        return getAllData().equals(that.getAllData());
    }

    @Override
    public int hashCode() {
        return getAllData().hashCode();
    }

    @Override
    public String toString() {
        return "SkillsComponent{" +
                "skills=" + getStrings(getAllData()) +
                '}';
    }

    // ========== 私有辅助方法 ==========

    /**
     * 按类型分组技能，并返回不可变Map
     */
    private static Pair<Map<SkillType, List<ItemSkill>>, Map<SkillType, List<DataSkill>>> groupSkillsByType(
            List<DataSkill> dataSkills) {
        Map<SkillType, List<ItemSkill>> skillsResult = new HashMap<>();
        Map<SkillType, List<DataSkill>> dataSkillsResult = new HashMap<>();

        for (DataSkill data : dataSkills) {
            ItemSkill skill = data.skill;
            skillsResult.computeIfAbsent(skill.getType(), k -> new ArrayList<>()).add(skill);
            dataSkillsResult.computeIfAbsent(skill.getType(), k -> new ArrayList<>()).add(data);
        }

        // 转换为不可变
        Map<SkillType, List<ItemSkill>> immutable = new HashMap<>(skillsResult.size());
        skillsResult.forEach((type, list) ->
            immutable.put(type, Collections.unmodifiableList(list))
        );

        Map<SkillType, List<DataSkill>> immutableData = new HashMap<>(dataSkillsResult.size());
        dataSkillsResult.forEach((type, list) ->
                immutableData.put(type, Collections.unmodifiableList(list))
        );

        return Pair.of(Collections.unmodifiableMap(immutable), Collections.unmodifiableMap(immutableData));
    }
}
