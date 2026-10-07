package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.config.equipment.StarShockConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

/**
 * <b>星芒嬗震一次发射的结算日志出口</b>（2026-10-05 行为零变化拆分，从 {@code StarShockRuntime} 的
 * {@code logSettlement} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这一发到底发生了什么</b> —— 作者实机验收的唯一凭据
 * （"把三处日志合并成一行，字段列清楚，别打三行"）。四个收尾路径共用它：
 * 松手 / 能量见底断停 / 离场·死亡 / 换套丢状态。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、
 * {@code Cast} → {@link StarShockCast}（同一个值类型，升为顶层包级私有类）与
 * {@code fmt2(...)} → {@code StarShockRuntime.fmt2(...)}（格式化只有宿主那一处）。
 * <b>日志文案、字段顺序与两张输出通道都没动</b>：一直走 {@link WaveDiag}（波系统唯一日志出口），
 * 外加一行只发给发射者本人的游戏内提示（{@link StarShockDebug#reportOrbit}，默认关闭）——
 * 后者是"可整体移除"的临时调试半区，删类 + 删本方法里那一行即可。</p>
 */
final class StarShockReport {

    private StarShockReport() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * <b>本次发射的唯一一行结算日志</b>（作者实机验收的唯一凭据；作者 2026-10-02 裁定第 4 条
     * "把三处日志合并成一行，字段列清楚，别打三行"）。走 {@link WaveDiag}（波系统唯一日志出口）。
     *
     * <p>一行里同时给出（顺序即字段顺序）：</p>
     * <ol>
     *   <li><b>实际蓄力</b>：按住 tick 数 / 秒数 / 该级蓄力上限秒数 / <b>t</b>；</li>
     *   <li><b>共几枚主波</b>（{@code cast.fired} —— 这就是"3 级满蓄力到底出几枚"的凭据）；</li>
     *   <li><b>环绕波滚没滚到</b>：掷骰次数 / 本次最高概率 / 命中次数 / 生成枚数
     *       ⇒ {@code 命中 0} = "没滚到"；{@code 命中 > 生成} = "滚到了但没生成"；
     *       {@code 生成 > 0} 但看不到 = 观感问题（"生成了但立刻消散"另有一行消散日志，见
     *       {@code AbstractChargerWaveEntity#tick} 的环绕要素收尾）；</li>
     *   <li><b>本次总耗能</b>：总额 + 点按部分 + 长按部分（点按 = 400，长按部分 = 总 − 400）。</li>
     * </ol>
     *
     * @param reason    收尾原因（"松手" / "能量见底断停"）
     * @param cast      本次发射状态（调用方已经从 {@code StarShockRuntime#CASTS} 摘掉）
     * @param heldTicks 收尾时的按住 tick 数
     * @param paid      本次发射实际扣掉的装备能量（点）
     */
    static void logSettlement(String reason, StarShockCast cast, int heldTicks, int paid) {
        int waveLevel = StarShockConfigs.waveLevelFor(cast.level);
        double t = StarShockConfigs.chargeProgress(heldTicks, cast.config);
        int holdPart = Math.max(0, paid - cast.config.tapCost());
        WaveDiag.trace(
            "星芒嬗震结算（{}）：技能 {} 级 → {} 级波（{}），实际蓄力 {} tick = {} 秒 / 上限 {} 秒（t={}），"
                + "共发 {} 枚主波；环绕波：掷骰 {} 次（本次最高概率 {}）命中 {} 次 → 生成 {} 枚；"
                + "本次总耗能 {} 点（点按 {} + 长按 {}）",
            reason, cast.level, waveLevel, WaveLevels.glyph(waveLevel),
            heldTicks, StarShockRuntime.fmt2(heldTicks / (double) ChargeConfigs.TICKS_PER_SECOND),
            cast.config.chargeSeconds(),
            StarShockRuntime.fmt2(t),
            cast.fired,
            cast.orbitRolls, StarShockRuntime.fmt2(cast.orbitPeakChance), cast.orbitHits, cast.orbitSpawned,
            paid, cast.config.tapCost(), holdPart);
        // 游戏内调试提示（临时、可整体移除；默认关闭，只有 /orbitdebug on 过的发射者本人会收到）：
        // 让作者<b>不用切窗口看日志</b>就能判"这一发有没有环绕波生成"。走聊天栏、只发本人、不广播；
        // 一个字符都不写日志文件（波日志的唯一出口仍是 WaveDiag）。
        // 移除方式见 StarShockDebug 类尾注释（删本类 + 删下面这一行）。
        if (cast.player instanceof net.minecraft.server.level.ServerPlayer serverCaster) {
            StarShockDebug.reportOrbit(serverCaster, cast.orbitRolls, cast.orbitHits, cast.orbitSpawned);
        }
    }
}
