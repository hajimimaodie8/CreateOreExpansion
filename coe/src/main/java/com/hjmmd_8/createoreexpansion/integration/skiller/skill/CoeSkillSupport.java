package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
import com.hjmmd_8.createoreexpansion.common.SkillCooldowns;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.ExcavationSkillContext;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.context.SkillContext;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 新内核（Skiller）技能实现的共用支撑：等级、配置、能量消耗三条口径都收在这里，
 * 避免每个迁移过来的技能各写一遍（写歪一处就是玩法差异）。
 *
 * <p>三条口径都<b>照旧框架的实现抄</b>，不是重新设计：</p>
 * <ul>
 *     <li><b>有效等级</b> = {@code min(基础等级 + 技艺提升 - 技艺回溯, 满级)}，不低于 1
 *         （旧 {@code SkillEnergyCost.effectiveLevel}）；</li>
 *     <li><b>配置按有效等级取</b> = 旧 {@code SkillsComponent.applySkillBoost} 的做法：
 *         附魔提升等级后，配置（范围/消耗等）也换成该等级的（例如技艺提升让范围变大）；</li>
 *     <li><b>消耗</b> = 一级消耗 × 有效等级 × 减耗附魔折扣（旧 {@code SkillEnergyCost.compute}）。</li>
 * </ul>
 *
 * <p>另外提供 {@link #consume(Player, ItemStack, Consumable, int)}：新内核的资源不足时是
 * <b>静默失败</b>，而旧路径会发低能量提示，这里把提示补回来（详见方法 javadoc）。</p>
 *
 * <p>还有 {@link #cooldownTicks(ItemStack, int)}：把"技能自己的秒数 / 显式无冷却 / 没配回落物品注册表"
 * 三态分开（2026-10-06 批 14 修掉"配了 0 会悄悄变成 1 秒"那个坑，见那个方法的 javadoc）。</p>
 *
 * @since 1.0.0
 */
public final class CoeSkillSupport {

    /** 查不到注册信息时的满级兜底（与旧 {@code SkillEnergyCost} 的默认值一致） */
    private static final int DEFAULT_MAX_LEVEL = 5;

    private CoeSkillSupport() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 该技能在本模组旧注册表里的满级；查不到时用 5（旧默认）。 */
    public static int maxLevel(@Nullable ResourceLocation skillId) {
        AllSkills.RegisteredDataSkill registered = skillId == null ? null : AllSkills.getData(skillId);
        return registered == null ? DEFAULT_MAX_LEVEL : registered.maxLevel();
    }

    /**
     * 该技能实例在给定工具上的<b>有效等级</b>（含技艺提升/回溯附魔，受满级限制）。
     *
     * @param stack    手持工具
     * @param instance 技能实例（基础等级取 {@code instance.level()}，即物品 NBT 的 {@code Level}）
     */
    public static int effectiveLevel(ItemStack stack, ISkillInstance<?> instance) {
        return SkillEnergyCost.effectiveLevel(stack, instance.level(), maxLevel(skillIdOf(instance)));
    }

    /**
     * 按<b>有效等级</b>取该技能注册的等级配置（等价旧 {@code applySkillBoost} 的效果）。
     *
     * @param stack    手持工具
     * @param instance 技能实例
     * @param type     期望的配置类型（与技能注册时 {@code config()} 的类型一致）
     * @return 该等级的配置；技能没注册/类型不符时返回 null
     */
    @Nullable
    public static <C extends SkillConfig> C configForLevel(ItemStack stack, ISkillInstance<?> instance, Class<C> type) {
        ResourceLocation skillId = skillIdOf(instance);
        AllSkills.RegisteredDataSkill registered = skillId == null ? null : AllSkills.getData(skillId);
        if (registered == null) {
            return null;
        }
        int effective = SkillEnergyCost.effectiveLevel(stack, instance.level(), registered.maxLevel());
        SkillConfig config = registered.configForLevel(effective);
        return type.isInstance(config) ? type.cast(config) : null;
    }

    /** 一次释放的实际消耗（含减耗附魔）。 */
    public static int cost(ItemStack stack, int baseCost, int effectiveLevel) {
        return SkillEnergyCost.compute(stack, baseCost, effectiveLevel);
    }

    /**
     * 按<b>调用方自己算好的等级</b>取该技能注册的等级配置（2026-10-03 弓技能批 1 新增）。
     *
     * <p>存在的理由：等级有<b>两个</b>合法来源 —— 内核口径（{@code instance.level()} 经注册表的
     * {@code maxLevel} 钳位，见 {@link #configForLevel(ItemStack, ISkillInstance, Class)}）与
     * <b>物品自己的口径</b>（弓：基准/上限都取档位表，读取点唯一
     * {@code JadeTopazBowItem#effectiveSkillLevel}）。后者的调用方已经有等级了，
     * 再让它按内核口径重算一遍就会"取配置用 5、扣费用 3"这类静默偏差。本重载只做
     * 「id → 注册条目 → 该等级配置 → 类型校验」，不参与任何等级计算。</p>
     *
     * @param skillId 技能注册 id（null 或未注册时返回 null）
     * @param level   调用方算好的等级（1 起；本方法不做钳位，钳位是等级读取点的事）
     * @param type    期望的配置类型（与技能注册时 {@code config()} 的类型一致）
     * @return 该等级的配置；技能没注册/类型不符时返回 null
     */
    @Nullable
    public static <C extends SkillConfig> C configForLevel(@Nullable ResourceLocation skillId, int level, Class<C> type) {
        AllSkills.RegisteredDataSkill registered = skillId == null ? null : AllSkills.getData(skillId);
        if (registered == null) {
            return null;
        }
        SkillConfig config = registered.configForLevel(level);
        return type.isInstance(config) ? type.cast(config) : null;
    }

    /**
     * 统一的资源消耗入口（技能在 {@code consumeResource} 里调）。
     *
     * <p><b>为什么要在这里发提示</b>：新内核的流程是「所有实例先
     * {@code consumable.consume(cost)} 累加 → {@code canConsume()} 不过就整体放弃」，
     * 途中<b>不会</b>给玩家任何反馈；旧路径则会在能量不足时发一条低能量提示。
     * 所以这里在不足时补一次 {@link ToolEnergy#sendLowEnergy}，然后<b>照常累加</b>——
     * 累加才会让内核的 {@code canConsume} 失败而整体放弃（{@code DelayConsumable} 只在
     * 校验通过后才 {@code apply()} 真正扣账，所以"不够也累加"不会误扣）。</p>
     *
     * @param player     释放者（可为 null，为 null 时只累加不提示）
     * @param stack      主手工具
     * @param consumable 内核给的累加器
     * @param cost       本次消耗（&lt;= 0 表示不耗资源）
     */
    public static void consume(@Nullable Player player, ItemStack stack, Consumable consumable, int cost) {
        if (cost <= 0) {
            return;
        }
        if (player != null && !ToolEnergy.canAfford(player, stack, cost)) {
            ToolEnergy.sendLowEnergy(player, stack);
        }
        consumable.consume(cost);
    }

    /** 技能实例对应的注册 id（取不到时返回 null）。 */
    @Nullable
    public static ResourceLocation skillIdOf(ISkillInstance<?> instance) {
        return instance == null || instance.skill() == null ? null : instance.skill().getId();
    }

    /**
     * 「<b>本条技能自己没配冷却</b>」的哨兵（2026-10-06 批 14 引入）。
     *
     * <p>见 {@link #cooldownTicks(ItemStack, int)} 的三态语义 —— 负数 = 没配，只有这一条路会去问
     * 物品的按物品冷却注册表。</p>
     */
    public static final int NO_SKILL_COOLDOWN = -1;

    /**
     * 本次释放的冷却时长（tick）—— <b>三态语义</b>（2026-10-06 批 14 厘清）。
     *
     * <h2>三态（为什么不是"小于等于 0 就回落"）</h2>
     * <ol>
     *   <li><b>{@code cooldownSeconds > 0}</b> ⇒ {@code 秒数 × 20} tick。四条调用方
     *       （剥取 / 夺取 / 弓那两条 / 三条弓专属）今天<b>全部</b>走这一支
     *       （3~12 秒，一个 0 都没有）⇒ <b>本批行为零变化</b>；</li>
     *   <li><b>{@code cooldownSeconds == 0}</b> ⇒ <b>0 tick = 显式"无冷却"</b>，调用方不写冷却。
     *       ⚠ 这正是批 14 修的坑：批 1~13 的判据是 {@code <= 0}，于是"把配置改成 0 想表达无冷却"
     *       会掉进下面的注册表分支，而物品未注册时 {@code SkillCooldowns} 返回它的
     *       {@code DEFAULT_COOLDOWN_TICKS = 20} ⇒ <b>悄悄变成 1 秒冷却</b>（想表达"无"却得到一个值）；</li>
     *   <li><b>{@code cooldownSeconds < 0}</b>（约定值 = {@link #NO_SKILL_COOLDOWN}）⇒
     *       <b>本条技能没配冷却</b> ⇒ 回落物品注册表 {@link SkillCooldowns#getTicks(ItemStack)}
     *       —— 全模组唯一写点是 {@code CoeItems} 翠玉之弓那行 {@code .skillCooldown(5 * 20)}
     *       （= 100 tick = 5 秒，红线行，批 14 未动）；物品也没注册时由 core 自己声明的
     *       {@code SkillCooldowns.DEFAULT_COOLDOWN_TICKS}（20 tick）兜底 —— 那是 core 的默认值，
     *       <b>不是本方法"悄悄"产生的</b>（第 ② 条已把"配了 0"从这里摘出去）。</li>
     * </ol>
     * <p>⚠ 为什么保留"回落注册表"这条支（而不是删掉那处注册）：那处注册落在翠玉之弓的注册链上、
     * 是关卡逐字钉住的<b>红线行</b>，而 {@code SkillCooldowns} 住在共享库 {@code core}
     * （本批不许动）；删掉注册只会让 core 那张表<b>一个读者一个写者都不剩</b>（死代码从一处挪到
     * 一处、还动了红线）。批 14 的选择是<b>让注册值重新有意义（有真实读者）</b>，并把
     * "没配"与"配了 0"两种意图分开表达。</p>
     *
     * <p>⚠ 本批<b>没有</b>给开岩 / 引渠 / 平场 / 耕作加冷却（作者明确"没必要"）：它们的配置里
     * 不出现冷却字段，也就不会调到本方法。</p>
     *
     * @param stack           手持工具（第 ③ 支只在物品注册表里查它）
     * @param cooldownSeconds 该技能<b>自己</b>的冷却秒数：正 = 用它；0 = 显式无冷却；
     *                        负（{@link #NO_SKILL_COOLDOWN}）= 没配，回落物品注册表
     */
    public static int cooldownTicks(ItemStack stack, int cooldownSeconds) {
        if (cooldownSeconds > 0) {
            return cooldownSeconds * 20;
        }
        if (cooldownSeconds == 0) {
            return 0;
        }
        return SkillCooldowns.getTicks(stack);
    }

    /**
     * 是否正在冷却中（创造模式恒 false，与旧实现一致）。
     *
     * <p>注意旧实现的**判定**与**时长**是分开的：判定恒为 {@code ToolSkillCooldown.isReady}，
     * 只有"进入冷却时写多少 tick"才分自冷却/通用冷却两种来源，所以这里也只判 isReady。</p>
     */
    public static boolean onCooldown(@Nullable Player player, ItemStack stack) {
        return player != null && !player.isCreative() && !ToolSkillCooldown.isReady(player, stack);
    }

    /**
     * <b>这次释放真的会有产出吗</b>：策略允许收集、且收集结果非空。
     *
     * <p>存在的理由：旧框架的扣能时机是「策略算出非空集合之后」（见旧
     * {@code AoeExcavationSkill#causeAoe} —— {@code toDestroy.isEmpty()} 就直接 return，
     * 根本不会走到 {@code ToolEnergy.tryConsume}）。而新内核是<b>先</b>
     * {@code consumeResource} <b>再</b> {@code release}，若在 {@code consumeResource} 里
     * 无条件扣能，"砍一块孤零零的原木（连锁结果为空）"这类场景就会白掉一份能量。</p>
     *
     * <p>代价是策略会算两遍（这里一遍、{@code release} 里一遍）。相对"最多破坏上百个方块 +
     * 掉落物"的开销，一遍有界搜索可以忽略；换来的是与旧实现逐字一致的扣能时机。</p>
     *
     * @return true = 这次释放会产生实际效果（该扣能）
     */
    public static boolean willDoWork(ExcavationSkillContext context,
                                     ISkillInstance<ExcavationSkillContext> instance,
                                     @Nullable SkillStrategy<BlockPos, ExcavationSkillContext> strategy) {
        return willDoWork(context, instance, strategy, new HashSet<>());
    }

    /**
     * 通用版 {@link #willDoWork(ExcavationSkillContext, ISkillInstance, SkillStrategy)}：
     * 收集元素类型与上下文类型都由调用方决定（受击系技能收集的是实体，不是方块）。
     *
     * @param scratch 调用方提供的临时集合（内容会被清空/填充，容量无所谓）
     */
    public static <T, C extends SkillContext> boolean willDoWork(C context, ISkillInstance<C> instance,
                                                                 @Nullable SkillStrategy<T, C> strategy,
                                                                 Set<T> scratch) {
        if (strategy == null || context == null || !strategy.canCollect(context, instance)) {
            return false;
        }
        scratch.clear();
        strategy.collect(scratch, context, instance);
        return !scratch.isEmpty();
    }
}
