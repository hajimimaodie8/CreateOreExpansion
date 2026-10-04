package com.hjmmd_8.createoreexpansion.integration.skiller;

import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.context.SkillContext;
import com.leaf.skiller.foundation.skill.config.SkillContextFactory;
import com.leaf.skiller.foundation.strategy.SkillStrategy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>Skiller 注册表键的"泛型收窄"唯一出处</b>。
 *
 * <p>Skiller 的注册表键是<b>通配</b>形态（{@code ResourceKey<Registry<SkillContextFactory<?>>>}
 * 与 {@code ResourceKey<Registry<SkillStrategy<?, ?>>>}），而注册技能条目时我们要把它收窄到
 * 自己那一份具体泛型实参（例如 {@code ResourceKey<SkillContextFactory<HitSkillContext>>}）——
 * 同一把注册表键，只是泛型不同，所以必须走一次"经 {@code ResourceKey<?>} 中转"的
 * 未检查转换。</p>
 *
 * <p>这段转换原本在 7 个类里逐字重复（4 个上下文工厂 + 3 个策略），每处都带一份
 * {@code @SuppressWarnings("unchecked")} 与同一段注释。收进本类之后：</p>
 * <ul>
 *   <li>{@code @SuppressWarnings} 只剩一处（未检查转换的范围最小化）；</li>
 *   <li>Skiller 若改了注册表键的泛型形态，只有这一个文件要跟着改。</li>
 * </ul>
 *
 * <p>⚠ 转换的<b>安全性前提</b>（与改造前逐字相同的约定）：传入的 {@code id} 必须是该类
 * 自己声明的 {@code ID} 常量，而该 id 在对应注册表里只会以这一个具体泛型实参被取用 ——
 * 换句话说"同一把键不会被两个不同的上下文/策略类型拿去用"。这条由注册点
 * （{@code SkillerIntegration}）保证。</p>
 *
 * @since 1.0.0
 */
public final class SkillerRegistryKeys {

    private SkillerRegistryKeys() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 取 {@code skiller:skill_context_factory} 注册表里某个上下文的键，并收窄到 {@code SkillContextFactory<T>}。
     *
     * @param id 该工厂自己声明的注册 id
     * @param <T> 该工厂产出的上下文类型（由调用点的赋值目标推断）
     * @return 收窄后的注册表键
     */
    @SuppressWarnings("unchecked")
    public static <T extends SkillContext> ResourceKey<SkillContextFactory<T>> contextFactory(ResourceLocation id) {
        // SkillerRegistries.CONTEXT_FACTORY 是 ResourceKey<Registry<SkillContextFactory<?>>>，
        // 这里收窄到调用方自己的泛型实参（同一把注册表键，仅泛型不同）。
        return (ResourceKey<SkillContextFactory<T>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.CONTEXT_FACTORY, id);
    }

    /**
     * 取 {@code skiller:skill_strategy} 注册表里某个策略的键。
     *
     * @param id 该策略自己声明的注册 id
     * @return 收窄后的注册表键
     */
    @SuppressWarnings("unchecked")
    public static ResourceKey<SkillStrategy<?, ?>> strategy(ResourceLocation id) {
        // SkillerRegistries.STRATEGY 是 ResourceKey<Registry<SkillStrategy<?, ?>>>，
        // 这里只是把 ResourceKey 的通配实参搬过来（同一把注册表键）。
        return (ResourceKey<SkillStrategy<?, ?>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.STRATEGY, id);
    }
}
