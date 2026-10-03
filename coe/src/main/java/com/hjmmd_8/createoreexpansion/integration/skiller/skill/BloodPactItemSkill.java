package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;

/**
 * 血契置换（{@code createoreexpansion:blood_pact}）在新内核里的技能条目。
 *
 * <p><b>⚠ coe-pact 批 1 的本文件是"注册脚手架"，两个方法体都还是空的 —— 执行体属批 2。</b>
 * 批 1 只做三件事：{@code AllSkills} 注册 id / 类型 / 数值真源
 * （{@code BloodPactConfigs}）、{@code SkillerIntegration#registerHitSkills} 登记内核白名单条目、
 * 以及两处语言键。本类之所以必须先存在，是因为 {@code skiller:skill} 是<b>数据驱动白名单</b>：
 * 注册条目必须带"类型 + 上下文工厂 + 技能实现"三件套，而
 * {@code SkillerIntegration} 的注册计数与 {@code AllSkills} 必须逐字对齐
 * （{@code tools/check-skill-registry-parity.ps1}）。</p>
 *
 * <h2>为什么不是"再建一个共用空壳"</h2>
 * <p>回旋镖的穿刺 / 环绕两条"执行不在内核里"的技能复用 {@code EquipmentSkillStub}
 * （用户 2026-10-01：「能继承就继承，别一个技能复制一个类」），
 * 而 {@code tools/check-armor-sets.ps1} 明确断言 {@code integration/skiller/skill} 下
 * <b>只有一个 {@code *Stub*.java}</b>（即 {@code EquipmentSkillStub.java}）——
 * 所以 HIT 族<b>不</b>另造空壳：这里就是血契置换自己的实现槽位，批 2 直接把体填进来。</p>
 *
 * <h2>批 2 要在这里实现的口径（需求 §3.4 的触发顺序，别改顺序）</h2>
 * <ol>
 *   <li>本次命中已由 {@code HurtLivingEntityHandler} 交给内核（{@code LivingIncomingDamageEvent}
 *       发生在扣血<b>之前</b>，所以这里天然是"未受到攻击之前"）；</li>
 *   <li>【冻结】读下 Hp_p / M_p / Hp_t / M_t —— <b>玩家那一侧也要冻结</b>（公式对称）；</li>
 *   <li>【交换】玩家新血 = M_p × (Hp_t / M_t)，对方新血 = M_t × (Hp_p / M_p)；结果恒 ≤ 上限、
 *       且只要双方都活着就恒 &gt; 0 ⇒ <b>不要加最低血量保护、不要加多余的上限钳位</b>
 *       （需求 §3.1 的三条性质、§5.3 陷阱 3/4）；</li>
 *   <li>【伤害】照常走这次攻击的伤害（本类<b>不碰</b>伤害链路）；</li>
 *   <li>【结算】能量消耗与冷却 —— 冷却载体（D2：A 沿用按物品记 / B 新建按技能记 / C 三个都改）
 *       <b>尚未裁定</b>；数值已在 {@code BloodPactConfig}（{@code energyCost} /
 *       {@code cooldownTicks} = 600 tick = 30 秒），批 1 <b>没有任何读取方</b>。</li>
 * </ol>
 *
 * @since 1.0.0
 */
public final class BloodPactItemSkill implements ItemSkill<HitSkillContext> {

    @Override
    public void release(HitSkillContext context, ISkillInstance<HitSkillContext> instance) {
        // 批 2 实现：按百分比双向交换双方血量（顺序见类注释），再让这次攻击照常造成伤害。
    }

    @Override
    public void consumeResource(HitSkillContext context, Consumable consumable,
                                ISkillInstance<HitSkillContext> instance) {
        // 批 2 实现：能量消耗（BloodPactConfig#energyCost）+ 冷却（载体待作者裁定）。
    }
}
