package com.hjmmd_8.createoreexpansion.integration.skiller.strategy;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.integration.skiller.context.HitSkillContext;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/**
 * 实体类策略（新内核版）——标记"这个技能作用于生物"，实际描边由客户端渲染器完成。
 *
 * <p>与旧 {@code foundation/item/skill/strategy/EntityStrategy} 同构：那边的
 * {@code calculate} 恒返回空集合（"实体计算由渲染器直接基于准星目标完成"），这里同样
 * {@link #collect} 不产出任何东西——它存在的意义只有一个：给技能一个
 * {@link #getRendererId()}，让客户端的 {@code StrategyRenderers} 能把描边渲染器找出来。</p>
 *
 * <p>这也解释了此前"剑类技能对着生物没有预选框"的原因：迁移时 {@code skin}/{@code plunder}
 * 被写成了普通 {@code ItemSkill}，**没有策略对象**，内核的渲染调度根本扫不到它们。</p>
 *
 * @since 1.0.0
 */
public class CoeEntityStrategy implements SkillStrategy<Entity, HitSkillContext> {

    /** 注册 id：{@code createoreexpansion:entity_strategy} */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "entity_strategy");

    /** 该策略在 {@code skiller:skill_strategy} 注册表中的键。 */
    public static final ResourceKey<SkillStrategy<?, ?>> KEY = createKey();

    /** 与方块描边区分开的渲染器 id（客户端注册同名渲染器）。 */
    public static final ResourceLocation RENDERER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "entity_outline");

    @Override
    public void collect(Set<Entity> set, HitSkillContext context, ISkillInstance<HitSkillContext> instance) {
        // 旧 EntityStrategy.calculate 恒返回空集合：描边的目标由渲染器直接取准星命中的实体。
        // 这里保持一致，不要在服务端"算实体"，避免把纯客户端的预览变成双端逻辑。
    }

    @Override
    public boolean canCollect(HitSkillContext context, ISkillInstance<HitSkillContext> instance) {
        return context != null && context.target() instanceof LivingEntity;
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<SkillStrategy<?, ?>> createKey() {
        return (ResourceKey<SkillStrategy<?, ?>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.STRATEGY, ID);
    }
}
