package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.handler.LastStandHandler;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.ChargeDashConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.LastStandConfigs;

import net.minecraft.server.level.ServerPlayer;

/**
 * <b>装备技能"按住期间"的段位 buff 与迅捷拖尾</b>（2026-10-05 行为零变化拆分，从
 * {@code ArmorSkillRuntime} 的 {@code applyChargeDash} / {@code applyLastStand} /
 * {@code applyBalanceChoice} / {@code applyTotemBranch} 与 {@code tick()} 尾部的拖尾推进
 * <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>长按推进到第几段时，往玩家身上施加/升级什么效果，
 * 以及"松手之后仍然存续的拖尾"要记多久、按哪一套上色</b>。技能的判定、扣能与结算时机
 * 一律不在这里（那是 {@link ArmorSkillRuntime} 与 {@link ArmorSkillSettlement} 的事）。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、宿主里的
 * {@code effectiveLevel} / {@code balanceBranchOf} / {@code BALANCE_BRANCH} / 技能 id 常量
 * 改写成 {@code ArmorSkillRuntime.} 限定名。三条口径一个字未动：① "只在段位变化时重新施加"
 * （每 tick 重置时长等于无限续杯）；② 拖尾的套是<b>登记那一刻</b>定死的，不是发射时现取；
 * ③ 图腾分支与绝境守护<b>共用一张段位表</b>（同一时刻只可能有一条长按在跑）。</p>
 */
final class ArmorSkillBuffs {

    private ArmorSkillBuffs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * <b>蓄能疾骋拖尾的存续状态</b>：{@code until} = 迅捷 buff 的到期时刻（gameTime，拖尾跟着它走），
     * {@code set} = <b>登记这一刻</b>玩家生效的那一套。
     *
     * <p><b>为什么把"套"和"到期时刻"存成一对</b>：拖尾在<b>松手之后仍会持续</b>（迅捷 buff 最多还有
     * 120 秒），而那段时间玩家完全可能换甲（脱一件、换一套、混搭）。如果拖尾颜色每 tick 现取
     * {@code ArmorSet.effectiveSet(player)}，换甲那一刻颜色就会当场跳变（宝石蓝红 ↔ 翠玉黄绿），
     * 与"这次技能是哪一套放的"也不符。所以套在<b>段位推进的那一刻</b>随到期时刻一起定死，
     * 松手后按记住的那一套上色（见 {@code tick} 里的发射点）。</p>
     *
     * @param until 迅捷 buff 到期时刻（{@code level().getGameTime()}，与旧 {@code DASH_UNTIL} 同一口径）
     * @param set   登记这一刻生效的那一套（调用点已保证非空：解析出 {@code charge_dash} 才有登记）
     */
    private record DashTrail(long until, ArmorSet set) {
    }

    /**
     * 玩家 → 蓄能疾骋拖尾状态（套与到期时刻<b>成对</b>存，理由见 {@link DashTrail}）。
     *
     * <p>生命周期与旧的"只有到期时刻"那张表一致：登记发生在段位推进时，
     * <b>松手不清</b>（拖尾要覆盖整个迅捷 buff），到期那一 tick 自动移除。</p>
     */
    private static final Map<UUID, DashTrail> DASH_TRAIL = new HashMap<>();

    /** 蓄能疾骋的"当前段位"（只在段位往上爬时重新施加效果，避免每 tick 重置时长）。 */
    private static final Map<UUID, Integer> DASH_SEGMENT = new HashMap<>();

    /**
     * 绝境守护 / 衡元择势（图腾分支）的"当前段位"（只在段位往上爬时重新施加 buff，
     * 避免每 tick 重置时长）。
     *
     * <p>两个技能<b>共用这一张段位表</b>：他们的段位都来自 {@link LastStandConfigs} 的
     * {@code segmentOf}，而同一时刻只可能有一条长按在跑（{@code ACTIVE} 是玩家 → 单值技能），
     * 所以不需要按技能再分一张表。</p>
     */
    private static final Map<UUID, Integer> LAST_STAND_SEGMENT = new HashMap<>();

    /**
     * 按住期间施加/升级<b>迅捷</b>（用户口径：一边按一边产生）。
     *
     * <p>只在<b>段位变化</b>时重新施加：每 tick 重置时长会让"松手后剩余时间"永远等于整段，
     * 那等于无限续杯。</p>
     */
    static void applyChargeDash(ServerPlayer player, ArmorSet set, int heldTicks) {
        ChargeDashConfigs.Config config =
            ChargeDashConfigs.config(ArmorSkillRuntime.effectiveLevel(player, set, ArmorSkillRuntime.CHARGE_DASH));
        int segment = ChargeDashConfigs.segmentOf(heldTicks, config);
        Integer last = DASH_SEGMENT.get(player.getUUID());
        if (last != null && last == segment) {
            return;
        }
        DASH_SEGMENT.put(player.getUUID(), segment);
        int seconds = config.segmentSeconds()[segment - 1];
        if (seconds > 0) {
            // 登记"拖尾存续到什么时候"：拖尾要覆盖整个迅捷 buff，而不只是按住的那几 tick。
            // 同时把**这一刻生效的那一套**记住（DashTrail）：松手后拖尾按它上色，换甲不变色。
            long now = player.level().getGameTime();
            long until = now + seconds * 20L;
            DashTrail registered = DASH_TRAIL.get(player.getUUID());
            if (registered == null || registered.until() <= until) {
                // 与旧口径一致地取"最长的那个到期时刻"（段位越高给得越久 ⇒ 真的会往后延）；
                // 谁给出更长的存续，就按谁的套上色。
                DASH_TRAIL.put(player.getUUID(), new DashTrail(until, set));
            }
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                seconds * 20, segment - 1, false, true, true));
        }
    }

    /**
     * <b>衡元择势 · 按住期间</b>（需求 §3.2）：按 <b>长按开始时定下的那一个分支</b>施加强度递增的
     * buff —— 移速分支 = 蓄能疾骋、图腾分支 = 绝境守护，<b>两张既有配置表各取一份</b>
     * （同级对齐，不做 1+1 那种相加 —— 那会突破 3 级封顶）。
     *
     * <p>副作用刻意做到最小：移速那一支完全复用 {@link #applyChargeDash}（含"拖尾跟着迅捷 buff 的
     * 存续期"这条口径），图腾那一支完全复用 {@link #applyLastStand}。两条路径的段位去重靠的是
     * 它们各自的段位表 {@code DASH_SEGMENT}／{@code LAST_STAND_SEGMENT}，与"是哪一套的技能"无关，
     * 所以这里<b>不必</b>再维护第三张段位表（同一时刻只可能有一条长按在跑：ACTIVE 是单值）。</p>
     *
     * <p>⚠ 只有一个例外：{@link #applyChargeDash} 会把"拖尾属于哪一套"记进 {@code DASH_TRAIL}。
     * 那里传的必须是<b>这次技能自己那一套</b>（星界套），否则星界套的拖尾会上成宝石/翠玉的颜色。</p>
     */
    static void applyBalanceChoice(ServerPlayer player, ArmorSet set, int heldTicks) {
        ArmorSkillRuntime.BalanceBranch branch = ArmorSkillRuntime.BALANCE_BRANCH.get(player.getUUID());
        if (branch == null) {
            // 理论上不会：分支在"开始长按"那一 tick 就写好了。真丢了就按移速分支兜底
            // （不静默什么都不做 —— 那会让玩家看到"按住没反应"）。
            branch = ArmorSkillRuntime.BalanceBranch.SPEED;
        }
        if (branch == ArmorSkillRuntime.BalanceBranch.TOTEM) {
            applyTotemBranch(player, set, heldTicks);
        } else {
            applyChargeDash(player, set, heldTicks);
        }
    }

    /**
     * 图腾分支（= 绝境守护的主动）按 <b>绝境守护的等级</b>取段位与 buff —— 与宝石套那条
     * 唯一的区别是"用哪张表的等级"。衡元择势的等级来自星界套（基准 2），
     * 而绝境守护的段数/时长表是同一张 {@link LastStandConfigs}。
     */
    static void applyTotemBranch(ServerPlayer player, ArmorSet set, int heldTicks) {
        int level = ArmorSkillRuntime.effectiveLevel(player, set, ArmorSkillRuntime.BALANCE_CHOICE);
        LastStandConfigs.Config config = LastStandConfigs.config(level);
        int segment = LastStandConfigs.segmentOf(heldTicks, config);
        Integer last = LAST_STAND_SEGMENT.get(player.getUUID());
        if (last != null && last == segment) {
            if (player.tickCount % 2 == 0) {
                ArmorSkillFx.lastStandAura(player, segment);
            }
            return;
        }
        LAST_STAND_SEGMENT.put(player.getUUID(), segment);
        LastStandHandler.applyTotemEffects(player, segment, level);
        ArmorSkillFx.lastStandAura(player, segment);
    }

    /**
     * 按住期间施加/升级<b>不死图腾</b>那一组 buff（规格 §8 第 2 层"主动：长按分段"）。
     *
     * <p>与 {@link #applyChargeDash} 同一条纪律：<b>只在段位变化时重新施加</b> ——
     * 每 tick 重置时长会让"松手后剩余时间"永远等于整段，等于无限续杯。</p>
     *
     * <p>段位 1..N ⇒ 三个效果的 amplifier 各 +0..+（N-1）（规格 §7 Q5 默认口径
     * "三类都按段位 +1 级"）；时长取 {@code segmentSeconds[段位-1]}。施加落点在
     * {@code LastStandHandler#applyTotemEffects}（被动触发走的是同一个方法 ⇒ 数值单一来源）。</p>
     */
    static void applyLastStand(ServerPlayer player, ArmorSet set, int heldTicks) {
        int level = ArmorSkillRuntime.effectiveLevel(player, set, ArmorSkillRuntime.LAST_STAND);
        LastStandConfigs.Config config = LastStandConfigs.config(level);
        int segment = LastStandConfigs.segmentOf(heldTicks, config);
        Integer last = LAST_STAND_SEGMENT.get(player.getUUID());
        if (last != null && last == segment) {
            // 段位没变：buff 已施加过（时长在推进），这里只续粒子
            if (player.tickCount % 2 == 0) {
                ArmorSkillFx.lastStandAura(player, segment);
            }
            return;
        }
        LAST_STAND_SEGMENT.put(player.getUUID(), segment);
        LastStandHandler.applyTotemEffects(player, segment, level);
        ArmorSkillFx.lastStandAura(player, segment);
    }

    /**
     * 一个服务端 tick 的<b>拖尾推进</b>（原 {@code ArmorSkillRuntime#tick()} 的尾部那一段，逐字搬来）。
     *
     * <p>迅捷拖尾（用户 2026-10-01 报"移速加成期间没有拖尾"）：
     * 拖尾跟着<b>迅捷 buff 的存续期</b>走，而不是只在按住的那几 tick —— 松手后 buff 还在（最多 120 秒），
     * 那段时间跑动同样应该有拖尾。到期自动清掉标记。</p>
     *
     * <p>⚠ 颜色用的是<b>登记时记住的那一套</b>（{@link DashTrail}），<b>不是</b>这里现取的玩家当前套：
     *   拖尾在松手后仍持续，现取的话 buff 期间换甲会让颜色当场跳变（见 {@link DashTrail} 的说明）。</p>
     *
     * <p>{@code id} 由调用方传入而不是这里现取：与原来 {@code tick()} 里的那一份逐字相同。</p>
     */
    static void advanceDashTrail(ServerPlayer player, UUID id) {
        DashTrail dash = DASH_TRAIL.get(id);
        if (dash != null) {
            if (player.level().getGameTime() >= dash.until()) {
                DASH_TRAIL.remove(id);
            } else if (player.tickCount % 2 == 0) {
                // 不限定"正在按住"：buff 有效期内跑动就该有拖尾（是否在移动由 dashTrail 自己判）
                ArmorSkillFx.dashTrail(player, dash.set());
            }
        }
    }

    /**
     * 段位表清理（原来是宿主里那两处逐行的 {@code DASH_SEGMENT.remove(id)} +
     * {@code LAST_STAND_SEGMENT.remove(id)}）：松手结算与"能量见底自动断停"两条路径都要走它。
     */
    static void clearSegments(UUID id) {
        DASH_SEGMENT.remove(id);
        LAST_STAND_SEGMENT.remove(id);
    }
}
