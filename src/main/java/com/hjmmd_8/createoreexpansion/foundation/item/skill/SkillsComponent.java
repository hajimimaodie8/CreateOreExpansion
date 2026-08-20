package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
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
     * 释放指定类型的技能。
     *
     * 流程：
     * <ul>
     *     <li>只释放 {@link ItemSkill#canRelease} 通过的技能（例如锄头的收割/种植按目标方块二选一）；</li>
     *     <li>释放前做一次总能量预检查，不足则整体放弃；</li>
     *     <li>能量扣减由各技能在真正生效前通过 {@link ToolEnergy#tryConsume} 自行完成
     *     （消耗以 {@link ItemSkill#getCost()} 为准，与注册配置一致）。</li>
     * </ul>
     *
     * @param skillStack 技能 ItemStack
     * @param type 技能类型
     * @param context 技能上下文
     * @return true=至少有一个技能被释放
     */
    @Override
    public boolean releaseSkills(SkillItemStack skillStack, SkillType type, Object context) {
        List<DataSkill> skills = dataSkills.get(type);
        if (skills == null || skills.isEmpty()) return false;

        ItemStack stack = skillStack.itemStack();
        Player player = resolvePlayer(context);

        // 1. 过滤出满足释放条件的技能
        List<DataSkill> toRelease = new ArrayList<>(skills.size());
        for (DataSkill data : skills) {
            if (data.skill.canRelease(context, data)) {
                toRelease.add(data);
            }
        }
        if (toRelease.isEmpty()) return false;

        // 1.5 技能提升附魔：先提升技能等级（受技能满级限制），再按提升后的等级计算消耗与效果
        for (DataSkill data : toRelease) {
            applySkillBoost(stack, data);
        }

        // 2. 能量预检查：能量不足以下一次（最低消耗的）技能释放时整体放弃。
        //    无论创造模式与否都消耗能量（与 ToolEnergy.tryConsume 一致），故不做创造豁免，
        //    否则低能量提示会被调用方的剩余能量提示覆盖。
        int minCost = toRelease.stream()
                .mapToInt(data -> SkillEnergyCost.compute(stack, data.skill))
                .min()
                .orElse(0);
        if (!ToolEnergy.canAfford(stack, minCost)) {
            if (player != null) ToolEnergy.sendLowEnergy(player, stack);
            return false;
        }

        // 3. 释放技能（能量由技能内部在真正生效前消耗）
        for (DataSkill data : toRelease) {
            data.skill.release(context, data);
        }
        return true;
    }

    /**
     * 按槽位释放指定类型的单个技能（剑类双技能：槽位 0=技能键一、槽位 1=技能键二）。
     *
     * 流程与 {@link #releaseSkills} 一致，但只释放指定槽位的技能，
     * 且各自执行独立的能量预检查与冷却读取，两个技能互不影响。
     *
     * @param skillStack 技能 ItemStack
     * @param type 技能类型
     * @param slot 技能槽位（0=第一个技能，1=第二个技能）
     * @param context 技能上下文
     * @return true=该槽位技能被释放
     */
    public boolean releaseSkillAt(SkillItemStack skillStack, SkillType type, int slot, Object context) {
        List<DataSkill> skills = dataSkills.get(type);
        if (skills == null || skills.isEmpty() || slot < 0 || slot >= skills.size()) return false;

        DataSkill data = skills.get(slot);
        if (!data.skill.canRelease(context, data)) return false;

        ItemStack stack = skillStack.itemStack();
        Player player = resolvePlayer(context);

        // 技能提升附魔：先提升技能等级（受技能满级限制），再按提升后的等级计算消耗与效果
        applySkillBoost(stack, data);

        // 能量预检查：不足则整体放弃（提示由低能量逻辑统一处理）
        if (!ToolEnergy.canAfford(stack, SkillEnergyCost.compute(stack, data.skill))) {
            if (player != null) ToolEnergy.sendLowEnergy(player, stack);
            return false;
        }

        // 释放（能量由技能内部在真正生效前消耗）
        data.skill.release(context, data);
        return true;
    }

    /**
     * 应用技能提升附魔：按有效等级（基础等级 + 附魔提升，受技能满级限制）更新释放时的效果配置。
     * 不写入物品 NBT——等级计算统一走 {@link SkillEnergyCost#effectiveLevel}，
     * 已满级时有效等级不变（停留原地），移除附魔即恢复原等级。
     */
    private static void applySkillBoost(ItemStack stack, DataSkill data) {
        AllSkills.RegisteredDataSkill registered = AllSkills.getData(AllSkills.getId(data.skill));
        if (registered == null) return;
        int effective = SkillEnergyCost.effectiveLevel(stack, data);
        SkillConfig levelConfig = registered.configForLevel(effective);
        if (levelConfig != null) data.config = levelConfig;
    }

    /**
     * 从技能上下文中解析出玩家，用于判断创造模式与发送提示消息。
     * 仅依赖技能上下文接口，不绑定具体事件类型。
     */
    private static Player resolvePlayer(Object context) {
        if (context instanceof ExcavationSkillContext excavation) {
            return excavation.entity() instanceof Player player ? player : null;
        }
        if (context instanceof UseItemContext<?> useContext) {
            return useContext.getPlayer();
        }
        if (context instanceof HitSkillContext hitContext) {
            return hitContext.player();
        }
        return null;
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
