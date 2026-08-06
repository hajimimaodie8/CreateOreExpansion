package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.content.skill.attribute.BreakBlockSpeedModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttributeType;

import java.util.List;
import java.util.function.Function;

public class AllModifiableAttributes<C, V> implements ModifiableAttributeType<C, V> {

    public static final List<AllModifiableAttributes<?, ?>> VALUES = new java.util.ArrayList<>();

    public static final AllModifiableAttributes<BreakBlockSpeedModifiableAttribute.Context, Float> BREAK_BLOCK_SPEED
            = of(BreakBlockSpeedModifiableAttribute::new);

    private final Function<C, ModifiableAttribute<V>> factory;
    private AllModifiableAttributes(Function<C, ModifiableAttribute<V>> factory) {
        this.factory = factory;
    }

    private static <C, V> AllModifiableAttributes<C, V> of(Function<C, ModifiableAttribute<V>> factory) {
        AllModifiableAttributes<C, V> attr = new AllModifiableAttributes<>(factory);
        VALUES.add(attr);
        return attr;
    }

    @Override
    public Function<C, ModifiableAttribute<V>> factory() {
        return factory;
    }
}
