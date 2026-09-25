package com.hjmmd_8.createoreexpansion.skill.attribute;

public class SingleModifiableAttribute<A> implements ModifiableAttribute<A> {
    private A value;

    public SingleModifiableAttribute(A value) {
        this.value = value;
    }

    @Override
    public A getValue() {
        return value;
    }

    @Override
    public void setValue(A value) {
        this.value = value;
    }
}
