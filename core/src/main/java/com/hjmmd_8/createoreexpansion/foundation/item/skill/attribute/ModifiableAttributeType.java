package com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute;

import java.util.function.Function;

public interface ModifiableAttributeType<C, V> {
    Function<C, ModifiableAttribute<V>> factory();

    default ModifiableAttribute<V> create(C value) {
        return factory().apply(value);
    }
}
