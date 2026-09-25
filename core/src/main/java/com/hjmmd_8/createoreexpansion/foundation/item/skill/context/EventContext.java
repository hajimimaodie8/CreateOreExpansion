package com.hjmmd_8.createoreexpansion.foundation.item.skill.context;

import net.neoforged.bus.api.Event;

public class EventContext<E extends Event> {
    private final E event;
    public EventContext(E e) {
        event = e;
    }

    public E event() {
        return event;
    }
}
