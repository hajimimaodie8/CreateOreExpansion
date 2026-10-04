package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.field.FieldChargeRuntime;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.ChargeDashConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.FallGuardConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.FieldChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.LastStandConfigs;

import net.minecraft.server.level.ServerPlayer;

/**
 * <b>装备技能的结算口径</b>（2026-10-05 行为零变化拆分，从 {@code ArmorSkillRuntime} 的
 * {@code release} / {@code accumulatedCost} / {@code cooldownSecondsOf} / {@code isExhausted}
 * <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这一段长按该收多少钱、该起几秒冷却、以及"能量见底"的判据
 * 是什么</b> —— 松手（{@link #release}）· 每 tick 的断停判定（{@link #isExhausted}）·
 * 累计应付（{@link #accumulatedCost}）· 该起的冷却秒数（{@link #cooldownSecondsOf}）四处
 * <b>共用同一批判据</b>，不在这里重算第二份。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、跨类引用改写成
 * {@code ArmorSkillRuntime.} / {@code ArmorSkillSlots.} 限定名。三条口径一个字未动：
 * ① 扣款比例 {@code floor(heldTicks × totalCost / (maxSeconds × 20))}（宿主上的
 * {@code holdCost}，公开出口没搬）；② 衡元择势按<b>长按开始时定下的分支</b>取表（这里问
 * {@code ArmorSkillRuntime#balanceBranchOf}）；③ 临域充力/星芒嬗震是<b>边按边扣</b>，
 * 见底判据比的是"下一步还扣得起吗"。<b>结算时机与顺序由调用方（tick 状态机）决定，
 * 本类只提供口径</b>。</p>
 */
final class ArmorSkillSettlement {

    private ArmorSkillSettlement() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 长按结算：按用户的比例式算钱、扣款、起冷却，并触发该槽位技能的效果收尾。
     *
     * @param heldTicks 实际按住的服务端 tick 数（不含松手那一 tick）
     */
    static void release(ServerPlayer player, @Nullable ArmorSet set, int index, int heldTicks) {
        if (set == null) {
            // 中途脱甲/换套 ⇒ 这个槽位解析不出技能 id 了。别的手段照旧（这一支以前就是直接返回），
            // 但**临域充力留下的会话必须收尾**：否则曲柄会一直转、注入器会一直留在世界里。
            FieldChargeRuntime.finish(player);
            return;
        }
        String skillId = ArmorSkillSlots.skillId(set, index);
        if (skillId == null) {
            FieldChargeRuntime.finish(player);
            return;
        }
        if (!ArmorSkillRuntime.FIELD_CHARGE.equals(skillId)) {
            // 长按期间换了套（槽位号相同、技能却不是临域充力了）：同样必须收尾。
            // 幂等：没有会话时什么都不做。
            FieldChargeRuntime.finish(player);
        }
        if (skillId.equals(ArmorSkillRuntime.FALL_GUARD)) {
            FallGuardConfigs.Config config = FallGuardConfigs.config(
                ArmorSkillRuntime.effectiveLevel(player, set, skillId));
            int cost = ArmorSkillRuntime.holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
            // 扣款走"四件平摊、全有或全无"
            ArmorEnergy.consume(player, cost);
            // 冷却：松手后开始计（记在玩家持久数据里）
            ArmorSkillRuntime.startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(ArmorSkillRuntime.CHARGE_DASH)) {
            // 蓄能疾骋：**迅捷在按住期间就已经逐段生效**（用户 2026-10-01 修正：
            // "按住 R 之后必须按完才有疾跑 buff，我想让它一边按一边产生"）。
            // 因此松手只结算能量与冷却，**不再补发效果** —— 否则松手等于白送一整段时长。
            ChargeDashConfigs.Config config = ChargeDashConfigs.config(
                ArmorSkillRuntime.effectiveLevel(player, set, skillId));
            ArmorEnergy.consume(player, ArmorSkillRuntime.holdCost(heldTicks, config.holdSeconds(),
                config.holdTotalCost()));
            ArmorSkillRuntime.startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(ArmorSkillRuntime.LAST_STAND)) {
            // 绝境守护（宝石套 · 槽位 1）：与蓄能疾骋同一条口径 —— 不死图腾 buff 在按住期间
            // 就按段位施加（见 applyLastStand），松手**只结算能量与冷却、不再补发**。
            // 补发会让"按住 1 tick 再松手"白拿一整段时长。
            LastStandConfigs.Config config = LastStandConfigs.config(
                ArmorSkillRuntime.effectiveLevel(player, set, skillId));
            ArmorEnergy.consume(player, ArmorSkillRuntime.holdCost(heldTicks, config.holdSeconds(),
                config.holdTotalCost()));
            ArmorSkillRuntime.startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(ArmorSkillRuntime.BALANCE_CHOICE)) {
            // 衡元择势（星界套 · 槽位 1）：与蓄能疾骋 / 绝境守护**同一条口径** —— buff 在按住期间
            // 就按段位施加（见 applyBalanceChoice），松手只结算能量与冷却、不再补发。
            // 结算用的表 = **这一次长按开始时定下的那个分支**那张表（需求 §3.2「耗能：走当前分支自己的表」），
            // 所以不能按「松手那一刻的血量」重算分支 —— 那会让玩家按住时看到 A 的 buff、却按 B 的表结账。
            int level = ArmorSkillRuntime.effectiveLevel(player, set, skillId);
            if (ArmorSkillRuntime.balanceBranchOf(player) == ArmorSkillRuntime.BalanceBranch.TOTEM) {
                LastStandConfigs.Config totem = LastStandConfigs.config(level);
                ArmorEnergy.consume(player, ArmorSkillRuntime.holdCost(heldTicks, totem.holdSeconds(),
                    totem.holdTotalCost()));
                ArmorSkillRuntime.startCooldown(player, skillId, totem.cooldownSeconds());
            } else {
                ChargeDashConfigs.Config dash = ChargeDashConfigs.config(level);
                ArmorEnergy.consume(player, ArmorSkillRuntime.holdCost(heldTicks, dash.holdSeconds(),
                    dash.holdTotalCost()));
                ArmorSkillRuntime.startCooldown(player, skillId, dash.cooldownSeconds());
            }
        } else if (skillId.equals(ArmorSkillRuntime.FIELD_CHARGE)) {            // 临域充力（宝石套 · 槽位 3，规格 §8 第 3 层）：用户明确"**中途松开即终止**供能"。
            // 能量是**边按边扣**的（见 FieldChargeRuntime#hold），所以松手只需补上最后不足一步的零头，
            // 随后收尾（移除注入器 + 曲柄立刻静止）并起冷却 25/20/15 秒。
            FieldChargeConfigs.Config config = FieldChargeConfigs.config(
                ArmorSkillRuntime.effectiveLevel(player, set, skillId));
            FieldChargeRuntime.release(player, heldTicks);
            ArmorSkillRuntime.startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(ArmorSkillRuntime.STAR_SHOCK)) {
            // 星芒嬗震（星界套 · 槽位 3）：波在按下那一 tick 就发出去了、长按期间边按边扣，
            // 所以松手只补上最后不足一步的零头 + 起冷却（10/7/4 秒）。**不补发、不召回**任何波。
            int level = ArmorSkillRuntime.effectiveLevel(player, set, skillId);
            StarShockRuntime.release(player, heldTicks);
            ArmorSkillRuntime.startCooldown(player, skillId, StarShockRuntime.cooldownSeconds(level));
        }
    }

    /** 该技能在该等级下、按住这么多 tick 时的累计花费（与松手结算同一个公式）。 */
    static int accumulatedCost(ServerPlayer player, ArmorSet set, int index, int heldTicks) {
        String skill = ArmorSkillSlots.skillId(set, index);
        int level = ArmorSkillRuntime.effectiveLevel(player, set, skill);
        if (ArmorSkillRuntime.FIELD_CHARGE.equals(skill)) {
            // 临域充力（宝石套槽位 3，规格 §8 第 3 层）：数值源 = FieldChargeConfigs 的"点/秒"，
            // 累计口径 = floor(heldTicks × energyPerSecond / 20)（见 FieldChargeConfigs#costAfter 的推导：
            // 与 holdCost(held, durationSeconds, durationSeconds × energyPerSecond) 逐值等价）。
            return FieldChargeConfigs.costAfter(heldTicks, FieldChargeConfigs.config(level));
        }
        if (ArmorSkillRuntime.FALL_GUARD.equals(skill)) {
            FallGuardConfigs.Config config = FallGuardConfigs.config(level);
            return ArmorSkillRuntime.holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
        }
        if (ArmorSkillRuntime.CHARGE_DASH.equals(skill)) {
            ChargeDashConfigs.Config config = ChargeDashConfigs.config(level);
            return ArmorSkillRuntime.holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
        }
        if (ArmorSkillRuntime.BALANCE_CHOICE.equals(skill)) {
            // 衡元择势：按**这一次长按开始时定下的分支**取表（与 release 同一处判据），
            // 否则"见底自动断停"会用错表算累计花费，提前或延后停下。
            if (ArmorSkillRuntime.balanceBranchOf(player) == ArmorSkillRuntime.BalanceBranch.TOTEM) {
                LastStandConfigs.Config totem = LastStandConfigs.config(level);
                return ArmorSkillRuntime.holdCost(heldTicks, totem.holdSeconds(), totem.holdTotalCost());
            }
            ChargeDashConfigs.Config dash = ChargeDashConfigs.config(level);
            return ArmorSkillRuntime.holdCost(heldTicks, dash.holdSeconds(), dash.holdTotalCost());
        }
        LastStandConfigs.Config config = LastStandConfigs.config(level);
        return ArmorSkillRuntime.holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
    }

    /** 该技能该起的冷却秒数（给"能量见底自动断停"用，与松手结算同一处取值）。 */
    static int cooldownSecondsOf(ServerPlayer player, ArmorSet set, int index) {
        String skill = ArmorSkillSlots.skillId(set, index);
        int level = ArmorSkillRuntime.effectiveLevel(player, set, skill);
        if (ArmorSkillRuntime.FIELD_CHARGE.equals(skill)) {
            // 临域充力（宝石套槽位 3，规格 §2.2"释放后冷却"）：25 / 20 / 15 秒
            return FieldChargeConfigs.config(level).cooldownSeconds();
        }
        if (ArmorSkillRuntime.FALL_GUARD.equals(skill)) {
            return FallGuardConfigs.config(level).cooldownSeconds();
        }
        if (ArmorSkillRuntime.CHARGE_DASH.equals(skill)) {
            return ChargeDashConfigs.config(level).cooldownSeconds();
        }
        if (ArmorSkillRuntime.BALANCE_CHOICE.equals(skill)) {
            // 衡元择势：冷却同样按"这一次长按开始时定下的那个分支"取（移速 60/45/30、图腾 30/30/30）
            return ArmorSkillRuntime.balanceBranchOf(player) == ArmorSkillRuntime.BalanceBranch.TOTEM
                ? LastStandConfigs.config(level).cooldownSeconds()
                : ChargeDashConfigs.config(level).cooldownSeconds();
        }
        return LastStandConfigs.config(level).cooldownSeconds();
    }

    /**
     * 累计花费是否已经<b>见底</b>（用户 2026-10-01："能量消耗到 100 的时候要自动断停，然后把能量全都清空"）。
     *
     * <p>判据 = 累计花费 ≥ 当前可用合计能量（等于 0 也视为见底）。</p>
     *
     * <p><b>临域充力是唯一的例外，它比的是"下一步"</b>：其它装备技能都是"松手时一次性按比例扣"，
     * 所以"累计应付 ≥ 可用"就是"付不起了"；而临域充力<b>边按边扣</b>
     * （见 {@code FieldChargeRuntime#hold}，每 tick 只扣增量）⇒ 累计应付那部分<b>已经扣掉了</b>，
     * 再拿它跟"剩下的可用能量"比会在能量还剩一半时就误停。所以它比的是
     * <b>下一 tick 的那一步（{@code FieldChargeConfigs#stepCost}）还扣得起吗</b>。</p>
     */
    static boolean isExhausted(ServerPlayer player, ArmorSet set, int index, int heldTicks) {
        int available = ArmorEnergy.totalEnergy(player);
        if (available <= 0) {
            return true;
        }
        String skill = ArmorSkillSlots.skillId(set, index);
        if (ArmorSkillRuntime.FIELD_CHARGE.equals(skill)) {
            FieldChargeConfigs.Config config =
                FieldChargeConfigs.config(ArmorSkillRuntime.effectiveLevel(player, set, skill));
            return FieldChargeConfigs.stepCost(heldTicks, config) > available;
        }
        if (ArmorSkillRuntime.STAR_SHOCK.equals(skill)) {
            // 星芒嬗震与临域充力同族：**边按边扣**，所以比的也是"下一步还扣得起吗"。
            // 点按那 400 是"按下那一 tick 就付掉了"的（不参与这次比较），所以这里比的就是
            // 下一 tick 的增量（100/20 = 每 20 tick 5 点）与当前可用能量的关系。
            int level = ArmorSkillRuntime.effectiveLevel(player, set, skill);
            int step = StarShockRuntime.holdCost(level, heldTicks + 1)
                - StarShockRuntime.holdCost(level, heldTicks);
            return step > available;
        }
        return accumulatedCost(player, set, index, heldTicks) >= available;
    }
}
