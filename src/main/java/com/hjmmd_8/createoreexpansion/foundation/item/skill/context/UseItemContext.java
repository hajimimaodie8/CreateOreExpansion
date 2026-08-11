package com.hjmmd_8.createoreexpansion.foundation.item.skill.context;

import net.neoforged.bus.api.Event;

public class UseItemContext<E extends Event> extends EventContext<E> {
    public UseItemContext(E e) {
        super(e);
    }
}
