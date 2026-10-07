package com.hjmmd_8.createoreexpansion.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能冷却时长注册表 —— 全模组统一的冷却时间管理系统。
 *
 * <p>自由度高：任何武器想自定义技能冷却，只需在 {@code CoeItems} 中注册一次：</p>
 * <pre>
 * SkillCooldowns.register(CoeItems.JADE_SWORD.get(), SkillCooldowns.TICKS_PER_SECOND); // 1 秒
 * </pre>
 *
 * <p>⚠ 上例原写作字面量 {@code 20}；批 15 起换算因数只有 {@link #TICKS_PER_SECOND} 一处，
 * 示例也跟着按名引用（行为一字不变）。</p>
 *
 * <p>触发端（如 {@code HurtLivingEntityHandler}）统一从注册表读取冷却时长。</p>
 */
public final class SkillCooldowns {

    private SkillCooldowns() {
    }

    /**
     * <b>一秒 = 多少 tick（全仓唯一的换算因数）</b> —— 2026-10-06 COE 批 15 收敛后的唯一真源。
     *
     * <p>它住在 {@code core} 是因为「秒 → tick」是共享算术：{@code :coe} 的电荷/技能/护甲三族
     * 都要用它，而 {@code core} 里的 {@code ToolSkillCooldown} 也要用它；反过来 {@code core}
     * 不许 import {@code :coe}（分层红线）⇒ 常量只能住这里，由 {@code :coe} 的
     * {@code ChargeConfigs#TICKS_PER_SECOND}（别名，仍是原引用路径）与各调用点引用。
     * <b>仓里任何地方要做这个换算都必须引用本常量，不许再写 {@code 20} 字面量。</b></p>
     */
    public static final int TICKS_PER_SECOND = 20;

    /** 默认冷却时长（tick）= {@link #TICKS_PER_SECOND}（1 秒），由唯一换算因数算出。 */
    public static final int DEFAULT_COOLDOWN_TICKS = TICKS_PER_SECOND;

    /** 武器 Item → 技能冷却时长（tick） */
    private static final Map<Item, Integer> REGISTRY = new HashMap<>();

    /**
     * 注册武器对应的技能冷却时长。
     *
     * @param item        武器物品
     * @param cooldownTicks 冷却时长（tick，20 tick = 1 秒）
     */
    public static void register(Item item, int cooldownTicks) {
        REGISTRY.put(item, cooldownTicks);
    }

    /**
     * 获取武器的技能冷却时长。
     *
     * @return 注册的冷却时长；未注册时返回默认 1 秒
     */
    public static int getTicks(ItemStack stack) {
        return stack.isEmpty() ? DEFAULT_COOLDOWN_TICKS
                : REGISTRY.getOrDefault(stack.getItem(), DEFAULT_COOLDOWN_TICKS);
    }

    /**
     * 获取武器的技能冷却时长。
     *
     * @return 注册的冷却时长；未注册时返回默认 1 秒
     */
    public static int getTicks(Item item) {
        return item == null ? DEFAULT_COOLDOWN_TICKS
                : REGISTRY.getOrDefault(item, DEFAULT_COOLDOWN_TICKS);
    }
}
