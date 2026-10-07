package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;

import java.util.List;

/**
 * 血契置换技能配置 —— 5 级共用（数值统一在 {@link BloodPactConfigs} 定义）。
 *
 * <p><b>本类刻意不含交换比例字段</b>：交换是「双方互换各自的当前血量百分比」的恒等式 ——
 * 玩家新血 = 上限_p × (Hp_t / M_t)、对方新血 = 上限_t × (Hp_p / M_p)（需求 §3.1，
 * 作者验收例 20/20 ⇄ 30/40 ⇒ 15/20 与 40/40），与等级无关（需求 §六 #4）
 * ⇒ 比例不进配置表；写死"没有等级加成"本身就是正确语义。</p>
 */
public class BloodPactConfig extends AutoSkillConfig {

    /**
     * <b>一级消耗</b>（= {@link BloodPactConfigs#ENERGY_COST_PER_LEVEL} = 100，见该类）。
     *
     * <p>实扣 = <b>本值 × 有效等级</b>（100 / 200 / 300 / 400 / 500），"乘等级"发生在
     * {@code CoeSkillSupport#cost} ← {@code SkillEnergyCost#compute} 里 —— 所以<b>本字段不写等级总额</b>
     * （批 1~13 写的是 100/200/300/400/500，于是实扣被二次乘成 100/400/900/1600/2500；
     * 作者 2026-10-06 批 14 裁定实扣 = 100 × 等级）。</p>
     */
    public int energyCost;

    /**
     * 技能冷却（tick）：600 tick = 30 秒（作者 2026-10-03 定），1~5 级恒为此值。
     *
     * <p>载体 = <b>按技能记</b>的 {@code PerSkillCooldown}（coe-pact 批 2 接上；作者 2026-10-03
     * 裁定 B）—— 唯一读取方是 {@code integration/skiller/skill/BloodPactItemSkill}。
     * 与按物品记的剥取 / 夺取冷却（{@code ToolSkillCooldown}）<b>互不读取</b>，
     * 所以 30 秒不会连坐到同一把剑的另外两个技能。</p>
     *
     * <p>另注：同族既有配置（{@code SkinConfig} / {@code PlunderConfig}）的冷却字段记的是
     * <b>秒</b>；这里按作者原话记 <b>tick</b>，秒数由 {@link #cooldownSeconds()} 换算
     * （NBT 键也叫 {@code CooldownTicks}，与它们的 {@code Cooldown} 不同名，避免同名不同单位）。</p>
     */
    public int cooldownTicks;

    public BloodPactConfig(int energyCost, int cooldownTicks) {
        this.energyCost = energyCost;
        this.cooldownTicks = cooldownTicks;
    }

    /**
     * 冷却秒数（= {@link #cooldownTicks} / {@link ChargeConfigs#TICKS_PER_SECOND}）。
     *
     * <p>存在的理由：批 2 无论选哪个冷却载体都要换算（{@code CoeSkillSupport.cooldownTicks}
     * 收的是秒、{@code ToolSkillCooldown.startTicks} 收的是 tick），换算只留这一处。</p>
     */
    public int cooldownSeconds() {
        return cooldownTicks / ChargeConfigs.TICKS_PER_SECOND;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("Cost", () -> energyCost, value -> energyCost = value),
                ofInt("CooldownTicks", () -> cooldownTicks, value -> cooldownTicks = value)
        );
    }
}
