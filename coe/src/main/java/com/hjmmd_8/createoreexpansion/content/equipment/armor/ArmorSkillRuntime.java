package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
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
                // 开始条件：这一套生效、且这一套在这个槽位真的有技能
                if (previous == null) {
                    if (set == null || !hasSkill(set, index)) {
                        continue;
                    }
                    if (skillId(set, index).equals(FALL_GUARD) && ArmorEnergy.totalEnergy(player) <= 0) {
                        continue; // 一分能量都没有：按下去不生效（避免"免了摔落却付不起"）
                    }
                    held.put(slot, 0);
                    ACTIVE.put(id, skillId(set, index));
                } else {
                    held.put(slot, previous + 1);
                }
            } else if (previous != null) {
                held.remove(slot);
                ACTIVE.remove(id);
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
        }
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

    /** 当前生效等级（套装基准等级 + 护甲上技艺提升/记忆回溯的加减，钳在 1~3）。 */
    public static int effectiveLevel(Player player, ArmorSet set) {
        if (player == null || set == null) {
            return 0;
        }
        int level = set.wornLevel(player);
        if (level <= 0) {
            return 0;
        }
        int boost = 0;
        int regression = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : ArmorSet.armorSlots()) {
            var stack = player.getItemBySlot(slot);
            boost = Math.max(boost, com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
                .skillBoostLevel(stack));
            regression = Math.max(regression, com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
                .skillRegressionLevel(stack));
        }
        return Math.max(1, Math.min(FallGuardConfigs.MAX_LEVEL, level + boost - regression));
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
        if (set == ArmorSet.JADE && index == 0) {
            return FALL_GUARD;
        }
        return null;
    }
}
