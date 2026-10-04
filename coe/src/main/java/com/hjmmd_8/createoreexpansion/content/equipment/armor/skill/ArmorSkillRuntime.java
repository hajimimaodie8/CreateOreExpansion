package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.field.FieldChargeRuntime;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.handler.LastStandHandler;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.ChargeDashConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.FallGuardConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.FieldChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.LastStandConfigs;
import com.leaf.skiller.server.PlayerPressedKeys;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.EquipCooldownPayload;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;

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
     * 蓄能疾骋（翠玉套槽位 2；<b>用户 2026-10-01 更正后同时也是宝石套槽位 2</b> ——
     * 同一个技能 id、同一套数值，从翠玉套移植，翠玉套那份保持不动）：长按越久，松手时给的
     * <b>迅捷</b>越强、越久（用户 2026-10-01 规格：Lv1 三段 20/40/60 · Lv2 四段 · Lv3 五段；
     * 数值源 {@code ChargeDashConfigs}）。
     *
     * <p>行为一律<b>按技能 id 分派</b>（见 {@code tick}/{@code release}），与它是哪一套无关：
     * 能量从玩家当前生效那一套的 {@link ArmorEnergy} 池扣。</p>
     */
    public static final String CHARGE_DASH = "charge_dash";

    /** 蓄能疾骋的技能 id（{@code createoreexpansion:charge_dash}）。 */
    public static final net.minecraft.resources.ResourceLocation CHARGE_DASH_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(CHARGE_DASH);

    /**
     * 绝境守护（宝石套槽位 1，基准等级 1 —— 见 {@link ArmorSkillLevels}）。
     *
     * <p>数值源 {@code LastStandConfigs}（概率 50/60/70、长按 15/10/5 秒、不死图腾 buff）：
     * 被动触发在 {@code LastStandHandler}、主动分段在 {@link #applyLastStand}。</p>
     */
    public static final String LAST_STAND = "last_stand";

    /** 绝境守护的技能 id（{@code createoreexpansion:last_stand}）—— 与基准等级表共用的唯一真源。 */
    public static final net.minecraft.resources.ResourceLocation LAST_STAND_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(LAST_STAND);

    /**
     * 临域充力（宝石套槽位 3，基准等级 1 —— 见 {@link ArmorSkillLevels}）。
     *
     * <p><b>用户 2026-10-01 更正</b>：它<b>不再</b>是槽位 2、基准也不是 2；槽位 2 让给了
     * 从翠玉套移植的蓄能疾骋（{@link #CHARGE_DASH}）。</p>
     *
     * <p>应力注入器、手摇曲柄判定、环绕粒子在第 3 层落地，不注册内核、不进 {@code AllSkills}
     * 之外的东西。</p>
     */
    public static final String FIELD_CHARGE = "field_charge";

    /** 临域充力的技能 id（{@code createoreexpansion:field_charge}）。 */
    public static final net.minecraft.resources.ResourceLocation FIELD_CHARGE_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(FIELD_CHARGE);

    /**
     * 衡元择势（星界套 · 槽位 1；用户 2026-10-02 裁定，id 由需求 §四 推断）。
     *
     * <p><b>融合规则</b>：{@code 衡元择势 LvN} = 蓄能疾骋 LvN（移速）+ 绝境守护 LvN（图腾），
     * <b>各取一份</b>，长按开始时按"选择判断"二选一生效 ⇒ 数值一律<b>引用</b>
     * {@link ChargeDashConfigs} 与 {@link LastStandConfigs}，<b>不新建配置类、不改那两张表</b>
     * （翠玉 / 宝石两套的现有行为因此逐值不变）。</p>
     * <p>与绝境守护同源的那条<b>被动</b>（高额伤害 ⇒ 概率取消伤害 + 不死图腾）也照搬
     * {@link LastStandConfigs} 的判定与概率，见 {@code LastStandHandler}。</p>
     */
    public static final String BALANCE_CHOICE = "balance_choice";

    /** 衡元择势的技能 id（{@code createoreexpansion:balance_choice}）。 */
    public static final net.minecraft.resources.ResourceLocation BALANCE_CHOICE_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(BALANCE_CHOICE);

    /**
     * 星芒嬗震（星界套 · 槽位 3，基准等级 1；用户 2026-10-02 裁定，id 由需求 §四 推断）。
     *
     * <p>点按/长按向准心发射<b>既有</b>能量波（攻击态，长按按蓄力曲线并排分叉），
     * 数值源 {@code StarShockConfigs}，发射与批次编组见 {@code StarShockRuntime}。</p>
     */
    public static final String STAR_SHOCK = "star_shock";

    /** 星芒嬗震的技能 id（{@code createoreexpansion:star_shock}）。 */
    public static final net.minecraft.resources.ResourceLocation STAR_SHOCK_ID =
        com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(STAR_SHOCK);

    /** 玩家持久数据里的冷却键前缀（后接技能 id）。 */
    private static final String COOLDOWN_PREFIX = "createoreexpansion:equip_cd_";

    /** 服务端长按计数：玩家 UUID → (槽位 → 已按住 tick 数)。 */
    private static final Map<UUID, Map<Integer, Integer>> HOLD_TICKS = new HashMap<>();

    /**
     * 星芒嬗震（星界套 · 槽位 3）的按住 tick 数：玩家 UUID → 已按住 tick。
     *
     * <p>为什么单独一张表：这张表的值要在<b>松手那一 tick</b>传给
     * {@code StarShockRuntime#release} 去补最后不足一步的零头，而 {@code held} 那张表
     * 在松手分支里是刚被 {@code remove} 掉的（取不到值）。其它技能用不到它 ——
     * 它们的耗能是"松手时一次性按比例扣"，由 {@code release(..., heldTicks)} 的参数直接拿到。</p>
     */
    private static final Map<UUID, Integer> STAR_SHOCK_HOLD = new HashMap<>();

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
                    // 临域充力（宝石套槽位 3）：发动前要过"§2.1 判定"（半径内至少一个动力源方块）
                    // 与"注入器放得下吗"两道关；任何一道不过 ⇒ 按**发动失败**处理：
                    // 不进长按状态（⇒ 不扣能、不进冷却），只提示原因。
                    if (FIELD_CHARGE.equals(skill)) {
                        FieldChargeRuntime.StartResult started =
                            FieldChargeRuntime.start(player, effectiveLevel(player, set, skill));
                        if (started != FieldChargeRuntime.StartResult.OK) {
                            notifyBlocked(player, slot, started == FieldChargeRuntime.StartResult.NO_SOURCE
                                ? "createoreexpansion.equip_skill.field_charge_no_source"
                                : "createoreexpansion.equip_skill.field_charge_no_space", 0);
                            continue;
                        }
                    }
                    // 衡元择势（星界套 · 槽位 1）：**分支只在这一 tick 取一次**（需求 §3.2 判定时机
                    // "长按开始时取一次，期间不翻转"）。取到的分支决定这一整段长按走哪张表。
                    if (BALANCE_CHOICE.equals(skill)) {
                        BALANCE_BRANCH.put(id, decideBalanceBranch(player));
                    }
                    // 星芒嬗震（星界套 · 槽位 3）：**按下那一 tick 就是点按** —— 立刻发第 1 枚主波
                    // 并扣点按能量（需求 §3.3(b)"点按（轻触）发出 1 枚主波，不需要长按"）。
                    // 点按能量付不出 ⇒ 按"发动失败"处理（只记日志，不进长按状态、不进冷却），
                    // 与临域充力的两道关同一条纪律：**绝不静默无效**。
                    if (STAR_SHOCK.equals(skill)) {
                        if (!StarShockRuntime.press(player, effectiveLevel(player, set, skill))) {
                            notifyBlocked(player, slot, "createoreexpansion.equip_skill.no_energy", 0);
                            continue;
                        }
                        STAR_SHOCK_HOLD.put(id, 0);
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
                    } else if (BALANCE_CHOICE.equals(skill)) {
                        // 衡元择势（星界套 · 槽位 1）：按**开始时定下的那个分支**跑 ——
                        // 移速分支完全等同蓄能疾骋、图腾分支完全等同绝境守护（同一套数值表）。
                        // 分支在 BALANCE_BRANCH 里定死，这里不重算（否则按住期间会来回切）。
                        applyBalanceChoice(player, set, ticks);
                    } else if (STAR_SHOCK.equals(skill)) {
                        // 星芒嬗震（星界套 · 槽位 3）：点按那一 tick 已经发过第 1 枚、扣过点按能量；
                        // 这里负责长按蓄力（补发主波 + 掷环绕概率）与按 tick 折算的持续耗能。
                        // 返回 false = 能量见底、本次发射已被收尾 ⇒ 与其它技能同一条"见底即断停"路径。
                        STAR_SHOCK_HOLD.put(id, ticks);
                        if (!StarShockRuntime.hold(player, ticks)) {
                            STAR_SHOCK_HOLD.remove(id);
                        }
                    } else if (FIELD_CHARGE.equals(skill)) {
                        // 临域充力（规格 §8 第 3 层）：每 tick 续期"曲柄在转 + 注入器在 + 容量挂在网上"，
                        // 并按"点/秒"累计扣能。非 ACTIVE = 这一 tick 已经收尾（到限 / 见底 / 失效）。
                        FieldChargeRuntime.HoldResult result = FieldChargeRuntime.hold(player, ticks);
                        if (result != FieldChargeRuntime.HoldResult.ACTIVE) {
                            held.remove(slot);
                            ACTIVE.remove(id);
                            if (result == FieldChargeRuntime.HoldResult.ENERGY_OUT) {
                                // 与"能量见底自动断停"同一条口径：见底即把剩余能量清空
                                ArmorEnergy.consume(player, ArmorEnergy.totalEnergy(player));
                            }
                            startCooldown(player, skill, cooldownSecondsOf(player, set, index));
                            continue;
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
                            && isExhausted(player, set, index, ticks)) {
                        held.remove(slot);
                        ACTIVE.remove(id);
                        DASH_SEGMENT.remove(id);
                        LAST_STAND_SEGMENT.remove(id);
                        BALANCE_BRANCH.remove(id);
                        // 星芒嬗震：见底即断停 —— 按下那一 tick 已扣的点按能量**不退**，
                        // 后续的零头也不再追扣（与其它技能"见底即清空"同一条口径）。
                        if (STAR_SHOCK.equals(skill)) {
                            StarShockRuntime.abandon(player);
                            STAR_SHOCK_HOLD.remove(id);
                        }
                        if (FIELD_CHARGE.equals(skill)) {
                            FieldChargeRuntime.finish(player); // 能量见底也要把注入器与曲柄收干净
                        }
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
                BALANCE_BRANCH.remove(id);
                STAR_SHOCK_HOLD.remove(id);
                release(player, set, index, previous);
            }
        }
        // 迅捷拖尾（用户 2026-10-01 报"移速加成期间没有拖尾"）：
        // 拖尾跟着**迅捷 buff 的存续期**走，而不是只在按住的那几 tick —— 松手后 buff 还在（最多 120 秒），
        // 那段时间跑动同样应该有拖尾。到期自动清掉标记。
        // ⚠ 颜色用的是**登记时记住的那一套**（DashTrail#set），**不是**这里现取的玩家当前套：
        //   拖尾在松手后仍持续，现取的话 buff 期间换甲会让颜色当场跳变（见 DashTrail 的说明）。
        DashTrail dash = DASH_TRAIL.get(id);
        if (dash != null) {
            if (player.level().getGameTime() >= dash.until()) {
                DASH_TRAIL.remove(id);
            } else if (player.tickCount % 2 == 0) {
                // 不限定"正在按住"：buff 有效期内跑动就该有拖尾（是否在移动由 dashTrail 自己判）
                ArmorSkillFx.dashTrail(player, dash.set());
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
        BALANCE_BRANCH.remove(player.getUUID());
        // 星芒嬗震（星界套 · 槽位 3）：登出/死亡/换维度 —— 忘掉发射状态（已经飞出去的那些波
        // 照自己的寿命飞完，不追回；长按的剩余零头也不再补扣）。
        STAR_SHOCK_HOLD.remove(player.getUUID());
        StarShockRuntime.forget(player);
        // 宝石套 · 临域充力（规格 §8 第 3 层）：登出/死亡也在退出路径里 ——
        // 会话里存着"被赋能的曲柄 + 注入器"两个坐标，不收尾就会留下一个永远在转的曲柄
        // 和一个永远留在世界里的隐藏方块。
        FieldChargeRuntime.forget(player);
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
            // 中途脱甲/换套 ⇒ 这个槽位解析不出技能 id 了。别的手段照旧（这一支以前就是直接返回），
            // 但**临域充力留下的会话必须收尾**：否则曲柄会一直转、注入器会一直留在世界里。
            FieldChargeRuntime.finish(player);
            return;
        }
        String skillId = skillId(set, index);
        if (skillId == null) {
            FieldChargeRuntime.finish(player);
            return;
        }
        if (!FIELD_CHARGE.equals(skillId)) {
            // 长按期间换了套（槽位号相同、技能却不是临域充力了）：同样必须收尾。
            // 幂等：没有会话时什么都不做。
            FieldChargeRuntime.finish(player);
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
        } else if (skillId.equals(BALANCE_CHOICE)) {
            // 衡元择势（星界套 · 槽位 1）：与蓄能疾骋 / 绝境守护**同一条口径** —— buff 在按住期间
            // 就按段位施加（见 applyBalanceChoice），松手只结算能量与冷却、不再补发。
            // 结算用的表 = **这一次长按开始时定下的那个分支**那张表（需求 §3.2「耗能：走当前分支自己的表」），
            // 所以不能按「松手那一刻的血量」重算分支 —— 那会让玩家按住时看到 A 的 buff、却按 B 的表结账。
            int level = effectiveLevel(player, set, skillId);
            if (balanceBranchOf(player) == BalanceBranch.TOTEM) {
                LastStandConfigs.Config totem = LastStandConfigs.config(level);
                ArmorEnergy.consume(player, holdCost(heldTicks, totem.holdSeconds(), totem.holdTotalCost()));
                startCooldown(player, skillId, totem.cooldownSeconds());
            } else {
                ChargeDashConfigs.Config dash = ChargeDashConfigs.config(level);
                ArmorEnergy.consume(player, holdCost(heldTicks, dash.holdSeconds(), dash.holdTotalCost()));
                startCooldown(player, skillId, dash.cooldownSeconds());
            }
        } else if (skillId.equals(FIELD_CHARGE)) {            // 临域充力（宝石套 · 槽位 3，规格 §8 第 3 层）：用户明确"**中途松开即终止**供能"。
            // 能量是**边按边扣**的（见 FieldChargeRuntime#hold），所以松手只需补上最后不足一步的零头，
            // 随后收尾（移除注入器 + 曲柄立刻静止）并起冷却 25/20/15 秒。
            FieldChargeConfigs.Config config = FieldChargeConfigs.config(effectiveLevel(player, set, skillId));
            FieldChargeRuntime.release(player, heldTicks);
            startCooldown(player, skillId, config.cooldownSeconds());
        } else if (skillId.equals(STAR_SHOCK)) {
            // 星芒嬗震（星界套 · 槽位 3）：波在按下那一 tick 就发出去了、长按期间边按边扣，
            // 所以松手只补上最后不足一步的零头 + 起冷却（10/7/4 秒）。**不补发、不召回**任何波。
            int level = effectiveLevel(player, set, skillId);
            StarShockRuntime.release(player, heldTicks);
            startCooldown(player, skillId, StarShockRuntime.cooldownSeconds(level));
        }
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
    private static void applyBalanceChoice(ServerPlayer player, ArmorSet set, int heldTicks) {
        BalanceBranch branch = BALANCE_BRANCH.get(player.getUUID());
        if (branch == null) {
            // 理论上不会：分支在"开始长按"那一 tick 就写好了。真丢了就按移速分支兜底
            // （不静默什么都不做 —— 那会让玩家看到"按住没反应"）。
            branch = BalanceBranch.SPEED;
        }
        if (branch == BalanceBranch.TOTEM) {
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
    private static void applyTotemBranch(ServerPlayer player, ArmorSet set, int heldTicks) {
        int level = effectiveLevel(player, set, BALANCE_CHOICE);
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
     * 衡元择势在这一 tick 的分支（给松手结算 / 见底结算 / 预览行共用）。
     *
     * <p>没有记录时按<b>当前状态现算</b>：那只可能发生在"长按状态丢了但槽位还在"的极窄窗口，
     * 而"松手时按哪个分支结算"必须有一个答案（凭空猜移速分支会让图腾分支白嫖一次免扣能）。</p>
     */
    private static BalanceBranch balanceBranchOf(ServerPlayer player) {
        BalanceBranch recorded = BALANCE_BRANCH.get(player.getUUID());
        return recorded != null ? recorded : decideBalanceBranch(player);
    }



    /**
     * <b>衡元择势的分支</b>（需求 §3.2 的"选择判断"）。
     *
     * <p>{@link #SPEED} = 蓄能疾骋那一支（移速），{@link #TOTEM} = 绝境守护那一支（不死图腾）。</p>
     */
    public enum BalanceBranch {
        /** 移速分支（血量 ≥ 半血且周围敌人不多）。 */
        SPEED,
        /** 图腾分支（血量 &lt; 半血，或周围敌对实体过多）。 */
        TOTEM
    }

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
     * 长按开始时取一次的分支判定结果（"长按期间不再翻转"，需求 §3.2 判定时机）。
     *
     * <p>存在这里的理由：用户明确的观感要求是"按住期间不来回切" —— 如果每 tick 现算，
     * 血量在长按期间被打下去/回上来（图腾分支自己就会回血）会让分支当场跳变，
     * 观感与扣能都乱。</p>
     */
    private static final Map<UUID, BalanceBranch> BALANCE_BRANCH = new HashMap<>();

    /**
     * "周围敌对实体过多"的判定立方体<b>半边</b>（格）—— 需求 §3.2 推断值 #6 取 <b>8</b>
     * （边长 = 2×8+1 = 17，沿用本仓 {@code StressSourceRegistry} 的"边长 = 2r+1"惯例）。
     */
    public static final int BALANCE_ENEMY_RADIUS = 8;

    /** "敌对实体过多"的数量门槛（需求 §3.2 推断值 #6 取 <b>≥ 5</b>）。 */
    public static final int BALANCE_ENEMY_THRESHOLD = 5;

    /** "低于半血"的阈值（需求 §3.2 推断值 #5）：{@code HP/maxHP < 0.50}；**恰好 50% 算半血以上**。 */
    public static final double BALANCE_LOW_HEALTH_RATIO = 0.50D;

    /**
     * <b>长按开始那一 tick 的分支判定</b>（需求 §3.2 表，逐字）：
     * <pre>
     * 血量 HP/maxHP &lt; 0.50            ⇒ 图腾分支
     * 或 立方体半边 8 格内 Enemy ≥ 5   ⇒ 图腾分支
     * 否则                            ⇒ 移速分支（含"恰好 50%"）
     * </pre>
     *
     * <p><b>两侧都可调用</b>（HUD 的长按预览行要用它显示"这次会走哪一支"）：血量判据两侧同源；
     * 敌对实体数只有服务端能查（{@link #countNearbyEnemies} 需要 {@code ServerLevel}），
     * 客户端因此按"血量那一半"判定 —— 也就是说，<b>服务端才是权威</b>，客户端的预览在
     * "血量 ≥ 半血但周围敌人很多"这一种情形下可能显示成移速分支。</p>
     *
     * @return 二选一的结果（永不为 {@code null}）
     */
    public static BalanceBranch decideBalanceBranch(net.minecraft.world.entity.player.Player player) {
        double maxHealth = player.getMaxHealth();
        double ratio = maxHealth <= 0.0D ? 1.0D : player.getHealth() / maxHealth;
        if (ratio < BALANCE_LOW_HEALTH_RATIO) {
            return BalanceBranch.TOTEM;
        }
        if (player instanceof ServerPlayer server && countNearbyEnemies(server) >= BALANCE_ENEMY_THRESHOLD) {
            return BalanceBranch.TOTEM;
        }
        return BalanceBranch.SPEED;
    }

    /**
     * 以玩家为中心、{@code (2r+1)³} 立方体内的敌对实体数量（{@code entity instanceof Enemy}）。
     *
     * <p><b>为什么查 {@code LivingEntity} 再过滤，而不是 {@code getEntitiesOfClass(Enemy.class, …)}</b>：
     * {@code Enemy} 是<b>接口</b>，而 {@code EntityGetter#getEntitiesOfClass} 的类型参数被限定为
     * {@code T extends Entity}，传接口进不去（javac 报"找不到合适的方法"，2026-10-02 实测）。
     * 换成"按类查 + 谓词过滤"后判据与需求写的 {@code entity instanceof Enemy} <b>逐字一致</b>：
     * 不排除创造模式玩家、不额外排除任何东西（原版返回的列表本身不含 dead 实体）。</p>
     */
    public static int countNearbyEnemies(ServerPlayer player) {
        double r = BALANCE_ENEMY_RADIUS;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
            player.getX() - r, player.getY() - r, player.getZ() - r,
            player.getX() + r + 1.0D, player.getY() + r + 1.0D, player.getZ() + r + 1.0D);
        return player.serverLevel()
            .getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box,
                e -> e instanceof net.minecraft.world.entity.monster.Enemy)
            .size();
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
        int level = effectiveLevel(player, set, skill);
        if (FIELD_CHARGE.equals(skill)) {
            // 临域充力（宝石套槽位 3，规格 §8 第 3 层）：数值源 = FieldChargeConfigs 的"点/秒"，
            // 累计口径 = floor(heldTicks × energyPerSecond / 20)（见 FieldChargeConfigs#costAfter 的推导：
            // 与 holdCost(held, durationSeconds, durationSeconds × energyPerSecond) 逐值等价）。
            return FieldChargeConfigs.costAfter(heldTicks, FieldChargeConfigs.config(level));
        }
        if (FALL_GUARD.equals(skill)) {
            FallGuardConfigs.Config config = FallGuardConfigs.config(level);
            return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
        }
        if (CHARGE_DASH.equals(skill)) {
            ChargeDashConfigs.Config config = ChargeDashConfigs.config(level);
            return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
        }
        if (BALANCE_CHOICE.equals(skill)) {
            // 衡元择势：按**这一次长按开始时定下的分支**取表（与 release 同一处判据），
            // 否则"见底自动断停"会用错表算累计花费，提前或延后停下。
            if (balanceBranchOf(player) == BalanceBranch.TOTEM) {
                LastStandConfigs.Config totem = LastStandConfigs.config(level);
                return holdCost(heldTicks, totem.holdSeconds(), totem.holdTotalCost());
            }
            ChargeDashConfigs.Config dash = ChargeDashConfigs.config(level);
            return holdCost(heldTicks, dash.holdSeconds(), dash.holdTotalCost());
        }
        LastStandConfigs.Config config = LastStandConfigs.config(level);
        return holdCost(heldTicks, config.holdSeconds(), config.holdTotalCost());
    }

    /** 该技能该起的冷却秒数（给"能量见底自动断停"用，与松手结算同一处取值）。 */
    private static int cooldownSecondsOf(ServerPlayer player, ArmorSet set, int index) {
        String skill = skillId(set, index);
        int level = effectiveLevel(player, set, skill);
        if (FIELD_CHARGE.equals(skill)) {
            // 临域充力（宝石套槽位 3，规格 §2.2"释放后冷却"）：25 / 20 / 15 秒
            return FieldChargeConfigs.config(level).cooldownSeconds();
        }
        if (FALL_GUARD.equals(skill)) {
            return FallGuardConfigs.config(level).cooldownSeconds();
        }
        if (CHARGE_DASH.equals(skill)) {
            return ChargeDashConfigs.config(level).cooldownSeconds();
        }
        if (BALANCE_CHOICE.equals(skill)) {
            // 衡元择势：冷却同样按"这一次长按开始时定下的那个分支"取（移速 60/45/30、图腾 30/30/30）
            return balanceBranchOf(player) == BalanceBranch.TOTEM
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
    private static boolean isExhausted(ServerPlayer player, ArmorSet set, int index, int heldTicks) {
        int available = ArmorEnergy.totalEnergy(player);
        if (available <= 0) {
            return true;
        }
        String skill = skillId(set, index);
        if (FIELD_CHARGE.equals(skill)) {
            FieldChargeConfigs.Config config =
                FieldChargeConfigs.config(effectiveLevel(player, set, skill));
            return FieldChargeConfigs.stepCost(heldTicks, config) > available;
        }
        if (STAR_SHOCK.equals(skill)) {
            // 星芒嬗震与临域充力同族：**边按边扣**，所以比的也是"下一步还扣得起吗"。
            // 点按那 400 是"按下那一 tick 就付掉了"的（不参与这次比较），所以这里比的就是
            // 下一 tick 的增量（100/20 = 每 20 tick 5 点）与当前可用能量的关系。
            int level = effectiveLevel(player, set, skill);
            int step = StarShockRuntime.holdCost(level, heldTicks + 1)
                - StarShockRuntime.holdCost(level, heldTicks);
            return step > available;
        }
        return accumulatedCost(player, set, index, heldTicks) >= available;
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
     * <b>逐技能</b>问 {@link ArmorSkillLevels}（宝石套 绝境守护 1 / 蓄能疾骋 2 / 临域充力 1；
     * 翠玉套两条仍为 1）。
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
            // 与工具侧口径**逐字一致**（{@code SkillEnergyCost#effectiveLevel:34-35} 的
            // {@code Math.min(..., 2)}）：技艺提升/记忆回溯 3 级及以上，提升量/削减量一律按 2 计。
            // 合法附魔等级只有 0/1/2（{@code data/createoreexpansion/enchantment/skill_boost.json}
            // 的 {@code max_level} = 2）⇒ 这是**零数值变化**的健壮性补丁；防的是"存档/命令塞进来的
            // 越级附魔"把加减量放大（例如 +9 让"基准 + 提升"早就越过钳位，掩盖钳位本身是否生效）。
            int boost = Math.min(com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
                .skillBoostLevel(stack), 2);
            int regression = Math.min(com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
                .skillRegressionLevel(stack), 2);
            best = Math.max(best, base + boost - regression);
        }
        return Math.max(1, Math.min(EQUIPMENT_SKILL_MAX_LEVEL, best));
    }

    /** 该套在第 index 个装备槽位上有没有技能（雷鸣套的 1、2 通星界 ⇒ 见实现）。 */
    private static boolean hasSkill(ArmorSet set, int index) {
        return skillId(set, index) != null;
    }

    /**
     * <b>装备技能等级的唯一钳位上限 = 3</b> —— 用户 2026-10-01 口径
     * "装备技能 3 级封顶（工具才 5 级）"；用户 2026-10-02 追加："<b>单件显示也要钳住</b>"。
     *
     * <p><b>它同时钳两个出口，不许再有第二个平行的上限常量</b>：</p>
     * <ol>
     *   <li><b>真实等级</b>（{@link #effectiveLevel}，参与一切结算）：基准 + 技艺提升 − 记忆回溯
     *       —— "基准 2 + 技艺提升 1 = 3" 恰好等于钳位值，钳位吃没吃到只看这一处；</li>
     *   <li><b>预测等级</b>（{@link #predictedLevelOf}，护甲 tooltip 的"没穿整套"分支）：
     *       <b>与真实等级同一个常量</b>。历史上这里是另一个常量 4（理由：雷鸣套套基准 4 要显紫），
     *       于是单件 tooltip 会出现 <b>IV</b> —— 而等级本来就封顶 3，附魔超限时那件显示的数字
     *       大于任何真实等级（用户 2026-10-02 报"单独在装备上的显示没有修"）。
     *       <b>预测只是"穿上之后大概几级"的预告</b>，上限不可能高于真实等级，故合并为一处真源；
     *       代价是雷鸣套（套基准 4，本套技能尚未落地）的悬停预告也会钳在 III 蓝 —— 那是口径
     *       要求的"单件显示不许超过 3"。</li>
     * </ol>
     * <p>将来若有人把某个配置类的 MAX_LEVEL 调成别的值，钳位<b>不会跟着漂</b>：装备技能只认本常量。</p>
     */
    public static final int EQUIPMENT_SKILL_MAX_LEVEL = 3;

    /**
     * {@link #EQUIPMENT_SKILL_MAX_LEVEL} 的别名 —— 给注册表（{@code AllSkills}）用的语义化名字：
     * 装备技能条目的 {@code maxLevel(...)} 必须与真实等级钳位<b>同一处取值</b>，
     * 否则 "登记的上限 5 / 实际钳 3" 会在 tooltip 与附魔提升路径上各说一套。
     */
    public static final int MAX_EQUIPMENT_SKILL_LEVEL = EQUIPMENT_SKILL_MAX_LEVEL;

    /**
     * <b>没穿这件事的时候，按这一件预测的技能等级</b>（用户 2026-10-02 口径，护甲 tooltip 专用）。
     *
     * <p>公式与钳位都<b>只在这里写一遍</b>（"技能等级只有一个出处"）：</p>
     * <pre>
     *   clamp( ArmorSkillLevels.baseLevelOf(这件所属的套, 该技能) + 该件技艺提升 − 该件记忆回溯,
     *          1, {@link #EQUIPMENT_SKILL_MAX_LEVEL} )
     * </pre>
     * <p><b>钳位与真实等级共用同一个常量</b>（{@link #EQUIPMENT_SKILL_MAX_LEVEL} = 3，用户
     * 2026-10-02："单件显示也要钳住"）：预告的数字不可能高于穿上之后的真实等级，所以不允许存在
     * 第二个平行的预测上限（历史上是 4，单件 tooltip 因此会显示 IV 紫）。</p>
     *
     * <p><b>为什么只读被悬停的那一件</b>（而不是像 {@link #effectiveLevel} 那样四件取最大）：
     * 玩家没穿整台时，"另外三件"要么不在身上、要么根本不是这一套，逐件取最大既取不到也不该取；
     * 悬停时唯一可读的就是这一件。穿上整套之后走的<b>不是</b>本方法，而是真实等级
     * {@link #levelOf(Player, String)}（护甲 tooltip 的三分支见 {@code ArmorSkillTooltipHandler}）。</p>
     *
     * <p><b>它不参与任何真实技能结算</b>：只给显示层预告一个颜色/罗马数字，
     * 不改能量、不改冷却、不改配置取值（真正的等级永远是 {@link #levelOf}）。</p>
     *
     * @param stack   被悬停的那一件护甲；不是本模组四套护甲 ⇒ 0
     * @param skillId 技能 id 的 path（如 {@code field_charge}，与 {@link #levelOf} 同形）
     * @return 1~{@link #EQUIPMENT_SKILL_MAX_LEVEL}；参数不适用 ⇒ 0
     */
    public static int predictedLevelOf(ItemStack stack, String skillId) {
        ArmorSet set = ArmorSet.of(stack);
        if (set == null || skillId == null) {
            return 0;
        }
        int base = ArmorSkillLevels.baseLevelOf(set,
            com.hjmmd_8.createoreexpansion.common.CoeCore.modLoc(skillId));
        // 与 effectiveLevel 同一处的 2 级封顶口径（越级附魔不放大加减量）
        int boost = Math.min(com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
            .skillBoostLevel(stack), 2);
        int regression = Math.min(com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments
            .skillRegressionLevel(stack), 2);
        return Math.max(1, Math.min(EQUIPMENT_SKILL_MAX_LEVEL, base + boost - regression));
    }

    /**
     * 该套在第 index 个装备槽位上的技能 id。
     *
     * <p>顺序 = 槽位顺序（{@link ArmorSkillProvider#SLOT_BASE} 起），与
     * {@link ArmorSkillProvider#skillIdsOf} 的表<b>必须逐字同序</b>：那张表决定客户端轮询哪个槽位、
     * HUD 列哪几行，这里决定"按下这个槽位到底跑哪个技能"。</p>
     *
     * <p>翠玉套 {@code fall_guard / charge_dash}；宝石套（用户 2026-10-01 更正后的编排）
     * {@code last_stand / charge_dash / field_charge} —— 蓄能疾骋是<b>从翠玉套移植</b>的同一条技能
     * （同一个 id、同一套数值），因此它同时住在两套的同一段槽位空间里。
     * 星界套 {@code balance_choice / field_charge / star_shock}；雷鸣套
     * {@code balance_choice / field_charge}（<b>只有两个槽位</b>：用户 2026-10-02
     * 「雷鸣套装：1、2 通星界」，槽位 3 雷鸣威震<b>故意未登记、待做</b>）。</p>
     */
    private static @Nullable String skillId(ArmorSet set, int index) {
        if (set == ArmorSet.JADE_TOPAZ) {
            return switch (index) {
                case 0 -> FALL_GUARD;
                case 1 -> CHARGE_DASH;
                default -> null;
            };
        }
        if (set == ArmorSet.SAPPHIRE_RUBY) {
            // 用户 2026-10-01 更正后的宝石套编排（三条，槽位顺序即此处 case 顺序，
            // 必须与 ArmorSkillProvider.SET_SKILL_IDS[SAPPHIRE_RUBY] 逐字同序）：
            //   槽位 1 = 绝境守护、槽位 2 = 蓄能疾骋（从翠玉套移植）、槽位 3 = 临域充力。
            return switch (index) {
                case 0 -> LAST_STAND;
                case 1 -> CHARGE_DASH;
                case 2 -> FIELD_CHARGE;
                default -> null;
            };
        }
        if (set == ArmorSet.ASTRAL) {
            // 星界套（用户 2026-10-02 星界轮，三条，槽位顺序即此处 case 顺序，
            // 必须与 ArmorSkillProvider.SET_SKILL_IDS[ASTRAL] 逐字同序）：
            //   槽位 1 = 衡元择势、槽位 2 = 临域充力（**同一个技能 id 的高等级形态**）、
            //   槽位 3 = 星芒嬗震。
            // 顺序错会**静默取错配置表**（编译通过、只有按住键看 HUD 才暴露）。
            return switch (index) {
                case 0 -> BALANCE_CHOICE;
                case 1 -> FIELD_CHARGE;
                case 2 -> STAR_SHOCK;
                default -> null;
            };
        }
        if (set == ArmorSet.THUNDER) {
            // 雷鸣套（用户 2026-10-02 雷鸣轮）：「雷鸣套装：1、2 通星界」——
            //   槽位 1 = 衡元择势、槽位 2 = 临域充力 II（**同一个技能 id 的高等级形态**）。
            // 执行体一律按 **skillId** 分派（本文件里的 FIELD_CHARGE.equals(skill) /
            // BALANCE_CHOICE.equals(skill) 等分支），**不按套**，所以复用这两个 id 之后
            // 雷鸣套上它们**天然生效**，与星界套上行为完全一致 —— 只差基准等级 3 vs 2
            // （3 是装备技能封顶，见 EQUIPMENT_SKILL_MAX_LEVEL）。
            // ⚠ **只有两个 case，不许给 index 2 造任何东西**：槽位 3（雷鸣威震）本轮
            //   **故意不登记（待做）** ⇒ default -> null ⇒ hasSkill(THUNDER, 2) 为 false、
            //   装备段键三对雷鸣套不响应、HUD 只列两行。
            // 顺序必须与 ArmorSkillProvider.SET_SKILL_IDS[THUNDER] 逐字同序。
            return switch (index) {
                case 0 -> BALANCE_CHOICE;
                case 1 -> FIELD_CHARGE;
                default -> null;
            };
        }
        return null;
    }
}
