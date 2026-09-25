package com.hjmmd_8.createoreexpansion.skill;

import com.hjmmd_8.createoreexpansion.common.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.common.energy.ToolEnchantments;
import com.hjmmd_8.createoreexpansion.common.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * <b>旧技能框架（{@link ItemSkill} / {@link DataSkill}）专用的能量口径与扣能编排</b>。
 *
 * <p><b>P3p 从 core 搬出来的一支</b>：它原先住在
 * {@code common.energy.SkillEnergyCost}（有效等级与消耗）与
 * {@code common.energy.ToolEnergy#tryConsume}（扣能编排）。那两个类已搬进共享库（core），
 * 而这里用到的东西——技能注册表 {@link AllSkills}、{@code SKILLS} 组件
 * （{@link AllDataComponents#SKILLS}）、旧技能类型 {@link DataSkill}/{@link ItemSkill}——
 * 全是层内（或根侧共享层里的旧框架）类型，<b>库不能反向依赖它们</b>，
 * 于是这一支整体搬到旧框架自己的包里，<b>方法体逐字未改</b>。</p>
 *
 * <p><b>新内核（Skiller）不走这里</b>：它直接调
 * {@link SkillEnergyCost#effectiveLevel(ItemStack, int, int)} /
 * {@link SkillEnergyCost#compute(ItemStack, int, int)}（只吃 {@code int} 的那两个重载）。
 * 于是「通用算术住 core、旧框架胶水住旧框架包」这条界线在源码上看得见。</p>
 *
 * <p>调用点：{@code content.skill.*} 的各技能与 {@code SkillsComponent}——
 * 其中 {@code SkillsComponent} 与本类同包，无需 import。</p>
 */
public final class SkillEnergySpend {

    private SkillEnergySpend() {
    }

    /**
     * 技能有效等级 = min(基础等级 + 技艺提升 - 技艺回溯, 技能满级)，且不低于 1。
     * 显示与消耗统一以此为准。技艺提升/技艺回溯 3 级及以上提升量/削减量一律按 2 计，
     * 两个附魔可共存（净效果 = 提升量 - 削减量）。
     *
     * <p>（原 {@code SkillEnergyCost.effectiveLevel(ItemStack, DataSkill)}，方法体逐字未改。）</p>
     */
    public static int effectiveLevel(ItemStack stack, DataSkill data) {
        int base = data.nbt != null ? data.nbt.getInt("Level") : 1;
        AllSkills.RegisteredDataSkill registered = AllSkills.getData(AllSkills.getId(data.skill));
        int maxLevel = registered != null ? registered.maxLevel() : 5;
        int boost = Math.min(ToolEnchantments.skillBoostLevel(stack), 2);
        int regression = Math.min(ToolEnchantments.skillRegressionLevel(stack), 2);
        int level = Math.max(1, base) + boost - regression;
        return Math.max(1, Math.min(level, maxLevel));
    }

    /**
     * 计算一次技能释放的实际能量消耗（旧框架版）。
     *
     * <p>口径与 {@link SkillEnergyCost#compute(ItemStack, int, int)} 完全一致，
     * 只是由本方法自己从技能实例取一级消耗、从 {@code SKILLS} 组件取当前等级。</p>
     *
     * @param stack 手持的工具
     * @param skill 将要释放的技能
     * @return 实际消耗（&lt;= 0 表示无需能量）
     */
    public static int compute(ItemStack stack, ItemSkill skill) {
        return SkillEnergyCost.compute(stack, skill.getCost(), skillLevel(stack, skill));
    }

    /**
     * 技能释放前统一检查并消耗能量。
     *
     * <p>由各技能在“真正生效前”调用一次（例如破坏方块前、收割前），
     * 能量不足时发送低能量提示并返回 false，技能应放弃本次释放。</p>
     *
     * <p>注意：无论创造模式与否都会消耗能量（与旧行为一致），
     * 消耗后立即标记物品栏变更，确保客户端能量条同步刷新。</p>
     *
     * <p>实现上把「凝能佩兜底」的判定与扣减委托给
     * {@link ToolEnergy#canAfford(Player, ItemStack, int)} /
     * {@link ToolEnergy#consume(Player, ItemStack, int)}，本方法只负责编排：
     * 预检查 → 扣能 → 提示。</p>
     *
     * <p>（原 {@code ToolEnergy#tryConsume(Player, ItemStack, ItemSkill)}，方法体逐字未改——
     * 那时它读的是同类里的 {@code SkillEnergyCost.compute}，现在读本类的
     * {@link #compute(ItemStack, ItemSkill)}，同一个实现。）</p>
     *
     * @param player 释放技能的玩家（可为 null）
     * @param stack  手持的工具
     * @param skill  将要释放的技能（通过 {@link ItemSkill#getCost()} 获取消耗）
     * @return 是否成功消耗能量（true 表示可以继续执行技能）
     */
    public static boolean tryConsume(Player player, ItemStack stack, ItemSkill skill) {
        int cost = compute(stack, skill);
        if (cost == 0) {
            return true;
        }
        // 预检查失败、或扣减失败（能量不足）都按旧行为提示并放弃本次释放
        if (!ToolEnergy.canAfford(player, stack, cost) || !ToolEnergy.consume(player, stack, cost)) {
            if (player != null) {
                ToolEnergy.sendLowEnergy(player, stack);
            }
            return false;
        }
        if (player != null) {
            // 强制物品栏同步，确保客户端立即看到能量变化
            player.getInventory().setChanged();
            // 同步显示剩余能量：绑定的凝能佩行在上、工具行在下（护目镜判定）
            ToolEnergy.sendRemainingEnergyWithMedallion(player, stack, IMedallion.findBoundMedallion(player, stack));
        }
        return true;
    }

    /**
     * 读取工具上指定技能的当前有效等级（含技能提升附魔）。
     *
     * @return 技能等级，找不到时为 1
     */
    private static int skillLevel(ItemStack stack, ItemSkill skill) {
        SkillsComponent component = stack.get(AllDataComponents.SKILLS);
        if (component != null) {
            for (DataSkill data : component.getAllData()) {
                if (data.skill == skill) {
                    return effectiveLevel(stack, data);
                }
            }
        }
        return 1;
    }
}
