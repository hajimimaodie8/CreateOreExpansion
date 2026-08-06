package com.hjmmd_8.createoreexpansion.content.skill.attribute;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SingleModifiableAttribute;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class BreakBlockSpeedModifiableAttribute extends SingleModifiableAttribute<Float> {

    private final Context ctx;
    public BreakBlockSpeedModifiableAttribute(Context ctx) {
        super(ctx.originalSpeed);
        this.ctx = ctx;
    }

    public Level getLevel() {
        return ctx.level;
    }

    public BlockPos getPos() {
        return ctx.pos;
    }

    public static Context ctx(Level level, BlockPos pos, Float originalSpeed) {
        return new Context(level, pos, originalSpeed);
    }

    public record Context(Level level, BlockPos pos, Float originalSpeed) { }
}
