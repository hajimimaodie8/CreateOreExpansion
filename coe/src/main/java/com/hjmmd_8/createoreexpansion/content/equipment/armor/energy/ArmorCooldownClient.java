package com.hjmmd_8.createoreexpansion.content.equipment.armor.energy;

import java.util.HashMap;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;

import net.minecraft.client.Minecraft;

/**
 * <b>客户端冷却表</b>（用户 2026-10-01 报"冷却时 HUD 那一行没被替换"的根因修复）。
 *
 * <h2>为什么需要一份客户端副本</h2>
 * <p>冷却存在<b>服务端</b>玩家的持久数据里（{@code createoreexpansion:equip_cd_<技能>}），
 * 而 {@code Entity#getPersistentData()} <b>不会同步到客户端</b> ⇒ 客户端读它恒为 0，
 * HUD 于是永远认为"就绪"，那一行自然不会被替换。</p>
 *
 * <p>修法：服务端在<b>起冷却的那一刻</b>发一个 {@link EquipCooldownPayload}（技能 id + 秒数），
 * 客户端记下"到期时刻"并<b>自己倒数</b> ⇒ 一个冷却只发一个包，之后零流量。</p>
 *
 * <p>本类只在客户端被加载（{@code EquipCooldownPayload#handle} 里引用），服务端不会碰它。</p>
 *
 * @since 1.0.0
 */
public final class ArmorCooldownClient {

    /** 技能 id → 到期时刻（客户端 {@code level.getGameTime()} 口径）。 */
    private static final Map<String, Long> UNTIL = new HashMap<>();

    private ArmorCooldownClient() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 收到服务端冷却通知：按客户端时钟记下到期时刻。 */
    public static void apply(String skillId, int seconds) {
        if (skillId == null || seconds <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        UNTIL.put(skillId, now + seconds * (long) ChargeConfigs.TICKS_PER_SECOND);
    }

    /** 该技能还剩几秒（向上取整；没冷却或已到点返回 0）。 */
    public static int remainingSeconds(String skillId) {
        if (skillId == null) {
            return 0;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0;
        }
        Long until = UNTIL.get(skillId);
        if (until == null) {
            return 0;
        }
        long left = until - minecraft.level.getGameTime();
        if (left <= 0) {
            UNTIL.remove(skillId);
            return 0;
        }
        // 向上取整（原式 (left + 19) / 20 的等价改写）：除以唯一换算因数，加数 = 除数 − 1。
        return (int) ((left + ChargeConfigs.TICKS_PER_SECOND - 1) / ChargeConfigs.TICKS_PER_SECOND);
    }

    /** 离开世界时清空（下次进来由服务端重新通知）。 */
    public static void clear() {
        UNTIL.clear();
    }
}
