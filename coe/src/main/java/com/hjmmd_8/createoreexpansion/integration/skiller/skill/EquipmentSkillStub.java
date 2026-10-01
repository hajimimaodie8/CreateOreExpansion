package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.integration.skiller.context.UseItemSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;

/**
 * <b>装备技能在内核里的"空壳条目"——所有装备技能共用这一个实例</b>（2026-10-01 收口）。
 *
 * <h2>为什么需要一个"什么都不做"的技能实现</h2>
 * <p>内核的技能注册表 {@code skiller:skill} 是<b>数据驱动的白名单</b>：实例反序列化
 * （{@code NbtSkillInstanceFactory#createFromData}）第一步就是 {@code SKILLS.get(skillId)}，
 * <b>查不到直接返回 null</b>。因此装备技能 id 也必须登记一条，否则
 * {@code ArmorSkillProvider} 构造不出实例 ⇒ 客户端不会为槽位 3/4/5 轮询按键（技能按不出来）、
 * HUD 也没有技能行。</p>
 *
 * <h2>为什么只有一个类（用户 2026-10-01：「能继承就继承，别一个技能复制一个类」）</h2>
 * <p>装备技能是<b>长按语义</b>（虚衡坠护按住期间持续豁免、蓄能疾骋按住越久 Buff 越强），
 * 内核只有"事件触发的瞬时释放"模型，所以<b>执行</b>统一在 {@code ArmorSkillRuntime}。
 * 于是内核侧的技能对象对<b>所有</b>装备技能都是同一个行为：什么都不做。
 * 因此这里做成<b>无状态单例</b>（{@link #INSTANCE}），注册时直接引用它即可 ——
 * 以后再加装备技能（绝境守护 / 临域充力 / 衡元择势 / 星芒嬗震 / 雷鸣威震…）<b>不需要再建类</b>。</p>
 *
 * <h2>为什么两个方法都是空的</h2>
 * <p>内核的释放路径已<b>显式跳过装备段槽位</b>（见 {@code CoeSkillRelease}），
 * 这两个方法永远不会被调用 —— 留空是<b>契约要求</b>，不是没写完。
 * 若哪天有人发现它们被调用了，说明那条跳过规则被删了，是回归。</p>
 *
 * @since 1.0.0
 */
public final class EquipmentSkillStub implements ItemSkill<UseItemSkillContext> {

    /** 唯一的共享实例（无状态，可安全复用；注册每个装备技能时都用它）。 */
    public static final EquipmentSkillStub INSTANCE = new EquipmentSkillStub();

    private EquipmentSkillStub() {
        // 单例：外部用 INSTANCE
    }

    @Override
    public void release(UseItemSkillContext context, ISkillInstance<UseItemSkillContext> instance) {
        // 空实现：装备技能由 ArmorSkillRuntime 执行（见类注释）
    }

    @Override
    public void consumeResource(UseItemSkillContext context, Consumable consumable,
                                ISkillInstance<UseItemSkillContext> instance) {
        // 空实现：扣费也在 ArmorSkillRuntime（按长按时长比例、向下取整）
    }
}
