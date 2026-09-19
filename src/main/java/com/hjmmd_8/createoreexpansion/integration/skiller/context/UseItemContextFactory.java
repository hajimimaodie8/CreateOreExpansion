package com.hjmmd_8.createoreexpansion.integration.skiller.context;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.leaf.skiller.api.registry.SkillerRegistries;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.config.SkillContextEnvironment;
import com.leaf.skiller.foundation.skill.config.SkillContextFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * {@link UseItemSkillContext} 的上下文工厂（注册到 {@code skiller:skill_context_factory}）。
 *
 * <p>只认右键的两条来源：{@link UseItemOnBlockEvent}（右键方块）与
 * {@link PlayerInteractEvent.RightClickItem}（右键空气/物品）；其余事件一律返回 null
 * （拿不到"用哪个物品点了哪里"就没法做右键技能，硬造上下文只会让技能做错事）。</p>
 *
 * @since 1.0.0
 */
public class UseItemContextFactory implements SkillContextFactory<UseItemSkillContext> {

    /** 注册 id：{@code createoreexpansion:use_item_context} */
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "use_item_context");

    /** 该工厂在 {@code skiller:skill_context_factory} 注册表中的键（注册技能条目时用作 factoryKey）。 */
    public static final ResourceKey<SkillContextFactory<UseItemSkillContext>> KEY = createKey();

    @Override
    @Nullable
    public UseItemSkillContext create(SkillContextEnvironment env, ISkillInstance<UseItemSkillContext> instance) {
        if (env == null) {
            return null;
        }
        // getEvent() 在"无事件"时抛 NPE、类型不符时抛 CCE，统一兜底（与其它工厂同一处理）
        Object event;
        try {
            event = env.getEvent();
        } catch (RuntimeException e) {
            return null;
        }
        if (event instanceof UseItemOnBlockEvent useOnBlock) {
            return new UseItemSkillContext(useOnBlock);
        }
        if (event instanceof PlayerInteractEvent.RightClickItem rightClick) {
            return new UseItemSkillContext(rightClick);
        }
        return null;
    }

    @Override
    @Nullable
    public UseItemSkillContext createDefault(SkillContextEnvironment env, int level) {
        return create(env, null);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<SkillContextFactory<UseItemSkillContext>> createKey() {
        // SkillerRegistries.CONTEXT_FACTORY 是 ResourceKey<Registry<SkillContextFactory<?>>>，
        // 这里收窄到本工厂自己的泛型实参（同一把注册表键，仅泛型不同）。
        return (ResourceKey<SkillContextFactory<UseItemSkillContext>>) (ResourceKey<?>)
                ResourceKey.create(SkillerRegistries.CONTEXT_FACTORY, ID);
    }
}
