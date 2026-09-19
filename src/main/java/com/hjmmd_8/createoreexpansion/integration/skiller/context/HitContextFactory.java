package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import com.leaf.skiller.foundation.skill.config.SkillContextFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * {@link HitSkillContext} 的上下文工厂（注册到 {@code skiller:skill_context_factory}）。
 *
 * <p>受击类技能只有一个来源：NeoForge 的 {@link LivingIncomingDamageEvent}
 * （旧触发点 {@code content/skill/handler/HurtLivingEntityHandler} 就是监听它），
 * 所以这里只需要事件这一条路——没有事件或事件类型不符时返回 null（拿不到事件就没法
 * 知道打的是谁，硬造一个上下文只会让技能做错事）。</p>
 *
 * @since 1.0.0
 */
public class HitContextFactory implements SkillContextFactory<HitSkillContext> {

    /** 注册 id：{@code createoreexpansion:hit_context} */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "hit_context");

    /** 该工厂在 {@code skiller:skill_context_factory} 注册表中的键（注册技能条目时用作 factoryKey）。 */
    public static final ResourceKey<SkillContextFactory<HitSkillContext>> KEY = createKey();

    @Override
    @Nullable
    public HitSkillContext create(SkillContextEnvironment env, ISkillInstance<HitSkillContext> instance) {
        if (env == null) {
            return null;
        }
        // getEvent() 在"无事件"时抛 NPE、类型不符时抛 CCE，统一兜底（与挖掘工厂同一处理）
        Object event;
        try {
            event = env.getEvent();
        } catch (RuntimeException e) {
            return null;
        }
        return event instanceof LivingIncomingDamageEvent damageEvent ? new HitSkillContext(damageEvent) : null;
    }

    /**
     * 没有事件时无法构造有意义的受击上下文 → 返回 null。
     *
     * <p>与挖掘上下文不同：那边的触发点（mixin）本身没有事件，靠 extraData 传现场信息；
     * 受击技能必定有事件，拿不到就说明调用方用错了环境，返回 null 让内核跳过本次更安全。</p>
     */
    @Override
    @Nullable
    public HitSkillContext createDefault(SkillContextEnvironment env, int level) {
        return create(env, null);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<SkillContextFactory<HitSkillContext>> createKey() {
        // SkillerRegistries.CONTEXT_FACTORY 是 ResourceKey<Registry<SkillContextFactory<?>>>，
        // 这里收窄到本工厂自己的泛型实参（同一把注册表键，仅泛型不同）。
        return (ResourceKey<SkillContextFactory<HitSkillContext>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.CONTEXT_FACTORY, ID);
    }
}
