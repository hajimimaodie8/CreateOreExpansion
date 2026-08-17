package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.content.skill.attribute.BreakBlockSpeedModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttributeType;

/**
 * 可修改属性的注册表 —— 目前仅用于挖掘速度。
 */
public final class AllModifiableAttributes {

    private AllModifiableAttributes() {
    }

    public static final ModifiableAttributeType<BreakBlockSpeedModifiableAttribute.Context, Float> BREAK_BLOCK_SPEED =
            () -> BreakBlockSpeedModifiableAttribute::new;
}
