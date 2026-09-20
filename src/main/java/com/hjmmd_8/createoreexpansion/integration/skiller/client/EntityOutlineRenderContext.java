package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.leaf.skiller.foundation.context.SkillContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 生物描边预览的渲染上下文（新内核版）。
 *
 * <p>与 {@link BlockOutlineRenderContext} 同构：新内核的渲染流程是 getContext 返回什么、
 * render 就收到什么，所以把"描哪个生物、什么颜色"放进上下文，render 里不再重新拾取。</p>
 *
 * @since 1.0.0
 */
public class EntityOutlineRenderContext implements SkillContext {

    private final Player player;
    private final LivingEntity target;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public EntityOutlineRenderContext(Player player, LivingEntity target,
                                      float red, float green, float blue, float alpha) {
        this.player = player;
        this.target = target;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public LivingEntity target() {
        return target;
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    public float alpha() {
        return alpha;
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
