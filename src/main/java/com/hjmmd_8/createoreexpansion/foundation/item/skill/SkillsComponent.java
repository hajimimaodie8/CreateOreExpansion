package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.ItemStack;

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
    private final Map<SkillType, List<DataSkill>> dataSkills;

    /**
     * 该字段不会参与网络通信和持续化，只能用于修改dataSkills的cost值
     */
    private final SkillCostModifier costModifier = SkillCostModifier.DEFAULT;

    /**
     * 修改 Cost，必须搭配 {@linkplain SkillsComponent#applyModifier()} 来应用更改
     * @param modifier Cost 修改器
     * @see SkillsComponent#applyModifier()
     */
    public void modifierCost(SkillCostModifier modifier) {
        costModifier.merge(modifier);
    }

    /**
     * 应用 {@linkplain SkillsComponent#modifierCost(SkillCostModifier)} 的更改
     * @see SkillsComponent#modifierCost(SkillCostModifier)
     */
    public void applyModifier() {
        for (DataSkill data : getAllData()) {
            data.modifyCost(costModifier);
        }
    }

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

    public static SkillsComponent of(List<ItemSkill> skills) {
        return new SkillsComponent(skills.stream()
                .map(DataSkill::fromSkill)
                .collect(Collectors.toList()));
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

    @Override
    public boolean releaseSkills(SkillItemStack skillStack, SkillType type, Object context) {
        List<DataSkill> skills = dataSkills.get(type);
        ItemStack stack = skillStack.itemStack();

        int energySum = 0;
        for (DataSkill data : skills) {
            energySum += data.cost;
        }

        if (energySum != 0 || !ToolEnergy.hasEnergy(stack)) return false;
        int energy = ToolEnergy.getEnergy(stack);
        if (ToolEnergy.isFailure(stack) || energy < energySum) return false;

        for (DataSkill data : skills) {
            ToolEnergy.consumeForSkill(stack, data);
        }

        return true;
    }

    // ========== 实用查询方法 ==========

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
