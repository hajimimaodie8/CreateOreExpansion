package com.hjmmd_8.createoreexpansion.content.skill;

/**
 * 夺取物品的处置方式 —— 决定夺到的装备去向。
 *
 * <p>{@link #DROP} 直接生成掉落物；{@link #PICKUP} 收缴进玩家背包（背包满则退回掉落物）。</p>
 */
public enum LootDisposition {
    /** 生成掉落物 */
    DROP,
    /** 收缴进玩家背包（满则生成掉落物） */
    PICKUP
}
