package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.hjmmd_8.createoreexpansion.content.skill.config.ChargeDashConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.FallGuardConfigs;
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

    /** 玩家持久数据里的冷却键前缀（后接技能 id）。 */
    private static final String COOLDOWN_PREFIX = "createoreexpansion:equip_cd_";

    /** 服务端长按计数：玩家 UUID → (槽位 → 已按住 tick 数)。 */
    private static final Map<UUID, Map<Integer, Integer>> HOLD_TICKS = new HashMap<>();

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
                    // 冷却中按住无效（冷却在松手时起，之前这里漏判 ⇒ 冷却形同虚设）
                    if (!isReady(player, skill) || ArmorEnergy.totalEnergy(player) <= 0) {
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
                    }
                    // 用户 2026-10-01 口径：能量消耗到"见底"⇒ 自动断停 + 把能量清空
                    if (set != null && skill != null && isExhausted(player, set, index, ticks)) {
                        held.remove(slot);
                        ACTIVE.remove(id);
                        DASH_SEGMENT.remove(id);
                        ArmorEnergy.consume(player, ArmorEnergy.totalEnergy(player)); // 见底即清空
                        startCooldown(player, skill, cooldownSecondsOf(player, set, index));
                        continue;
                    }
                }
            } else if (previous != null) {
                held.remove(slot);
                ACTIVE.remove(id);
                DASH_SEGMENT.remove(id);
                release(player, set, index, previous);
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
     * @return 1~3；未生效 / 该套没有这个技能 ⇒ 0
     */
    public static int levelOf(Player player, String skillId) {
        ArmorSet set = ArmorSet.effectiveSet(player);
        if (set == null || skillId == null) {
            return 0;
        }
        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            if (skillId.equals(skillId(set, index))) {
                return effectiveLevel(player, set);
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
            FallGuardConfigs.Config config = FallGuardConfigs.config(effectiveLevel(player, set));
            int cost = holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
            // 扣款走"四件平摊、全有或全无"
            ArmorEnergy.consume(player, cost);
            // 冷却：松手后开始计（记在玩家持久数据里）
            startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(CHARGE_DASH)) {
            // 蓄能疾骋：**迅捷在按住期间就已经逐段生效**（用户 2026-10-01 修正：
            // "按住 R 之后必须按完才有疾跑 buff，我想让它一边按一边产生"）。
            // 因此松手只结算能量与冷却，**不再补发效果** —— 否则松手等于白送一整段时长。
            ChargeDashConfigs.Config config = ChargeDashConfigs.config(effectiveLevel(player, set));
            ArmorEnergy.consume(player, holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost()));
            startCooldown(player, skillId, config.cooldownSeconds());
        }
    }

    /** 蓄能疾骋的"当前段位"（只在段位往上爬时重新施加效果，避免每 tick 重置时长）。 */
    private static final Map<UUID, Integer> DASH_SEGMENT = new HashMap<>();

    /**
     * 按住期间施加/升级<b>迅捷</b>（用户口径：一边按一边产生）。
     *
     * <p>只在<b>段位变化</b>时重新施加：每 tick 重置时长会让"松手后剩余时间"永远等于整段，
     * 那等于无限续杯。</p>
     */
    private static void applyChargeDash(ServerPlayer player, ArmorSet set, int heldTicks) {
        ChargeDashConfigs.Config config = ChargeDashConfigs.config(effectiveLevel(player, set));
        int segment = ChargeDashConfigs.segmentOf(heldTicks, config);
        Integer last = DASH_SEGMENT.get(player.getUUID());
        if (last != null && last == segment) {
            return;
        }
        DASH_SEGMENT.put(player.getUUID(), segment);
        int seconds = config.segmentSeconds()[segment - 1];
        if (seconds > 0) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                seconds * 20, segment - 1, false, true, true));
        }
    }

    /** 该技能在该等级下、按住这么多 tick 时的累计花费（与松手结算同一个公式）。 */
    private static int accumulatedCost(ServerPlayer player, ArmorSet set, int index, int heldTicks) {
        int level = effectiveLevel(player, set);
        if (FALL_GUARD.equals(skillId(set, index))) {
            FallGuardConfigs.Config config = FallGuardConfigs.config(level);
            return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
        }
        ChargeDashConfigs.Config config = ChargeDashConfigs.config(level);
        return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
    }

    /** 该技能该起的冷却秒数（给"能量见底自动断停"用，与松手结算同一处取值）。 */
    private static int cooldownSecondsOf(ServerPlayer player, ArmorSet set, int index) {
        int level = effectiveLevel(player, set);
        if (FALL_GUARD.equals(skillId(set, index))) {
            return FallGuardConfigs.config(level).cooldownSeconds();
        }
        return ChargeDashConfigs.config(level).cooldownSeconds();
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
    }

    /**
     * 当前生效的<b>整体技能等级</b>（用户 2026-10-01 定稿口径）。
     *
     * <p><b>逐件算完再取最大值</b>：每件护甲的等级 = 该套基准等级 + 该件上的技艺提升 − 该件上的记忆回溯；
     * 整体等级 = 四件里最大的那个（最后钳在 1~3）。</p>
     *
     * <p><b>为什么不是"提升取最大、回溯取最大"</b>（我第一版那样写是错的）：</p>
     * <pre>
     * 基准 1；头盔有「技艺提升 1」，靴子有「记忆回溯 1」
     *   逐件取最大（本实现）：max(1+1, 1-1) = 2   ← 用户口径：以"某一件上最好的净结果"为准
     *   分别取最大（旧实现）：1 + max(1) - max(1) = 1
     * </pre>
     * <p>用户原话：「记忆重塑和记忆提升这两个附魔针对于套装来说，整体技能的等级，
     * 取决于所有套装中相应增或减的技能等级的最大值。」</p>
     */
    public static int effectiveLevel(Player player, ArmorSet set) {
        if (player == null || set == null) {
            return 0;
        }
        int base = set.wornLevel(player);
        if (base <= 0) {
            return 0;
        }
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
     * <p>本轮只落地翠玉套的槽位 1（{@link #FALL_GUARD}）；其余在各自技能实现时补。</p>
     */
    private static @Nullable String skillId(ArmorSet set, int index) {
        if (set == ArmorSet.JADE) {
            return switch (index) {
                case 0 -> FALL_GUARD;
                case 1 -> CHARGE_DASH;
                default -> null;
            };
        }
        return null;
    }
}
