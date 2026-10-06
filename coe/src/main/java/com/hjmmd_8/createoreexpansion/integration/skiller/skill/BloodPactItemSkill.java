package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.PerSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BloodPactConfig;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.ItemSkill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 血契置换（{@code createoreexpansion:blood_pact}）在新内核里的技能条目 —— <b>执行体</b>
 * （coe-pact 批 2；批 1 建的空槽位在这里填上）。
 *
 * <h2>口径（需求 §3.1 / §3.4，作者 2026-10-03 裁定；顺序不许改）</h2>
 * <ol>
 *   <li><b>触发</b>：这一击的命中链把本次伤害事件交到内核（{@code HurtLivingEntityHandler} →
 *       {@code LivingIncomingDamageEvent}），而该事件发生在<b>扣血之前</b>
 *       （{@code LivingEntity#hurt} 里 {@code CommonHooks.onEntityIncomingDamage(..)} 在
 *       {@code actuallyHurt} 之前 —— {@code LivingEntity.java:1143-1144}）
 *       ⇒ 本类读到的血量天然是"<b>未受到这一击之前</b>"的；</li>
 *   <li><b>冻结</b>四个值：玩家 {@code Hp_p} / {@code M_p}、目标 {@code Hp_t} / {@code M_t}
 *       —— 玩家那一侧<b>也要</b>冻结（公式对称，需求 §3.4 第 2 步 / §5.3 陷阱 2）；</li>
 *   <li><b>交换</b>：{@code 玩家新血 = M_p × (Hp_t / M_t)}、{@code 对方新血 = M_t × (Hp_p / M_p)}；
 *       两个结果都先算完再写回（否则"先写玩家"会污染对方那一侧要用的 {@code Hp_p}）；
 *       结果恒 ≤ 各自上限、双方都活着时恒 &gt; 0 ⇒ <b>不自己加上限钳位、也不加最低血量保护</b>
 *       （需求 §3.1 三条性质、§5.3 陷阱 3/4；写回只靠原版
 *       {@code LivingEntity#setHealth} 自带的 {@code Mth.clamp(hp, 0, getMaxHealth())}
 *       —— {@code LivingEntity.java:1124-1126}）；</li>
 *   <li><b>伤害</b>：这一击的既有伤害<b>照常</b> —— 本类不碰事件、不改伤害值、不取消、
 *       也不自己调 {@code hurt(..)}（换血不是伤害，免疫帧与伤害类型都与它无关）；</li>
 *   <li><b>结算</b>：能量走 {@code createoreexpansion:tool_energy}（工具技能的统一资源，
 *       {@code CoeSkillProvider} 给每个工具技能实例都挂的那一个），冷却走
 *       {@link PerSkillCooldown}（<b>按技能记</b>，30 秒 = 600 tick，与同一把剑上的剥取 / 夺取
 *       互不连坐 —— 作者 2026-10-03 裁定 B）。</li>
 * </ol>
 *
 * <h2>为什么没有 {@code getStrategy()}</h2>
 * <p>剥取 / 夺取实现 {@code StrategySkill} 只是为了客户端实体描边预览（它们的 {@code collect}
 * 恒为空集）。血契置换是<b>纯命中效果、没有范围选择</b>：主目标由既有命中链给的那一个决定，
 * 所以本类不需要策略对象，也不产生任何预选框。</p>
 *
 * <h2>一次挥砍打到多个目标</h2>
 * <p>扫击的每个目标各自发一次事件，而<b>第一个</b>（= 主目标）换完之后立刻起了冷却
 * ⇒ 随后的从目标在 {@link #release} 的开头就被冷却挡掉 ⇒ 天然"<b>只对主目标交换</b>"
 * （需求 §3.5 / §六 #7 的推断口径），不需要另写一个"本次挥砍已用过"的标记。</p>
 *
 * @since 1.0.0
 */
public final class BloodPactItemSkill implements ItemSkill<HitSkillContext> {

    @Override
    public void release(HitSkillContext context, ISkillInstance<HitSkillContext> instance) {
        LivingEntity target = context.target();
        Player player = context.getPlayer();
        if (target == null || player == null || target.level().isClientSide()) {
            return;
        }
        ItemStack sword = player.getMainHandItem();
        BloodPactConfig config = CoeSkillSupport.configForLevel(sword, instance, BloodPactConfig.class);
        if (config == null) {
            return;
        }
        ResourceLocation skillId = CoeSkillSupport.skillIdOf(instance);
        if (skillId == null) {
            return;
        }
        // 冷却中：不交换（consumeResource 里同判 —— 两处口径必须一致，否则会出现
        // "扣了能量却没换血"或"没扣能量却换了血"）
        if (!PerSkillCooldown.isReady(player, skillId)) {
            return;
        }

        // 【冻结】攻击前的四个值（这一击的伤害还没发生，见类注释第 1 条）
        float playerHealth = player.getHealth();
        float playerMaxHealth = player.getMaxHealth();
        float targetHealth = target.getHealth();
        float targetMaxHealth = target.getMaxHealth();

        // 【边界】上限为 0 会除零 ⇒ 不交换，打一条 warn（需求 §3.5 第 2 行）；
        // 任一方当前血量 ≤ 0 ⇒ 也由同一个判据挡住（§3.5 第 3 行：防产生 0 / 负值）。
        // 判的是刚才冻结下来的那几个值 ⇒ 不会把四个血值读第二遍。
        if (targetMaxHealth <= 0.0F || playerMaxHealth <= 0.0F) {
            CoeCore.LOGGER.warn("[血契置换] 血量上限为 0（玩家上限 {} / 目标上限 {}）⇒ 本次不交换，伤害照常",
                    playerMaxHealth, targetMaxHealth);
            return;
        }
        if (!isSwappable(playerHealth, playerMaxHealth, targetHealth, targetMaxHealth)) {
            return;
        }

        // 【交换】交换公式的唯一一处。两个结果先算完再写回（对称、顺序无关）。
        float newPlayerHealth = playerMaxHealth * (targetHealth / targetMaxHealth);
        float newTargetHealth = targetMaxHealth * (playerHealth / playerMaxHealth);
        player.setHealth(newPlayerHealth);
        target.setHealth(newTargetHealth);

        // 【结算】冷却（按技能记；写与显示在 PerSkillCooldown 里同一入口）
        int ticks = config.cooldownTicks;
        if (ticks > 0) {
            PerSkillCooldown.startTicks(player, skillId, ticks);
        }
    }

    /**
     * 本次释放真的会交换吗 —— 本技能的 {@code willDoWork} 等价物
     * （{@code AGENTS.md}：{@code consumeResource} 必须先过"这次到底会不会有产出"）。
     *
     * <p>新内核的流程是"<b>先</b> {@code consumeResource} <b>再</b> {@code release}"，所以
     * {@code consumeResource} 里无条件扣能会让"打一个血量上限为 0 的异常目标"白掉一份能量。
     * {@link CoeSkillSupport#willDoWork} 那个泛型版是为<b>策略收集</b>类技能写的
     * （挖掘系：收集结果为空 = 不出力），血契置换没有策略对象，产出判据就是
     * "双方都还有血、上限都 &gt; 0" ⇒ 这里用同语义的、<b>唯一一处</b>的判据，
     * {@code consumeResource} 与 {@code release} 共用（{@code release} 传的是它冻结下来的值，
     * 所以两处判定逐值等价，且不会把四个血值读第二遍）。</p>
     */
    private static boolean isSwappable(float playerHealth, float playerMaxHealth,
                                       float targetHealth, float targetMaxHealth) {
        return playerMaxHealth > 0.0F && targetMaxHealth > 0.0F
                && playerHealth > 0.0F && targetHealth > 0.0F;
    }

    /**
     * 消耗：冷却通过 + 这次真的会交换 ⇒ 扣 {@code 一级消耗 × 有效等级 × 减耗折扣}
     * （{@code BloodPactConfig#energyCost} = <b>一级消耗 100</b>
     * = {@code BloodPactConfigs#ENERGY_COST_PER_LEVEL}，乘等级由
     * {@code SkillEnergyCost#compute} 完成 ⇒ 实扣 100 / 200 / 300 / 400 / 500；
     * 资源 = {@code tool_energy}）。
     *
     * <p>能量不够由内核统一裁决（{@code DelayConsumable#canConsume} 不过 ⇒ 不扣不释放），
     * 提示由 {@link CoeSkillSupport#consume} 补（与剥取 / 夺取同一条路径）。</p>
     */
    @Override
    public void consumeResource(HitSkillContext context, Consumable consumable,
                                ISkillInstance<HitSkillContext> instance) {
        LivingEntity target = context.target();
        Player player = context.getPlayer();
        if (target == null || player == null || target.level().isClientSide()) {
            return;
        }
        ItemStack sword = player.getMainHandItem();
        BloodPactConfig config = CoeSkillSupport.configForLevel(sword, instance, BloodPactConfig.class);
        if (config == null) {
            return;
        }
        ResourceLocation skillId = CoeSkillSupport.skillIdOf(instance);
        if (skillId == null) {
            return;
        }
        // 冷却中：不消耗（release 里同一条判定）
        if (!PerSkillCooldown.isReady(player, skillId)) {
            return;
        }
        // 不会交换就绝不扣能（willDoWork 等价物；与 release 的边界判定同一处）
        if (!isSwappable(player.getHealth(), player.getMaxHealth(),
                target.getHealth(), target.getMaxHealth())) {
            return;
        }
        int cost = CoeSkillSupport.cost(sword, config.energyCost,
                CoeSkillSupport.effectiveLevel(sword, instance));
        CoeSkillSupport.consume(player, sword, consumable, cost);
    }
}
