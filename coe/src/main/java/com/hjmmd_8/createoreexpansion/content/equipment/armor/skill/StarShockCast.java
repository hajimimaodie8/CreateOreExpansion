package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.StarShockConfigs;

/**
 * <b>星芒嬗震一次发射的状态</b>（2026-10-05 行为零变化拆分，从 {@code StarShockRuntime} 的私有嵌套类
 * {@code Cast} <b>逐字搬出</b>；搬出时只把 {@code private} 放宽到包级私有，字段含义、注释与构造
 * 一律未动）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这一次按键发射累计了什么</b> —— 批次号、发射者、定死的配置与技能
 * 等级、已发几枚主波、已扣多少能量，以及环绕波的掷骰 / 命中 / 生成三个计数与最近一次按住 tick
 * 数。登记表 {@code CASTS}（玩家 → 本次发射）仍住在 {@link StarShockRuntime}：那是宿主的运行态，
 * 本类只是它的值类型。</p>
 *
 * <p>为什么升为顶层包级私有类：{@link StarShockWaveLauncher}（发波）与
 * {@link StarShockReport}（结算日志）都要读同一份状态，而嵌套类的 {@code private} 字段只有
 * <b>外层类</b>能访问 —— 跨顶层类必须放宽可见性。</p>
 */
final class StarShockCast {

    /** 本次发射的批次号（> 0）。 */
    final int batch;

    /**
     * <b>发射者本人</b>（服务端玩家）。
     *
     * <p>为什么要存它：结算时要把"这一发掷骰/命中/生成了多少"作为<b>游戏内提示</b>发给发射者
     * （{@link StarShockDebug#reportOrbit}，只发本人、不广播），而 {@code StarShockRuntime#abandon}
     * （能量见底断停）与 {@code StarShockRuntime#forget}（离场/死亡）这两条收尾路径<b>拿不到 player
     * 参数</b> —— 它们只有 {@code CASTS.remove(...)} 的结果。存在这里就三条路径一视同仁。</p>
     */
    final net.minecraft.server.level.ServerPlayer player;

    /** 该等级的配置在**发射那一刻**定死（中途换甲不会让还在飞的波改变编组语义）。 */
    final StarShockConfigs.Config config;

    /** 该技能的逐技能等级（1~3）。 */
    final int level;

    /** 已经发出过几枚主波（含点按那第一枚）。 */
    int fired;

    /** 已经扣掉的能量（点按 + 长按增量）。 */
    int paid;

    /**
     * 本次发射里环绕波<b>滚了几次骰</b>（每枚主波各自一次，含点按那第一枚 ——
     * 点按 t = 0 ⇒ 概率 0，那次滚骰必然不中，但它仍然算"问过一次"）。
     */
    int orbitRolls;

    /** 环绕波掷骰<b>命中</b>了几次（概率通过）。
     *  {@code hits < rolls} = "没滚到"；{@code spawns < hits} = "滚到了但没生成"。 */
    int orbitHits;

    /** 环绕波真的<b>生成了</b>几枚（命中后建实体并加入世界成功）。 */
    int orbitSpawned;

    /** 本次发射用过的<b>最高</b>环绕概率（日志用：没滚到时也能看出"当时的概率是多少"）。 */
    double orbitPeakChance;

    /**
     * 最近一次 {@code StarShockRuntime#hold} 收到的按住 tick 数（0 = 只有点按那一 tick）。
     *
     * <p>为什么在这里也存一份：{@code StarShockRuntime#abandon}（能量见底断停）拿不到
     * {@code heldTicks} 参数，而结算日志必须能写出"实际蓄了多久"（作者只有日志可验收）。</p>
     */
    int lastHeldTicks;

    StarShockCast(net.minecraft.server.level.ServerPlayer player, int batch,
        StarShockConfigs.Config config, int level) {
        this.player = player;
        this.batch = batch;
        this.config = config;
        this.level = level;
    }
}
