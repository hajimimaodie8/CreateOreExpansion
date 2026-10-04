package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.field.FieldChargeRuntime;
import com.leaf.skiller.server.PlayerPressedKeys;

import net.minecraft.server.level.ServerPlayer;

/**
 * <b>一个装备槽位在服务端 tick 上的状态迁移</b>（2026-10-05 行为零变化拆分，从
 * {@code ArmorSkillRuntime#tick(ServerPlayer)} 的那个 {@code for} 循环体<b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这个槽位此刻是"刚开始按 / 已经按了 N tick / 刚松手 / 没按"，
 * 该往哪边走</b>。四条出口全在这里，顺序与原来逐行一致：</p>
 * <ol>
 *   <li><b>刚开始按</b>（{@code previous == null}）：过"这一套在这个槽位有技能吗 → 冷却过了吗 →
 *       还有能量吗 → 临域充力的两道关 → 星芒嬗震的点按"这几道门；任何一道不过就<b>只提示、
 *       不进长按状态</b>（不扣能、不进冷却）—— 绝不静默无效；</li>
 *   <li><b>按住推进</b>：按技能 id 分派（{@code charge_dash} / {@code last_stand} /
 *       {@code balance_choice} / {@code star_shock} / {@code field_charge}），
 *       最后统一走一趟"见底即断停"；</li>
 *   <li><b>松手</b>（{@code previous != null}）：清理往返状态后交给
 *       {@link ArmorSkillSettlement#release}；</li>
 *   <li>没有任何变化就什么都不做。</li>
 * </ol>
 *
 * <p><b>搬运口径与省下的那一处改写</b>：循环体逐字相同，差异只有四类 —— ①
 * {@code private} → 包级私有、跨类引用改写成 {@code ArmorSkillRuntime.} /
 * {@code ArmorSkillSlots.} / {@code ArmorSkillBuffs.} / {@code ArmorSkillSettlement.} 限定名；
 * ② 整段缩进减 4 格（原来在 {@code for} 里，现在在方法体里）；③
 * {@code DASH_SEGMENT.remove(id); LAST_STAND_SEGMENT.remove(id);} 两行合并成
 * {@code ArmorSkillBuffs.clearSegments(id);}（同一处、同顺序的两个 map 删除）；
 * ④ <b>{@code continue} → {@code return}</b>（原 {@code for} 循环体里被搬走的就是整个循环体，
 * 循环体末尾没有别的语句，所以"跳到下一轮"与"从这个方法返回"逐字等价）。
 * <b>per-tick 的阶段顺序与结算汇合点一个字未动</b>：本方法在 {@code tick()} 里的调用点就是
 * 原来那个循环体的位置，一轮一次。</p>
 */
final class ArmorSkillHoldLoop {

    private ArmorSkillHoldLoop() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 推进<b>一个槽位</b>（{@code tick()} 的 {@code for} 循环体那一份）。
     *
     * @param held  宿主的长按计数表（玩家 → (槽位 → 已按住 tick)），与原来传的是同一张表
     * @param slot  {@code ArmorSkillProvider.SLOT_BASE + index}
     * @param index 0 起的槽位序号（技能 id 表的行号）
     */
    static void tickSlot(ServerPlayer player, @Nullable ArmorSet set, UUID id, Map<Integer, Integer> held,
                         int slot, int index) {
        boolean pressed = PlayerPressedKeys.isPressed(player, slot);
        Integer previous = held.get(slot);
        if (pressed) {
            // 开始条件：这一套生效、这一套在这个槽位真的有技能、冷却已过、还有能量
            if (previous == null) {
                if (set == null || !ArmorSkillSlots.hasSkill(set, index)) {
                    return;
                }
                String skill = ArmorSkillSlots.skillId(set, index);
                // 冷却中 / 没能量：**必须让玩家看得见原因**（用户 2026-10-01 报"有的时候按了根本
                // 不生效、松开也没给" —— 就是被这两条静默挡掉的，玩家只能感到"随机失效"）。
                if (!ArmorSkillRuntime.isReady(player, skill)) {
                    ArmorSkillRuntime.notifyBlocked(player, slot, "createoreexpansion.equip_skill.cooldown",
                        Math.max(1, ArmorSkillRuntime.cooldownLeft(player, skill) / 20));
                    return;
                }
                if (ArmorEnergy.totalEnergy(player) <= 0) {
                    ArmorSkillRuntime.notifyBlocked(player, slot, "createoreexpansion.equip_skill.no_energy", 0);
                    return;
                }
                // 临域充力（宝石套槽位 3）：发动前要过"§2.1 判定"（半径内至少一个动力源方块）
                // 与"注入器放得下吗"两道关；任何一道不过 ⇒ 按**发动失败**处理：
                // 不进长按状态（⇒ 不扣能、不进冷却），只提示原因。
                if (ArmorSkillRuntime.FIELD_CHARGE.equals(skill)) {
                    FieldChargeRuntime.StartResult started =
                        FieldChargeRuntime.start(player,
                            ArmorSkillRuntime.effectiveLevel(player, set, skill));
                    if (started != FieldChargeRuntime.StartResult.OK) {
                        ArmorSkillRuntime.notifyBlocked(player, slot,
                            started == FieldChargeRuntime.StartResult.NO_SOURCE
                                ? "createoreexpansion.equip_skill.field_charge_no_source"
                                : "createoreexpansion.equip_skill.field_charge_no_space", 0);
                        return;
                    }
                }
                // 衡元择势（星界套 · 槽位 1）：**分支只在这一 tick 取一次**（需求 §3.2 判定时机
                // "长按开始时取一次，期间不翻转"）。取到的分支决定这一整段长按走哪张表。
                if (ArmorSkillRuntime.BALANCE_CHOICE.equals(skill)) {
                    ArmorSkillRuntime.BALANCE_BRANCH.put(id, ArmorSkillRuntime.decideBalanceBranch(player));
                }
                // 星芒嬗震（星界套 · 槽位 3）：**按下那一 tick 就是点按** —— 立刻发第 1 枚主波
                // 并扣点按能量（需求 §3.3(b)"点按（轻触）发出 1 枚主波，不需要长按"）。
                // 点按能量付不出 ⇒ 按"发动失败"处理（只记日志，不进长按状态、不进冷却），
                // 与临域充力的两道关同一条纪律：**绝不静默无效**。
                if (ArmorSkillRuntime.STAR_SHOCK.equals(skill)) {
                    if (!StarShockRuntime.press(player, ArmorSkillRuntime.effectiveLevel(player, set, skill))) {
                        ArmorSkillRuntime.notifyBlocked(player, slot,
                            "createoreexpansion.equip_skill.no_energy", 0);
                        return;
                    }
                    ArmorSkillRuntime.STAR_SHOCK_HOLD.put(id, 0);
                }
                held.put(slot, 0);
                ArmorSkillRuntime.ACTIVE.put(id, skill);
            } else {
                int ticks = previous + 1;
                held.put(slot, ticks);
                String skill = ArmorSkillSlots.skillId(set, index);
                if (ArmorSkillRuntime.CHARGE_DASH.equals(skill)) {
                    // 用户 2026-10-01 修正：**一边按一边产生疾跑 buff**（不是松手才给）
                    ArmorSkillBuffs.applyChargeDash(player, set, ticks);
                } else if (ArmorSkillRuntime.LAST_STAND.equals(skill)) {
                    // 绝境守护：同样"一边按一边给"—— 段位推进时施加/升级不死图腾 buff
                    // （规格 §8 第 2 层"段位随长按推进"，松手不再补发，理由同蓄能疾骋）
                    ArmorSkillBuffs.applyLastStand(player, set, ticks);
                } else if (ArmorSkillRuntime.BALANCE_CHOICE.equals(skill)) {
                    // 衡元择势（星界套 · 槽位 1）：按**开始时定下的那个分支**跑 ——
                    // 移速分支完全等同蓄能疾骋、图腾分支完全等同绝境守护（同一套数值表）。
                    // 分支在 BALANCE_BRANCH 里定死，这里不重算（否则按住期间会来回切）。
                    ArmorSkillBuffs.applyBalanceChoice(player, set, ticks);
                } else if (ArmorSkillRuntime.STAR_SHOCK.equals(skill)) {
                    // 星芒嬗震（星界套 · 槽位 3）：点按那一 tick 已经发过第 1 枚、扣过点按能量；
                    // 这里负责长按蓄力（补发主波 + 掷环绕概率）与按 tick 折算的持续耗能。
                    // 返回 false = 能量见底、本次发射已被收尾 ⇒ 与其它技能同一条"见底即断停"路径。
                    ArmorSkillRuntime.STAR_SHOCK_HOLD.put(id, ticks);
                    if (!StarShockRuntime.hold(player, ticks)) {
                        ArmorSkillRuntime.STAR_SHOCK_HOLD.remove(id);
                    }
                } else if (ArmorSkillRuntime.FIELD_CHARGE.equals(skill)) {
                    // 临域充力（规格 §8 第 3 层）：每 tick 续期"曲柄在转 + 注入器在 + 容量挂在网上"，
                    // 并按"点/秒"累计扣能。非 ACTIVE = 这一 tick 已经收尾（到限 / 见底 / 失效）。
                    FieldChargeRuntime.HoldResult result = FieldChargeRuntime.hold(player, ticks);
                    if (result != FieldChargeRuntime.HoldResult.ACTIVE) {
                        held.remove(slot);
                        ArmorSkillRuntime.ACTIVE.remove(id);
                        if (result == FieldChargeRuntime.HoldResult.ENERGY_OUT) {
                            // 与"能量见底自动断停"同一条口径：见底即把剩余能量清空
                            ArmorEnergy.consume(player, ArmorEnergy.totalEnergy(player));
                        }
                        ArmorSkillRuntime.startCooldown(player, skill,
                            ArmorSkillSettlement.cooldownSecondsOf(player, set, index));
                        return;
                    }
                } else if (skill == null) {
                    // 中途脱甲/换套 ⇒ 这个槽位解析不出技能了。**别的手段都不管，
                    // 但临域充力留下的会话必须收尾**（否则曲柄永远在转、注入器永远在世界里）。
                    FieldChargeRuntime.finish(player);
                }
                // 用户 2026-10-01 口径：能量消耗到"见底"⇒ 自动断停 + 把能量清空。
                // 临域充力（宝石套槽位 3）从第 3 层起也走这条分支（它的"见底"判据见 isExhausted：
                // 它是**边按边扣**，所以判的是"下一步还扣得起吗"）。
                if (set != null && skill != null
                        && ArmorSkillSettlement.isExhausted(player, set, index, ticks)) {
                    held.remove(slot);
                    ArmorSkillRuntime.ACTIVE.remove(id);
                    ArmorSkillBuffs.clearSegments(id);
                    ArmorSkillRuntime.BALANCE_BRANCH.remove(id);
                    // 星芒嬗震：见底即断停 —— 按下那一 tick 已扣的点按能量**不退**，
                    // 后续的零头也不再追扣（与其它技能"见底即清空"同一条口径）。
                    if (ArmorSkillRuntime.STAR_SHOCK.equals(skill)) {
                        StarShockRuntime.abandon(player);
                        ArmorSkillRuntime.STAR_SHOCK_HOLD.remove(id);
                    }
                    if (ArmorSkillRuntime.FIELD_CHARGE.equals(skill)) {
                        FieldChargeRuntime.finish(player); // 能量见底也要把注入器与曲柄收干净
                    }
                    ArmorEnergy.consume(player, ArmorEnergy.totalEnergy(player)); // 见底即清空
                    ArmorSkillRuntime.startCooldown(player, skill,
                        ArmorSkillSettlement.cooldownSecondsOf(player, set, index));
                    return;
                }
            }
        } else if (previous != null) {
            held.remove(slot);
            ArmorSkillRuntime.ACTIVE.remove(id);
            ArmorSkillBuffs.clearSegments(id);
            ArmorSkillRuntime.BALANCE_BRANCH.remove(id);
            ArmorSkillRuntime.STAR_SHOCK_HOLD.remove(id);
            ArmorSkillSettlement.release(player, set, index, previous);
        }
    }
}
