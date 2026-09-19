package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import com.leaf.skiller.foundation.skill.config.SkillContextFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@link BowShootSkillContext} 的上下文工厂（注册到 {@code skiller:skill_context_factory}）。
 *
 * <p>弓射击<b>没有</b>对应的 NeoForge 事件：旧触发点是弓物品自己的
 * {@code releaseUsing}（松手那一刻），所以调用方必须用
 * {@code SkillContextEnvironment.noEvent(player, level)} 再通过 extraData 带上现场信息
 * （约定键见 {@link #KEY_BOW}）。<b>绝不能</b>去调 {@code env.getEvent()}——
 * 无事件时它会抛 NPE。</p>
 *
 * @since 1.0.0
 */
public class BowContextFactory implements SkillContextFactory<BowShootSkillContext> {

    /** 注册 id：{@code createoreexpansion:bow_context} */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "bow_context");

    /** 该工厂在 {@code skiller:skill_context_factory} 注册表中的键（注册技能条目时用作 factoryKey）。 */
    public static final ResourceKey<SkillContextFactory<BowShootSkillContext>> KEY = createKey();

    /** extraData 约定键：本次射击所用的弓 */
    public static final String KEY_BOW = "bow";

    @Override
    @Nullable
    public BowShootSkillContext create(SkillContextEnvironment env, ISkillInstance<BowShootSkillContext> instance) {
        if (env == null) {
            return null;
        }
        Player player = env.getPlayer();
        if (player == null) {
            return null;
        }
        ItemStack bow;
        try {
            bow = env.getExtraData(KEY_BOW, ItemStack.class);
        } catch (RuntimeException e) {
            return null;
        }
        if (bow == null) {
            return null;
        }
        return new BowShootSkillContext(player, bow);
    }

    @Override
    @Nullable
    public BowShootSkillContext createDefault(SkillContextEnvironment env, int level) {
        return create(env, null);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<SkillContextFactory<BowShootSkillContext>> createKey() {
        // SkillerRegistries.CONTEXT_FACTORY 是 ResourceKey<Registry<SkillContextFactory<?>>>，
        // 这里收窄到本工厂自己的泛型实参（同一把注册表键，仅泛型不同）。
        return (ResourceKey<SkillContextFactory<BowShootSkillContext>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.CONTEXT_FACTORY, ID);
    }
}
