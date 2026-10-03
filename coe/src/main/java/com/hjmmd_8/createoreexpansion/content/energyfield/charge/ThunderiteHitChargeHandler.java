package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.SeriesTraits;
import com.hjmmd_8.createoreexpansion.content.energyfield.ChargeApi;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillEnergySpend;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * <b>获得途径 #2：被「雷鸣合金系列的特殊武器技能」击中 ⇒ 随机染上一种电荷</b>
 * （coe-charge 批 5；需求 §3.2 表格第 2 行）。
 *
 * <h2>这一条为什么是「最小集合」</h2>
 * <p>作者只举了两个例子——本模组的<b>雷鸣合金</b>与 BGE 的<b>结雷金剑</b>——没有给调用点清单。
 * BGE 是别的模组、本仓<b>不许</b>硬编码它的类（需求 §3.2 备注 + §四），它只能通过
 * {@link ChargeApi} 自己来施加；所以本仓能落地的那一半 = <b>本模组自己的雷鸣合金系列</b>，
 * 判据用现成的 {@link SeriesTraits#isThunderite(ItemStack)}（物品标签 ∪ 方块标签 ∪ 注册名约定，
 * 与凝能佩/工具/护甲/方块物品同一处口径，不另写第二套字符串匹配）。</p>
 *
 * <h2>等级怎么取（★ 需求留白，本批定的口径，依据在下面）</h2>
 * <p>需求原话是「等级 = <b>该技能的等级</b>」，而雷鸣合金剑身上<b>同时挂着两个攻击类技能</b>
 * （{@code CoeItems.THUNDERITE_SWORD}：{@code AllSkills.SKIN} 基准 5 + {@code AllSkills.PLUNDER}
 * 基准 3）⇒「该技能」在这件物品上是一对多。<b>本批取其中等级最高者</b>
 * （{@link #highestHitSkillLevel(ItemStack)}），依据有二：</p>
 * <ol>
 *   <li>「这把武器的技能等级」在直觉上就是它最强的那个技能档位；取最高 ⇒ 一把 Lv5 剑不会因为
 *       另一个技能是 Lv3 而被判成 Lv3；</li>
 *   <li>等级无论怎么取都要过 {@link ChargeConfigs#clampLevel}（上限
 *       {@link ChargeConfigs#MAX_LEVEL}）⇒ 不同取法的差别只在 1..上限之间，取最高者是最保守的
 *       「按最强技能算」。</li>
 * </ol>
 * <p>每个技能的等级走 {@link SkillEnergySpend#effectiveLevel(ItemStack, DataSkill)}——本仓
 * 「技能有效等级」的<b>唯一</b>口径（基准等级 + 技艺提升 − 技艺回溯，再夹到技能满级），
 * 也就是 tooltip 上显示的那个数 ⇒ 玩家看到的 Lv 与实际染电的 Lv 永远一致。</p>
 *
 * <h2>「必须真的挂了攻击类技能」才算这一条</h2>
 * <p>需求说的是「被特殊武器<b>技能</b>击中」⇒ 只拿雷鸣合金系列里<b>带攻击类技能</b>的那些物品
 * 当判据（今天恰好是雷鸣合金剑；锭/碎块/板/杆/线/机壳这类材料与没有技能的工具即使属于系列，
 * 也不走这一条——它们没有「技能等级」可取）。物品上没有攻击类技能时
 * {@link #highestHitSkillLevel(ItemStack)} 返回 {@link #NO_HIT_SKILL}，调用点直接返回。</p>
 *
 * <h2>没做的两件事（刻意，见汇报的「待作者点名的调用点」一节）</h2>
 * <ul>
 *   <li><b>别的模组的雷系武器/法术</b>：BGE 结雷金剑、铁魔法那类雷法术书 —— 本仓不硬编码，
 *       它们自己调 {@link ChargeApi#apply} / {@link ChargeApi#applyRandom} 即可；</li>
 *   <li><b>投掷物与间接伤害</b>：只认「活着的攻击者<b>直接</b>打中」
 *       （{@code getDirectEntity()} 是 {@link LivingEntity}），
 *       因为「被雷鸣合金的<b>近战</b>技能击中」是这一条的字面口径；弓箭/法术弹等间接路径
 *       没有「手上那件雷鸣合金武器」，等作者点名要哪些再加。</li>
 * </ul>
 *
 * <h2>限频</h2>
 * <p>不需要额外计时器：{@link LivingIncomingDamageEvent} 每次<b>真的打到</b>只发一次
 * （它在原版无敌帧与免疫判定之后、结算之前发出），所以「同一次命中只施加一次」由事件本身保证；
 * 连击是多次命中、自然多次施加（每次都会算出同一个技能等级 ⇒ 同极合并不刷新缩短）。</p>
 *
 * <p><b>数字</b>：本文件一个数字字符都不写（连日志文本里的编号都不写）——等级上下限、时长、
 * 「没有技能」的哨兵值全部从 {@link ChargeConfigs} 读或推（关卡 {@code charge-route-*} 守着）。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public final class ThunderiteHitChargeHandler {

    /**
     * 「这件物品上没有攻击类技能」的哨兵值 —— <b>从数值真源推</b>，不写字面量
     * （{@code amplifierFor(MIN_LEVEL)} 就是 0，与 {@code ChargeApi#levelOf} 的「无电荷」哨兵
     * 同一口径：等级从 {@link ChargeConfigs#MIN_LEVEL} 起算，0 不可能是一个真等级）。
     */
    private static final int NO_HIT_SKILL = ChargeConfigs.amplifierFor(ChargeConfigs.MIN_LEVEL);

    private ThunderiteHitChargeHandler() {
    }

    /**
     * 生物挨打时的唯一入口：<b>攻击者手上那件物品属于雷鸣合金系列、且带着攻击类技能</b>
     * ⇒ 按该技能的有效等级给<b>被打中的生物</b>施加一次<b>随机极性</b>的电荷。
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) {
            return; // 别处已经把这笔伤害取消了 ⇒ 这一击没有真的打到，不施加
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return; // 服务端权威
        }
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) {
            return; // 只有"活着的攻击者直接打中"：弹射物 / 环境伤害不在这条途径里
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!SeriesTraits.isThunderite(weapon)) {
            return; // 不是本模组雷鸣合金系列（BGE 那类别的模组走 ChargeApi 自己施加）
        }
        int level = highestHitSkillLevel(weapon);
        if (level < ChargeConfigs.MIN_LEVEL) {
            return; // 系列材料/无技能工具：没有"该技能的等级"可取 ⇒ 这一条不成立
        }
        ChargeApi.applyRandom(target, level, ChargeConfigs.durationTicks(level));
        CoeCore.LOGGER.info("[电荷获得] 雷鸣合金系列武器技能命中：{} 用 {}（技能等级 Lv{}）打中 {}，已随机染上一种电荷",
            attacker, weapon.getHoverName(), level, target);
    }

    /**
     * 这件物品上<b>攻击类（{@code HIT_SKILL}）技能的最高有效等级</b>；没有攻击类技能时
     * 返回 {@link #NO_HIT_SKILL}。
     *
     * <p>没有技能组件（材料、机壳、没有技能的工具）⇒ {@link SkillItemStack#hasSkill()} 为 false，
     * 直接返回哨兵值，不碰 {@code getSkillsHolder()}（那里对空组件返回 null）。</p>
     */
    private static int highestHitSkillLevel(ItemStack stack) {
        SkillItemStack skillStack = SkillItemStack.of(stack);
        if (!skillStack.hasSkill(SkillType.HIT_SKILL)) {
            return NO_HIT_SKILL;
        }
        int best = NO_HIT_SKILL;
        for (DataSkill data : skillStack.getSkillsHolder().getDataSkills(SkillType.HIT_SKILL)) {
            best = Math.max(best, SkillEnergySpend.effectiveLevel(stack, data));
        }
        return best;
    }
}
