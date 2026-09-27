package com.hjmmd_8.createoreexpansion.mixin.renderers;

import com.hjmmd_8.createoreexpansion.integration.skiller.client.GlowingEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让技能预览挑中的生物走原版发光渲染（用渲染器设好的 outline 颜色）。
 *
 * <p>登记表是 {@link GlowingEntities#ENTITIES}：渲染统一前该字段挂在旧类
 * {@code client/tool/renderer/EntityOutlineRenderer} 上（写方已是新渲染器，读方是本
 * mixin），本类因此跨了两个框架；现在写读两端都指 {@code GlowingEntities}。</p>
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("TAIL"), cancellable = true)
    private void entityGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GlowingEntities.ENTITIES.contains(entity)) {
            GlowingEntities.ENTITIES.remove(entity);
            cir.setReturnValue(true);
        }
    }
}
