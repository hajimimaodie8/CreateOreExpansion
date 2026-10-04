package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.field.FieldChargeRuntime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
 * <h2>本类的形状（2026-10-05 行为零变化拆分）</h2>
 * <p>本类保留<b>公开出口与运行态</b>（技能 id 常量、等级取值、冷却记账、长按状态、衡元择势的分支判定），
 * 按职责域拆出的同包类只做搬运、不改口径：</p>
 * <ul>
 *   <li>{@link ArmorSkillSlots} —— 套 × 槽位 → 技能 id 的槽位表；</li>
 *   <li>{@link ArmorSkillHoldLoop} —— 一个槽位每 tick 的状态迁移（开始 / 推进 / 断停 / 松手）；</li>
 *   <li>{@link ArmorSkillBuffs} —— 按住期间的段位 buff 与迅捷拖尾的存续；</li>
 *   <li>{@link ArmorSkillSettlement} —— 松手 / 见底的结算口径（算钱、断停判据、冷却秒数）；</li>
 *   <li>{@link ArmorSkillPressDiag} —— 【临时诊断】按下状态日志（可整体删除）。</li>
 * </ul>
 * <p>per-tick 的阶段顺序与结算汇合点未变：{@link #tick} 仍是"诊断 → 逐槽位推进 → 拖尾推进 → 清表"。</p>
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
    static final Map<UUID, Integer> STAR_SHOCK_HOLD = new HashMap<>();

    /** 发动被挡的提示节流：玩家#槽位 → 上次提示的 tick（每 20 tick 最多一次）。 */
    private static final Map<String, Integer> BLOCK_NOTIFY_TICK = new HashMap<>();

    /** 当前处于"长按生效中"的玩家与技能（虚衡坠护的主动豁免要读它）。 */
    static final Map<UUID, String> ACTIVE = new HashMap<>();

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
        ArmorSkillPressDiag.logPressedChanges(player, set, id);

        for (int index = 0; index < ArmorSkillProvider.SLOT_COUNT; index++) {
            int slot = ArmorSkillProvider.SLOT_BASE + index;
            // 一个槽位的完整状态迁移（开始 / 推进 / 断停 / 松手）逐字搬到 ArmorSkillHoldLoop#tickSlot。
            ArmorSkillHoldLoop.tickSlot(player, set, id, held, slot, index);
        }
        // 迅捷拖尾（用户 2026-10-01 报"移速加成期间没有拖尾"）：跟着迅捷 buff 的存续期走，
        // 理由与上色口径（登记时记住的那一套）逐字搬到 ArmorSkillBuffs#advanceDashTrail。
        ArmorSkillBuffs.advanceDashTrail(player, id);

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
            if (skillId.equals(ArmorSkillSlots.skillId(set, index))) {
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
     * 衡元择势在这一 tick 的分支（给松手结算 / 见底结算 / 预览行共用）。
     *
     * <p>没有记录时按<b>当前状态现算</b>：那只可能发生在"长按状态丢了但槽位还在"的极窄窗口，
     * 而"松手时按哪个分支结算"必须有一个答案（凭空猜移速分支会让图腾分支白嫖一次免扣能）。</p>
     */
    static BalanceBranch balanceBranchOf(ServerPlayer player) {
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
     * 长按开始时取一次的分支判定结果（"长按期间不再翻转"，需求 §3.2 判定时机）。
     *
     * <p>存在这里的理由：用户明确的观感要求是"按住期间不来回切" —— 如果每 tick 现算，
     * 血量在长按期间被打下去/回上来（图腾分支自己就会回血）会让分支当场跳变，
     * 观感与扣能都乱。</p>
     */
    static final Map<UUID, BalanceBranch> BALANCE_BRANCH = new HashMap<>();

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
     * 按住时"发动不了"的可见反馈（动作栏 + 日志）。
     *
     * <p>为什么必须有：用户 2026-10-01 报"有的时候按住 Alt+R 根本不生效、松开也没给" ——
     * 真实原因是**冷却中**与**能量为 0** 这两条静默 `continue`，玩家感受就是"随机失效"。
     * 现在会明说原因，并且**按同一槽位每 20 tick 只提示一次**（按住不放也不会刷屏）。</p>
     */
    static void notifyBlocked(ServerPlayer player, int slot, String langKey, int seconds) {
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

}
