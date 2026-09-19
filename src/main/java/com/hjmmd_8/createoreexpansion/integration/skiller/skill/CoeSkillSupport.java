package com.hjmmd_8.createoreexpansion.integration.skiller.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.SkillEnergyCost;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.leaf.skiller.foundation.Consumable;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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
}
