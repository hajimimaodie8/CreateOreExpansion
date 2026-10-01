package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.handler.LastStandHandler;
import com.hjmmd_8.createoreexpansion.content.skill.config.ChargeDashConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.FallGuardConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.LastStandConfigs;
import com.leaf.skiller.server.PlayerPressedKeys;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

/**
 * <b>装备技能的长按运行时</b>（用户 2026-10-01 要求的新骨架）。
 *
 * <h2>为什么需要它</h2>
 * <p>本模组已有的 9 个技能（工具与弓）<b>全是"事件触发的瞬时释放"</b>：事件来了 → 交内核 →
 * 算目标 → 扣能 → 放。而装备技能是<b>长按语义</b>（虚衡坠护按多久免多久、蓄能疾骋按住越久 Buff 越强），
 * 内核没有"按住期间持续生效"的模型，因此本类在服务端负责这段编排。</p>
 *
 * <h2>按键来源与槽位</h2>
 * <p>读 {@link PlayerPressedKeys}（客户端按键包写入的<b>服务端权威</b>状态），槽位号与
 * {@link ArmorSkillProvider} 一致：装备占 {@code 3/4/5}（键一/二/三）。客户端只在按住装备修饰键时
 * 让这些槽位报"按下"（见 {@code CoeSkillClient}），所以这里天然就是"按住 Alt + 技能键"。</p>
 *
 * <h2>扣能口径（用户原文照搬）</h2>
 * <p>「用长按的时间秒数除以总的最长时间秒数，再乘以消耗的能量值进行计算。所有计算的能量扣除均向下取整」
 * ⇒ {@code cost = floor(heldTicks * totalCost / (maxSeconds * 20))}（整数运算，floor 天然成立）。
 * 提前松手只扣按比例的那部分。</p>
 *
 * <h2>何时扣、扣不动怎么办</h2>
 * <ul>
 *   <li><b>松手时一次性扣</b>（与用户描述的"按比例算"一致）；扣款走
 *       {@link ArmorEnergy#consume}（四件平摊、全有或全无）。</li>
 *   <li><b>按住开始前就要求付得起</b>：能量为 0 时按下去不生效（不进入长按状态），
 *       否则会出现"免了摔落却付不起"的漏洞。</li>
 *   <li>扣款失败（四件合计不够）时<b>效果已经发生</b>（长按期间的豁免无法回收）——
 *       这是长按语义的固有代价，用"开始前检查 + 每 tick 检查剩余量"把窗口压到最小。</li>
 * </ul>
 *
 * <h2>冷却</h2>
 * <p>冷却记在<b>玩家持久数据</b>（{@code createoreexpansion:equip_cd_<skill>}）而不是某一件护甲上：
 * 它是<b>套级状态</b>，写在件上会因换甲而失配。松手后开始计时。</p>
 *
 * @since 1.0.0
 */
public final class ArmorSkillRuntime {

    /** 装备技能槽位 → 技能 id（与 {@link ArmorSkillProvider#SLOT_BASE} 顺序一致）。 */
    public static final String FALL_GUARD = "fall_guard";

    /** 虚衡坠护的技能 id（{@code createoreexpansion:fall_guard}）—— 注册与 provider 共用的唯一真源。 */
    public static final net.minecraft.resources.ResourceLocation FALL_GUARD_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(FALL_GUARD);

    /**
     * 蓄能疾骋（翠玉套槽位 2）：长按越久，松手时给的<b>迅捷</b>越强、越久
     * （用户 2026-10-01 规格：Lv1 三段 20/40/60 · Lv2 四段 · Lv3 五段；数值源 {@code ChargeDashConfigs}）。
     */
    public static final String CHARGE_DASH = "charge_dash";

    /** 蓄能疾骋的技能 id（{@code createoreexpansion:charge_dash}）。 */
    public static final net.minecraft.resources.ResourceLocation CHARGE_DASH_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(CHARGE_DASH);

    /**
     * 绝境守护（宝石套槽位 1，基准等级 1 —— 见 {@link ArmorSkillLevels}）。
     *
     * <p><b>本轮（规格 §8 第 1 层）只登记 id 与基准等级</b>：配置类（概率 50/60/70、长按
     * 15/10/5 秒、不死图腾 buff）、被动触发与主动分段都在<b>第 2 层</b>。因此它现在还
     * <b>不在</b> {@link ArmorSkillProvider#skillIdsOf(ArmorSet)}、不在内核注册表、也不在
     * {@code AllSkills} —— 那是第 2 层的事，现在加进去会让"按住 Alt+R 有反应但什么也不发生"。</p>
     */
    public static final String LAST_STAND = "last_stand";

    /** 绝境守护的技能 id（{@code createoreexpansion:last_stand}）—— 与基准等级表共用的唯一真源。 */
    public static final net.minecraft.resources.ResourceLocation LAST_STAND_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(LAST_STAND);

    /**
     * 临域充力（宝石套槽位 2，基准等级 2 —— 见 {@link ArmorSkillLevels}）。
     *
     * <p><b>本轮只登记 id 与基准等级</b>：应力注入器、手摇曲柄判定、环绕粒子都在<b>第 3 层</b>，
     * 同样不注册内核、不进 {@code AllSkills}。</p>
     */
    public static final String FIELD_CHARGE = "field_charge";

    /** 临域充力的技能 id（{@code createoreexpansion:field_charge}）。 */
    public static final net.minecraft.resources.ResourceLocation FIELD_CHARGE_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(FIELD_CHARGE);

    /** 玩家持久数据里的冷却键前缀（后接技能 id）。 */
    private static final String COOLDOWN_PREFIX = "createoreexpansion:equip_cd_";

    /** 服务端长按计数：玩家 UUID → (槽位 → 已按住 tick 数)。 */
    private static final Map<UUID, Map<Integer, Integer>> HOLD_TICKS = new HashMap<>();

    /** 发动被挡的提示节流：玩家#槽位 → 上次提示的 tick（每 20 tick 最多一次）。 */
    private static final Map<String, Integer> BLOCK_NOTIFY_TICK = new HashMap<>();

    /** 【临时诊断】玩家#槽位 → 上一次读到的按下状态（只在变化时打日志）。 */
    private static final Map<String, Boolean> DIAG_PRESSED = new HashMap<>();

    /** 当前处于"长按生效中"的玩家与技能（虚衡坠护的主动豁免要读它）。 */
    private static final Map<UUID, String> ACTIVE = new HashMap<>();

    private ArmorSkillRuntime() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 每个服务端 tick 调一次（由 {@code ArmorSkillHandler} 在 {@code ServerTickEvent.Post} 里调）。
     *
     * <p>三次遍历：① 推进按住的槽位；② 处理"刚松手"的槽位（结算 + 冷却）；③ 清掉离场玩家。</p>
     */
    public static void tick(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        ArmorSet set = ArmorSet.effectiveSet(player);
        Map<Integer, Integer> held = HOLD_TICKS.computeIfAbsent(id, k -> new HashMap<>());

        // 【临时诊断，定位"按住 Alt+R 服务端毫无反应"】只在状态变化时打印，不刷屏；定位后删掉。
        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            int slot = ArmorSkillProvider.SLOT_BASE + index;
            boolean now = PlayerPressedKeys.isPressed(player, slot);
            String diagKey = id + "#" + slot;
            Boolean before = DIAG_PRESSED.get(diagKey);
            if (before == null || before != now) {
                DIAG_PRESSED.put(diagKey, now);
                com.hjmmd_8.createoreexpansion.common.CoeCore.LOGGER.info(
                    "[装备技能诊断] 槽位={} 服务端读到按下={} 套装={} 技能={} 能量合计={}",
                    slot, now, set, set == null ? null : skillId(set, index), ArmorEnergy.totalEnergy(player));
            }
        }

        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            int slot = ArmorSkillProvider.SLOT_BASE + index;
            boolean pressed = PlayerPressedKeys.isPressed(player, slot);
            Integer previous = held.get(slot);
            if (pressed) {
                // 开始条件：这一套生效、这一套在这个槽位真的有技能、冷却已过、还有能量
                if (previous == null) {
                    if (set == null || !hasSkill(set, index)) {
                        continue;
                    }
                    String skill = skillId(set, index);
                    // 冷却中 / 没能量：**必须让玩家看得见原因**（用户 2026-10-01 报"有的时候按了根本
                    // 不生效、松开也没给" —— 就是被这两条静默挡掉的，玩家只能感到"随机失效"）。
                    if (!isReady(player, skill)) {
                        notifyBlocked(player, slot, "createoreexpansion.equip_skill.cooldown",
                            Math.max(1, cooldownLeft(player, skill) / 20));
                        continue;
                    }
                    if (ArmorEnergy.totalEnergy(player) <= 0) {
                        notifyBlocked(player, slot, "createoreexpansion.equip_skill.no_energy", 0);
                        continue;
                    }
                    held.put(slot, 0);
                    ACTIVE.put(id, skill);
                } else {
                    int ticks = previous + 1;
                    held.put(slot, ticks);
                    String skill = skillId(set, index);
                    if (CHARGE_DASH.equals(skill)) {
                        // 用户 2026-10-01 修正：**一边按一边产生疾跑 buff**（不是松手才给）
                        applyChargeDash(player, set, ticks);
                    } else if (LAST_STAND.equals(skill)) {
                        // 绝境守护：同样"一边按一边给"—— 段位推进时施加/升级不死图腾 buff
                        // （规格 §8 第 2 层"段位随长按推进"，松手不再补发，理由同蓄能疾骋）
                        applyLastStand(player, set, ticks);
                    }
                    // 用户 2026-10-01 口径：能量消耗到"见底"⇒ 自动断停 + 把能量清空
                    // ⚠ 只在"这条技能真的有耗能曲线"时判：宝石套槽位 2（临域充力）的行为是
                    // **第 3 层**，它此刻只是被登记进套件表（客户端会轮询、HUD 会列行），
                    // 按住它不该把护甲能量清空 —— 见 accumulatedCost 对它的 0 返回。
                    if (set != null && skill != null && !FIELD_CHARGE.equals(skill)
                            && isExhausted(player, set, index, ticks)) {
                        held.remove(slot);
                        ACTIVE.remove(id);
                        DASH_SEGMENT.remove(id);
                        LAST_STAND_SEGMENT.remove(id);
                        ArmorEnergy.consume(player, ArmorEnergy.totalEnergy(player)); // 见底即清空
                        startCooldown(player, skill, cooldownSecondsOf(player, set, index));
                        continue;
                    }
                }
            } else if (previous != null) {
                held.remove(slot);
                ACTIVE.remove(id);
                DASH_SEGMENT.remove(id);
                LAST_STAND_SEGMENT.remove(id);
                release(player, set, index, previous);
            }
        }
        // 迅捷拖尾（用户 2026-10-01 报"移速加成期间没有拖尾"）：
        // 拖尾跟着**迅捷 buff 的存续期**走，而不是只在按住的那几 tick —— 松手后 buff 还在（最多 120 秒），
        // 那段时间跑动同样应该有拖尾。到期自动清掉标记。
        Long dashUntil = DASH_UNTIL.get(id);
        if (dashUntil != null) {
            if (dashUntil == null || player.level().getGameTime() >= dashUntil) {
                DASH_UNTIL.remove(id);
            } else if (player.tickCount % 2 == 0) {
                // 不限定"正在按住"：buff 有效期内跑动就该有拖尾（是否在移动由 dashTrail 自己判）
                ArmorSkillFx.dashTrail(player);
            }
        }

        if (held.isEmpty()) {
            HOLD_TICKS.remove(id);
        }
    }

    /** 玩家离场/死亡：清掉长按状态（冷却仍留在持久数据里）。 */
    public static void forget(Player player) {
        if (player == null) {
            return;
        }
        HOLD_TICKS.remove(player.getUUID());
        ACTIVE.remove(player.getUUID());
    }

    /** 该玩家此刻是否正在长按某个装备技能（虚衡坠护的"按住 = 100% 豁免"读它）。 */
    public static boolean isHolding(Player player, String skillId) {
        return player != null && skillId != null && skillId.equals(ACTIVE.get(player.getUUID()));
    }

    /**
     * 玩家当前生效的<b>某技能等级</b>（该技能必须由当前生效的那一套提供）。
     *
     * <p><b>HUD / 护甲 tooltip / 其它调用点的唯一公开入口</b>：等级是<b>逐技能</b>的
     * （规格 §0.1/§0.2），所以每一条技能行都问一次本方法，不要拿某一个技能的等级去涂所有行。</p>
     *
     * @return 1~3；未生效 / 该套没有这个技能 ⇒ 0
     */
    public static int levelOf(Player player, String skillId) {
        ArmorSet set = ArmorSet.effectiveSet(player);
        if (set == null || skillId == null) {
            return 0;
        }
        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            if (skillId.equals(skillId(set, index))) {
                return effectiveLevel(player, set, skillId);
            }
        }
        return 0;
    }

    /** 该玩家此刻是否正在长按任一装备技能。 */
    public static boolean isHoldingAny(Player player) {
        return player != null && ACTIVE.containsKey(player.getUUID());
    }

    /** 冷却剩余 tick（0 = 就绪）。 */
    public static int cooldownLeft(Player player, String skillId) {
        if (player == null || skillId == null) {
            return 0;
        }
        CompoundTag data = player.getPersistentData();
        long until = data.getLong(COOLDOWN_PREFIX + skillId);
        return (int) Math.max(0, until - player.level().getGameTime());
    }

    /** 该技能是否就绪（冷却已过）。 */
    public static boolean isReady(Player player, String skillId) {
        return cooldownLeft(player, skillId) <= 0;
    }

    /**
     * 长按结算：按用户的比例式算钱、扣款、起冷却，并触发该槽位技能的效果收尾。
     *
     * @param heldTicks 实际按住的服务端 tick 数（不含松手那一 tick）
     */
    private static void release(ServerPlayer player, @Nullable ArmorSet set, int index, int heldTicks) {
        if (set == null) {
            return;
        }
        String skillId = skillId(set, index);
        if (skillId == null) {
            return;
        }
        if (skillId.equals(FALL_GUARD)) {
            FallGuardConfigs.Config config = FallGuardConfigs.config(effectiveLevel(player, set, skillId));
            int cost = holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
            // 扣款走"四件平摊、全有或全无"
            ArmorEnergy.consume(player, cost);
            // 冷却：松手后开始计（记在玩家持久数据里）
            startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(CHARGE_DASH)) {
            // 蓄能疾骋：**迅捷在按住期间就已经逐段生效**（用户 2026-10-01 修正：
            // "按住 R 之后必须按完才有疾跑 buff，我想让它一边按一边产生"）。
            // 因此松手只结算能量与冷却，**不再补发效果** —— 否则松手等于白送一整段时长。
            ChargeDashConfigs.Config config = ChargeDashConfigs.config(effectiveLevel(player, set, skillId));
            ArmorEnergy.consume(player, holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost()));
            startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(LAST_STAND)) {
            // 绝境守护（宝石套 · 槽位 1）：与蓄能疾骋同一条口径 —— 不死图腾 buff 在按住期间
            // 就按段位施加（见 applyLastStand），松手**只结算能量与冷却、不再补发**。
            // 补发会让"按住 1 tick 再松手"白拿一整段时长。
            LastStandConfigs.Config config = LastStandConfigs.config(effectiveLevel(player, set, skillId));
            ArmorEnergy.consume(player, holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost()));
            startCooldown(player, skillId, config.cooldownSeconds());
        }
    }

    /** 迅捷 buff 的到期时刻（gameTime）：拖尾跟着它走，松手后 buff 还在就仍有拖尾。 */
    private static final Map<UUID, Long> DASH_UNTIL = new HashMap<>();

    /** 蓄能疾骋的"当前段位"（只在段位往上爬时重新施加效果，避免每 tick 重置时长）。 */
    private static final Map<UUID, Integer> DASH_SEGMENT = new HashMap<>();

    /**
     * 按住期间施加/升级<b>迅捷</b>（用户口径：一边按一边产生）。
     *
     * <p>只在<b>段位变化</b>时重新施加：每 tick 重置时长会让"松手后剩余时间"永远等于整段，
     * 那等于无限续杯。</p>
     */
    private static void applyChargeDash(ServerPlayer player, ArmorSet set, int heldTicks) {
        ChargeDashConfigs.Config config =
            ChargeDashConfigs.config(effectiveLevel(player, set, CHARGE_DASH));
        int segment = ChargeDashConfigs.segmentOf(heldTicks, config);
        Integer last = DASH_SEGMENT.get(player.getUUID());
        if (last != null && last == segment) {
            return;
        }
        DASH_SEGMENT.put(player.getUUID(), segment);
        int seconds = config.segmentSeconds()[segment - 1];
        if (seconds > 0) {
            // 登记"拖尾存续到什么时候"：拖尾要覆盖整个迅捷 buff，而不只是按住的那几 tick
            long now = player.level().getGameTime();
            long until = now + seconds * 20L;
            DASH_UNTIL.merge(player.getUUID(), until, Math::max);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                seconds * 20, segment - 1, false, true, true));
        }
    }

    /** 绝境守护的"当前段位"（只在段位往上爬时重新施加 buff，避免每 tick 重置时长）。 */
    private static final Map<UUID, Integer> LAST_STAND_SEGMENT = new HashMap<>();

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
    private static void applyLastStand(ServerPlayer player, ArmorSet set, int heldTicks) {
        int level = effectiveLevel(player, set, LAST_STAND);
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
     * 按住时"发动不了"的可见反馈（动作栏 + 日志）。
     *
     * <p>为什么必须有：用户 2026-10-01 报"有的时候按住 Alt+R 根本不生效、松开也没给" ——
     * 真实原因是**冷却中**与**能量为 0** 这两条静默 `continue`，玩家感受就是"随机失效"。
     * 现在会明说原因，并且**按同一槽位每 20 tick 只提示一次**（按住不放也不会刷屏）。</p>
     */
    private static void notifyBlocked(ServerPlayer player, int slot, String langKey, int seconds) {
        String key = player.getUUID() + "#" + slot;
        Integer last = BLOCK_NOTIFY_TICK.get(key);
        int now = player.tickCount;
        if (last != null && now - last < 20) {
            return;
        }
        BLOCK_NOTIFY_TICK.put(key, now);
        // 2026-10-01 用户否掉动作栏字幕（"单独来一个字幕，把装备的 tooltip 全都盖住"）：
        // 冷却信息现在只出现在 HUD 的括号冷却行，这里只留日志。
        com.hjmmd_8.createoreexpansion.common.CoeCore.LOGGER.info(
            "[装备技能] 槽位 {} 发动被挡：{}（冷却剩余 {} 秒，能量合计 {}）",
            slot, langKey, seconds, ArmorEnergy.totalEnergy(player));
    }

    /** 该技能在该等级下、按住这么多 tick 时的累计花费（与松手结算同一个公式）。 */
    private static int accumulatedCost(ServerPlayer player, ArmorSet set, int index, int heldTicks) {
        String skill = skillId(set, index);
        if (FIELD_CHARGE.equals(skill)) {
            // 临域充力（宝石套槽位 2）：行为是第 3 层，此刻**没有耗能曲线**（规格 §2.2 的
            // 点/秒要等第 3 层接）。返回 0 才能让"能量见底自动断停"不误触发，
            // 否则按住槽位 2 会把满套能量一次性清空。
            return 0;
        }
        int level = effectiveLevel(player, set, skill);
        if (FALL_GUARD.equals(skill)) {
            FallGuardConfigs.Config config = FallGuardConfigs.config(level);
            return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
        }
        if (CHARGE_DASH.equals(skill)) {
            ChargeDashConfigs.Config config = ChargeDashConfigs.config(level);
            return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
        }
        LastStandConfigs.Config config = LastStandConfigs.config(level);
        return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
    }

    /** 该技能该起的冷却秒数（给"能量见底自动断停"用，与松手结算同一处取值）。 */
    private static int cooldownSecondsOf(ServerPlayer player, ArmorSet set, int index) {
        String skill = skillId(set, index);
        if (FIELD_CHARGE.equals(skill)) {
            // 同上：行为是第 3 层 ⇒ 此刻不起冷却（第 3 层会补 25/20/15 秒那张表）
            return 0;
        }
        int level = effectiveLevel(player, set, skill);
        if (FALL_GUARD.equals(skill)) {
            return FallGuardConfigs.config(level).cooldownSeconds();
        }
        if (CHARGE_DASH.equals(skill)) {
            return ChargeDashConfigs.config(level).cooldownSeconds();
        }
        return LastStandConfigs.config(level).cooldownSeconds();
    }

    /**
     * 累计花费是否已经<b>见底</b>（用户 2026-10-01："能量消耗到 100 的时候要自动断停，然后把能量全都清空"）。
     *
     * <p>判据 = 累计花费 ≥ 当前可用合计能量（等于 0 也视为见底）。</p>
     */
    private static boolean isExhausted(ServerPlayer player, ArmorSet set, int index, int heldTicks) {
        int available = ArmorEnergy.totalEnergy(player);
        return available <= 0 || accumulatedCost(player, set, index, heldTicks) >= available;
    }

    /**
     * 长按花费：<b>{@code floor(heldTicks × totalCost / (maxSeconds × 20))}</b>。
     *
     * <p>用户原文："用长按的时间秒数除以总的最长时间秒数，再乘以消耗的能量值…所有计算的能量扣除均向下取整"。
     * 用整数运算实现，天然向下取整（不引入浮点误差）。</p>
     *
     * @param heldTicks     实际按住的 tick
     * @param maxSeconds    该级的长按上限秒数
     * @param totalCost     该级按满整段的总量
     * @return 应扣能量（按满即 {@code totalCost}；不足一个 tick 的余量被 floor 掉）
     */
    public static int holdCost(int heldTicks, int maxSeconds, int totalCost) {
        if (heldTicks <= 0 || maxSeconds <= 0 || totalCost <= 0) {
            return 0;
        }
        int maxTicks = maxSeconds * 20;
        long cost = (long) Math.min(heldTicks, maxTicks) * totalCost / maxTicks;
        return (int) cost;
    }

    /** 起冷却（秒）。 */
    public static void startCooldown(Player player, String skillId, int seconds) {
        if (player == null || skillId == null || seconds <= 0) {
            return;
        }
        player.getPersistentData().putLong(COOLDOWN_PREFIX + skillId,
            player.level().getGameTime() + seconds * 20L);
        // 同步给客户端（持久数据不同步 ⇒ HUD 读不到冷却；用户 2026-10-01 报"HUD 那行没被替换"）
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                new EquipCooldownPayload(skillId, seconds));
        }
    }

    /**
     * <b>某个技能</b>当前生效的等级（用户 2026-10-01 规格 §0.1 定稿口径）。
     *
     * <p><b>逐件算完再取最大值</b>：每件护甲的等级 = <b>该技能自己的基准等级</b>
     * （{@link ArmorSkillLevels#baseLevelOf}）+ 该件上的技艺提升 − 该件上的记忆回溯；
     * 等级 = 四件里最大的那个（最后钳在 1~3）。</p>
     *
     * <p><b>为什么不是"提升取最大、回溯取最大"</b>（我第一版那样写是错的）：</p>
     * <pre>
     * 基准 1；头盔有「技艺提升 1」，靴子有「记忆回溯 1」
     *   逐件取最大（本实现）：max(1+1, 1-1) = 2   ← 用户口径：以"某一件上最好的净结果"为准
     *   分别取最大（旧实现）：1 + max(1) - max(1) = 1
     * </pre>
     * <p>用户原话：「记忆重塑和记忆提升这两个附魔针对于套装来说，整体技能的等级，
     * 取决于所有套装中相应增或减的技能等级的最大值。」</p>
     *
     * <p><b>与旧版的唯一区别</b>：基准不再取"整套一个值"（{@code set.wornLevel}），而是
     * <b>逐技能</b>问 {@link ArmorSkillLevels}（宝石套 绝境守护 1 / 临域充力 2；翠玉套两条仍为 1）。
     * 用户 2026-10-01 第二轮明确否掉"整体 LV1"的展示，所以每个技能都要单独算一遍。</p>
     *
     * @param player  玩家
     * @param set     生效的那一套
     * @param skillId 技能 id 的 path（如 {@code fall_guard}，与 {@link #levelOf} 同形）
     * @return 1~3；未成套 / 参数为空 ⇒ 0
     */
    public static int effectiveLevel(Player player, ArmorSet set, String skillId) {
        if (player == null || set == null || skillId == null) {
            return 0;
        }
        // 生效门槛：不成套（且没有散构聚能补齐）时，该套一个技能都不给
        if (set.wornLevel(player) <= 0) {
            return 0;
        }
        // 该技能自己的基准（有显式覆盖用覆盖，否则回落到该套基准）
        int base = ArmorSkillLevels.baseLevelOf(set,
            com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(skillId));
        int best = base;
        for (net.minecraft.world.entity.EquipmentSlot slot : ArmorSet.armorSlots()) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || ArmorSet.of(stack) == null) {
                continue;
            }
            int boost = com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
                .skillBoostLevel(stack);
            int regression = com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
                .skillRegressionLevel(stack);
            best = Math.max(best, base + boost - regression);
        }
        return Math.max(1, Math.min(FallGuardConfigs.MAX_LEVEL, best));
    }

    /** 该套在第 index 个装备槽位上有没有技能（雷鸣套的 1、2 通星界 ⇒ 见实现）。 */
    private static boolean hasSkill(ArmorSet set, int index) {
        return skillId(set, index) != null;
    }

    /**
     * 该套在第 index 个装备槽位上的技能 id。
     *
     * <p>顺序 = 槽位顺序（{@link ArmorSkillProvider#SLOT_BASE} 起），与
     * {@link ArmorSkillProvider#skillIdsOf} 的表<b>必须逐字同序</b>：那张表决定客户端轮询哪个槽位、
     * HUD 列哪几行，这里决定"按下这个槽位到底跑哪个技能"。</p>
     *
     * <p>翠玉套 {@code fall_guard / charge_dash}（第 1 层已有）；宝石套
     * {@code last_stand / field_charge}（规格 §八 第 2 层：两条技能都登记进套件表 ——
     * 临域充力的<b>行为</b>是第 3 层，本轮只有绝境守护有行为）。</p>
     */
    private static @Nullable String skillId(ArmorSet set, int index) {
        if (set == ArmorSet.JADE) {
            return switch (index) {
                case 0 -> FALL_GUARD;
                case 1 -> CHARGE_DASH;
                default -> null;
            };
        }
        if (set == ArmorSet.GEM) {
            return switch (index) {
                case 0 -> LAST_STAND;
                case 1 -> FIELD_CHARGE;
                default -> null;
            };
        }
        return null;
    }
}
