package com.hjmmd_8.createoreexpansion.foundation.item.skill.context;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * 使用物品类技能上下文（右键方块 / 右键空气）。
 *
 * @param <E> 触发的 NeoForge 事件类型
 */
public abstract class UseItemContext<E extends Event> extends EventContext<E> {
    public UseItemContext(E e) {
        super(e);
    }

    /**
     * 获取触发本次技能释放的玩家。
     */
    public abstract Player getPlayer();
}
