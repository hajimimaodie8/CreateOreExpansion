package com.hjmmd_8.createoreexpansion.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能冷却时长注册表 —— 全模组统一的冷却时间管理系统。
 *
 * <p>自由度高：任何武器想自定义技能冷却，只需在 {@code AllItems} 中注册一次：</p>
 * <pre>
 * SkillCooldowns.register(AllItems.JADE_SWORD.get(), 20); // 1 秒
 * </pre>
 *
 * <p>触发端（如 {@code HurtLivingEntityHandler}）统一从注册表读取冷却时长。</p>
 */
public final class SkillCooldowns {

    private SkillCooldowns() {
    }

    /** 默认冷却时长（tick）：1 秒 = 20 tick */
    public static final int DEFAULT_COOLDOWN_TICKS = 20;

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
